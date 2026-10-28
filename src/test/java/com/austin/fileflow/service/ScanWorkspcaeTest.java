
package com.austin.fileflow.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScanWorkspaceTest {

    @TempDir
    Path tempDir;

    @Test
    void addsMultipleIndependentRoots() {

        ScanWorkspace workspace = new ScanWorkspace();

        Path photos = tempDir.resolve("Photos");
        Path projects = tempDir.resolve("Projects");

        assertTrue(workspace.addRoot(photos));
        assertTrue(workspace.addRoot(projects));

        assertEquals(2, workspace.size());
        assertEquals(
                List.of(photos, projects),
                workspace.getRoots()
        );
    }

    @Test
    void ignoresDuplicateRoot() {

        ScanWorkspace workspace = new ScanWorkspace();
        Path photos = tempDir.resolve("Photos");

        assertTrue(workspace.addRoot(photos));
        assertFalse(workspace.addRoot(photos.resolve(".")));

        assertEquals(1, workspace.size());
    }

    @Test
    void ignoresChildAlreadyCoveredByParent() {

        ScanWorkspace workspace = new ScanWorkspace();

        Path photos = tempDir.resolve("Photos");

        workspace.addRoot(photos);

        assertFalse(
                workspace.addRoot(photos.resolve("2026"))
        );

        assertEquals(List.of(photos), workspace.getRoots());
    }

    @Test
    void addingParentReplacesChild() {

        ScanWorkspace workspace = new ScanWorkspace();

        Path photos = tempDir.resolve("Photos");
        Path child = photos.resolve("2026");

        workspace.addRoot(child);
        workspace.addRoot(photos);

        assertEquals(List.of(photos), workspace.getRoots());
    }

    @Test
    void removesRoot() {

        ScanWorkspace workspace = new ScanWorkspace();

        Path photos = tempDir.resolve("Photos");
        Path projects = tempDir.resolve("Projects");

        workspace.addRoot(photos);
        workspace.addRoot(projects);

        assertTrue(workspace.removeRoot(photos));

        assertEquals(
                List.of(projects),
                workspace.getRoots()
        );
    }

    @Test
    void clearsWorkspace() {

        ScanWorkspace workspace = new ScanWorkspace();

        workspace.addRoot(tempDir.resolve("Photos"));
        workspace.addRoot(tempDir.resolve("Projects"));

        workspace.clear();

        assertTrue(workspace.isEmpty());
        assertEquals(0, workspace.size());
    }

    @Test
    void exposesImmutableSnapshot() {

        ScanWorkspace workspace = new ScanWorkspace();

        workspace.addRoot(tempDir.resolve("Photos"));

        List<Path> snapshot = workspace.getRoots();

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.add(tempDir.resolve("Other"))
        );
    }
}
