package com.austin.fileflow.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DuplicateGroupTest {
    
    @Test 
    void calculatesReclaimableBytes() {
        long fileSize = 100L;

        FileItem first = new FileItem(
            Path.of("first.txt"),
            "first.text",
            "txt",
            fileSize,
            LocalDateTime.now(),
            LocalDateTime.now(),
            FileType.DOCUMENT,
            false,
            false,
            Set.of()
        );

        FileItem second = new FileItem(
                Path.of("second.txt"),
                "second.txt",
                "txt",
                fileSize,
                LocalDateTime.now(),
                LocalDateTime.now(),
                FileType.DOCUMENT,
                false,
                false,
                Set.of()
        );

        FileItem third = new FileItem(
                Path.of("third.txt"),
                "third.txt",
                "txt",
                fileSize,
                LocalDateTime.now(),
                LocalDateTime.now(),
                FileType.DOCUMENT,
                false,
                false,
                Set.of()
        );

        DuplicateGroup group = new DuplicateGroup(List.of(first, second, third), fileSize);

        assertEquals(200L, group.getReclaimableBytes());
    }
}
