package com.myproject.watcher;

import com.myproject.helper.DirHelper;
import com.myproject.helper.FileHelper;
import com.myproject.versioning.FileVersioner;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventHandler {
    private final FileVersioner fileVersioner;
    private final CopyOnWriteArrayList<Path> files;
    private final ConcurrentHashMap<Path, byte[]> mapPathToContent;
    private final Path DIRECTORY_PATH;
    private final WatchService watchService;

    public EventHandler(Path DIRECTORY_PATH, WatchService watchService) throws IOException {this.DIRECTORY_PATH = DIRECTORY_PATH;
        fileVersioner = new FileVersioner(DIRECTORY_PATH);
        this.watchService = watchService;
        this.mapPathToContent = fileVersioner.getMapFileToContent();
        files = FileHelper.getFiles(DIRECTORY_PATH);
    }

    /**
     * Handles events in the case where a file was created.
     * @param filePath Path to the file that was created -> Used to build the versioned filename.
     * @param timestamp TimeStamp at which the file was created -> Used to build the versioned filename.
     */
    public void handleFileCreation(Path filePath, long timestamp) {
        //Sets the number of times the file can be accessed
        final int MAX_RETRIES = 5;
        //The time the thread waits before trying to access the file again
        final long RETRY_DELAY_MS = 100;
        int attempts = 0;

        while (attempts < MAX_RETRIES) {
            try {
                Path absolutePath = filePath.toAbsolutePath();

                //Checks if the path is a folder
                if(DirHelper.isADir(absolutePath)){
                //TODO: Find a way to register newly created directories
                    return;
                }

                String fileName = absolutePath.getFileName().toString();

                byte[] fileBytes = Files.readAllBytes(absolutePath);

                String versionedFileName = FileHelper.buildVersionedFileName(this.DIRECTORY_PATH, fileName, "created", timestamp);
                Path versionedFile = Paths.get(versionedFileName);
                ensureParentDirectoryExists(versionedFile);

                if (Files.exists(versionedFile)) {
                    return;
                }

                Files.write(versionedFile, fileBytes, StandardOpenOption.CREATE_NEW);

                files.add(absolutePath);
                mapPathToContent.put(absolutePath, fileBytes);

                return; // Exit after successful operation

            } catch (NoSuchFileException e) {
                //If the file does not exist log an error and return
                Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "File does not exist: " + filePath.toString(), e);
                return;
            } catch (FileSystemException e) {
                //Checks if the file is locked or inaccessible. After trying 5 times log an error and don't version the file
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "File is locked or inaccessible after multiple attempts: " + filePath.toString(), e);
                    return;
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Retry interrupted: " + filePath.toString(), interruptedException);
                    return;
                }
            } catch (IOException e) {
                Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Could not write to: " + filePath.toString(), e);
                return;
            }
        }
    }

    /**
     * Handles events in the case where a file was modified.
     * @param filePath Path to the file that was modified -> Used to build the versioned filename.
     * @param timestamp TimeStamp at which the file was modified -> Used to build the versioned filename.
     */
    public void handleFileModification(Path filePath, long timestamp) {
        try {
            Path absolutePath = filePath.toAbsolutePath();
            String fileName = absolutePath.getFileName().toString();

            if (!mapPathToContent.containsKey(absolutePath)) return;

            byte[] oldBytes = mapPathToContent.get(absolutePath);
            byte[] modifiedBytes = FileHelper.readFileContent(absolutePath);

            // If the content of the file is still the same, then the file has not been modified.
            if (Arrays.equals(oldBytes, modifiedBytes)) return;

            String versionedFileName = FileHelper.buildVersionedFileName(this.DIRECTORY_PATH, fileName, "modified", timestamp);
            Path versionedFile = Paths.get(versionedFileName);
            ensureParentDirectoryExists(versionedFile);
            Files.write(versionedFile, oldBytes, StandardOpenOption.CREATE_NEW);

            mapPathToContent.replace(absolutePath, modifiedBytes);

        } catch (IOException e) {
            Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Could not process file modification for: " + filePath.toString(), e);
        }
    }

    /**
     * Handles events in the case where a file was deleted.
     * @param filePath Path to the file that was deleted -> Used to build the versioned filename.
     * @param timestamp TimeStamp at which the file was deleted -> Used to build the versioned filename.
     */
    public void handleFileDeletion(Path filePath, long timestamp) {
        try {
            Path absolutePath = filePath.toAbsolutePath();
            String fileName = absolutePath.getFileName().toString();

            if (!mapPathToContent.containsKey(absolutePath)) return;

            byte[] fileBytes = mapPathToContent.get(absolutePath);

            String versionedFileName = FileHelper.buildVersionedFileName(this.DIRECTORY_PATH, fileName, "deleted", timestamp);
            Path versionedFile = Paths.get(versionedFileName);

            ensureParentDirectoryExists(versionedFile);
            Files.write(versionedFile, fileBytes, StandardOpenOption.CREATE_NEW);

            files.remove(absolutePath);
            mapPathToContent.remove(absolutePath);

        } catch (IOException e) {
            Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Could not process file deletion for: " + filePath.toString(), e);
        }
    }

    /**
     * Ensures the parent directory of a file exists.
     * @param filePath The path to the file.
     */
    public void ensureParentDirectoryExists(Path filePath) {
        try {
            if (filePath != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
        } catch (IOException e) {
            Logger.getLogger(EventHandler.class.getName()).log(Level.WARNING, "Could not create parent directory for: " + filePath, e);
        }
    }
}
