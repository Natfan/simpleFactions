package com.crossedshadows.simpleFactions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.bukkit.Bukkit;
import org.json.JSONObject;

public class Language {
	public static JSONObject language = new JSONObject(); 
	public static String languageVersion = "1.0"; 
	
	public static String loadLanguageFile(){
		String languageFile = "";
		InputStream input = simpleFactions.class.getResourceAsStream("/languageFile.json");
		Scanner scan = new Scanner(input).useDelimiter("\\Z");
		languageFile = scan.next();
		scan.close(); 
		
		language = new JSONObject(languageFile);
		return languageFile;
	}
	
	public static void loadLanguageData(){
    	File dataFolder = simpleFactions.dataFolder; 
    	String version = languageVersion; 
    	String filename = "translations/" + Config.configData.getString("Language"); 
    	
    	File languageFile = new File(dataFolder + "/" + filename + ".json");
    	if(!languageFile.exists()){
			try {
				FileWriter fw = new FileWriter(languageFile);
				BufferedWriter bw=new BufferedWriter(fw);
				
				//createConfigData();
				
				bw.write(loadLanguageFile());
				bw.newLine();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	try {
    		FileReader filereader = new FileReader(dataFolder + "/" + filename + ".json");
			Scanner scan = new Scanner(filereader).useDelimiter("\\Z");
			language = new JSONObject(scan.next());
			//loadLanguageFile(); 
			scan.close();
			
			if(!language.getString("LanguageVersion").equals(version)){
				Bukkit.getServer().getConsoleSender().sendMessage("§cLanguage file is out of date! " +
						"Backing up old language file and creating a new one! Please go and redo your configs with the new format!");

				try {
					File backupFile = new File(languageFile.getAbsoluteFile() + ".backup");
					FileWriter filew = new FileWriter(backupFile);
					BufferedWriter baw = new BufferedWriter(filew);
					baw.write(loadLanguageFile());
					baw.newLine();
					baw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
					FileWriter fw = new FileWriter(languageFile);
					BufferedWriter bw=new BufferedWriter(fw);
					bw.write(loadLanguageFile()); //creates a new language, sets language data, and saves it via bw
					bw.newLine();
					bw.close();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getLanguage(){
		return language.getString("LanguageName"); 
	}
	
	public static String getMessage(String message){
		if(language.has(message))
			return language.getString(message); 
		else
			return "Error! Message not in language file!"; 
	}
}
