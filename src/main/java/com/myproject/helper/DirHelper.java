package com.myproject.helper;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
public class DirHelper {

  //Checks if the directory entered exists
    public static boolean checkDirExists(Path path){
      return Files.exists(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
    }

    public static boolean isADir(Path path){
      return Files.isDirectory(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
    }

    public static void main(String[] args) {
        
    }
}
