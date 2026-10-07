package com.austin.fileflow.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

public class FileItem {
    
    private final Path path;
    private final String name;
    private final String extension;
    private final long sizeBytes;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastModified;
    private final FileType type;
    private final boolean directory;

    private boolean favourite;
    private final Set<String> tags;

    public FileItem(
        Path path,
        String name,
        String extension,
        long sizeBytes,
        LocalDateTime createdAt,
        LocalDateTime lastModified,
        FileType type,
        boolean directory,
        boolean favourite,
        Set<String> tags
    ) {
        this.path = path;
        this.name = name;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
        this.type = type;
        this.directory = directory;
        this.favourite = favourite;
        this.tags = tags == null
            ? new HashSet<>()
            : new HashSet<>(tags);
    }

    public Path getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public FileType getType() {
        return type;
    }

    public boolean isDirectory() {
        return directory;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
}


