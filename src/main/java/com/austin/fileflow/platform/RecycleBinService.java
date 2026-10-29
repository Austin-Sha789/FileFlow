
package com.austin.fileflow.platform;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class RecycleBinService {

    public void moveToRecycleBin(Path path) throws IOException {

        if (path == null) {
            throw new IOException("File path cannot be null.");
        }

        Path target = path.toAbsolutePath().normalize();

        // Never move the root of a drive
        if (target.getParent() == null) {
            throw new IOException(
                    "Cannot move a drive root to the Recycle Bin."
            );
        }

        // The file or folder must exist
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "File or folder does not exist: " + target
            );
        }

        // Avoid unexpected symbolic link behaviour
        if (Files.isSymbolicLink(target)) {
            throw new IOException(
                    "Symbolic links are not supported: " + target
            );
        }

        // Only regular files and directories are supported
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                && !Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "Unsupported file type: " + target
            );
        }

        try {

            if (!Desktop.isDesktopSupported()) {
                throw new IOException(
                        "Desktop operations are not supported."
                );
            }

            Desktop desktop = Desktop.getDesktop();

            if (!desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) {
                throw new IOException(
                        "Recycle Bin is not supported on this system."
                );
            }

            boolean success = desktop.moveToTrash(target.toFile());

            if (!success) {
                throw new IOException(
                        "Unable to move item to Recycle Bin: " + target
                );
            }

        } catch (UnsupportedOperationException
                | SecurityException exception) {

            throw new IOException(
                    "Recycle Bin operation failed: " + target,
                    exception
            );
        }
    }
}
