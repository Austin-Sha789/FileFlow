
package com.austin.fileflow.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class ScanRootPlanner {

    private ScanRootPlanner() {
        // Utility class
    }

    public static List<Path> plan(Collection<Path> selectedRoots) {

        Objects.requireNonNull(
                selectedRoots,
                "Selected roots cannot be null"
        );

        List<Path> roots = new ArrayList<>();

        for (Path selectedRoot : selectedRoots) {

            Path candidate = Objects.requireNonNull(
                    selectedRoot,
                    "A scan root cannot be null"
            ).toAbsolutePath().normalize();

            // Already covered by another root
            boolean alreadyCovered = roots.stream()
                    .anyMatch(candidate::startsWith);

            if (alreadyCovered) {
                continue;
            }

            // A parent directory replaces selected children
            roots.removeIf(existing -> existing.startsWith(candidate));

            roots.add(candidate);
        }

        return List.copyOf(roots);
    }
}
