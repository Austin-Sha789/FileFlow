package com.austin.fileflow.service;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ThumbnailLoaderTest {
    @TempDir Path directory;
    private static final ThumbnailLoader.Thumbnail PIXEL = new ThumbnailLoader.Thumbnail(1, 1, new int[]{0xff336699});
    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(5, TimeUnit.SECONDS), "Background decoder did not reach checkpoint"); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }
    private static ThumbnailLoader.Thumbnail get(CompletableFuture<ThumbnailLoader.Thumbnail> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS);
    }

    @Test void largeJpegIsSubsampledToPreviewSize() throws Exception {
        BufferedImage source = new BufferedImage(6000, 4000, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, 0xffff8800);
        Path path = directory.resolve("camera.jpg");
        ImageIO.write(source, "jpg", path.toFile()); source.flush();
        var result = ThumbnailLoader.decode(new ThumbnailLoader.Key(path, 220, 160));
        assertEquals(220, result.width()); assertEquals(147, result.height());
        assertEquals(220 * 147, result.pixels().length);
    }

    @Test void transparentPngPreservesAlphaAndDoesNotUpscale() throws Exception {
        BufferedImage source = new BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0x7f112233);
        Path path = directory.resolve("alpha.png"); ImageIO.write(source, "png", path.toFile());
        var result = ThumbnailLoader.decode(new ThumbnailLoader.Key(path, 112, 84));
        assertEquals(16, result.width()); assertEquals(8, result.height());
        assertEquals(0x7f, result.pixels()[0] >>> 24);
    }

    @Test void requestsSharePendingDecodeAndCompletedCache() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        try (var loader = new ThumbnailLoader(1, 2, key -> {
            calls.incrementAndGet(); started.countDown(); await(release); return PIXEL;
        })) {
            var first = loader.load(directory.resolve("same"), 20, 20); await(started);
            var second = loader.load(directory.resolve("same"), 20, 20);
            release.countDown(); assertSame(get(first), get(second));
            assertSame(PIXEL, get(loader.load(directory.resolve("same"), 20, 20)));
            assertEquals(1, calls.get());
        } finally { release.countDown(); }
    }

    @Test void cancellingOneSubscriberDoesNotCancelAnother() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        try (var loader = new ThumbnailLoader(1, 2, key -> { started.countDown(); await(release); return PIXEL; })) {
            var first = loader.load(directory.resolve("same"), 20, 20); await(started);
            var second = loader.load(directory.resolve("same"), 20, 20);
            first.cancel(false); release.countDown(); assertSame(PIXEL, get(second));
        } finally { release.countDown(); }
    }

    @Test void abandonedQueuedWorkIsRemovedBeforeDecode() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        try (var loader = new ThumbnailLoader(1, 1, key -> {
            calls.incrementAndGet(); started.countDown(); await(release); return PIXEL;
        })) {
            var first = loader.load(directory.resolve("first"), 20, 20); await(started);
            var abandoned = loader.load(directory.resolve("abandoned"), 20, 20); abandoned.cancel(false);
            var latest = loader.load(directory.resolve("latest"), 20, 20);
            release.countDown(); get(first); get(latest); assertEquals(2, calls.get());
        } finally { release.countDown(); }
    }

    @Test void saturatedQueueNeverDecodesOnTheCallerThread() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        try (var loader = new ThumbnailLoader(1, 1, key -> {
            calls.incrementAndGet(); started.countDown(); await(release); return PIXEL;
        })) {
            var first = loader.load(directory.resolve("first"), 20, 20); await(started);
            var second = loader.load(directory.resolve("second"), 20, 20);
            var refused = loader.load(directory.resolve("third"), 20, 20);
            ExecutionException error = assertThrows(ExecutionException.class, () -> get(refused));
            assertInstanceOf(RejectedExecutionException.class, error.getCause());
            assertEquals(1, calls.get());
            release.countDown(); get(first); get(second); assertEquals(2, calls.get());
        } finally { release.countDown(); }
    }

    @Test void invalidatedInFlightDecodeCannotReplaceNewerResult() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        try (var loader = new ThumbnailLoader(1, 2, key -> {
            if (calls.incrementAndGet() == 1) { started.countDown(); await(release); }
            return PIXEL;
        })) {
            Path path = directory.resolve("changed");
            var stale = loader.load(path, 20, 20); await(started); loader.invalidate(path);
            assertTrue(stale.isCancelled());
            var fresh = loader.load(path, 20, 20); release.countDown(); get(fresh);
            assertEquals(2, calls.get());
            get(loader.load(path, 20, 20)); assertEquals(2, calls.get());
        } finally { release.countDown(); }
    }

    @Test void unreadableImageFailsWithoutBlockingSubsequentRequests() throws Exception {
        try (var loader = new ThumbnailLoader()) {
            assertThrows(ExecutionException.class, () -> get(loader.load(directory.resolve("missing.jpg"), 20, 20)));
            Path path = directory.resolve("valid.png");
            ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", path.toFile());
            assertEquals(8, get(loader.load(path, 20, 20)).width());
        }
    }
}
