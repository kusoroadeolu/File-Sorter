package com.myproject.helper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirHelper {

  /**
   * Checks if a directory exists
   * @param path the directory path
   * @return true if the directory exists
   * */
  public static boolean checkDirExists(Path path) {
    return Files.exists(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
  }

  /**
   * Checks if a path is a folder
   * @param path the path to be checked
   * @return true if the path is a folder
   * */

  public static boolean isADir(Path path) {
    return Files.isDirectory(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
  }

  /**
   * Prevents file duplication when the file is modified
   * @param path the path that was affected
   * @param eventKind the kind of event that occurred
   * @param currentTime the timestamp at which the event occurred
   * @param map maps the path to the timestamp
   * */
  public static void handleDirectoryDuplication(Path path, WatchEvent.Kind<?> eventKind, long currentTime, ConcurrentHashMap<Path, Long> map) {
    if (path == null) {
      return;
    }
      boolean dirExists = map.containsKey(path);
      boolean lastEventTime = (currentTime - map.getOrDefault(path, (long) 0)) > 500;

      if (!dirExists || lastEventTime) {
        System.out.println("Event Kind: " + eventKind + ", File: " + path.toString());
        map.put(path, currentTime);
        return;
      }

  }

  /**
   * Creates a folder
   *
   * @param folderName the name of the folder. Note: Parent paths of the folder should be included in its name
   */
  public static void createFolder(String folderName) {
    try {
      Path directoryPath = Paths.get(folderName);
      // Checks if the folder already exists before creating the folder
      if (!Files.exists(directoryPath)) {
        Files.createDirectories(directoryPath);
      }
    } catch (IOException e) {
      Logger.getLogger(DirHelper.class.getName()).log(Level.SEVERE, "Failed to create folder at this directory!", e);
    }
  }

  }

