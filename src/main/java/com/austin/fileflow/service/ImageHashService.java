package com.austin.fileflow.service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ImageHashService {

    public long calculateHash(Path imagePath) throws IOException {

        BufferedImage original = ImageIO.read(imagePath.toFile());

        if (original == null) {
            throw new IOException("Unsupported image: " + imagePath);
        }

        return calculateHash(original);
    }

    public long calculateHash(BufferedImage original) {

        BufferedImage resized =
                new BufferedImage(9, 8, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D graphics = resized.createGraphics();

        try {
            // Make transparent PNG backgrounds white
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 9, 8);

            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            graphics.drawImage(original, 0, 0, 9, 8, null);

        } finally {
            graphics.dispose();
        }

        long hash = 0L;

        for (int y = 0; y < 8; y++) {

            for (int x = 0; x < 8; x++) {

                int left = resized.getRaster().getSample(x, y, 0);
                int right = resized.getRaster().getSample(x + 1, y, 0);

                hash <<= 1;

                if (left > right) {
                    hash |= 1L;
                }
            }
        }

        return hash;
    }

    public int hammingDistance(long hash1, long hash2) {

        long difference = hash1 ^ hash2;

        return Long.bitCount(difference);
    }

}