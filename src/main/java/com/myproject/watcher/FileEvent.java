package com.myproject.watcher;
import java.nio.file.Path;


public class FileEvent {
    public enum EventType{FOLDER_REGISTRATION, FOLDER_DELETION, FILE_CREATION, FILE_MODIFICATION, FILE_DELETION}
    private final EventType eventType;
    private final int priority;
    private final Path path;

    public FileEvent(Path path, EventType eventType, int priority) {
        this.eventType = eventType;
        this.priority = priority;
        this.path = path;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getPriority() {
        return priority;
    }
    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "FileEvent -> {" +
                "eventType=" + eventType +
                ", priority=" + priority +
                ", path=" + path +
                '}';
    }


}
