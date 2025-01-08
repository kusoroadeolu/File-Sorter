package com.myproject;

import java.nio.file.Paths;

import com.myproject.versioning.FileVersioner;

public class Main {
        public static void main(String[] args) throws Exception {
           FileVersioner fv = new FileVersioner(Paths.get("C:\\Users\\eastw\\Git Projects\\Personal\\filemanager"));
           fv.createVersionedFolders();
            
            
        
    }
}

   