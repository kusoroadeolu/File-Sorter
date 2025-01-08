package com.myproject.watcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class WatchedDirectory {
  private final Path path;
  private final long timestamp;
  //Stores if the file was created, modified, deleted etc
  private final WatchEvent.Kind<?> eventKind;

  public WatchedDirectory(Path path, long timestamp, WatchEvent.Kind<?> eventKind) {
    this.path = path;
    this.timestamp = timestamp;
    this.eventKind = eventKind;
  }

  public Path getPath() {
    return this.path;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public WatchEvent.Kind<?> getEventKind() {
    return this.eventKind;
  }


}
