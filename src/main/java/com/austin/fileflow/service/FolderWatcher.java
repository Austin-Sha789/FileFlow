package com.austin.fileflow.service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.StandardWatchEventKinds.*;

public class FolderWatcher implements AutoCloseable {

    private WatchService watchService;
    private Thread watchThread;

    private void registerAll(Path root, WatchService watcher)
            throws IOException {

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(
                    Path dir,
                    BasicFileAttributes attrs
            ) throws IOException {

                dir.register(
                        watcher,
                        ENTRY_CREATE,
                        ENTRY_DELETE,
                        ENTRY_MODIFY
                );

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void start(Path folder, Runnable onChange)
            throws IOException {

        close();

        watchService =
                FileSystems.getDefault().newWatchService();

        registerAll(folder, watchService);

        WatchService activeWatcher = watchService;

        watchThread = new Thread(() -> {

            try {

                while (!Thread.currentThread().isInterrupted()) {

                    WatchKey key = activeWatcher.take();

                    boolean changed = false;

                    Path watchedDir = (Path) key.watchable();

                    for (WatchEvent<?> event : key.pollEvents()) {

                        changed = true;

                        if (event.kind() == ENTRY_CREATE) {

                            Path createdPath = watchedDir.resolve(
                                    (Path) event.context()
                            );

                            if (Files.isDirectory(createdPath)) {

                                try {
                                    registerAll(createdPath, activeWatcher);

                                } catch (IOException exception) {
                                    exception.printStackTrace();
                                }
                            }
                        }
                    }

                    boolean valid = key.reset();

                    if (changed) {
                        onChange.run();
                    }

                    if (!valid) {
                        break;
                    }
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            } catch (ClosedWatchServiceException e) {

                // Watcher was stopped.
            }

        }, "fileflow-folder-watcher");

        watchThread.setDaemon(true);
        watchThread.start();
    }

    @Override
    public void close() throws IOException {

        if (watchService != null) {
            watchService.close();
            watchService = null;
        }

        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
    }
}