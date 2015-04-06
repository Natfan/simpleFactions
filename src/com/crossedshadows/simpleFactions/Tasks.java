package com.crossedshadows.simpleFactions;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class Tasks implements Listener {
	
	private final simpleFactions plugin;

	/**
	 * TODO: Move all tasks into this file.
	 * */
	public Tasks(final simpleFactions plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getLogger().info("Task schedulor started!");
	}
	
	/**
	 * Save data task
	 * */
	@SuppressWarnings("deprecation")
	public static void saveDataToDisk(){
		long minutesBetweenSaves = Config.configData.getLong("minutes between disk saving"); 
		long interval = minutesBetweenSaves*120; 
		
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleAsyncRepeatingTask(simpleFactions.plugin, new Runnable() {
			@Override
			public void run() {
				simpleFactions.saveData(); 
			}
			
		}, interval, interval); 
	}
	
	/**
	 * Task handling for player power (every minute)
	 * */
	@SuppressWarnings("deprecation")
	public static void updatePlayerPower(){
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleAsyncRepeatingTask(simpleFactions.plugin, new Runnable() {
            @Override
            public void run() {
        		//Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions]: Updating player power.."); //debug
            	//Update player power
        		simpleFactions.currentTime = System.currentTimeMillis();
        		Config.loadConfig();
            	double power = 0;
            	double update = (1200.0/72000.0);
            	double powerUpdateOnline = Config.configData.getDouble("power per hour while online") * update;
            	double powerUpdateOffline = Config.configData.getDouble("power per hour while offline") * update;
            	double powerUpdateEnemyInYourTerritory = Config.configData.getDouble("power per hour while enemy in your territory") * update;
            	double powerUpdateWhileInOwnTerritory = Config.configData.getDouble("power per hour while in own territory") * update;
            	double powerUpdateWhileInEnemyTerritory = Config.configData.getDouble("power per hour while in enemy territory") * update;
            	
        		OfflinePlayer[] off = Bukkit.getOfflinePlayers();
        		Collection<? extends Player> on = Bukkit.getOnlinePlayers();
        		

        		if(Config.configData.getString("update power while offline").equalsIgnoreCase("true"))
        			for(int i = 0; i < off.length; i++){ //offline players
        				if(!off[i].isOnline()) {
        					
        					simpleFactions.loadPlayer(off[i].getUniqueId()); 
        					//Bukkit.getLogger().info(simpleFactions.playerData.toString(4)); //debug
        					power = simpleFactions.playerData.getDouble("power");
        					power += powerUpdateOffline;
        					
        					if(power<Config.configData.getDouble("minimum player power"))
        						power = Config.configData.getDouble("minimum player power");
        					if(power>Config.configData.getDouble("max player power"))
        						power = Config.configData.getDouble("max player power");
        					
        					simpleFactions.playerData.put("power",power);
        					simpleFactions.savePlayer(simpleFactions.playerData);
        				}
        			}
        		
        		if(Config.configData.getString("update power while online").equalsIgnoreCase("true"))
        			for(Player player : on){
        				if(player.isOnline()) {
        					
        					simpleFactions.loadPlayer(player.getUniqueId());
        					//Bukkit.getLogger().info(simpleFactions.playerData.toString(4)); //debug
        					power  = simpleFactions.playerData.getDouble("power");
        					
        					if(Config.configData.getString("update power while enemy in your territory").equalsIgnoreCase("true")
        							|| Config.configData.getString("update power while in enemy territory").equalsIgnoreCase("true")){
        						simpleFactions.loadWorld(Bukkit.getPlayer(player.getName()).getLocation().getWorld().getName());
            					Player p = player.getPlayer();
            			    	int posX = p.getLocation().getBlockX();
            			    	int posY = p.getLocation().getBlockY();
            			    	int posZ = p.getLocation().getBlockZ();
            			    	
            			    	posX = Math.round(posX / Config.chunkSizeX) * Config.chunkSizeX;
            			    	posY = Math.round(posY / Config.chunkSizeY) * Config.chunkSizeY;
            			    	posZ = Math.round(posZ / Config.chunkSizeZ) * Config.chunkSizeZ;
            			    	
            			    	if(simpleFactions.boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ)){
            			    		String pFaction = simpleFactions.playerData.getString("faction");
            			    		String rel = simpleFactions.getFactionRelationColor(simpleFactions.boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ),pFaction);
            			    		
            			    		if(rel.equalsIgnoreCase(Config.Rel_Enemy)){
            			    			if(Config.configData.getString("update power while in enemy territory").equalsIgnoreCase("true")){
            			    				power += powerUpdateWhileInEnemyTerritory;
            			    			}
            			    			if(Config.configData.getString("update power while enemy in your territory").equalsIgnoreCase("true")){
            			    				
            			    				for(Player player2 : on){
            			    					simpleFactions.loadPlayer(player2.getPlayer().getUniqueId());
            			    					if(simpleFactions.getFactionRelationColor(simpleFactions.playerData.getString("faction"),pFaction).equalsIgnoreCase(Config.Rel_Enemy)){
            			    						power += powerUpdateEnemyInYourTerritory;
            			    					}
            			    				}
            			    			}
            			    		}
            			    		
            			    		if(rel.equalsIgnoreCase(Config.Rel_Faction)){
            			    			if(Config.configData.getString("update power while in own territory").equalsIgnoreCase("true")){
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
        					
        					
        					if(power<Config.configData.getDouble("minimum player power"))
        						power = Config.configData.getDouble("minimum player power");
        					if(power>Config.configData.getDouble("max player power"))
        						power = Config.configData.getDouble("max player power");
        					
        					long onlineTime = simpleFactions.playerData.getLong("time online");
        					
        					onlineTime += simpleFactions.currentTime - simpleFactions.lastTime;
        					
        					simpleFactions.playerData.put("last online", System.currentTimeMillis());
        					simpleFactions.playerData.put("time online", onlineTime);
        					simpleFactions.playerData.put("power",power);
        					simpleFactions.savePlayer(simpleFactions.playerData);
        				}
        			}

        		//Bukkit.getServer().getConsoleSender().sendMessage("§a[SimpleFactions]: Finished updating player power."); //debug
        		simpleFactions.lastTime = System.currentTimeMillis();
            }
        }, 0L, 1200L);
	}
}
