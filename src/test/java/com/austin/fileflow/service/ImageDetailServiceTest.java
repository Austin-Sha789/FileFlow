package com.austin.fileflow.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageDetailServiceTest {

    @Test
    void shouldDistinguishSimilarAndDifferentImages()
            throws IOException {

        Path folder = Path.of(
                "src/test/resources/similarity"
        );

        ImageDetailService service =
                new ImageDetailService();

        int[] original = service.createSignature(
                folder.resolve("original.png")
        );

        int[] resized = service.createSignature(
                folder.resolve("resized.png")
        );

        int[] different = service.createSignature(
                folder.resolve("different.png")
        );

        double similarDifference =
                service.difference(original, resized);

        double differentDifference =
                service.difference(original, different);

        System.out.println(
                "Detail - Original vs Resized: "
                        + similarDifference
        );

        System.out.println(
                "Detail - Original vs Different: "
                        + differentDifference
        );

        assertTrue(
                similarDifference < differentDifference
        );
    }
}