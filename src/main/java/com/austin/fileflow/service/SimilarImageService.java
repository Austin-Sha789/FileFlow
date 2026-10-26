package com.austin.fileflow.service;

import com.austin.fileflow.model.SimilarImageGroup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.function.BooleanSupplier;
import java.util.function.BiConsumer;
import java.util.concurrent.CancellationException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.FileType;
import com.austin.fileflow.model.SimilarImagePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimilarImageService {

    private final ImageHashService hashService =
        new ImageHashService();

    private final ImageDetailService detailService =
        new ImageDetailService();

    private record HashedImage(
            FileItem item,
            long hash,
            int[] detail
    ) {}

    private record CachedSignature(
        long size,
        FileTime lastModified,
        long hash,
        int[] detail
) {}

    private static final int MAX_CACHE_ENTRIES = 2048;

    private final Map<Path, CachedSignature> signatureCache =
            Collections.synchronizedMap(
                    new LinkedHashMap<Path, CachedSignature>(
                            256, 0.75f, true
                    ) {
                        @Override
                        protected boolean removeEldestEntry(
                                Map.Entry<Path, CachedSignature> eldest
                        ) {
                            return size() > MAX_CACHE_ENTRIES;
                        }
                    }
            );

    // Compatibility with existing calls
    public List<SimilarImagePair> findSimilarImages(
            List<FileItem> items,
            int maxDistance
    ) {
        return findSimilarImages(items, maxDistance, 0.20);
    }

    // New method with configurable thresholds
    public List<SimilarImagePair> findSimilarImages(
            List<FileItem> items,
            int maxDistance,
            double maxDetailDifference
    ) {
        return findSimilarImages(
                items,
                maxDistance,
                maxDetailDifference,
                () -> false
        );
    }

    public List<SimilarImagePair> findSimilarImages(
        List<FileItem> items,
        int maxDistance,
        double maxDetailDifference,
        BooleanSupplier cancelled
    ) {
        return findSimilarImages(
                items,
                maxDistance,
                maxDetailDifference,
                cancelled,
                (processed, total) -> {}
        );
    }

    public List<SimilarImagePair> findSimilarImages(
            List<FileItem> items,
            int maxDistance,
            double maxDetailDifference,
            BooleanSupplier cancelled,
            BiConsumer<Integer, Integer> progress
    ) {

        if (!Double.isFinite(maxDetailDifference)
                || maxDetailDifference < 0
                || maxDetailDifference > 1) {

            throw new IllegalArgumentException(
                    "maxDetailDifference must be between 0 and 1"
            );
        }

        if (maxDistance < 0 || maxDistance > 64) {
            throw new IllegalArgumentException(
                    "maxDistance must be between 0 and 64"
            );
        }

        long analysisStart = System.nanoTime();

        List<HashedImage> hashedImages = new ArrayList<>();

        int totalImages = (int) items.stream()
                .filter(item ->
                        !item.isDirectory()
                        && item.getType() == FileType.IMAGE
                )
                .count();

        int processedImages = 0;

        progress.accept(0, totalImages);

        for (FileItem item : items) {

            if (item.isDirectory()
                    || item.getType() != FileType.IMAGE) {
                continue;
            }

            if (cancelled.getAsBoolean()) {
                throw new CancellationException(
                        "Image analysis cancelled."
                );
            }

            try {
                Path path = item.getPath()
                .toAbsolutePath()
                .normalize();

        BasicFileAttributes before = Files.readAttributes(
                path, BasicFileAttributes.class
        );

        CachedSignature cached = signatureCache.get(path);

        if (cached != null
                && cached.size() == before.size()
                && cached.lastModified().equals(before.lastModifiedTime())) {

            hashedImages.add(
                    new HashedImage(
                            item,
                            cached.hash(),
                            cached.detail()
                    )
            );

            continue;
        }

        // Cache miss: decode and calculate features
        BufferedImage image = ImageIO.read(path.toFile());

        if (image == null) {
            throw new IOException("Unsupported image: " + path);
        }

        long hash = hashService.calculateHash(image);
        int[] detail = detailService.createSignature(image);

        // Check if the file changed during analysis
        BasicFileAttributes after = Files.readAttributes(
                path, BasicFileAttributes.class
        );

        if (before.size() != after.size()
                || !before.lastModifiedTime().equals(after.lastModifiedTime())) {

            throw new IOException(
                    "Image changed during analysis: " + path
            );
        }

        signatureCache.put(
                path,
                new CachedSignature(
                        after.size(),
                        after.lastModifiedTime(),
                        hash,
                        detail
                )
        );

        hashedImages.add(
                new HashedImage(item, hash, detail)
        );

            } catch (IOException e) {
                System.err.println(
                        "Cannot read image: " + item.getPath()
                );
            } finally {

                processedImages++;

                progress.accept(
                        processedImages,
                        totalImages
                );
            }
        }
        
        long featureEnd = System.nanoTime();

        List<SimilarImagePair> results = new ArrayList<>();

        hashedImages.sort(
                Comparator.comparingInt(
                        image -> Long.bitCount(image.hash())
                )
        );

        for (int i = 0; i < hashedImages.size(); i++) {

            for (int j = i + 1; j < hashedImages.size(); j++) {

                if (cancelled.getAsBoolean()) {
                    throw new CancellationException(
                            "Image analysis cancelled."
                    );
                }

                HashedImage first = hashedImages.get(i);
                HashedImage second = hashedImages.get(j);

                int firstBits = Long.bitCount(first.hash());
                int secondBits = Long.bitCount(second.hash());

                if (secondBits - firstBits > maxDistance) {
                    break;
                }

                int distance = hashService.hammingDistance(
                        first.hash(),
                        second.hash()
                );

                if (distance <= maxDistance) {

                    double detailDifference = detailService.difference(
                            first.detail(),
                            second.detail()
                    );

                    if (detailDifference <= maxDetailDifference) {

                        results.add(
                                new SimilarImagePair(
                                        first.item(),
                                        second.item(),
                                        distance
                                )
                        );
                    }
                }
            }
        }

        long analysisEnd = System.nanoTime();

        double featureTime =
                (featureEnd - analysisStart) / 1_000_000_000.0;

        double comparisonTime =
                (analysisEnd - featureEnd) / 1_000_000_000.0;

        System.out.printf(
                "Similar Images Performance:%n"
                        + "Images processed: %d%n"
                        + "Feature calculation: %.3f seconds%n"
                        + "Image comparison: %.3f seconds%n"
                        + "Total time: %.3f seconds%n"
                        + "Matched pairs: %d%n",
                hashedImages.size(),
                featureTime,
                comparisonTime,
                featureTime + comparisonTime,
                results.size()
        );

        return results;
    }

    public List<SimilarImageGroup> groupSimilarPairs(
            List<SimilarImagePair> pairs
    ) {
        Map<Path, FileItem> files = new LinkedHashMap<>();
        Map<Path, Set<Path>> connections = new LinkedHashMap<>();

        // Build connections between similar images
        for (SimilarImagePair pair : pairs) {

            FileItem first = pair.first();
            FileItem second = pair.second();

            Path a = first.getPath().toAbsolutePath().normalize();
            Path b = second.getPath().toAbsolutePath().normalize();

            if (a.equals(b)) {
                continue;
            }

            files.putIfAbsent(a, first);
            files.putIfAbsent(b, second);

            connections.computeIfAbsent(
                    a, key -> new LinkedHashSet<>()
            ).add(b);

            connections.computeIfAbsent(
                    b, key -> new LinkedHashSet<>()
            ).add(a);
        }

        List<SimilarImageGroup> groups = new ArrayList<>();
        Set<Path> visited = new HashSet<>();

        // Find connected groups
        for (Path start : connections.keySet()) {

            if (!visited.add(start)) {
                continue;
            }

            ArrayDeque<Path> queue = new ArrayDeque<>();
            List<FileItem> groupFiles = new ArrayList<>();

            queue.add(start);

            while (!queue.isEmpty()) {

                Path current = queue.removeFirst();
                groupFiles.add(files.get(current));

                for (Path next : connections.getOrDefault(
                        current, Set.of()
                )) {
                    if (visited.add(next)) {
                        queue.addLast(next);
                    }
                }
            }

            if (groupFiles.size() >= 2) {
                groups.add(new SimilarImageGroup(groupFiles));
            }
        }

        return groups;
    }
}