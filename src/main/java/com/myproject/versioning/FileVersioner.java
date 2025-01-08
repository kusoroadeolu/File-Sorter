package com.myproject.versioning;
import java.io.IOException;
import java.nio.file.Path;

import com.myproject.helper.DateHelper;
import com.myproject.helper.DirHelper;
import com.myproject.watcher.DirectoryWatcher;

public class FileVersioner {
  private final Path DIRECTORY_PATH;
  private DirectoryWatcher directoryWatcher;

  public FileVersioner(Path DIRECTORY_PATH) throws IOException{
    this.DIRECTORY_PATH = DIRECTORY_PATH;
    //initailizes a new directory watcher for the versioner
    this.directoryWatcher = new DirectoryWatcher(DIRECTORY_PATH);
    DirHelper.createFolder(DIRECTORY_PATH.getFileName().toString() + "_versioned-folders");
  }

  //Create folders corresponding to the current date
  public void createVersionedFolders(){
    String formattedDate = DateHelper.formatDate();
    //Build the folder name
    StringBuilder buildFolderName = new StringBuilder(DIRECTORY_PATH.toString());
    buildFolderName.append("\\")
    .append(DIRECTORY_PATH.getFileName())
    .append("_versioned-folders")
    .append("\\Folder_")
    .append(formattedDate);
    DirHelper.createFolder(buildFolderName.toString());
 }











  
}
