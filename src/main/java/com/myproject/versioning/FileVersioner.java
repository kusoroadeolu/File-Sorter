package com.myproject.versioning;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.myproject.helper.DirHelper;
import com.myproject.helper.FileHelper;

/**
 * The FileVersioner class is responsible for managing file versions within a specified directory.
 * It initializes by mapping the initial content of files in the directory and creates a folder
 * structure for versioning based on the current date. The class also provides functionality to
 * retrieve the mapping of files to their content.
 *
 * <p>Key functionalities include:
 * <ul>
 *   <li>Creating version folders based on the current date</li>
 *   <li>Mapping files to their content at the time of initialization</li>
 *   <li>Providing access to the file-to-content mapping</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * Path directoryPath = Paths.get("path/to/directory");
 * FileVersioner fileVersioner = new FileVersioner(directoryPath);
 * String versionFolder = fileVersioner.versionFolders();
 * HashMap<Path, byte[]> fileContentMap = FileVersioner.getMapFileToContent();
 * }
 * </pre>
 */
public final class FileVersioner {

  private final List<Path> files;
  private final static ConcurrentHashMap<Path, byte[]> mapFileToContent = new ConcurrentHashMap<>();

  public FileVersioner(Path directoryPath) throws IOException {
    this.files = FileHelper.getFiles(directoryPath);
    mapFileToContent();
    // Initializes a new directory watcher for the version
    DirHelper.createFolder(directoryPath.getParent().resolve(directoryPath.getFileName().toString() + "\\versions").toString());
  }


  /**
   * Get the files and the content of the initial files in the directory path
   * */
  public void mapFileToContent() {
    for (Path filePath : files) {
      Path absoluteFile = filePath.toAbsolutePath();
      byte[] fileBytes = FileHelper.readFileContent(filePath);
      //Checks if the file has content that can be read
        assert fileBytes != null : "File bytes should not be null";
        mapFileToContent.put(absoluteFile, fileBytes);
    }
  }

  /**
   * Retrieves the mapping of file paths to their corresponding content.
   * @return a ConcurrentHashMap containing the mapping of file paths to file content.
   */
  public static ConcurrentHashMap<Path, byte[]> getMapFileToContent() {
    return mapFileToContent;
  }

  public List<Path> getFiles(){
    return files;
  }
}
