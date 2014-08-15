package com.crossedshadows.simpleFactions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import net.minecraft.util.com.google.gson.JsonSerializer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitScheduler;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.corba.se.impl.orbutil.ObjectWriter;

/**
 * TODO List
 * 
 * Misc TODO: 
 * 		Optional power scaling systems
 * 
 * Commands TODO:
 * 		/f negative (for seeing who in your faction has negative power)
 *		/f deny
 *
 *		PROTIP FOR OTHER PROGRAMMERS: If you're using eclipse, do <ctrl>+<shift>+/ (divide on the numpad) 
 *		to minimize all of the functions. Makes reading the code a breeze!
 * */

public class simpleFactions extends JavaPlugin implements Listener {

	//index of players/factions (constantly updating to stay accurate)
	List<String> factionIndex = new ArrayList<String>();
	List<String> playerIndex = new ArrayList<String>();
	List<String> boardIndex = new ArrayList<String>();
	
	//for traveling around faction territory (one of the few things that stay in memory for entire life of plugin)
	List<String> playerIsIn_player = new ArrayList<String>();
	List<String> playerIsIn_faction = new ArrayList<String>();
	List<Location> playerIsIn_location = new ArrayList<Location>();
	
	
	/**
	 * Okay this shit is kind of confusing; so here's how it works:
		If you want to access data of a faction or something, you do loadFaction(); 
		Once you do that, it loads all of that shit into the JSONObject factionData.
		Once it's loaded up, you can move stuff around, edit stuff within the jsonobject
		Once you're done changing shit, do saveFaction(String name_of_faction) in order to
		save all the shit you did and clear the factionData jsonobject. Works the same way
		for the loadPlayer() and loadWorld() functions. 
		
		In reality, you can use any jsonObject or jsonArray, even your own; all that really matters
		is that you keep track of what is actually containing what. So everything in this plugin COULD
		be done with a single, or hundreds, of separate jsonobjects; but I've broke them up like this
		for simplicity sake. 
	*/ 
	JSONObject boardData = new JSONObject();
	JSONObject factionData = new JSONObject();
	JSONObject playerData = new JSONObject();
	JSONArray enemyData = new JSONArray();
	JSONArray allyData = new JSONArray();
	JSONArray truceData = new JSONArray();
	JSONArray inviteData = new JSONArray();
	JSONObject configData = new JSONObject();
	JSONArray neutralBreakData = new JSONArray();
	JSONArray allyBreakData = new JSONArray();
	JSONArray truceBreakData = new JSONArray();
	JSONArray otherBreakData = new JSONArray();
	JSONArray enemyBreakData = new JSONArray();
	JSONArray neutralPlaceData = new JSONArray();
	JSONArray allyPlaceData = new JSONArray();
	JSONArray trucePlaceData = new JSONArray();
	JSONArray otherPlaceData = new JSONArray();
	JSONArray enemyPlaceData = new JSONArray();
	JSONArray neutralItemData = new JSONArray();
	JSONArray allyItemData = new JSONArray();
	JSONArray truceItemData = new JSONArray();
	JSONArray otherItemData = new JSONArray();
	JSONArray enemyItemData = new JSONArray();
	
	//default configs
	int chunkSizeX = 16;
	int chunkSizeY = 16;
	int chunkSizeZ = 16;
	
	String Rel_Faction = "§b";
	String Rel_Ally = "§d";
	String Rel_Enemy = "§c";
	String Rel_Neutral = "§2";
	String Rel_Other = "§f";
	String Rel_Truce = "§6";
	
	String version = "1.05";
	
	long currentTime = System.currentTimeMillis();
	long lastTime = System.currentTimeMillis();
	
	/**
	 * Starts with server startup or /reload
	 * */
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
		loadData();
		updatePlayerPower();
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has enabled successfully!]");
	}
	
	/**
	 * Runs when the server is shutting down
	 * */
    public void onDisable() {
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has shut down successfully!]");
		saveData();
    }
    
    public void createDirectories(){
    	File dir0 = new File(this.getDataFolder() + "/");
    	if(!dir0.exists()){
    		dir0.mkdir();
    		}
    	File dir_fac = new File(this.getDataFolder() + "/factionData");
    	if(!dir_fac.exists()){
    		dir_fac.mkdir();
    		}
    	File dir_play = new File(this.getDataFolder() + "/playerData");
    	if(!dir_play.exists()){
    		dir_play.mkdir();
    		}
    	File dir_world = new File(this.getDataFolder() + "/boardData");
    	if(!dir_world.exists()){
    		dir_world.mkdir();
    		}

    	FileUtil fileutil = new FileUtil();

    	List<String> factionIndexList = Arrays.asList(fileutil.listFiles(this.getDataFolder() + "/factionData"));
    	List<String> playerIndexList = Arrays.asList(fileutil.listFiles(this.getDataFolder() + "/playerData"));
    	List<String> boardIndexList = Arrays.asList(fileutil.listFiles(this.getDataFolder() + "/boardData"));

    	factionIndex = new ArrayList<String>();
    	playerIndex = new ArrayList<String>();
    	boardIndex = new ArrayList<String>();
    	
    	//display loaded factions and players
    	for(int i=0; i<factionIndexList.size(); i++){
    		String name = factionIndexList.get(i);
    		factionIndex.add(name.replaceFirst(".json", ""));
    		}
    	for(int i=0; i<playerIndexList.size(); i++){
    		String name = playerIndexList.get(i);
    		playerIndex.add(name.replaceFirst(".json", ""));
    		}
    	for(int i=0; i<boardIndexList.size(); i++){
    		String name = boardIndexList.get(i);
    		boardIndex.add(name.replaceFirst(".json", ""));
    		}
    }
    
    /**
     * Saves misc data. Currently empty.
     * */
    public void saveData(){
    	createDirectories();
    }
    
    /**
     * Loads misc data. Currently unnecessary. 
     * */
    public void loadData(){    

    	createDirectories();
    	
    	loadConfig();
    	
    	FileUtil fileutil = new FileUtil();
    	
    	factionIndex = new ArrayList<String>();
    	playerIndex = new ArrayList<String>();
    	boardIndex = new ArrayList<String>();
    	
    	List<String> factionIndexList = Arrays.asList(fileutil.listFiles(this.getDataFolder() + "/factionData"));
    	List<String> playerIndexList = Arrays.asList(fileutil.listFiles(this.getDataFolder() + "/playerData"));
    	List<String> boardIndexList = Arrays.asList(fileutil.listFiles(this.getDataFolder() + "/boardData"));
    	
    	//display loaded factions and players
    	for(int i=0; i<factionIndexList.size(); i++){
    		String name = factionIndexList.get(i);
    		factionIndex.add(name.replaceFirst(".json", ""));
    		}
		Bukkit.getServer().getConsoleSender().sendMessage("§bLoaded all factions.");
    	for(int i=0; i<playerIndexList.size(); i++){
    		String name = playerIndexList.get(i);
    		playerIndex.add(name.replaceFirst(".json", ""));
    		}
		Bukkit.getServer().getConsoleSender().sendMessage("§bLoaded all players.");
    	for(int i=0; i<boardIndexList.size(); i++){
    		String name = boardIndexList.get(i);
    		boardIndex.add(name.replaceFirst(".json", ""));
    		}
		Bukkit.getServer().getConsoleSender().sendMessage("§bLoaded all worlds.");
    }
    
    /**
     * Creates config file.
     * */
    public void createConfigData(){
    	configData = new JSONObject();
    	neutralBreakData = new JSONArray();
    	allyBreakData = new JSONArray();
    	truceBreakData = new JSONArray();
    	otherBreakData = new JSONArray();
    	enemyBreakData = new JSONArray();
    	neutralPlaceData = new JSONArray();
    	allyPlaceData = new JSONArray();
    	trucePlaceData = new JSONArray();
    	otherPlaceData = new JSONArray();
    	enemyPlaceData = new JSONArray();
    	neutralItemData = new JSONArray();
    	allyItemData = new JSONArray();
    	truceItemData = new JSONArray();
    	otherItemData = new JSONArray();
    	enemyItemData = new JSONArray();
		
		neutralPlaceData.put("LAVA_BUCKET");
		allyPlaceData.put("LAVA_BUCKET");
		trucePlaceData.put("LAVA_BUCKET");
		otherPlaceData.put("LAVA_BUCKET");
		enemyPlaceData.put("LAVA_BUCKET");
		
		neutralPlaceData.put("WATER_BUCKET");
		allyPlaceData.put("WATER_BUCKET");
		trucePlaceData.put("WATER_BUCKET");
		otherPlaceData.put("WATER_BUCKET");
		enemyPlaceData.put("WATER_BUCKET");
		
		truceItemData.put("BUCKET");truceItemData.put("WATER_BUCKET");truceItemData.put("LAVA_BUCKET");
			truceItemData.put("WOOD_PLATE"); 
			
		otherItemData.put("BUCKET");otherItemData.put("WATER_BUCKET");otherItemData.put("LAVA_BUCKET");
			otherItemData.put("WOOD_PLATE"); otherItemData.put("WOOD_BUTTON");
			
		enemyItemData.put("BUCKET");enemyItemData.put("WATER_BUCKET");enemyItemData.put("LAVA_BUCKET");
			enemyItemData.put("WOOD_PLATE"); enemyItemData.put("WOOD_BUTTON");
		
		//what still needs to be implemented
		//faction settings
		configData.put("version", version);
		configData.put("default faction description","Please do /sf desc to change this description!"); 
		configData.put("allow player titles", "true");			//#
		configData.put("default player title", "");						
		configData.put("default player rank", "member");				
		configData.put("factions open by default", "true");				 
		configData.put("enforce relations", "");						
		configData.put("friendly fire melee", "false");					
		configData.put("friendly fire projectile (arrows)", "false");  
		configData.put("friendly fire other", "false");				  
		configData.put("default player money", 100.0);				
		configData.put("default player power", 10.0);				
		configData.put("local chat distance", 500);				
		configData.put("faction symbol left", "/");				
		configData.put("faction symbol right", "/");				
		configData.put("seconds before faction is considered really offline", 300);		
		
		//power settings
		configData.put("max player power", 25.0);				
		configData.put("minimum player power", -25.0);			
		configData.put("power lost on death", 2.0);				
		configData.put("update power while online", "true");	
		configData.put("update power while offline", "true");	
		configData.put("update power while in own territory", "true");	
		configData.put("update power while in enemy territory", "true");
		configData.put("update power while enemy in your territory", "true"); 
		configData.put("power per hour while online",   2.0);					
		configData.put("power per hour while offline", -1.0);					
		configData.put("power per hour while in own territory",   2.0);			
		configData.put("power per hour while in enemy territory",   2.0);		
		configData.put("power per hour while enemy in your territory",   -0.25); 
		
		//protection settings
		configData.put("claim size x", chunkSizeX);	
		configData.put("claim size y", chunkSizeY);	
		configData.put("claim size z", chunkSizeZ);	
		configData.put("use 3D claim system?", "true");
		configData.put("protect claimed land from explosions while faction is online", "false"); 
		configData.put("protect claimed land from explosions while faction is offline", "true"); 
		
		configData.put("protect all claimed blocks from being broken in neutral territory", "false");
		configData.put("protect all claimed blocks from being broken in ally territory", "false");
		configData.put("protect all claimed blocks from being broken in truce territory", "true");
		configData.put("protect all claimed blocks from being broken in other territory", "true");
		configData.put("protect all claimed blocks from being broken in enemy territory", "true");
		
		configData.put("protect all claimed blocks from being placed in neutral territory", "false");
		configData.put("protect all claimed blocks from being placed in ally territory", "false");
		configData.put("protect all claimed blocks from being placed in truce territory", "true");
		configData.put("protect all claimed blocks from being placed in other territory", "true");
		configData.put("protect all claimed blocks from being placed in enemy territory", "true");
		
		configData.put("block all item use by default in neutral territory", "false"); 
		configData.put("block all item use by default in ally territory", "false"); 
		configData.put("block all item use by default in truce territory", "true"); 
		configData.put("block all item use by default in other territory", "true"); 
		configData.put("block all item use by default in enemy territory", "true"); 
		
		configData.put("## -- COMMENT: the lists below have the opposite effects of the above settings :COMMENT --##", ""); 
		configData.put("block break protection in neutral land", neutralBreakData);
		configData.put("block break protection in ally land", allyBreakData);
		configData.put("block break protection in truce land", truceBreakData);
		configData.put("block break protection in other land", otherBreakData);
		configData.put("block break protection in enemy land", enemyBreakData);
		configData.put("block place protection in neutral land", neutralPlaceData); 
		configData.put("block place protection in ally land", allyPlaceData);
		configData.put("block place protection in truce land", trucePlaceData); 
		configData.put("block place protection in other land", otherPlaceData); 
		configData.put("block place protection in enemy land", enemyPlaceData); 
		configData.put("item protection in neutral land", neutralItemData); 
		configData.put("item protection in ally land", allyItemData); 
		configData.put("item protection in truce land", truceItemData); 
		configData.put("item protection in other land", otherItemData); 
		configData.put("item protection in enemy land", enemyItemData); 
    }

    /**
     * Loads config data into configData JSONObject.
     * */
    public void loadConfig(){
    	File configFile = new File(this.getDataFolder() + "/config.json");
    	if(!configFile.exists()){
			try {
				FileWriter fw = new FileWriter(configFile);
				BufferedWriter bw=new BufferedWriter(fw);
				
				createConfigData();
				
				bw.write(configData.toString(8));
				bw.newLine();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	try {
    		FileReader filereader = new FileReader(this.getDataFolder() + "/config.json");
			Scanner scan = new Scanner(filereader).useDelimiter("\\Z");
			configData = new JSONObject(scan.next());
			scan.close();
			loadConfigData(); 
			
			if(!configData.getString("version").equals(version)){
				//Bukkit.getServer().getConsoleSender().sendMessage("");
				Bukkit.getServer().getConsoleSender().sendMessage("§cConfig file is out of date! Backing up old config file and creating a new one! Please go and redo your configs with the new format!");

				try {
					File backupFile = new File(configFile.getAbsoluteFile() + ".backup");
					FileWriter filew = new FileWriter(backupFile);
					BufferedWriter baw = new BufferedWriter(filew);
					baw.write(configData.toString(5));
					baw.newLine();
					baw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					FileWriter fw = new FileWriter(configFile);
					BufferedWriter bw=new BufferedWriter(fw);
					
					createConfigData();
					
					bw.write(configData.toString(8));
					bw.newLine();
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    public void loadConfigData(){
    	chunkSizeX = 			configData.getInt("claim size x");
		chunkSizeY =			configData.getInt("claim size y");
		chunkSizeZ = 			configData.getInt("claim size z");
		
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
    }
    
    /**
     * Loads player data into the playerData JSONObject
     * */
    public void loadPlayer(String name){

    	createDirectories();
    	
    	File playerFile = new File(this.getDataFolder() + "/playerData/" + name + ".json");
    	if(!playerFile.exists()){
			try {
				
				FileWriter fw = new FileWriter(playerFile);
				BufferedWriter bw=new BufferedWriter(fw);
				if(Bukkit.getOfflinePlayer(name) == null)
					createPlayer(Bukkit.getPlayer(name));
				else
					createPlayer(Bukkit.getOfflinePlayer(name));
				bw.write(playerData.toString(8));
				bw.newLine();
				bw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	try {

			Scanner scan = new Scanner(new FileReader(this.getDataFolder() + "/playerData/" + name + ".json")).useDelimiter("\\Z");
			
			if(scan.hasNext())
				playerData = new JSONObject(scan.next());
			else{
				scan.close();
				if(Bukkit.getOfflinePlayer(name) == null)
					createPlayer(Bukkit.getPlayer(name));
				else
					createPlayer(Bukkit.getOfflinePlayer(name));
				scan = new Scanner(new FileReader(this.getDataFolder() + "/playerData/" + name + ".json")).useDelimiter("\\Z");
				savePlayer(playerData);
				playerData = new JSONObject(scan.next());
			}
			scan.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }
    
    /**
     * Loads faction data into the factionData JSONObject
     * */
    public void loadFaction(String name){

    	createDirectories();
    	
    	File factionFile = new File(this.getDataFolder() + "/factionData/" + name + ".json");
    	if(!factionFile.exists()){
			try {
				FileWriter fw = new FileWriter(factionFile);
				BufferedWriter bw=new BufferedWriter(fw);
				//factionData = new JSONObject();
				createFaction(name);
				bw.write(factionData.toString(8));
				bw.newLine();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	try {
    		
			Scanner scan = new Scanner(new FileReader(this.getDataFolder() + "/factionData/" + name + ".json")).useDelimiter("\\Z");
			factionData = new JSONObject(scan.next());
			scan.close();
    		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
    	
    }
    
    /**
     * Loads world data into the worldData JSONObject
     * */
    public void loadWorld(String name){

    	createDirectories();
    	
    	File worldFile = new File(this.getDataFolder() + "/boardData/" + name + ".json");
    	if(!worldFile.exists()){
			try {
				FileWriter fw = new FileWriter(worldFile);
				BufferedWriter bw=new BufferedWriter(fw);
				boardData = new JSONObject();
				boardData.put("name", "world");
				bw.write(boardData.toString(8));
				bw.newLine();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	
    	try {

			Scanner scan = new Scanner(new FileReader(this.getDataFolder() + "/boardData/" + name + ".json")).useDelimiter("\\Z");
			boardData = new JSONObject(scan.next());
			scan.close();
    		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
    	
    }
    
    /**
     * Saves playerData into a JSON file
     * */
    public void savePlayer(JSONObject pData){

    	createDirectories();
    	
    	String saveString = pData.toString(8); //save data for player
		try{
			FileWriter fw=new FileWriter(this.getDataFolder() + "/playerData/" + pData.getString("name") + ".json");
			BufferedWriter bw=new BufferedWriter(fw);
			bw.write(saveString);
			bw.newLine();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println("error"+e.getMessage());
		}
    }
    
    /**
     * Saves factionData into a JSON file
     * */
    public void saveFaction(JSONObject fData){

    	createDirectories();
    	
    	String saveString = fData.toString(8); //save data for faction
		try{
			FileWriter fw=new FileWriter(this.getDataFolder() + "/factionData/" + fData.getString("name") + ".json");
			BufferedWriter bw=new BufferedWriter(fw);
			bw.write(saveString);
			bw.newLine();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println("error"+e.getMessage());
		}
    }
    
    /**
     * saves boardData into a JSON file
     * */
    public void saveWorld(JSONObject wData){

    	createDirectories();
    	
    	String saveString = wData.toString(8); //save data for faction
		try{
			FileWriter fw=new FileWriter(this.getDataFolder() + "/boardData/" + wData.get("name").toString() + ".json");
			BufferedWriter bw=new BufferedWriter(fw);
			bw.write(saveString);
			bw.newLine();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println("error"+e.getMessage());
		}
    }
    
    /**
     * Runs any time /sf or /f is picked up
     * */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	String command = cmd.getName().toLowerCase();
    	
    	if(command.equals("sf") || command.equals("f")){ //allow use of /f for legacy/compatibility purposes
    		if(sender instanceof Player){
    			if(args.length<1){
    				sender.sendMessage("§6Try using a command! Example: /sf create faction");
    				return true;
    			}else{
    				if(args[0].toLowerCase().equals("create")){
    					return tryCreateFaction(sender,args);
    				}
    				if(args[0].toLowerCase().equals("claim")){
    					return tryClaim(sender);
    				}
    				if(args[0].toLowerCase().equals("unclaim")){
    					return tryUnClaim(sender);
    				}
    				if(args[0].toLowerCase().equals("unclaimall")){
    					return tryUnClaimAll(sender);
    				}
    				if(args[0].toLowerCase().equals("autoclaim")){
    					return toggleAutoclaim(sender);
    				}
    				if(args[0].toLowerCase().equals("map")){
    					return drawMap(sender);
    				}
    				if(args[0].toLowerCase().equals("power") || args[0].toLowerCase().equals("p") || args[0].toLowerCase().equals("player")){
    					return showPlayerPower(sender, args);
    				}
    				if(args[0].toLowerCase().equals("list")){
    					return listFactions(sender, args);
    				}
    				if(args[0].toLowerCase().equals("sethome")){
    					return trySetHome(sender);
    				}
    				if(args[0].toLowerCase().equals("access")){
    					return setAccess(sender,args);
    				}
    				if(args[0].toLowerCase().equals("enemy")){
    					return setRelation(sender, args, "enemies"); 
    				}
    				if(args[0].toLowerCase().equals("ally")){
    					return setRelation(sender, args, "allies"); 
    				}
    				if(args[0].toLowerCase().equals("truce")){
    					return setRelation(sender, args, "truce"); 
    				}
    				if(args[0].toLowerCase().equals("neutral")){
    					return setRelation(sender, args, "neutral"); 
    				}
    				if(args[0].toLowerCase().equals("home")){
    					return tryHome(sender);
    				}
    				if(args[0].toLowerCase().equals("desc")){
    					return setDesc(sender,args);
    				}
    				if(args[0].toLowerCase().equals("promote")){
    					return setRank(sender,args);
    				}
    				if(args[0].toLowerCase().equals("setrank")){
    					return setRank(sender,args);
    				}
    				if(args[0].toLowerCase().equals("demote")){
    					return setRank(sender,args);
    				}
    				if(args[0].toLowerCase().equals("leader")){
    					return setRank(sender,args);
    				}
    				if(args[0].toLowerCase().equals("kick")){
    					return tryKick(sender, args);
    				}
    				if(args[0].toLowerCase().equals("help")){
    					return showHelp(sender, args);
    				}
    				if(args[0].toLowerCase().equals("top")){
    					return showTop(sender, args);
    				}
    				if(args[0].toLowerCase().equals("chat") || args[0].toLowerCase().equals("c") || args[0].toLowerCase().equals("channel")){
    					return setChatChannel(sender, args);
    				}
    				if(args[0].toLowerCase().equals("invite")){
    					if(args.length>1)
    						return invitePlayer(sender, args[1]);
    					else
    						sender.sendMessage("§cYou must provide the name of the person you wish to invite to your faction!");
    				}
    				if(args[0].toLowerCase().equals("join")){
    					if(args.length>1)
    						return tryJoin(sender, args[1]);
    					else
    						sender.sendMessage("§cYou must provide the name of the faction you want to join!");
    				}
    				if(args[0].toLowerCase().equals("info") || args[0].toLowerCase().equals("f")){
    					if(args.length>1)
    						return displayInfo(sender, args[1]);
    					else
    						return displayInfo(sender, sender.getName());
    				}
    				if(args[0].toLowerCase().equals("leave")){
    					return tryLeave(sender);
    				}
    				if(args[0].toLowerCase().equals("disband")){
    					if(args.length>1)
    						return tryDisband(sender,args[1]);
    					else{
    					
    						loadPlayer(sender.getName());
    						String factionName = playerData.getString("faction");
    						return tryDisband(sender,factionName);
    					}
    				}
    			}
    		}else{
    			sender.sendMessage("§cThe console cannot execute simpleFaction commands!");
    		}
    	}
             
    	return true; 
    }
    
    	/*
  		playerData.put("deaths",0);
  		playerData.put("kills",0);
  		playerData.put("time online",(long) 0.0);
  		*/
    public boolean showTop(CommandSender sender, String[] args){
    	String example = "§7Example usage: §b/sf showtop §7<§btime§7/§bkills§7/§bdeaths§7>";
    	if(args.length>1){
    		String arg = args[1];
    		OfflinePlayer[] offline = Bukkit.getOfflinePlayers();
    		Player[] online = Bukkit.getOnlinePlayers();
    		String[] playerTop = new String[offline.length + online.length + 1];
    		int count = 0;
    		
    		if(arg.equals("kills") || arg.equals("deaths")){
    			int[] value = new int[offline.length + online.length + 1];
    			value[0] = 0;
    			for(int i = 0; i < offline.length; i++){
    				if(!offline[i].isOnline()){
    					count++;
    					loadPlayer(offline[i].getName());
    					playerTop[count] = playerData.getString("name");
    					value[count] = playerData.getInt(arg);
    				}
    			}
    			
    			for(int i = 0; i < online.length; i++){
    				if(online[i].isOnline()){
    					count++;
    					loadPlayer(online[i].getName());
    					playerTop[count] = playerData.getString("name");
    					value[count] = playerData.getInt(arg);
    				}
    			}
    			
    			
    			boolean swapped = true;
    		    int j = 0;
    		    int tmp; String tmp2;
    		    while (swapped) {
    		        swapped = false;
    		        j++;
    		        for (int i = 0; i < value.length - j; i++) {
    		            if (value[i] < value[i + 1]) {
    		                tmp = value[i];
    		                tmp2 = playerTop[i];
    		                value[i] = value[i + 1];
    		                playerTop[i] = playerTop[i + 1];
    		                value[i + 1] = tmp;
    		                playerTop[i + 1] = tmp2;
    		                swapped = true;
    		            }
    		        }
    		    }
    			
    			//for(int i = 0; i < playerTop.length; i++){
    			//	if(value[i] < value[i+1]){
    			//		value[i] = value[i+1];
    			//	}
    			//}
    			
    			String message = "§6Showing top data for §b" + arg + "§6. \n";
    			int howManyToShow = 10;
    			if(playerTop.length<=howManyToShow) howManyToShow = playerTop.length;
    			for(int i = 0; i < howManyToShow; i++){
    				if(playerTop[i] != null)
    					message += "§7" + (i+1) + ". " + playerTop[i] + " (" + value[i] + " " + arg + ") \n";
    			}
    			sender.sendMessage(message);
    			
    		}
    		
    		if(arg.equals("time")){
    			arg = "time online";
    			long[] value = new long[offline.length + online.length + 1];
    			value[0] = 0l;
    			long temp = 0;
    			for(int i = 0; i < offline.length; i++){
    				if(!offline[i].isOnline()){
    					count++;
    					loadPlayer(offline[i].getName());
    					playerTop[count] = playerData.getString("name");
    					value[count] = playerData.getLong(arg);
    				}
    			}
    			
    			for(int i = 0; i < online.length; i++){
    				if(online[i].isOnline()){
    					count++;
    					loadPlayer(online[i].getName());
    					playerTop[count] = playerData.getString("name");;
    					value[count] = playerData.getLong(arg);
    				}
    			}
    			
    			
    			boolean swapped = true;
    		    int j = 0;
    		    long tmp; String tmp2;
    		    while (swapped) {
    		        swapped = false;
    		        j++;
    		        for (int i = 0; i < value.length - j; i++) {
    		            if (value[i] < value[i + 1]) {
    		                tmp = value[i];
    		                tmp2 = playerTop[i];
    		                value[i] = value[i + 1];
    		                playerTop[i] = playerTop[i + 1];
    		                value[i + 1] = tmp;
    		                playerTop[i + 1] = tmp2;
    		                swapped = true;
    		            }
    		        }
    		    }
    		    
    		    
    			String message = "§6Showing top data for §b" + arg + "§6. \n";
    			int howManyToShow = 10;
    			if(playerTop.length<=howManyToShow) howManyToShow = playerTop.length;
    			for(int i = 0; i < howManyToShow; i++){
    				if(playerTop[i] != null)
    					message += "§7" + (i+1) + ". " + playerTop[i] + " (" + (value[i]/1000/60) + " minutes) \n";
    			}
    			sender.sendMessage(message);
		}
    	}else{
    		sender.sendMessage(example);
    	}
    	return true;
    }
    
    /**
     * setAccess() is complicated, read more...
     * Example usage: /sf access <p/r/f> <player/rank/faction> <block> <true/false> (this chunk only <true/false>) 
     * You provide p/r/f (player, rank, or faction) and then the name of the player, rank, or faction.
     * You provide the block or item, and then true/false to show you want it to be allowed or not.
     * You can also add an additional "true" at the end of it, if you want this info to only affect the current chunk.
     * 
     * Using this, you can literally create faction specific ranks and permissions (both globally and for specific chunks).
     * */
    public boolean setAccess(CommandSender sender,String[] args){
    	//argument base   0     1             2               3       4                               5
    	//example /sf access <p/r/f> <player/rank/faction> <block> <true/false> (this chunk only <true/false>) 
    	String example = "§7Example usage: §b/sf access §7<§bp§7/§br§7/§bf§7> " +
				"§7<§bplayer§7/§brank§7/§bfaction§7> §7<§bblock§7> §7<§btrue§7/§bfalse§7> " +
				"(optional, this chunk only §7<§btrue§7/§bfalse§7>)";
    	
    	Location location = Bukkit.getPlayer(sender.getName()).getLocation();
    	int posX = location.getBlockX();
    	int posY = location.getBlockY();
    	int posZ = location.getBlockZ();
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
    	String board = location.getWorld().getName() + "chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ;
    	
    	if(args.length>4){
    		loadPlayer(sender.getName());
    		loadFaction(playerData.getString("faction"));
    	JSONObject chunk = new JSONObject();
    		JSONObject type = new JSONObject();
    			JSONObject subType = new JSONObject();
    				JSONArray allowed = new JSONArray();
    				JSONArray notAllowed = new JSONArray();
    		
    		if(args[1].equals("p") || args[1].equals("r") || args[1].equals("f")){
    			
    			if(args[1].equals("p") && !playerCheck(args[2])){
    				sender.sendMessage("Player not found!");
    				return true;
    			}
    			
    			if(args[1].equals("f") && !factionCheck(args[2])){
    				sender.sendMessage("Faction not found!");
    				return true;
    			}
    			
        		if(factionData.has(args[1])){
        			if(args.length>5){
        				if(factionData.has(board))
        					chunk = factionData.getJSONObject(board);
        			}
        			type = factionData.getJSONObject(args[1]);
        			if(type.has(args[2])){
        				subType = type.getJSONObject(args[2]);
        				if(subType.has("allowed")){
        					allowed = subType.getJSONArray("allowed");
        				}
        				if(subType.has("notAllowed")){
        					notAllowed = subType.getJSONArray("notAllowed");
        				}
        			}
        		}
        		
        		if(args[4].equals("true") || args[4].equals("yes")){
        			allowed.put(args[3].toUpperCase());
        			for(int i = 0; i < notAllowed.length(); i++) 
        				if(notAllowed.get(i).equals(args[3].toUpperCase())) 
        					notAllowed.remove(i);
        		}
        		if(args[4].equals("false") || args[4].equals("no")){
        			notAllowed.put(args[3].toUpperCase());
        			for(int i = 0; i < allowed.length(); i++) 
        				if(allowed.get(i).equals(args[3].toUpperCase())) 
        					allowed.remove(i);
        		}

        		subType.put("allowed", allowed);
        		subType.put("notAllowed", notAllowed);
        		type.put(args[2], subType);
        		if(args.length>5){
        			chunk.put(args[1], type);
            		factionData.put(board, chunk); 
        		}
        		else{
            		factionData.put(args[1], type); 
        		}
        		saveFaction(factionData);
        		
        		String stype = ""; 
        			if(args[1].equals("p")) stype = "§7The player " + Rel_Faction;
        			if(args[1].equals("r")) stype = "§7Members ranked as " + Rel_Faction;
        			if(args[1].equals("f")) stype = "§7The faction " + getFactionRelationColor(factionData.getString("name"),args[2]);
        		String sSubType = args[2];
        		String access = "";
        			if(args[4].equals("true")) access = " §7can now access §a";
        			if(args[4].equals("false")) access = " §7can no longer access §c";
        		String block = args[3].toLowerCase() + "§7";
        		String area = "";
        		if(args.length>5 && !board.equals("")) area = " §7at§6 " + board;
        		messageFaction(factionData.getString("name"),stype + sSubType + access + block + area + "§7.");
        		return true;
    		}else{
        		sender.sendMessage(example);
        		return true;
    		}
    	}
    	else{
    		sender.sendMessage(example);
    		return true;
    	}
    }
    
    /**
     * Sets the rank of a player.
     * */
    public boolean setRank(CommandSender sender, String[] args){
    	// -1  0         1     2
    	// sf setrank player rank
    	// sf promote player
    	loadPlayer(sender.getName());
		String rank = playerData.getString("factionRank");
		String faction = playerData.getString("faction");
		
		if(rank.equals("officer") || rank.equals("leader")){
    	if(args.length>1){
    		if(playerCheck(args[1])){
    			loadPlayer(args[1]);
    			if(!playerData.getString("faction").equals(faction)){
    				sender.sendMessage("§cThis player is not in your faction!");
    				return true;
    			}
    		}
    		else{
    			sender.sendMessage("§cPlayer not found!");
    			return true;
    		}
    		if(args[0].equals("leader")){
    			if(rank.equals("leader")){
        			loadPlayer(args[1]);
    				playerData.put("factionRank", "leader");
        			savePlayer(playerData);
    				messageFaction(faction,Rel_Faction + args[1] + "§a has been promoted to leader.");
    			}
    			else{
    				sender.sendMessage("§cOnly leaders can select new leaders!");
            		return true;
    			}
    		}
    		if(args[0].equals("promote")){
    			loadPlayer(args[1]);
    			playerData.put("factionRank", "officer");
        		savePlayer(playerData);
    			messageFaction(faction,Rel_Faction + args[1] + "§a has been promoted to officer.");
        		return true;
    		}
    		if(args[0].equals("demote")){
    			loadPlayer(args[1]);
    			playerData.put("factionRank", "member");
        		savePlayer(playerData);
    			messageFaction(faction,Rel_Faction + args[1] + "§a has been demoted to member.");
        		return true;
    		}
    		
			if(rank.equals("leader")){
    			loadPlayer(args[1]);
				playerData.put("factionRank", args[2]);
    			savePlayer(playerData);
				messageFaction(faction,Rel_Faction + args[1] + "§a has been set as to a " + args[2] + ".");
    			return true;
			}
			else{
				sender.sendMessage("§cOnly leaders can set custom ranks!");
	    		return true;
			}
    	}
    	else{
    		if(args[0].equals("setrank"))
        		sender.sendMessage("§cInvalid!§7 Correct usage: §b/sf setrank name rank");
    		else
    			sender.sendMessage("§cInvalid!§7 Correct usage: §b/sf promote§6/§bdemote§6/§bleader name");
    		
    		return true;
    	}
    	}else{
    		sender.sendMessage("§cYour rank isn't high enough to do this!");
    		return true;
    	}
    	
    }
    
    /**
     * Sends out a sendMessage to everyone in a certain faction.
     * */
    public void messageFaction(String faction, String message){
    	Player[] on = Bukkit.getOnlinePlayers();
    	for(int i = 0; i<on.length; i++){
    		loadPlayer(on[i].getPlayer().getName());
    		if(playerData.getString("faction").equals(faction)){
    			on[i].getPlayer().sendMessage(message);
    		}
    	}
    }
    
    /**
     * Sends out a sendMessage to the entire server.
     * */
    public void messageEveryone(String message){
    	Player[] on = Bukkit.getOnlinePlayers();
    	for(int i = 0; i<on.length; i++)
    		on[i].getPlayer().sendMessage(message);
    }
    
    /**
     * Sets the description of the faction.
     * */
    public boolean setDesc(CommandSender sender, String[] args){
    	loadPlayer(sender.getName());
    	String faction = playerData.getString("faction");
    	if(faction.equals("")){
    		sender.sendMessage("§cYou are not in a faction!");
    		return true;
    	}
    	if(args.length>1){
    		String desc = "";
    		for(int i = 1; i<args.length; i++)
    			desc += args[i] + " ";
    		loadFaction(faction);
    		factionData.put("desc", desc);
    		saveFaction(factionData);
    		messageFaction(faction, "§7Description updated: §f" + desc);
    		return true;
    	}else{
    		sender.sendMessage("§cPlease provide a description! Example: /sf desc Example Description");
    		return true;
    	}
    }
    
    /**
     * Sets the current chat channel for the sender; custom chat channels supported.
     * */
    public boolean setChatChannel(CommandSender sender, String[] args){
    	if(args.length>1){
    		String rel = Rel_Other;
    		
    		if(args[1].equals("f")) args[1] = "faction";
    		if(args[1].equals("a")) args[1] = "ally";
    		if(args[1].equals("t")) args[1] = "truce";
    		if(args[1].equals("e")) args[1] = "enemy";
    		if(args[1].equals("l")) args[1] = "local";
    		if(args[1].equals("g")) args[1] = "global";
    		if(args[1].equals("p") || args[1].equals("all") || args[1].equals("public")) args[1] = "global";
    		
    		if(args[1].equals("faction")) rel = Rel_Faction;
    		if(args[1].equals("ally")) rel = Rel_Ally;
    		if(args[1].equals("truce")) rel = Rel_Truce;
    		if(args[1].equals("enemy")) rel = Rel_Enemy;
    		if(args[1].equals("local")) rel = Rel_Neutral;
    		
    		//if(args[1].equals("faction") || args[1].equals("ally") || args[1].equals("truce") || args[1].equals("enemy") || args[1].equals("local") || args[1].equals("global")){
    			loadPlayer(sender.getName());
    			playerData.put("chat channel", args[1]);
    			savePlayer(playerData);
    			sender.sendMessage("You have switched to " + rel + args[1] + " chat.");
    		//}
    	}
    	else{
    		sender.sendMessage("Default chat channels: " + Rel_Faction + "faction, " + Rel_Ally + "ally, " + 
    				Rel_Truce + "truce, " + Rel_Enemy + "enemy, " + Rel_Neutral + "local, " + Rel_Other + "global.");
    	}
    	
    	return true;
    }
    
    /**
     * Attempts to kick a player from their faction.
     * */
    public boolean tryKick(CommandSender sender, String[] args){
    	if(args.length>1){
    		loadPlayer(sender.getName());
    		String faction = playerData.getString("faction");
    		String rank = playerData.getString("factionRank");
    		if(faction.equals("")){
    			sender.sendMessage("You are not in a faction!");
    			return true;
    		}
    		else{
    			if(playerCheck(args[1])){
    				loadPlayer(args[1]);
    				if(!playerData.getString("faction").equals(faction)){
    					sender.sendMessage("Player not in your faction!");
    					return true;
    				}
    				else{
    					if(!rank.equals("leader") && !rank.equals("officer")){
    						sender.sendMessage("You are not a high enough rank to kick players!");
    						return true;
    					}
    					else{
    						playerData.put("faction", "");
    						savePlayer(playerData);
    						Bukkit.getPlayer(playerData.getString("name")).sendMessage("You have been kicked from your faction!");
    			    		messageFaction(faction,Rel_Other + playerData.getString("name") + "§7 kicked from faction by " + Rel_Faction + sender.getName());
    						//sender.sendMessage("Player kicked from faction!");
    						return true;
    					}
    				} 
    			}
    			else{
    				sender.sendMessage("Player not found!");
    				return true;
    			}
    		}
    	}
    	else{
    		sender.sendMessage("Please specify a player that you wish to kick from your faction.");
    	}
    	return true;
    }
    
    /**
     * Displays information about the player, their faction, and their various stats.
     * */
    public boolean showPlayerPower(CommandSender sender, String[] args){
    	String player = sender.getName();
    	if(args.length>1){
    		if(playerCheck(args[1]))
    			player = args[1];
    		else
    			sender.sendMessage("§cPlayer not found.");
    	}
    	DecimalFormat df = new DecimalFormat("0.###");
    	loadPlayer(sender.getName());
    	String faction1 = playerData.getString("faction");
    	loadPlayer(player);
    	String faction2 = playerData.getString("faction");
    	sender.sendMessage("§7 ------ [" + getFactionRelationColor(faction1,faction2) + player + "§7] ------ ");
    	sender.sendMessage("§6Power: §f" + df.format(playerData.getDouble("power")));
    	if(!playerData.getString("faction").equals("")) 
    		sender.sendMessage(getFactionRelationColor(faction1,faction2) + configData.getString("faction symbol left") + faction2 + 
    				configData.getString("faction symbol right") + "'s §6power: " + getFactionClaimedLand(faction2) + "/" + 
    				df.format(getFactionPower(faction2)) + "/" + df.format(getFactionPowerMax(faction2)));
    	sender.sendMessage("§6Gaining §f" + df.format(configData.getDouble("power per hour while online")) + "§6 power an hour while online.");
    	sender.sendMessage("§6Losing §f" + df.format(-1*configData.getDouble("power per hour while offline")) + "§6 power an hour while offline.");
    	loadPlayer(player);
    	sender.sendMessage("§6Kills: " + playerData.getInt("kills"));
    	sender.sendMessage("§6Deaths: " + playerData.getInt("deaths"));
    	
    	Long timeOnline = playerData.getLong("time online");
    	int seconds = (int) (timeOnline / 1000) % 60 ;
    	int minutes = (int) ((timeOnline / (1000*60)) % 60);
    	int hours   = (int) ((timeOnline / (1000*60*60)) % 24);
    	sender.sendMessage("§6Time on server: " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds.");
    	
    	Long lastOnline = playerData.getLong("last online");
    	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        Date resultdate = new Date(lastOnline);
    	sender.sendMessage("§6Last online: " + sdf.format(resultdate) + " (server time)");
    	
    	return true;
    }
    
    /**
     * Returns whether or not the player exists on the server.
     * */
    public boolean playerCheck(String name){
    	createDirectories();
    	
    	File factionFile = new File(this.getDataFolder() + "/playerData/" + name + ".json");
    	if(factionFile.exists()){
			return true;
		}
    	
    	return false;
    }
    
    /**
     * Returns whether or not the faction exists on the server.
     * */
    public boolean factionCheck(String faction){
    	createDirectories();
    	
    	File factionFile = new File(this.getDataFolder() + "/factionData/" + faction + ".json");
    	if(factionFile.exists()){
			return true;
		}
    	
    	return false;
    }
    
    /**
     * Attempts to set a relation with another faction.
     * */
    public boolean setRelation(CommandSender sender, String[] args, String relation){
    	if(args.length>1){
    		if(factionCheck(args[1])){

				String relString = "";
				if(relation.equals("enemies")) relString = Rel_Enemy;
				if(relation.equals("allies")) relString = Rel_Ally;
				if(relation.equals("truce")) relString = Rel_Truce;
				if(relation.equals("neutral")) relString = Rel_Other;
				
    			if(!relation.equals("neutral")){
    				
    				loadPlayer(sender.getName());
    				loadFaction(playerData.getString("faction"));
    				enemyData = factionData.getJSONArray(relation); //says enemy data; but it can be any of them. Just a free array
    				int k = 0;
    				for(int i = 0; i < enemyData.length(); i++){ //make sure we don't already have this relation set
    					if(enemyData.getString(i).equals(args[1]))
    						k++;
    				}
    				if(k<1){
    					enemyData.put(args[1]);
        				factionData.put(relation, enemyData);
        				saveFaction(factionData);
        				
        				loadFaction(playerData.getString("faction"));
        				enemyData = factionData.getJSONArray("enemies");
        				allyData = factionData.getJSONArray("allies");
        				truceData = factionData.getJSONArray("truce");
    					if(!relation.equals("enemies")){
        					for(int i = 0; i < enemyData.length(); i++){
        						if(enemyData.getString(i).equals(args[1]))
        							enemyData.remove(i);
        					}
        				}
        				if(!relation.equals("allies")){
        					for(int i = 0; i < allyData.length(); i++){
        						if(allyData.getString(i).equals(args[1]))
        							allyData.remove(i);
        					}
        				}
        				if(!relation.equals("truce")){
        					for(int i = 0; i < truceData.length(); i++){
        						if(truceData.getString(i).equals(args[1]))
        							truceData.remove(i);
        					}
        				}
        				
        				factionData.put("enemies", enemyData);
        				factionData.put("allies", allyData);
        				factionData.put("truce", truceData);
        				saveFaction(factionData);
        				
    				}else{
    					sender.sendMessage("§cYou have already this relation set!");
    					return true;
    				}
    				
    				
    				
    				loadFaction(args[1]);
    				enemyData = factionData.getJSONArray("enemies");
    				allyData = factionData.getJSONArray("allies");
    				truceData = factionData.getJSONArray("truce");
    				
    				if(!relation.equals("allies")){
    					for(int i = 0; i < allyData.length(); i++){
    						if(allyData.getString(i).equals(playerData.getString("faction")))
    							allyData.remove(i);
    					}
    				}
    				if(!relation.equals("truce")){
    					for(int i = 0; i < truceData.length(); i++){
    						if(truceData.getString(i).equals(playerData.getString("faction")))
    							truceData.remove(i);
    					}
    				}
    				
    				
					if(relation.equals("enemies")){
	    				int m = 0;
	    				for(int i = 0; i < enemyData.length(); i++){ //make sure we don't already have this relation set
	    					if(enemyData.getString(i).equals(playerData.getString("faction")))
	    						m++;
	    				}
	    				
	    				if(m<1){
							enemyData.put(playerData.getString("faction"));
	    				}
					}

    				factionData.put("enemies", enemyData);
    				factionData.put("allies", allyData);
    				factionData.put("truce", truceData);
					saveFaction(factionData);
					
    				int j = 0;
    				
    				if(relation.equals("enemies"))
    					for(int i = 0; i < enemyData.length(); i++)
    						if(enemyData.getString(i).equals(playerData.getString("faction")))
    							j++;
    				
    				if(relation.equals("allies"))	
    					for(int i = 0; i < allyData.length(); i++)
    						if(allyData.getString(i).equals(playerData.getString("faction")))
    							j++;
    				
    				if(relation.equals("truce"))		
    					for(int i = 0; i < truceData.length(); i++)
    						if(truceData.getString(i).equals(playerData.getString("faction")))
    							j++;
    				
    				if(j>0 || (j==0 && relation.equals("neutral"))){
    					messageFaction(playerData.getString("faction"),"§6You are now " + relString + relation + "§6 with " + 
    							getFactionRelationColor(playerData.getString("faction"),args[1]) + configData.getString("faction symbol left") + args[1] + 
    							configData.getString("faction symbol right") + "§6.");
    					//sender.sendMessage();
    					return true;
    				}
    				if(j==0){

			    		messageFaction(playerData.getString("faction"),"§6You have asked " + getFactionRelationColor(playerData.getString("faction"),args[1]) + 
    							configData.getString("faction symbol left") + args[1] + configData.getString("faction symbol right") + " §6if they would like to become " + relString + relation + "§6.");
    					//sender.sendMessage();
    					return true;
    				}
    				
    			}
    			else{
    				loadPlayer(sender.getName());
    				loadFaction(playerData.getString("faction"));
    				enemyData = factionData.getJSONArray("enemies");
    				allyData = factionData.getJSONArray("allies");
    				truceData = factionData.getJSONArray("truce");
    				
    				int k = 0;
    				
    				for(int i = 0; i < enemyData.length(); i++)
    					if(enemyData.getString(i).equals(args[1])){
    						enemyData.remove(i);
    						k++;
    						}
    				for(int i = 0; i < allyData.length(); i++)
    					if(allyData.getString(i).equals(args[1])){
    						allyData.remove(i);
    						k++;
    						}
    				for(int i = 0; i < truceData.length(); i++)
    					if(truceData.getString(i).equals(args[1])){
    						truceData.remove(i);
    						k++;
    						}
    				
    				if(k==0){
    					sender.sendMessage("§6You are already neutral with " + getFactionRelationColor(playerData.getString("faction"),args[1]) + 
    							configData.getString("faction symbol left") + args[1] + configData.getString("faction symbol right") + "§6!");
    					return true;
    				}
    				
    				factionData.put("enemies", enemyData);
    				factionData.put("allies", allyData);
    				factionData.put("truce", truceData);
    				saveFaction(factionData);
    				
    				loadFaction(args[1]);
    				enemyData = factionData.getJSONArray("enemies");
    				allyData = factionData.getJSONArray("allies");
    				truceData = factionData.getJSONArray("truce");
    				
    				int j = 0;
    				
    				for(int i = 0; i < enemyData.length(); i++)
    					if(enemyData.getString(i).equals(playerData.getString("faction"))){
    						j++;
    						}
    				for(int i = 0; i < allyData.length(); i++)
    					if(allyData.getString(i).equals(playerData.getString("faction"))){
    						j++;
    						}
    				for(int i = 0; i < truceData.length(); i++)
    					if(truceData.getString(i).equals(playerData.getString("faction"))){
    						j++;
    						}
    				
    				if(k>0 && j==0){
    					sender.sendMessage("§6You are now neutral with " + getFactionRelationColor(playerData.getString("faction"),args[1]) + 
    							configData.getString("faction symbol left") + args[1] + configData.getString("faction symbol right") + "§6.");
    					return true;
    				}
    				if(k>0 && j>0){
    					sender.sendMessage("§6You have asked " + getFactionRelationColor(playerData.getString("faction"),args[1]) +
    							configData.getString("faction symbol left") + args[1] + configData.getString("faction symbol right") + 
    							"§6 if they would like to become " + relString + "neutral.");
    					return true;
    				}
    			}
    		}
    		else{
    			sender.sendMessage("§cThis faction doesn't exist!");
    		}
    	}
    	else{
    		sender.sendMessage("§cYou must provide the name of the faction that you wish to enemy! Example: /sf enemy factionName");
    	}
    	return true;
    }
    
    /**
     * Returns faction power information.
     * */
    public int getFactionClaimedLand(String faction){
    	int claimedLand = 0;
    	loadWorld("world");
    	JSONArray array = boardData.names();
    	for(int i = 0; i < array.length(); i++){
    		String name = array.getString(i);
    		if(boardData.getString(name).equals(faction)){
    			claimedLand++;
    		}
    	}
    	return claimedLand;
    }
    public double getFactionPowerMax(String faction){
    	double factionPower = 0;
    	
		OfflinePlayer[] off = Bukkit.getOfflinePlayers();
		OfflinePlayer[] on = Bukkit.getOnlinePlayers();
		
		for(int i = 0; i < off.length; i++){
			if(!off[i].isOnline()) {
				loadPlayer(off[i].getName());
				if(playerData.getString("faction").equals(faction)){
					factionPower += configData.getDouble("max player power");
				}
			}
		}
		
		for(int i = 0; i < on.length; i++){
			if(on[i].isOnline()) {
				loadPlayer(on[i].getName());
				if(playerData.getString("faction").equals(faction)){
					factionPower += configData.getDouble("max player power");
				}
			}
		}
    	
    	return factionPower;
    }
    public double getFactionPower(String faction){
    	double factionPower = 0;
    	
		OfflinePlayer[] off = Bukkit.getOfflinePlayers();
		OfflinePlayer[] on = Bukkit.getOnlinePlayers();
		
		for(int i = 0; i < off.length; i++){
			if(!off[i].isOnline()) {
				loadPlayer(off[i].getName());
				if(playerData.getString("faction").equals(faction)){
					factionPower += playerData.getDouble("power");
				}
			}
		}
		
		for(int i = 0; i < on.length; i++){
			if(on[i].isOnline()) {
				loadPlayer(on[i].getName());
				if(playerData.getString("faction").equals(faction)){
					factionPower += playerData.getDouble("power");
				}
			}
		}
    	
    	return factionPower;
    }
    
    /**
     * Toggles autoclaim. While on, players can run around claiming land they touch.
     * */
    public boolean toggleAutoclaim(CommandSender sender){
    	loadPlayer(sender.getName());
    	if(playerData.has("autoclaim")){
    		String claim = playerData.getString("autoclaim");
    		if(claim.equals("true"))
    			playerData.put("autoclaim", "false");
    		else
    			playerData.put("autoclaim", "true");
    	}
    	else{
    		playerData.put("autoclaim", "true");
    	}
    	
    	savePlayer(playerData);
    	sender.sendMessage("§aAutoclaim toggled.");
    	return true;
    }
    
    /**
     * Shows a paged help thing in chat
     * */
    public boolean showHelp(CommandSender sender, String[] args){
    	int page = 0;
    	if(args.length>1){
    		Scanner scan = new Scanner(args[1]);
    		if(scan.hasNextInt()){
    			page = scan.nextInt();
    			page--;
    			if(page<0) page = 0;
    		}
    		else{
    			sender.sendMessage("§cPlease provide a page number. Example: /sf help 2");
        		scan.close();
    			return true;
    		}
    		scan.close();
    	}
    	String helpMessage =  "  §6 /sf - §aBase command." + "\n";
    		   helpMessage += " §6 help (page)- §aList of commands (wip)" + "\n";
    		   helpMessage += " §6 create (name) - §aCreate a faction with specified name" + "\n";
    		   helpMessage += " §6 join (name) - §aJoin a faction with specified name" + "\n";
    		   helpMessage += " §6 invite (name) - §aInvite a player to your faction." + "\n";
    		   helpMessage += " §6 disband (§f(optional)§6name) - §aDisband a faction." + "\n";
    		   helpMessage += " §6 leave - §aLeave your faction." + "\n";
    		   helpMessage += " §6 kick (name) - §aKicks member from faction.." + "\n";
    		   
    		   //2
    		   helpMessage += " §6 claim - §aClaims a chunk of land for your faction." + "\n";
    		   helpMessage += " §6 sethome - §aSet a warp home and respawn point for faction." + "\n";
    		   helpMessage += " §6 home - §aTeleport home." + "\n";
    		   helpMessage += " §6 map - §aDraws a map of surrounding faction land." + "\n";
    		   helpMessage += " §6 info (name) - §aShows info on a faction." + "\n";
    		   helpMessage += " §6 player (name) - §aShow info on a player." + "\n";
    		   helpMessage += " §6 list (page) - §aCreates a list of Factions." + "\n";
    		   helpMessage += " §6 autoclaim - §aToggles autoclaiming of land." + "\n";
    		   
    		   //3
    		   helpMessage += " §6 chat (channel) - §aSwitches to specified channel." + "\n";
    		   helpMessage += " §a(also works with abbreviations such as §b/sf c g§a" + "\n";
    		   helpMessage += " §aor §b/sf c f§a. Custom channels supported.)" + "\n";
    		   helpMessage += " \n";
    		   helpMessage += " §6Claims explination:§a In order to claim someone" + "\n";
    		   helpMessage += " §aelse's land, their land claimed must be higher " + "\n";
    		   helpMessage += " §athan their current power! Kill them to lower their" + "\n";
    		   helpMessage += " §apower in order to claim their land." + "\n";
    	
    		   //4
    		   helpMessage += " §6 access type(p/r/f) name(player/rank/faction) " + "\n"; 
    		   helpMessage += " §6 block(block) allow(true/false) thisChunkOnly(true/false) " + "\n"; 
    		   helpMessage += " §a - This very powerful command will allow you to edit  " + "\n"; 
    		   helpMessage += " §a permissions to your liking, within your faction!  " + "\n"; 
    		   helpMessage += " \n"; 
    		   helpMessage += " §6 promote (player) - §aPromotes player to officer." + "\n"; 
    		   helpMessage += " §6 demote (player) - §aDemotes player to member." + "\n"; 
    		   helpMessage += " §6 leader (player) - §aAdds leader to faction." + "\n";

    		   //5
    		   helpMessage += " §6 setrank (name) (rank) - §aYou can specify a" + "\n"; 
    		   helpMessage += "  §aspecific rank to give a player. You can even" + "\n"; 
    		   helpMessage += "  §ause custom rank names (with /sf access) to " + "\n"; 
    		   helpMessage += "  §acreate entirely new faction ranks!" + "\n"; 
    		   helpMessage += " \n"; 
    		   helpMessage += " §6 (§dCoty loves you :3c§6)" + "\n"; 
    		   
    		   
    		   
    	int lineCount = 0;
    	int pageCount = 0;
    	String pageToDisplay = "";
    	
    	for(int i = 1; i < helpMessage.length(); i++){
    		if(pageCount == page) pageToDisplay+= helpMessage.charAt(i);
    		if(helpMessage.substring(i-1, i).equals("\n")){
    			lineCount++;
    			if(lineCount>7){
    				lineCount = 0;
    				pageCount++;
    				pageToDisplay+=" ";
    			}
    		}
    	}
    	
    	if(page>pageCount) {
    		sender.sendMessage("§cThere are only " + (pageCount+1) + " help pages!");
    	}
    	sender.sendMessage("§6simpleFactions help - page " + (page+1) + "/ " + (pageCount+1) + " \n" + pageToDisplay);
    	return true;
    }
    
    /**
     * Attempts to set a home on the specified block.
     * */
    public boolean trySetHome(CommandSender sender){
    	loadPlayer(sender.getName());
    	Player player = Bukkit.getPlayer(sender.getName());
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equals("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}
    	
    	String world = player.getLocation().getWorld().getName().toString();
    	double posX = player.getLocation().getX();
    	double posY = player.getLocation().getY()+1;
    	double posZ = player.getLocation().getZ();
    	

    	Location loc = new Location(Bukkit.getWorld(world), posX, posY, posZ);
    	Block block = loc.getBlock();
    	if(block.getRelative(BlockFace.UP).getType() != Material.AIR){
    		sender.sendMessage("§cMake more empty space above you, and then try setting a home again.");
    	}
    	
    	loadPlayer(player.getName());
    	loadFaction(playerData.getString("faction"));
    	factionData.put("home",world + " " + posX + " " + posY + " " + posZ);
    	saveFaction(factionData);
		sender.sendMessage("§7You have set your faction home at §6" + (int)posX + "," + (int)posY + "," + (int)posZ + "§7.");
    	return true;
    }
    
    /**
     * Tries to teleport home. If the home is blocked, attempts to find the next best spot.
     * */
    public boolean tryHome(CommandSender sender){
    	loadPlayer(sender.getName());
    	Player player = Bukkit.getPlayer(sender.getName());
    	String factionName = playerData.getString("faction");
    	if(factionName.equals("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}

    	loadPlayer(sender.getName());
    	loadFaction(playerData.getString("faction"));
    	String home = factionData.getString("home");
    	
    	if(home.equals("")){
    		sender.sendMessage("Your faction doesn't have a home!");
    		return true;
    	}
    	Scanner scan = new Scanner(home);
    	String world = scan.next();
    	double x = scan.nextDouble();
    	double y = scan.nextDouble();
    	double z = scan.nextDouble();
    	scan.close();
    	
    	Location loc = new Location(Bukkit.getWorld(world), x, y, z);

    	Block block = loc.getBlock();
    	int i = 0;
    	while(block.getRelative(BlockFace.DOWN).getType() != Material.AIR || block.getType() != Material.AIR){
    		block = block.getRelative((int) (Math.random()*10), (int)(Math.random()*10), (int)(Math.random()*10));
    		//sender.sendMessage("§cUnsafe! §6Trying next block! Block type here is " + block.getType().toString() + " at " + block.getLocation().toString());
    		i++;
    		if(i>9){
    			sender.sendMessage("§cArea is to unsafe to teleport to.");
    			return true;
    		}
    	}
    	
    	loc = block.getLocation();
    	player.teleport(loc);
		sender.sendMessage("§7Teleported home.");
    	return true;
    }
    
    /**
     * Invites a player to a faction
     * */
    public boolean invitePlayer(CommandSender sender, String invitedPlayer){
    	
    	if(sender.getName().equals(invitedPlayer)){
    		sender.sendMessage("§cepic.");
    		return true;
    	}
    	
    	loadPlayer(sender.getName());
    	String faction = playerData.getString("faction");
    	loadFaction(faction);
    	
    	String rank = playerData.getString("factionRank");
    	if(rank.equals("officer") || rank.equals("leader")){
        	inviteData = factionData.getJSONArray("invited");
        	inviteData.put(invitedPlayer.toLowerCase());
        	factionData.put("invited", inviteData);
        	saveFaction(factionData);
        	
        	sender.sendMessage("§6You have invited §f" + invitedPlayer + "§6 to your faction!");
        	loadPlayer(invitedPlayer);
        	String factionString = getFactionRelationColor(playerData.getString("faction"),faction) + configData.getString("faction symbol left") + faction + configData.getString("faction symbol right");
        	Bukkit.getPlayer(invitedPlayer).sendMessage("§6You have been invited to " + factionString + 
        			"§6. Type §b/sf join " + faction + "§6 in order to accept");
    	}else{
    		sender.sendMessage("§cYou are not ranked high enough to invite members.");
    		return true;
    	}

    	return true;
    }
    
    /**
     * Display information on a faction or player's faction.
     * */
    public boolean displayInfo(CommandSender sender, String name){
    	
    	if(factionCheck(name)){
    		loadFaction(name);
    		sender.sendMessage(factionInformationString(sender,name));
    		return true;
    	}
    	
    	if(playerCheck(name)){
    		loadPlayer(name);
    		if(!playerData.getString("faction").equals(""))
    			sender.sendMessage(factionInformationString(sender,playerData.getString("faction")));
    		else
    			sender.sendMessage(name + " is not in a faction!");
    		return true;
    	}
    	
    	sender.sendMessage("Faction or player not found!");
    	return true;
    }
    
    public boolean isFactionOnline(String faction){
		loadFaction(faction);
		OfflinePlayer[] on = Bukkit.getOnlinePlayers();
		for(int i = 0; i < on.length; i++){
			loadPlayer(on[i].getName());
			if(playerData.getString("faction").equals(faction) && on[i].isOnline()) {
				factionData.put("lastOnline", System.currentTimeMillis());
				saveFaction(factionData);
				return true; //if anyone from faction is online, break from loop and return true; update last online time
			}
		}
    	
		if(factionData.getLong("lastOnline")+ (configData.getInt("seconds before faction is considered really offline") * 1000) > System.currentTimeMillis()){
			return true;
		}
		
    	return false;
    }
    
    /**
     * Gather information on a faction and put it into a string.
     * */
    public String factionInformationString(CommandSender sender, String faction){
    	loadPlayer(sender.getName());
    	String viewingFaction = playerData.getString("faction");
    	loadFaction(faction);
    	DecimalFormat df = new DecimalFormat("0.###");
    	
    	String truce = "";
    	String ally = "";
    	String enemy = "";
    	
    	enemyData = factionData.getJSONArray("enemies");
    	truceData = factionData.getJSONArray("truce");
    	allyData = factionData.getJSONArray("allies");
    	
    	for(int i = 0; i<enemyData.length(); i++){
    		String rel = getFactionRelationColor(faction, enemyData.getString(i));
    		if(rel.equals(Rel_Enemy)){
    			enemy += ", " + configData.getString("faction symbol left") + enemyData.getString(i) + configData.getString("faction symbol right");// enemyData.getString(i);
    		}
    	}
    	
    	for(int i = 0; i<factionIndex.size(); i++){
    		if(!enemy.contains(factionIndex.get(i) + ",") && !enemy.contains(", " + factionIndex.get(i)) ){
    			loadFaction(factionIndex.get(i));
    			enemyData = factionData.getJSONArray("enemies");
    			for(int l = 0; l<enemyData.length(); l++) 
    				if(enemyData.getString(l).equals(faction)) 
    					enemy += ", " + configData.getString("faction symbol left") + factionIndex.get(i) + configData.getString("faction symbol right");
    		}
    	}

    	enemy = enemy.replaceFirst(",","");
    	
    	for(int i = 0; i<truceData.length(); i++){
    		String rel = getFactionRelationColor(faction, truceData.getString(i));
    		if(rel.equals(Rel_Truce)){
    			truce += ", " + configData.getString("faction symbol left") + truceData.getString(i) + configData.getString("faction symbol right"); //truceData.getString(i);
    		}
    	}
    	truce = truce.replaceFirst(",","");

    	for(int i = 0; i<allyData.length(); i++){
    		String rel = getFactionRelationColor(faction, allyData.getString(i));
    		if(rel.equals(Rel_Ally)){
    			ally += ", " + configData.getString("faction symbol left") + allyData.getString(i) + configData.getString("faction symbol right");// + allyData.getString(i);
    		}
    	}
    	ally = ally.replaceFirst(",","");

    	loadFaction(faction);
    	String factionInfo = "";
    	factionInfo += "§6---- " + getFactionRelationColor(viewingFaction,faction) + 
    			configData.getString("faction symbol left") + faction + configData.getString("faction symbol right") + "§6 ---- \n";
    	factionInfo += "§6" + factionData.getString("desc") + "\n§6";
    	
    	factionInfo += "§6Power: " + getFactionClaimedLand(faction) + "/" + df.format(getFactionPower(faction)) + "/" + df.format(getFactionPowerMax(faction)) + "\n";
    	
    	String isOnline = "§coffline";
    	
    	if(isFactionOnline(faction))
    		isOnline = "§bonline";
    	
    	factionInfo += "§6This faction is " + isOnline + "§6.\n";
    	
    	if(isOnline.equals("§coffline")){
    		long time = factionData.getLong("lastOnline") - System.currentTimeMillis() - (configData.getInt("seconds before faction is considered really offline") * 1000);
    		int seconds = (int) (-time/1000);
    		factionInfo += "§6Has been offline for " + (seconds) + " seconds. \n";
    	}else{
    		factionInfo += "§6Faction will become §coffline§6 if no members are §bonline§6 for " + (((factionData.getLong("lastOnline") - System.currentTimeMillis())/1000) + configData.getInt("seconds before faction is considered really offline")) + "§6 more seconds. \n";
    	}
    	
    	if(!ally.equals("")) factionInfo += "§dAlly: " + ally.replace("]", "").replace("[", "").replace("\"", "") + "\n§6";
    	if(!truce.equals("")) factionInfo += "§6Truce: " + truce.replace("]", "").replace("[", "").replace("\"", "") + "\n§6";
    	if(!enemy.equals("")) factionInfo += "§cEnemy: " + enemy.replace("]", "").replace("[", "").replace("\"", "") + "\n§6";
    	
    	String members = "";
    	String offMembers = "";
		OfflinePlayer[] off = Bukkit.getOfflinePlayers();
		OfflinePlayer[] on = Bukkit.getOnlinePlayers();
		for(int i = 0; i < off.length; i++){
			loadPlayer(off[i].getName());
			if(playerData.getString("faction").equals(faction) && !off[i].isOnline()) {
				if(!offMembers.equals("")) 
					offMembers+= ", ";
				offMembers+=off[i].getName();
			}
		}
		for(int i = 0; i < on.length; i++){
			loadPlayer(on[i].getName());
			if(playerData.getString("faction").equals(faction) && on[i].isOnline()) {
				if(!members.equals("")) 
					members+= ", ";
				members+=on[i].getName();
			}
		}
    	
    	if(!members.equals("")) factionInfo += "§6Online: " + members + "\n";
    	if(!offMembers.equals("")) factionInfo += "§6Offline: " + offMembers + "\n";
    	
    	return factionInfo;
    }
    
    /**
     * Try to join the supplied faction.
     * */
    public boolean tryJoin(CommandSender sender, String faction){
    	if(factionIndex.contains(faction)){
    		loadPlayer(sender.getName());
    		if(playerData.getString("faction").equals("")){
    			loadFaction(faction);
    			inviteData = factionData.getJSONArray("invited");
    			
    			if(inviteData.toString().contains(sender.getName().toLowerCase()) || factionData.getString("open").equals("true")){
        			playerData.put("faction", faction);
        			playerData.put("factionRank", configData.getString("default player rank"));
        			savePlayer(playerData); 
        			sender.sendMessage("§6You have joined " + Rel_Faction + playerData.getString("faction") + "§6!");
        			messageFaction(faction,Rel_Faction + sender.getName() + "§6 has joined your faction!");
        			return true;
    			}
    			else{
        			sender.sendMessage("§cThis faction is either not open or has not invited you!");
        			return true;
    			}
    			
    		}
    		else
    			sender.sendMessage("§cYou must leave your current faction first! Do /sf leave");
    	}else{
    		sender.sendMessage("§cThe faction §f" + configData.getString("faction symbol left") + faction + configData.getString("faction symbol right") + " §cdoes not exist!");
    	}
    	
    	return true;
    }
    
    /**
     * List all factions on the server.
     * */
    public boolean listFactions(CommandSender sender, String[] args){
    	int page = 0;
    	DecimalFormat df = new DecimalFormat("0.###");
    	String filter = "";
    	if(args.length>1){
    		Scanner scan = new Scanner(args[1]);
    		if(scan.hasNext() && !scan.hasNextInt()){
    			filter = scan.next();
    			filter = filter.toLowerCase();
    			if(!filter.equals("ally") && !filter.equals("enemy") && !filter.equals("truce")){
    				sender.sendMessage("§cInvalid filter!§7 Example: §b/sf list ally");
    				scan.close();
    				return true;
    			}
    		}
    		if(scan.hasNextInt()){
    			page = scan.nextInt();
    			page--;
    			if(page<0) page = 0;
    		}
    		else{
    			if(filter.equals("")){
    				sender.sendMessage("§cPlease provide a page number.§7 Example: §b/sf list 2");
        			scan.close();
    				return true;
    			}
    		}
    		scan.close();
    	}
    	String factionList = "";

    	if(factionIndex.size() == 0){
    		sender.sendMessage("§6There are no factions to show!");
    		return true;
    	}
    	if(factionIndex.size() == 1)
    		sender.sendMessage("§6Only one faction exsists on this server.");
    	else
    		sender.sendMessage("§6There are " + factionIndex.size() + " factions on this server.");
    	
    	loadPlayer(sender.getName());
    	String factionName = playerData.getString("faction");
    		factionList += "  " + getFactionRelationColor(factionName,factionName) + "" + configData.getString("faction symbol left") + factionName + configData.getString("faction symbol right")
    			+ " "  + getFactionClaimedLand(factionName) + "/" + df.format(getFactionPower(factionName)) + "/" + df.format(getFactionPowerMax(factionName))   +"" + "§7 <-- you\n";
    	
    	for(int i=0; i<factionIndex.size(); i++){
    		String name = factionIndex.get(i);
        	String Rel = "";
        	if(filter.equals("ally")) Rel = Rel_Ally;
        	if(filter.equals("enemy")) Rel = Rel_Enemy;
        	if(filter.equals("truce")) Rel = Rel_Truce;
        	if(filter.equals("")) Rel = getFactionRelationColor(factionName,name);
        		
        	if(!factionIndex.get(i).equals(factionName) && Rel.equals(getFactionRelationColor(factionName,name)))
        		factionList += "  " + getFactionRelationColor(factionName,name) + "" + configData.getString("faction symbol left") + name + configData.getString("faction symbol right")
        			+ " " + getFactionClaimedLand(name) + "/" + df.format(getFactionPower(name)) + "/" + df.format(getFactionPowerMax(name))   + "\n";
        }
    	
    	int lineCount = 0;
    	int pageCount = 0;
    	String pageToDisplay = "";
    	
    	for(int i = 1; i < factionList.length(); i++){
    		if(pageCount == page) pageToDisplay+= factionList.charAt(i);
    		if(factionList.substring(i-1, i).equals("\n")){
    			lineCount++;
    			if(lineCount>7){
    				lineCount = 0;
    				pageCount++;
    				pageToDisplay+=" ";
    			}
    		}
    	}
    	
    	if(page>pageCount) {
    		sender.sendMessage("§cThere are only " + (pageCount+1) + " faction list pages!");
    	}
    	
    	if(filter.equals("ally")) sender.sendMessage("§6Filter: " + Rel_Ally + "Ally");
    	if(filter.equals("truce")) sender.sendMessage("§6Filter: " + Rel_Truce + "Truce");
    	if(filter.equals("enemy")) sender.sendMessage("§6Filter: " + Rel_Enemy + "Enemy");
    	sender.sendMessage("§6Faction List - page " + (page+1) + "/ " + (pageCount+1) + " \n" + pageToDisplay);
    	return true;
    }
    
    /**
     * Display a text map of the surround faction claims.
     * */
    public boolean drawMap(CommandSender sender){
    	String mapkey = "§7Unclaimed = #";
    	String map = "";

    	Player player = Bukkit.getPlayer(sender.getName());
    	loadWorld(player.getWorld().getName());
    	loadPlayer(sender.getName());
    	String factionName = playerData.getString("faction");
    	
    	int posX = player.getLocation().getBlockX();
    	int posY = player.getLocation().getBlockY();
    	int posZ = player.getLocation().getBlockZ();
    	
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
    	
    	String[] factionsFoundArray = new String[64];
    	String[] mapSymbols = {"/","]","[","}","{",";",",","-","0","_","=","*","&","^","%","$","!","@","\\"};
    	int factionsFound = 0;
    	String factionStandingOn = "neutral";
    	
    	for(int i = posZ-3*chunkSizeZ; i < posZ+3*chunkSizeZ; i++){
    		if(i%chunkSizeZ==0){
    			map += "\n";
    		
    		for(int j = posX-15*chunkSizeX; j < posX+15*chunkSizeX; j++){
        		if(j%chunkSizeX==0){
    			
    			if(i == posZ && j == posX){
    				map += "§a+§7";
    				if(boardData.has("chunkX" + j + " chunkY" + posY + " chunkZ" + i)){
    					if(!boardData.get("chunkX" + j + " chunkY" + posY + " chunkZ" + i).equals("")){
    						factionStandingOn = boardData.get("chunkX" + j + " chunkY" + posY + " chunkZ" + i).toString();
    						//mapkey += "," + getFactionRelationColor(factionName,factionStandingOn) + factionStandingOn + " = " + mapSymbols[factionsFound];
    					}
    				}
    			}
    			else{
    				if(boardData.has("chunkX" + j + " chunkY" + posY + " chunkZ" + i)){
    						String mapFaction = boardData.get("chunkX" + j + " chunkY" + posY + " chunkZ" + i).toString();
    						factionsFoundArray[factionsFound] = mapFaction;
    						boolean newOne = true;
    						int usekey = factionsFound+1;
    						for(int k = 0; k <= factionsFound; k++){
    							if(factionsFoundArray[k].equals(mapFaction)){
    								newOne = false;
    								usekey = k;
    							}
    						}
    						if(newOne){
    							factionsFound++;
        						mapkey += ", " + getFactionRelationColor(factionName,mapFaction) + mapFaction + " = " + mapSymbols[usekey];
    						}
    						
							map +=  getFactionRelationColor(factionName,mapFaction) + mapSymbols[usekey];
    				}
    				
    				
    				else{
    					map += "§7#§7";
    				}
    				}}
    			}
    		}
    	}
    	
    	String dashThing = "";
    	String spaceThing = "  ";
    	for(int i = 15; i>factionStandingOn.length(); i--){
    		dashThing += "-";
    		//if(i%2==0) spaceThing += " ";
    	}
    	sender.sendMessage("§7" + spaceThing + dashThing + " §f[" + getFactionRelationColor(factionName,factionStandingOn) + factionStandingOn + "§f]§7 " + dashThing + " ");
    	sender.sendMessage(map + "\n§3Layer: y" + posY + "§f -- " + mapkey);
    	return true;
    }
    
    /**
     * It tries to claim the chunk.
     * */
    public boolean tryClaim(CommandSender sender){
    	Player player = Bukkit.getPlayer(sender.getName());
    	loadWorld(player.getWorld().getName());
    	loadPlayer(sender.getName());
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equals("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}
    	
    	if(getFactionClaimedLand(factionName)>=getFactionPower(factionName)){
    		sender.sendMessage("§cYou need more power! §7Staying online and having more members increases power. Do §6/sf help§7 for more information.");
    		return true;
    	}
    	
    	int posX = player.getLocation().getBlockX();
    	int posY = player.getLocation().getBlockY();
    	int posZ = player.getLocation().getBlockZ();
    	
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;

    	if(boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ)){
    		if(boardData.get("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ).equals(factionName)){
    			sender.sendMessage("§cYou already own this land!");
    			return true;
    		}
    		
    		String faction2 = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
    		if(!faction2.equals("")){
    			if(getFactionPower(faction2)>=getFactionClaimedLand(faction2)){
    				sender.sendMessage(getFactionRelationColor(factionName,faction2) + configData.getString("faction symbol left") + faction2 + configData.getString("faction symbol right") + "§cowns this chunk.§7 If you want it, you need to lower their power before claiming it.");
    				return true;
    			}
    		}
    	}
    	
    	boardData.put("name", player.getWorld().getName());
    	boardData.put("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ, factionName);
    	
    	saveWorld(boardData);
    	messageFaction(factionName,Rel_Faction + sender.getName() + "§6 claimed x:" + posX + " y:" + posY + 
    			" z:" + posZ + " for " + Rel_Faction + configData.getString("faction symbol left") + factionName + configData.getString("faction symbol right") + "§6!");
    	//sender.sendMessage();
    	return true;
    }
    
    /**
     * It tries to unclaim the chunk.
     * */
    public boolean tryUnClaim(CommandSender sender){
    	Player player = Bukkit.getPlayer(sender.getName());
    	loadWorld(player.getWorld().getName());
    	loadPlayer(sender.getName());
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equals("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}
    	
    	int posX = player.getLocation().getBlockX();
    	int posY = player.getLocation().getBlockY();
    	int posZ = player.getLocation().getBlockZ();
    	
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;

    	//sender.sendMessage("x:" + posX + " y:" + posY + 
    	//		" z:" + posZ + " for the faction " + factionName + "!");
    	if(boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ)){
    		if(boardData.get("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ).equals("")){
    			sender.sendMessage("§cThis area is not already claimed!");
    			return true;
    		}
    		else{
        		if(boardData.get("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ).equals(factionName)){
        			boardData.remove("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
        			//sender.sendMessage("§6Chunk has been unclaimed.");
        			messageFaction(factionName,Rel_Faction + sender.getName() + "§6 unclaimed " + "chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
        	    	saveWorld(boardData);
        			return true;
        		}
        		
    			String faction2 = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
    			if(getFactionPower(faction2)<getFactionClaimedLand(faction2)){
    				
    				messageFaction(factionName,Rel_Faction + sender.getName() + "§6 unclaimed " + getFactionRelationColor(factionName,faction2) + 
    						configData.getString("faction symbol left") + faction2 + configData.getString("faction symbol right") + "§6's land!");
    				messageFaction(faction2,getFactionRelationColor(faction2,factionName) + configData.getString("faction symbol left") +  factionName + 
    						configData.getString("faction symbol right") + " " + sender.getName() + "§6 unclaimed your land!");
    				//sender.sendMessage();
        			
    				
    				boardData.remove("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
        	    	saveWorld(boardData);
    				return true;
    			}
    			else{
    				sender.sendMessage("§cYou could not unclaim " + getFactionRelationColor(factionName,faction2) + configData.getString("faction symbol left") + faction2 + configData.getString("faction symbol right") + "§c's land!§7 They have too much power!");
    				return true;
    			}
    		}
    	}
    	
    	sender.sendMessage("§cCould not unclaim chunk!");
    	return true;
    }
    
    /**
     * It tries to unclaim all chunks.
     * */
    public boolean tryUnClaimAll(CommandSender sender){
    	Player player = Bukkit.getPlayer(sender.getName());
    	loadWorld(player.getWorld().getName());
    	loadPlayer(sender.getName());
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equals("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}
    	
    	JSONArray array = boardData.names();
    	for(int i = 0; i < array.length(); i++){
    		String name = array.getString(i);
    		if(boardData.getString(name).equals(factionName)){
    			boardData.remove(name);
    		}
    	}
		saveWorld(boardData);
    	
    	//sender.sendMessage("§6Unclaimed all of your faction's land!");
    	messageFaction(factionName,Rel_Faction + sender.getName() + "§6 has unclaimed all of your factions land in §f" + player.getWorld().getName());
    	return true;
    }
    
    /**
     * Tries to create a faction with the specified name.
     * */
    public boolean tryCreateFaction(CommandSender sender, String[] args){
    	if(args.length<2){
			sender.sendMessage("Please include a name! Example: /sf create name");
		}else{
			
			if(args[1].contains("/") || args[1].contains("\\") || args[1].contains(".") || args[1].contains("\"") 
				|| args[1].contains(",") || args[1].contains("?") || args[1].contains("'") || args[1].contains("*") 
				|| args[1].contains("|") || args[1].contains("<") || args[1].contains(":")){
				sender.sendMessage("§cName cannot contain special characters!");
				return true;
			}
			
			for(int i = 0; i<factionIndex.size(); i++){
				if(factionIndex.get(i).equals(args[1].toString())){
					sender.sendMessage("§cThe faction name §f" + args[1].toString() + "§c is already taken!");
					return false;
				}
			}

			loadPlayer(sender.getName()); //load up playerData jsonobject
			
			String factionName = playerData.getString("faction");
			if(!factionName.equals("")){
				sender.sendMessage("§cYou are already in a faction!");
				sender.sendMessage("§ccurrent faction: §b" + configData.getString("faction symbol left") + factionName + configData.getString("faction symbol right"));
				sender.sendMessage("§cPlease do /sf leave in order to create a new faction!");
				return false;
			}

			createFaction(args[1].toString());
			
			loadPlayer(sender.getName());
			playerData.put("factionRank","leader");
			playerData.put("faction", args[1].toString());
			savePlayer(playerData);
			
			messageEveryone("§6The faction name " + Rel_Other + args[1].toString() + " §6has been created!");
			//sender.sendMessage();
			return true;
			
		}
    	
    	return false;
    }
    
    /**
     * Creates a faction
     * Is used for
     * 		New faction creation
     * 		used if faction is somehow deleted from filesystem
     * */
    public void createFaction(String faction){
    	enemyData = new JSONArray();
    	allyData = new JSONArray();
    	truceData = new JSONArray();
    	inviteData = new JSONArray();
    	factionData = new JSONObject();
		factionData.put("name", faction);
		factionData.put("ID", Math.random()*1000);
		factionData.put("shekels", 0.0);
		factionData.put("enemies",enemyData);
		factionData.put("allies",allyData);
		factionData.put("truce", truceData);
		factionData.put("invited", inviteData);
		factionData.put("home", "");
		factionData.put("desc", configData.getString("default faction description"));
		factionData.put("open", configData.getString("factions open by default"));
		saveFaction(factionData);
    	factionIndex.add(faction);
    }
    
    /**
     * Returns the jsonobject key, or a default value if no key is found.
     * */
    public String getKey(JSONObject jsonData, String key, String defaultString){
    	if(jsonData.has(key))
    		return jsonData.getString(key);
    	else
    		return defaultString;
    }
    
    /**
     * Leaves the current faction, if possible.
     * */
    public boolean tryLeave(CommandSender sender){
    	loadPlayer(sender.getName());
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equals("")){
    		sender.sendMessage("You are not currently in a faction, silly.");
    		return true;
    	}

    	playerData.put("faction", "");
    	playerData.put("factionRank", "member");
    	savePlayer(playerData);
    	
    	//disband if you're the last one there
    	boolean canDisband = true;
    	for(String name : playerIndex){
    		loadPlayer(name);
    		if(playerData.getString("faction").equals(factionName)){
    			canDisband = false;
    		}
    	}
    	
    	if(canDisband){
    		File file = new File(this.getDataFolder() + "/factionData/" + factionName + ".json");
    		file.delete();
        	int k = -1;
        	for(int i = 0; i<factionIndex.size(); i++){
        		if(factionIndex.get(i).equals(factionName)){
        			k = i;
        		}
        	}
        	
        	if(k>=0){
        		factionIndex.remove(k);
        		Bukkit.getServer().getConsoleSender().sendMessage("removed");
        	}
        	
        	loadWorld(Bukkit.getPlayer(sender.getName()).getWorld().getName());
        	JSONArray array = boardData.names();
        	for(int i = 0; i < array.length(); i++){
        		String name = array.getString(i);
        		if(boardData.getString(name).equals(factionName)){
        			boardData.remove(name);
        		}
        	}
    		saveWorld(boardData);
    	}
    	

    	
		sender.sendMessage("You have left your faction!");
    	return true;
    }
    
    /**
     * Disbands the faction given.
     * */
    public boolean tryDisband(CommandSender sender,String factionName){
    	
    	for(String name : playerIndex){
    		loadPlayer(name.replaceFirst(".json", ""));
    		if(playerData.getString("faction").equals(factionName)){
    			playerData.put("faction", "");
    			playerData.put("factionRank", "member");
    			savePlayer(playerData);
    		}
    	}
    	
    	File file = new File(this.getDataFolder() + "/factionData/" + factionName + ".json");
    	if(file.exists())
    		file.delete();

    	int k = -1;
    	for(int i = 0; i<factionIndex.size(); i++){
    		if(factionIndex.get(i).equals(factionName)){
    			k = i;
    		}
    	}
    	
    	if(k>=0){
    		factionIndex.remove(k);
    	}
    	
    	loadWorld(Bukkit.getPlayer(sender.getName()).getWorld().getName());
    	JSONArray array = boardData.names();
    	for(int i = 0; i < array.length(); i++){
    		String name = array.getString(i);
    		if(boardData.getString(name).equals(factionName)){
    			boardData.remove(name);
    		}
    	}
		saveWorld(boardData);
    	
		sender.sendMessage("The faction has been disbanded!");
    	return true;
    }
    
    /**
     * Does stuff when a new player joins the server.
     * */
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
    	File playerFile = new File(this.getDataFolder() + "/playerData/" + event.getPlayer().getName() + ".json");
    	if(!playerFile.exists()){
        	createPlayer(event.getPlayer());
    	}
    }
    
    /**
     * Creates player data.
     * This is used for 
    		New player join
    		When the player file is missing (deleted?)
     * */
    public void createPlayer(Player player){
    	playerData = new JSONObject();
  		playerData.put("name", player.getName());
  		playerData.put("ID", player.getUniqueId());
  		playerData.put("faction","");
  		playerData.put("factionRank",configData.getString("default player rank"));
  		playerData.put("factionTitle",configData.getString("default player title"));
  		playerData.put("shekels", configData.getInt("default player money"));
  		playerData.put("power", configData.getInt("default player power"));
  		playerData.put("deaths",0);
  		playerData.put("kills",0);
  		playerData.put("time online",(long) 0.0);
  		playerData.put("chat channel","global");
  		playerData.put("last online",System.currentTimeMillis());
  		savePlayer(playerData);
    	
    	playerIndex.add(player.getName());
    }
    public void createPlayer(OfflinePlayer player){
    	playerData = new JSONObject();
  		playerData.put("name", player.getName());
  		playerData.put("ID", player.getUniqueId());
  		playerData.put("faction","");
  		playerData.put("factionRank",configData.getString("default player rank"));
  		playerData.put("factionTitle",configData.getString("default player title"));
  		playerData.put("shekels", configData.getInt("default player money"));
  		playerData.put("power", configData.getInt("default player power"));
  		playerData.put("deaths",0);
  		playerData.put("kills",0);
  		playerData.put("chat channel","global");
  		playerData.put("time online",(long) 0.0);
  		playerData.put("last online",System.currentTimeMillis());
  		savePlayer(playerData);
    	
    	playerIndex.add(player.getName());
    }
    
    /**
     * The first argument is your faction, the second argument is the other faction.
     * Returns a string with the color code of your relation to the second faction.
     * */
    public String getFactionRelationColor(String senderFaction, String reviewedFaction){
    	
    	if(!configData.getString("enforce relations").equals("")){
    		String rel = configData.getString("enforce relations");
    		if(rel.equals("enemies")) return Rel_Enemy;
    		if(rel.equals("ally")) return Rel_Ally;
    		if(rel.equals("truce")) return Rel_Truce;
    		if(rel.equals("neutral")) return Rel_Other;
    		if(rel.equals("other")) return Rel_Other;
    	}
    	
    	String relation = "";
    	String relation2 = "";
    	if(senderFaction.equals("")) return Rel_Other;
    	if(senderFaction.equals(reviewedFaction)) return Rel_Faction;
    	
    	if(!senderFaction.equals("") && !reviewedFaction.equals("") && !reviewedFaction.equals("neutral territory")
    			&& !senderFaction.equals("neutral territory")) {
    		
    		loadFaction(senderFaction);
    		if(factionData.get("enemies").toString().contains(reviewedFaction)) relation="enemy";// return Rel_Enemy;
    		if(factionData.get("allies").toString().contains(reviewedFaction)) relation="ally";// return Rel_Ally;
    		if(factionData.get("truce").toString().contains(reviewedFaction)) relation="truce";// return Rel_Truce;
    	
    		loadFaction(reviewedFaction);
    		if(factionData.get("enemies").toString().contains(senderFaction)) relation2="enemy";// return Rel_Enemy;
    		if(factionData.get("allies").toString().contains(senderFaction)) relation2="ally";// return Rel_Ally;
    		if(factionData.get("truce").toString().contains(senderFaction)) relation2="truce";// return Rel_Truce;
    	
    		if(relation.equals("enemy") || relation2.equals("enemy")) return Rel_Enemy;
    		if(relation.equals("ally") && relation2.equals("ally")) return Rel_Ally;
    		if(relation.equals("truce") && relation2.equals("truce")) return Rel_Truce;
    	
    		loadFaction(senderFaction);

        	return Rel_Other;
    	}else{
        	return Rel_Other;
    	}
    }
    
    /**
     * EventHandler for damage being done/taken
     * */
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
    	Entity entityAttacking = event.getDamager();
    	Entity entityAttacked = event.getEntity();
    	
    	DamageCause damagedCause = event.getCause();
    	
    	if((entityAttacking.getType() == EntityType.PLAYER || entityAttacking.getType() == EntityType.ARROW) && entityAttacked.getType() == EntityType.PLAYER){
    		
    		if(entityAttacking.getType() == EntityType.ARROW){
    			Arrow arrow = (Arrow) entityAttacking;
    			
            	if(arrow instanceof Player){
                	Player playerAttacked = (Player) entityAttacked;
            		Player playerAttacking = (Player) arrow.getShooter();
            		loadPlayer(playerAttacking.getName());
            		String factionAttacking = playerData.getString("faction");
            		loadPlayer(playerAttacked.getName());
            		String factionAttacked = playerData.getString("faction");
            	
            		if(configData.getString("friendly fire projectile (arrows)").equals("false")){
        				playerAttacking.sendMessage("§7You cannot shoot members of " + getFactionRelationColor(factionAttacking,factionAttacked) + 
        						configData.getString("faction symbol left") + factionAttacked + configData.getString("faction symbol right") + "§7!");
            			event.setCancelled(true);
        				return;
        			}
            	}
            	return;
    		}
    		
        	Player playerAttacking = (Player) entityAttacking;
        	Player playerAttacked = (Player) entityAttacked;
        	
        	loadPlayer(playerAttacking.getName());
        	String factionAttacking = playerData.getString("faction");
        	loadPlayer(playerAttacked.getName());
        	String factionAttacked = playerData.getString("faction");
        	
        	if(factionAttacking.equals(factionAttacked)){
        		
        		if(damagedCause == DamageCause.ENTITY_ATTACK && configData.getString("friendly fire melee").equals("true")){
        			playerAttacking.sendMessage("§7Hit player!");
        			return;
        		}
        		
        		if(configData.getString("friendly fire other").equals("false")){
        			playerAttacking.sendMessage("§7You cannot hurt members of " + getFactionRelationColor(factionAttacking,factionAttacked) + 
        					configData.getString("faction symbol left") + factionAttacked + configData.getString("faction symbol right") + "§7!");
            		event.setCancelled(true);
            		return;
        		}
        		
        		return;
        	}
        	else{
        		return;
        	}
    	}
    	return;
    }
    
    /**
     * This is where the plugin edits the chat messages/formatting.
     * */
	@EventHandler
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event){
		event.setMessage(event.getMessage().replace(">", "§a>"));
		
		int posX_talk = event.getPlayer().getLocation().getBlockX();
		int posZ_talk = event.getPlayer().getLocation().getBlockZ();
		
		Set<Player> playerList = event.getRecipients();
		String playerName = event.getPlayer().getName();
    	loadPlayer(playerName);
    	String chatChannel_talk = playerData.getString("chat channel");
    	String rank = playerData.get("factionRank").toString();
    	String title = playerData.get("factionTitle").toString();
    	String faction = playerData.get("faction").toString();
    	String factionString = playerData.get("faction").toString();
    	rank += " ";
    	if(rank.contains("leader")) rank = "** ";
    	if(rank.contains("officer")) rank = "* ";
    	if(rank.contains("member")) rank = "";
    	if(rank.equals(" ")) rank = "";
    	String factionRelation = "";
    	String faction2 = "";
    	if(!configData.getString("allow player titles").equals("true"))
    		title = "";
    	
		for(Player player : playerList){
	    	loadPlayer(playerName);
	    	factionString = playerData.getString("faction");
	    	loadPlayer(player.getName());
	    	faction2 = playerData.getString("faction");
	    	String chatChannel_listen = playerData.getString("chat channel");
	    	if(!faction.equals("") && !faction2.equals(""))
	    		factionRelation = getFactionRelationColor(faction2,faction);
	    	else
	    		factionRelation = Rel_Other;
	    	if(!faction.equals("")) factionString = configData.getString("faction symbol left") + faction + configData.getString("faction symbol right");

	    	
	    	if(!faction.equals("") && !faction2.equals(""))
	    		loadFaction(faction);
	    	
	    	//global
	    	if(chatChannel_talk.equals("global")){
	    		player.sendMessage("" + factionRelation + rank + "" + factionString + " §f(" + factionRelation + playerName + "§f): " + event.getMessage());
	    		continue;
	    	}
	    	
	    	//faction
	    	if(chatChannel_talk.equals("faction") && faction.equals(faction2)){
	    		player.sendMessage(Rel_Faction + "(faction) " + factionRelation + title + " " + rank + "" + factionString + " §f(" + factionRelation + playerName + "§f): " + event.getMessage());
	    		continue;
	    	}
	    	
	    	//ally
	    	if(chatChannel_talk.equals("ally")){
	    		allyData = factionData.getJSONArray("allies");
	    		for(int i = 0; i<allyData.length(); i++)
	    			if(allyData.getString(i).equals(faction2))
	    	    		player.sendMessage(Rel_Ally + "(ally) " + factionRelation + title + " " + rank + "" + factionString + " §f(" + factionRelation + playerName + "§f): " + event.getMessage());
	    		if(faction.equals(faction2))
    	    		player.sendMessage(Rel_Ally + "(ally) " + factionRelation + title + " " + rank + "" + factionString + " §f(" + factionRelation + playerName + "§f): " + event.getMessage());
	    		continue;
	    	}
	    	
	    	//truce
	    	if(chatChannel_talk.equals("truce")){
	    		truceData = factionData.getJSONArray("truce");
	    		for(int i = 0; i<truceData.length(); i++)
	    			if(truceData.getString(i).equals(faction2))
	    	    		player.sendMessage(Rel_Truce + "(truce) " + factionRelation + title + " " + rank + "" + factionString + " §f(" + factionRelation + playerName + "§f): " + event.getMessage());
	    		if(faction.equals(faction2))
    	    		player.sendMessage(Rel_Truce + "(truce) " + factionRelation + title + " " + rank + "" + factionString + " §f(" + factionRelation + playerName + "§f): " + event.getMessage());
	    		continue;
	    	}
	    	
	    	//enemy
	    	if(chatChannel_talk.equals("enemy")){
	    		enemyData = factionData.getJSONArray("enemies");
	    		for(int i = 0; i<enemyData.length(); i++)
	    			if(enemyData.getString(i).equals(faction2))
	    	    		player.sendMessage(Rel_Enemy + "(enemy) " + factionRelation + title + " " + rank + "" + factionString + " §f(" + factionRelation + playerName + "§f): " + event.getMessage());
	    		if(faction.equals(faction2))
    	    		player.sendMessage(Rel_Enemy + "(enemy) " + factionRelation + title + " " + rank + "" + factionString + " §f(" + factionRelation + playerName + "§f): " + event.getMessage());
	    		
	    		continue;
	    	}
	    	
	    	//local
	    	if(chatChannel_talk.equals("local")){
	    		String Direction = "";
	    		int posX_listen = player.getLocation().getBlockX();
	    		int posZ_listen = player.getLocation().getBlockZ();
	    			//it's time for highschool trig!
	    		int distance = (int) Math.sqrt(Math.pow(posX_talk - posX_listen,2) + Math.pow(posZ_talk - posZ_listen,2));
	    		double direction = Math.toDegrees(Math.atan2((posZ_talk - posZ_listen), (posX_talk - posX_listen) + 0.001));
	    		
	    		if(direction>-15 && direction<15) Direction = "E";
	    		if(direction>-75 && direction<-14) Direction = "NE";
	    		if(direction>-105 && direction<-74) Direction = "N";
	    		if(direction>-165 && direction<-104) Direction = "NW";
	    		if(direction<-164 || direction>165) Direction = "W";
	    		if(direction<165 && direction>105) Direction = "SW";
	    		if(direction<106 && direction>75) Direction = "S";
	    		if(direction<76 && direction>15) Direction = "SE";
	    		
	    		if(distance<configData.getInt("local chat distance") && !player.getName().equals(event.getPlayer().getName()))
    	    		player.sendMessage(Rel_Neutral + "(local:" + (distance) + "" + Direction + ") " + 
    	    				factionRelation + title + " " + rank + "" + factionString + " §f(" + factionRelation + 
    	    				playerName + "§f): " + event.getMessage());
	    		if(player.getName().equals(event.getPlayer().getName()))
	    	    		player.sendMessage(Rel_Neutral + "(local) " + factionRelation + title + " " + rank + "" + factionString + " §f(" + factionRelation + playerName + "§f): " + event.getMessage());
	    		continue;
	    	}
	    	
	    	//custom
	    	if(chatChannel_talk.equals(chatChannel_listen)){
	    		player.sendMessage(Rel_Other + "(" + chatChannel_talk  + ") " + factionRelation + rank + "" + factionString + " §f(" + factionRelation + playerName + "§f): " + event.getMessage());
	    		continue;
	    	}
	    	
		}
		
		event.setCancelled(true);
	}

	/**
	 * Run this code whenever a player moves from one block to another.
	 * */
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		
		//declare stuff
		Location loc = event.getTo().getBlock().getLocation();
		Player player = event.getPlayer();
		World world = player.getWorld();
		String inFaction = "neutral territory";
		
		//load stuff into databases
    	loadWorld(world.getName());
    	loadPlayer(player.getName());
    	
    	String playerFaction = playerData.getString("faction");
    	
    	//do math
		int posX = loc.getBlockX();
		int posY = loc.getBlockY();
		int posZ = loc.getBlockZ();
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
    	
    	
    	if(boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ))
    		inFaction = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
    	
    	//figure out if this is a new place
    	int k = -1;
    	for(int i = 0; i < playerIsIn_player.size(); i++){
    		if(playerIsIn_player.get(i).equals(player.getName())){
    			k = i;
    		}
    	}
    	
    	Location location = new Location(loc.getWorld(),posX,posY,posZ);
    	
    	if(k==-1){
    		playerIsIn_player.add(player.getName());
    		playerIsIn_faction.add(inFaction);
    		playerIsIn_location.add(location);
    		k = 0;
    	}
    	
    	if(!playerIsIn_faction.get(k).equals(inFaction)){
    		player.sendMessage("§7You have traveled from " + getFactionRelationColor(playerFaction,playerIsIn_faction.get(k)) + configData.getString("faction symbol left") + 
    				playerIsIn_faction.get(k) + configData.getString("faction symbol right") + 
    				"§7 to " + getFactionRelationColor(playerFaction,inFaction) + configData.getString("faction symbol left") + 
    				inFaction + configData.getString("faction symbol right") + "§7.");
    		playerIsIn_faction.set(k, inFaction);

    	}

    	if(!playerIsIn_location.get(k).equals(location) && !playerData.getString("faction").equals(inFaction)){
    		playerIsIn_location.set(k,location);
        	if(playerData.has("autoclaim"))
        		if(playerData.getString("autoclaim").equals("true"))
        			tryClaim((CommandSender) player); 
    	}
    	
    	
	}
	
	
	public String getFactionAt(Location location){
		String faction = "";
    	loadWorld(location.getWorld().getName());
    	
    	int posX = location.getBlockX();
    	int posY = location.getBlockY();
    	int posZ = location.getBlockZ();
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
    	
    	if(boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ))
    		faction = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
    	
		return faction;
	}
	
	/**
	 * Checks if the player can edit terrain at location, returns true/false if they can or cannot.
	 * */
	public boolean canEditHere(Player player, Location location, String breakorplace){
		loadPlayer(player.getName());
		String rankEditing = playerData.getString("factionRank");
		String factionEditing = playerData.getString("faction");
		String factionBeingEdited = getFactionAt(location);
    	
    	if(!factionBeingEdited.equals("")){
    		String rel = getFactionRelationColor(factionEditing,factionBeingEdited);
    		
    		Boolean returnType = true;
    		String relation = "neutral";
    		String forceRel = configData.getString("enforce relations");
    		if(!forceRel.equals("")) rel = forceRel;
    		if(rel.equals(Rel_Ally)) relation = "ally";
    		if(rel.equals(Rel_Enemy)) relation = "enemy";
    		if(rel.equals(Rel_Truce)) relation = "truce";
    		if(rel.equals(Rel_Other)) relation = "other";
    		
			if(breakorplace.equals("break") && configData.getString("protect all claimed blocks from being broken in " + relation + " territory").equals("true"))
				returnType=!returnType;
			
			if(breakorplace.equals("place") && configData.getString("protect all claimed blocks from being placed in " + relation + " territory").equals("true"))
				returnType=!returnType;
			
			if(breakorplace.equals("break") && configData.getJSONArray("block break protection in " + relation + " land").toString().contains(location.getBlock().getType().toString()))
				returnType=!returnType;
				
			if(breakorplace.equals("place") && configData.getJSONArray("block place protection in " + relation + " land").toString().contains(location.getBlock().getType().toString()))
				returnType=!returnType;

			loadFaction(factionBeingEdited);
			for(int j = 0; j<2; j++){
				JSONObject chunk = new JSONObject();
				
				if(j==0){
		    		int posX = location.getBlockX();
		    		int posY = location.getBlockY();
		    		int posZ = location.getBlockZ();
		    		posX = Math.round(posX / chunkSizeX) * chunkSizeX;
		    		posY = Math.round(posY / chunkSizeY) * chunkSizeY;
		    		posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
		    		String board = location.getWorld().getName() + "chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ;
				
		    		if(factionData.has(board))
		    			chunk = factionData.getJSONObject(board);
		    		else
		    			continue;
				}
				
				for(int k = 0; k<3; k++){
					
					JSONObject type = new JSONObject();
					String subtype = "";
					
					if(k == 1){
		    			if(j!=0 && factionData.has("p")) type = factionData.getJSONObject("p");
		    			if(j==0 && chunk.has("p")) type = chunk.getJSONObject("p");
		    			subtype = player.getName();
		    			if(!factionEditing.equals(factionBeingEdited))
		    				continue;
		    		}
					if(factionData.has("f") && k == 2){
		    			if(j!=0 && factionData.has("f")) type = factionData.getJSONObject("f");
		    			if(j==0 && chunk.has("f")) type = chunk.getJSONObject("f");
		    			subtype = factionBeingEdited;
		    			if(factionEditing.equals(factionBeingEdited))
		    				continue;
		    		}
					
					if(factionData.has("r") && k == 0){
		    			if(j!=0 && factionData.has("r")) type = factionData.getJSONObject("r");
		    			if(j==0 && chunk.has("r")) type = chunk.getJSONObject("r");
		    			subtype = rankEditing;
		    			if(!factionEditing.equals(factionBeingEdited))
		    				continue;
		    		}
		    		
		    		if(type.has(subtype)){
		    			JSONObject playerJson = type.getJSONObject(subtype);
		    			if(playerJson.has("allowed")){
		    				JSONArray allowed = playerJson.getJSONArray("allowed");
		    				for(int i = 0; i < allowed.length(); i++)
		    					if(allowed.getString(i).equals(location.getBlock().getType().toString()))
		    						returnType = true;;
		    			}if(playerJson.has("notAllowed")){
		    				JSONArray notAllowed = playerJson.getJSONArray("notAllowed");
		    				for(int i = 0; i < notAllowed.length(); i++)
		    					if(notAllowed.getString(i).equals(location.getBlock().getType().toString()))
		    						returnType = false;
		    			}
		    		}
		    	}
			}
			
			
    		return returnType;
    	}
		
		return true;
	}
	
	
	/**
	 * Checks if the player can use items at this location.
	 * */
	public boolean canInteractHere(Player player, Location location, String itemName){
		loadPlayer(player.getName());
		String rankEditing = playerData.getString("factionRank");
		String factionEditing = playerData.getString("faction");
		String factionBeingEdited = getFactionAt(location);
		
    	String rel = Rel_Neutral;
    	String relation = "neutral";
    	
    	boolean returnType = true;
    	if(!factionBeingEdited.equals(""))
    		rel = getFactionRelationColor(factionEditing,factionBeingEdited);

		if(rel.equals(Rel_Ally)) relation = "ally";
		if(rel.equals(Rel_Enemy)) relation = "enemy";
		if(rel.equals(Rel_Truce)) relation = "truce";
		if(rel.equals(Rel_Other)) relation = "other";
		
		if(configData.getString("block all item use by default in " + relation + " territory").equals("true"))
			returnType=!returnType;
		
		if(itemName.equals("") && configData.getJSONArray("item protection in " + relation + " land").toString().contains(location.getBlock().getType().toString()))
			returnType=!returnType;
		
		if(!itemName.equals("") && configData.getJSONArray("item protection in " + relation + " land").toString().contains(itemName))
			returnType=!returnType;
		
		loadFaction(factionBeingEdited);
		for(int j = 0; j<2; j++){
			JSONObject chunk = new JSONObject();
			
			if(j==0){
	    		int posX = location.getBlockX();
	    		int posY = location.getBlockY();
	    		int posZ = location.getBlockZ();
	    		posX = Math.round(posX / chunkSizeX) * chunkSizeX;
	    		posY = Math.round(posY / chunkSizeY) * chunkSizeY;
	    		posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
	    		String board = location.getWorld().getName() + "chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ;
			
	    		if(factionData.has(board))
	    			chunk = factionData.getJSONObject(board);
	    		else
	    			continue;
			}
			
			for(int k = 0; k<3; k++){
				
				JSONObject type = new JSONObject();
				String subtype = "";
				
				if(k == 1){
	    			if(j!=0 && factionData.has("p")) type = factionData.getJSONObject("p");
	    			if(j==0 && chunk.has("p")) type = chunk.getJSONObject("p");
	    			subtype = player.getName();
	    			if(!factionEditing.equals(factionBeingEdited))
	    				continue;
	    		}
				if(factionData.has("f") && k == 2){
	    			if(j!=0 && factionData.has("f")) type = factionData.getJSONObject("f");
	    			if(j==0 && chunk.has("f")) type = chunk.getJSONObject("f");
	    			subtype = factionBeingEdited;
	    			if(factionEditing.equals(factionBeingEdited))
	    				continue;
	    		}
				
				if(factionData.has("r") && k == 0){
	    			if(j!=0 && factionData.has("r")) type = factionData.getJSONObject("r");
	    			if(j==0 && chunk.has("r")) type = chunk.getJSONObject("r");
	    			subtype = rankEditing;
	    			if(!factionEditing.equals(factionBeingEdited))
	    				continue;
	    		}
	    		
	    		if(type.has(subtype)){
	    			JSONObject playerJson = type.getJSONObject(subtype);
	    			if(playerJson.has("allowed")){
	    				JSONArray allowed = playerJson.getJSONArray("allowed");
	    				for(int i = 0; i < allowed.length(); i++)
	    					if(allowed.getString(i).equals(location.getBlock().getType().toString()))
	    						returnType = true;;
	    			}if(playerJson.has("notAllowed")){
	    				JSONArray notAllowed = playerJson.getJSONArray("notAllowed");
	    				for(int i = 0; i < notAllowed.length(); i++)
	    					if(notAllowed.getString(i).equals(location.getBlock().getType().toString()))
	    						returnType = false;
	    			}
	    		}
	    	}
		}
		
		return returnType;
	}
	
	
	/**
	 * Eventhandler for block breaking
	 * */
	@EventHandler
	public void blockBreak(BlockBreakEvent event){
		if(!canEditHere(event.getPlayer(),event.getBlock().getLocation(),"break")){
			event.setCancelled(true);
		}
		
	}
	
	
	/**
	 * Eventhandler for block placing
	 * */
	@EventHandler
	public void blockPlace(BlockPlaceEvent event){
		if(!canEditHere(event.getPlayer(),event.getBlock().getLocation(),"place")){
			event.setCancelled(true);
		}
	}
	
	
	/**
	 * When a creeper or tnt explodes, check all affected blocks. If claimed, ignore it (if its set that way in the options);
	 * */
	@EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
	    List<Block> destroyed = event.blockList();
	    Iterator<Block> it = destroyed.iterator();
	    while (it.hasNext()) {
	        Block block = it.next();
	        Location loc = block.getLocation();
	        loadWorld(loc.getWorld().getName());
	        int posX = loc.getBlockX();
	        int posY = loc.getBlockY();
	        int posZ = loc.getBlockZ();
	        posX = Math.round(posX / chunkSizeX) * chunkSizeX;
	        posY = Math.round(posY / chunkSizeY) * chunkSizeY;
	        posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
	        if(boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ)){
	        	String faction = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
	        	boolean online = isFactionOnline(faction);
	        	if(online && configData.getString("protect claimed land from explosions while faction is online").equals("true")){
		        	event.setCancelled(true);
		        	break;
	        	}
	        	if(!online && configData.getString("protect claimed land from explosions while faction is offline").equals("true")){
		        	event.setCancelled(true);
		        	break;
	        	}
	        }
	    }
    }

	

	/**
	 * when a player right clicks an item
	 * */
	@EventHandler
	public void playerInteract(PlayerInteractEvent event){
		if(event.hasBlock() && event.getAction() == Action.RIGHT_CLICK_BLOCK){
			if(!canInteractHere(event.getPlayer(),event.getClickedBlock().getLocation(),"")){
				event.setCancelled(true);
			}
		}
		if(event.hasItem() && event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK){
			if(!canInteractHere(event.getPlayer(),event.getPlayer().getLocation(),event.getItem().getType().toString())){
				event.setCancelled(true);
			}
		}
	}
	
	
	/**
	 * when a player dies
	 * */
	@EventHandler
	public void playerDied(PlayerDeathEvent event){
		String player = ((Player) event.getEntity()).getName();
		loadPlayer(player);
		double power = playerData.getDouble("power");
		double maxpower = configData.getDouble("max player power");
		double minpower = configData.getDouble("minimum player power");
		
		power-=configData.getDouble("power lost on death");
		if(power<minpower) power = minpower;
		if(power>maxpower) power = maxpower;
		
		int deaths = playerData.getInt("deaths") + 1;
		playerData.put("deaths", deaths);
		playerData.put("power", power);
		savePlayer(playerData);
		Player p = event.getEntity();
		if(p.isDead()) {
			p.getKiller();
			if(p.getKiller() instanceof Player) {
				loadPlayer(p.getKiller().getName());
				int kills = playerData.getInt("kills") + 1;
				playerData.put("kills", kills);
				savePlayer(playerData);
			}
		}
	}

	@EventHandler
	public void respawnEvent(PlayerRespawnEvent event){
		//set new spawn (if faction has one)
		Player player = event.getPlayer();
		String playerString = player.getName();
		loadPlayer(playerString);
		if(!playerData.equals("")){
			loadFaction(playerData.getString("faction"));
			String home = factionData.getString("home");
			if(!home.equals("")){
				Scanner scan = new Scanner(home);
		    	String world = scan.next();
		    	double x = scan.nextDouble();
		    	double y = scan.nextDouble();
		    	double z = scan.nextDouble();
		    	scan.close();
		    	
		    	Location loc = new Location(Bukkit.getWorld(world), x, y, z);
		    	Block block = loc.getBlock();
		    	int i = 0;
		    	while(block.getRelative(BlockFace.DOWN).getType() != Material.AIR || block.getType() != Material.AIR){
		    		block = block.getRelative((int) (Math.random()*10), (int)(Math.random()*10), (int)(Math.random()*10));
		    		i++;
		    		if(i>9){
		    			loc = player.getLocation().getWorld().getSpawnLocation();
		    		}
		    	}
		    	
		    	loc = block.getLocation();
				event.setRespawnLocation(loc);
			}
		}
	}
	
	/**
	 * Task handling for player power (every 3 seconds)
	 * */
	public void updatePlayerPower(){
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
            	//Update player power
        		currentTime = System.currentTimeMillis();
        		loadConfig();
            	double power = 0;
            	double update = (80.0/72000.0);
            	double powerUpdateOnline = configData.getDouble("power per hour while online") * update;
            	double powerUpdateOffline = configData.getDouble("power per hour while offline") * update;
            	double powerUpdateEnemyInYourTerritory = configData.getDouble("power per hour while enemy in your territory") * update;
            	double powerUpdateWhileInOwnTerritory = configData.getDouble("power per hour while in own territory") * update;
            	double powerUpdateWhileInEnemyTerritory = configData.getDouble("power per hour while in enemy territory") * update;
            	
        		OfflinePlayer[] off = Bukkit.getOfflinePlayers();
        		OfflinePlayer[] on = Bukkit.getOnlinePlayers();
        		

        		if(configData.getString("update power while offline").equals("true"))
        			for(int i = 0; i < off.length; i++){ //offline players
        				if(!off[i].isOnline()) {
        					loadPlayer(off[i].getName());
        					power = playerData.getDouble("power");
        					power += powerUpdateOffline;
        					
        					if(power<configData.getDouble("minimum player power"))
        						power = configData.getDouble("minimum player power");
        					if(power>configData.getDouble("max player power"))
        						power = configData.getDouble("max player power");
        					
        					playerData.put("power",power);
        					savePlayer(playerData);
        				}
        			}
        		
        		if(configData.getString("update power while online").equals("true"))
        			for(int i = 0; i < on.length; i++){ //online players
        				if(on[i].isOnline()) {
        					loadPlayer(on[i].getName());

        					power  = playerData.getDouble("power");
        					
        					if(configData.getString("update power while enemy in your territory").equals("true")
        							|| configData.getString("update power while in enemy territory").equals("true")){
            					loadWorld(Bukkit.getPlayer(on[i].getName()).getLocation().getWorld().getName());
            					Player p = on[i].getPlayer();
            			    	int posX = p.getLocation().getBlockX();
            			    	int posY = p.getLocation().getBlockY();
            			    	int posZ = p.getLocation().getBlockZ();
            			    	
            			    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
            			    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
            			    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
            			    	
            			    	if(boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ)){
            			    		String pFaction = playerData.getString("faction");
            			    		String rel = getFactionRelationColor(boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ),pFaction);
            			    		
            			    		if(rel.equals(Rel_Enemy)){
            			    			if(configData.getString("update power while in enemy territory").equals("true")){
            			    				power += powerUpdateWhileInEnemyTerritory;
            			    			}
            			    			if(configData.getString("update power while enemy in your territory").equals("true")){
            			    				for(int j = 0; j < on.length; j++){
            			    					loadPlayer(on[j].getPlayer().getName());
            			    					if(getFactionRelationColor(playerData.getString("faction"),pFaction).equals(Rel_Enemy)){
            			    						power += powerUpdateEnemyInYourTerritory;
            			    					}
            			    				}
            			    			}
            			    		}
            			    		
            			    		if(rel.equals(Rel_Faction)){
            			    			if(configData.getString("update power while in own territory").equals("true")){
            			    				power += powerUpdateWhileInOwnTerritory;
            			    			}
            			    		}
            			    		
            			    	}
            			    	else{
                					power += powerUpdateOnline;
            			    	}
        					}
        					else{
            					power += powerUpdateOnline;
        					}
        					
        					
        					if(power<configData.getDouble("minimum player power"))
        						power = configData.getDouble("minimum player power");
        					if(power>configData.getDouble("max player power"))
        						power = configData.getDouble("max player power");
        					
        					long lastOnline = playerData.getLong("last online");
        					long onlineTime = playerData.getLong("time online");
        					
        					onlineTime += currentTime - lastTime;
        					
        					playerData.put("last online", System.currentTimeMillis());
        					playerData.put("time online", onlineTime);
        					playerData.put("power",power);
        					savePlayer(playerData);
        				}
        			}
            	

        		lastTime = System.currentTimeMillis();
            }
        }, 0L, 80L);
	}

}


