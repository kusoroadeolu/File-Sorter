package com.myproject.helper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StringHelper {
  /**
   * Formats the current date and time
   * @return a formatted date of pattern "yyyy-MM-dd"
   * */
  public static String formatDate() {
    LocalDateTime date = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return date.format(formatter);
  }

  /**
   * Extracts the extension from a file's name
   * @param fileName the name of the file
   * @return the file's extension
   * */
  public static String extractExtension(String fileName){
    String[] fileNameParts = fileName.split("\\.");
    return fileNameParts[fileNameParts.length - 1];

  }

  /**
   * Removes the extension from a file's name
   * @param fileName the name of the file
   * @return the filename without the extension
   * */
  public static String removeExtension(String fileName){
    return fileName.replace("." + extractExtension(fileName), "");
  }

}
