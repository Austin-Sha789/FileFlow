
package com.austin.fileflow.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScanWorkspace {

    private final List<Path> roots = new ArrayList<>();

    // Add a folder to the workspace
    public boolean addRoot(Path path) {

        Path normalized = Objects.requireNonNull(
                path,
                "Scan root cannot be null"
        ).toAbsolutePath().normalize();

        List<Path> candidates = new ArrayList<>(roots);
        candidates.add(normalized);

        // Remove duplicates and overlapping folders
        List<Path> plannedRoots =
                ScanRootPlanner.plan(candidates);

        if (roots.equals(plannedRoots)) {
            return false;
        }

        roots.clear();
        roots.addAll(plannedRoots);

        return true;
    }

    // Remove one selected root
    public boolean removeRoot(Path path) {

        Path normalized = Objects.requireNonNull(
                path,
                "Scan root cannot be null"
        ).toAbsolutePath().normalize();

        return roots.remove(normalized);
    }

    // Remove all selected folders
    public void clear() {
        roots.clear();
    }

    // Return a read-only snapshot
    public List<Path> getRoots() {
        return List.copyOf(roots);
    }

    public int size() {
        return roots.size();
    }

    public boolean isEmpty() {
        return roots.isEmpty();
    }
}
