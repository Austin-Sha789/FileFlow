package com.austin.fileflow.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MacVideoThumbnailTest {
    @TempDir Path directory;
    private Path engine(Path folder) throws IOException {
        Files.createDirectories(folder); Path file = Files.writeString(folder.resolve("ffmpeg"), "fixture");
        return file;
    }
    // Test Unix lookup rules on Windows without relying on Windows executable extensions.
    private Path find(String os, String searchPath, String separator, List<Path> folders) {
        return FfmpegRuntime.findInstalled(os, searchPath, separator, folders, Files::isRegularFile);
    }
    @Test void guiLaunchFindsHomebrewEvenWhenItIsAbsentFromPath() throws Exception {
        Path brew = engine(directory.resolve("homebrew/bin"));
        assertEquals(brew, find("mac os x", directory.resolve("empty").toString(), ";", List.of(brew.getParent())));
        assertNull(find("linux", null, ";", List.of(brew.getParent())));
        assertNull(find("mac os x", null, ";", List.of(directory.resolve("missing"))));
    }
    @Test void pathEngineTakesPrecedenceOverHomebrewFallback() throws Exception {
        Path configured = engine(directory.resolve("custom")), brew = engine(directory.resolve("homebrew/bin"));
        assertEquals(configured, find("mac os x", configured.getParent().toString(), ";", List.of(brew.getParent())));
    }
    @Test void macFallsBackAfterMissingEngineOrDecodeFailureButWindowsDoesNot() throws Exception {
        AtomicInteger fallback = new AtomicInteger();
        VideoThumbnailDecoder.FrameExtractor unavailable = (video, image) -> { throw new IOException("missing engine or unsupported codec"); };
        VideoThumbnailDecoder.FrameExtractor nativeEngine = (video, image) -> { fallback.incrementAndGet(); Files.writeString(image, "native frame"); };
        Path output = directory.resolve("poster");
        FfmpegRuntime.extractFrame(directory.resolve("clip.mov"), output, true, unavailable, nativeEngine);
        assertEquals("native frame", Files.readString(output)); assertEquals(1, fallback.get());
        assertThrows(IOException.class, () -> FfmpegRuntime.extractFrame(directory.resolve("clip.mov"), output, false, unavailable, nativeEngine));
        assertEquals(1, fallback.get());
        FfmpegRuntime.extractFrame(directory.resolve("clip.mov"), output, true, nativeEngine,
                (video, image) -> fail("Working FFmpeg must not invoke Quick Look"));
    }
    @Test void cancellationDoesNotStartANativeFallback() {
        AtomicInteger fallback = new AtomicInteger();
        try {
            assertThrows(IOException.class, () -> FfmpegRuntime.extractFrame(directory.resolve("clip.mov"), directory.resolve("poster"), true,
                    (video, image) -> { Thread.currentThread().interrupt(); throw new IOException("cancelled"); },
                    (video, image) -> fallback.incrementAndGet()));
            assertEquals(0, fallback.get());
        } finally { Thread.interrupted(); }
    }
    @Test void quickLookUsesIsolatedOutputUnicodePathsAndBoundedPosterDimensions() throws Exception {
        Path video = directory.resolve("旅行 ' & movie.MOV"), output = directory.resolve("poster.tmp");
        var folders = new ArrayList<Path>();
        MacQuickLookThumbnail.extract(video, output, (command, log, timeout) -> {
            assertEquals("/usr/bin/qlmanage", command.get(0)); assertTrue(command.contains("-t"));
            assertEquals(video.toAbsolutePath().toString(), command.get(command.size()-1));
            assertTrue(timeout.toSeconds() <= 10); Path scratch = Path.of(command.get(command.indexOf("-o")+1)); folders.add(scratch);
            var image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(256,256,0xff438cff); ImageIO.write(image,"png",scratch.resolve(video.getFileName()+".png").toFile());
        });
        var result = ImageIO.read(output.toFile()); assertEquals(384,result.getWidth()); assertEquals(384,result.getHeight());
        assertTrue(Arrays.stream(result.getRGB(0,0,384,384,null,0,384)).anyMatch(pixel -> (pixel >>> 24) != 0));
        for(Path scratch : folders) assertFalse(Files.exists(scratch));
    }
    @Test void missingOrInvalidNativeResultIsRejectedAndScratchFilesAreCleaned() throws Exception {
        Path output = directory.resolve("poster.tmp");
        assertThrows(IOException.class, () -> MacQuickLookThumbnail.extract(directory.resolve("clip.mov"), output, (command, log, timeout) -> {}));
        assertThrows(IOException.class, () -> MacQuickLookThumbnail.extract(directory.resolve("clip.mov"), output, (command, log, timeout) -> {
            Path scratch=Path.of(command.get(command.indexOf("-o")+1)); Files.writeString(scratch.resolve("clip.mov.png"),"broken");
        }));
        assertFalse(Files.exists(output)); try(var files=Files.list(directory)){assertEquals(0,files.count());}
    }
}
