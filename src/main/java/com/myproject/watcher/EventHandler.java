package com.myproject.watcher;

import com.myproject.helper.FileHelper;
import com.myproject.versioning.FileVersioner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventHandler{
    private final FileVersioner fileVersioner;
    private final List<Path> files;
    private final static ConcurrentHashMap<Path,byte[]> mapPathToContent = FileVersioner.getMapFileToContent();
    private final Path DIRECTORY_PATH;

    public EventHandler(Path DIRECTORY_PATH) throws IOException{
        this.DIRECTORY_PATH = DIRECTORY_PATH;
        fileVersioner = new FileVersioner(DIRECTORY_PATH);
        this.files = fileVersioner.getFiles();
    }

    /**
     * Handles events in the case where a file was created
     * @param filePath Path to the file that was created -> Used to build the versioned filename
     * @param timestamp TimeStamp at which the file was created -> Used to build the versioned filename
     * */
    public void handleFileCreation(Path filePath, long timestamp){
        try{
        Path absolutePath = filePath.toAbsolutePath();
        String fileName = absolutePath.getFileName().toString();

        byte[] fileBytes = Files.readAllBytes(absolutePath);

        String versionedFileName = FileHelper.buildVersionedFileName(this.DIRECTORY_PATH, fileName, "created", timestamp);
        Path versionedFile = Paths.get(versionedFileName);
        ensureParentDirectoryExists(absolutePath);

        if(Files.exists(versionedFile)){
            return;
        }

        Files.createFile(versionedFile);
        Files.write(versionedFile, fileBytes);
        
        files.add(absolutePath);
        mapPathToContent.put(absolutePath, fileBytes);

        } catch (IOException e) {
            Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Could not read bytes from: " + filePath.toString(), e);
        }
    }

    /**
     * Handles events in the case where a file was created
     * @param filePath Path to the file that was modified -> Used to build the versioned filename
     * @param timestamp TimeStamp at which the file was modified -> Used to build the versioned filename
     * */
    public void handleFileModification(Path filePath, long timestamp){
        try{
            Path absolutePath = filePath.toAbsolutePath();
            String fileName = absolutePath.getFileName().toString();

            if (!mapPathToContent.contains(absolutePath)) return;

            byte[] oldBytes = mapPathToContent.get(absolutePath);
            byte[] modifiedBytes = FileHelper.readFileContent(absolutePath);

            //if the content of the file is still the same then the file has not been modified
            if (Arrays.equals(oldBytes, modifiedBytes)) return;

            String versionedFileName = FileHelper.buildVersionedFileName(this.DIRECTORY_PATH, fileName, "modified", timestamp);
            Path versionedFile = Paths.get(versionedFileName);
            ensureParentDirectoryExists(absolutePath);
            Files.createFile(versionedFile);
            Files.write(versionedFile, oldBytes);

            mapPathToContent.replace(absolutePath, modifiedBytes);

        } catch (IOException e) {
            Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Could not read bytes from: " + filePath.toString(), e);
        }
    }

    /**
     * Handles events in the case where a file was created
     * @param filePath Path to the file that was modified -> Used to build the versioned filename
     * @param timestamp TimeStamp at which the file was modified -> Used to build the versioned filename
     * */
    public void handleFileDeletion(Path filePath, long timestamp){
        try{
            Path absolutePath = filePath.toAbsolutePath();
            String fileName = absolutePath.getFileName().toString();

            if(!mapPathToContent.contains(absolutePath)) return;

            byte[] fileBytes = mapPathToContent.get(absolutePath);

            String versionedFileName = FileHelper.buildVersionedFileName(this.DIRECTORY_PATH, fileName, "deleted", timestamp);
            Path versionedFile = Paths.get(versionedFileName);

            ensureParentDirectoryExists(absolutePath);
            Files.createFile(versionedFile);
            Files.write(versionedFile, fileBytes);

            files.remove(absolutePath);
            mapPathToContent.remove(absolutePath);

        } catch (IOException e) {
            Logger.getLogger(EventHandler.class.getName()).log(Level.SEVERE, "Could not read bytes from: " + filePath.toString(), e);
        }
    }
    
    
    /**
     * Ensures the parent directory of a file exists
     * @param filePath The path to the file
     * */
    public void ensureParentDirectoryExists(Path filePath){
        try{
        if(filePath != null && !Files.exists(filePath.getParent())){
            Files.createDirectories(filePath.getParent());
            }
        }catch(IOException e){
            Logger.getLogger(EventHandler.class.getName()).log(Level.WARNING, "Could not create parent directory for: " + filePath, e);
        }
    }



}
