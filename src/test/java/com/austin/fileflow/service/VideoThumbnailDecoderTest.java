package com.austin.fileflow.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class VideoThumbnailDecoderTest {
    @TempDir Path directory;
    private void poster(Path video, Path output) throws IOException {
        ImageIO.write(new BufferedImage(160, 90, BufferedImage.TYPE_INT_RGB), "png", output.toFile());
    }
    @Test void listAndQuickInfoSharePersistentPosterAcrossDecoderInstances() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Path video = Files.writeString(directory.resolve("拍摄 ' & clip.MP4"), "fixture");
        Path cache = directory.resolve("cache");
        var first = new VideoThumbnailDecoder(cache, (source, output) -> { calls.incrementAndGet(); poster(source, output); });
        assertEquals(56, first.decode(new ThumbnailLoader.Key(video, 56, 42)).width());
        var second = new VideoThumbnailDecoder(cache, (source, output) -> { calls.incrementAndGet(); poster(source, output); });
        assertEquals(160, second.decode(new ThumbnailLoader.Key(video, 440, 320)).width());
        assertEquals(1, calls.get());
    }
    @Test void modifiedSourceGeneratesNewPosterEvenAtTheSameRequestedSize() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Path video = Files.writeString(directory.resolve("clip.mp4"), "same-size");
        var decoder = new VideoThumbnailDecoder(directory.resolve("cache"), (source, output) -> { calls.incrementAndGet(); poster(source, output); });
        var key = new ThumbnailLoader.Key(video, 56, 42);
        decoder.decode(key);
        Files.setLastModifiedTime(video, FileTime.fromMillis(Files.getLastModifiedTime(video).toMillis() + 5000));
        decoder.decode(key);
        assertEquals(2, calls.get());
    }
    @Test void corruptCacheIsRebuilt() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Path video = Files.writeString(directory.resolve("clip.mp4"), "fixture");
        Path cache = directory.resolve("cache");
        var decoder = new VideoThumbnailDecoder(cache, (source, output) -> { calls.incrementAndGet(); poster(source, output); });
        var key = new ThumbnailLoader.Key(video, 56, 42); decoder.decode(key);
        try (var paths = Files.list(cache)) { Files.writeString(paths.findFirst().orElseThrow(), "damaged"); }
        decoder.decode(key); assertEquals(2, calls.get());
    }
    @Test void failuresBackOffWithoutPublishingPartialPosters() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Path video = Files.writeString(directory.resolve("bad.mp4"), "fixture");
        Path cache = directory.resolve("cache");
        var decoder = new VideoThumbnailDecoder(cache, (source, output) -> { calls.incrementAndGet(); Files.writeString(output, "partial"); throw new IOException("bad clip"); });
        var key = new ThumbnailLoader.Key(video, 56, 42);
        assertThrows(IOException.class, () -> decoder.decode(key));
        assertThrows(IOException.class, () -> decoder.decode(key));
        assertEquals(1, calls.get());
        try (var paths = Files.list(cache)) { assertEquals(0, paths.count()); }
    }
    @Test void changedDuringExtractionIsNotPublished() throws Exception {
        Path video = Files.writeString(directory.resolve("changing.mp4"), "before");
        Path cache = directory.resolve("cache");
        var decoder = new VideoThumbnailDecoder(cache, (source, output) -> { poster(source, output); Files.writeString(source, "after-change"); });
        assertThrows(IOException.class, () -> decoder.decode(new ThumbnailLoader.Key(video, 56, 42)));
        try (var paths = Files.list(cache)) { assertEquals(0, paths.count()); }
    }
    @Test void cancellingAnActiveVideoRequestInterruptsItAndReleasesTheQueue() throws Exception {
        CountDownLatch started = new CountDownLatch(1), interrupted = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        var pixel = new ThumbnailLoader.Thumbnail(1, 1, new int[]{0xff000000});
        try (var loader = new ThumbnailLoader(1, 1, key -> {
            if (calls.incrementAndGet() == 1) {
                started.countDown();
                try { new CountDownLatch(1).await(); }
                catch (InterruptedException error) { interrupted.countDown(); Thread.currentThread().interrupt(); throw new IOException("cancelled", error); }
            }
            return pixel;
        }, false, true)) {
            var abandoned = loader.load(directory.resolve("first.mp4"), 20, 20);
            assertTrue(started.await(5, TimeUnit.SECONDS));
            abandoned.cancel(false);
            assertTrue(interrupted.await(5, TimeUnit.SECONDS));
            assertSame(pixel, loader.load(directory.resolve("latest.mp4"), 20, 20).get(5, TimeUnit.SECONDS));
        }
    }
    @Test void videoModeDoesNotReturnStalePathOnlyMemoryResults() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (var loader = new ThumbnailLoader(1, 1, key -> {
            calls.incrementAndGet(); return new ThumbnailLoader.Thumbnail(1, 1, new int[]{0});
        }, false, true)) {
            loader.load(directory.resolve("clip.mp4"), 20, 20).get(5, TimeUnit.SECONDS);
            loader.load(directory.resolve("clip.mp4"), 20, 20).get(5, TimeUnit.SECONDS);
            assertEquals(2, calls.get());
        }
    }
    @Test void commonVideoExtensionsAreRecognizedWithoutCaseSensitivity() {
        for (String extension : new String[]{"MP4", "mov", "webm", "m4v", "m2ts", "mkv", "avi", "wmv"}) {
            assertTrue(VideoThumbnailDecoder.supports(directory.resolve("file." + extension)));
        }
        assertFalse(VideoThumbnailDecoder.supports(directory.resolve("file.mp3")));
        assertFalse(VideoThumbnailDecoder.supports(directory.resolve("file.mp4.txt")));
    }
}