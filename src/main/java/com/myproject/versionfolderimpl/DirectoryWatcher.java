package com.myproject.versionfolderimpl;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.myproject.helper.DirHelper;



public class DirectoryWatcher {
  //private String filePath;
  private final Path DIR_PATH;
  private final HashMap<Path, Long> mapPathToCreationTime = new HashMap<>();
  private WatchService watchService;

@SuppressWarnings (value = { "" })
  public DirectoryWatcher(Path DIR_PATH) {
    this.DIR_PATH = DIR_PATH;
    watchAllDirectories(DIR_PATH);
  }

  //allows a directory to be watched by the watchservice
  private void registerDirectory(Path path) {
    Thread thread = new Thread(() -> {
        try {
          //checks if the directory exists and the directory is a folder and not a file
          if (DirHelper.checkDirExists(path) && DirHelper.isADir(path)) {
             this.watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
                watchDirectoryEvents(watchService);
          }
        } catch (IOException e) {
          System.out.println("Could not initialize watch service for: " + DIR_PATH.toString());
          closeWatchService();
          e.printStackTrace();
        }  
    });
    thread.start();
  }

  //watches for certain events in a directory
  private void watchDirectoryEvents(WatchService watchService) {
    try {
      boolean running = true;
    
      while (running) {
        WatchKey key = watchService.take();

        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> eventKind = event.kind();
          Path affectedPath = (Path) event.context();
          Path absolutePath = DIR_PATH.resolve(affectedPath);
          System.out.println(key.watchable());
          handleDirectoryDuplication(absolutePath, eventKind);
        }

        if (!key.reset()) {
          System.out.println("Key invalid");
          break;
        }

        running = !Thread.interrupted();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  //allows all subdirectories in a directory to be watched 
  public void watchAllDirectories(Path path){
    //gets the subdirectories from the path using a stream
      try(Stream<Path> paths = Files.walk(path)){
        List<Path> subDirectories = paths.filter(Files::isDirectory).distinct().sorted().collect(Collectors.toList());
        subDirectories.forEach(this::registerDirectory);
      }catch(IOException e){
        closeWatchService();
      }
  }

  //prevents directory duplication
  private void handleDirectoryDuplication(Path path, WatchEvent.Kind<?> eventKind) {
    long currentTime = System.currentTimeMillis();
    boolean dirExists = mapPathToCreationTime.containsKey(path);
    boolean duplicateDirCreatedWithinThreshold = (currentTime - mapPathToCreationTime.getOrDefault(path, (long)0)) > 500;
    if (!dirExists || duplicateDirCreatedWithinThreshold) {
      System.out.println("Event Kind: " + eventKind + ", File: " + path.toString());
      mapPathToCreationTime.put(path, currentTime);
      
    }
  }

  private void closeWatchService(){
    try{
      this.watchService.close();
    }catch(IOException e){
      e.printStackTrace();
    }
  }
}
