
package com.austin.fileflow.model;

import com.austin.fileflow.service.MultiScanCoordinator;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceScanSnapshotTest {

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
