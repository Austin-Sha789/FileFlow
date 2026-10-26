package com.austin.fileflow.model;

import java.util.List;

public record SimilarImageGroup(
        List<FileItem> files
) {

    public SimilarImageGroup {

        files = List.copyOf(files);

        if (files.size() < 2) {
            throw new IllegalArgumentException(
                    "A similar image group requires at least two files."
            );
        }
    }
}