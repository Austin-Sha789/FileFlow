
package com.austin.fileflow.service;

import com.austin.fileflow.model.ScanResult;
import com.austin.fileflow.model.WorkspaceScanSnapshot;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class WorkspaceScanService {

    private final FileScanService fileScanService;

    public WorkspaceScanService(
            FileScanService fileScanService
    ) {
        this.fileScanService = Objects.requireNonNull(
                fileScanService
        );
    }

    public WorkspaceScanSnapshot scan(
            ScanWorkspace workspace,
            BooleanSupplier cancelled,
            Consumer<MultiScanCoordinator.ScanProgress> onProgress
    ) {

        Objects.requireNonNull(workspace);

        // Capture the workspace at the start of the scan.
        var roots = workspace.getRoots();

        MultiScanCoordinator.ScanReport<ScanResult> report =
                MultiScanCoordinator.<ScanResult>scanRoots(
                        roots,

                        (root, cancellationSignal, itemProgress) ->
                                fileScanService.scan(
                                        root,
                                        cancellationSignal,
                                        count -> itemProgress.accept(count)
                                ),

                        cancelled,
                        onProgress
                );

        return WorkspaceScanSnapshot.from(report);
    }
}
