
package com.austin.fileflow.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public final class MultiScanCoordinator {

    private MultiScanCoordinator() {
        // Utility class
    }

    @FunctionalInterface
    public interface RootScanner<T> {

        T scan(
                Path root,
                BooleanSupplier cancelled,
                LongConsumer onItemsProcessed
        ) throws Exception;
    }

    // Progress for the currently scanned folder
    public record ScanProgress(
            Path root,
            int folderIndex,
            int totalFolders,
            long processedItems
    ) {}

    // A successfully completed folder
    public record CompletedRoot<T>(
            Path root,
            T result
    ) {}

    // A folder that failed to scan
    public record FailedRoot(
            Path root,
            Exception error
    ) {}

    // Final outcome, including partial success
    public record ScanReport<T>(
            List<CompletedRoot<T>> completed,
            List<FailedRoot> failures,
            boolean cancelled
    ) {
        public ScanReport {
            completed = List.copyOf(completed);
            failures = List.copyOf(failures);
        }
    }

    public static <T> ScanReport<T> scanRoots(
            Collection<Path> selectedRoots,
            RootScanner<T> scanner,
            BooleanSupplier cancelled,
            Consumer<ScanProgress> onProgress
    ) {

        Objects.requireNonNull(scanner);
        Objects.requireNonNull(cancelled);
        Objects.requireNonNull(onProgress);

        // Reuse the planner created in Commit #1
        List<Path> roots = ScanRootPlanner.plan(selectedRoots);

        List<CompletedRoot<T>> completed = new ArrayList<>();
        List<FailedRoot> failures = new ArrayList<>();

        int totalFolders = roots.size();

        for (int i = 0; i < totalFolders; i++) {

            // Do not start another folder after cancellation
            if (cancelled.getAsBoolean()
                    || Thread.currentThread().isInterrupted()) {

                return new ScanReport<>(
                        completed,
                        failures,
                        true
                );
            }

            Path root = roots.get(i);
            int folderIndex = i + 1;

            try {

                T result = scanner.scan(
                        root,
                        cancelled,
                        processedItems -> onProgress.accept(
                                new ScanProgress(
                                        root,
                                        folderIndex,
                                        totalFolders,
                                        processedItems
                                )
                        )
                );

                // Do not publish an incomplete scan result
                if (cancelled.getAsBoolean()
                        || Thread.currentThread().isInterrupted()) {

                    return new ScanReport<>(
                            completed,
                            failures,
                            true
                    );
                }

                completed.add(
                        new CompletedRoot<>(
                                root,
                                Objects.requireNonNull(
                                        result,
                                        "Scanner returned null"
                                )
                        )
                );

            } catch (CancellationException exception) {

                return new ScanReport<>(
                        completed,
                        failures,
                        true
                );

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

                return new ScanReport<>(
                        completed,
                        failures,
                        true
                );

            } catch (Exception exception) {

                if (cancelled.getAsBoolean()
                        || Thread.currentThread().isInterrupted()) {

                    return new ScanReport<>(
                            completed,
                            failures,
                            true
                    );
                }

                // Record the failure and continue
                failures.add(
                        new FailedRoot(root, exception)
                );
            }
        }

        return new ScanReport<>(
                completed,
                failures,
                false
        );
    }
}
