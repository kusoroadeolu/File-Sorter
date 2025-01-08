package com.myproject.helper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirHelper {

  // Checks if a directory exists
  public static boolean checkDirExists(Path path) {
    return Files.exists(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
  }

  public static boolean isADir(Path path) {
    return Files.isDirectory(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
  }

  // Prevents directory duplication within a specified threshold
  public static void handleDirectoryDuplication(Path path, WatchEvent.Kind<?> eventKind, long currentTime, HashMap<Path, Long> map) {
    if (path != null) {
      boolean dirExists = map.containsKey(path);
      boolean duplicateDirCreatedWithinThreshold = (currentTime - map.getOrDefault(path, (long) 0)) > 500;
      if (!dirExists || duplicateDirCreatedWithinThreshold) {
        System.out.println("Event Kind: " + eventKind + ", File: " + path.toString());
        map.put(path, currentTime);
      }
    }
  }

  // Creates a folder and returns it as a path
  public static Path createFolder(String folderName) {
    try {
      Path directoryPath = Paths.get(folderName);
      // Checks if the folder already exists before creating the folder
      if (!Files.exists(directoryPath)) {
        Files.createDirectories(directoryPath);
        return directoryPath;
      }
      return null;
    } catch (IOException e) {
      Logger.getLogger(DirHelper.class.getName()).log(Level.SEVERE, "Failed to create folder at this directory!", e);
      return null;
    }
  }

  public static Path createFolder(String folderName, String formattedDate) {
    StringBuilder builder = new StringBuilder(folderName);
    builder.append(formattedDate);
    try {
      Path directoryPath = Paths.get(builder.toString());
      if (!Files.exists(directoryPath)) {
        Files.createDirectories(directoryPath);
        return directoryPath;
      }
      return null;
    } catch (IOException e) {
      Logger.getLogger(DirHelper.class.getName()).log(Level.SEVERE, "Failed to create folder at this directory!", e);
      return null;
    }
  }
}
