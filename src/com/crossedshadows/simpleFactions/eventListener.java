package com.crossedshadows.simpleFactions;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

/**
 * TODO: Move all event handlers into this file. 
 * */
public class eventListener implements Listener {
	
	/**
     * Does stuff when a new player joins the server.
     * */
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
    	File playerFile = new File(simpleFactions.dataFolder + "/playerData/" + event.getPlayer().getUniqueId().toString() + ".json");
    	if(!playerFile.exists()){
        	simpleFactions.createPlayer(event.getPlayer());
    	}
    }
	
	/**
	 * Handles player respawn stuff.
	 * */
	@EventHandler
	public void respawnEvent(PlayerRespawnEvent event){
		//set new spawn (if faction has one)
		Player player = event.getPlayer();
		player.sendMessage("debug, you died");
		String playerString = player.getName();
		simpleFactions.loadPlayer(Bukkit.getPlayer(playerString).getUniqueId()); 
		if(!simpleFactions.playerData.getString("faction").equals("")){
			simpleFactions.loadFaction(simpleFactions.playerData.getString("faction"));
			String home = simpleFactions.factionData.getString("home");
			if(!home.equalsIgnoreCase("")){
				Scanner scan = new Scanner(home);
		    	String world = scan.next();
		    	double x = scan.nextDouble();
		    	double y = scan.nextDouble();
		    	double z = scan.nextDouble();
		    	scan.close();
		    	
		    	Location loc = new Location(Bukkit.getWorld(world), x, y, z);
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
            		simpleFactions.loadPlayer(playerAttacking.getUniqueId());
            		String factionAttacking = simpleFactions.playerData.getString("faction");
            		simpleFactions.loadPlayer(playerAttacked.getUniqueId());
            		String factionAttacked = simpleFactions.playerData.getString("faction");
            	
            		if(Config.configData.getString("friendly fire projectile (arrows)").equalsIgnoreCase("false")){
        				playerAttacking.sendMessage("§7You cannot shoot members of " + simpleFactions.getFactionRelationColor(factionAttacking,factionAttacked) + 
        						Config.configData.getString("faction symbol left") + factionAttacked + Config.configData.getString("faction symbol right") + "§7!");
            			event.setCancelled(true);
        				return;
        			}
            	} 
            	return;
    		}
    		
        	Player playerAttacking = (Player) entityAttacking;
        	Player playerAttacked = (Player) entityAttacked;
        	
        	simpleFactions.loadPlayer(playerAttacking.getUniqueId());
        	String factionAttacking = simpleFactions.playerData.getString("faction");
        	simpleFactions.loadPlayer(playerAttacked.getUniqueId());
        	String factionAttacked = simpleFactions.playerData.getString("faction");
        	
        	String inFactionLand = "neutral";
        	
	        Location loc = playerAttacked.getLocation();
	        simpleFactions.loadWorld(loc.getWorld().getName());
	        int posX = loc.getBlockX();
	        int posY = loc.getBlockY();
	        int posZ = loc.getBlockZ();
	        int chunkSizeX = Config.chunkSizeX; 
	        int chunkSizeY = Config.chunkSizeY; 
	        int chunkSizeZ = Config.chunkSizeZ; 
	        posX = Math.round(posX / chunkSizeX) * chunkSizeX;
	        posY = Math.round(posY / chunkSizeY) * chunkSizeY;
	        posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;

	        if(simpleFactions.boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ)){
	        	inFactionLand = simpleFactions.boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
	        	simpleFactions.loadFaction(inFactionLand);
	        	
	        	if(simpleFactions.factionData.has("peaceful")){
	        		if(simpleFactions.factionData.getString("peaceful").equalsIgnoreCase("true")){
	        			playerAttacking.sendMessage("§7You cannot hurt players in peaceful land.");
	        			event.setCancelled(true);
	        		}
	        	}else{
	        		simpleFactions.factionData.put("peaceful", "false");
	        		simpleFactions.saveFaction(simpleFactions.factionData);
        		}
	        	
	        	if(simpleFactions.factionData.has("safezone")){
	        		if(simpleFactions.factionData.getString("safezone").equalsIgnoreCase("true")){
	        			playerAttacking.sendMessage("§7You cannot hurt players in a safezone.");
	        			event.setCancelled(true);
	        		}
	        	}else{
	        		simpleFactions.factionData.put("safezone", "false");
	        		simpleFactions.saveFaction(simpleFactions.factionData);
        		}
	        }
	        
        	//peaceful characters
	        simpleFactions.loadFaction(factionAttacking);
        	if(simpleFactions.factionData.has("peaceful")){
        		if(simpleFactions.factionData.getString("peaceful").equalsIgnoreCase("true")){
        			playerAttacking.sendMessage("§7Peaceful players cannot attack other players.");
        			return;
        		}
        	}else{
        		simpleFactions.factionData.put("peaceful", "false");
        		simpleFactions.saveFaction(simpleFactions.factionData);
        	}
        	

        	simpleFactions.loadFaction(factionAttacked);
        	if(simpleFactions.factionData.has("peaceful")){
        		if(simpleFactions.factionData.getString("peaceful").equalsIgnoreCase("true")){
        			playerAttacking.sendMessage("§7You cannot hurt peaceful players.");
        			return;
        		}
        	}else{
        		simpleFactions.factionData.put("peaceful", "false");
        		simpleFactions.saveFaction(simpleFactions.factionData);
        	}
        	
        	if(factionAttacking.equalsIgnoreCase(factionAttacked)){
        		simpleFactions.loadFaction(inFactionLand); 
        		
        		if(damagedCause == DamageCause.ENTITY_ATTACK && (Config.configData.getString("friendly fire melee").equalsIgnoreCase("true") || simpleFactions.factionData.getString("warzone").equalsIgnoreCase("true"))){
        			playerAttacking.sendMessage("§7Hit player!");
        			return;
        		}
        		
        		if(Config.configData.getString("friendly fire other").equalsIgnoreCase("false")){
        			playerAttacking.sendMessage("§7You cannot hurt members of " + simpleFactions.getFactionRelationColor(factionAttacking,factionAttacked) + 
        					Config.configData.getString("faction symbol left") + factionAttacked + Config.configData.getString("faction symbol right") + "§7!");
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

		
		String playerName = event.getPlayer().getName();
		
		boolean hasEssentialsUtils = false; 
		
		Plugin[] plugins = Bukkit.getServer().getPluginManager().getPlugins();
		for(Plugin plugin : plugins){
			if(plugin.getName().toLowerCase().contains("essentials")){
				hasEssentialsUtils = true; 
			}
		}
		
		String playerNickname = ""; 
    	if(hasEssentialsUtils) playerNickname = EssentialsUtils.getNickname(playerName);
    	if(playerNickname == null) playerNickname = playerName; 
		if(playerNickname.equalsIgnoreCase("")) playerNickname = playerName; 
    			
		//exit the chat event, if the config says so
		if(Config.configData.getString("enable simplefaction chat").equalsIgnoreCase("false")){
			return;
		}
		
		int posX_talk = event.getPlayer().getLocation().getBlockX();
		int posZ_talk = event.getPlayer().getLocation().getBlockZ();
		
		Set<Player> playerList = event.getRecipients();
		simpleFactions.loadPlayer(event.getPlayer().getUniqueId());
    	String chatChannel_talk = simpleFactions.playerData.getString("chat channel");
    	String factionRank = simpleFactions.playerData.get("factionRank").toString();
    	String title = simpleFactions.playerData.get("factionTitle").toString();
    	String faction = simpleFactions.playerData.get("faction").toString();
    	String factionString = simpleFactions.playerData.get("faction").toString().replaceAll("\\$", "\\\\\\$");
    	factionRank += " ";
    	if(factionRank.contains("leader")) factionRank = "** ";
    	if(factionRank.contains("officer")) factionRank = "* ";
    	if(factionRank.contains("member")) factionRank = "";
    	if(factionRank.equalsIgnoreCase(" ")) factionRank = "";
    	String factionRelation = "";
    	String faction2 = "";
    	if(!Config.configData.getString("allow player titles").equalsIgnoreCase("true"))
    		title = "";
    	
    	if(event.getMessage().charAt(0) == '!'){
			chatChannel_talk = "global"; 
			event.setMessage(event.getMessage().replace("!", ""));
		}
    	
		for(Player player : playerList){
			simpleFactions.loadPlayer(Bukkit.getPlayer(playerName).getUniqueId());
	    	factionString = simpleFactions.playerData.getString("faction").replaceAll("\\$", "\\\\\\$");
	    	simpleFactions.loadPlayer(player.getUniqueId());
	    	faction2 = simpleFactions.playerData.getString("faction");
	    	String chatChannel_listen = simpleFactions.playerData.getString("chat channel");
	    	if(!faction.equalsIgnoreCase("") && !faction2.equalsIgnoreCase(""))
	    		factionRelation = simpleFactions.getFactionRelationColor(faction2,faction);
	    	else
	    		factionRelation = Config.Rel_Other;
	    	if(!faction.equalsIgnoreCase("")) factionString = Config.configData.getString("faction symbol left") + faction.replaceAll("\\$", "\\\\\\$") + Config.configData.getString("faction symbol right");

	    	
	    	if(!faction.equalsIgnoreCase("") && !faction2.equalsIgnoreCase(""))
	    		simpleFactions.loadFaction(faction);

			/*	
			configData.put("show faction data in global chat", "true");
			*/
	    	
	    	//global
	    	if(chatChannel_talk.equalsIgnoreCase("global")){
	    		String message = "";
	    		if(Config.configData.getString("show faction data in global chat").equalsIgnoreCase("true"))
	    			message +=  factionRelation + factionRank + "" + factionString;
	    		message += " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage();
	    		
	    		if(Config.configData.getString("inject faction into message instead of replacing whole message").equalsIgnoreCase("true")){
	    			String format = event.getFormat();
	    			message = format.replaceFirst("%1", factionRelation + factionRank + "" + factionString + " " + playerNickname);
	    			message = message.replaceFirst("%2", event.getMessage().replaceAll("\\$", "\\\\\\$")); //man don't ask me shit
	    			message = message.replaceAll("\\$s", "§f");
	    		}
	    		
	    		player.sendMessage(message);
	    		continue;
	    	}
	    	
	    	//faction
	    	if(chatChannel_talk.equalsIgnoreCase("faction")){
	    		if(faction.equalsIgnoreCase(faction2))
	    			player.sendMessage(Config.Rel_Faction + "(faction) " + factionRelation + title + " " + factionRank + "" + factionString + " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage());
	    		continue;
	    	}
	    	
	    	//ally
	    	if(chatChannel_talk.equalsIgnoreCase("ally")){
	    		simpleFactions.allyData = simpleFactions.factionData.getJSONArray("allies");
	    		for(int i = 0; i<simpleFactions.allyData.length(); i++)
	    			if(simpleFactions.allyData.getString(i).equalsIgnoreCase(faction2))
	    	    		player.sendMessage(Config.Rel_Ally + "(ally) " + factionRelation + title + " " + factionRank + "" + factionString + " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage());
	    		if(faction.equalsIgnoreCase(faction2))
    	    		player.sendMessage(Config.Rel_Ally + "(ally) " + factionRelation + title + " " + factionRank + "" + factionString + " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage());
	    		continue;
	    	}
	    	
	    	//truce
	    	if(chatChannel_talk.equalsIgnoreCase("truce")){
	    		simpleFactions.truceData = simpleFactions.factionData.getJSONArray("truce");
	    		for(int i = 0; i<simpleFactions.truceData.length(); i++)
	    			if(simpleFactions.truceData.getString(i).equalsIgnoreCase(faction2))
	    	    		player.sendMessage(Config.Rel_Truce + "(truce) " + factionRelation + title + " " + factionRank + "" + factionString + " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage());
	    		if(faction.equalsIgnoreCase(faction2))
    	    		player.sendMessage(Config.Rel_Truce + "(truce) " + factionRelation + title + " " + factionRank + "" + factionString + " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage());
	    		continue;
	    	}
	    	
	    	//enemy
	    	if(chatChannel_talk.equalsIgnoreCase("enemy")){
	    		simpleFactions.enemyData = simpleFactions.factionData.getJSONArray("enemies");
	    		for(int i = 0; i<simpleFactions.enemyData.length(); i++)
	    			if(simpleFactions.enemyData.getString(i).equalsIgnoreCase(faction2))
	    	    		player.sendMessage(Config.Rel_Enemy + "(enemy) " + factionRelation + title + " " + factionRank + "" + factionString + " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage());
	    		if(faction.equalsIgnoreCase(faction2))
    	    		player.sendMessage(Config.Rel_Enemy + "(enemy) " + factionRelation + title + " " + factionRank + "" + factionString + " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage());
	    		
	    		continue;
	    	}
	    	
	    	//local
	    	boolean treatchatlikeradio = false; 
	    	if(Config.configData.has("treat all chat like a radio"))
	    		if(Config.configData.getString("treat all chat like a radio").equalsIgnoreCase("true"))
	    			treatchatlikeradio = true;
	    	
	    	if(chatChannel_talk.equalsIgnoreCase("local") || treatchatlikeradio){
	    		
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
	    		
	    		if(distance<Config.configData.getInt("local chat distance") && !player.getName().equalsIgnoreCase(event.getPlayer().getName())){
	    				String message_ = Config.Rel_Neutral + "(" + (distance) + "" + Direction + ") "; 
	    				if(Config.configData.getString("show faction data in local chat").equalsIgnoreCase("true")) //only display faction stuff if settings say so
	    					message_ += factionRelation + title + " " + factionRank + "" + factionString;
	    				message_ += " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage();
    	    			player.sendMessage(message_);
    	    		}
	    		
	    		if(distance>=Config.configData.getInt("local chat distance") && distance<Config.configData.getInt("local chat distance")*1.5 && !player.getName().equalsIgnoreCase(event.getPlayer().getName())){ 
    				String message_ = Config.Rel_Neutral + "(you hear something " + distance + " blocks " + Direction + " from you)";
	    			player.sendMessage(message_);
	    		}
	    		
	    		if(player.getName().equalsIgnoreCase(event.getPlayer().getName())){
	    	    		String _message = Config.Rel_Neutral + "(local) ";
	    				if(Config.configData.getString("show faction data in local chat").equalsIgnoreCase("true")) 
	    					_message += factionRelation + title + " " + factionRank + "" + factionString;
	    	    		_message += " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage();
	    			player.sendMessage(_message);
	    		}
	    	    continue;
	    	}
	    	
	    	//custom
	    	if(chatChannel_talk.equalsIgnoreCase(chatChannel_listen)){
	    		player.sendMessage(Config.Rel_Other + "(" + chatChannel_talk  + ") " + factionRelation + factionRank + "" + factionString + " §f(" + factionRelation + playerNickname + "§f): " + event.getMessage());
	    		continue;
	    	}
	    	
		}
		
		//send all messages to the console
		String loggerMessage = "[" + chatChannel_talk + "]" + " " + factionString + " (" + playerNickname + "): " + event.getMessage();
		Bukkit.getLogger().info(loggerMessage);
		
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
		simpleFactions.loadWorld(world.getName());
		simpleFactions.loadPlayer(player.getUniqueId());
    	
    	String playerFaction = simpleFactions.playerData.getString("faction");
    	
    	//do math
		int posX = loc.getBlockX();
		int posY = loc.getBlockY();
		int posZ = loc.getBlockZ();
        int chunkSizeX = Config.chunkSizeX; 
        int chunkSizeY = Config.chunkSizeY; 
        int chunkSizeZ = Config.chunkSizeZ; 
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
    	
    	if(simpleFactions.boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ))
    		inFaction = simpleFactions.boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
    	
    	
    	//figure out if this is a new place
    	int k = -1;
    	for(int i = 0; i < simpleFactions.playerIsIn_player.size(); i++){
    		if(simpleFactions.playerIsIn_player.get(i).equalsIgnoreCase(player.getName())){
    			k = i;
    		}
    	}
    	
    	Location location = new Location(loc.getWorld(),posX,posY,posZ);
    	
    	if(k==-1){
    		simpleFactions.playerIsIn_player.add(player.getName());
    		simpleFactions.playerIsIn_faction.add(inFaction);
    		simpleFactions.playerIsIn_location.add(location);
    		k = 0;
    	}
    	
    	if(!simpleFactions.playerIsIn_faction.get(k).equalsIgnoreCase(inFaction)
    			&& simpleFactions.playerData.has("autoclaim") && simpleFactions.playerData.getString("autoclaim").equalsIgnoreCase("false")
    			&& simpleFactions.playerData.has("autounclaim") && simpleFactions.playerData.getString("autounclaim").equalsIgnoreCase("false")
    			){
    		player.sendMessage("§7You have traveled from " + simpleFactions.getFactionRelationColor(playerFaction,simpleFactions.playerIsIn_faction.get(k)) + Config.configData.getString("faction symbol left") + 
    				simpleFactions.playerIsIn_faction.get(k) + Config.configData.getString("faction symbol right") + 
    				"§7 to " + simpleFactions.getFactionRelationColor(playerFaction,inFaction) + Config.configData.getString("faction symbol left") + 
    				inFaction + Config.configData.getString("faction symbol right") + "§7.");
    		simpleFactions.playerIsIn_faction.set(k, inFaction);

    	}

    	//auto claim land
    	if(!simpleFactions.playerIsIn_location.get(k).equals(location) && !simpleFactions.playerData.getString("faction").equalsIgnoreCase(inFaction)){
    		simpleFactions.playerIsIn_location.set(k,location);
        	if(simpleFactions.playerData.has("autoclaim"))
        		if(simpleFactions.playerData.getString("autoclaim").equalsIgnoreCase("true"))
        			simpleFactions.tryClaim((CommandSender) player); 
    	}
    	
    	//auto un claim land
    	if(!simpleFactions.playerIsIn_location.get(k).equals(location) && simpleFactions.playerData.getString("faction").equalsIgnoreCase(inFaction) 
    			&& !inFaction.equalsIgnoreCase("neutral territory") && !inFaction.equalsIgnoreCase("")){
        	if(simpleFactions.playerData.has("autounclaim"))
        		if(simpleFactions.playerData.getString("autounclaim").equalsIgnoreCase("true"))
        			simpleFactions.tryUnClaim((CommandSender) player); 
    	}
    	
    	
	}


	/**
	 * Eventhandler for block breaking
	 * */
	@EventHandler
	public void blockBreak(BlockBreakEvent event){
		if(!simpleFactions.canEditHere(event.getPlayer(),event.getBlock().getLocation(),"break")){
			event.setCancelled(true);
		}
		
	}
	
	
	/**
	 * Eventhandler for block placing
	 * */
	@EventHandler
	public void blockPlace(BlockPlaceEvent event){
		if(!simpleFactions.canEditHere(event.getPlayer(),event.getBlock().getLocation(),"place")){
			event.setCancelled(true);
		}
	}
	
	
	/**
	 * Eventhandler for name tags (dependent on tagAPI)
	 * */
	/*public void onNameTag(AsyncPlayerReceiveNameTagEvent  event){
		
		boolean hasapi = false; 
		Plugin[] plugins = getServer().getPluginManager().getPlugins();
		
		for(int i = 0; i < plugins.length; i++){
			if(plugins[i].getName().toLowerCase().trim().contains("tagapi")) 
				hasapi = true; 
			if(plugins[i].getName().toLowerCase().contains("tag api")) 
				hasapi = true; 
		}
		
		//boolean hastagapi = getServer().getPluginManager().getPlugins().toString().toLowerCase().contains("tagapi");
		
		if(hasapi){
			String player = event.getPlayer().getName();
			String player2 = event.getNamedPlayer().getName();
		
			loadPlayer(player);
			String faction = playerData.getString("faction");
			loadPlayer(player2);
			String faction2 = playerData.getString("faction");
		
			String rel = getFactionRelationColor(faction,faction2);
		
			event.setTag(rel + event.getTag());
		}
	}*/
	
	/**
	 * When a creeper or tnt explodes, check all affected blocks. If claimed, ignore it (if its set that way in the options);
	 * */
	@EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
		if(simpleFactions.getScheduledTime().equalsIgnoreCase("peace")){ //never explode in peacetime
			event.setCancelled(true);
			return;
		}
		
		if(!simpleFactions.getScheduledTime().equalsIgnoreCase("war")){ //always explode things in wartime
		    List<Block> destroyed = event.blockList();
		    Iterator<Block> it = destroyed.iterator();
		    while (it.hasNext()) {
		        Block block = it.next();
		        Location loc = block.getLocation();
		        simpleFactions.loadWorld(loc.getWorld().getName());
		        int posX = loc.getBlockX();
		        int posY = loc.getBlockY();
		        int posZ = loc.getBlockZ();
		        posX = Math.round(posX / Config.chunkSizeX) * Config.chunkSizeX;
		        posY = Math.round(posY / Config.chunkSizeY) * Config.chunkSizeY;
		        posZ = Math.round(posZ / Config.chunkSizeZ) * Config.chunkSizeZ;
		        if(simpleFactions.boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ)){
		        	String faction = simpleFactions.boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
		        	boolean online = simpleFactions.isFactionOnline(faction);
	
		        	simpleFactions.loadFaction(faction);
		        	if(simpleFactions.factionData.has("peaceful")){
		        		if(simpleFactions.factionData.getString("peaceful").equalsIgnoreCase("true")){
		        			event.setCancelled(true);
				        	break;
		        		}
		        	}else{
		        		simpleFactions.factionData.put("peaceful", "false");
		        		simpleFactions.saveFaction(simpleFactions.factionData);
		        	}
		        	if(simpleFactions.factionData.has("warzone")){
		        		if(simpleFactions.factionData.getString("warzone").equalsIgnoreCase("true")){
		        			event.setCancelled(true);
				        	break;
		        		}
		        	}else{
		        		simpleFactions.factionData.put("warzone", "false");
		        		simpleFactions.saveFaction(simpleFactions.factionData);
		        	}
		        	if(simpleFactions.factionData.has("safezone")){
		        		if(simpleFactions.factionData.getString("safezone").equalsIgnoreCase("true")){
		        			event.setCancelled(true);
				        	break;
		        		}
		        	}else{
		        		simpleFactions.factionData.put("safezone", "false");
		        		simpleFactions.saveFaction(simpleFactions.factionData);
		        	}
		        	
		        	if(online && Config.configData.getString("protect claimed land from explosions while faction is online").equalsIgnoreCase("true")){
			        	event.setCancelled(true);
			        	break;
		        	}
		        	if(!online && Config.configData.getString("protect claimed land from explosions while faction is offline").equalsIgnoreCase("true")){
			        	event.setCancelled(true);
			        	break;
		        	}
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
			if(!simpleFactions.canInteractHere(event.getPlayer(),event.getClickedBlock().getLocation(),"")){
				event.setCancelled(true);
			}
		}
		if(event.hasItem() && event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK){
			if(!simpleFactions.canInteractHere(event.getPlayer(),event.getPlayer().getLocation(),event.getItem().getType().toString())){
				event.setCancelled(true);
			}
		}
	}
	
	/**
	 * when a player dies
	 * */
	@EventHandler
	public void playerDied(PlayerDeathEvent event){
		
		String deathMessage = event.getDeathMessage();
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		String player = ((Player) event.getEntity()).getName();
		String player2 = ""; 
		
		for(Player p : players){
			if(deathMessage.contains(p.getName())){
				if(!p.getName().equalsIgnoreCase(player))
					player2 = p.getName(); 
			}
		}

		if(!player2.equalsIgnoreCase("")){
			simpleFactions.loadPlayer(Bukkit.getPlayer(player2).getUniqueId());
			deathMessage = deathMessage.replace(player2, Config.configData.getString("faction symbol left") + 
					simpleFactions.playerData.getString("faction") + Config.configData.getString("faction symbol right") + " " + player2); 
		}
		
		simpleFactions.loadPlayer(Bukkit.getPlayer(player).getUniqueId());

		deathMessage = deathMessage.replace(player, Config.configData.getString("faction symbol left") + 
				simpleFactions.playerData.getString("faction") + Config.configData.getString("faction symbol right") + " " + player); 
		
		event.setDeathMessage(deathMessage); 
		

		if(player2.equalsIgnoreCase("")){
			event.setDeathMessage(null); 
		}
		
		double power = simpleFactions.playerData.getDouble("power");
		double maxpower = Config.configData.getDouble("max player power");
		double minpower = Config.configData.getDouble("minimum player power");
		
		power-=Config.configData.getDouble("power lost on death");
		if(power<minpower) power = minpower;
		if(power>maxpower) power = maxpower;
		
		int deaths = simpleFactions.playerData.getInt("deaths") + 1;
		simpleFactions.playerData.put("deaths", deaths);
		simpleFactions.playerData.put("power", power);
		simpleFactions.savePlayer(simpleFactions.playerData);
		Player p = event.getEntity();
		if(p.isDead()) {
			p.getKiller();
			if(p.getKiller() instanceof Player) {
				simpleFactions.loadPlayer(p.getKiller().getUniqueId());
				int kills = simpleFactions.playerData.getInt("kills") + 1;
				simpleFactions.playerData.put("kills", kills);
				simpleFactions.savePlayer(simpleFactions.playerData);
			}
		}
	}
	

	/**
	 * EventHandler for when pistons move blocks.
	 * In our case, if a slime block is moved we check the config and
	 * see if it crossed faction lines. Programmatically we're really
	 * only checking if it's it's exceptionally close to faction lines.
	 * */
	/*@EventHandler
	public void PistonMovedBlocksEvent(BlockPistonExtendEvent event){
		
		loadConfig();
		String allow = configData.getString("allow pistons to move slime blocks across faction lines"); 
		
		List<Block> blocks = event.getBlocks();
		for(Block block : blocks){
			if(block.getType() == Material.SLIME_BLOCK){
		        String faction = "";
		        String faction2 = "";
		        
				Location loc = block.getLocation();
		        loadWorld(loc.getWorld().getName());
		        int posX = loc.getBlockX();
		        int posY = loc.getBlockY();
		        int posZ = loc.getBlockZ();
		        posX = Math.round(posX / chunkSizeX) * chunkSizeX;
		        posY = Math.round(posY / chunkSizeY) * chunkSizeY;
		        posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
		        
		        if(boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ)){
		        	faction = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
		        	faction2 = faction;
		        }
		        
		        for(int i = -1; i <= 1; i++){
		        	for(int j = -1; j <= 1; j++){
		        		
		        		int k = i * chunkSizeX;
		        		int l = j * chunkSizeX;
		        		
		        		if(boardData.has("chunkX" + posX+k+l + " chunkY" + posY + " chunkZ" + posZ)){
		        			faction2 = boardData.getString("chunkX" + posX+k+l + " chunkY" + posY + " chunkZ" + posZ);
		        		}

		        		k = i * chunkSizeZ;
		        		l = j * chunkSizeZ;
		        		if(boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ+k+l)){
		        			faction2 = boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ+k+l);
		        		}

		        		k = i * chunkSizeY;
		        		l = j * chunkSizeY;
		        		if(boardData.has("chunkX" + posX + " chunkY" + posY+k+l + " chunkZ" + posZ)){
		        			faction2 = boardData.getString("chunkX" + posX + " chunkY" + posY+k+l + " chunkZ" + posZ);
		        		}
		        	}
		        }
		        
		        if(!faction.equalsIgnoreCase(faction2) && allow.contains("false")){
		        	getLogger().info("faction: " + faction);
		        	getLogger().info("faction2: " + faction2);
		        	event.setCancelled(true);
		        }
			}
		}
	}
	*/
	/*
	 * NOTE: Apparently ExplosionPrimeEvent is bugged; it sets off 1 tick before the
	 * TNT explodes; instead of when the TNT is primed. It's been a bukkit bug for
	 * years apparently and has never been fixed. 
	 * TODO: figure out wtf to do about it.
	 * TODO: Looks like the Spigot team might introduce a new event to solve our issue.
	 * Will keep this code commented out until their next build which implements the 
	 * new event is ready. 
	@SuppressWarnings("deprecation")
	@EventHandler
	public void BlockIgnitedEvent(ExplosionPrimeEvent event){
		getLogger().info("[TNT Debug]: event triggered");
		final TNTPrimed tnt = (TNTPrimed) event.getEntity();
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleAsyncRepeatingTask(this, new BukkitRunnable() {
			@Override
			public void run() {
				double fall = tnt.getFallDistance(); 
				getLogger().info("[TNT Debug]: fall = " + fall);
				if(fall > 10 || tnt.isDead()){
					tnt.setFuseTicks(1); 
					getLogger().info("[TNT Debug]: Should cancel the task now. ");
					getServer().getScheduler().cancelTask(this.getTaskId());
					getLogger().info("[TNT Debug]: Task cancelled. ");
				}
			}},5L,5L);
	}
	*/

}
