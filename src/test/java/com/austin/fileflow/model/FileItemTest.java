package com.austin.fileflow.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

public class FileItemTest {

    @Test
    void constructorStoresFileMetadata() {
        Path path = Path.of("C:/User/Austin/Documents/report.pdf");
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 10,30);
        LocalDateTime lastModified = LocalDateTime.of(2026, 10, 2, 15, 45);

        FileItem item = new FileItem(
            path,
            "report.pdf",
            "pdf",
            2048L,
            createdAt,
            lastModified,
            FileType.DOCUMENT,
            false,
            false,
            Set.of("Work")
        );

        assertEquals(path, item.getPath());
        assertEquals("report.pdf", item.getName());
        assertEquals("pdf", item.getExtension());
        assertEquals(2048L, item.getSizeBytes());
        assertEquals(createdAt, item.getCreatedAt());
        assertEquals(lastModified, item.getLastModified());
        assertEquals(FileType.DOCUMENT, item.getType());
        assertFalse(item.isDirectory());
        assertFalse(item.isFavourite());
        assertEquals(Set.of("Work"), item.getTags());
    }

    @Test 
    void favouriteCanBeUpdated() {
        FileItem item = new FileItem(
            Path.of("C:/User/Austin/Documents/report.pdf"),
            "report.pdf",
            "pdf",
            2048L,
            LocalDateTime.now(),
            LocalDateTime.now(),
            FileType.DOCUMENT,
            false,
            false,
            Set.of()
        );

        assertFalse(item.isFavourite());
        item.setFavourite(true);
        assertTrue(item.isFavourite());
    }

    @Test 
    void tagsCanBeAddedAndRemoved() {
        FileItem item = new FileItem(
            Path.of("C:/User/Austin/Documents/report.pdf"),
            "report.pdf",
            "pdf",
            2048L,
            LocalDateTime.now(),
            LocalDateTime.now(),
            FileType.DOCUMENT,
            false,
            false,
            Set.of()
        );

        item.addTag("Work");
        assertTrue(item.getTags().contains("Work"));

        item.removeTag("Work");
        assertFalse(item.getTags().contains("Work"));
    }

    @Test 
    void constructorCopiesTagsToProtectInternalState() {
        Set<String> originalTags = new HashSet<>();
        originalTags.add("Work");

        FileItem item = new FileItem(
            Path.of("C:/User/Austin/Documents/report.pdf"),
            "report.pdf",
            "pdf",
            2048L,
            LocalDateTime.now(),
            LocalDateTime.now(),
            FileType.DOCUMENT,
            false,
            false,
            originalTags
        );

        originalTags.add("Important");

        assertTrue(item.getTags().contains("Work"));
        assertFalse(item.getTags().contains("Important"));
    }

    @Test 
    void returnedTagsCannotModifyInternalState() {
        FileItem item = new FileItem(
            Path.of("C:/User/Austin/Documents/report.pdf"),
            "report.pdf",
            "pdf",
            2048L,
            LocalDateTime.now(),
            LocalDateTime.now(),
            FileType.DOCUMENT,
            false,
            false,
            Set.of("Work")
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> item.getTags().add("Important")
        );
    }
}