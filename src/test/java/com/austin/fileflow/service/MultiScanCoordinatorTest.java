
package com.austin.fileflow.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiScanCoordinatorTest {

    @TempDir
    Path tempDir;

    @Test
    void scansIndependentFoldersSequentially() {

        Path first = tempDir.resolve("University");
        Path second = tempDir.resolve("Projects");

        List<MultiScanCoordinator.ScanProgress> updates =
                new ArrayList<>();

        AtomicInteger calls = new AtomicInteger();

        var report = MultiScanCoordinator.scanRoots(
                List.of(first, second),

                (root, cancelled, onProgress) -> {
                    calls.incrementAndGet();
                    onProgress.accept(10);
                    return root.getFileName().toString();
                },

                () -> false,
                updates::add
        );

        assertEquals(2, calls.get());
        assertEquals(2, report.completed().size());
        assertTrue(report.failures().isEmpty());
        assertFalse(report.cancelled());

        assertEquals(first, report.completed().get(0).root());
        assertEquals(second, report.completed().get(1).root());

        assertEquals(1, updates.get(0).folderIndex());
        assertEquals(2, updates.get(1).folderIndex());
        assertEquals(2, updates.get(0).totalFolders());
        assertEquals(10, updates.get(0).processedItems());
    }

    @Test
    void avoidsOverlappingFolders() {

        Path photos = tempDir.resolve("Photos");
        Path child = photos.resolve("2026");

        AtomicInteger calls = new AtomicInteger();

        var report = MultiScanCoordinator.scanRoots(
                List.of(photos, child),

                (root, cancelled, progress) -> {
                    calls.incrementAndGet();
                    return root.toString();
                },

                () -> false,
                progress -> {}
        );

        assertEquals(1, calls.get());
        assertEquals(1, report.completed().size());
        assertEquals(photos, report.completed().get(0).root());
    }

    @Test
    void continuesAfterOneFolderFails() {

        Path first = tempDir.resolve("Broken");
        Path second = tempDir.resolve("Working");

        var report = MultiScanCoordinator.scanRoots(
                List.of(first, second),

                (root, cancelled, progress) -> {

                    if (root.equals(first)) {
                        throw new IOException("Access denied");
                    }

                    return "Success";
                },

                () -> false,
                progress -> {}
        );

        assertEquals(1, report.failures().size());
        assertEquals(first, report.failures().get(0).root());

        assertEquals(1, report.completed().size());
        assertEquals(second, report.completed().get(0).root());

        assertFalse(report.cancelled());
    }

    @Test
    void cancelBeforeStartingScansNothing() {

        AtomicInteger calls = new AtomicInteger();

        var report = MultiScanCoordinator.scanRoots(
                List.of(tempDir.resolve("Photos")),

                (root, cancelled, progress) -> {
                    calls.incrementAndGet();
                    return "Done";
                },

                () -> true,
                progress -> {}
        );

        assertTrue(report.cancelled());
        assertTrue(report.completed().isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    void cancellationKeepsEarlierCompletedFolders() {

        Path first = tempDir.resolve("University");
        Path second = tempDir.resolve("Photos");
        Path third = tempDir.resolve("Projects");

        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicInteger calls = new AtomicInteger();

        var report = MultiScanCoordinator.scanRoots(
                List.of(first, second, third),

                (root, isCancelled, progress) -> {

                    calls.incrementAndGet();

                    if (root.equals(second)) {
                        cancelled.set(true);
                        return "Incomplete";
                    }

                    return "Complete";
                },

                cancelled::get,
                progress -> {}
        );

        assertTrue(report.cancelled());

        // The first successful scan is preserved
        assertEquals(1, report.completed().size());
        assertEquals(first, report.completed().get(0).root());

        // The third folder was never started
        assertEquals(2, calls.get());
    }
}
