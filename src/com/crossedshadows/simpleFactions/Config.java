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
import org.json.JSONArray;
import org.json.JSONObject;

public class Config {
	
	//the json object storing all config data
	static JSONObject configData = new JSONObject();
	
	//default configs
	static int chunkSizeX = 16;
	static int chunkSizeY = 16;
	static int chunkSizeZ = 16;
	static int powerCapMax = 750; // 25 * 30 (default power is 25 * 30 players)
	
	//default colors
	static String Rel_Faction = "§b";
	static String Rel_Ally = "§d";
	static String Rel_Enemy = "§c";
	static String Rel_Neutral = "§2";
	static String Rel_Other = "§f";
	static String Rel_Truce = "§6";
	static String powerCapType = "none";
	
	//priv config
	static JSONArray neutralBreakData = new JSONArray();
	static JSONArray allyBreakData = new JSONArray();
	static JSONArray truceBreakData = new JSONArray();
	static JSONArray otherBreakData = new JSONArray();
	static JSONArray enemyBreakData = new JSONArray();
	static JSONArray neutralPlaceData = new JSONArray();
	static JSONArray allyPlaceData = new JSONArray();
	static JSONArray trucePlaceData = new JSONArray();
	static JSONArray otherPlaceData = new JSONArray();
	static JSONArray enemyPlaceData = new JSONArray();
	static JSONArray neutralItemData = new JSONArray();
	static JSONArray allyItemData = new JSONArray();
	static JSONArray truceItemData = new JSONArray();
	static JSONArray otherItemData = new JSONArray();
	static JSONArray enemyItemData = new JSONArray();
	static JSONArray claimsDisabledInTheseWorlds = new JSONArray();
	
    /**
     * Creates config file.
     * */
    public static String createConfigData(){
    	
    	/*
    		The configJson is now stored in the .jar itself,
    		to prevent ugliness. 
		*/
		
		String configFile = "";
		InputStream input = simpleFactions.class.getResourceAsStream("/configFile.json");
		Scanner scan = new Scanner(input).useDelimiter("\\Z");
		configFile = scan.next();
		scan.close(); 
		
		configData = new JSONObject(configFile);
		//getLogger().info("configFile: " + configFile);
		//getLogger().info("########################");
		//getLogger().info("configJson: " + configData.toString(8));

		loadConfigData();
		return configFile;
    }

    /**
     * Loads config data into configData JSONObject.
     * */
    public static void loadConfig(){
    	
    	File dataFolder = simpleFactions.dataFolder; 
    	String version = simpleFactions.version; 
    	
    	File configFile = new File(dataFolder + "/config.json");
    	if(!configFile.exists()){
			try {
				FileWriter fw = new FileWriter(configFile);
				BufferedWriter bw=new BufferedWriter(fw);
				
				//createConfigData();
				
				bw.write(createConfigData());
				bw.newLine();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	try {
    		FileReader filereader = new FileReader(dataFolder + "/config.json");
			Scanner scan = new Scanner(filereader).useDelimiter("\\Z");
			configData = new JSONObject(scan.next());
			loadConfigData(); 
			scan.close();
			
			if(!configData.getString("version").equals(version)){
				Bukkit.getServer().getConsoleSender().sendMessage("§cConfig file is out of date! " +
						"Backing up old config file and creating a new one! Please go and redo your configs with the new format!");

				try {
					File backupFile = new File(configFile.getAbsoluteFile() + ".backup");
					FileWriter filew = new FileWriter(backupFile);
					BufferedWriter baw = new BufferedWriter(filew);
					baw.write(createConfigData());
					baw.newLine();
					baw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
					FileWriter fw = new FileWriter(configFile);
					BufferedWriter bw=new BufferedWriter(fw);
					bw.write(createConfigData()); //creates a new config, sets config data, and saves it via bw
					bw.newLine();
					bw.close();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    public static void loadConfigData(){
    	chunkSizeX = 			configData.getInt("claim size x");
		chunkSizeY =			configData.getInt("claim size y");
		chunkSizeZ = 			configData.getInt("claim size z");
		if(configData.has("power cap max power")) powerCapMax =			configData.getInt("power cap max power");
		if(configData.has("power cap type (none/soft/hard)")) powerCapType = 			configData.getString("power cap type (none/soft/hard)");
		
		neutralBreakData = 		configData.getJSONArray("block break protection in neutral land");
		allyBreakData = 		configData.getJSONArray("block break protection in ally land");
		truceBreakData = 		configData.getJSONArray("block break protection in truce land");
		otherBreakData = 		configData.getJSONArray("block break protection in other land");
		enemyBreakData = 		configData.getJSONArray("block break protection in enemy land");
		 
		neutralPlaceData = 		configData.getJSONArray("block place protection in neutral land");
		allyPlaceData = 		configData.getJSONArray("block place protection in ally land");
		trucePlaceData = 		configData.getJSONArray("block place protection in truce land");
		otherPlaceData = 		configData.getJSONArray("block place protection in other land");
		enemyPlaceData = 		configData.getJSONArray("block place protection in enemy land");
		 
		neutralItemData = 		configData.getJSONArray("item protection in neutral land");
		allyItemData = 			configData.getJSONArray("item protection in ally land");
		truceItemData = 		configData.getJSONArray("item protection in truce land");
		otherItemData = 		configData.getJSONArray("item protection in other land");
		enemyItemData = 		configData.getJSONArray("item protection in enemy land");
		
		claimsDisabledInTheseWorlds = configData.getJSONArray("disable claims and homes in these worlds");
    }
    
}
