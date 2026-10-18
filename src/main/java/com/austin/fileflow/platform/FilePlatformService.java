package com.austin.fileflow.platform;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Locale;

public class FilePlatformService {

    public void open(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException(
                "File does not exist: " + path
            );
        }

        if (!Desktop.isDesktopSupported()) {
            throw new IOException(
                "Desktop opreations are not suppoerted."
            );
        }

        Desktop.getDesktop().open(path.toFile());
    }

    public void showInFolder(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException(
                "File does not exist: " + path
            );
        }

        Path targetPath = path.toAbsolutePath().normalize();

        String operatingSystem = System
                .getProperty("os.name")
                .toLowerCase(Locale.ROOT);

        if (operatingSystem.contains("win")) {
            new ProcessBuilder(
                "explorer.exe",
                "/select,",
                targetPath.toString()
            ).start();

            return;
        }

        if (operatingSystem.contains("mac")) {
            new ProcessBuilder(
                "open",
                "-R",
                targetPath.toString()
            ).start();

            return;
        }

        throw new IOException(
            "Show in Folder is not supported on this operating system."
        );
    }

    public void copyPath(Path path) throws IOException {
        Path targetPath = path
                .toAbsolutePath()
                .normalize();

        StringSelection selection = 
            new StringSelection(targetPath.toString());

        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(selection, null);
        } catch (IllegalStateException e) {
            throw new IOException(
                "System clipboard is currently unavailable.",
                e
            );
        }
    }

    public void deleteFile(Path path) throws IOException {

        if (Files.isDirectory(path)) {
            throw new IOException(
                    "Folder deletion is not supported."
            );
        }

        Files.delete(path);
    }
    
}
