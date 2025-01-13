package com.myproject.helper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StringHelper {
  public static String formatDate() {
    LocalDateTime date = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return date.format(formatter);
  }
}
