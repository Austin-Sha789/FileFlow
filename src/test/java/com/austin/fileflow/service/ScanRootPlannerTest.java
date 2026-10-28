
package com.austin.fileflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScanRootPlannerTest {

    @TempDir
    Path tempDir;

    @Test
    void removesNestedFolder() {

        Path photos = tempDir.resolve("Photos");
        Path child = photos.resolve("2026");

        List<Path> result = ScanRootPlanner.plan(
                List.of(photos, child)
        );

        assertEquals(List.of(photos), result);
    }

    @Test
    void removesChildWhenParentIsAddedLater() {

        Path photos = tempDir.resolve("Photos");
        Path child = photos.resolve("2026");

        List<Path> result = ScanRootPlanner.plan(
                List.of(child, photos)
        );

        assertEquals(List.of(photos), result);
    }

    @Test
    void keepsIndependentFolders() {

        Path photos = tempDir.resolve("Photos");
        Path projects = tempDir.resolve("Projects");

        List<Path> result = ScanRootPlanner.plan(
                List.of(photos, projects)
        );

        assertEquals(
                List.of(photos, projects),
                result
        );
    }

    @Test
    void removesDuplicatePaths() {

        Path photos = tempDir.resolve("Photos");

        List<Path> result = ScanRootPlanner.plan(
                List.of(photos, photos.resolve("."))
        );

        assertEquals(List.of(photos), result);
    }

    @Test
    void acceptsEmptySelection() {

        assertEquals(
                List.of(),
                ScanRootPlanner.plan(List.of())
        );
    }
}
