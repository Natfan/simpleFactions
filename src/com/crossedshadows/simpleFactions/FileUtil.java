package com.crossedshadows.simpleFactions;
 
import java.io.File;
import java.io.FilenameFilter;
 
public class FileUtil {
 
	  public String[] listFiles(String dir) {
		  
		    File directory = new File(dir);
		 
		    if (!directory.isDirectory()) {
		      System.out.println("No directory provided");
		      return null;
		    }
		 
		    //create a FilenameFilter and override its accept-method
		    FilenameFilter filefilter = new FilenameFilter() {
		 
		      public boolean accept(File dir, String name) {
		        //if the file extension is .txt return true, else false
		        return name.endsWith(".json");
		      }
		    };
		 
		    return directory.list(filefilter);
		  }
}