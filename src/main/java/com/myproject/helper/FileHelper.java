package com.myproject.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.myproject.versioning.FileVersioner;

public class FileHelper {
    /**
     * Recursively gets the files from a directory at the time the directory was instantiated to be watched
     * @param path The directory path
     * @return a thread-safe list of the files gotten from the directory
     * */
   public static CopyOnWriteArrayList getFiles(Path path){
   try(Stream<Path> paths = Files.walk(path)){
     List<Path> files = paths.filter(Files::isReadable)
                             .filter(Files::isWritable)
                             .filter(Files::isRegularFile)
                             .distinct()
                             .sorted()
                             .collect(Collectors.toList());
     Path exemptedFile = path.resolve("versions");

     CopyOnWriteArrayList<Path> concurrentList = new CopyOnWriteArrayList<>(files);
     concurrentList.stream().filter(Objects::nonNull).filter(file -> file.startsWith(exemptedFile)).forEach(files::remove);
     return concurrentList;

   }catch(IOException e){
     Logger.getLogger(FileVersioner.class.getName()).log(Level.SEVERE, "Could not walk through directory: {0}", path.toString());
     return null;
   }
}

/**
 * Reads the content of a file
 * @param filePath The file to be read
 * @return the file content in bytes
 * */

  public static byte[] readFileContent(Path filePath){
    try {
        byte[] b = Files.readAllBytes(filePath);
        return b;
    } catch (IOException e) {
      Logger.getLogger(FileVersioner.class.getName()).log(Level.SEVERE, "Could read file: {0}", filePath);
      return null;
    }
  }

/**
 * Builds the name of the file to be versioned
 * @param directoryPath Path to the directory the file is located
 * @param fileName Name of the file to be versioned
 * @param eventType The type of event
 * @param timestamp The timestamp for the event
 * @return The versioned filename
 * */

  public static String buildVersionedFileName(Path directoryPath, String fileName, String eventType, long timestamp){
    String formattedDate = StringHelper.formatDate();

    String fileExtension = StringHelper.extractExtension(fileName);
    String fileNameWithoutExtension = fileName.replace("." + fileExtension, "");

    // Build the folder name
    Path versionedPath = directoryPath.getParent()
    .resolve(directoryPath.getFileName())
    .resolve("versions")
    .resolve("Version_" + formattedDate)
    .resolve(eventType)
    .resolve(fileNameWithoutExtension + "_" + timestamp);

    //add the extension back to the versioned path
    return versionedPath.toString() + "." + fileExtension;

  }
}
