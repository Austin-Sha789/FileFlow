package com.austin.fileflow.model;


import java.nio.file.Path;

public class ScanIssue {
    
    private final Path path;
    private final String message;

    public ScanIssue(Path path, String message) {
        this.path = path;
        this.message = message;
    }

    public Path getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }
}
