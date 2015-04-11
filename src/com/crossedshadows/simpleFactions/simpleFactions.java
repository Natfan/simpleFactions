package com.crossedshadows.simpleFactions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit; 
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World; 
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.json.JSONArray;
import org.json.JSONObject;
//import org.kitteh.tag.AsyncPlayerReceiveNameTagEvent;
import org.mcstats.MetricsLite;

import com.sun.corba.se.spi.activation.Server;

/**
 * TODO List
 * 
 * Idea TODO:
 * 		Make a secondary claiming system, so factions can "claim" and, but won't have protections there.
 * 		Make claims go away after a configurable amount of time
 * 
 * Misc TODO: 
 * 		Optional power scaling systems
 * 
 * Commands TODO:
 * 		/f negative (for seeing who in your faction has negative power)
 *		/f deny
 *		Fix /f leader name
 *
 *		PROTIP FOR OTHER PROGRAMMERS: If you're using eclipse, do <ctrl>+<shift>+/ (divide on the numpad) 
 *		to minimize all of the functions. Makes reading the code a breeze!
 * */

public class simpleFactions extends JavaPlugin implements Listener {
	
	//index of players/factions (constantly updating to stay accurate)
	static List<String> factionIndex = new ArrayList<String>();
	static List<UUID> playerIndex = new ArrayList<UUID>();
	static List<String> boardIndex = new ArrayList<String>();
	
	//for traveling around faction territory (one of the few things that stay in memory for entire life of plugin)
	static List<String> playerIsIn_player = new ArrayList<String>();
	static List<String> playerIsIn_faction = new ArrayList<String>();
	static List<Location> playerIsIn_location = new ArrayList<Location>();
	
	static String serverAddress = "server.forgotten-lore.com";
	static int serverPort = 420; //blaze it
	
	
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
	static JSONObject boardData = new JSONObject();
	static JSONObject factionData = new JSONObject();
	static JSONObject playerData = new JSONObject();
	static JSONArray scheduleData = new JSONArray();
	static JSONArray enemyData = new JSONArray();
	static JSONArray allyData = new JSONArray();
	static JSONArray truceData = new JSONArray();
	static JSONArray inviteData = new JSONArray();
	
	//version
	static String version = "1.95"; 

	//global thing to pass to async task
	static TNTPrimed lastCheckedTNT; 
	
	public static File dataFolder;
    public static JavaPlugin plugin;
    
    static long currentTime = System.currentTimeMillis();
    static long lastTime = System.currentTimeMillis();
	
	/**
	 * Starts with server startup or /reload
	 * */
	public void onEnable()
	{

		plugin = this; 
		
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions is starting!]");
		dataFolder = getDataFolder(); 
		
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(new eventListener(), this);
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has registered the events!]");
		loadData();
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has loaded necessary data!]");
		scheduleSetup(); 
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has set up tasks!]");
		Language.loadLanguageData();
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has loaded the language file!]");
		
		if(Config.configData.getString("checkForUpdates").equalsIgnoreCase("true")){
			Config.checkForUpdates(); 
			Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has finished checking for updates!]");
		}

		if(Config.configData.getString("getDataFromHome").equalsIgnoreCase("true")){
			getDateFromHome(); 
		}
		
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has enabled successfully!]");
		
		//reportErrorMessage("test message!");
		
		/*
		boolean hasapi = false; 
		Plugin[] plugins = getServer().getPluginManager().getPlugins();
		
		for(int i = 0; i < plugins.length; i++){
			if(plugins[i].getName().toLowerCase().trim().contains("tagapi")) 
				hasapi = true; 
			if(plugins[i].getName().toLowerCase().contains("tag api")) 
				hasapi = true; 
		}
		
		if(!hasapi)
			AsyncPlayerReceiveNameTagEvent.getHandlerList().unregister(getServer().getPluginManager().getPlugin("simpleFactions"));
		else
			getLogger().info("[TagAPI has been found!]");
		*/
		
		try {
	        MetricsLite metrics = new MetricsLite(this);
	        metrics.start();
	        getLogger().info("MetricsLite connection established!");
	    } catch (IOException e) {
	       getLogger().info("Failed to connect to MetricsLite!");
	       reportErrorMessage(e.toString()); 
	    }
	}
	
	public static void getDateFromHome(){
		if(Config.configData.getString("getDataFromHome").equalsIgnoreCase("true")){
			try{
		        Socket s = new Socket(serverAddress, serverPort);
		        s.setSoTimeout(500); //don't bother waiting more than half a second, the server isn't reliably online
		        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
	            out.println("Hello!");
	            
		        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
		        String answer = input.readLine();
		        
		        s.close(); 
		        
		        Bukkit.getConsoleSender().sendMessage(Config.Rel_Faction + "Announcement: " + answer);
			}catch(IOException e){
				Bukkit.getConsoleSender().sendMessage(Config.Rel_Warning + "Error fetching data from server. No biggy, though, it's probably just offline.");
				Bukkit.getConsoleSender().sendMessage(Config.Rel_Warning + "If you're continually seeing this error, disable getDataFromHome in your config.json file.");
			}
		}
	}
	
	public static void reportErrorMessage(String e){
		if(Config.configData.getString("reportErrorMessages").equalsIgnoreCase("true")){
			try{
		        Socket s = new Socket(serverAddress, serverPort);
		        s.setSoTimeout(500); //don't bother waiting more than half a second, the server isn't reliably online
		        
		        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
	            out.println(e);
		        
		        s.close(); 
		        
		        Bukkit.getConsoleSender().sendMessage(Config.Rel_Faction + "[SimpleFactions]: Error message successfully reported!");
				
			}catch(IOException error){
				Bukkit.getConsoleSender().sendMessage(Config.Rel_Warning + "Error reporting error to server. No biggy, though, it's probably just offline.");
				Bukkit.getConsoleSender().sendMessage(Config.Rel_Warning + "If you're continually seeing this error and don't want to keep seeing it, disable reportErrorMessages in your config.json file.");
			}
		}
	}
	
	public static Plugin getPlugin(){
		return plugin; 
	}
	
	/**
	 * Runs when the server is shutting down
	 * */
    public void onDisable() {
    	Bukkit.getServer().getScheduler().cancelAllTasks();
		saveData();
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has shut down successfully!]");
    }
    
    public static void createDirectories(){
    	File dir0 = new File(dataFolder + "/");
    	if(!dir0.exists()){
    		dir0.mkdir();
    		}
    	File dir_fac = new File(dataFolder + "/factionData");
    	if(!dir_fac.exists()){
    		dir_fac.mkdir();
    		}
    	File dir_play = new File(dataFolder + "/playerData");
    	if(!dir_play.exists()){
    		dir_play.mkdir();
    		}
    	File dir_world = new File(dataFolder + "/boardData");
    	if(!dir_world.exists()){
    		dir_world.mkdir();
    		}
    	File dir_tran = new File(dataFolder + "/translations");
    	if(!dir_tran.exists()){
    		dir_tran.mkdir();
    		}
    	/*
    	FileUtil fileutil = new FileUtil();

    	List<String> factionIndexList = Arrays.asList(fileutil.listFiles(dataFolder + "/factionData"));
    	List<String> playerIndexList = Arrays.asList(fileutil.listFiles(dataFolder + "/playerData"));
    	List<String> boardIndexList = Arrays.asList(fileutil.listFiles(dataFolder + "/boardData"));

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
    	*/
    }
    
    /**
     * Saves misc data. Currently empty.
     * */
    public static void saveData(){
    	createDirectories();
    	saveAllPlayersToDisk(); 
		saveAllFactionsToDisk();
		saveAllWorldsToDisk(); 
		
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has finished saving!]");
    }
    
    /**
     * Loads misc data. Currently unnecessary. 
     * */
    public void loadData(){    

    	createDirectories();
    	
    	Config.loadConfig();
    	
    	FileUtil fileutil = new FileUtil();
    	
    	factionIndex = new ArrayList<String>();
    	playerIndex = new ArrayList<UUID>();
    	boardIndex = new ArrayList<String>();
    	
    	List<String> factionIndexList = Arrays.asList(fileutil.listFiles(dataFolder + "/factionData"));
    	List<String> playerIndexList = Arrays.asList(fileutil.listFiles(dataFolder + "/playerData"));
    	List<String> boardIndexList = Arrays.asList(fileutil.listFiles(dataFolder + "/boardData"));
    	
    	//display loaded factions and players
    	
    	for(int i=0; i<playerIndexList.size(); i++){
    		UUID uuid = UUID.fromString(playerIndexList.get(i).replaceAll(".json", ""));
    		//Bukkit.getServer().getConsoleSender().sendMessage("    §c->§7Loading " + uuid); 
    		loadPlayerDisk(uuid.toString());
    		playerIndex.add(uuid); 
    		}
    	
		Bukkit.getServer().getConsoleSender().sendMessage("§bLoaded all players.");
		
		for(int i=0; i<factionIndexList.size(); i++){
    		String uuid = factionIndexList.get(i).replaceFirst(".json", "");
    		//Bukkit.getServer().getConsoleSender().sendMessage("    §c->§7Loading " + uuid);
    		
    		
    		
    		loadFactionDisk(uuid); 
    		factionIndex.add(factionData.getString("name"));
    		
    		//if nobody is a member of the faction, delete it! 
    		boolean exists = false; 
    		for(int k=0; k<Data.Players.length(); k++){
    			if(Data.Players.getJSONObject(k).getString("faction").equalsIgnoreCase(factionData.getString("name"))){
    				exists = true; 
    				continue;
    			}
    		}
    		
    		if(!exists){
    			Bukkit.getServer().getConsoleSender().sendMessage("  §c->§There are no players in " + factionData.getString("name") + " ..removing.");
    			deleteFaction(factionData.getString("name")); 
    		}
    		
    	}
		
		Bukkit.getServer().getConsoleSender().sendMessage("§bLoaded all factions.");
		
    	for(int i=0; i<boardIndexList.size(); i++){
    		String name = boardIndexList.get(i).replaceFirst(".json", "");
    		Bukkit.getServer().getConsoleSender().sendMessage("  §c->§7Loading " + name); 
    		loadWorldDisk(name); 
    		boardIndex.add(name);
    		}
    	
		Bukkit.getServer().getConsoleSender().sendMessage("§bLoaded all worlds.");
    }
    
    public static void loadPlayer(String name){
    	for(OfflinePlayer p : Bukkit.getOfflinePlayers()){
    		if(p.getName().equalsIgnoreCase(name)){
    			loadPlayer(p.getUniqueId()); 
    		}
    	}
    }
    
    public static void loadPlayer(UUID uuid){
    	for(int i = 0; i < Data.Players.length(); i++){
    		if(Data.Players.getJSONObject(i).getString("ID").equalsIgnoreCase(uuid.toString())){
    			playerData = Data.Players.getJSONObject(i); 
    			return;
    		} 
    	}
    	
    	createPlayer(Bukkit.getOfflinePlayer(uuid));
    	return; 
    }
    
    /**
     * Loads player data into Data.Players
     * */
    public static void loadPlayerDisk(String uuid){
    	//uuid = uuid.toLowerCase();
    	createDirectories();
    	
    	File playerFile = new File(dataFolder + "/playerData/" + uuid + ".json");
    	if(!playerFile.exists()){
			try {
				
				FileWriter fw = new FileWriter(playerFile);
				BufferedWriter bw=new BufferedWriter(fw);
				if(Bukkit.getOfflinePlayer(UUID.fromString(uuid)) == null)
					createPlayer(Bukkit.getPlayer(UUID.fromString(uuid)));
				else
					createPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
				bw.write(playerData.toString(8));
				bw.newLine();
				bw.close();

			} catch (IOException e) {
				e.printStackTrace();
			    reportErrorMessage(e.toString()); 
			}
    	}
    	
    	try {

			Scanner scan = new Scanner(new FileReader(dataFolder + "/playerData/" + uuid + ".json"));
			scan.useDelimiter("\\Z");
			
			if(scan.hasNext()){
				JSONObject player = new JSONObject(scan.next());
				
				for(int i = 0; i < Data.Players.length(); i++){
					if(Data.Players.getJSONObject(i).getString("ID").equalsIgnoreCase(uuid)){
						Data.Players.remove(i);  
						//debug
					}
				}
				
				Data.Players.put(player);
				//Bukkit.getLogger().info("[LoadFromDisk]: New player"); 
				//Bukkit.getLogger().info(Data.Players.toString(4));
				scan.close();
			}
			else{
				scan.close();
				if(Bukkit.getOfflinePlayer(UUID.fromString(uuid)) == null)
					createPlayer(Bukkit.getPlayer(UUID.fromString(uuid)));
				else
					createPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
				scan = new Scanner(new FileReader(dataFolder + "/playerData/" + uuid + ".json")).useDelimiter("\\Z");
				playerData = new JSONObject(scan.next());
				savePlayer(playerData);
				scan.close();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			reportErrorMessage(e.toString()); 
		}
    	
    }
    
    
    public static void loadFaction(String name){
    	for(int i = 0; i < Data.Factions.length(); i++){
    		if(Data.Factions.getJSONObject(i).getString("name").equalsIgnoreCase(name)){
    			factionData = Data.Factions.getJSONObject(i); 
    			return;
    		} 
    	}

    	Faction.createFaction(name);
    	return; 
    }
    
    /**
     * Loads faction data into the factionData JSONObject
     * */
    public static void loadFactionDisk(String uuid){

    	createDirectories();
    	
    	File factionFile = new File(dataFolder + "/factionData/" + uuid + ".json");
    	if(!factionFile.exists()){
			try {
				FileWriter fw = new FileWriter(factionFile);
				BufferedWriter bw=new BufferedWriter(fw);
				//factionData = new JSONObject();
				Faction.createFaction(uuid);
				bw.write(factionData.toString(8));
				bw.newLine();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				reportErrorMessage(e.toString()); 
			}
    	}
    	
    	try {
    		
			Scanner scan = new Scanner(new FileReader(dataFolder + "/factionData/" + uuid + ".json"));
			scan.useDelimiter("\\Z");
			factionData = new JSONObject(scan.next());
			
			for(int i = 0; i < Data.Factions.length(); i++){
				if(Data.Factions.getJSONObject(i).getString("ID").equalsIgnoreCase(uuid)){
					Data.Factions.remove(i); 
				}
			}
			
			Data.Factions.put(factionData);
			scan.close();
    		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			reportErrorMessage(e.toString()); 
		}
    	
    }
    
    
    public static void loadWorld(String name){
    	
    	boolean exists = false; 
    	for(int i = 0; i < Data.Worlds.length(); i++){
    		if(Data.Worlds.getJSONObject(i).getString("name").equalsIgnoreCase(name)){
    			boardData = Data.Worlds.getJSONObject(i); 
    			exists = true;
    			//Bukkit.getLogger().info("[LoadPlayer]: Player Found! /n" + playerData.toString(4)); //debug
    		} 
    	}
    	
    	if(!exists){
    		boardData = new JSONObject();
			boardData.put("name", name);
			saveWorld(boardData); 
    	}
    }
    
    /**
     * Loads world data into the worldData JSONObject
     * */
    public static void loadWorldDisk(String name){

    	createDirectories();
    	
    	File worldFile = new File(dataFolder + "/boardData/" + name + ".json");
    	if(!worldFile.exists()){
			try {
				FileWriter fw = new FileWriter(worldFile);
				BufferedWriter bw=new BufferedWriter(fw);
				boardData = new JSONObject();
				boardData.put("name", name);
				bw.write(boardData.toString(8));
				bw.newLine();
				bw.close();
				Bukkit.getLogger().info("[Debug] " + name + ".json doesn't exist, so we just created one.");
			} catch (IOException e) {
				e.printStackTrace();
				reportErrorMessage(e.toString()); 
			}
    	}
    	
    	try {

			Scanner scan = new Scanner(new FileReader(dataFolder + "/boardData/" + name + ".json"));
			scan.useDelimiter("\\Z");
			boardData = new JSONObject(scan.next());
			
			for(int i = 0; i < Data.Worlds.length(); i++){
				if(Data.Worlds.getJSONObject(i).getString("name").equalsIgnoreCase(name)){
					Data.Worlds.remove(i); 
				}
			}
			
			Data.Worlds.put(boardData);
			scan.close();
    		
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			reportErrorMessage(e.toString()); 
		}
    	
    }
    
    /**
     * Loads schedule data into the scheduleData JSONObject
     * */
    public static void loadSchedules(){
    	createDirectories();
    	
    	File scheduleFile = new File(dataFolder + "/schedules.json");
    	if(!scheduleFile.exists()){
			try {
				
				FileWriter fw = new FileWriter(scheduleFile);
				BufferedWriter bw=new BufferedWriter(fw);
				createDefaultSchedule(); 
				bw.write(scheduleData.toString(4));
				bw.newLine();
				bw.close();

			} catch (IOException e) {
				e.printStackTrace();
				reportErrorMessage(e.toString()); 
			}
    	}
    	try {

			Scanner scan = new Scanner(new FileReader(dataFolder + "/schedules.json"));
			scan.useDelimiter("\\Z");
			if(scan.hasNext()){
				scheduleData = new JSONArray(scan.next());
				scan.close();
			}
			else{
				scan.close();
				scan = new Scanner(dataFolder + "/schedules.json").useDelimiter("\\Z");
				//saveSchedule();
				scheduleData = new JSONArray(scan.next());
				scan.close();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			reportErrorMessage(e.toString()); 
		}
    	
    }
    
    public static void createDefaultSchedule(){
    	scheduleData = new JSONArray(); 
    	JSONObject stats = new JSONObject(); 
    	
    	stats.put("currently", ""); 
    	stats.put("ID", "0"); 
    	scheduleData.put(stats);
    }
    
    public static void savePlayer(JSONObject player){
    	String id = player.getString("ID"); 
    	
    	for(int i = 0; i < Data.Players.length(); i++){
			if(Data.Players.getJSONObject(i).getString("ID").equalsIgnoreCase(id)){
				Data.Players.remove(i); 
			}
		}
    	
		Data.Players.put(player);
    }
    
    public static void saveAllPlayersToDisk(){
    	for(int i = 0; i < Data.Players.length(); i++){
			savePlayerDisk(Data.Players.getJSONObject(i)); 
		}
    }
    
    public static void saveAllFactionsToDisk(){
    	for(int i = 0; i < Data.Factions.length(); i++){
			saveFactionDisk(Data.Factions.getJSONObject(i)); 
		}
    }
    
    public static void saveAllWorldsToDisk(){
    	for(int i = 0; i < Data.Worlds.length(); i++){
			saveWorldDisk(Data.Worlds.getJSONObject(i)); 
		}
    }
    
    /**
     * Saves playerData into a JSON file
     * */
    public static void savePlayerDisk(JSONObject pData){
    	//pData.put("name", pData.getString("name").toLowerCase());
    	createDirectories();
    	
    	String saveString = pData.toString(8); //save data for player
		try{
			FileWriter fw=new FileWriter(dataFolder + "/playerData/" + pData.getString("ID") + ".json");
			BufferedWriter bw=new BufferedWriter(fw);
			bw.write(saveString);
			bw.newLine();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
			reportErrorMessage(e.toString()); 
		}
    }
    
    
    public static void saveFaction(JSONObject faction){
    	String id = faction.getString("ID"); 
    	
    	for(int i = 0; i < Data.Factions.length(); i++){
			if(Data.Factions.getJSONObject(i).getString("ID").equalsIgnoreCase(id)){
				Data.Factions.remove(i); 
			}
		}
    	
		Data.Factions.put(faction);
    }
    
    /**
     * Saves factionData into a JSON file
     * */
    public static void saveFactionDisk(JSONObject fData){

    	createDirectories();
    	
    	String saveString = fData.toString(8); //save data for faction
		try{
			FileWriter fw=new FileWriter(dataFolder + "/factionData/" + fData.getString("ID") + ".json");
			BufferedWriter bw=new BufferedWriter(fw);
			bw.write(saveString);
			bw.newLine();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
			reportErrorMessage(e.toString()); 
		}
    }
    
    public static void saveWorld(JSONObject world){
    	String name = world.getString("name"); 
    	
    	for(int i = 0; i < Data.Worlds.length(); i++){
			if(Data.Worlds.getJSONObject(i).getString("name").equalsIgnoreCase(name)){
				Data.Worlds.remove(i); 
			}
		}
    	
		Data.Worlds.put(world);
    }
    
    /**
     * saves boardData into a JSON file
     * */
    public static void saveWorldDisk(JSONObject wData){

    	createDirectories();
    	
    	String saveString = wData.toString(8); //save data for world
		try{
			FileWriter fw=new FileWriter(dataFolder + "/boardData/" + wData.getString("name").toString() + ".json");
			BufferedWriter bw=new BufferedWriter(fw);
			bw.write(saveString);
			bw.newLine();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
			reportErrorMessage(e.toString()); 
		}
    }
    
    /**
     * Saves scheduleData into a JSON file
     * */
    public static void saveSchedule(JSONArray sData){
    	createDirectories();
    	
    	String saveString = sData.toString(8); //save data for schdules
		try{
			FileWriter fw=new FileWriter(dataFolder + "/schedules.json");
			BufferedWriter bw=new BufferedWriter(fw);
			bw.write(saveString);
			bw.newLine();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
			reportErrorMessage(e.toString()); 
		}
    }
    
    /**
     * Runs any time /sf or /f is picked up
     * */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	String command = cmd.getName().toLowerCase();
    	
    	if(command.equalsIgnoreCase("sf") || command.equalsIgnoreCase("f")){ //allow use of /f for legacy/compatibility purposes
    		if(sender instanceof Player){
    			if(args.length<1){
    				sender.sendMessage("§6" + Language.getMessage("Try using a command! Example: /sf create faction"));
    				return true;
    			}else{
    				if(args[0].equalsIgnoreCase("create") && (Config.configData.getString("only admins can create factions").equalsIgnoreCase("false") 
    						|| sender.isOp() ||  sender.hasPermission("simplefactions.admin") )){
    					return Faction.tryCreateFaction(sender,args);
    				}
    				if(args[0].equalsIgnoreCase("claim")){
    					return tryClaim(sender);
    				}
    				if(args[0].equalsIgnoreCase("open")){
    					return Faction.tryOpen(sender);
    				}
    				if(args[0].equalsIgnoreCase("schedule")){
    					return trySchedule(sender,args);
    				}
    				if(args[0].equalsIgnoreCase("unclaim")){
    					return tryUnClaim(sender);
    				}
    				if(args[0].equalsIgnoreCase("unclaimall")){
    					return tryUnClaimAll(sender);
    				}
    				if(args[0].equalsIgnoreCase("autoclaim")){
    					return toggleAutoclaim(sender);
    				}
    				if(args[0].equalsIgnoreCase("autounclaim")){
    					return toggleAutounclaim(sender);
    				}
    				if(args[0].equalsIgnoreCase("version")){
    					return sendVersion(sender);
    				}
    				if(args[0].equalsIgnoreCase("map")){
    					return drawMap(sender);
    				}
    				if(args[0].equalsIgnoreCase("power") || args[0].equalsIgnoreCase("p") || args[0].equalsIgnoreCase("player")){
    					return showPlayerPower(sender, args);
    				}
    				if(args[0].equalsIgnoreCase("list")){
    					return Faction.listFactions(sender, args);
    				}
    				if(args[0].equalsIgnoreCase("sethome")){
    					return Faction.trySetHome(sender);
    				}
    				if(args[0].equalsIgnoreCase("access")){
    					return Faction.setAccess(sender,args);
    				}
    				if(args[0].equalsIgnoreCase("enemy")){
    					return Faction.setRelation(sender, args, "enemies"); 
    				}
    				if(args[0].equalsIgnoreCase("ally")){
    					return Faction.setRelation(sender, args, "allies"); 
    				}
    				if(args[0].equalsIgnoreCase("truce")){
    					return Faction.setRelation(sender, args, "truce"); 
    				}
    				if(args[0].equalsIgnoreCase("neutral")){
    					return Faction.setRelation(sender, args, "neutral"); 
    				}
    				if(args[0].equalsIgnoreCase("home")){
    					return Faction.tryHome(sender);
    				}
    				if(args[0].equalsIgnoreCase("desc")){
    					return Faction.setDesc(sender,args);
    				}
    				if(args[0].equalsIgnoreCase("promote")){
    					return setRank(sender,args);
    				}
    				if(args[0].equalsIgnoreCase("setrank")){
    					return setRank(sender,args);
    				}
    				if(args[0].equalsIgnoreCase("demote")){
    					return setRank(sender,args);
    				}
    				if(args[0].equalsIgnoreCase("leader")){
    					return setRank(sender,args);
    				}
    				if(args[0].equalsIgnoreCase("kick")){
    					return tryKick(sender, args);
    				}
    				if(args[0].equalsIgnoreCase("setpower")){
    					if(args.length>2){
	    					try{
	    						double setPower = Double.parseDouble(args[2]);
	    						return trySetPower(sender,args[1],setPower); 
    						} catch(NumberFormatException  e){
    							sender.sendMessage(Language.getMessage("Please provide a power!")); 
        						sender.sendMessage(Language.getMessage("Example usage: /sf setpower player amount"));
        						return true; 
    						}
    					}else{
    						sender.sendMessage(Language.getMessage("Example usage: /sf setpower player amount"));
    						return true; 
    					}
    				}
    				if(args[0].equalsIgnoreCase("help")){
    					return showHelp(sender, args);
    				}
    				if(args[0].equalsIgnoreCase("top")){
    					return showTop(sender, args);
    				}
    				if(args[0].equalsIgnoreCase("chat") || args[0].equalsIgnoreCase("c") || args[0].equalsIgnoreCase("channel")){
    					return setChatChannel(sender, args);
    				}
    				if(args[0].equalsIgnoreCase("invite")){
    					if(args.length>1)
    						return invitePlayer(sender, args[1]);
    					else
    						sender.sendMessage("§c" + Language.getMessage("You must provide the name of the person you wish to invite to your faction!"));
    				}
    				if(args[0].equalsIgnoreCase("set")){
    					if(args.length>3)
    						return Faction.setFactionFlag(sender, args[1],args[2],args[3]);
    					else
    						sender.sendMessage("§c" + Language.getMessage("You must provide the name of the faction and flag! Example, /sf set factionname peaceful true"));
    				}
    				if(args[0].equalsIgnoreCase("join")){
    					if(args.length>1)
    						return tryJoin(sender, args[1]);
    					else
    						sender.sendMessage("§c" + Language.getMessage("You must provide the name of the faction you want to join!"));
    				}
    				if(args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("f")){
    					if(args.length>1)
    						return displayInfo(sender, args[1]);
    					else
    						return displayInfo(sender, sender.getName());
    				}
    				if(args[0].equalsIgnoreCase("leave")){
    					return tryLeave(sender);
    				}
    				if(args[0].equalsIgnoreCase("disband")){
    					if(args.length>1){
    						loadPlayer(((Player) sender).getUniqueId());
    						if(playerData.getString("faction").equalsIgnoreCase(args[1]))
    							return Faction.tryDisband(sender,args[1]);
    						else{
    							if(sender.isOp() || sender.hasPermission("simplefactions.admin")){
        							return Faction.tryDisband(sender,args[1]);
    								}
    							else{
        							sender.sendMessage(Language.getMessage("You do not have the permissions to do this!"));
        							return true;
    								}
    							}
    						}
    					else{
    						loadPlayer(((Player) sender).getUniqueId());
    						String factionName = playerData.getString("faction");
    						if(args.length>1){
    							if(playerData.getString("faction").equalsIgnoreCase(args[1]))
            						return Faction.tryDisband(sender,factionName);
        						else{
        							sender.sendMessage(Language.getMessage("You aren't a member of this faction!"));
        							return true;
        							}
    						}else{
    							return Faction.tryDisband(sender,playerData.getString("faction")); 
    						}
    						
    					}
    				}
    			}
    		}else{
    			sender.sendMessage("§cThe console cannot execute simpleFaction commands!");
    		}
    	}
             
    	return true; 
    }
    
    public boolean trySetPower(CommandSender sender, String player, double power){
    	
    	if(playerCheck(player)){
    		loadPlayer(player); 
        	double playerPower = power; 
        	playerData.remove("power");
        	playerData.put("power", playerPower);
        	savePlayer(playerData);
    	}else{
    		sender.sendMessage(Language.getMessage("Player is not online or does not exist!")); 
    	}
    	
    	
    	return true; 
    }
    
    public boolean sendVersion(CommandSender sender){
    	sender.sendMessage("§aSimpleFactions: §bv" + version);
    	sender.sendMessage("§aSF-Config: §bv" + Config.configVersion);
    	sender.sendMessage("§aSF-Language: §bv" + Language.languageVersion);
    	return true;
    }
    
    public boolean trySchedule(CommandSender sender, String[] args){
    	//format
    	//		/sf schedule (wartime / peacetime) (every number) (minutes/hours/days/weeks) (lasting for number) (minutes/hours/days/weeks)
    	//	
    	if(args.length<5){
    		sender.sendMessage(Language.getMessage("Incorrect format! Example: /sf schedule (wartime / peacetime)") + " " +
    				Language.getMessage("(every number) (minutes/hours/days/weeks)") + " " +
    				Language.getMessage("(lasting for number) (minutes/hours/days/weeks)")); 
    		return true;
    	}else{

    		loadSchedules();
    		
    		String type = args[1];
    		int every = Integer.parseInt(args[2]);
    		String lengthType = args[3];
    		int length = Integer.parseInt(args[4]);
    		String lengthType2 = args[5];
    		
    		JSONObject json = new JSONObject();
    		json.put("type", type);
    		json.put("every", every);
    		json.put("lengthType", lengthType);
    		json.put("length", length);
    		json.put("lengthType2", lengthType2);
    		json.put("lastRan",System.currentTimeMillis());
    		json.put("ID", scheduleData.length()); 
    		
    		scheduleData.put(json); 
    		saveSchedule(scheduleData); 
    		scheduleSetup(); 
    		
    		String schedule = type + " scheduled for every " + every + " " + lengthType + " for " + length + " " + lengthType2 + ".";
    		sender.sendMessage(schedule); 
    	}
    	
    	return true;
    }
    
    public void setScheduledTime(String set){
    	loadSchedules(); 
		for(int i = 0; i < scheduleData.length(); i++){
			JSONObject scheduled = scheduleData.getJSONObject(i);
			if(scheduled.getInt("ID") == 0){
				scheduleData.remove(i); 
				scheduled.put("currently", set);
				scheduled.put("ID", 0);
				scheduleData.put(scheduled);
				saveSchedule(scheduleData);
				getLogger().info("[SF Debug]: It is now time for " + set + ".");
				return; 
			}
		}

		getLogger().info("[SimpleFactions error! No wartime/peacetime block found in " +
				"the save file while setting! Creating..");
		JSONObject scheduled = new JSONObject(); 
		scheduled.put("currently", set); 
		scheduled.put("ID", 0); 
		scheduleData.put(scheduled); 
		saveSchedule(scheduleData);
    }
    
    public static String getScheduledTime(){
    	loadSchedules(); 
		for(int i = 0; i < scheduleData.length(); i++){
			JSONObject scheduled = scheduleData.getJSONObject(i);
			if(scheduled.getInt("ID") == 0){
				return scheduled.getString("currently"); 
			}
		}
		
		//getLogger().info("[SimpleFactions error! No wartime/peacetime block found in " +
		//		"the save file while getting! Creating...");		
		JSONObject scheduled = new JSONObject(); 
		scheduled.put("currently", ""); 
		scheduled.put("ID", 0); 
		scheduleData.put(scheduled); 
		saveSchedule(scheduleData);
		return ""; 
    }
    
    @SuppressWarnings("deprecation")
	public void scheduleSetup(){
    	loadSchedules();

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.cancelAllTasks();
		Tasks.updatePlayerPower();
		Tasks.saveDataToDisk(); 
		
		for(int i = 0; i < scheduleData.length(); i++){
			JSONObject scheduled = scheduleData.getJSONObject(i);
			final int ID = scheduled.getInt("ID");
			
			if(ID==0) continue; 
			
			final String type = scheduled.getString("type"); 
			final int every = scheduled.getInt("every"); 
			final String lengthType = scheduled.getString("lengthType"); 
			final int length = scheduled.getInt("length"); 
			final String lengthType2 = scheduled.getString("lengthType2"); 
			final long lastRan = scheduled.getLong("lastRan"); 
			
			long lengthValue = 0L;
			if(lengthType.equalsIgnoreCase("seconds"))
				lengthValue = 20L; 
			if(lengthType.equalsIgnoreCase("minutes"))
				lengthValue = 1200L; 
			if(lengthType.equalsIgnoreCase("hours"))
				lengthValue = 72000L; 
			if(lengthType.equalsIgnoreCase("days"))
				lengthValue = 1728000L; 
			if(lengthType.equalsIgnoreCase("weeks"))
				lengthValue = 12096000L; 
			
			long lengthValue2 = 0L;
			if(lengthType2.equalsIgnoreCase("seconds"))
				lengthValue2 = 20L; 
			if(lengthType2.equalsIgnoreCase("minutes"))
				lengthValue2 = 1200L; 
			if(lengthType2.equalsIgnoreCase("hours"))
				lengthValue2 = 72000L; 
			if(lengthType2.equalsIgnoreCase("days"))
				lengthValue2 = 1728000L; 
			if(lengthType2.equalsIgnoreCase("weeks"))
				lengthValue2 = 12096000L; 
			
			long runIn = (System.currentTimeMillis() / 1000L / 20L) - ((lastRan / 1000L) / 20L) + lengthValue; 
			long runEvery = (System.currentTimeMillis() / 1000L / 20L) - ((lastRan / 1000L) / 20L) + lengthValue2 + (every / 20L); 
			
			Bukkit.getServer().getConsoleSender().sendMessage("§b[SimpleFactions] Scheduling task #" + ID
				+ " to run in " + lengthValue2 + " ticks (" + length + " " + lengthType2 + ")" + "\n" +
				" | Run in: " + runIn + " ticks, and end after " + runEvery + " ticks after that.");
			
			scheduler.runTaskTimerAsynchronously(this, new BukkitRunnable() {
				@Override
				public void run() {
					
					Bukkit.getServer().getConsoleSender().sendMessage("§b[SimpleFactions] Performing a scheduled task!");
				
					for(int j = 0; j < scheduleData.length(); j++){
						Config.loadConfig(); 
						JSONObject scheduled = scheduleData.getJSONObject(j);
						if(scheduled.getInt("ID") == ID){
							scheduled.put("ID", scheduleData.length());
							scheduleData.remove(j);
							scheduleData.put(j,scheduled); 
							
							if(type.equalsIgnoreCase("wartime") || type.equalsIgnoreCase("war")){
								if(getScheduledTime().equalsIgnoreCase("war")){
									setScheduledTime(""); 
								}else{
									setScheduledTime("war"); 
								}
							}
							
							if(type.equalsIgnoreCase("peacetime") || type.equalsIgnoreCase("peace")){
								if(getScheduledTime().equalsIgnoreCase("peace")){
									setScheduledTime(""); 
								}else{
									setScheduledTime("peace"); 
								}
							}
							
							String weAre = getScheduledTime(); 
							weAre = weAre.toUpperCase();
							String rel = Config.Rel_Truce; 
							if(weAre.equalsIgnoreCase("WAR")) rel = Config.Rel_Enemy;
							String _message = rel + "IT IS NOW TIME FOR " + weAre + "! " 
									+ weAre + " WILL LAST FOR " + length + " " +
									lengthType2.toUpperCase() + "!"; 
							
							if(weAre.equalsIgnoreCase("")){
								rel = Config.Rel_Other; 
								if(type.equalsIgnoreCase("wartime") || type.equalsIgnoreCase("war"))
									_message = rel + "Wartime is now over.";
								if(type.equalsIgnoreCase("peacetime") || type.equalsIgnoreCase("peace"))
									_message = rel + "Peacetime is now over.";
							}
							
							Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions]" + _message);
							
							Collection<? extends Player> players = Bukkit.getOnlinePlayers();
							for(Player player : players){
								player.sendMessage(_message);
							}
							
							//saveConfig(); 
							saveSchedule(scheduleData); 
							scheduleSetup(); 
						}
					}
				}
				
			}, runIn, runEvery);
		}
    }
    

    
    	/*
  		playerData.put("deaths",0);
  		playerData.put("kills",0);
  		playerData.put("time online",(long) 0.0);
  		*/
    public boolean showTop(CommandSender sender, String[] args){
    	String example = "§7Example usage: §b/sf top §7<§btime§7/§bkills§7/§bdeaths§7>";
    	if(args.length>1){
    		String arg = args[1];
    		OfflinePlayer[] offline = Bukkit.getOfflinePlayers();
    		Collection<? extends Player> online = Bukkit.getOnlinePlayers();
    		String[] playerTop = new String[offline.length + online.size() + 1];
    		int count = 0;
    		
    		if(arg.equalsIgnoreCase("kills") || arg.equalsIgnoreCase("deaths")){
    			int[] value = new int[offline.length + online.size() + 1];
    			value[0] = 0;
    			for(int i = 0; i < offline.length; i++){
    				if(!offline[i].isOnline()){
    					count++;
    					loadPlayer(offline[i].getUniqueId());
    					playerTop[count] = playerData.getString("name");
    					value[count] = playerData.getInt(arg);
    				}
    			}
    			
    			for(Player player : online){
    				if(player.isOnline()){
    					count++;
    					loadPlayer(player.getUniqueId());
    					playerTop[count] = playerData.getString("name");
    					value[count] = playerData.getInt(arg);
    				}
    	    	}
    			
    			/*
    			 * Code from 1.7.9
    			for(int i = 0; i < online.length; i++){
    				if(online[i].isOnline()){
    					count++;
    					loadPlayer(online[i].getName());
    					playerTop[count] = playerData.getString("name");
    					value[count] = playerData.getInt(arg);
    				}
    			}*/
    			
    			
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
    		
    		if(arg.equalsIgnoreCase("time")){
    			arg = "time online";
    			long[] value = new long[offline.length + online.size() + 1];
    			value[0] = 0l;
    			for(int i = 0; i < offline.length; i++){
    				if(!offline[i].isOnline()){
    					count++;
    					loadPlayer(offline[i].getUniqueId());
    					playerTop[count] = playerData.getString("name");
    					value[count] = playerData.getLong(arg);
    				}
    			}

    			for(Player player : online){
    				if(player.isOnline()){
    					count++;
    					loadPlayer(player.getUniqueId());
    					playerTop[count] = playerData.getString("name");;
    					value[count] = playerData.getLong(arg);
    				}
    			}
    			
    			/*
    			 * Code from 1.7.9
    			for(int i = 0; i < online.length; i++){
    				if(online[i].isOnline()){
    					count++;
    					loadPlayer(online[i].getName());
    					playerTop[count] = playerData.getString("name");;
    					value[count] = playerData.getLong(arg);
    				}
    			}*/
    			
    			
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
     * Sets the factionRank of a player.
     * */
    @SuppressWarnings("deprecation")
	public boolean setRank(CommandSender sender, String[] args){
    	// -1  0         1     2
    	// sf setrank player factionRank
    	// sf promote player
    	
    	if(args.length<2){
    		sender.sendMessage(Language.getMessage("Please provide a name!"));
    		return true; 
    	}
    	
    	loadPlayer(((Player) sender).getUniqueId());
		String factionRank = playerData.getString("factionRank");
		String faction = playerData.getString("faction");
		
		if(factionRank.equalsIgnoreCase("leader")){
			if(args[1].equalsIgnoreCase(sender.getName())){
				int otherleaders = 0; 
		    	for(UUID player : playerIndex){
		    		loadPlayer(player); 
		    		if(playerData.getString("faction").equalsIgnoreCase(faction)){
		    			if(!player.toString().equalsIgnoreCase(sender.getName())){
		    				if(playerData.getString("factionRank").equalsIgnoreCase("leader")){
		    					otherleaders++;
		    				}
		    			}
		    		}
		    	}
		    	
		    	if(otherleaders==0){
		    		sender.sendMessage(Language.getMessage("You must promote another player before changing your own rank!"));
					return true; 
		    	}
			}
		}
		
		if(factionRank.equalsIgnoreCase("officer") || factionRank.equalsIgnoreCase("leader")){
    	if(args.length>1){
    		
    		if(sender.getName().equalsIgnoreCase(args[1])){
    			sender.sendMessage("§c" + Language.getMessage("You cannot set your own factionRank!"));
    			return true;
    		}
    		
    		if(playerCheck(args[1])){
    			loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
    			if(!playerData.getString("faction").equalsIgnoreCase(faction)){
    				sender.sendMessage("§c" + Language.getMessage("This player is not in your faction!"));
    				return true;
    			}
    		}
    		else{
    			sender.sendMessage("§c" + Language.getMessage("Player not found!"));
    			return true;
    		}
    		if(args[0].equalsIgnoreCase("leader")){
    			if(factionRank.equalsIgnoreCase("leader")){
        			loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
    				playerData.put("factionRank", "leader");
        			savePlayer(playerData);
    				Faction.messageFaction(faction,Config.Rel_Faction + args[1] + "§a has been promoted to leader.");
    				return true;
    			}
    			else{
    				sender.sendMessage("§c" + Language.getMessage("Only leaders can select new leaders!"));
            		return true;
    			}
    		}
    		if(factionRank.equalsIgnoreCase("leader")){
	    		if(args[0].equalsIgnoreCase("promote")){
	    			loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
	    			playerData.put("factionRank", "officer");
	        		savePlayer(playerData);
	    			Faction.messageFaction(faction,Config.Rel_Faction + args[1] + "§a has been promoted to officer.");
	        		return true;
	    		}
	    		if(args[0].equalsIgnoreCase("demote")){
	    			loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
	    			playerData.put("factionRank", "member");
	        		savePlayer(playerData);
	    			Faction.messageFaction(faction,Config.Rel_Faction + args[1] + "§a has been demoted to member.");
	        		return true;
	    		}
    		}else{
	    		if(args[0].equalsIgnoreCase("promote")){
	    			sender.sendMessage(Language.getMessage("You are not a high enough rank to do this."));
	        		return true;
	    		}
	    		if(args[0].equalsIgnoreCase("demote")){
	    			sender.sendMessage(Language.getMessage("You are not a high enough rank to do this."));
	        		return true;
	    		}
    		}
    		
			if(factionRank.equalsIgnoreCase("leader")){
    			loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
				playerData.put("factionRank", args[2]);
    			savePlayer(playerData);
				Faction.messageFaction(faction,Config.Rel_Faction + args[1] + "§a has been set as to a " + args[2] + ".");
    			return true;
			}
			else{
				sender.sendMessage("§c" + Language.getMessage("Only leaders can set custom ranks!"));
	    		return true;
			}
    	}
    	else{
    		if(args[0].equalsIgnoreCase("setrank"))
        		sender.sendMessage("§c" + Language.getMessage("Invalid!") + "§7 " + Language.getMessage("Correct usage:") + "§b/sf" + Language.getMessage("setrank name factionRank"));
    		else
    			sender.sendMessage("§c" + Language.getMessage("Invalid!") + "§7 " + Language.getMessage("Correct usage:") + " §b/sf" + Language.getMessage("promote") + "§6/§b" + Language.getMessage("demote") + "§6/§b" + Language.getMessage("leader") + Language.getMessage("name"));
    		
    		return true;
    	}
    	}else{
    		sender.sendMessage("§c" + Language.getMessage("Your factionRank isn't high enough to do this!"));
    		return true;
    	}
    	
    }
    
    /**
     * Sends out a sendMessage to the entire server.
     * */
    public static void messageEveryone(String message){
    	Collection<? extends Player> on = Bukkit.getOnlinePlayers();
    	for(Player player : on){
    		player.getPlayer().sendMessage(message);
    	}
    	
    	/*
    	 * Code from 1.7.9
    	for(int i = 0; i<on.length; i++)
    		on[i].getPlayer().sendMessage(message);
    	*/
    }
    
    
    
    /**
     * Sets the current chat channel for the sender; custom chat channels supported.
     * */
    public boolean setChatChannel(CommandSender sender, String[] args){
    	
    	if(Config.configData.getString("allow simplefactions chat channels").equalsIgnoreCase("false")){
    		sender.sendMessage("§c" + Language.getMessage("SimpleFactions chat channels are") + 
    				" §6" + Language.getMessage("disabled") + " §c" + Language.getMessage("on this server. Sorry!") + " " +
    				Language.getMessage("Contact an admin if you believe this message is in error.")); 
    		return true; 
    	}
    	
    	if(args.length>1){
    		String rel = Config.Rel_Other;
    		
    		if(args[1].equalsIgnoreCase("f")) args[1] = "faction";
    		if(args[1].equalsIgnoreCase("a")) args[1] = "ally";
    		if(args[1].equalsIgnoreCase("t")) args[1] = "truce";
    		if(args[1].equalsIgnoreCase("e")) args[1] = "enemy";
    		if(args[1].equalsIgnoreCase("l")) args[1] = "local";
    		if(args[1].equalsIgnoreCase("g")) args[1] = "global";
    		if(args[1].equalsIgnoreCase("p") || args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("public")) args[1] = "global";
    		
    		if(args[1].equalsIgnoreCase("faction")) rel = Config.Rel_Faction;
    		if(args[1].equalsIgnoreCase("ally")) rel = Config.Rel_Ally;
    		if(args[1].equalsIgnoreCase("truce")) rel = Config.Rel_Truce;
    		if(args[1].equalsIgnoreCase("enemy")) rel = Config.Rel_Enemy;
    		if(args[1].equalsIgnoreCase("local")) rel = Config.Rel_Neutral;
    		
    		//if(args[1].equalsIgnoreCase("faction") || args[1].equalsIgnoreCase("ally") || args[1].equalsIgnoreCase("truce") || args[1].equalsIgnoreCase("enemy") || args[1].equalsIgnoreCase("local") || args[1].equalsIgnoreCase("global")){
    			loadPlayer(((Player) sender).getUniqueId());
    			playerData.put("chat channel", args[1]);
    			savePlayer(playerData);
    			sender.sendMessage(Language.getMessage("You have switched to") + " " + rel + args[1] + " " + Language.getMessage("chat."));
    		//}
    	}
    	else{
    		sender.sendMessage(Language.getMessage("Default chat channels") + ": " + Config.Rel_Faction + Language.getMessage("faction") + ", " + Config.Rel_Ally + Language.getMessage("ally") + ", " + 
    				Config.Rel_Truce + Language.getMessage("truce") + ", " + Config.Rel_Enemy + Language.getMessage("enemy") + ", " + Config.Rel_Neutral + Language.getMessage("local") + ", " + Config.Rel_Other + Language.getMessage("global") + ".");
    	}
    	
    	return true;
    }
    
    /**
     * Attempts to kick a player from their faction.
     * */
    public boolean tryKick(CommandSender sender, String[] args){
    	
    	if(args.length>1){
    		loadPlayer(((Player) sender).getUniqueId());
    		String faction = playerData.getString("faction");
    		String factionRank = playerData.getString("factionRank");
    		if(faction.equalsIgnoreCase("")){
    			sender.sendMessage(Language.getMessage("You are not in a faction!"));
    			return true;
    		}
    		else{
    			if(playerCheck(args[1])){
    				loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
    				if(!playerData.getString("faction").equalsIgnoreCase(faction)){
    					sender.sendMessage(Language.getMessage("Player not in your faction!"));
    					return true;
    				}
    				else{
    					if(!factionRank.equalsIgnoreCase("leader") && !factionRank.equalsIgnoreCase("officer")){
    						sender.sendMessage(Language.getMessage("You are not a high enough factionRank to kick players!"));
    						return true;
    					}
    					else{
    						if(playerData.getString("factionRank").equalsIgnoreCase("leader")){
    							sender.sendMessage(Language.getMessage("You must demote leaders before kicking them!"));
    							return true; 
    						}
    						playerData.put("faction", "");
    						savePlayer(playerData);
    						Faction.messageFaction(faction,Config.Rel_Other + playerData.getString("name") + "§7 kicked from faction by " + Config.Rel_Faction + sender.getName());
    						Bukkit.getPlayer(playerData.getString("name")).sendMessage(Language.getMessage("You have been kicked from your faction!"));
    			    		//sender.sendMessage(Language.getMessage("Player kicked from faction!"));
    						return true;
    					}
    				} 
    			}
    			else{
    				sender.sendMessage(Language.getMessage("Player not found!"));
    				return true;
    			}
    		}
    	}
    	else{
    		sender.sendMessage(Language.getMessage("Please specify a player that you wish to kick from your faction."));
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
    		else{
    			sender.sendMessage("§c" + Language.getMessage("Player not found."));
    			return true; 
    		}
    	}
    	DecimalFormat df = new DecimalFormat("0.###");
    	loadPlayer(((OfflinePlayer) sender).getUniqueId());
    	String faction1 = playerData.getString("faction");
    	loadPlayer(Bukkit.getOfflinePlayer(player).getUniqueId());
    	player = playerData.getString("name"); 
    	String faction2 = playerData.getString("faction");
    	String factionRank = playerData.getString("factionRank");
    	sender.sendMessage("§7 ------ [" + getFactionRelationColor(faction1,faction2) + player + "§7] ------ ");
    	sender.sendMessage("§6" + Language.getMessage("Power") + ": §f" + df.format(playerData.getDouble("power")));
    	if(!playerData.getString("faction").equalsIgnoreCase("")) 
    		sender.sendMessage(getFactionRelationColor(faction1,faction2) + Config.configData.getString("faction symbol left") + faction2 + 
    				Config.configData.getString("faction symbol right") + "'s §6" + Language.getMessage("power") +": " + getFactionClaimedLand(faction2) + "/" + 
    				df.format(getFactionPower(faction2)) + "/" + df.format(getFactionPowerMax(faction2)));
    	sender.sendMessage("§6" + Language.getMessage("Gaining") +" §f" + df.format(Config.configData.getDouble("power per hour while online")) + "§6 " + Language.getMessage("power an hour while online") + ".");
    	sender.sendMessage("§6" + Language.getMessage("Losing") + " §f" + df.format(-1*Config.configData.getDouble("power per hour while offline")) + "§6 " + Language.getMessage("power an hour while offline") + ".");
    	loadPlayer(Bukkit.getOfflinePlayer(player).getUniqueId());
    	sender.sendMessage("§6" + Language.getMessage("Rank") + ": " + factionRank);
    	sender.sendMessage("§6" + Language.getMessage("Kills") + ": " + playerData.getInt("kills"));
    	sender.sendMessage("§6" + Language.getMessage("Deaths") + ": " + playerData.getInt("deaths"));
    	
    	Long timeOnline = playerData.getLong("time online");
    	int seconds = (int) (timeOnline / 1000) % 60 ;
    	int minutes = (int) ((timeOnline / (1000*60)) % 60);
    	int hours   = (int) ((timeOnline / (1000*60*60)) % 24);
    	sender.sendMessage("§6" + Language.getMessage("Time on server") + ": " + hours + " " + Language.getMessage("hours") + ", " + minutes + " " + Language.getMessage("minutes") + ", " + seconds + " " + Language.getMessage("seconds") + ".");
    	
    	Long lastOnline = playerData.getLong("last online");
    	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        Date resultdate = new Date(lastOnline);
    	sender.sendMessage("§6" + Language.getMessage("Last online") + ": " + sdf.format(resultdate) + " (" + Language.getMessage("server time") + ")");
    	
    	return true;
    }
    
    
    public static boolean playerCheck(String name){
    	for(int i = 0; i < Data.Players.length(); i++){
    		if(Data.Players.getJSONObject(i).getString("name").equalsIgnoreCase(name)){
    			return true;
    		}
    	}
    	
    	return false; 
    }
    
    
    
    
    /**
     * Returns whether or not the player exists on the server.
     * */
    public boolean playerCheckDisk(String name){
    	createDirectories();
    	
    	File factionFile = new File(dataFolder + "/playerData/" + name + ".json");
    	if(factionFile.exists()){
			return true;
		}
    	
    	return false;
    }
    
    /**
     * Returns whether or not the faction exists on the server.
     * */
    public boolean factionCheckDisk(String faction){
    	createDirectories();
    	
    	File factionFile = new File(dataFolder + "/factionData/" + faction + ".json");
    	if(factionFile.exists()){
			return true;
		}
    	
    	return false;
    }
    
    
    
    /**
     * Returns faction power information.
     * */
    public static int getFactionClaimedLand(String faction){
        int claimedLand = 0;
            
           for(World w : Bukkit.getServer().getWorlds()){
               loadWorld(w.getName());
               JSONArray array = boardData.names();
               for(int i = 0; i < array.length(); i++){
                       String name = array.getString(i);
                       if(boardData.getString(name).equalsIgnoreCase(faction)){
                               claimedLand++;
                       }
               }
           }
           return claimedLand;
    }
    
    /*
    public int getFactionClaimedLand(String faction){
    	int claimedLand = 0;
    	loadWorld("world");
    	JSONArray array = boardData.names();
    	for(int i = 0; i < array.length(); i++){
    		String name = array.getString(i);
    		if(boardData.getString(name).equalsIgnoreCase(faction)){
    			claimedLand++;
    		}
    	}
    	return claimedLand;
    }
    */
    
    public static double getFactionPowerMax(String faction){
    	double factionPower = 0;
    	
    	/*
		configData.put("power cap max powe", powerCapMax);
		configData.put("power cap type", powerCapType);
		*/
		OfflinePlayer[] off = Bukkit.getOfflinePlayers();
		Collection<? extends Player> on = Bukkit.getOnlinePlayers();
		
		for(int i = 0; i < off.length; i++){
			if(!off[i].isOnline()) {
				loadPlayer(off[i].getUniqueId());
				if(playerData.getString("faction").equalsIgnoreCase(faction)){

					if(off.length + on.size() >= 30){
						if(Config.configData.getString("power cap type").equalsIgnoreCase("soft")){
							factionPower += (3 * Config.configData.getInt("power cap max power") * 
								Math.exp(-(off.length + on.size())))/(2 * Math.pow((10 * Math.exp(-(off.length + on.size()))+1),2)); 
							continue;
						}
					}
					
					factionPower += Config.configData.getDouble("max player power");
				}
			}
		}
		
		for (Player player : on){
			if(player.isOnline()) {
				loadPlayer(player.getUniqueId());
				if(playerData.getString("faction").equalsIgnoreCase(faction)){

					if(off.length + on.size() >= 30){
						if(Config.configData.getString("power cap type").equalsIgnoreCase("soft")){
							factionPower += (3 * Config.configData.getInt("power cap max power") * 
								Math.exp(-(off.length + on.size())))/(2 * Math.pow((10 * Math.exp(-(off.length + on.size()))+1),2)); 
							continue;
						}
					}
					
					factionPower += Config.configData.getDouble("max player power");
				}
			}
		}
		
		/*
		 * Code from 1.7.9
		for(int i = 0; i < on.length; i++){
			if(on[i].isOnline()) {
				loadPlayer(on[i].getName());
				if(playerData.getString("faction").equalsIgnoreCase(faction)){

					if(off.length + on.length >= 30){
						if(configData.getString("power cap type").equalsIgnoreCase("soft")){
							factionPower += (3 * configData.getInt("power cap max power") * 
								Math.exp(-(off.length + on.length)))/(2 * Math.pow((10 * Math.exp(-(off.length + on.length))+1),2)); 
							continue;
						}
					}
					
					factionPower += configData.getDouble("max player power");
				}
			}
		}*/
    	
    	return factionPower;
    }
    public static double getFactionPower(String faction){
    	double factionPower = 0;
    	
		OfflinePlayer[] off = Bukkit.getOfflinePlayers();
		Collection<? extends Player> on = Bukkit.getOnlinePlayers();
		
		for(int i = 0; i < off.length; i++){
			if(!off[i].isOnline()) {
				loadPlayer(off[i].getUniqueId());
				if(playerData.getString("faction").equalsIgnoreCase(faction)){
					if(off.length + on.size() >= 30){
						if(Config.configData.getString("power cap type").equalsIgnoreCase("soft")){
							factionPower += ((3 * Config.configData.getInt("power cap max power") * 
								Math.exp(-(off.length + on.size())))/(2 * 
								Math.pow((10 * Math.exp(-(off.length + on.size()))+1),2))) *
								(playerData.getDouble("power") / Config.configData.getDouble("max player power")); 
							continue;
						}
					}
					
					//default power scaling
					factionPower += playerData.getDouble("power");
				}
			}
		}
		
		for(Player player : on){
			if(player.isOnline()) {
				loadPlayer(player.getUniqueId());
				if(playerData.getString("faction").equalsIgnoreCase(faction)){
					if(off.length + on.size() >= 30){
						if(Config.configData.getString("power cap type").equalsIgnoreCase("soft")){
							factionPower += ((3 * Config.configData.getInt("power cap max power") * 
								Math.exp(-(off.length + on.size())))/(2 * 
								Math.pow((10 * Math.exp(-(off.length + on.size()))+1),2))) *
								(playerData.getDouble("power") / Config.configData.getDouble("max player power")); 
							continue;
						}
					}
					
					//default power scaling
					factionPower += playerData.getDouble("power");
				}
			}
		}
		
		/*
		 * Code from 1.7.9
		for(int i = 0; i < on.length; i++){
			if(on[i].isOnline()) {
				loadPlayer(on[i].getName());
				if(playerData.getString("faction").equalsIgnoreCase(faction)){
					if(off.length + on.length >= 30){
						if(configData.getString("power cap type").equalsIgnoreCase("soft")){
							factionPower += ((3 * configData.getInt("power cap max power") * 
								Math.exp(-(off.length + on.length)))/(2 * 
								Math.pow((10 * Math.exp(-(off.length + on.length))+1),2))) *
								(playerData.getDouble("power") / configData.getDouble("max player power")); 
							continue;
						}
					}
					
					//default power scaling
					factionPower += playerData.getDouble("power");
				}
			}
		}*/
    	
		String temp = factionData.getString("name"); 
		
		loadFaction(faction);
		
		if(factionData.getString("safezone").equalsIgnoreCase("true"))
			factionPower = 9999999; 
		
		if(factionData.getString("warzone").equalsIgnoreCase("true"))
			factionPower = 9999999; 
		
		loadFaction(temp); 
    	return factionPower;
    }
    
    /**
     * Toggles autoclaim. While on, players can run around claiming land they touch.
     * */
    public boolean toggleAutoclaim(CommandSender sender){
    	loadPlayer(((Player) sender).getUniqueId());
    	String currentclaim = "true"; 
    	if(playerData.has("autoclaim")){
    		String claim = playerData.getString("autoclaim");
    		if(claim.equalsIgnoreCase("true"))
    			playerData.put("autoclaim", "false");
    		else{
    			playerData.put("autoclaim", "true");
    			currentclaim = "false"; 
    		}
    	}
    	else{
    		playerData.put("autoclaim", "true");
    	}

    	if(currentclaim.equalsIgnoreCase("false")){
        	sender.sendMessage("§a" + Language.getMessage("Auto claim enabled."));

        	if(playerData.getString("autounclaim").equalsIgnoreCase("true"))
        		playerData.put("autounclaim","false"); 
    	}
    	else
        	sender.sendMessage("§a" + Language.getMessage("Auto claim disabled."));
    	
    	savePlayer(playerData);
    	return true;
    }
    /**
     * Toggles autounclaim. While on, players can run around unclaiming land they touch.
     * */
    public boolean toggleAutounclaim(CommandSender sender){
    	loadPlayer(((Player) sender).getUniqueId());
    	String currentclaim = "true"; 
    	if(playerData.has("autounclaim")){
    		String claim = playerData.getString("autounclaim");
    		if(claim.equalsIgnoreCase("true"))
    			playerData.put("autounclaim", "false");
    		else{
    			playerData.put("autounclaim", "true");
    			currentclaim = "false"; 
    		}
    	}
    	else{
    		playerData.put("autounclaim", "true");
    	}
    	
    	
    	if(currentclaim.equalsIgnoreCase("false")){
        	sender.sendMessage("§a" + Language.getMessage("Auto unclaim enabled."));
        	if(playerData.getString("autoclaim").equalsIgnoreCase("true"))
        		playerData.put("autoclaim","false"); 
        }
    	else
        	sender.sendMessage("§a" + Language.getMessage("Auto unclaim disabled."));

    	savePlayer(playerData);
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
    			sender.sendMessage("§c" + Language.getMessage("Please provide a page number. Example:") + " /sf help 2");
        		scan.close();
    			return true;
    		}
    		scan.close();
    	}
    	String helpMessage =  "  §6 /sf - §a" + Language.getMessage("Base command") + "." + "\n";
    		   helpMessage += " §6 help (" + Language.getMessage("page") + ")- §a" + Language.getMessage("List of commands") + "\n";
    		   helpMessage += " §6 create (" + Language.getMessage("name") + ") - §a" + Language.getMessage("Create a faction with specified name") + "\n";
    		   helpMessage += " §6 join (" + Language.getMessage("name") + ") - §a" + Language.getMessage("Join a faction with specified name") + "\n";
    		   helpMessage += " §6 invite (" + Language.getMessage("name") + ") - §a" + Language.getMessage("Invite a player to your faction.") + "\n";
    		   helpMessage += " §6 disband (§f(" + Language.getMessage("optional") + ")§6" + Language.getMessage("name") + ") - §a" + Language.getMessage("Disband a faction.") + "\n";
    		   helpMessage += " §6 leave - §aLeave your faction." + "\n";
    		   helpMessage += " §6 kick (" + Language.getMessage("name") + ") - §a" + Language.getMessage("Kicks member from faction.") + "\n";
    		   
    		   //2
    		   helpMessage += " §6 claim - §a" + Language.getMessage("Claims a chunk of land for your faction.") + "\n";
    		   helpMessage += " §6 sethome - §a" + Language.getMessage("Set a warp home and respawn point for faction.") + "\n";
    		   helpMessage += " §6 home - §a" + Language.getMessage("Teleport home.") + "\n";
    		   helpMessage += " §6 map - §a" + Language.getMessage("Draws a map of surrounding faction land.") + "\n";
    		   helpMessage += " §6 info (name) - §a" + Language.getMessage("Shows info on a faction.") + "\n";
    		   helpMessage += " §6 player (name) - §a" + Language.getMessage("Show info on a player.") + "\n";
    		   helpMessage += " §6 list (page) - §a" + Language.getMessage("Creates a list of Factions.") + "\n";
    		   helpMessage += " §6 auto(un)claim - §a" + Language.getMessage("Toggles auto(un)claiming of land.") + "\n";
    		   
    		   //3
    		   helpMessage += " §6 chat (channel) - §a" + Language.getMessage("Switches to specified channel.") + "\n";
    		   helpMessage += " §a(" + Language.getMessage("also works with abbreviations such as") + " §b/sf c g§a" + "\n";
    		   helpMessage += " §a" + Language.getMessage("or") + " §b/sf c f§a. " +  Language.getMessage("Custom channels supported.") + ")" + "\n";
    		   helpMessage += " \n";
    		   helpMessage += " §6" + Language.getMessage("Claims explanation") + ":§a " + Language.getMessage("In order to claim someone") + "\n";
    		   helpMessage += " §a" + Language.getMessage("else's land, their land claimed must be higher") + " " + "\n";
    		   helpMessage += " §a" + Language.getMessage("than their current power! Kill them to lower their") + "\n";
    		   helpMessage += " §a" + Language.getMessage("power in order to claim their land.") + "\n";
    	
    		   //4
    		   helpMessage += " §6 access " + Language.getMessage("type") + "(p/r/f) " + Language.getMessage("name") + "(" + Language.getMessage("player/factionRank/faction") + ") " + "\n"; 
    		   helpMessage += " §6 " + Language.getMessage("block") + "(" + Language.getMessage("block") + ") " + "allow(true/false)" + " " + Language.getMessage("thisChunkOnly(true/false)") + "\n"; 
    		   helpMessage += " §a - " + Language.getMessage("This very powerful command will allow you to edit") + "  " + "\n"; 
    		   helpMessage += " §a " + Language.getMessage("permissions to your liking, within your faction!") + "  " + "\n"; 
    		   helpMessage += " \n"; 
    		   helpMessage += " §6 promote (" + Language.getMessage("player") + ") - §a" + Language.getMessage("Promotes player to officer.") + "\n"; 
    		   helpMessage += " §6 demote (" + Language.getMessage("player") + ") - §a" + Language.getMessage("Demotes player to member.") + "\n"; 
    		   helpMessage += " §6 leader (" + Language.getMessage("player") + ") - §a" + Language.getMessage("Adds leader to faction.") + "\n";

    		   //5
    		   helpMessage += " §6 " + Language.getMessage("top") + " §7<§b" + Language.getMessage("time") + "§7/§b" + Language.getMessage("kills") + "§7/§b" + Language.getMessage("deaths") + "§7 - §a" + Language.getMessage("Shows server's top stats.") + "\n";
    		   helpMessage += " §6 setrank (" + Language.getMessage("name") + ") (" + Language.getMessage("factionRank") + ") - §a" + Language.getMessage("You can specify a") + "\n"; 
    		   helpMessage += "  §a" + Language.getMessage("specific factionRank to give a player. You can even") + "\n"; 
    		   helpMessage += "  §a" + Language.getMessage("use custom factionRank names (with /sf access) to") + " " + "\n"; 
    		   helpMessage += "  §a" + Language.getMessage("create entirely new faction ranks!") + "\n"; 
    		   helpMessage += " \n"; 
    		   helpMessage += " \n"; 
    		   helpMessage += " \n"; 
    		   
    		   //6
    		   helpMessage += " §6 set " + Language.getMessage("(peaceful/safezone/warzone) (true/false)") + " - §a" + Language.getMessage("Sets flag for faction.") + "\n";
    		   helpMessage += " §a" + Language.getMessage("If peaceful, land cannot be damaged and players cannot be hurt.") + "\n";
    		   helpMessage += " §a" + Language.getMessage("If safezone, land cannot be damaged and anyone inside of the land cannot be hurt.") + "\n";
    		   helpMessage += " §a" + Language.getMessage("If warzone, land cannot be damaged and friendly fire inside of land is enabled.") + "\n";
    		   helpMessage += " \n"; 
    		   helpMessage += " §6 (§dCoty loves you :3c§6)" + "\n"; 
    		   helpMessage += "§a" + Language.getMessage("Plugin version") +   ": " + version +" \n"; 
    		   helpMessage += "§aUsing language: §6" + Language.getLanguage() + "§f \n"; 
    		   
    	int lineCount = 0;
    	int pageCount = 0;
    	String pageToDisplay = "";
    	
    	for(int i = 1; i < helpMessage.length(); i++){
    		if(pageCount == page) pageToDisplay+= helpMessage.charAt(i);
    		if(helpMessage.substring(i-1, i).equalsIgnoreCase("\n")){
    			lineCount++;
    			if(lineCount>7){
    				lineCount = 0;
    				pageCount++;
    				pageToDisplay+=" ";
    			}
    		}
    	}
    	
    	if(page>pageCount) {
    		sender.sendMessage("§c" + Language.getMessage("There are only") + " " + (pageCount+1) + " help pages!");
    	}
    	sender.sendMessage("§6simpleFactions help - page " + (page+1) + "/ " + (pageCount+1) + " \n" + pageToDisplay);
    	return true;
    }
    
    
    
   
    
    /**
     * Invites a player to a faction
     * */
    public boolean invitePlayer(CommandSender sender, String invitedPlayer){
    	
    	if(sender.getName().equalsIgnoreCase(invitedPlayer)){
    		sender.sendMessage("§cepic.");
    		return true;
    	}
    	
    	loadPlayer(((Player) sender).getUniqueId());
    	String faction = playerData.getString("faction");
    	loadFaction(faction);
    	
    	String factionRank = playerData.getString("factionRank");
    	if(factionRank.equalsIgnoreCase("officer") || factionRank.equalsIgnoreCase("leader")){
        	inviteData = factionData.getJSONArray("invited");
        	inviteData.put(invitedPlayer.toLowerCase());
        	factionData.put("invited", inviteData);
        	saveFaction(factionData);
        	
        	sender.sendMessage("§6You have invited §f" + invitedPlayer + "§6 to your faction!");
        	
        	boolean invitedSomebody = false; 
        	for(Player on : Bukkit.getOnlinePlayers()){
        		if(on.getName().equalsIgnoreCase(invitedPlayer)){
        			invitedSomebody = true; 
        			loadPlayer(on.getUniqueId()); 
        			String factionString = getFactionRelationColor(playerData.getString("faction"),faction) + 
        					Config.configData.getString("faction symbol left") + faction + 
        					Config.configData.getString("faction symbol right");
                	on.sendMessage("§6You have been invited to " + factionString + 
        			"§6. Type §b/sf join " + faction + "§6 in order to accept");
        		}
        	}
        	
        	if(!invitedSomebody){
        		sender.sendMessage("§cThe player you invited either does not exist or is offline."); 
        	}
        	
        	//old
        	//loadPlayer(Bukkit.getPlayer(invitedPlayer).getUniqueId()); 
        	//String factionString = getFactionRelationColor(playerData.getString("faction"),faction) + Config.configData.getString("faction symbol left") + faction + Config.configData.getString("faction symbol right");
        	//Bukkit.getPlayer(invitedPlayer).sendMessage("§6You have been invited to " + factionString + 
        	//		"§6. Type §b/sf join " + faction + "§6 in order to accept");
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
    	
    	if(Faction.factionCheck(name)){
    		loadFaction(name);
    		sender.sendMessage(Faction.factionInformationString(sender,name));
    		return true;
    	}
    	
    	if(playerCheck(name)){
    		loadPlayer(Bukkit.getPlayer(name).getUniqueId());
    		if(!playerData.getString("faction").equalsIgnoreCase(""))
    			sender.sendMessage(Faction.factionInformationString(sender,playerData.getString("faction")));
    		else
    			sender.sendMessage(name + " is not in a faction!");
    		return true;
    	}
    	
    	sender.sendMessage("Faction or player not found!");
    	return true;
    }
    

    
    
    
    /**
     * Try to join the supplied faction.
     * */
    public boolean tryJoin(CommandSender sender, String faction){
    	if(factionIndex.contains(faction)){
    		loadPlayer(((Player) sender).getUniqueId());
    		if(playerData.getString("faction").equalsIgnoreCase("")){
    			loadFaction(faction);
    			inviteData = factionData.getJSONArray("invited");
    			
    			if(inviteData.toString().contains(sender.getName().toLowerCase()) || factionData.getString("open").equalsIgnoreCase("true") || 
    					sender.isOp() || sender.hasPermission("simplefactions.admin")){
        			playerData.put("faction", faction);
        			playerData.put("factionRank", Config.configData.getString("default player factionRank"));
        			savePlayer(playerData); 
        			sender.sendMessage("§6You have joined " + Config.Rel_Faction + playerData.getString("faction") + "§6!");
        			Faction.messageFaction(faction,Config.Rel_Faction + sender.getName() + "§6 has joined your faction!");
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
    		sender.sendMessage("§cThe faction §f" + Config.configData.getString("faction symbol left") + faction + Config.configData.getString("faction symbol right") + " §cdoes not exist!");
    	}
    	
    	return true;
    }
    
    
    /**
     * Display a text map of the surround faction claims.
     * */
    public boolean drawMap(CommandSender sender){
    	String mapkey = "§7Unclaimed = #";
    	String map = "";

    	Player player = ((Player) sender);
    	loadWorld(player.getWorld().getName());
    	loadPlayer(((Player) sender).getUniqueId());
    	String factionName = playerData.getString("faction");
    	
    	int posX = player.getLocation().getBlockX(), chunkSizeX = Config.chunkSizeX;
    	int posY = player.getLocation().getBlockY(), chunkSizeY = Config.chunkSizeY;
    	int posZ = player.getLocation().getBlockZ(), chunkSizeZ = Config.chunkSizeZ;
    	
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
    					if(!boardData.getString("chunkX" + j + " chunkY" + posY + " chunkZ" + i).equalsIgnoreCase("")){
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
    							if(factionsFoundArray[k].equalsIgnoreCase(mapFaction)){
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
    public static boolean tryClaim(CommandSender sender){
    	Player player = ((Player) sender);
    	loadWorld(player.getWorld().getName());
    	loadPlayer(((Player) sender).getUniqueId());
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equalsIgnoreCase("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}
    	
    	if(!playerData.getString("factionRank").equalsIgnoreCase("leader") && !playerData.getString("factionRank").equalsIgnoreCase("officer")){
    		sender.sendMessage("§cYou aren't a high enough factionRank to do this.");
    		return true;
    	}

    	if(getFactionClaimedLand(factionName)>=getFactionPower(factionName)){
    		sender.sendMessage("§cYou need more power! §7Staying online and having more members increases power. Do §6/sf help§7 for more information.");
    		return true;
    	}

    	loadWorld(player.getWorld().getName()); //have to load world again (getFactionClaimedLand() unloads it)

    	Config.loadConfig(); 
    	for(int i = 0; i<Config.claimsDisabledInTheseWorlds.length(); i++){
    		String worldDisabled = Config.claimsDisabledInTheseWorlds.optString(i);
    		if(worldDisabled.equalsIgnoreCase(player.getWorld().getName())){
    			sender.sendMessage("§cClaims are disabled in §f" + player.getWorld().getName() + "§c.");
        		return true;
    		}
    	}
    	
    	int posX = player.getLocation().getBlockX(), chunkSizeX = Config.chunkSizeX;
    	int posY = player.getLocation().getBlockY(), chunkSizeY = Config.chunkSizeY;
    	int posZ = player.getLocation().getBlockZ(), chunkSizeZ = Config.chunkSizeZ;
    	
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;

    	if(boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ)){
    		if(boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ).equalsIgnoreCase(factionName)){
    			sender.sendMessage("§cYou already own this land!");
    			return true;
    		}
    		
    		String faction2 = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
    		
    		loadFaction(faction2); 
    		if(factionData.getString("safezone").equalsIgnoreCase("true")){
    			sender.sendMessage("You cannot claim over a safezone!");
    			return true; 
    		}
    		if(factionData.getString("warzone").equalsIgnoreCase("true")){
    			sender.sendMessage("You cannot claim over a warzone!");
    			return true; 
    		}
    		
    		if(!faction2.equalsIgnoreCase("")){
    			if(getFactionPower(faction2)>=getFactionClaimedLand(faction2)){
    				sender.sendMessage(getFactionRelationColor(factionName,faction2) + Config.configData.getString("faction symbol left") + faction2 + Config.configData.getString("faction symbol right") + "§cowns this chunk.§7 If you want it, you need to lower their power before claiming it.");
    				return true;
    			}
    		}
    	}
    	
    	boardData.put("name", player.getWorld().getName());
    	boardData.put("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ, factionName);
    	
    	saveWorld(boardData);
    	Faction.messageFaction(factionName,Config.Rel_Faction + sender.getName() + "§6 claimed x:" + posX + " y:" + posY + 
    			" z:" + posZ + " for " + Config.Rel_Faction + Config.configData.getString("faction symbol left") + factionName + Config.configData.getString("faction symbol right") + "§6!");
    	//sender.sendMessage();
    	return true;
    }
    
    /**
     * It tries to unclaim the chunk.
     * */
    public static boolean tryUnClaim(CommandSender sender){
    	Player player = ((Player) sender);
    	loadWorld(player.getWorld().getName());
    	loadPlayer(((Player) sender).getUniqueId());
    	String factionName = playerData.getString("faction");
    	Config.loadConfig(); 
    	
    	if(factionName.equalsIgnoreCase("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}

    	int posX = player.getLocation().getBlockX(), chunkSizeX = Config.chunkSizeX;
    	int posY = player.getLocation().getBlockY(), chunkSizeY = Config.chunkSizeY;
    	int posZ = player.getLocation().getBlockZ(), chunkSizeZ = Config.chunkSizeZ;
    	
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;

    	//sender.sendMessage("x:" + posX + " y:" + posY + 
    	//		" z:" + posZ + " for the faction " + factionName + "!");
    	if(boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ)){
    		if(boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ).equalsIgnoreCase("")){
    			sender.sendMessage("§cThis area is not already claimed!");
    			return true;
    		}
    		else{
        		if(boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ).equalsIgnoreCase(factionName)){
        			boardData.remove("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
        			//sender.sendMessage("§6Chunk has been unclaimed.");
        			Faction.messageFaction(factionName,Config.Rel_Faction + sender.getName() + "§6 unclaimed " + "chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
        	    	saveWorld(boardData);
        			return true;
        		}
        		
    			String faction2 = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
    			if(getFactionPower(faction2)<getFactionClaimedLand(faction2)){
    				
    				Faction.messageFaction(factionName,Config.Rel_Faction + sender.getName() + "§6 unclaimed " + getFactionRelationColor(factionName,faction2) + 
    						Config.configData.getString("faction symbol left") + faction2 + Config.configData.getString("faction symbol right") + "§6's land!");
    				Faction.messageFaction(faction2,getFactionRelationColor(faction2,factionName) + Config.configData.getString("faction symbol left") +  factionName + 
    						Config.configData.getString("faction symbol right") + " " + sender.getName() + "§6 unclaimed your land!");
    				//sender.sendMessage();
        			
    				
    				boardData.remove("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
        	    	saveWorld(boardData);
    				return true;
    			}
    			else{
    				sender.sendMessage("§cYou could not unclaim " + getFactionRelationColor(factionName,faction2) + Config.configData.getString("faction symbol left") + faction2 + Config.configData.getString("faction symbol right") + "§c's land!§7 They have too much power!");
    				return true;
    			}
    		}
    	}

		if(playerData.has("autounclaim") && playerData.getString("autounclaim").equalsIgnoreCase("false"))
			sender.sendMessage("§cCould not unclaim chunk!");
    	return true;
    }
    
    /**
     * It tries to unclaim all chunks.
     * */
    public boolean tryUnClaimAll(CommandSender sender){
    	Player player = ((Player) sender);
    	loadWorld(player.getWorld().getName());
    	loadPlayer(((Player) sender).getUniqueId());
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equalsIgnoreCase("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}
    	
    	JSONArray array = boardData.names();
    	for(int i = 0; i < array.length(); i++){
    		String name = array.getString(i);
    		if(boardData.getString(name).equalsIgnoreCase(factionName)){
    			boardData.remove(name);
    		}
    	}
		saveWorld(boardData);
    	
    	//sender.sendMessage("§6Unclaimed all of your faction's land!");
    	Faction.messageFaction(factionName,Config.Rel_Faction + sender.getName() + "§6 has unclaimed all of your factions land in §f" + player.getWorld().getName());
    	return true;
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
    	loadPlayer(((Player) sender).getUniqueId());
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equalsIgnoreCase("")){
    		sender.sendMessage("You are not currently in a faction, silly.");
    		return true;
    	}

    	int otherleaders = 0; 
    	for(UUID player : playerIndex){
    		loadPlayer(player);
    		if(playerData.getString("faction").equalsIgnoreCase(factionName)){
    			if(!player.toString().equalsIgnoreCase(sender.getName())){
    				if(playerData.getString("factionRank").equalsIgnoreCase("leader")){
    					otherleaders++;
    				}
    			}
    		}
    	}
    	
    	if(otherleaders==0){
    		sender.sendMessage("You must promote another player before leaving the faction!");
			return true; 
    	}
    	
    	loadPlayer(((Player) sender).getUniqueId());
    	playerData.put("faction", "");
    	playerData.put("factionRank", "member");
    	savePlayer(playerData);
    	
    	//disband if you're the last one there
    	boolean canDisband = true;
    	for(UUID player : playerIndex){
    		loadPlayer(player);
    		if(playerData.getString("faction").equalsIgnoreCase(factionName)){
    			canDisband = false;
    		}
    	}
    	
    	if(canDisband){
    		deleteFaction(factionName); 
    	}
    	

    	
		sender.sendMessage("You have left your faction!");
    	return true;
    }
    
    public static void deleteFaction(String factionName){
    	
    	String uuid = ""; 
    	
    	for(int i = 0; i < Data.Factions.length(); i++){
    		if(Data.Factions.getJSONObject(i).getString("name").equalsIgnoreCase(factionName)){
    			uuid = Data.Factions.getJSONObject(i).getString("ID"); 
    			Data.Factions.remove(i); 
    		}
    	}
    	
    	File file = new File(dataFolder + "/factionData/" + uuid + ".json");
    	if(file.exists()){
    		file.delete();
    	}
		
    	int k = -1;
    	for(int i = 0; i<factionIndex.size(); i++){
    		if(factionIndex.get(i).equalsIgnoreCase(factionName)){
    			k = i;
    		}
    	}
    	
    	if(k>=0){
    		factionIndex.remove(k);
    		Bukkit.getServer().getConsoleSender().sendMessage("removed");
    	}
    	
    	for(int j = 0; j<Data.Worlds.length(); j++){
    		loadWorld(Data.Worlds.getJSONObject(j).getString("name")); 
    		JSONArray array = boardData.names();
        	for(int m = 0; m < array.length(); m++){
        		String name = array.getString(m);
        		if(boardData.getString(name).equalsIgnoreCase(factionName)){
        			boardData.remove(name);
        		}
        	}
    		saveWorld(boardData);
    	}
    }
    
    
    
    
    
    /**
     * Creates player data.
     * This is used for 
    		New player join
    		When the player file is missing (deleted?)
     * */
    public static void createPlayer(Player player){
    	playerData = new JSONObject();
  		playerData.put("name", player.getName());
  		playerData.put("ID", player.getUniqueId().toString());
  		playerData.put("faction","");
  		playerData.put("autoclaim","false");
  		playerData.put("autounclaim","false");
  		playerData.put("factionRank",Config.configData.getString("default player factionRank"));
  		playerData.put("factionTitle",Config.configData.getString("default player title"));
  		playerData.put("shekels", Config.configData.getInt("default player money"));
  		playerData.put("power", Config.configData.getInt("default player power"));
  		playerData.put("deaths",0);
  		playerData.put("kills",0);
  		playerData.put("time online",(long) 0.0);
  		playerData.put("chat channel","global");
  		playerData.put("last online",System.currentTimeMillis());
  		savePlayer(playerData);
    	
  		for(int i = 0; i < Data.Players.length(); i++){
  			if(Data.Players.getJSONObject(i).getString("ID").equals(player.getUniqueId().toString())){
  				Data.Players.remove(i); 
  			}
  		}
  		
  		for(int i = 0; i < playerIndex.size(); i++){
  			if(playerIndex.get(i).equals(player.getUniqueId().toString())){
  				playerIndex.remove(i); 
  			}
  		}
  		
  		Data.Players.put(playerData); 
    	playerIndex.add(player.getUniqueId());
    }
    public static void createPlayer(OfflinePlayer player){
    	playerData = new JSONObject();
  		playerData.put("name", player.getName());
  		playerData.put("ID", player.getUniqueId().toString());
  		playerData.put("faction","");
  		playerData.put("autoclaim","false");
  		playerData.put("autounclaim","false");
  		playerData.put("factionRank",Config.configData.getString("default player factionRank"));
  		playerData.put("factionTitle",Config.configData.getString("default player title"));
  		playerData.put("shekels", Config.configData.getInt("default player money"));
  		playerData.put("power", Config.configData.getInt("default player power"));
  		playerData.put("deaths",0);
  		playerData.put("kills",0);
  		playerData.put("time online",(long) 0.0);
  		playerData.put("chat channel","global");
  		playerData.put("last online",System.currentTimeMillis());
  		savePlayer(playerData);
  		
  		for(int i = 0; i < Data.Players.length(); i++){
  			if(Data.Players.getJSONObject(i).getString("ID").equals(player.getUniqueId().toString())){
  				Data.Players.remove(i); 
  			}
  		}
  		
  		for(int i = 0; i < playerIndex.size(); i++){
  			if(playerIndex.get(i).equals(player.getUniqueId().toString())){
  				playerIndex.remove(i); 
  			}
  		}
  		
  		Data.Players.put(playerData); 
    	playerIndex.add(player.getUniqueId());
    }
    
    /**
     * The first argument is your faction, the second argument is the other faction.
     * Returns a string with the color code of your relation to the second faction.
     * */
    public static String getFactionRelationColor(String senderFaction, String reviewedFaction){

    	String relation = Config.Rel_Other;
    	String relation2 = "";
    	
    	if(!Config.configData.getString("enforce relations").equalsIgnoreCase("")){
    		String rel = Config.configData.getString("enforce relations");
    		if(rel.equalsIgnoreCase("enemies")) return Config.Rel_Enemy;
    		if(rel.equalsIgnoreCase("ally")) return Config.Rel_Ally;
    		if(rel.equalsIgnoreCase("truce")) return Config.Rel_Truce;
    		if(rel.equalsIgnoreCase("neutral")) return Config.Rel_Other;
    		if(rel.equalsIgnoreCase("other")) return Config.Rel_Other;
    	}
    	
    	//war or peacetime?
    	if(getScheduledTime().equalsIgnoreCase("war")){
			relation="enemy";
			relation2="enemy";
			if(senderFaction.equalsIgnoreCase("neutral territory"))
				return Config.Rel_Enemy; 
    	}
    	
    	if(getScheduledTime().equalsIgnoreCase("peace")){
			relation="truce";
			relation2="truce";
			if(senderFaction.equalsIgnoreCase("neutral territory"))
				return Config.Rel_Truce; 
    	}
    	
    	if(senderFaction.equalsIgnoreCase("")){
    		if(relation.equalsIgnoreCase("enemy")) return Config.Rel_Enemy;
    		if(relation.equalsIgnoreCase("truce")) return Config.Rel_Truce;
    	}
    	
    	if(senderFaction.equalsIgnoreCase(reviewedFaction)) return Config.Rel_Faction;
    	
    	if(!senderFaction.equalsIgnoreCase("") && !reviewedFaction.equalsIgnoreCase("") && !reviewedFaction.equalsIgnoreCase("neutral territory")
    			&& !senderFaction.equalsIgnoreCase("neutral territory")) {
    		
    		//faction 1
    		loadFaction(senderFaction);
    		enemyData = factionData.getJSONArray("enemies"); 
    		for(int i = 0; i < enemyData.length(); i++){
    			if(enemyData.getString(i).equalsIgnoreCase(reviewedFaction)) {
    				relation="enemy"; 
    			}
    		}
    		
    		allyData = factionData.getJSONArray("allies"); 
    		for(int i = 0; i < allyData.length(); i++) {
    			if(allyData.getString(i).equalsIgnoreCase(reviewedFaction)) {
    				relation="ally"; 
    			}
    		}
    		
    		truceData = factionData.getJSONArray("truce"); 
    		for(int i = 0; i < truceData.length(); i++) {
    			if(truceData.getString(i).equalsIgnoreCase(reviewedFaction)) {
    				relation="truce"; 
    			}
    		}
    		
    		//faction 2
    		loadFaction(reviewedFaction);
    		
    		enemyData = factionData.getJSONArray("enemies"); 
    		for(int i = 0; i < enemyData.length(); i++) {
    			if(enemyData.getString(i).equalsIgnoreCase(senderFaction)) {
    				relation2="enemy"; 
    			}
    		}
    		
    		allyData = factionData.getJSONArray("allies"); 
    		for(int i = 0; i < allyData.length(); i++) {
    			if(allyData.getString(i).equalsIgnoreCase(senderFaction)) {
    				relation2="ally"; 
    			}
    		}
    		truceData = factionData.getJSONArray("truce"); 
    		for(int i = 0; i < truceData.length(); i++) {
    			if(truceData.getString(i).equalsIgnoreCase(senderFaction)) {
    				relation2="truce"; 
    			}
    		}
    		
    		//loadFaction(senderFaction);
    		//if(factionData.get("enemies").toString().contains(reviewedFaction)) relation="enemy";// return Rel_Enemy;
    		//if(factionData.get("allies").toString().contains(reviewedFaction)) relation="ally";// return Rel_Ally;
    		//if(factionData.get("truce").toString().contains(reviewedFaction)) relation="truce";// return Rel_Truce;
    	
    		//loadFaction(reviewedFaction);
    		//if(factionData.get("enemies").toString().contains(senderFaction)) relation2="enemy";// return Rel_Enemy;
    		//if(factionData.get("allies").toString().contains(senderFaction)) relation2="ally";// return Rel_Ally;
    		//if(factionData.get("truce").toString().contains(senderFaction)) relation2="truce";// return Rel_Truce;
    		
    		//reviewed faction still loaded right now
        	if(factionData.getString("peaceful").equalsIgnoreCase("true")) return Config.Rel_Truce;  
        	if(factionData.getString("safezone").equalsIgnoreCase("true")) return Config.Rel_Truce; 
        	if(factionData.getString("warzone").equalsIgnoreCase("true")) return Config.Rel_Enemy;
        	
    		loadFaction(senderFaction);
        	if(factionData.getString("peaceful").equalsIgnoreCase("true")) return Config.Rel_Truce;  
        	if(factionData.getString("safezone").equalsIgnoreCase("true")) return Config.Rel_Truce; 
        	if(factionData.getString("warzone").equalsIgnoreCase("true")) return Config.Rel_Enemy;
    		
    		if(relation.equalsIgnoreCase("enemy") || relation2.equalsIgnoreCase("enemy")) return Config.Rel_Enemy;
    		if(relation.equalsIgnoreCase("ally") && relation2.equalsIgnoreCase("ally")) return Config.Rel_Ally;
    		if(relation.equalsIgnoreCase("truce") && relation2.equalsIgnoreCase("truce")) return Config.Rel_Truce;
    	
    		loadFaction(senderFaction);

    		if(relation.equalsIgnoreCase("enemy")) return Config.Rel_Enemy;
    		if(relation.equalsIgnoreCase("truce")) return Config.Rel_Truce;
        	return relation;
    	}else{
    		if(relation.equalsIgnoreCase("enemy")) return Config.Rel_Enemy;
    		if(relation.equalsIgnoreCase("truce")) return Config.Rel_Truce;
        	return relation;
    	}
    }
	
	/**
	 * Checks if the player can edit terrain at location, returns true/false if they can or cannot.
	 * */
	public static boolean canEditHere(Player player, Location location, String breakorplace){
		
		if(player.isOp() || player.hasPermission("simplefactions.admin")) return true; //skip everything else
		
		loadPlayer(player.getUniqueId());
		String rankEditing = playerData.getString("factionRank");
		String factionEditing = playerData.getString("faction");
		String factionBeingEdited = Faction.getFactionAt(location);
    	
    	if(!factionBeingEdited.equalsIgnoreCase("")){
    		String rel = getFactionRelationColor(factionEditing,factionBeingEdited);
    		
    		Boolean returnType = true;
    		String relation = "neutral";
    		String forceRel = Config.configData.getString("enforce relations");
    		if(!getScheduledTime().equalsIgnoreCase("") && !getScheduledTime().equalsIgnoreCase("war") && !getScheduledTime().equalsIgnoreCase("peace")) rel = forceRel;
    		if(rel.equalsIgnoreCase(Config.Rel_Ally)) relation = "ally";
    		if(rel.equalsIgnoreCase(Config.Rel_Enemy)) relation = "enemy";
    		if(rel.equalsIgnoreCase(Config.Rel_Truce)) relation = "truce";
    		if(rel.equalsIgnoreCase(Config.Rel_Other)) relation = "other";
    		
			if(breakorplace.equalsIgnoreCase("break") && Config.configData.getString("protect all claimed blocks from being broken in " + relation + " territory").equalsIgnoreCase("true"))
				returnType=!returnType;

			if(breakorplace.equalsIgnoreCase("place") && Config.configData.getString("protect all claimed blocks from being placed in " + relation + " territory").equalsIgnoreCase("true"))
				returnType=!returnType;

			if(breakorplace.equalsIgnoreCase("break") && Config.configData.getJSONArray("block break protection in " + relation + " land").toString().contains("" + location.getBlock().getType().getId()))
				returnType=!returnType;
			if(breakorplace.equalsIgnoreCase("break") && Config.configData.getJSONArray("block break protection in " + relation + " land").toString().contains(location.getBlock().getType().toString()))
				returnType=!returnType;
			
			if(breakorplace.equalsIgnoreCase("place") && Config.configData.getJSONArray("block place protection in " + relation + " land").toString().contains("" + location.getBlock().getType().getId()))
				returnType=!returnType; 
			if(breakorplace.equalsIgnoreCase("place") && Config.configData.getJSONArray("block place protection in " + relation + " land").toString().contains(location.getBlock().getType().toString()))
				returnType=!returnType;

			loadFaction(factionBeingEdited);
			for(int j = 0; j<2; j++){
				JSONObject chunk = new JSONObject();
				
				if(j==0){
			    	int posX = location.getBlockX(), chunkSizeX = Config.chunkSizeX;
			    	int posY = location.getBlockY(), chunkSizeY = Config.chunkSizeY;
			    	int posZ = location.getBlockZ(), chunkSizeZ = Config.chunkSizeZ;
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
		    			if(!factionEditing.equalsIgnoreCase(factionBeingEdited))
		    				continue;
		    		}
					if(factionData.has("f") && k == 2){
		    			if(j!=0 && factionData.has("f")) type = factionData.getJSONObject("f");
		    			if(j==0 && chunk.has("f")) type = chunk.getJSONObject("f");
		    			subtype = factionBeingEdited;
		    			if(factionEditing.equalsIgnoreCase(factionBeingEdited))
		    				continue;
		    		}
					
					if(factionData.has("r") && k == 0){
		    			if(j!=0 && factionData.has("r")) type = factionData.getJSONObject("r");
		    			if(j==0 && chunk.has("r")) type = chunk.getJSONObject("r");
		    			subtype = rankEditing;
		    			if(!factionEditing.equalsIgnoreCase(factionBeingEdited))
		    				continue;
		    		}
		    		
		    		if(type.has(subtype)){
		    			JSONObject playerJson = type.getJSONObject(subtype);
		    			if(playerJson.has("allowed")){
		    				JSONArray allowed = playerJson.getJSONArray("allowed");
		    				for(int i = 0; i < allowed.length(); i++)
		    					if(allowed.getString(i).equalsIgnoreCase(location.getBlock().getType().toString()))
		    						returnType = true;;
		    			}if(playerJson.has("notAllowed")){
		    				JSONArray notAllowed = playerJson.getJSONArray("notAllowed");
		    				for(int i = 0; i < notAllowed.length(); i++)
		    					if(notAllowed.getString(i).equalsIgnoreCase(location.getBlock().getType().toString()))
		    						returnType = false;
		    			}
		    		}
		    	}
			}
			
			if(getScheduledTime().equalsIgnoreCase("war")){
				Material mat = location.getBlock().getType();
				if(mat == Material.WOOL) returnType = true; 
			}
			
    		return returnType;
    	}
		
		return true;
	}
	
	
	/**
	 * Checks if the player can use items at this location.
	 * */
	public static boolean canInteractHere(Player player, Location location, String itemName){
		
		if(player.isOp() || player.hasPermission("simplefactions.admin")) return true; //skip everything else
		
		
		loadPlayer(player.getUniqueId());
		String rankEditing = playerData.getString("factionRank");
		String factionEditing = playerData.getString("faction");
		String factionBeingEdited = Faction.getFactionAt(location);
		
    	String rel = Config.Rel_Neutral;
    	String relation = "neutral";
    	
    	boolean returnType = true;
    	if(!factionBeingEdited.equalsIgnoreCase(""))
    		rel = getFactionRelationColor(factionEditing,factionBeingEdited);

		if(rel.equalsIgnoreCase(Config.Rel_Ally)) relation = "ally";
		if(rel.equalsIgnoreCase(Config.Rel_Enemy)) relation = "enemy";
		if(rel.equalsIgnoreCase(Config.Rel_Truce)) relation = "truce";
		if(rel.equalsIgnoreCase(Config.Rel_Other)) relation = "other";
		
		if(Config.configData.getString("block all item use by default in " + relation + " territory").equalsIgnoreCase("true"))
			returnType=!returnType;

		if(itemName.equalsIgnoreCase("") && Config.configData.getJSONArray("item protection in " + relation + " land").toString().contains("" + location.getBlock().getType().getId()))
			returnType=!returnType;
		if(itemName.equalsIgnoreCase("") && Config.configData.getJSONArray("item protection in " + relation + " land").toString().contains(location.getBlock().getType().toString()))
			returnType=!returnType;
		
		if(!itemName.equalsIgnoreCase("") && Config.configData.getJSONArray("item protection in " + relation + " land").toString().contains("" + player.getItemInHand().getType().getId()))
			returnType=!returnType;
		if(!itemName.equalsIgnoreCase("") && Config.configData.getJSONArray("item protection in " + relation + " land").toString().contains(itemName))
			returnType=!returnType;
		
		loadFaction(factionBeingEdited);
		for(int j = 0; j<2; j++){
			JSONObject chunk = new JSONObject();
			
			if(j==0){
		    	int posX = location.getBlockX(), chunkSizeX = Config.chunkSizeX;
		    	int posY = location.getBlockY(), chunkSizeY = Config.chunkSizeY;
		    	int posZ = location.getBlockZ(), chunkSizeZ = Config.chunkSizeZ;
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
	    			if(!factionEditing.equalsIgnoreCase(factionBeingEdited))
	    				continue;
	    		}
				if(factionData.has("f") && k == 2){
	    			if(j!=0 && factionData.has("f")) type = factionData.getJSONObject("f");
	    			if(j==0 && chunk.has("f")) type = chunk.getJSONObject("f");
	    			subtype = factionBeingEdited;
	    			if(factionEditing.equalsIgnoreCase(factionBeingEdited))
	    				continue;
	    		}
				
				if(factionData.has("r") && k == 0){
	    			if(j!=0 && factionData.has("r")) type = factionData.getJSONObject("r");
	    			if(j==0 && chunk.has("r")) type = chunk.getJSONObject("r");
	    			subtype = rankEditing;
	    			if(!factionEditing.equalsIgnoreCase(factionBeingEdited))
	    				continue;
	    		}
	    		
	    		if(type.has(subtype)){
	    			JSONObject playerJson = type.getJSONObject(subtype);
	    			if(playerJson.has("allowed")){
	    				JSONArray allowed = playerJson.getJSONArray("allowed");
	    				for(int i = 0; i < allowed.length(); i++)
	    					if(allowed.getString(i).equalsIgnoreCase(location.getBlock().getType().toString()))
	    						returnType = true;;
	    			}if(playerJson.has("notAllowed")){
	    				JSONArray notAllowed = playerJson.getJSONArray("notAllowed");
	    				for(int i = 0; i < notAllowed.length(); i++)
	    					if(notAllowed.getString(i).equalsIgnoreCase(location.getBlock().getType().toString()))
	    						returnType = false;
	    			}
	    		}
	    	}
		}
		
		if(getScheduledTime().equalsIgnoreCase("war")){
			if(itemName.contains("steel")){
				returnType = true; 
			}
		}
		
		return returnType;
	}


}


