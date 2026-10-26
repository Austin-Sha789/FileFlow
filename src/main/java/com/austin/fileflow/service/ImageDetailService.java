package com.austin.fileflow.service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ImageDetailService {

    private static final int SIZE = 64;

    public int[] createSignature(Path path) throws IOException {

        BufferedImage original = ImageIO.read(path.toFile());

        if (original == null) {
            throw new IOException("Unsupported image: " + path);
        }

        return createSignature(original);
    }

    public int[] createSignature(BufferedImage original) {

        BufferedImage resized = new BufferedImage(
                SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D graphics = resized.createGraphics();

        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, SIZE, SIZE);

            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            graphics.drawImage(
                    original, 0, 0, SIZE, SIZE, null
            );

        } finally {
            graphics.dispose();
        }

        int[] signature = new int[SIZE * SIZE];

        int index = 0;

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {

                signature[index++] =
                        resized.getRaster().getSample(x, y, 0);
            }
        }

        return signature;
    }

    public double difference(int[] first, int[] second) {

        if (first.length != SIZE * SIZE
                || second.length != SIZE * SIZE) {
            throw new IllegalArgumentException(
                    "Invalid image signature size"
            );
        }

        long totalDifference = 0;

        for (int i = 0; i < first.length; i++) {
            totalDifference += Math.abs(
                    first[i] - second[i]
            );
        }

        return totalDifference / (double) (SIZE * SIZE * 255);
    }
}