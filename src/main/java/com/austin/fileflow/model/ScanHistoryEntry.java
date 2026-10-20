package com.austin.fileflow.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class ScanHistoryEntry {
    
    private  final long id;
    private final Path folderPath;
    private final LocalDateTime scannedAt;
    private final long fileCount;
    private final long totalBytes;

    public ScanHistoryEntry(
        long id,
        Path folderPath,
        LocalDateTime scannedAt,
        long fileCount,
        long totalBytes
    ){

        this.id = id;
        this.folderPath = folderPath;
        this.scannedAt = scannedAt;
        this.fileCount = fileCount;
        this.totalBytes = totalBytes;
    }

    public long getId() {
        return id;
    }

    public Path getFolderPath() {
        return folderPath;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public long getFileCount() {
        return fileCount;
    }

    public long getTotalBytes() {
        return totalBytes;
    }
}
