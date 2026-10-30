package com.austin.fileflow.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Bounded image decoding. No JavaFX objects or application-thread I/O. */
public final class ThumbnailLoader implements AutoCloseable {
    public record Key(Path path, int width, int height) {
        public Key {
            path = path.toAbsolutePath().normalize();
            if (width < 1 || height < 1 || width > 1024 || height > 1024) {
                throw new IllegalArgumentException("Invalid preview size");
            }
        }
    }
    public record Thumbnail(int width, int height, int[] pixels) { }
    @FunctionalInterface interface Decoder { Thumbnail decode(Key key) throws IOException; }
    private final ThreadPoolExecutor executor;
    private final Decoder decoder;
    private final Map<Key, Job> pending = new HashMap<>();
    private final LinkedHashMap<Key, Thumbnail> cache = new LinkedHashMap<>(64, 0.75f, true);
    private int cachedPixels;
    private static final int MAX_CACHED_PIXELS = 4 * 1024 * 1024;
    private boolean closed;

    public ThumbnailLoader() { this(2, 32, ThumbnailLoader::decode); }
    ThumbnailLoader(int workers, int queueSize, Decoder decoder) {
        this.decoder = decoder;
        executor = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize), runnable -> {
                    Thread worker = new Thread(runnable, "fileflow-thumbnail");
                    worker.setDaemon(true);
                    worker.setPriority(Thread.NORM_PRIORITY - 1);
                    return worker;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public synchronized CompletableFuture<Thumbnail> load(Path path, int width, int height) {
        Key key = new Key(path, width, height);
        if (closed) return CompletableFuture.failedFuture(new RejectedExecutionException("Preview loader closed"));
        Thumbnail cached = cache.get(key);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        Job job = pending.get(key);
        boolean fresh = job == null;
        if (fresh) { job = new Job(key); pending.put(key, job); }
        Job selected = job;
        CompletableFuture<Thumbnail> subscriber = new CompletableFuture<>();
        job.subscribers.add(subscriber);
        subscriber.whenComplete((result, failure) -> {
            if (subscriber.isCancelled()) release(selected, subscriber);
        });
        if (fresh) {
            try { executor.execute(job.task); }
            catch (RejectedExecutionException rejected) { finish(job, null, rejected); }
        }
        return subscriber;
    }

    private synchronized void release(Job job, CompletableFuture<Thumbnail> subscriber) {
        job.subscribers.remove(subscriber);
        if (job.subscribers.isEmpty() && pending.remove(job.key, job)) {
            job.task.cancel(false);
            executor.remove(job.task);
        }
    }

    private void finish(Job job, Thumbnail thumbnail, Throwable failure) {
        ArrayList<CompletableFuture<Thumbnail>> subscribers;
        synchronized (this) {
            if (!pending.remove(job.key, job)) return;
            subscribers = new ArrayList<>(job.subscribers);
            job.subscribers.clear();
            if (thumbnail != null) {
                cache.put(job.key, thumbnail);
                cachedPixels += thumbnail.pixels().length;
                while (cache.size() > 256 || cachedPixels > MAX_CACHED_PIXELS) {
                    var iterator = cache.entrySet().iterator();
                    cachedPixels -= iterator.next().getValue().pixels().length;
                    iterator.remove();
                }
            }
        }
        for (var subscriber : subscribers) {
            if (failure == null) subscriber.complete(thumbnail);
            else subscriber.completeExceptionally(failure);
        }
    }

    public synchronized void invalidate(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        cache.entrySet().removeIf(entry -> {
            if (!entry.getKey().path().equals(normalized)) return false;
            cachedPixels -= entry.getValue().pixels().length;
            return true;
        });
        for (Job job : new ArrayList<>(pending.values())) {
            if (job.key.path().equals(normalized)) cancel(job);
        }
    }

    private void cancel(Job job) {
        pending.remove(job.key, job);
        job.task.cancel(false);
        executor.remove(job.task);
        var subscribers = new ArrayList<>(job.subscribers);
        job.subscribers.clear();
        subscribers.forEach(future -> future.cancel(false));
    }

    public synchronized void clear() {
        cache.clear(); cachedPixels = 0;
        for (Job job : new ArrayList<>(pending.values())) cancel(job);
    }

    @Override public synchronized void close() {
        closed = true; clear(); executor.shutdownNow();
    }

    private final class Job {
        final Key key;
        final ArrayList<CompletableFuture<Thumbnail>> subscribers = new ArrayList<>();
        final FutureTask<Void> task;
        Job(Key key) {
            this.key = key;
            task = new FutureTask<>(() -> {
                try { finish(this, decoder.decode(key), null); }
                catch (IOException | RuntimeException failure) { finish(this, null, failure); }
                return null;
            });
        }
    }

    static Thumbnail decode(Key key) throws IOException {
        try (var input = new MemoryCacheImageInputStream(Files.newInputStream(key.path()))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int sourceWidth = reader.getWidth(0), sourceHeight = reader.getHeight(0);
                double scale = Math.min(1.0, Math.min((double) key.width / sourceWidth, (double) key.height / sourceHeight));
                int step = Math.max(1, (int) Math.floor(1.0 / scale));
                var params = reader.getDefaultReadParam();
                params.setSourceSubsampling(step, step, 0, 0);
                BufferedImage sampled = reader.read(0, params);
                int width = Math.max(1, (int) Math.round(sourceWidth * scale));
                int height = Math.max(1, (int) Math.round(sourceHeight * scale));
                BufferedImage small = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = small.createGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.drawImage(sampled, 0, 0, width, height, null);
                } finally { graphics.dispose(); sampled.flush(); }
                int[] pixels = small.getRGB(0, 0, width, height, null, 0, width);
                small.flush();
                return new Thumbnail(width, height, pixels);
            } finally { reader.dispose(); }
        }
    }
}
