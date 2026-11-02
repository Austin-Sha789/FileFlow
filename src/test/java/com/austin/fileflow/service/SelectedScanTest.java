package com.austin.fileflow.service;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SelectedScanTest {
    @TempDir Path root;
    final FileScanService scanner = new FileScanService();
    @Test void selectedFilesDoNotIncludeUnselectedSiblings() throws Exception {
        Path first = Files.writeString(root.resolve("first.txt"), "first");
        Path second = Files.writeString(root.resolve("second.txt"), "second");
        Files.writeString(root.resolve("private.txt"), "not selected");
        var result = scanner.scanSelected(List.of(first, second), () -> false, count -> {});
        assertEquals(Set.of(first, second), result.getItems().stream().map(item -> item.getPath()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(result.getIssues().isEmpty());
    }
    @Test void overlappingFolderAndFileSelectionsAreCountedOnce() throws Exception {
        Path folder = Files.createDirectory(root.resolve("photos"));
        Path image = Files.writeString(folder.resolve("one.png"), "image");
        AtomicInteger progress = new AtomicInteger();
        var result = scanner.scanSelected(List.of(image, folder, folder), () -> false, progress::set);
        assertEquals(2, result.getItems().size());
        assertEquals(1, result.getItems().stream().filter(item -> item.getPath().equals(image)).count());
        assertEquals(2, progress.get());
    }
    @Test void missingSelectionDoesNotDiscardReadableFiles() throws Exception {
        Path file = Files.writeString(root.resolve("good.txt"), "data");
        var result = scanner.scanSelected(List.of(root.resolve("missing"), file), () -> false, count -> {});
        assertEquals(1, result.getItems().size());
        assertEquals(1, result.getIssues().size());
    }
    @Test void cancellationStopsBeforeOpeningTheNextSelection() throws Exception {
        Path file = Files.writeString(root.resolve("good.txt"), "data");
        var result = scanner.scanSelected(List.of(file), () -> true, count -> fail("No progress expected"));
        assertTrue(result.getItems().isEmpty());
        assertTrue(result.getIssues().isEmpty());
    }
}
