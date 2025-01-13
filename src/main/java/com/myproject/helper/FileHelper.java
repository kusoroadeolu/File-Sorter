package com.myproject.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.myproject.versioning.FileVersioner;

public class FileHelper {
   public static List<Path> getFiles(Path path){
   try(Stream<Path> paths = Files.walk(path)){
     List<Path> files = paths.filter(Files::isReadable)
                             .filter(Files::isWritable)
                             .filter(Files::isRegularFile)
                             .distinct()
                             .sorted()
                             .collect(Collectors.toList());
     return files;
   }catch(IOException e){
     Logger.getLogger(FileVersioner.class.getName()).log(Level.SEVERE, "Could not walk through directory: {0}", path.toString());
     return null;
   }
}

  public static byte[] readFileContent(Path filePath){
    try {
        byte[] b = Files.readAllBytes(filePath);
        return b;
    } catch (IOException e) {
      Logger.getLogger(FileVersioner.class.getName()).log(Level.SEVERE, "Could read file: {0}", filePath);
      return null;
    }
  }

//builds the name of a versioned file
  public static String buildVersionedFileName(Path directoryPath, String fileName, String eventType, long timestamp){
    String formattedDate = StringHelper.formatDate();
    // Build the folder name
    Path versionedPath = directoryPath.getParent()
    .resolve(directoryPath.getFileName())
    .resolve("versions")
    .resolve("Version_" + formattedDate)
    .resolve(eventType)
    .resolve(fileName + "_" + timestamp);
     return versionedPath.toString();

  }
}
