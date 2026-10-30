package com.austin.fileflow.service;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

/** Resolves/extracts the bundled engine on a thumbnail worker, never on the FX thread. */
final class FfmpegRuntime {
    private static final String BUILD = "9.0.2-gae19868753";
    private static volatile Path bundledExecutable;

    static Path cacheRoot() {
        String override = System.getProperty("fileflow.previewCache");
        if (override != null && !override.isBlank()) return Path.of(override);
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            return (local == null || local.isBlank() ? Path.of(home, "AppData", "Local") : Path.of(local))
                    .resolve("FileFlow").resolve("Cache");
        }
        if (os.contains("mac")) return Path.of(home, "Library", "Caches", "FileFlow");
        String xdg = System.getenv("XDG_CACHE_HOME");
        return (xdg == null || xdg.isBlank() ? Path.of(home, ".cache") : Path.of(xdg)).resolve("fileflow");
    }

    static synchronized Path executable() throws IOException {
        String configured = System.getProperty("fileflow.ffmpeg", System.getenv("FILEFLOW_FFMPEG"));
        if (configured != null && !configured.isBlank()) {
            Path path = Path.of(configured).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) throw new IOException("Configured FFmpeg does not exist");
            return path;
        }
        if (bundledExecutable != null && Files.isRegularFile(bundledExecutable)) return bundledExecutable;
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (os.contains("win") && (arch.equals("amd64") || arch.equals("x86_64"))) {
            Path engine = cacheRoot().resolve("tools").resolve("ffmpeg-" + BUILD + "-windows-x86_64");
            Files.createDirectories(engine);
            // A second FileFlow instance must not launch a partially extracted executable.
            try (FileChannel channel = FileChannel.open(engine.resolve(".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                FileLock lock = null;
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
                while (lock == null) {
                    checkInterrupted();
                    lock = channel.tryLock();
                    if (lock == null) {
                        if (System.nanoTime() >= deadline) throw new IOException("FFmpeg extraction is busy");
                        try { Thread.sleep(50); }
                        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IOException("Preview cancelled", error); }
                    }
                }
                try {
                    Path executable = engine.resolve("ffmpeg.exe");
                    if (!Files.isRegularFile(engine.resolve(".ready")) || !Files.isRegularFile(executable)) {
                        try (var resource = FfmpegRuntime.class.getResourceAsStream("/native/windows-x86_64.zip")) {
                            if (resource == null) throw new IOException("Bundled FFmpeg is missing; run scripts/setup-video-thumbnails.ps1");
                            try (ZipInputStream zip = new ZipInputStream(resource)) {
                                long total = 0;
                                byte[] buffer = new byte[65536];
                                for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                                    checkInterrupted();
                                    String name = entry.getName();
                                    if (entry.isDirectory() || name.contains("/") || name.contains("\\") || name.equals("..")) {
                                        throw new IOException("Invalid bundled engine entry");
                                    }
                                    Path destination = engine.resolve(name);
                                    try (var output = Files.newOutputStream(destination)) {
                                        int read;
                                        while ((read = zip.read(buffer)) != -1) {
                                            checkInterrupted(); total += read;
                                            if (total > 512L * 1024 * 1024) throw new IOException("Bundled engine is too large");
                                            output.write(buffer, 0, read);
                                        }
                                    }
                                }
                            }
                        }
                        if (!Files.isRegularFile(executable)) throw new IOException("Bundled FFmpeg executable is missing");
                        Files.writeString(engine.resolve(".ready"), BUILD);
                    }
                    bundledExecutable = executable;
                    return executable;
                } finally { lock.release(); }
            }
        }
        // Allows development on other platforms; distributable Mac bundles will supply their own engine.
        String path = System.getenv("PATH");
        if (path != null) for (String directory : path.split(java.io.File.pathSeparator)) {
            if (directory.isBlank()) continue;
            Path candidate = Path.of(directory).resolve(os.contains("win") ? "ffmpeg.exe" : "ffmpeg");
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) return candidate;
        }
        throw new IOException("No FFmpeg engine for this platform");
    }

    static void extractFrame(Path video, Path image) throws IOException {
        Path executable = executable();
        // A short clip may contain no frame at one second. Retry at its beginning.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        for (String position : List.of("1", "0")) {
            checkInterrupted();
            List<String> command = List.of(executable.toString(), "-hide_banner", "-loglevel", "error", "-nostdin",
                    "-threads", "1", "-filter_threads", "1", "-ss", position,
                    "-protocol_whitelist", "file,pipe", "-i", video.toString(), "-map", "0:v:0",
                    "-an", "-sn", "-dn", "-frames:v", "1", "-vf",
                    "scale=512:384:force_original_aspect_ratio=decrease,setsar=1",
                    "-threads", "1", "-c:v", "png", "-f", "image2pipe", "pipe:1");
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) throw new IOException("Video preview timed out");
            run(command, image, Duration.ofNanos(remaining));
            if (Files.size(image) > 0) return;
        }
        throw new IOException("Video has no decodable frame");
    }

    /** Output goes directly to a small image file, so neither process pipe can block the FX thread. */
    static void run(List<String> command, Path output, Duration timeout) throws IOException {
        checkInterrupted();
        Process process = new ProcessBuilder(command).redirectOutput(output.toFile())
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            process.getOutputStream().close();
            if (!process.waitFor(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS)) {
                throw new IOException("Video preview timed out");
            }
            if (process.exitValue() != 0) throw new IOException("Video cannot be decoded");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Preview cancelled", error);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                boolean interrupted = Thread.interrupted();
                try { process.waitFor(2, TimeUnit.SECONDS); }
                catch (InterruptedException error) { interrupted = true; }
                finally { if (interrupted) Thread.currentThread().interrupt(); }
            }
        }
    }

    static void checkInterrupted() throws IOException {
        if (Thread.currentThread().isInterrupted()) throw new IOException("Preview cancelled");
    }
}
