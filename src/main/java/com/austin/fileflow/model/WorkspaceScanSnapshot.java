
package com.austin.fileflow.model;

import com.austin.fileflow.service.MultiScanCoordinator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record WorkspaceScanSnapshot(
        Map<Path, ScanResult> resultsByRoot,
        List<FileItem> items,
        List<MultiScanCoordinator.FailedRoot> failures,
        boolean cancelled
) {

    public WorkspaceScanSnapshot {

        resultsByRoot = Collections.unmodifiableMap(
                new LinkedHashMap<>(resultsByRoot)
        );

        items = List.copyOf(items);
        failures = List.copyOf(failures);
    }

    public static WorkspaceScanSnapshot from(
            MultiScanCoordinator.ScanReport<ScanResult> report
    ) {

        Objects.requireNonNull(report);

        Map<Path, ScanResult> results = new LinkedHashMap<>();
        Map<Path, FileItem> uniqueItems = new LinkedHashMap<>();

        for (var completed : report.completed()) {

            Path root = completed.root();
            ScanResult scanResult = completed.result();

            results.put(root, scanResult);

            for (FileItem item : scanResult.getItems()) {

                Path normalizedPath = item.getPath()
                        .toAbsolutePath()
                        .normalize();

                uniqueItems.putIfAbsent(
                        normalizedPath,
                        item
                );
            }
        }

        return new WorkspaceScanSnapshot(
                results,
                new ArrayList<>(uniqueItems.values()),
                report.failures(),
                report.cancelled()
        );
    }

    public static WorkspaceScanSnapshot mergeRefresh(
            WorkspaceScanSnapshot previous,
            WorkspaceScanSnapshot refreshed,
            List<Path> requestedRoots
    ) {

        Objects.requireNonNull(previous);
        Objects.requireNonNull(refreshed);
        Objects.requireNonNull(requestedRoots);

        List<MultiScanCoordinator.CompletedRoot<ScanResult>>
                mergedResults = new ArrayList<>();

        for (Path selectedRoot : requestedRoots) {

            Path root = selectedRoot
                    .toAbsolutePath()
                    .normalize();

            // Prefer freshly scanned data
            ScanResult result =
                    refreshed.resultsByRoot().get(root);

            // Keep previously successful data if this root failed
            if (result == null) {
                result = previous.resultsByRoot().get(root);
            }

            if (result != null) {
                mergedResults.add(
                        new MultiScanCoordinator.CompletedRoot<>(
                                root,
                                result
                        )
                );
            }
        }

        var mergedReport =
                new MultiScanCoordinator.ScanReport<>(
                        mergedResults,
                        refreshed.failures(),
                        refreshed.cancelled()
                );

        return WorkspaceScanSnapshot.from(mergedReport);
    }

    /** Called on the scan worker so aggregating large libraries does not block the application thread. */
    public static WorkspaceScanSnapshot withRoot(WorkspaceScanSnapshot previous, Path previousRoot,
            ScanResult previousResult, Path root, ScanResult result) {
        Map<Path, ScanResult> results = new LinkedHashMap<>();
        if (previous != null) results.putAll(previous.resultsByRoot());
        else if (previousRoot != null && previousResult != null) results.put(previousRoot.toAbsolutePath().normalize(), previousResult);
        results.put(root.toAbsolutePath().normalize(), result);
        Map<Path, FileItem> uniqueItems = new LinkedHashMap<>();
        for (ScanResult scan : results.values()) for (FileItem item : scan.getItems()) {
            uniqueItems.putIfAbsent(item.getPath().toAbsolutePath().normalize(), item);
        }
        // The freshly scanned root wins when a saved parent/child overlap in All Files.
        for (FileItem item : result.getItems()) uniqueItems.put(item.getPath().toAbsolutePath().normalize(), item);
        List<MultiScanCoordinator.FailedRoot> failures = previous == null ? List.of() : previous.failures().stream()
                .filter(failure -> !failure.root().toAbsolutePath().normalize().equals(root.toAbsolutePath().normalize())).toList();
        return new WorkspaceScanSnapshot(results, new ArrayList<>(uniqueItems.values()), failures, false);
    }

    public int completedFolderCount() {
        return resultsByRoot.size();
    }

    public long fileCount() {
        return items.stream()
                .filter(item -> !item.isDirectory())
                .count();
    }

    public long totalFileBytes() {
        return items.stream()
                .filter(item -> !item.isDirectory())
                .mapToLong(FileItem::getSizeBytes)
                .sum();
    }
}
