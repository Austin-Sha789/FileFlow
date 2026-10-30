package com.austin.fileflow.service;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import javax.imageio.ImageIO;

/** Native fallback for QuickTime/HEVC videos when FFmpeg is missing or cannot decode them. */
final class MacQuickLookThumbnail {
    @FunctionalInterface interface Runner { void run(List<String> command, Path log, Duration timeout) throws IOException; }
    private MacQuickLookThumbnail() { }

    static void extract(Path video, Path image) throws IOException {
        extract(video, image, FfmpegRuntime::run);
    }

    static void extract(Path video, Path image, Runner runner) throws IOException {
        FfmpegRuntime.checkInterrupted();
        // Unique output directories prevent equal basenames in separate folders colliding.
        Path scratch = Files.createTempDirectory(image.toAbsolutePath().getParent(), "quicklook-");
        Path thumbnail = scratch.resolve(video.getFileName().toString() + ".png");
        Path log = scratch.resolve("quicklook.log");
        try {
            runner.run(List.of("/usr/bin/qlmanage", "-t", "-s", "512", "-o", scratch.toString(),
                    video.toAbsolutePath().normalize().toString()), log, Duration.ofSeconds(8));
            FfmpegRuntime.checkInterrupted();
            if (!Files.isRegularFile(thumbnail, LinkOption.NOFOLLOW_LINKS)
                    || Files.size(thumbnail) == 0 || Files.size(thumbnail) > 4L * 1024 * 1024) {
                throw new IOException("Quick Look did not produce a video thumbnail");
            }
            BufferedImage source;
            try (var input = ImageIO.createImageInputStream(thumbnail.toFile())) {
                if (input == null) throw new IOException("Invalid Quick Look image");
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IOException("Invalid Quick Look image");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    if (reader.getWidth(0) > 2048 || reader.getHeight(0) > 2048) throw new IOException("Quick Look image is too large");
                    source = reader.read(0);
                } finally { reader.dispose(); }
            }
            try {
                double scale = Math.min(1.0, Math.min(512.0 / source.getWidth(), 384.0 / source.getHeight()));
                var scaled = new BufferedImage(Math.max(1, (int) (source.getWidth() * scale)),
                        Math.max(1, (int) (source.getHeight() * scale)), BufferedImage.TYPE_INT_ARGB);
                try {
                    var graphics = scaled.createGraphics();
                    try { graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        graphics.drawImage(source, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
                    } finally { graphics.dispose(); }
                    FfmpegRuntime.checkInterrupted();
                    if (!ImageIO.write(scaled, "png", image.toFile())) throw new IOException("Cannot write Quick Look thumbnail");
                } finally { scaled.flush(); }
            } finally { source.flush(); }
        } finally {
            // These are only the known files in our private scratch directory.
            Files.deleteIfExists(thumbnail); Files.deleteIfExists(log); Files.deleteIfExists(scratch);
        }
    }
}
