package com.myproject.watcher;

import java.util.Comparator;

public class FileEventComparator implements Comparator<FileEvent> {

    /**
     * Compares the priority of a file event to another
     * @param event1 A file event
     * @param event2 A file event
     * */
    @Override
    public int compare(FileEvent event1, FileEvent event2){
        return Integer.compare(event1.getPriority(), event2.getPriority());
    }

}
