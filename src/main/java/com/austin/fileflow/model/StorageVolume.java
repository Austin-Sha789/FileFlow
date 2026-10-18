package com.austin.fileflow.model;

import java.nio.file.Path;

public class StorageVolume {
    
    private final Path rootPath;
    private final String name;
    private final long totalBytes;
    private final long  freeBytes;

    public StorageVolume(
        Path rootPath,
        String name,
        long totalBytes,
        long freeBytes
    ) {
        this.rootPath = rootPath;
        this.name = name;
        this.totalBytes = totalBytes;
        this.freeBytes = freeBytes;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public String getName() {
        return name;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getFreeBytes() {
        return freeBytes;
    }

    public long getUsedBytes() {
        return Math.max(
            0,
            totalBytes - freeBytes
        );
    }

    public double getUsageRatio() {
        if (totalBytes <= 0) {
            return 0;
        }

        return (double) getUsedBytes() / totalBytes;
    }
}
