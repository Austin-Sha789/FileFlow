package com.austin.fileflow.service;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageHashServiceTest {

    private final ImageHashService service =
            new ImageHashService();

    @Test
    void shouldCalculateHammingDistance() {

        assertEquals(0,
                service.hammingDistance(0b1010L, 0b1010L));

        assertEquals(1,
                service.hammingDistance(0b1010L, 0b1000L));

        assertEquals(64,
                service.hammingDistance(0L, -1L));
    }

    @Test
    void shouldCompareRealImages() throws IOException {

        Path folder = Path.of("src/test/resources/similarity");

        long original = service.calculateHash(
                folder.resolve("original.png")
        );

        long resized = service.calculateHash(
                folder.resolve("resized.png")
        );

        long different = service.calculateHash(
                folder.resolve("different.png")
        );

        int sameImageDistance =
                service.hammingDistance(original, resized);

        int differentImageDistance =
                service.hammingDistance(original, different);

        System.out.println(
                "Original vs Resized: " + sameImageDistance
        );

        System.out.println(
                "Original vs Different: " + differentImageDistance
        );
    }
}