
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
