package com.austin.fileflow.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.imageio.ImageIO;

/** One canonical poster per file revision; list and Quick Info requests share the disk image. */
public final class VideoThumbnailDecoder implements ThumbnailLoader.Decoder {
    private static final Set<String> EXTENSIONS = Set.of("mp4", "mov", "mkv", "avi", "webm", "m4v", "wmv",
            "mpg", "mpeg", "mts", "m2ts", "ts", "flv", "3gp", "ogv");
    private static final int MAX_FILES = 2000;
    private static final long MAX_BYTES = 128L * 1024 * 1024;
    private final Path directory;
    private final FrameExtractor extractor;
    private final Map<String, Long> failures = new LinkedHashMap<>();
    private int generated;
    private boolean pruned;

    @FunctionalInterface interface FrameExtractor { void extract(Path video, Path image) throws IOException; }
    public VideoThumbnailDecoder() { this(FfmpegRuntime.cacheRoot().resolve("video-v1"), FfmpegRuntime::extractFrame); }
    VideoThumbnailDecoder(Path directory, FrameExtractor extractor) { this.directory = directory; this.extractor = extractor; }

    public static boolean supportsExtension(String extension) {
        return extension != null && EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT).replaceFirst("^\\.", ""));
    }
    public static boolean supports(Path path) {
        if (path == null || path.getFileName() == null) return false;
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && supportsExtension(name.substring(dot + 1));
    }

    @Override public ThumbnailLoader.Thumbnail decode(ThumbnailLoader.Key key) throws IOException {
        FfmpegRuntime.checkInterrupted();
        String revision = revision(key.path());
        Long failed = failures.get(revision);
        if (failed != null && System.nanoTime() - failed < java.util.concurrent.TimeUnit.SECONDS.toNanos(60)) {
            throw new IOException("Video preview is temporarily unavailable");
        }
        Files.createDirectories(directory);
        if (!pruned) { prune(); pruned = true; }
        Path poster = directory.resolve(revision + ".png");
        if (Files.isRegularFile(poster)) {
            try {
                var thumbnail = ThumbnailLoader.decode(new ThumbnailLoader.Key(poster, key.width(), key.height()));
                try { Files.setLastModifiedTime(poster, FileTime.fromMillis(System.currentTimeMillis())); } catch (IOException ignored) { }
                return thumbnail;
            } catch (IOException corruptCache) { Files.deleteIfExists(poster); }
        }
        Path temporary = Files.createTempFile(directory, "frame-", ".tmp");
        try {
            extractor.extract(key.path(), temporary);
            FfmpegRuntime.checkInterrupted();
            if (Files.size(temporary) == 0 || Files.size(temporary) > 4L * 1024 * 1024) throw new IOException("Invalid video poster size");
            var image = ImageIO.read(temporary.toFile());
            if (image == null) throw new IOException("Invalid video poster");
            try {
                if (image.getWidth() > 512 || image.getHeight() > 384) throw new IOException("Video poster exceeds preview dimensions");
            } finally { image.flush(); }
            if (!revision.equals(revision(key.path()))) throw new IOException("Video changed while generating its preview");
            try { Files.move(temporary, poster, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, poster, StandardCopyOption.REPLACE_EXISTING); }
            if (++generated % 32 == 0) prune();
            return ThumbnailLoader.decode(new ThumbnailLoader.Key(poster, key.width(), key.height()));
        } catch (IOException failure) {
            if (!Thread.currentThread().isInterrupted()) {
                failures.put(revision, System.nanoTime());
                if (failures.size() > 256) failures.remove(failures.keySet().iterator().next());
            }
            throw failure;
        } finally { Files.deleteIfExists(temporary); }
    }

    private String revision(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        if (!attributes.isRegularFile()) throw new IOException("Video is not a regular file");
        String identity = "poster-v1\n" + path.toAbsolutePath().normalize() + "\n" + attributes.size()
                + "\n" + attributes.lastModifiedTime();
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private record CachedFile(Path path, long bytes, long modified) { }
    private void prune() {
        try (var entries = Files.list(directory)) {
            List<CachedFile> files = new ArrayList<>();
            for (Path path : entries.toList()) {
                if (!path.getFileName().toString().endsWith(".png")) continue;
                var attributes = Files.readAttributes(path, BasicFileAttributes.class);
                files.add(new CachedFile(path, attributes.size(), attributes.lastModifiedTime().toMillis()));
            }
            files.sort(Comparator.comparingLong(CachedFile::modified));
            long bytes = files.stream().mapToLong(CachedFile::bytes).sum();
            int count = files.size();
            for (CachedFile file : files) {
                if (bytes <= MAX_BYTES && count <= MAX_FILES) break;
                if (Files.deleteIfExists(file.path())) { bytes -= file.bytes(); count--; }
            }
        } catch (IOException ignored) { /* Cache maintenance must not fail a usable preview. */ }
    }
}
