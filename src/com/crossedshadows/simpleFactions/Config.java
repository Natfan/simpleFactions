package com.crossedshadows.simpleFactions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.bukkit.Bukkit;
import org.json.JSONArray;
import org.json.JSONObject;

public class Config {
	
	//the json object storing all config data
	public static JSONObject configData = new JSONObject();
	public static String configVersion = "1.0"; 
	
	//default configs
	public static int chunkSizeX = 16;
	public static int chunkSizeY = 16;
	public static int chunkSizeZ = 16;
	public static int powerCapMax = 750; // 25 * 30 (default power is 25 * 30 players)
	
	//default colors
	public static String Rel_Faction = "§b";
	public static String Rel_Ally = "§d";
	public static String Rel_Enemy = "§c";
	public static String Rel_Neutral = "§2";
	public static String Rel_Other = "§f";
	public static String Rel_Truce = "§6";
	public static String powerCapType = "none";
	
	//priv config
	public static JSONArray neutralBreakData = new JSONArray();
	public static JSONArray allyBreakData = new JSONArray();
	public static JSONArray truceBreakData = new JSONArray();
	public static JSONArray otherBreakData = new JSONArray();
	public static JSONArray enemyBreakData = new JSONArray();
	public static JSONArray neutralPlaceData = new JSONArray();
	public static JSONArray allyPlaceData = new JSONArray();
	public static JSONArray trucePlaceData = new JSONArray();
	public static JSONArray otherPlaceData = new JSONArray();
	public static JSONArray enemyPlaceData = new JSONArray();
	public static JSONArray neutralItemData = new JSONArray();
	public static JSONArray allyItemData = new JSONArray();
	public static JSONArray truceItemData = new JSONArray();
	public static JSONArray otherItemData = new JSONArray();
	public static JSONArray enemyItemData = new JSONArray();
	public static JSONArray claimsDisabledInTheseWorlds = new JSONArray();
	
	
	
	private static boolean isRedirected( Map<String, List<String>> header ) {
	      for( String hv : header.get( null )) {
	         if(   hv.contains( " 301 " )
	            || hv.contains( " 302 " )) return true;
	      }
	      return false;
	   }
	
	public static String getOnlineVersion() throws Throwable {
	  String link = "https://raw.githubusercontent.com/coty-crg/simpleFactions/master/configFile.json";
	  URL url  = new URL( link );
	  HttpURLConnection http = (HttpURLConnection)url.openConnection();
	  http.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0"); //trick to bypass cloudflare checks on various sites
		
	  Map< String, List< String >> header = http.getHeaderFields();
	  while( isRedirected( header )) {
		  	link = header.get( "Location" ).get( 0 );
	         url    = new URL( link );
	         http   = (HttpURLConnection)url.openConnection();
	         header = http.getHeaderFields();
	  }
	  BufferedReader response1 = new BufferedReader(new InputStreamReader(http.getInputStream()));
	  String res1 = ""; //response1.toString();
	  String someLine = "";
	  while((someLine = response1.readLine())!=null)
		  res1 += someLine;
	  
	  //Bukkit.getConsoleSender().sendMessage(res1);
	  JSONObject onlineConfig = new JSONObject(res1); 

	  String onlineVersion = "NULL"; //just in case no version is there
	  if(onlineConfig.has("pluginVersion")){
		  onlineVersion = onlineConfig.getString("pluginVersion"); 
	  }
	  
	  return onlineVersion; 
	}
	
	/**
	 * Checks for updates online. 
	 * */
	public static void checkForUpdates(){
		try {
			
			String onlineVersion = getOnlineVersion();
			
			if(!onlineVersion.equalsIgnoreCase(simpleFactions.version)){
				Bukkit.getConsoleSender().sendMessage("§c#############################################");
				Bukkit.getConsoleSender().sendMessage("  ");
				Bukkit.getConsoleSender().sendMessage("§aYOUR SIMPLEFACTIONS PLUGIN MIGHT BE OUT OF DATE.");
				Bukkit.getConsoleSender().sendMessage("  ");
				Bukkit.getConsoleSender().sendMessage("§aYour version: §c" + simpleFactions.version + " §aGithub version: §c" + onlineVersion);
				Bukkit.getConsoleSender().sendMessage("  ");
				Bukkit.getConsoleSender().sendMessage("§aPLEASE GO TO ONE OF THE FOLLOWING URLS TO UPDATE IT!");
				Bukkit.getConsoleSender().sendMessage("  ");
				Bukkit.getConsoleSender().sendMessage("  §b https://github.com/coty-crg/simpleFactions");
				Bukkit.getConsoleSender().sendMessage("  §b http://dev.bukkit.org/bukkit-plugins/simple-factions/");
				Bukkit.getConsoleSender().sendMessage("  §b http://www.spigotmc.org/threads/simplefactions.36766/");
				Bukkit.getConsoleSender().sendMessage("  ");
				Bukkit.getConsoleSender().sendMessage("§c#############################################");
			}
		} catch (MalformedURLException e) {
			Bukkit.getConsoleSender().sendMessage("§c[SimpleFactions Error]: Malformed URL detected while checking for updates!");
		} catch (IOException e) {
			Bukkit.getConsoleSender().sendMessage("§c[SimpleFactions Error]: I/O exception while checking for updates. Are you online?");
		} catch (Throwable e) {
			Bukkit.getConsoleSender().sendMessage("§c[SimpleFactions Error]: Unable to check for update. You may be offline or Github may be experiencing heavy traffic.");
		}
	}
	
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
    	String version = configVersion; 
    	
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
			
			if(!configData.getString("configVersion").equals(version)){
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
