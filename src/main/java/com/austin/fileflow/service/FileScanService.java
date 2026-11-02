package com.austin.fileflow.service;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.austin.fileflow.model.ScanIssue;
import com.austin.fileflow.model.ScanResult;
import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.FileType;

public class FileScanService {
    public ScanResult scan(Path rootPath) throws IOException {
        return scan(rootPath, () -> false);
    } 

    public ScanResult scan(
            Path rootPath,
            BooleanSupplier isCancelled
    ) throws IOException {
        return scan(rootPath, isCancelled, count -> {});
    }

    public ScanResult scan(
        Path rootPath,
        BooleanSupplier isCancelled,
        IntConsumer onItemScanned
    ) throws IOException {

        List<FileItem> items = new ArrayList<>();
        List<ScanIssue> issues = new ArrayList<>();

        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            
            @Override 
            public FileVisitResult preVisitDirectory(
                    Path dir,
                    BasicFileAttributes attrs
            ) {
                if (isCancelled.getAsBoolean()) {
                    return FileVisitResult.TERMINATE;
                }

                try {
                    items.add(createFileItem(dir));
                    onItemScanned.accept(items.size());
                } catch (IOException e) {
                    issues.add(new ScanIssue(dir, e.getMessage()));
                }

                return FileVisitResult.CONTINUE;
            }

            @Override 
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs) {

                if (isCancelled.getAsBoolean()) {
                    return FileVisitResult.TERMINATE;
                }

                try {
                    items.add(createFileItem(file));
                    onItemScanned.accept(items.size());
                } catch (IOException e) {
                    issues.add(new ScanIssue(file, e.getMessage()));
                }
            
                return FileVisitResult.CONTINUE;
            }

            @Override 
            public FileVisitResult visitFileFailed(
                    Path file,
                    IOException exc) {

                if (isCancelled.getAsBoolean()) {
                    return FileVisitResult.TERMINATE;
                }

                issues.add(new ScanIssue(file, exc.getMessage()));

                return FileVisitResult.CONTINUE;
            }
        });

        return new ScanResult(items, issues);
    }

    /** Scan only the chosen files/folders; never broaden a file selection to its parent. */
    public ScanResult scanSelected(List<Path> selected, BooleanSupplier isCancelled,
                                   IntConsumer onItemScanned) {
        var items = new java.util.LinkedHashMap<Path, FileItem>();
        List<ScanIssue> issues = new ArrayList<>();
        for (Path path : ScanRootPlanner.plan(selected)) {
            if (isCancelled.getAsBoolean()) break;
            int previousCount = items.size();
            try {
                ScanResult result = scan(path, isCancelled, count -> onItemScanned.accept(previousCount + count));
                for (FileItem item : result.getItems()) items.putIfAbsent(item.getPath().toAbsolutePath().normalize(), item);
                issues.addAll(result.getIssues());
            } catch (IOException failure) {
                issues.add(new ScanIssue(path, failure.getMessage()));
            }
        }
        return new ScanResult(new ArrayList<>(items.values()), issues);
    }

    private FileItem createFileItem(Path path) throws IOException {
        BasicFileAttributes attributes = 
            Files.readAttributes(path, BasicFileAttributes.class);

            Path fileName = path.getFileName();

            String name = fileName != null
                    ? fileName.toString()
                    : path.toString();

            long sizeBytes = attributes.size();
            String extension = getExtension(name);

            LocalDateTime createdAt = LocalDateTime.ofInstant(
                attributes.creationTime().toInstant(),
                ZoneId.systemDefault()
            );
            
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                attributes.lastModifiedTime().toInstant(),
                ZoneId.systemDefault()
            );

            boolean directory = attributes.isDirectory();

            FileType type = determineFileType(extension, directory);

            return new FileItem (
                path,
                name,
                extension,
                sizeBytes,
                createdAt,
                lastModified,
                type,
                directory,
                false,
                Set.of()
            );
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private FileType determineFileType(String extension, boolean directory) {
        if (directory) {
            return FileType.OTHER;
        }

        if (VideoThumbnailDecoder.supportsExtension(extension)) return FileType.VIDEO;
        return switch (extension) {
            case "jpg", "jpeg", "png", "gif", "webp" ->
                FileType.IMAGE;
            
            case "mp3", "wav", "flac", "aac" ->
                FileType.AUDIO;

            case "pdf", "doc", "docx", "txt", "rtf" ->
                FileType.DOCUMENT;

            case "zip", "rar", "tar", "gz", "7z" ->
                FileType.ARCHIVE;

            case "java", "py", "js", "ts", "cs", "cpp", "c", "html", "css" ->
                FileType.CODE;

            default ->
                FileType.OTHER;
        };

    }
}
