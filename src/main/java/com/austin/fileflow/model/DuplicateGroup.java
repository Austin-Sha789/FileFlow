package com.austin.fileflow.model;

import java.util.List;

public class DuplicateGroup {   
    private final List<FileItem> files;
    private final long fileSizeBytes;

    public DuplicateGroup(List<FileItem> files, long fileSizeBytes) {
        this.files = List.copyOf(files);
        this.fileSizeBytes =  fileSizeBytes;
    }

    public List<FileItem> getFiles() {
        return files;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public long getReclaimableBytes() {
        if (files.size() <= 1) {
            return 0;
        }

        return fileSizeBytes * (files.size() - 1);
    }


}
