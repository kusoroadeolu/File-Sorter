package com.myproject.watcher;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.myproject.helper.DirHelper;

public class DirectoryWatcher {
    private final Path DIRECTORY_PATH;
    private final WatchService watchService;
    private final ConcurrentHashMap<Path, Long> mapPathToCreationTime = new ConcurrentHashMap<>();
    private final EventHandler eventHandler;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Set<Path> registeredDirectories = Collections.synchronizedSet(new HashSet<>());

    public DirectoryWatcher(Path DIRECTORY_PATH) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.eventHandler = new EventHandler(DIRECTORY_PATH, watchService);
        this.DIRECTORY_PATH = DIRECTORY_PATH;
        watchAllDirectories();
    }

    /**
     * Registers a directory to be watched by the watchservice class
     * @param path The directory to be watched
     * @see WatchService watchservice
     * @see ExecutorService executorService
     * */
    private void registerDirectory(Path path) {
        executorService.submit(() -> {
            try {
                if (path != null && DirHelper.checkDirExists(path) && DirHelper.isADir(path)) {
                    synchronized (registeredDirectories) {
                        if (!registeredDirectories.contains(path)) {
                            registeredDirectories.add(path);
                            path.register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE,
                                    StandardWatchEventKinds.ENTRY_DELETE,
                                    StandardWatchEventKinds.ENTRY_MODIFY);
                        }
                    }
                    analyzeEvents();
                }else{
                    throw new RuntimeException("Entered Path: " + DIRECTORY_PATH + " is not a directory");

                }
            } catch (IOException e) {
                Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Failed to register directory!", e);
                return;
            }
        });
    }

    /**
     * Analyzes and handles the creation, delete and modify events that occur in a directory
     */
    private void analyzeEvents() {
        executorService.submit(() -> {
            try {
                while (running.get()) {
                    WatchKey key = this.watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> eventKind = event.kind();
                        Path affectedPath = (Path) event.context();
                        Path absolutePath = DIRECTORY_PATH.resolve(affectedPath);
                        long timestamp = System.currentTimeMillis();

                        DirHelper.handleDirectoryDuplication(absolutePath, eventKind, timestamp, mapPathToCreationTime);

                        if (eventKind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                            eventHandler.handleFileCreation(absolutePath, timestamp);
                        } else if (eventKind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                            eventHandler.handleFileModification(absolutePath, timestamp);
                        } else if (eventKind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                            eventHandler.handleFileDeletion(absolutePath, timestamp);
                        }
                    }

                    if (!key.reset()) {
                        Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.WARNING, "WatchKey is no longer valid.");
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Watch service interrupted!", e);
                Thread.currentThread().interrupt();
            } catch (ClosedWatchServiceException e) {
                Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Watch service closed!", e);
                closeWatchService();
            }
        });
    }

    public void watchAllDirectories() {
        // Recursively walks through all the subdirectories in a directory
        try (Stream<Path> paths = Files.walk(this.DIRECTORY_PATH)) {
            List<Path> subDirectories = paths.filter(Files::isDirectory).distinct().sorted().toList();
            Path exemptedPath = DIRECTORY_PATH.resolve("versions");

            // Register the directory if the path is not part of the version folder
            subDirectories.stream()
                    .filter(validPaths -> !validPaths.startsWith(exemptedPath))
                    .forEach(this::registerDirectory);

        } catch (IOException e) {
            Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Failed to get directories!", e);
            closeWatchService();
        } catch (SecurityException e) {
            Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Security exception!", e);
            closeWatchService();
        }
    }

    private void closeWatchService() {
        running.set(false);
        executorService.shutdown();
        try {
            this.watchService.close();
        } catch (IOException e) {
            Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Failed to close watch service!", e);
        }
    }
}
