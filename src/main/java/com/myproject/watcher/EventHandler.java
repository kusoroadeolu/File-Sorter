package com.myproject.watcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.myproject.helper.DirHelper;
import com.myproject.helper.FileHelper;
import com.myproject.helper.StringHelper;
import com.myproject.versioning.FileVersioner;

import static java.nio.file.StandardWatchEventKinds.*;

public class EventHandler {
    private final FileVersioner fileVersioner;
    private ConcurrentSkipListSet<Path> files = new ConcurrentSkipListSet<>();
    private final ConcurrentHashMap<Path, byte[]> mapPathToContent;
    private final Path DIRECTORY_PATH;
    private final ExecutorService eventService = Executors.newCachedThreadPool();
    private final BlockingQueue<FileEvent> eventQueue = new PriorityBlockingQueue<>(20, new FileEventComparator());
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Logger eventLogger =  Logger.getLogger(EventHandler.class.getName());
    private final  WatchService watchService;

    public EventHandler(Path DIRECTORY_PATH, WatchService watchService){
        System.out.println("Event Handler init");
        this.watchService = watchService;
        this.DIRECTORY_PATH = DIRECTORY_PATH;
        fileVersioner = new FileVersioner(DIRECTORY_PATH);
        this.mapPathToContent = fileVersioner.getMapFileToContent();
        getAllFiles();
    }

    private void getAllFiles(){
        try(Stream<Path> directories = Files.walk(DIRECTORY_PATH)){
        CopyOnWriteArrayList<Path> folders = directories.filter(Files::isDirectory).collect(Collectors.toCollection(CopyOnWriteArrayList::new));
            for(Path folder: folders){
                    files = FileHelper.getFiles(folder);
                if(files == null){
                    files = new ConcurrentSkipListSet<>();
                }
                addFile(files);
            }
        }catch (IOException e){
            Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Could not walk through : " + DIRECTORY_PATH.toString());
        }
    }

    private void addFile(ConcurrentSkipListSet<Path> files){
        for(Path file: files){
            Path absoluteFile = file.toAbsolutePath();
            try{
            if(files.contains(absoluteFile) && Files.exists(absoluteFile) && !absoluteFile.toString().contains("versions")){
                files.add(absoluteFile);
                mapPathToContent.put(absoluteFile, Files.readAllBytes(absoluteFile));
                System.out.println("Added file: " + absoluteFile.toString());
            }
            }catch (IOException e){
                Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Failed to read content from: " + absoluteFile.toString(), e);
            }
        }
    }

    /**
     * Handles events in the case where a file was created.
     * @param filePath Path to the file that was created -> Used to build the versioned filename.
     * @param timestamp TimeStamp at which the file was created -> Used to build the versioned filename.
     */
    public void handleFileCreation(Path filePath, String timestamp) {
        if(!Files.exists(filePath)) return;
        //Sets the number of times the file can be accessed
        final int MAX_RETRIES = 5;
        //The time the thread waits before trying to access the file again
        final long RETRY_DELAY_MS = 100;
        int attempts = 0;

        while (attempts < MAX_RETRIES) {
            try {

                Path absolutePath = filePath.toAbsolutePath();

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
    private void handleFileModification(Path filePath, String timestamp) {
        if(filePath == null) return;

        try {
            Path absolutePath = filePath.toAbsolutePath();
            String fileName = absolutePath.getFileName().toString();

            if(!mapPathToContent.containsKey(absolutePath)) return;

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
    private void handleFileDeletion(Path filePath, String timestamp) {
        try {
            Path absolutePath = filePath.toAbsolutePath();
            String fileName = absolutePath.getFileName().toString();

            if (!mapPathToContent.containsKey(absolutePath)) return;

            byte[] fileBytes = mapPathToContent.get(absolutePath);

            String versionedFileName = FileHelper.buildVersionedFileName(this.DIRECTORY_PATH, fileName, "deleted", timestamp);
            Path versionedFile = Paths.get(versionedFileName);

            ensureParentDirectoryExists(versionedFile);
            Files.write(versionedFile, fileBytes, StandardOpenOption.CREATE_NEW);

            if(files.contains(absolutePath)){
            files.remove(absolutePath);
            mapPathToContent.remove(absolutePath);
            }

        } catch (IOException e) {
            Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Could not read content from: " + filePath.toString(), e);
        }
    }

    private void handleFolderDeletion(Path folderPath, String timestamp){
        if(!Files.exists(folderPath)) return;

        try(Stream<Path> paths = Files.walk(folderPath)){
            CopyOnWriteArrayList<Path> deletedDirectories = paths.filter(Files::isDirectory).sorted().collect(Collectors.toCollection(CopyOnWriteArrayList::new));
            ConcurrentSkipListSet <Path> deletedFiles;
            try{
                for(Path directory: deletedDirectories){
                    deletedFiles = Files.walk(directory).filter(Files::isRegularFile).filter(Files::isReadable).filter(Files::isWritable).collect(Collectors.toCollection(ConcurrentSkipListSet::new));
                    for(Path file: deletedFiles){
                        if(Files.exists(file.toAbsolutePath())){
                            handleFileDeletion(file, timestamp);
                        }
                    }
                }
            }catch (IOException e){
                Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Failed to create versioned file for: " + folderPath, e);
            }

        }catch (IOException e){
            Logger.getLogger(DirectoryWatcher.class.getName()).log(Level.SEVERE, "Failed to get directories!", e);
        }

    }

    public void consumeEvents(Path absolutePath, WatchEvent.Kind<?> eventKind){

        eventService.submit(() -> {
            try{
            if(eventKind.equals(ENTRY_CREATE)){
                eventQueue.put(new FileEvent(absolutePath, FileEvent.EventType.FILE_CREATION, 3));
            }else if(eventKind.equals(ENTRY_DELETE)){
                if(!DirHelper.isADir(absolutePath)){
                eventQueue.put(new FileEvent(absolutePath, FileEvent.EventType.FILE_DELETION, 2 ));
                return;
                }
                eventQueue.put(new FileEvent(absolutePath, FileEvent.EventType.FOLDER_DELETION, 4));
            }else if(eventKind.equals(ENTRY_MODIFY)){
                eventQueue.put(new FileEvent(absolutePath, FileEvent.EventType.FILE_MODIFICATION, 1));
            }
            } catch (InterruptedException e) {
                Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, Thread.currentThread().getName() + " was interrupted during its operation.", e);
                Thread.currentThread().interrupt();
            }
        });



    }

    public void handleEvents(String timestamp){
        eventService.submit(() -> {
            while (true){
            try {
                FileEvent fileEvent = eventQueue.take();
                FileEvent.EventType eventType = fileEvent.getEventType();
                Path eventPath = fileEvent.getPath().toAbsolutePath();

                switch (eventType){
                    case FILE_CREATION -> handleFileCreation(eventPath, timestamp);
                    case FILE_DELETION -> handleFileDeletion(eventPath, timestamp);
                    case FILE_MODIFICATION -> handleFileModification(eventPath, timestamp);
                    case FOLDER_DELETION -> handleFolderDeletion(eventPath, timestamp);
                    default -> {
                        System.out.println("Default event found");
                        return;
                    }

                }
                System.out.println("Handled path: " + fileEvent.getPath());


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Stopped the event service object", e);
            }
        }});
    }

    /**
     * Ensures the parent directory of a file exists.
     * @param filePath The path to the file.
     */
    private void ensureParentDirectoryExists(Path filePath) {
        try {
            if (filePath != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
        } catch (IOException e) {
            Logger.getLogger(EventHandler.class.getName()).log(Level.WARNING, "Could not create parent directory for: " + filePath, e);
        }
    }

    private void closeEventService(){
       running.set(false);
       eventService.shutdown();
    }

}