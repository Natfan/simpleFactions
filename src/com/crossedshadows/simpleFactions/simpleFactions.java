package com.crossedshadows.simpleFactions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
	static List<String> playerIndex = new ArrayList<String>();
	static List<String> boardIndex = new ArrayList<String>();
	
	//for traveling around faction territory (one of the few things that stay in memory for entire life of plugin)
	static List<String> playerIsIn_player = new ArrayList<String>();
	static List<String> playerIsIn_faction = new ArrayList<String>();
	static List<Location> playerIsIn_location = new ArrayList<Location>();
	
	
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
	static String version = "1.83";

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
		Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions has enabled successfully!]");
		
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
		Bukkit.getServer().getConsoleSender().sendMessage("§a[All player data saved to disk!]");
		saveAllFactionsToDisk();
		Bukkit.getServer().getConsoleSender().sendMessage("§a[All faction data saved to disk!]");
    }
    
    /**
     * Loads misc data. Currently unnecessary. 
     * */
    public void loadData(){    

    	createDirectories();
    	
    	Config.loadConfig();
    	
    	FileUtil fileutil = new FileUtil();
    	
    	factionIndex = new ArrayList<String>();
    	playerIndex = new ArrayList<String>();
    	boardIndex = new ArrayList<String>();
    	
    	List<String> factionIndexList = Arrays.asList(fileutil.listFiles(dataFolder + "/factionData"));
    	List<String> playerIndexList = Arrays.asList(fileutil.listFiles(dataFolder + "/playerData"));
    	List<String> boardIndexList = Arrays.asList(fileutil.listFiles(dataFolder + "/boardData"));
    	
    	//display loaded factions and players
    	for(int i=0; i<factionIndexList.size(); i++){
    		String uuid = factionIndexList.get(i).replaceFirst(".json", "");
    		Bukkit.getServer().getConsoleSender().sendMessage("    §c->§7Loading " + uuid);
    		loadFactionDisk(uuid); 
    		factionIndex.add(factionData.getString("name"));
    		}
		Bukkit.getServer().getConsoleSender().sendMessage("§bLoaded all factions.");
    	for(int i=0; i<playerIndexList.size(); i++){
    		String uuid = playerIndexList.get(i).replaceAll(".json", "");
    		Bukkit.getServer().getConsoleSender().sendMessage("    §c->§7Loading " + uuid); 
    		loadPlayerDisk(uuid);
    		playerIndex.add(uuid); 
    		}
		Bukkit.getServer().getConsoleSender().sendMessage("§bLoaded all players.");
    	for(int i=0; i<boardIndexList.size(); i++){
    		String name = boardIndexList.get(i).replaceFirst(".json", "");
    		boardIndex.add(name);
    		}
		Bukkit.getServer().getConsoleSender().sendMessage("§bLoaded all worlds.");
    }
    
    
    public static void loadPlayer(UUID uuid){
    	for(int i = 0; i < Data.Players.length(); i++){
    		if(Data.Players.getJSONObject(i).getString("ID").equals(uuid.toString())){
    			playerData = Data.Players.getJSONObject(i); 
    			//Bukkit.getLogger().info("[LoadPlayer]: Player Found! /n" + playerData.toString(4)); //debug
    		} 
    	}
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
			}
    	}
    	
    	try {

			Scanner scan = new Scanner(new FileReader(dataFolder + "/playerData/" + uuid + ".json"));
			scan.useDelimiter("\\Z");
			
			if(scan.hasNext()){
				JSONObject player = new JSONObject(scan.next());
				
				for(int i = 0; i < Data.Players.length(); i++){
					if(Data.Players.getJSONObject(i).getString("ID").equals(uuid)){
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
		}
    	
    }
    
    
    public static void loadFaction(String name){
    	for(int i = 0; i < Data.Factions.length(); i++){
    		if(Data.Factions.getJSONObject(i).getString("name").equals(name)){
    			factionData = Data.Factions.getJSONObject(i); 
    			//Bukkit.getLogger().info("[LoadPlayer]: Player Found! /n" + playerData.toString(4)); //debug
    		} 
    	}
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
				createFaction(uuid);
				bw.write(factionData.toString(8));
				bw.newLine();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	try {
    		
			Scanner scan = new Scanner(new FileReader(dataFolder + "/factionData/" + uuid + ".json"));
			scan.useDelimiter("\\Z");
			factionData = new JSONObject(scan.next());
			
			for(int i = 0; i < Data.Factions.length(); i++){
				if(Data.Factions.getJSONObject(i).getString("ID").equals(uuid)){
					Data.Factions.remove(i); 
				}
			}
			
			Data.Factions.put(factionData);
			scan.close();
    		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	
    }
    
    /**
     * Loads world data into the worldData JSONObject
     * */
    public static void loadWorld(String name){

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
			}
    	}
    	
    	try {

			Scanner scan = new Scanner(new FileReader(dataFolder + "/boardData/" + name + ".json"));
			scan.useDelimiter("\\Z");
			boardData = new JSONObject(scan.next());
			scan.close();
    		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
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
			if(Data.Players.getJSONObject(i).getString("ID").equals(id)){
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
			System.out.println("error"+e.getMessage());
		}
    }
    
    
    public static void saveFaction(JSONObject faction){
    	String id = faction.getString("ID"); 
    	
    	for(int i = 0; i < Data.Factions.length(); i++){
			if(Data.Factions.getJSONObject(i).getString("ID").equals(id)){
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
			System.out.println("error"+e.getMessage());
		}
    }
    
    /**
     * saves boardData into a JSON file
     * */
    public static void saveWorld(JSONObject wData){

    	createDirectories();
    	
    	String saveString = wData.toString(8); //save data for faction
		try{
			FileWriter fw=new FileWriter(dataFolder + "/boardData/" + wData.getString("name").toString() + ".json");
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
    				if(args[0].toLowerCase().equals("create") && (Config.configData.getString("only admins can create factions").equals("false") 
    						|| sender.isOp() ||  sender.hasPermission("simplefactions.admin") )){
    					return tryCreateFaction(sender,args);
    				}
    				if(args[0].toLowerCase().equals("claim")){
    					return tryClaim(sender);
    				}
    				if(args[0].toLowerCase().equals("open")){
    					return tryOpen(sender);
    				}
    				if(args[0].toLowerCase().equals("schedule")){
    					return trySchedule(sender,args);
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
    				if(args[0].toLowerCase().equals("autounclaim")){
    					return toggleAutounclaim(sender);
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
    				if(args[0].toLowerCase().equals("set")){
    					if(args.length>3)
    						return setFactionFlag(sender, args[1],args[2],args[3]);
    					else
    						sender.sendMessage("§cYou must provide the name of the faction and flag! Example, /sf set factionname peaceful true");
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
    					if(args.length>1){
    						loadPlayer(((Player) sender).getUniqueId());
    						if(playerData.getString("faction").equals(args[1]))
    							return tryDisband(sender,args[1]);
    						else{
    							if(sender.isOp() || sender.hasPermission("simplefactions.admin")){
        							return tryDisband(sender,args[1]);
    								}
    							else{
        							sender.sendMessage("You do not have the permissions to do this!");
        							return true;
    								}
    							}
    						}
    					else{
    						loadPlayer(((Player) sender).getUniqueId());
    						String factionName = playerData.getString("faction");
    						if(playerData.getString("faction").equals(args[1]))
        						return tryDisband(sender,factionName);
    						else{
    							sender.sendMessage("You aren't a member of this faction!");
    							return true;
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
    
    public boolean trySchedule(CommandSender sender, String[] args){
    	//format
    	//		/sf schedule (wartime / peacetime) (every number) (minutes/hours/days/weeks) (lasting for number) (minutes/hours/days/weeks)
    	//	
    	if(args.length<5){
    		sender.sendMessage("Incorrect format! Example: /sf schedule (wartime / peacetime) " +
    				"(every number) (minutes/hours/days/weeks) " +
    				"(lasting for number) (minutes/hours/days/weeks)"); 
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
			if(lengthType.equals("seconds"))
				lengthValue = 20L; 
			if(lengthType.equals("minutes"))
				lengthValue = 1200L; 
			if(lengthType.equals("hours"))
				lengthValue = 72000L; 
			if(lengthType.equals("days"))
				lengthValue = 1728000L; 
			if(lengthType.equals("weeks"))
				lengthValue = 12096000L; 
			
			long lengthValue2 = 0L;
			if(lengthType2.equals("seconds"))
				lengthValue2 = 20L; 
			if(lengthType2.equals("minutes"))
				lengthValue2 = 1200L; 
			if(lengthType2.equals("hours"))
				lengthValue2 = 72000L; 
			if(lengthType2.equals("days"))
				lengthValue2 = 1728000L; 
			if(lengthType2.equals("weeks"))
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
							
							if(type.equals("wartime") || type.equals("war")){
								if(getScheduledTime().equals("war")){
									setScheduledTime(""); 
								}else{
									setScheduledTime("war"); 
								}
							}
							
							if(type.equals("peacetime") || type.equals("peace")){
								if(getScheduledTime().equals("peace")){
									setScheduledTime(""); 
								}else{
									setScheduledTime("peace"); 
								}
							}
							
							String weAre = getScheduledTime(); 
							weAre = weAre.toUpperCase();
							String rel = Config.Rel_Truce; 
							if(weAre.equals("WAR")) rel = Config.Rel_Enemy;
							String _message = rel + "IT IS NOW TIME FOR " + weAre + "! " 
									+ weAre + " WILL LAST FOR " + length + " " +
									lengthType2.toUpperCase() + "!"; 
							
							if(weAre.equals("")){
								rel = Config.Rel_Other; 
								if(type.equals("wartime") || type.equals("war"))
									_message = rel + "Wartime is now over.";
								if(type.equals("peacetime") || type.equals("peace"))
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
    
    public boolean tryOpen(CommandSender sender){
    	
    	loadPlayer(((Player) sender).getUniqueId());
    	
    	if(!playerData.getString("factionRank").equals("officer") && !playerData.getString("factionRank").equals("leader")){
    		sender.sendMessage("§cYou aren't a high enough rank to do this.");
    		return true; 
    	}
    	
    	loadFaction(playerData.getString("faction"));
    	
    	String open = factionData.getString("open");
    	if(open.equals("true")){
    		open = "false";
    		factionData.put("open", "false");
    	}else{
    		open = "true";
    		factionData.put("open", "true");
    	}
    	
    	saveFaction(factionData);
    	messageFaction(playerData.getString("faction"), "§bYour faction is now set to " + open + ".");
    	
    	return true;
    }
    
    public boolean setFactionFlag(CommandSender sender, String faction, String flag, String tr){
    	if(sender.isOp() || sender.hasPermission("simplefactions.admin")){
    		loadFaction(faction);
    		if(flag.equals("peaceful") || flag.equals("warzone") || flag.equals("safezone")){
    			if(tr.equals("true") || tr.equals("false")){
    				String ntr = "false";
    				if(tr.equals("false")) ntr = "true";
					factionData.put("peaceful", ntr);
					factionData.put("warzone",  ntr);
					factionData.put("safezone", ntr);
					factionData.put(flag, tr);
					saveFaction(factionData);
					if(tr.equals("true"))
						sender.sendMessage("§aThe faction " + faction + " is now a " + flag + " faction.");
					if(tr.equals("false"))
						sender.sendMessage("§aThe faction " + faction + " is no longer a " + flag + " faction.");
    			}else{
    				sender.sendMessage("§cPlease specify whether you want " + faction + " to be " + flag + " with true or false at the end.");
    			}
    		}else{
    			sender.sendMessage("§cPlease use either peaceful, warzone, or safezone.");
    		}
    	}else{
    		sender.sendMessage("§cYou must be a server OP or have the simplefactions.admin permission to do this!");
    	}
    	return true;
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
    		
    		if(arg.equals("kills") || arg.equals("deaths")){
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
    		
    		if(arg.equals("time")){
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
     * setAccess() is complicated, read more...
     * Example usage: /sf access <p/r/f> <player/factionRank/faction> <block> <true/false> (this chunk only <true/false>) 
     * You provide p/r/f (player, factionRank, or faction) and then the name of the player, factionRank, or faction.
     * You provide the block or item, and then true/false to show you want it to be allowed or not.
     * You can also add an additional "true" at the end of it, if you want this info to only affect the current chunk.
     * 
     * Using this, you can literally create faction specific ranks and permissions (both globally and for specific chunks).
     * */
    public boolean setAccess(CommandSender sender,String[] args){
    	//argument base   0     1             2               3       4                               5
    	//example /sf access <p/r/f> <player/factionRank/faction> <block> <true/false> (this chunk only <true/false>) 
    	String example = "§7Example usage: §b/sf access §7<§bp§7/§br§7/§bf§7> " +
				"§7<§bplayer§7/§brank§7/§bfaction§7> §7<§bblock§7> §7<§btrue§7/§bfalse§7> " +
				"(optional, this chunk only §7<§btrue§7/§bfalse§7>)";
    	
    	Location location = ((Player) sender).getLocation();
    	int posX = location.getBlockX(), chunkSizeX = Config.chunkSizeX;
    	int posY = location.getBlockY(), chunkSizeY = Config.chunkSizeY;
    	int posZ = location.getBlockZ(), chunkSizeZ = Config.chunkSizeZ;
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
    	String board = location.getWorld().getName() + "chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ;
    	
    	if(args.length>4){
    		loadPlayer(((Player) sender).getUniqueId());
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
        			if(args[1].equals("p")) stype = "§7The player " + Config.Rel_Faction;
        			if(args[1].equals("r")) stype = "§7Members ranked as " + Config.Rel_Faction;
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
     * Sets the factionRank of a player.
     * */
    @SuppressWarnings("deprecation")
	public boolean setRank(CommandSender sender, String[] args){
    	// -1  0         1     2
    	// sf setrank player factionRank
    	// sf promote player
    	loadPlayer(((Player) sender).getUniqueId());
		String factionRank = playerData.getString("factionRank");
		String faction = playerData.getString("faction");
		
		if(factionRank.equals("leader")){
			if(args[1].equals(sender.getName())){
				int otherleaders = 0; 
		    	for(String player : playerIndex){
		    		loadPlayer(UUID.fromString(player)); 
		    		if(playerData.getString("faction").equals(faction)){
		    			if(!player.equals(sender.getName())){
		    				if(playerData.getString("factionRank").equals("leader")){
		    					otherleaders++;
		    				}
		    			}
		    		}
		    	}
		    	
		    	if(otherleaders==0){
		    		sender.sendMessage("You must promote another player before changing your own rank!");
					return true; 
		    	}
			}
		}
		
		if(factionRank.equals("officer") || factionRank.equals("leader")){
    	if(args.length>1){
    		
    		if(sender.getName().equals(args[1])){
    			sender.sendMessage("§cYou cannot set your own factionRank!");
    			return true;
    		}
    		
    		if(playerCheck(args[1])){
    			loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
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
    			if(factionRank.equals("leader")){
        			loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
    				playerData.put("factionRank", "leader");
        			savePlayer(playerData);
    				messageFaction(faction,Config.Rel_Faction + args[1] + "§a has been promoted to leader.");
    				return true;
    			}
    			else{
    				sender.sendMessage("§cOnly leaders can select new leaders!");
            		return true;
    			}
    		}
    		if(factionRank.equals("leader")){
	    		if(args[0].equals("promote")){
	    			loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
	    			playerData.put("factionRank", "officer");
	        		savePlayer(playerData);
	    			messageFaction(faction,Config.Rel_Faction + args[1] + "§a has been promoted to officer.");
	        		return true;
	    		}
	    		if(args[0].equals("demote")){
	    			loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
	    			playerData.put("factionRank", "member");
	        		savePlayer(playerData);
	    			messageFaction(faction,Config.Rel_Faction + args[1] + "§a has been demoted to member.");
	        		return true;
	    		}
    		}else{
	    		if(args[0].equals("promote")){
	    			sender.sendMessage("You are not a high enough rank to do this.");
	        		return true;
	    		}
	    		if(args[0].equals("demote")){
	    			sender.sendMessage("You are not a high enough rank to do this.");
	        		return true;
	    		}
    		}
    		
			if(factionRank.equals("leader")){
    			loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
				playerData.put("factionRank", args[2]);
    			savePlayer(playerData);
				messageFaction(faction,Config.Rel_Faction + args[1] + "§a has been set as to a " + args[2] + ".");
    			return true;
			}
			else{
				sender.sendMessage("§cOnly leaders can set custom ranks!");
	    		return true;
			}
    	}
    	else{
    		if(args[0].equals("setrank"))
        		sender.sendMessage("§cInvalid!§7 Correct usage: §b/sf setrank name factionRank");
    		else
    			sender.sendMessage("§cInvalid!§7 Correct usage: §b/sf promote§6/§bdemote§6/§bleader name");
    		
    		return true;
    	}
    	}else{
    		sender.sendMessage("§cYour factionRank isn't high enough to do this!");
    		return true;
    	}
    	
    }
    
    /**
     * Sends out a sendMessage to everyone in a certain faction.
     * */
    public static void messageFaction(String faction, String message){
    	Collection<? extends Player> on = Bukkit.getOnlinePlayers();
    	
    	for(Player player : on){
    		loadPlayer(player.getUniqueId()); 
    		if(playerData.getString("faction").equals(faction)){
    			player.getPlayer().sendMessage(message);
    		}
    	}
    	
    	/*
    	 * This is code from 1.7.9
    	for(int i = 0; i<on.length; i++){
    		loadPlayer(on[i].getPlayer().getName());
    		if(playerData.getString("faction").equals(faction)){
    			on[i].getPlayer().sendMessage(message);
    		}
    	}
    	*/
    }
    
    /**
     * Sends out a sendMessage to the entire server.
     * */
    public void messageEveryone(String message){
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
     * Sets the description of the faction.
     * */
    public boolean setDesc(CommandSender sender, String[] args){
    	loadPlayer(((Player) sender).getUniqueId());
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
    	
    	if(Config.configData.getString("allow simplefactions chat channels").equals("false")){
    		sender.sendMessage("§cSimpleFactions chat channels are §6disabled §con this server. Sorry! " +
    				"Contact an admin if you believe this message is in error."); 
    		return true; 
    	}
    	
    	if(args.length>1){
    		String rel = Config.Rel_Other;
    		
    		if(args[1].equals("f")) args[1] = "faction";
    		if(args[1].equals("a")) args[1] = "ally";
    		if(args[1].equals("t")) args[1] = "truce";
    		if(args[1].equals("e")) args[1] = "enemy";
    		if(args[1].equals("l")) args[1] = "local";
    		if(args[1].equals("g")) args[1] = "global";
    		if(args[1].equals("p") || args[1].equals("all") || args[1].equals("public")) args[1] = "global";
    		
    		if(args[1].equals("faction")) rel = Config.Rel_Faction;
    		if(args[1].equals("ally")) rel = Config.Rel_Ally;
    		if(args[1].equals("truce")) rel = Config.Rel_Truce;
    		if(args[1].equals("enemy")) rel = Config.Rel_Enemy;
    		if(args[1].equals("local")) rel = Config.Rel_Neutral;
    		
    		//if(args[1].equals("faction") || args[1].equals("ally") || args[1].equals("truce") || args[1].equals("enemy") || args[1].equals("local") || args[1].equals("global")){
    			loadPlayer(((Player) sender).getUniqueId());
    			playerData.put("chat channel", args[1]);
    			savePlayer(playerData);
    			sender.sendMessage("You have switched to " + rel + args[1] + " chat.");
    		//}
    	}
    	else{
    		sender.sendMessage("Default chat channels: " + Config.Rel_Faction + "faction, " + Config.Rel_Ally + "ally, " + 
    				Config.Rel_Truce + "truce, " + Config.Rel_Enemy + "enemy, " + Config.Rel_Neutral + "local, " + Config.Rel_Other + "global.");
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
    		if(faction.equals("")){
    			sender.sendMessage("You are not in a faction!");
    			return true;
    		}
    		else{
    			if(playerCheck(args[1])){
    				loadPlayer(Bukkit.getPlayer(args[1]).getUniqueId());
    				if(!playerData.getString("faction").equals(faction)){
    					sender.sendMessage("Player not in your faction!");
    					return true;
    				}
    				else{
    					if(!factionRank.equals("leader") && !factionRank.equals("officer")){
    						sender.sendMessage("You are not a high enough factionRank to kick players!");
    						return true;
    					}
    					else{
    						if(playerData.getString("factionRank").equals("leader")){
    							sender.sendMessage("You must demote leaders before kicking them!");
    							return true; 
    						}
    						playerData.put("faction", "");
    						savePlayer(playerData);
    						messageFaction(faction,Config.Rel_Other + playerData.getString("name") + "§7 kicked from faction by " + Config.Rel_Faction + sender.getName());
    						Bukkit.getPlayer(playerData.getString("name")).sendMessage("You have been kicked from your faction!");
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
    	loadPlayer(((Player) sender).getUniqueId());
    	String faction1 = playerData.getString("faction");
    	loadPlayer(Bukkit.getPlayer(player).getUniqueId());
    	String faction2 = playerData.getString("faction");
    	String factionRank = playerData.getString("factionRank");
    	sender.sendMessage("§7 ------ [" + getFactionRelationColor(faction1,faction2) + player + "§7] ------ ");
    	sender.sendMessage("§6Power: §f" + df.format(playerData.getDouble("power")));
    	if(!playerData.getString("faction").equals("")) 
    		sender.sendMessage(getFactionRelationColor(faction1,faction2) + Config.configData.getString("faction symbol left") + faction2 + 
    				Config.configData.getString("faction symbol right") + "'s §6power: " + getFactionClaimedLand(faction2) + "/" + 
    				df.format(getFactionPower(faction2)) + "/" + df.format(getFactionPowerMax(faction2)));
    	sender.sendMessage("§6Gaining §f" + df.format(Config.configData.getDouble("power per hour while online")) + "§6 power an hour while online.");
    	sender.sendMessage("§6Losing §f" + df.format(-1*Config.configData.getDouble("power per hour while offline")) + "§6 power an hour while offline.");
    	loadPlayer(Bukkit.getPlayer(player).getUniqueId());
    	sender.sendMessage("§6Rank: " + factionRank);
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
    
    
    public boolean playerCheck(String name){
    	for(int i = 0; i < Data.Players.length(); i++){
    		if(Data.Players.getJSONObject(i).getString("name").equals(name)){
    			return true;
    		}
    	}
    	
    	return false; 
    }
    
    public boolean factionCheck(String name){
    	for(int i = 0; i < Data.Factions.length(); i++){
    		if(Data.Factions.getJSONObject(i).getString("name").equals(name)){
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
     * Attempts to set a relation with another faction.
     * */
    public boolean setRelation(CommandSender sender, String[] args, String relation){
    	if(args.length>1){
    		if(factionCheck(args[1])){

				String relString = "";
				if(relation.equals("enemies")) relString = Config.Rel_Enemy;
				if(relation.equals("allies")) relString = Config.Rel_Ally;
				if(relation.equals("truce")) relString = Config.Rel_Truce;
				if(relation.equals("neutral")) relString = Config.Rel_Other;
				
    			if(!relation.equals("neutral")){
    				
    				loadPlayer(((Player) sender).getUniqueId());
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
					
					//below this is messages
					
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
    					
    					//message sender's faction
    					messageFaction(playerData.getString("faction"),"§6You are now " + relString + relation + "§6 with " + 
    							getFactionRelationColor(playerData.getString("faction"),args[1]) + Config.configData.getString("faction symbol left") + args[1] + 
    							Config.configData.getString("faction symbol right") + "§6.");
    					
    					//message other faction
    					messageFaction(args[1],"§6You are now " + relString + relation + "§6 with " + 
    							getFactionRelationColor(playerData.getString("faction"),args[1]) + Config.configData.getString("faction symbol left") + 
    							playerData.getString("faction") + Config.configData.getString("faction symbol right") + "§6.");
    					return true;
    				}
    				if(j==0){
    					//message sender's faction
			    		messageFaction(playerData.getString("faction"),"§6You have asked " + getFactionRelationColor(playerData.getString("faction"),args[1]) + 
			    				Config.configData.getString("faction symbol left") + args[1] + Config.configData.getString("faction symbol right") + 
    							" §6if they would like to become " + relString + relation + "§6.");
			    		
			    		//message ask'd faction
			    		messageFaction(args[1],getFactionRelationColor(playerData.getString("faction"),args[1]) + Config.configData.getString("faction symbol left") + 
			    				playerData.getString("faction") + Config.configData.getString("faction symbol right") + " §6have asked  if you would like to become " + 
			    				relString + relation + "§6.");
			    		
    					return true;
    				}
    				
    			}
    			else{
    				loadPlayer(((Player) sender).getUniqueId());
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
    							Config.configData.getString("faction symbol left") + args[1] + Config.configData.getString("faction symbol right") + "§6!");
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
    							Config.configData.getString("faction symbol left") + args[1] + Config.configData.getString("faction symbol right") + "§6.");
    					return true;
    				}
    				if(k>0 && j>0){
    					sender.sendMessage("§6You have asked " + getFactionRelationColor(playerData.getString("faction"),args[1]) +
    							Config.configData.getString("faction symbol left") + args[1] + Config.configData.getString("faction symbol right") + 
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
    public static int getFactionClaimedLand(String faction){
        int claimedLand = 0;
            
           for(World w : Bukkit.getServer().getWorlds()){
               loadWorld(w.getName());
               JSONArray array = boardData.names();
               for(int i = 0; i < array.length(); i++){
                       String name = array.getString(i);
                       if(boardData.getString(name).equals(faction)){
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
    		if(boardData.getString(name).equals(faction)){
    			claimedLand++;
    		}
    	}
    	return claimedLand;
    }
    */
    
    public double getFactionPowerMax(String faction){
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
				if(playerData.getString("faction").equals(faction)){

					if(off.length + on.size() >= 30){
						if(Config.configData.getString("power cap type").equals("soft")){
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
				if(playerData.getString("faction").equals(faction)){

					if(off.length + on.size() >= 30){
						if(Config.configData.getString("power cap type").equals("soft")){
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
				if(playerData.getString("faction").equals(faction)){

					if(off.length + on.length >= 30){
						if(configData.getString("power cap type").equals("soft")){
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
				if(playerData.getString("faction").equals(faction)){
					if(off.length + on.size() >= 30){
						if(Config.configData.getString("power cap type").equals("soft")){
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
				if(playerData.getString("faction").equals(faction)){
					if(off.length + on.size() >= 30){
						if(Config.configData.getString("power cap type").equals("soft")){
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
				if(playerData.getString("faction").equals(faction)){
					if(off.length + on.length >= 30){
						if(configData.getString("power cap type").equals("soft")){
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
		
		if(factionData.getString("safezone").equals("true"))
			factionPower = 9999999; 
		
		if(factionData.getString("warzone").equals("true"))
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
    		if(claim.equals("true"))
    			playerData.put("autoclaim", "false");
    		else{
    			playerData.put("autoclaim", "true");
    			currentclaim = "false"; 
    		}
    	}
    	else{
    		playerData.put("autoclaim", "true");
    	}

    	if(currentclaim.equals("false")){
        	sender.sendMessage("§aAuto claim enabled.");

        	if(playerData.getString("autounclaim").equals("true"))
        		playerData.put("autounclaim","false"); 
    	}
    	else
        	sender.sendMessage("§aAuto claim disabled.");
    	
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
    		if(claim.equals("true"))
    			playerData.put("autounclaim", "false");
    		else{
    			playerData.put("autounclaim", "true");
    			currentclaim = "false"; 
    		}
    	}
    	else{
    		playerData.put("autounclaim", "true");
    	}
    	
    	
    	if(currentclaim.equals("false")){
        	sender.sendMessage("§aAuto unclaim enabled.");
        	if(playerData.getString("autoclaim").equals("true"))
        		playerData.put("autoclaim","false"); 
        }
    	else
        	sender.sendMessage("§aAuto unclaim disabled.");

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
    		   helpMessage += " §6 auto(un)claim - §aToggles auto(un)claiming of land." + "\n";
    		   
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
    		   helpMessage += " §6 access type(p/r/f) name(player/factionRank/faction) " + "\n"; 
    		   helpMessage += " §6 block(block) allow(true/false) thisChunkOnly(true/false) " + "\n"; 
    		   helpMessage += " §a - This very powerful command will allow you to edit  " + "\n"; 
    		   helpMessage += " §a permissions to your liking, within your faction!  " + "\n"; 
    		   helpMessage += " \n"; 
    		   helpMessage += " §6 promote (player) - §aPromotes player to officer." + "\n"; 
    		   helpMessage += " §6 demote (player) - §aDemotes player to member." + "\n"; 
    		   helpMessage += " §6 leader (player) - §aAdds leader to faction." + "\n";

    		   //5
    		   helpMessage += " §6 top §7<§btime§7/§bkills§7/§bdeaths§7 - §aShows server's top stats." + "\n";
    		   helpMessage += " §6 setrank (name) (factionRank) - §aYou can specify a" + "\n"; 
    		   helpMessage += "  §aspecific factionRank to give a player. You can even" + "\n"; 
    		   helpMessage += "  §ause custom factionRank names (with /sf access) to " + "\n"; 
    		   helpMessage += "  §acreate entirely new faction ranks!" + "\n"; 
    		   helpMessage += " \n"; 
    		   helpMessage += " \n"; 
    		   helpMessage += " \n"; 
    		   
    		   //6
    		   helpMessage += " §6 set (peaceful/safezone/warzone) (true/false) - §aSets flag for faction." + "\n";
    		   helpMessage += " §aIf peaceful, land cannot be damaged and players cannot be hurt." + "\n";
    		   helpMessage += " §aIf safezone, land cannot be damaged and anyone inside of the land cannot be hurt." + "\n";
    		   helpMessage += " §aIf warzone, land cannot be damaged and friendly fire inside of land is enabled." + "\n";
    		   helpMessage += " \n"; 
    		   helpMessage += " \n"; 
    		   helpMessage += " §6 (§dCoty loves you :3c§6)" + "\n"; 
    		   helpMessage += "§aPlugin version: " + version +" \n"; 
    		   
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
    	loadPlayer(((Player) sender).getUniqueId());
    	Player player = ((Player) sender);
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equals("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}
    	
    	if(!playerData.getString("factionRank").equals("officer") && !playerData.getString("factionRank").equals("leader")){
    		sender.sendMessage("§cYou aren't a high enough rank to do this.");
    		return true; 
    	}
    	
    	Config.loadConfig(); 
    	for(int i = 0; i<Config.claimsDisabledInTheseWorlds.length(); i++){
    		String worldDisabled = Config.claimsDisabledInTheseWorlds.optString(i);
    		if(worldDisabled.equals(player.getWorld().getName())){
        		sender.sendMessage("§cHomes are disabled in §f" + player.getWorld().getName() + "§c.");
        		return true;
    		}
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
    	
    	loadPlayer(player.getUniqueId());
    	loadFaction(playerData.getString("faction"));
    	factionData.put("home",world + " " + posX + " " + posY + " " + posZ);
    	saveFaction(factionData);
		//sender.sendMessage("§7You have set your faction home at §6" + (int)posX + "," + (int)posY + "," + (int)posZ + "§7.");
		messageFaction(playerData.getString("faction"), Config.Rel_Faction + sender.getName()  + "§7 has set your faction home at §6" + (int)posX + "," + (int)posY + "," + (int)posZ + "§7.");
    	return true;
    }
    
    /**
     * Returns the Location of the faction home. 
     * */
    public Location getHome(String faction){
    	loadFaction(faction);
    	String home = factionData.getString("home");
    	
    	if(home.equals(""))
    		return null;
    	
    	Scanner scan = new Scanner(home);
    	String world = scan.next();
    	double x = scan.nextDouble();
    	double y = scan.nextDouble();
    	double z = scan.nextDouble();
    	scan.close();
    	
    	return new Location(Bukkit.getWorld(world), x, y, z);
    }
    
    /**
     * Tries to teleport home. If the home is blocked, attempts to find the next best spot.
     * */
    public boolean tryHome(CommandSender sender){
    	loadPlayer(((Player) sender).getUniqueId());
    	Player player = ((Player) sender);
    	String factionName = playerData.getString("faction");
    	if(factionName.equals("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}

    	loadPlayer(((Player) sender).getUniqueId());
    	loadFaction(playerData.getString("faction"));
    	
    	Location loc = getHome(factionName);
    	if(loc == null){
    		sender.sendMessage("Your faction doesn't have a home!");
    		return true;
    	}
    	
    	Block block = loc.getBlock();
    	int i = 0;
    	while(block.getRelative(BlockFace.UP).getType() != Material.AIR || block.getType() != Material.AIR){
    		
    		Random generater = new Random(System.currentTimeMillis() + block.getX() + block.getY() + block.getZ());
    		block = block.getRelative((generater.nextInt(10) *i), (int)(generater.nextInt(10)*i), (int)(generater.nextInt(10)*i));
    		
    		int l = 0;
    		while(block.getRelative(BlockFace.DOWN).getType() == Material.AIR){
    			l++; if(l>9) break;
    			block = block.getRelative(BlockFace.DOWN);
    		}
    		
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
    	
    	loadPlayer(((Player) sender).getUniqueId());
    	String faction = playerData.getString("faction");
    	loadFaction(faction);
    	
    	String factionRank = playerData.getString("factionRank");
    	if(factionRank.equals("officer") || factionRank.equals("leader")){
        	inviteData = factionData.getJSONArray("invited");
        	inviteData.put(invitedPlayer.toLowerCase());
        	factionData.put("invited", inviteData);
        	saveFaction(factionData);
        	
        	sender.sendMessage("§6You have invited §f" + invitedPlayer + "§6 to your faction!");
        	loadPlayer(Bukkit.getPlayer(invitedPlayer).getUniqueId()); 
        	String factionString = getFactionRelationColor(playerData.getString("faction"),faction) + Config.configData.getString("faction symbol left") + faction + Config.configData.getString("faction symbol right");
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
    		loadPlayer(Bukkit.getPlayer(name).getUniqueId());
    		if(!playerData.getString("faction").equals(""))
    			sender.sendMessage(factionInformationString(sender,playerData.getString("faction")));
    		else
    			sender.sendMessage(name + " is not in a faction!");
    		return true;
    	}
    	
    	sender.sendMessage("Faction or player not found!");
    	return true;
    }
    
    public static boolean isFactionOnline(String faction){
		loadFaction(faction);
		Collection<? extends Player> on = Bukkit.getOnlinePlayers();
		for(Player player : on){
			loadPlayer(player.getUniqueId());
			if(playerData.getString("faction").equals(faction) && player.isOnline()) {
				factionData.put("lastOnline", System.currentTimeMillis());
				saveFaction(factionData);
				return true; //if anyone from faction is online, break from loop and return true; update last online time
			}
		}
		
		/*
		 * Code from 1.7.9
		for(int i = 0; i < on.length; i++){
			loadPlayer(on[i].getName());
			if(playerData.getString("faction").equals(faction) && on[i].isOnline()) {
				factionData.put("lastOnline", System.currentTimeMillis());
				saveFaction(factionData);
				return true; //if anyone from faction is online, break from loop and return true; update last online time
			}
		}*/
    	
		if(factionData.getLong("lastOnline")+ (Config.configData.getInt("seconds before faction is considered really offline") * 1000) > System.currentTimeMillis()){
			return true;
		}
		
    	return false;
    }
    
    /**
     * Gather information on a faction and put it into a string.
     * */
    public String factionInformationString(CommandSender sender, String faction){
    	loadPlayer(((Player) sender).getUniqueId());
    	String viewingFaction = playerData.getString("faction");
    	loadFaction(faction);
    	Bukkit.getLogger().info(factionData.toString(4)); //debug
    	DecimalFormat df = new DecimalFormat("0.###");
    	
    	String truce = "";
    	String ally = "";
    	String enemy = "";
    	
    	enemyData = factionData.getJSONArray("enemies");
    	truceData = factionData.getJSONArray("truce");
    	allyData = factionData.getJSONArray("allies");
    	
    	Location factionHome = getHome(faction); 
    	
    	if(!factionData.has("safezone"))
    		factionData.put("safezone", "false"); 

    	if(!factionData.has("warzone"))
    		factionData.put("warzone", "false"); 

    	if(!factionData.has("peaceful"))
    		factionData.put("peaceful", "false"); 
    	
    	
    	for(int i = 0; i<enemyData.length(); i++){
    		String rel = getFactionRelationColor(faction, enemyData.getString(i));
    		if(rel.equals(Config.Rel_Enemy)){
    			enemy += ", " + Config.configData.getString("faction symbol left") + enemyData.getString(i) + Config.configData.getString("faction symbol right");// enemyData.getString(i);
    		}
    	}
    	
    	for(int i = 0; i<factionIndex.size(); i++){
    		if(!enemy.contains(factionIndex.get(i) + ",") && !enemy.contains(", " + factionIndex.get(i)) 
    			&& !enemy.contains(Config.configData.getString("faction symbol left") + factionIndex.get(i) + Config.configData.getString("faction symbol right"))){
    			loadFaction(factionIndex.get(i));
    			enemyData = factionData.getJSONArray("enemies");
    			for(int l = 0; l<enemyData.length(); l++) 
    				if(enemyData.getString(l).equals(faction)) 
    					enemy += ", " + Config.configData.getString("faction symbol left") + factionIndex.get(i) + Config.configData.getString("faction symbol right");
    		}
    	}

    	enemy = enemy.replaceFirst(",","");
    	
    	
    	
    	for(int i = 0; i<truceData.length(); i++){
    		String rel = getFactionRelationColor(faction, truceData.getString(i));
    		if(rel.equals(Config.Rel_Truce)){
    			truce += ", " + Config.configData.getString("faction symbol left") + truceData.getString(i) + Config.configData.getString("faction symbol right"); //truceData.getString(i);
    		}
    	}
    	truce = truce.replaceFirst(",","");

    	for(int i = 0; i<allyData.length(); i++){
    		String rel = getFactionRelationColor(faction, allyData.getString(i));
    		if(rel.equals(Config.Rel_Ally)){
    			ally += ", " + Config.configData.getString("faction symbol left") + allyData.getString(i) + Config.configData.getString("faction symbol right");// + allyData.getString(i);
    		}
    	}
    	ally = ally.replaceFirst(",","");

    	loadFaction(faction);
    	String factionInfo = "";
    	factionInfo += "§6---- " + getFactionRelationColor(viewingFaction,faction) + 
    			Config.configData.getString("faction symbol left") + faction + Config.configData.getString("faction symbol right") + "§6 ---- \n";
    	factionInfo += "§6" + factionData.getString("desc") + "\n§6";
    	
    	if(factionData.getString("safezone").equals("true"))
    		factionInfo += "§6This faction is a safezone.\n"; 
    	if(factionData.getString("warzone").equals("true"))
    		factionInfo += "§cThis faction is a warzone.\n"; 
    	if(factionData.getString("peaceful").equals("true"))
    		factionInfo += "§6This faction is peaceful.\n"; 
    	
    	if(factionHome != null && viewingFaction.equals(faction))
    		factionInfo += "Home in " + factionHome.getWorld().getName() + " at x" + 
    			Math.round(factionHome.getX()) + " z" + Math.round(factionHome.getZ()) + " y" + Math.round(factionHome.getY());
    	
    	factionInfo += "§6Power: " + getFactionClaimedLand(faction) + "/" + df.format(getFactionPower(faction)) + "/" + df.format(getFactionPowerMax(faction)) + "\n";
    	
    	String isOnline = "§coffline";
    	
    	if(isFactionOnline(faction))
    		isOnline = "§bonline";
    	
    	factionInfo += "§6This faction is " + isOnline + "§6.\n";
    	
    	if(isOnline.equals("§coffline")){
    		long time = factionData.getLong("lastOnline") - System.currentTimeMillis() - (Config.configData.getInt("seconds before faction is considered really offline") * 1000);
    		int seconds = (int) (-time/1000);
    		factionInfo += "§6Has been offline for " + (seconds) + " seconds. \n";
    	}else{
    		factionInfo += "§6Faction will become §coffline§6 if no members are §bonline§6 for " + (((factionData.getLong("lastOnline") - System.currentTimeMillis())/1000) + Config.configData.getInt("seconds before faction is considered really offline")) + "§6 more seconds. \n";
    	}
    	
    	if(!ally.equals("")) factionInfo += "§dAlly: " + ally.replace("]", "").replace("[", "").replace("\"", "") + "\n§6";
    	if(!truce.equals("")) factionInfo += "§6Truce: " + truce.replace("]", "").replace("[", "").replace("\"", "") + "\n§6";
    	if(!enemy.equals("")) factionInfo += "§cEnemy: " + enemy.replace("]", "").replace("[", "").replace("\"", "") + "\n§6";
    	
    	String members = "";
    	String offMembers = "";
		OfflinePlayer[] off = Bukkit.getOfflinePlayers();
		Collection<? extends Player> on = Bukkit.getOnlinePlayers();
		
		
		for(Player player : on){
			loadPlayer(player.getUniqueId());
			if(playerData.getString("faction").equals(faction) && player.isOnline()) {
				if(!members.equals("")) 
					members+= ", ";
				members+="(" + playerData.getString("factionRank") + ") " + player.getName();
			}
		}
		
		for(int i = 0; i < off.length; i++){
			loadPlayer(off[i].getUniqueId());
			if(playerData.getString("faction").equals(faction) && !off[i].isOnline()) {
				if(!members.contains(off[i].getName())){
					if(!offMembers.equals("")) 
						offMembers+= ", ";
					offMembers+="(" + playerData.getString("factionRank") + ") " + off[i].getName();
				}
			}
		}
		
		/*
		 * Code from 1.7.9
		for(int i = 0; i < on.length; i++){
			loadPlayer(on[i].getName());
			if(playerData.getString("faction").equals(faction) && on[i].isOnline()) {
				if(!members.equals("")) 
					members+= ", ";
				members+="(" + playerData.getString("factionRank") + ") " + on[i].getName();
			}
		}*/
    	
    	if(!members.equals("")) factionInfo += "§6Online: " + members + "\n";
    	if(!offMembers.equals("")) factionInfo += "§6Offline: " + offMembers + "\n";
    	
    	return factionInfo;
    }
    
    /**
     * Try to join the supplied faction.
     * */
    public boolean tryJoin(CommandSender sender, String faction){
    	if(factionIndex.contains(faction)){
    		loadPlayer(((Player) sender).getUniqueId());
    		if(playerData.getString("faction").equals("")){
    			loadFaction(faction);
    			inviteData = factionData.getJSONArray("invited");
    			
    			if(inviteData.toString().contains(sender.getName().toLowerCase()) || factionData.getString("open").equals("true") || 
    					sender.isOp() || sender.hasPermission("simplefactions.admin")){
        			playerData.put("faction", faction);
        			playerData.put("factionRank", Config.configData.getString("default player factionRank"));
        			savePlayer(playerData); 
        			sender.sendMessage("§6You have joined " + Config.Rel_Faction + playerData.getString("faction") + "§6!");
        			messageFaction(faction,Config.Rel_Faction + sender.getName() + "§6 has joined your faction!");
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
    	
    	loadPlayer(((Player) sender).getUniqueId());
    	String factionName = playerData.getString("faction");
    		factionList += "  " + getFactionRelationColor(factionName,factionName) + "" + Config.configData.getString("faction symbol left") + factionName + Config.configData.getString("faction symbol right")
    			+ " "  + getFactionClaimedLand(factionName) + "/" + df.format(getFactionPower(factionName)) + "/" + df.format(getFactionPowerMax(factionName))   +"" + "§7 <-- you\n";
    	
    	for(int i=0; i<factionIndex.size(); i++){
    		String name = factionIndex.get(i);
        	String Rel = "";
        	if(filter.equals("ally")) Rel = Config.Rel_Ally;
        	if(filter.equals("enemy")) Rel = Config.Rel_Enemy;
        	if(filter.equals("truce")) Rel = Config.Rel_Truce;
        	if(filter.equals("")) Rel = getFactionRelationColor(factionName,name);
        		
        	if(!factionIndex.get(i).equals(factionName) && Rel.equals(getFactionRelationColor(factionName,name)))
        		factionList += "  " + getFactionRelationColor(factionName,name) + "" + Config.configData.getString("faction symbol left") + name + Config.configData.getString("faction symbol right")
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
    	
    	if(filter.equals("ally")) sender.sendMessage("§6Filter: " + Config.Rel_Ally + "Ally");
    	if(filter.equals("truce")) sender.sendMessage("§6Filter: " + Config.Rel_Truce + "Truce");
    	if(filter.equals("enemy")) sender.sendMessage("§6Filter: " + Config.Rel_Enemy + "Enemy");
    	sender.sendMessage("§6Faction List - page " + (page+1) + "/ " + (pageCount+1) + " \n" + pageToDisplay);
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
    public static boolean tryClaim(CommandSender sender){
    	Player player = ((Player) sender);
    	loadWorld(player.getWorld().getName());
    	loadPlayer(((Player) sender).getUniqueId());
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equals("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}
    	
    	if(!playerData.getString("factionRank").equals("leader") && !playerData.getString("factionRank").equals("officer")){
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
    		if(worldDisabled.equals(player.getWorld().getName())){
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
    		if(boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ).equals(factionName)){
    			sender.sendMessage("§cYou already own this land!");
    			return true;
    		}
    		
    		String faction2 = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
    		
    		loadFaction(faction2); 
    		if(factionData.getString("safezone").equals("true")){
    			sender.sendMessage("You cannot claim over a safezone!");
    			return true; 
    		}
    		if(factionData.getString("warzone").equals("true")){
    			sender.sendMessage("You cannot claim over a warzone!");
    			return true; 
    		}
    		
    		if(!faction2.equals("")){
    			if(getFactionPower(faction2)>=getFactionClaimedLand(faction2)){
    				sender.sendMessage(getFactionRelationColor(factionName,faction2) + Config.configData.getString("faction symbol left") + faction2 + Config.configData.getString("faction symbol right") + "§cowns this chunk.§7 If you want it, you need to lower their power before claiming it.");
    				return true;
    			}
    		}
    	}
    	
    	boardData.put("name", player.getWorld().getName());
    	boardData.put("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ, factionName);
    	
    	saveWorld(boardData);
    	messageFaction(factionName,Config.Rel_Faction + sender.getName() + "§6 claimed x:" + posX + " y:" + posY + 
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
    	
    	if(factionName.equals("")){
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
    		if(boardData.get("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ).equals("")){
    			sender.sendMessage("§cThis area is not already claimed!");
    			return true;
    		}
    		else{
        		if(boardData.get("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ).equals(factionName)){
        			boardData.remove("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
        			//sender.sendMessage("§6Chunk has been unclaimed.");
        			messageFaction(factionName,Config.Rel_Faction + sender.getName() + "§6 unclaimed " + "chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
        	    	saveWorld(boardData);
        			return true;
        		}
        		
    			String faction2 = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
    			if(getFactionPower(faction2)<getFactionClaimedLand(faction2)){
    				
    				messageFaction(factionName,Config.Rel_Faction + sender.getName() + "§6 unclaimed " + getFactionRelationColor(factionName,faction2) + 
    						Config.configData.getString("faction symbol left") + faction2 + Config.configData.getString("faction symbol right") + "§6's land!");
    				messageFaction(faction2,getFactionRelationColor(faction2,factionName) + Config.configData.getString("faction symbol left") +  factionName + 
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

		if(playerData.has("autounclaim") && playerData.getString("autounclaim").equals("false"))
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
    	messageFaction(factionName,Config.Rel_Faction + sender.getName() + "§6 has unclaimed all of your factions land in §f" + player.getWorld().getName());
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

			loadPlayer(((Player) sender).getUniqueId()); //load up playerData jsonobject
			
			String factionName = playerData.getString("faction");
			if(!factionName.equals("")){
				sender.sendMessage("§cYou are already in a faction!");
				sender.sendMessage("§ccurrent faction: §b" + Config.configData.getString("faction symbol left") + factionName + Config.configData.getString("faction symbol right"));
				sender.sendMessage("§cPlease do /sf leave in order to create a new faction!");
				return false;
			}

			createFaction(args[1].toString());
			
			loadPlayer(((Player) sender).getUniqueId());
			playerData.put("factionRank","leader");
			playerData.put("faction", args[1].toString());
			savePlayer(playerData);
			
			messageEveryone("§6The faction name " + Config.Rel_Other + args[1].toString() + " §6has been created!");
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
    public static void createFaction(String faction){
    	enemyData = new JSONArray();
    	allyData = new JSONArray();
    	truceData = new JSONArray();
    	inviteData = new JSONArray();
    	factionData = new JSONObject();
		factionData.put("name", faction);
		factionData.put("peaceful", "false");
		factionData.put("warzone", "false");
		factionData.put("safezone", "false");
		factionData.put("ID", UUID.randomUUID().toString());
		factionData.put("shekels", 0.0);
		factionData.put("enemies",enemyData);
		factionData.put("allies",allyData);
		factionData.put("truce", truceData);
		factionData.put("invited", inviteData);
		factionData.put("lastOnline", System.currentTimeMillis());
		factionData.put("home", "");
		factionData.put("desc", Config.configData.getString("default faction description"));
		factionData.put("open", Config.configData.getString("factions open by default"));
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
    	loadPlayer(((Player) sender).getUniqueId());
    	String factionName = playerData.getString("faction");
    	
    	if(factionName.equals("")){
    		sender.sendMessage("You are not currently in a faction, silly.");
    		return true;
    	}

    	int otherleaders = 0; 
    	for(String player : playerIndex){
    		loadPlayer(UUID.fromString(player));
    		if(playerData.getString("faction").equals(factionName)){
    			if(!player.equals(sender.getName())){
    				if(playerData.getString("factionRank").equals("leader")){
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
    	for(String player : playerIndex){
    		loadPlayer(UUID.fromString(player));
    		if(playerData.getString("faction").equals(factionName)){
    			canDisband = false;
    		}
    	}
    	
    	if(canDisband){
    		File file = new File(dataFolder + "/factionData/" + factionName + ".json");
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
        	
        	loadWorld(((Player) sender).getWorld().getName());
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
    	
    	loadPlayer(((Player) sender).getUniqueId());
    	if(playerData.getString("faction").equals("")){
    		sender.sendMessage("You must be in a faction to do this!");
    		return true;
    	}
    	
    	if(!playerData.getString("factionRank").equals("leader") && !sender.isOp()){
    		sender.sendMessage("You cannot do this unless you are the leader of your faction!");
    		return true; 
    	}
    	
    	for(String player : playerIndex){
    		loadPlayer(UUID.fromString(player));
    		if(playerData.getString("faction").equals(factionName)){
    			playerData.put("faction", "");
    			playerData.put("factionRank", "member");
    			savePlayer(playerData);
    		}
    	}
    	
    	File file = new File(dataFolder + "/factionData/" + factionName + ".json");
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
    	
    	
    	loadWorld(((Player) sender).getWorld().getName()); //
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
    	
  		Data.Players.put(playerData); 
    	playerIndex.add(player.getUniqueId().toString());
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

  		Data.Players.put(playerData); 
    	playerIndex.add(player.getUniqueId().toString());
    }
    
    /**
     * The first argument is your faction, the second argument is the other faction.
     * Returns a string with the color code of your relation to the second faction.
     * */
    public static String getFactionRelationColor(String senderFaction, String reviewedFaction){

    	String relation = Config.Rel_Other;
    	String relation2 = "";
    	
    	if(!Config.configData.getString("enforce relations").equals("")){
    		String rel = Config.configData.getString("enforce relations");
    		if(rel.equals("enemies")) return Config.Rel_Enemy;
    		if(rel.equals("ally")) return Config.Rel_Ally;
    		if(rel.equals("truce")) return Config.Rel_Truce;
    		if(rel.equals("neutral")) return Config.Rel_Other;
    		if(rel.equals("other")) return Config.Rel_Other;
    	}
    	
    	if(getScheduledTime().equals("war")){
			relation="enemy";
			relation2="enemy";
			if(senderFaction.equals("neutral territory"))
				return Config.Rel_Enemy; 
    	}
    	
    	if(getScheduledTime().equals("peace")){
			relation="truce";
			relation2="truce";
			if(senderFaction.equals("neutral territory"))
				return Config.Rel_Truce; 
    	}
    	
    	if(senderFaction.equals("")){
    		if(relation.equals("enemy")) return Config.Rel_Enemy;
    		if(relation.equals("truce")) return Config.Rel_Truce;
    	}
    	
    	if(senderFaction.equals(reviewedFaction)) return Config.Rel_Faction;
    	
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

        	if(!factionData.has("safezone"))
        		factionData.put("safezone", "false"); 

        	if(!factionData.has("warzone"))
        		factionData.put("warzone", "false"); 

        	if(!factionData.has("peaceful"))
        		factionData.put("peaceful", "false"); 

        	if(factionData.getString("peaceful").equals("true")) return Config.Rel_Truce;  
        	if(factionData.getString("safezone").equals("true")) return Config.Rel_Truce; 
        	if(factionData.getString("warzone").equals("true")) return Config.Rel_Enemy;
    		
    		if(relation.equals("enemy") || relation2.equals("enemy")) return Config.Rel_Enemy;
    		if(relation.equals("ally") && relation2.equals("ally")) return Config.Rel_Ally;
    		if(relation.equals("truce") && relation2.equals("truce")) return Config.Rel_Truce;
    	
    		loadFaction(senderFaction);

    		if(relation.equals("enemy")) return Config.Rel_Enemy;
    		if(relation.equals("truce")) return Config.Rel_Truce;
        	return relation;
    	}else{
    		if(relation.equals("enemy")) return Config.Rel_Enemy;
    		if(relation.equals("truce")) return Config.Rel_Truce;
        	return relation;
    	}
    }
    
    
	
	
	public static String getFactionAt(Location location){
		String faction = "";
    	loadWorld(location.getWorld().getName());

    	int posX = location.getBlockX(), chunkSizeX = Config.chunkSizeX;
    	int posY = location.getBlockY(), chunkSizeY = Config.chunkSizeY;
    	int posZ = location.getBlockZ(), chunkSizeZ = Config.chunkSizeZ;
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
	public static boolean canEditHere(Player player, Location location, String breakorplace){
		
		if(player.isOp() || player.hasPermission("simplefactions.admin")) return true; //skip everything else
		
		loadPlayer(player.getUniqueId());
		String rankEditing = playerData.getString("factionRank");
		String factionEditing = playerData.getString("faction");
		String factionBeingEdited = getFactionAt(location);
    	
    	if(!factionBeingEdited.equals("")){
    		String rel = getFactionRelationColor(factionEditing,factionBeingEdited);
    		
    		Boolean returnType = true;
    		String relation = "neutral";
    		String forceRel = Config.configData.getString("enforce relations");
    		if(!getScheduledTime().equals("") && !getScheduledTime().equals("war") && !getScheduledTime().equals("peace")) rel = forceRel;
    		if(rel.equals(Config.Rel_Ally)) relation = "ally";
    		if(rel.equals(Config.Rel_Enemy)) relation = "enemy";
    		if(rel.equals(Config.Rel_Truce)) relation = "truce";
    		if(rel.equals(Config.Rel_Other)) relation = "other";
    		
			if(breakorplace.equals("break") && Config.configData.getString("protect all claimed blocks from being broken in " + relation + " territory").equals("true"))
				returnType=!returnType;

			if(breakorplace.equals("place") && Config.configData.getString("protect all claimed blocks from being placed in " + relation + " territory").equals("true"))
				returnType=!returnType;

			if(breakorplace.equals("break") && Config.configData.getJSONArray("block break protection in " + relation + " land").toString().contains("" + location.getBlock().getType().getId()))
				returnType=!returnType;
			if(breakorplace.equals("break") && Config.configData.getJSONArray("block break protection in " + relation + " land").toString().contains(location.getBlock().getType().toString()))
				returnType=!returnType;
			
			if(breakorplace.equals("place") && Config.configData.getJSONArray("block place protection in " + relation + " land").toString().contains("" + location.getBlock().getType().getId()))
				returnType=!returnType; 
			if(breakorplace.equals("place") && Config.configData.getJSONArray("block place protection in " + relation + " land").toString().contains(location.getBlock().getType().toString()))
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
			
			if(getScheduledTime().equals("war")){
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
		String factionBeingEdited = getFactionAt(location);
		
    	String rel = Config.Rel_Neutral;
    	String relation = "neutral";
    	
    	boolean returnType = true;
    	if(!factionBeingEdited.equals(""))
    		rel = getFactionRelationColor(factionEditing,factionBeingEdited);

		if(rel.equals(Config.Rel_Ally)) relation = "ally";
		if(rel.equals(Config.Rel_Enemy)) relation = "enemy";
		if(rel.equals(Config.Rel_Truce)) relation = "truce";
		if(rel.equals(Config.Rel_Other)) relation = "other";
		
		if(Config.configData.getString("block all item use by default in " + relation + " territory").equals("true"))
			returnType=!returnType;

		if(itemName.equals("") && Config.configData.getJSONArray("item protection in " + relation + " land").toString().contains("" + location.getBlock().getType().getId()))
			returnType=!returnType;
		if(itemName.equals("") && Config.configData.getJSONArray("item protection in " + relation + " land").toString().contains(location.getBlock().getType().toString()))
			returnType=!returnType;
		
		if(!itemName.equals("") && Config.configData.getJSONArray("item protection in " + relation + " land").toString().contains("" + player.getItemInHand().getType().getId()))
			returnType=!returnType;
		if(!itemName.equals("") && Config.configData.getJSONArray("item protection in " + relation + " land").toString().contains(itemName))
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
		
		if(getScheduledTime().equals("war")){
			if(itemName.contains("steel")){
				returnType = true; 
			}
		}
		
		return returnType;
	}


}


