package com.austin.fileflow.service;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.SimilarImagePair;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SimilarImageServiceTest {

    @Test
    void shouldFindSimilarImages() throws Exception {

        // 1. Scan the test folder
        Path folder = Path.of(
                "src/test/resources/similarity"
        );

        FileScanService scanService =
                new FileScanService();

        List<FileItem> items = scanService
                .scan(folder, () -> false)
                .getItems();

        // 2. Calculate the actual distance
        ImageHashService hashService =
                new ImageHashService();

        long original = hashService.calculateHash(
                folder.resolve("original.png")
        );

        long resized = hashService.calculateHash(
                folder.resolve("resized.png")
        );

        long different = hashService.calculateHash(
                folder.resolve("different.png")
        );

        int similarDistance =
                hashService.hammingDistance(original, resized);

        int differentDistance =
                hashService.hammingDistance(original, different);

        System.out.println(
                "Original vs Resized: " + similarDistance
        );

        System.out.println(
                "Original vs Different: " + differentDistance
        );

        // 3. Find similar images
        SimilarImageService service =
                new SimilarImageService();

        List<SimilarImagePair> matches =
            service.findSimilarImages(
                    items,
                    8
            );

        // 4. Verify the expected pair exists
        assertTrue(matches.stream().anyMatch(pair -> {

            Set<String> names = Set.of(
                    pair.first().getName(),
                    pair.second().getName()
            );

            return names.equals(
                    Set.of("original.png", "resized.png")
            );
        }));

        assertFalse(matches.stream().anyMatch(pair ->
                pair.first().getName().equals("different.png")
                || pair.second().getName().equals("different.png")
        ));
    }
}