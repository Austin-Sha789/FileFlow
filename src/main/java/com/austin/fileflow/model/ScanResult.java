package com.austin.fileflow.model;

import java.util.List;

public class ScanResult {
    
    private final List<FileItem> items;
    private final List<ScanIssue> issues;

    public ScanResult(List<FileItem> items, List<ScanIssue> issues) {
        this.items = List.copyOf(items);
        this.issues = List.copyOf(issues);
    }

    public List<FileItem> getItems() {
        return items;
    }

    public List<ScanIssue> getIssues() {
        return issues;
    }
}
