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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.myproject.helper.DirHelper;

public class DirectoryWatcher {
    private final Path DIRECTORY_PATH;
    private final WatchService watchService;
    private final HashMap<Path, Long> mapPathToCreationTime = new HashMap<>();
    private final EventHandler eventHandler;

    @SuppressWarnings(value = { "" })
    public DirectoryWatcher(Path DIRECTORY_PATH) throws IOException {
        this.eventHandler = new EventHandler(DIRECTORY_PATH);
        this.watchService = FileSystems.getDefault().newWatchService();
        this.DIRECTORY_PATH = DIRECTORY_PATH;
        watchAllDirectories();
    }

    /**
     * Registers a directory to be watched by the watchservice class
     * @param path The directory to be watched
     * */
    private void registerDirectory(Path path) {
        Thread thread = new Thread(() -> {
            try {
                if (path != null && DirHelper.checkDirExists(path) && DirHelper.isADir(path)) {
                    path.register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
                    analyzeEvents();
                }
            } catch (IOException e) {
                Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Failed to register directory!", e);
                closeWatchService();
            }
        });
        thread.start();
    }
    /**
     * Analyzes and handles the creation, delete and modify events that occur in a directory
     */
    private void analyzeEvents() {
        Thread thread = new Thread(() -> {

            try {
                boolean running = true;

                while (running) {
                    WatchKey key = this.watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> eventKind = event.kind();
                        Path affectedPath = (Path) event.context();
                        Path absolutePath = DIRECTORY_PATH.resolve(affectedPath);
                        long timestamp = System.currentTimeMillis();
                        //DirHelper.handleDirectoryDuplication(absolutePath, eventKind, timestamp, mapPathToCreationTime);

                        if(eventKind.equals(StandardWatchEventKinds.ENTRY_CREATE)){
                            eventHandler.handleFileCreation(absolutePath, timestamp);
                        }

                        if(eventKind.equals(StandardWatchEventKinds.ENTRY_MODIFY)){
                            eventHandler.handleFileModification(absolutePath, timestamp);
                        }

                        if(eventKind.equals(StandardWatchEventKinds.ENTRY_DELETE)){
                            eventHandler.handleFileDeletion(absolutePath, timestamp);
                        }

                    }

                    if (!key.reset()) {
                        System.out.println("Key invalid");
                        break;
                    }

                    running = !Thread.interrupted();
                }
            } catch (InterruptedException e) {
                Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Watch service interrupted!", e);
                Thread.currentThread().interrupt();
            } catch (ClosedWatchServiceException e) {
                Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Watch service closed!", e);
            }

        });

        thread.start();

    }

    public void watchAllDirectories() {
        //Recursively walks through all the subdirectories in a directory
        try (Stream<Path> paths = Files.walk(this.DIRECTORY_PATH)) {
            List<Path> subDirectories = paths.filter(Files::isDirectory).distinct().sorted().toList();
            Path exemptedPath = DIRECTORY_PATH.resolve("versions");

            //Register the directory if the path is not part of the version folder
            for(Path validPaths: subDirectories){
                if(!validPaths.startsWith(exemptedPath)){
                    registerDirectory(validPaths);
                }
            }

        } catch (IOException e) {
            Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Failed to walk directories!", e);
        } catch (SecurityException e) {
            Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Security exception!", e);
        }
    }

    private void closeWatchService() {
        try {
            this.watchService.close();
        } catch (IOException e) {
            Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Failed to close watch service!", e);
        }
    }

}