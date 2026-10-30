
package com.austin.fileflow.model;

import com.austin.fileflow.service.MultiScanCoordinator;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceScanSnapshotTest {

    @Test void addingAndRescanningRootsPreservesOtherResultsAndFreshOverlapMetadata() {
        Path firstRoot = Path.of("Photos").toAbsolutePath();
        Path secondRoot = firstRoot.resolve("Trips");
        Path file = secondRoot.resolve("photo.jpg");
        var time = java.time.LocalDateTime.of(2026, 1, 1, 0, 0);
        FileItem old = new FileItem(file, "photo.jpg", "jpg", 10, time, time, FileType.IMAGE, false, false, java.util.Set.of());
        FileItem fresh = new FileItem(file, "photo.jpg", "jpg", 20, time, time, FileType.IMAGE, false, false, java.util.Set.of());
        var first = new ScanResult(List.of(old), List.of());
        var second = new ScanResult(List.of(fresh), List.of());
        var combined = WorkspaceScanSnapshot.withRoot(null, firstRoot, first, secondRoot, second);
        assertEquals(2, combined.completedFolderCount());
        assertSame(first, combined.resultsByRoot().get(firstRoot));
        assertSame(second, combined.resultsByRoot().get(secondRoot));
        assertEquals(1, combined.fileCount()); assertEquals(20, combined.totalFileBytes());
        var updated = new ScanResult(List.of(), List.of());
        var refreshed = WorkspaceScanSnapshot.withRoot(combined, secondRoot, second, secondRoot, updated);
        assertSame(first, refreshed.resultsByRoot().get(firstRoot));
        assertSame(updated, refreshed.resultsByRoot().get(secondRoot));
    }

    @Test
    void emptyReportProducesEmptySnapshot() {

        MultiScanCoordinator.ScanReport<ScanResult> report =
                new MultiScanCoordinator.ScanReport<>(
                        List.of(),
                        List.of(),
                        false
                );

        WorkspaceScanSnapshot snapshot =
                WorkspaceScanSnapshot.from(report);

        assertEquals(0, snapshot.completedFolderCount());
        assertEquals(0, snapshot.fileCount());
        assertEquals(0, snapshot.totalFileBytes());
        assertTrue(snapshot.items().isEmpty());
        assertTrue(snapshot.failures().isEmpty());
        assertFalse(snapshot.cancelled());
    }

    @Test
    void preservesFailedFolders() {

        Path brokenRoot = Path.of("broken-folder");

        var failure = new MultiScanCoordinator.FailedRoot(
                brokenRoot,
                new IOException("Access denied")
        );

        MultiScanCoordinator.ScanReport<ScanResult> report =
                new MultiScanCoordinator.ScanReport<>(
                        List.of(),
                        List.of(failure),
                        false
                );

        WorkspaceScanSnapshot snapshot =
                WorkspaceScanSnapshot.from(report);

        assertEquals(1, snapshot.failures().size());
        assertEquals(
                brokenRoot,
                snapshot.failures().get(0).root()
        );
    }

    @Test
    void preservesCancellationState() {

        MultiScanCoordinator.ScanReport<ScanResult> report =
                new MultiScanCoordinator.ScanReport<>(
                        List.of(),
                        List.of(),
                        true
                );

        WorkspaceScanSnapshot snapshot =
                WorkspaceScanSnapshot.from(report);

        assertTrue(snapshot.cancelled());
    }

    @Test
    void returnedCollectionsAreImmutable() {

        MultiScanCoordinator.ScanReport<ScanResult> report =
                new MultiScanCoordinator.ScanReport<>(
                        List.of(),
                        List.of(),
                        false
                );

        WorkspaceScanSnapshot snapshot =
                WorkspaceScanSnapshot.from(report);

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.items().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.resultsByRoot().clear()
        );
    }
}
