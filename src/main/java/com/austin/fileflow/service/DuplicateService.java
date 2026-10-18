package com.austin.fileflow.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.BiConsumer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;

import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.austin.fileflow.model.DuplicateGroup;
import com.austin.fileflow.model.FileItem;

public class DuplicateService {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateService.class);

    public Map<Long, List<FileItem>> findSizeCandidates(List<FileItem> items) {
        Map<Long, List<FileItem>> filesBySize = new HashMap<>();

        for (FileItem item : items) {
            if (item.isDirectory()) {
                continue;
            }

            if (item.getSizeBytes() == 0) {
                continue;
            }

            filesBySize
                    .computeIfAbsent(
                        item.getSizeBytes(),
                        size -> new ArrayList<>()
                    )
                    .add(item);
        }

        filesBySize.entrySet().removeIf(
            entry -> entry.getValue().size() < 2
        );

        return filesBySize;
    }

    private String calculateHash(Path path, BooleanSupplier isCancelled) throws IOException {
        MessageDigest digest;

        try{
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 is not available",
                exception
            );
        }

        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {

                if (isCancelled.getAsBoolean()) {
                    throw new CancellationException("Duplicate scan cancelled");
                }

                digest.update(
                    buffer,
                    0,
                    bytesRead
                );
            }
        }

        return HexFormat.of().formatHex(
            digest.digest()
        );
    }

    public List<DuplicateGroup> findDuplicates(List<FileItem> items) {
        return findDuplicates(items, () -> false, (processed, total) -> {});
    }

    public List<DuplicateGroup> findDuplicates(List<FileItem> items, BooleanSupplier isCancelled) {
        return findDuplicates(items, isCancelled, (processed, total) -> {});
    }
    
    public List<DuplicateGroup> findDuplicates(
            List<FileItem> items, 
            BooleanSupplier isCancelled,
            BiConsumer<Integer, Integer> onProgress
    ) {

        Map<Long, List<FileItem>> sizeCandidates = findSizeCandidates(items);

        int totalCandidates = sizeCandidates.values()
                .stream()
                .mapToInt(List::size)
                .sum();

        int processed = 0;

        onProgress.accept(processed, totalCandidates);

        List<DuplicateGroup> duplicateGroups = new ArrayList<>();

        for (Map.Entry<Long, List<FileItem>> sizeEntry : sizeCandidates.entrySet()) {

            if (isCancelled.getAsBoolean()) {
                throw new CancellationException("Duplicate scan cancelled");
            }

            long fileSize = sizeEntry.getKey();

            Map<String, List<FileItem>> filesByHash = new HashMap<>();

            for (FileItem item : sizeEntry.getValue()) {

                if (isCancelled.getAsBoolean()){
                    throw new CancellationException("Duplicate scan cancelled");
                }

                try {
                    String hash = calculateHash(item.getPath(), isCancelled);

                    filesByHash.computeIfAbsent(hash, key -> new ArrayList<>()).add(item);
                } catch (IOException exception) {
                    logger.warn(
                        "Could not hash file: {}",
                        item.getPath(),
                        exception
                    );
                }

                processed++;

                onProgress.accept(
                    processed,
                    totalCandidates
                );
            }

            for (List<FileItem> hashGroup : filesByHash.values()) {
                if (hashGroup.size() < 2){
                    continue;
                }

                duplicateGroups.add(
                    new DuplicateGroup(hashGroup, fileSize)
                );
            }
        }

        return duplicateGroups;
    }
}
