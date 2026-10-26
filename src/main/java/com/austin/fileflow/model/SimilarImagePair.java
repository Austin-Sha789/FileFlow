package com.austin.fileflow.model;

public record SimilarImagePair(
        FileItem first,
        FileItem second,
        int distance
) {
}