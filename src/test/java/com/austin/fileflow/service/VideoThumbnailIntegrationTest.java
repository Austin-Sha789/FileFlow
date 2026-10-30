package com.austin.fileflow.service;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "fileflow.videoTests", matches = "true")
class VideoThumbnailIntegrationTest {
    @TempDir Path directory;
    private Path clip(String name, String codec, String length) throws Exception {
        Path path = directory.resolve(name);
        Path log = directory.resolve(name + ".log");
        var command = List.of(FfmpegRuntime.executable().toString(), "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                "-f", "lavfi", "-i", "testsrc2=size=320x180:rate=10", "-t", length,
                "-an", "-threads", "1", "-c:v", codec, path.toString());
        FfmpegRuntime.run(command, log, Duration.ofSeconds(20));
        assertTrue(Files.size(path) > 0);
        return path;
    }
    @Test void actualBundledEngineDecodesCommonContainersAndUnicodePaths() throws Exception {
        var decoder = new VideoThumbnailDecoder(directory.resolve("cache"), FfmpegRuntime::extractFrame);
        for (var format : Map.of("mp4", "mpeg4", "mov", "mpeg4", "mkv", "ffv1", "avi", "mpeg4", "webm", "libvpx").entrySet()) {
            Path video = clip("旅行 ' & sample." + format.getKey(), format.getValue(), "2");
            var result = decoder.decode(new ThumbnailLoader.Key(video, 112, 84));
            assertEquals(112, result.width(), format.getKey());
            assertEquals(63, result.height(), format.getKey());
            assertTrue(Arrays.stream(result.pixels()).distinct().count() > 10, "Poster should contain an actual video frame");
        }
    }
    @Test void subSecondClipFallsBackToBeginning() throws Exception {
        Path video = clip("short.mp4", "mpeg4", "0.2");
        var decoder = new VideoThumbnailDecoder(directory.resolve("cache"), FfmpegRuntime::extractFrame);
        assertEquals(220, decoder.decode(new ThumbnailLoader.Key(video, 220, 160)).width());
    }
    @Test void corruptVideoFailsAndDoesNotBlockTheNextClip() throws Exception {
        Path broken = Files.writeString(directory.resolve("bad.mp4"), "not a video");
        Path good = clip("good.mp4", "mpeg4", "2");
        try (var loader = new ThumbnailLoader(1, 2, new VideoThumbnailDecoder(directory.resolve("cache"), FfmpegRuntime::extractFrame), false, true)) {
            assertThrows(ExecutionException.class, () -> loader.load(broken, 56, 42).get(15, TimeUnit.SECONDS));
            assertEquals(56, loader.load(good, 56, 42).get(15, TimeUnit.SECONDS).width());
        }
    }
    @Test void interruptingExtractionTerminatesTheChildProcess() throws Exception {
        var command = List.of(FfmpegRuntime.executable().toString(), "-nostdin", "-loglevel", "error", "-re",
                "-f", "lavfi", "-i", "color=size=16x16:rate=1", "-t", "30", "-f", "null", "-");
        Thread worker = new Thread(() -> {
            try { FfmpegRuntime.run(command, directory.resolve("cancel.out"), Duration.ofSeconds(20)); }
            catch (java.io.IOException expected) { }
        });
        ProcessHandle child = null;
        try {
            worker.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (child == null && System.nanoTime() < deadline) {
                try (var children = ProcessHandle.current().children()) {
                    child = children.filter(handle -> handle.info().command().orElse("").toLowerCase(Locale.ROOT).contains("ffmpeg")).findFirst().orElse(null);
                }
                if (child == null) Thread.sleep(20);
            }
            assertNotNull(child, "FFmpeg should be running before cancellation");
            worker.interrupt(); worker.join(3000);
            assertFalse(worker.isAlive(), "Cancelled worker must release the queue");
            assertFalse(child.isAlive(), "Cancelled extraction must not leave an FFmpeg process");
        } finally { worker.interrupt(); worker.join(3000); }
    }
    @Test void processTimeoutIsEnforced() throws Exception {
        Path output = directory.resolve("timeout.out");
        var command = List.of(FfmpegRuntime.executable().toString(), "-nostdin", "-loglevel", "error", "-re",
                "-f", "lavfi", "-i", "color=size=16x16:rate=1", "-t", "30", "-f", "null", "-");
        long started = System.nanoTime();
        assertThrows(java.io.IOException.class, () -> FfmpegRuntime.run(command, output, Duration.ofMillis(250)));
        assertTrue(System.nanoTime() - started < TimeUnit.SECONDS.toNanos(5));
    }
}