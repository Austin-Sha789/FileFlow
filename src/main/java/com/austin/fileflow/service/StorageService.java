package com.austin.fileflow.service;

import com.austin.fileflow.model.StorageVolume;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.FileStore;

import java.util.ArrayList;
import java.util.List;

public class StorageService {
    public List<StorageVolume> getAvailableVolumes() {

        List<StorageVolume> volumes = new ArrayList<>();

        File[] roots = File.listRoots();

        if (roots == null) {
            return volumes;
        }

        for (File root : roots) {
            Path rootPath = root.toPath()
                                .toAbsolutePath()
                                .normalize();

            long totalBytes = root.getTotalSpace();

            long freeBytes = root.getUsableSpace();

            if (totalBytes <= 0) {
                continue;
            }

            String name = createVolumeName(rootPath);

            volumes.add(
                new StorageVolume(
                    rootPath,
                    name,
                    totalBytes,
                    freeBytes
                )
            );
        }

        return List.copyOf(volumes);
    }

    public String createVolumeName(Path rootPath) {

        String osName =
                System.getProperty("os.name")
                        .toLowerCase();

        if (osName.contains("win")) {

            String drive =
                    rootPath.toString()
                            .replace("\\", "");

            try {

                FileStore fileStore =
                        Files.getFileStore(rootPath);

                String volumeName =
                        fileStore.name();

                if (volumeName != null
                        && !volumeName.isBlank()) {

                    return volumeName
                            + " ("
                            + drive
                            + ")";
                }

            } catch (IOException exception) {

            }

            return "Local Disk ("
                    + drive
                    + ")";
        }

        if (osName.contains("mac")) {
            return "Macintosh HD";
        }

        return rootPath.toString();
    }

    public StorageVolume getVolumeForPath(Path path) {

        Path absolutePath =
                path.toAbsolutePath()
                        .normalize();

        for (StorageVolume volume
                : getAvailableVolumes()) {

            if (absolutePath.startsWith(
                    volume.getRootPath()
            )) {
                return volume;
            }
        }

        return null;
    }
}
