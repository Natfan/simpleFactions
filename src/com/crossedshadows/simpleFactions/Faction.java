package com.crossedshadows.simpleFactions;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

public class Faction {
	public static boolean factionCheck(String name){
    	for(int i = 0; i < Data.Factions.length(); i++){
    		if(Data.Factions.getJSONObject(i).getString("name").equalsIgnoreCase(name)){
    			return true;
    		}
    	}
    	
    	return false; 
    }
	
    public static boolean isFactionOnline(String faction){
		simpleFactions.loadFaction(faction);
		Collection<? extends Player> on = Bukkit.getOnlinePlayers();
		for(Player player : on){
			simpleFactions.loadPlayer(player.getUniqueId());
			if(simpleFactions.playerData.getString("faction").equalsIgnoreCase(faction) && player.isOnline()) {
				simpleFactions.factionData.put("lastOnline", System.currentTimeMillis());
				simpleFactions.saveFaction(simpleFactions.factionData);
				return true; //if anyone from faction is online, break from loop and return true; update last online time
			}
		}
    	
		if(simpleFactions.factionData.getLong("lastOnline")+ (Config.configData.getInt("seconds before faction is considered really offline") * 1000) > System.currentTimeMillis()){
			return true;
		}
		
    	return false;
    }
    
    public static boolean setFactionFlag(CommandSender sender, String faction, String flag, String tr){
    	if(sender.isOp() || sender.hasPermission("simplefactions.admin")){
    		simpleFactions.loadFaction(faction);
    		if(flag.equalsIgnoreCase("peaceful") || flag.equalsIgnoreCase("warzone") || flag.equalsIgnoreCase("safezone")){
    			if(tr.equalsIgnoreCase("true") || tr.equalsIgnoreCase("false")){
    				String ntr = "false";
    				if(tr.equalsIgnoreCase("false")) ntr = "true";
    				simpleFactions.factionData.put("peaceful", ntr);
    				simpleFactions.factionData.put("warzone",  ntr);
    				simpleFactions.factionData.put("safezone", ntr);
    				simpleFactions.factionData.put(flag, tr);
    				simpleFactions.saveFaction(simpleFactions.factionData);
					if(tr.equalsIgnoreCase("true"))
						sender.sendMessage("§a" + Language.getMessage("The faction") + " " + faction + " " + Language.getMessage("is now a") + " " + flag + " " + Language.getMessage("faction."));
					if(tr.equalsIgnoreCase("false"))
						sender.sendMessage("§a" + Language.getMessage("The faction") + faction + " " + Language.getMessage("is no longer a") + " " + flag + " " + Language.getMessage("faction."));
    			}else{
    				sender.sendMessage("§c" + Language.getMessage("Please specify whether you want")  + faction + " " + Language.getMessage("to be") + " " + flag + " " + Language.getMessage("with true or false at the end."));
    			}
    		}else{
    			sender.sendMessage("§c" + Language.getMessage("Please use either peaceful, warzone, or safezone."));
    		}
    	}else{
    		sender.sendMessage("§c" + Language.getMessage("You must be a server OP or have the simplefactions.admin permission to do this!"));
    	}
    	return true;
    }
    
    /**
     * Sets the description of the faction.
     * */
    public static boolean setDesc(CommandSender sender, String[] args){
    	simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    	String faction = simpleFactions.playerData.getString("faction");
    	if(faction.equalsIgnoreCase("")){
    		sender.sendMessage("§c" + Language.getMessage("You are not in a faction!"));
    		return true;
    	}
    	if(args.length>1){
    		String desc = "";
    		for(int i = 1; i<args.length; i++)
    			desc += args[i] + " ";
    		simpleFactions.loadFaction(faction);
    		simpleFactions.factionData.put("desc", desc);
    		simpleFactions.saveFaction(simpleFactions.factionData);
    		messageFaction(faction, "§7Description updated: §f" + desc);
    		return true;
    	}else{
    		sender.sendMessage("§c" + Language.getMessage("Please provide a description!") + " " + 
    				Language.getMessage("Example: /sf desc Example Description"));
    		return true;
    	}
    }
    
    /**
     * Attempts to set a relation with another faction.
     * */
    public static boolean setRelation(CommandSender sender, String[] args, String relation){
    	if(args.length>1){

    		simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    		String senderFaction = simpleFactions.playerData.getString("faction"); 
    		String otherFaction = args[1]; 
    		
    		if(Faction.factionCheck(otherFaction)){

				String relString = "";
				if(relation.equalsIgnoreCase("enemies")) relString = Config.Rel_Enemy;
				if(relation.equalsIgnoreCase("allies")) relString = Config.Rel_Ally;
				if(relation.equalsIgnoreCase("truce")) relString = Config.Rel_Truce;
				if(relation.equalsIgnoreCase("neutral")) relString = Config.Rel_Other;
				
    			if(!relation.equalsIgnoreCase("neutral")){
    				
    				simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    				simpleFactions.loadFaction(senderFaction);
    				simpleFactions.enemyData = simpleFactions.factionData.getJSONArray(relation); //says enemy data; but it can be any of them. Just a free array
    				int k = 0;
    				for(int i = 0; i < simpleFactions.enemyData.length(); i++){ //make sure we don't already have this relation set
    					if(simpleFactions.enemyData.getString(i).equalsIgnoreCase(otherFaction))
    						k++;
    				}
    				if(k<1){
    					simpleFactions.enemyData.put(otherFaction);
    					simpleFactions.factionData.put(relation, simpleFactions.enemyData);
    					simpleFactions.saveFaction(simpleFactions.factionData);
        				
    					simpleFactions.loadFaction(senderFaction);
    					simpleFactions.enemyData = simpleFactions.factionData.getJSONArray("enemies");
    					simpleFactions.allyData = simpleFactions.factionData.getJSONArray("allies");
    					simpleFactions.truceData = simpleFactions.factionData.getJSONArray("truce");
    					if(!relation.equalsIgnoreCase("enemies")){
        					for(int i = 0; i < simpleFactions.enemyData.length(); i++){
        						if(simpleFactions.enemyData.getString(i).equalsIgnoreCase(otherFaction))
        							simpleFactions.enemyData.remove(i);
        					}
        				}
        				if(!relation.equalsIgnoreCase("allies")){
        					for(int i = 0; i < simpleFactions.allyData.length(); i++){
        						if(simpleFactions.allyData.getString(i).equalsIgnoreCase(otherFaction))
        							simpleFactions.allyData.remove(i);
        					}
        				}
        				if(!relation.equalsIgnoreCase("truce")){
        					for(int i = 0; i < simpleFactions.truceData.length(); i++){
        						if(simpleFactions.truceData.getString(i).equalsIgnoreCase(otherFaction))
        							simpleFactions.truceData.remove(i);
        					}
        				}
        				
        				simpleFactions.factionData.put("enemies", simpleFactions.enemyData);
        				simpleFactions.factionData.put("allies", simpleFactions.allyData);
        				simpleFactions.factionData.put("truce", simpleFactions.truceData);
        				simpleFactions.saveFaction(simpleFactions.factionData);
        				
    				}else{
    					sender.sendMessage("§c" + Language.getMessage("You have already this relation set") + "!");
    					return true;
    				}
    				
    				
    				
    				simpleFactions.loadFaction(otherFaction);
    				simpleFactions.enemyData = simpleFactions.factionData.getJSONArray("enemies");
    				simpleFactions.allyData = simpleFactions.factionData.getJSONArray("allies");
    				simpleFactions.truceData = simpleFactions.factionData.getJSONArray("truce");
    				
    				if(!relation.equalsIgnoreCase("allies")){
    					for(int i = 0; i < simpleFactions.allyData.length(); i++){
    						if(simpleFactions.allyData.getString(i).equalsIgnoreCase(senderFaction))
    							simpleFactions.allyData.remove(i);
    					}
    				}
    				if(!relation.equalsIgnoreCase("truce")){
    					for(int i = 0; i < simpleFactions.truceData.length(); i++){
    						if(simpleFactions.truceData.getString(i).equalsIgnoreCase(senderFaction))
    							simpleFactions.truceData.remove(i);
    					}
    				}
    				
    				
					if(relation.equalsIgnoreCase("enemies")){
	    				int m = 0;
	    				for(int i = 0; i < simpleFactions.enemyData.length(); i++){ //make sure we don't already have this relation set
	    					if(simpleFactions.enemyData.getString(i).equalsIgnoreCase(senderFaction))
	    						m++;
	    				}
	    				
	    				if(m<1){
	    					simpleFactions.enemyData.put(senderFaction);
	    				}
					}

					simpleFactions.factionData.put("enemies", simpleFactions.enemyData);
    				simpleFactions.factionData.put("allies", simpleFactions.allyData);
    				simpleFactions.factionData.put("truce", simpleFactions.truceData);
    				simpleFactions.saveFaction(simpleFactions.factionData);
					
					//below this is messages
					
    				int j = 0;
    				
    				if(relation.equalsIgnoreCase("enemies"))
    					for(int i = 0; i < simpleFactions.enemyData.length(); i++)
    						if(simpleFactions.enemyData.getString(i).equalsIgnoreCase(senderFaction))
    							j++;
    				
    				if(relation.equalsIgnoreCase("allies"))	
    					for(int i = 0; i < simpleFactions.allyData.length(); i++)
    						if(simpleFactions.allyData.getString(i).equalsIgnoreCase(senderFaction))
    							j++;
    				
    				if(relation.equalsIgnoreCase("truce"))		
    					for(int i = 0; i < simpleFactions.truceData.length(); i++)
    						if(simpleFactions.truceData.getString(i).equalsIgnoreCase(senderFaction))
    							j++;
    				
    				if(j>0 || (j==0 && relation.equalsIgnoreCase("neutral"))){
    					
    					
    					
    					//message sender's faction
    					messageFaction(senderFaction,"§6You are now " + relString + relation + "§6 with " + 
    							simpleFactions.getFactionRelationColor(senderFaction,otherFaction) + Config.configData.getString("faction symbol left") + otherFaction + 
    							Config.configData.getString("faction symbol right") + "§6.");
    					
    					//message other faction
    					messageFaction(otherFaction,"§6You are now " + relString + relation + "§6 with " + 
    							simpleFactions.getFactionRelationColor(senderFaction,otherFaction) + Config.configData.getString("faction symbol left") + 
    							senderFaction + Config.configData.getString("faction symbol right") + "§6.");
    					return true;
    				}
    				if(j==0){
    					//message sender's faction
    					messageFaction(senderFaction,"§6You have asked " + simpleFactions.getFactionRelationColor(senderFaction,otherFaction) + 
			    				Config.configData.getString("faction symbol left") + otherFaction + Config.configData.getString("faction symbol right") + 
    							" §6if they would like to become " + relString + relation + "§6.");
			    		
			    		//message ask'd faction
    					messageFaction(otherFaction,simpleFactions.getFactionRelationColor(senderFaction,otherFaction) + Config.configData.getString("faction symbol left") + 
			    				senderFaction + Config.configData.getString("faction symbol right") + " §6have asked  if you would like to become " + 
			    				relString + relation + "§6.");
			    		
    					return true;
    				}
    				
    			}
    			else{
    				simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    				simpleFactions.loadFaction(senderFaction);
    				simpleFactions.enemyData = simpleFactions.factionData.getJSONArray("enemies");
    				simpleFactions.allyData = simpleFactions.factionData.getJSONArray("allies");
    				simpleFactions.truceData = simpleFactions.factionData.getJSONArray("truce");
    				
    				int k = 0;
    				
    				for(int i = 0; i < simpleFactions.enemyData.length(); i++)
    					if(simpleFactions.enemyData.getString(i).equalsIgnoreCase(otherFaction)){
    						simpleFactions.enemyData.remove(i);
    						k++;
    						}
    				for(int i = 0; i < simpleFactions.allyData.length(); i++)
    					if(simpleFactions.allyData.getString(i).equalsIgnoreCase(otherFaction)){
    						simpleFactions.allyData.remove(i);
    						k++;
    						}
    				for(int i = 0; i < simpleFactions.truceData.length(); i++)
    					if(simpleFactions.truceData.getString(i).equalsIgnoreCase(otherFaction)){
    						simpleFactions.truceData.remove(i);
    						k++;
    						}
    				
    				if(k==0){
    					sender.sendMessage("§6" + Language.getMessage("You are already neutral with") + " " + simpleFactions.getFactionRelationColor(senderFaction,otherFaction) + 
    							Config.configData.getString("faction symbol left") + otherFaction + Config.configData.getString("faction symbol right") + "§6!");
    					return true;
    				}
    				
    				simpleFactions.factionData.put("enemies", simpleFactions.enemyData);
    				simpleFactions.factionData.put("allies", simpleFactions.allyData);
    				simpleFactions.factionData.put("truce", simpleFactions.truceData);
    				simpleFactions.saveFaction(simpleFactions.factionData);
    				
    				simpleFactions.loadFaction(otherFaction);
    				simpleFactions.enemyData = simpleFactions.factionData.getJSONArray("enemies");
    				simpleFactions.allyData = simpleFactions.factionData.getJSONArray("allies");
    				simpleFactions.truceData = simpleFactions.factionData.getJSONArray("truce");
    				
    				int j = 0;
    				
    				for(int i = 0; i < simpleFactions.enemyData.length(); i++)
    					if(simpleFactions.enemyData.getString(i).equalsIgnoreCase(senderFaction)){
    						j++;
    						}
    				for(int i = 0; i < simpleFactions.allyData.length(); i++)
    					if(simpleFactions.allyData.getString(i).equalsIgnoreCase(senderFaction)){
    						j++;
    						}
    				for(int i = 0; i < simpleFactions.truceData.length(); i++)
    					if(simpleFactions.truceData.getString(i).equalsIgnoreCase(senderFaction)){
    						j++;
    						}
    				
    				if(k>0 && j==0){
    					sender.sendMessage("§6" + Language.getMessage("You are now neutral with") + " " + simpleFactions.getFactionRelationColor(senderFaction,otherFaction) + 
    							Config.configData.getString("faction symbol left") + otherFaction + Config.configData.getString("faction symbol right") + "§6.");
    					return true;
    				}
    				if(k>0 && j>0){
    					sender.sendMessage("§6" + Language.getMessage("You have asked") + " " + simpleFactions.getFactionRelationColor(senderFaction,otherFaction) +
    							Config.configData.getString("faction symbol left") + otherFaction + Config.configData.getString("faction symbol right") + 
    							"§6 " + Language.getMessage("if they would like to become") + " " + relString + "" + Language.getMessage("neutral") + ".");
    					return true;
    				}
    			}
    		}
    		else{
    			sender.sendMessage("§c" + Language.getMessage("This faction doesn't exist") + "!");
    		}
    	}
    	else{
    		sender.sendMessage("§c" + Language.getMessage("You must provide the name of the faction that you wish to enemy! Example: /sf enemy factionName"));
    	}
    	return true;
    }
    
    
    /**
     * Tries to create a faction with the specified name.
     * */
    public static boolean tryCreateFaction(CommandSender sender, String[] args){
    	if(args.length<2){
			sender.sendMessage("Please include a name! Example: /sf create name");
		}else{
			
			if(args[1].contains("/") || args[1].contains("\\") || args[1].contains(".") || args[1].contains("\"") 
				|| args[1].contains(",") || args[1].contains("?") || args[1].contains("'") || args[1].contains("*") 
				|| args[1].contains("|") || args[1].contains("<") || args[1].contains(":") || args[1].contains("$")){
				sender.sendMessage("§cName cannot contain special characters!");
				return true;
			}
			
			for(int i = 0; i<simpleFactions.factionIndex.size(); i++){
				if(simpleFactions.factionIndex.get(i).equalsIgnoreCase(args[1].toString())){
					sender.sendMessage("§cThe faction name §f" + args[1].toString() + "§c is already taken!");
					return false;
				}
			}

			simpleFactions.loadPlayer(((Player) sender).getUniqueId()); //load up playerData jsonobject
			
			String factionName = simpleFactions.playerData.getString("faction");
			if(!factionName.equalsIgnoreCase("")){
				sender.sendMessage("§cYou are already in a faction!");
				sender.sendMessage("§ccurrent faction: §b" + Config.configData.getString("faction symbol left") + factionName + Config.configData.getString("faction symbol right"));
				sender.sendMessage("§cPlease do /sf leave in order to create a new faction!");
				return false;
			}

			createFaction(args[1].toString());
			
			simpleFactions.loadPlayer(((Player) sender).getUniqueId());
			simpleFactions.playerData.put("factionRank","leader");
			simpleFactions.playerData.put("faction", args[1].toString());
			simpleFactions.savePlayer(simpleFactions.playerData);
			
			simpleFactions.messageEveryone("§6The faction name " + Config.Rel_Other + args[1].toString() + " §6has been created!");
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
    	simpleFactions.enemyData = new JSONArray();
    	simpleFactions.allyData = new JSONArray();
    	simpleFactions.truceData = new JSONArray();
    	simpleFactions.inviteData = new JSONArray();
    	simpleFactions.factionData = new JSONObject();
    	simpleFactions.factionData.put("name", faction);
    	simpleFactions.factionData.put("peaceful", "false");
    	simpleFactions.factionData.put("warzone", "false");
    	simpleFactions.factionData.put("safezone", "false");
    	simpleFactions.factionData.put("ID", UUID.randomUUID().toString());
    	simpleFactions.factionData.put("shekels", 0.0);
    	simpleFactions.factionData.put("enemies",simpleFactions.enemyData);
    	simpleFactions.factionData.put("allies",simpleFactions.allyData);
    	simpleFactions.factionData.put("truce", simpleFactions.truceData);
    	simpleFactions.factionData.put("invited", simpleFactions.inviteData);
    	simpleFactions.factionData.put("lastOnline", System.currentTimeMillis());
    	simpleFactions.factionData.put("home", "");
    	simpleFactions.factionData.put("desc", Config.configData.getString("default faction description"));
    	simpleFactions.factionData.put("open", Config.configData.getString("factions open by default"));
    	simpleFactions.saveFaction(simpleFactions.factionData);
    	//simpleFactions.factionIndex.add(faction);
    	
    	for(int i = 0; i < simpleFactions.factionIndex.size(); i++){
  			if(simpleFactions.factionIndex.get(i).equals(faction)){
  				simpleFactions.factionIndex.remove(i); 
  			}
  		}
    	 
    	simpleFactions.factionIndex.add(faction);
    	
    	for(int i = 0; i < Data.Factions.length(); i++){
  			if(Data.Factions.getJSONObject(i).getString("ID").equals(simpleFactions.factionData.getString("ID"))){
  				Data.Factions.remove(i); 
  			}
  		}
  		
  		Data.Factions.put(simpleFactions.factionData);
    	
    }
    
    /**
     * Attempts to set a home on the specified block.
     * */
    public static boolean trySetHome(CommandSender sender){
    	simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    	Player player = ((Player) sender);
    	String factionName = simpleFactions.playerData.getString("faction");
    	
    	if(factionName.equalsIgnoreCase("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}
    	
    	if(!simpleFactions.playerData.getString("factionRank").equalsIgnoreCase("officer") && !simpleFactions.playerData.getString("factionRank").equalsIgnoreCase("leader")){
    		sender.sendMessage("§cYou aren't a high enough rank to do this.");
    		return true; 
    	}
    	
    	Config.loadConfig(); 
    	for(int i = 0; i<Config.claimsDisabledInTheseWorlds.length(); i++){
    		String worldDisabled = Config.claimsDisabledInTheseWorlds.optString(i);
    		if(worldDisabled.equalsIgnoreCase(player.getWorld().getName())){
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
    		return true;
    	}
    	
    	simpleFactions.loadPlayer(player.getUniqueId());
    	simpleFactions.loadFaction(simpleFactions.playerData.getString("faction"));
    	simpleFactions.factionData.put("home",world + " " + posX + " " + posY + " " + posZ);
    	simpleFactions.saveFaction(simpleFactions.factionData);
		//sender.sendMessage("§7You have set your faction home at §6" + (int)posX + "," + (int)posY + "," + (int)posZ + "§7.");
    	messageFaction(simpleFactions.playerData.getString("faction"), Config.Rel_Faction + sender.getName()  + "§7 has set your faction home at §6" + (int)posX + "," + (int)posY + "," + (int)posZ + "§7.");
    	return true;
    }
    
    /**
     * Disbands the faction given.
     * */
    public static boolean tryDisband(CommandSender sender,String factionName){
    	
    	simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    	if(simpleFactions.playerData.getString("faction").equalsIgnoreCase("")){
    		sender.sendMessage("You must be in a faction to do this!");
    		return true;
    	}
    	
    	if(!simpleFactions.playerData.getString("factionRank").equalsIgnoreCase("leader") && !sender.isOp()){
    		sender.sendMessage("You cannot do this unless you are the leader of your faction!");
    		return true; 
    	}
    	
    	if(factionName.equalsIgnoreCase("")){
    		factionName = simpleFactions.playerData.getString("faction"); 
    	}
    	
    	for(UUID player : simpleFactions.playerIndex){
    		simpleFactions.loadPlayer(player);
    		if(simpleFactions.playerData.getString("faction").equalsIgnoreCase(factionName)){
    			simpleFactions.playerData.put("faction", "");
    			simpleFactions.playerData.put("factionRank", "member");
    			simpleFactions.savePlayer(simpleFactions.playerData);
    		}
    	}
    	
    	simpleFactions.deleteFaction(factionName); 
    	
		sender.sendMessage("The faction has been disbanded!");
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
    public static boolean setAccess(CommandSender sender,String[] args){
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
    		simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    		simpleFactions.loadFaction(simpleFactions.playerData.getString("faction"));
    	JSONObject chunk = new JSONObject();
    		JSONObject type = new JSONObject();
    			JSONObject subType = new JSONObject();
    				JSONArray allowed = new JSONArray();
    				JSONArray notAllowed = new JSONArray();
    		
    		if(args[1].equalsIgnoreCase("p") || args[1].equalsIgnoreCase("r") || args[1].equalsIgnoreCase("f")){
    			
    			if(args[1].equalsIgnoreCase("p") && !simpleFactions.playerCheck(args[2])){
    				sender.sendMessage(Language.getMessage("Player not found!"));
    				return true;
    			}
    			
    			if(args[1].equalsIgnoreCase("f") && !Faction.factionCheck(args[2])){
    				sender.sendMessage(Language.getMessage("Faction not found!"));
    				return true;
    			}
    			
        		if(simpleFactions.factionData.has(args[1])){
        			if(args.length>5){
        				if(simpleFactions.factionData.has(board))
        					chunk = simpleFactions.factionData.getJSONObject(board);
        			}
        			type = simpleFactions.factionData.getJSONObject(args[1]);
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
        		
        		if(args[4].equalsIgnoreCase("true") || args[4].equalsIgnoreCase("yes")){
        			allowed.put(args[3].toUpperCase());
        			for(int i = 0; i < notAllowed.length(); i++) 
        				if(notAllowed.getString(i).equalsIgnoreCase(args[3].toUpperCase())) 
        					notAllowed.remove(i);
        		}
        		if(args[4].equalsIgnoreCase("false") || args[4].equalsIgnoreCase("no")){
        			notAllowed.put(args[3].toUpperCase());
        			for(int i = 0; i < allowed.length(); i++) 
        				if(allowed.getString(i).equalsIgnoreCase(args[3].toUpperCase())) 
        					allowed.remove(i);
        		}

        		subType.put("allowed", allowed);
        		subType.put("notAllowed", notAllowed);
        		type.put(args[2], subType);
        		if(args.length>5){
        			chunk.put(args[1], type);
        			simpleFactions.factionData.put(board, chunk); 
        		}
        		else{
        			simpleFactions.factionData.put(args[1], type); 
        		}
        		simpleFactions.saveFaction(simpleFactions.factionData);
        		
        		String stype = ""; 
        			if(args[1].equalsIgnoreCase("p")) stype = "§7The player " + Config.Rel_Faction;
        			if(args[1].equalsIgnoreCase("r")) stype = "§7Members ranked as " + Config.Rel_Faction;
        			if(args[1].equalsIgnoreCase("f")) stype = "§7The faction " + simpleFactions.getFactionRelationColor(simpleFactions.factionData.getString("name"),args[2]);
        		String sSubType = args[2];
        		String access = "";
        			if(args[4].equalsIgnoreCase("true")) access = " §7can now access §a";
        			if(args[4].equalsIgnoreCase("false")) access = " §7can no longer access §c";
        		String block = args[3].toLowerCase() + "§7";
        		String area = "";
        		if(args.length>5 && !board.equalsIgnoreCase("")) area = " §7at§6 " + board;
        		messageFaction(simpleFactions.factionData.getString("name"),stype + sSubType + access + block + area + "§7.");
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
     * Returns the Location of the faction home. 
     * */
    public static Location getHome(String faction){
    	simpleFactions.loadFaction(faction);
    	String home = simpleFactions.factionData.getString("home");
    	
    	if(home.equalsIgnoreCase(""))
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
    public static boolean tryHome(CommandSender sender){
    	simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    	Player player = ((Player) sender);
    	String factionName = simpleFactions.playerData.getString("faction");
    	if(factionName.equalsIgnoreCase("")){
    		sender.sendMessage("§cYou aren't in a faction.");
    		return true;
    	}

    	simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    	simpleFactions.loadFaction(simpleFactions.playerData.getString("faction"));
    	
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
     * Gather information on a faction and put it into a string.
     * */
    public static String factionInformationString(CommandSender sender, String faction){
    	simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    	String viewingFaction = simpleFactions.playerData.getString("faction");
    	simpleFactions.loadFaction(faction);
    	faction = simpleFactions.factionData.getString("name"); 
    	//Bukkit.getLogger().info(factionData.toString(4)); //debug
    	DecimalFormat df = new DecimalFormat("0.###");
    	
    	String truce = "";
    	String ally = "";
    	String enemy = "";
    	
    	simpleFactions.enemyData = simpleFactions.factionData.getJSONArray("enemies");
    	simpleFactions.truceData = simpleFactions.factionData.getJSONArray("truce");
    	simpleFactions.allyData = simpleFactions.factionData.getJSONArray("allies");
    	
    	Location factionHome = Faction.getHome(faction); 
    	
    	//scan relationships
    	for(int i = 0; i < simpleFactions.factionIndex.size(); i++){
    		String checkFaction = simpleFactions.factionIndex.get(i);
    		String rel = simpleFactions.getFactionRelationColor(faction, checkFaction);
    		
    		simpleFactions.loadFaction(checkFaction); 
        	if(simpleFactions.factionData.getString("peaceful").equalsIgnoreCase("true")) continue;  
        	if(simpleFactions.factionData.getString("safezone").equalsIgnoreCase("true")) continue; 
        	if(simpleFactions.factionData.getString("warzone").equalsIgnoreCase("true")) continue;
    		
    		if(rel.equalsIgnoreCase(Config.Rel_Enemy)){
    			enemy += ", " + Config.configData.getString("faction symbol left") + checkFaction + Config.configData.getString("faction symbol right");
    		}
    		
    		if(rel.equalsIgnoreCase(Config.Rel_Truce)){
    			truce += ", " + Config.configData.getString("faction symbol left") + checkFaction + Config.configData.getString("faction symbol right");
    		}

    		if(rel.equalsIgnoreCase(Config.Rel_Ally)){
    			ally += ", " + Config.configData.getString("faction symbol left") + checkFaction + Config.configData.getString("faction symbol right");
    		}
    	}

    	//manage strings
    	enemy = enemy.replaceFirst(",","");
    	truce = truce.replaceFirst(",","");
    	ally = ally.replaceFirst(",","");

    	simpleFactions.loadFaction(faction);
    	String factionInfo = "";
    	factionInfo += "§6---- " + simpleFactions.getFactionRelationColor(viewingFaction,faction) + 
    			Config.configData.getString("faction symbol left") + faction + Config.configData.getString("faction symbol right") + "§6 ---- \n";
    	factionInfo += "§6" + simpleFactions.factionData.getString("desc") + "\n§6";
    	
    	if(simpleFactions.factionData.getString("safezone").equalsIgnoreCase("true"))
    		factionInfo += "§6This faction is a safezone.\n"; 
    	if(simpleFactions.factionData.getString("warzone").equalsIgnoreCase("true"))
    		factionInfo += "§cThis faction is a warzone.\n"; 
    	if(simpleFactions.factionData.getString("peaceful").equalsIgnoreCase("true"))
    		factionInfo += "§6This faction is peaceful.\n"; 
    	
    	if(factionHome != null && viewingFaction.equalsIgnoreCase(faction))
    		factionInfo += "Home in " + factionHome.getWorld().getName() + " at x" + 
    			Math.round(factionHome.getX()) + " z" + Math.round(factionHome.getZ()) + " y" + Math.round(factionHome.getY());
    	
    	factionInfo += "§6Power: " + simpleFactions.getFactionClaimedLand(faction) + "/" + df.format(simpleFactions.getFactionPower(faction)) + "/" + df.format(simpleFactions.getFactionPowerMax(faction)) + "\n";
    	
    	String isOnline = "§coffline";
    	
    	if(Faction.isFactionOnline(faction))
    		isOnline = "§bonline";
    	
    	factionInfo += "§6This faction is " + isOnline + "§6.\n";
    	
    	if(isOnline.equalsIgnoreCase("§coffline")){
    		long time = simpleFactions.factionData.getLong("lastOnline") - System.currentTimeMillis() - (Config.configData.getInt("seconds before faction is considered really offline") * 1000);
    		int seconds = (int) (-time/1000);
    		factionInfo += "§6Has been offline for " + (seconds) + " seconds. \n";
    	}else{
    		long time = simpleFactions.factionData.getLong("lastOnline") - System.currentTimeMillis() - (Config.configData.getInt("seconds before faction is considered really offline") * 1000);
    		int seconds = (int) (-time/1000);
    		
    		if(seconds<299){
    		factionInfo += "§6Faction will become §coffline§6 if no members are §bonline§6 for " + (((simpleFactions.factionData.getLong("lastOnline") - System.currentTimeMillis())/1000) + Config.configData.getInt("seconds before faction is considered really offline")) + "§6 more seconds. \n";
    		}
    	}
    	
    	if(!ally.equals("")) 
    		factionInfo += "§dAlly: " + ally.replace("]", "").replace("[", "").replace("\"", "") + "\n§6";
    	if(!truce.equals("")) 
    		factionInfo += "§6Truce: " + truce.replace("]", "").replace("[", "").replace("\"", "") + "\n§6";
    	if(!enemy.equals("")) 
    		factionInfo += "§cEnemy: " + enemy.replace("]", "").replace("[", "").replace("\"", "") + "\n§6";
    	
    	String members = "";
    	String offMembers = "";
		OfflinePlayer[] off = Bukkit.getOfflinePlayers();
		Collection<? extends Player> on = Bukkit.getOnlinePlayers();
		
		
		for(Player player : on){
			simpleFactions.loadPlayer(player.getUniqueId());
			if(simpleFactions.playerData.getString("faction").equalsIgnoreCase(faction) && player.isOnline()) {
				if(!members.equalsIgnoreCase("")) 
					members+= ", ";
				members+="(" + simpleFactions.playerData.getString("factionRank") + ") " + player.getName();
			}
		}
		
		for(int i = 0; i < off.length; i++){
			simpleFactions.loadPlayer(off[i].getUniqueId());
			if(simpleFactions.playerData.getString("faction").equalsIgnoreCase(faction) && !off[i].isOnline()) {
				if(!members.contains(off[i].getName())){
					if(!offMembers.equalsIgnoreCase("")) 
						offMembers+= ", ";
					offMembers+="(" + simpleFactions.playerData.getString("factionRank") + ") " + off[i].getName();
				}
			}
		}
		
		/*
		 * Code from 1.7.9
		for(int i = 0; i < on.length; i++){
			loadPlayer(on[i].getName());
			if(playerData.getString("faction").equalsIgnoreCase(faction) && on[i].isOnline()) {
				if(!members.equalsIgnoreCase("")) 
					members+= ", ";
				members+="(" + playerData.getString("factionRank") + ") " + on[i].getName();
			}
		}*/
    	
    	if(!members.equalsIgnoreCase("")) factionInfo += "§6Online: " + members + "\n";
    	if(!offMembers.equalsIgnoreCase("")) factionInfo += "§6Offline: " + offMembers + "\n";
    	
    	return factionInfo;
    }
    
    /**
     * List all factions on the server.
     * */
    public static boolean listFactions(CommandSender sender, String[] args){
    	int page = 0;
    	DecimalFormat df = new DecimalFormat("0.###");
    	String filter = "";
    	if(args.length>1){
    		Scanner scan = new Scanner(args[1]);
    		if(scan.hasNext() && !scan.hasNextInt()){
    			filter = scan.next();
    			filter = filter.toLowerCase();
    			if(!filter.equalsIgnoreCase("ally") && !filter.equalsIgnoreCase("enemy") && !filter.equalsIgnoreCase("truce")){
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
    			if(filter.equalsIgnoreCase("")){
    				sender.sendMessage("§cPlease provide a page number.§7 Example: §b/sf list 2");
        			scan.close();
    				return true;
    			}
    		}
    		scan.close();
    	}
    	String factionList = "";

    	if(simpleFactions.factionIndex.size() == 0){
    		sender.sendMessage("§6There are no factions to show!");
    		return true;
    	}
    	if(simpleFactions.factionIndex.size() == 1)
    		sender.sendMessage("§6Only one faction exsists on this server.");
    	else
    		sender.sendMessage("§6There are " + simpleFactions.factionIndex.size() + " factions on this server.");
    	
    	simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    	String factionName = simpleFactions.playerData.getString("faction");
    		factionList += "  " + simpleFactions.getFactionRelationColor(factionName,factionName) + "" + Config.configData.getString("faction symbol left") + factionName + Config.configData.getString("faction symbol right")
    			+ " "  + simpleFactions.getFactionClaimedLand(factionName) + "/" + df.format(simpleFactions.getFactionPower(factionName)) + "/" + df.format(simpleFactions.getFactionPowerMax(factionName))   +"" + "§7 <-- you\n";
    	
    	for(int i=0; i<simpleFactions.factionIndex.size(); i++){
    		String name = simpleFactions.factionIndex.get(i);
        	String Rel = "";
        	if(filter.equalsIgnoreCase("ally")) Rel = Config.Rel_Ally;
        	if(filter.equalsIgnoreCase("enemy")) Rel = Config.Rel_Enemy;
        	if(filter.equalsIgnoreCase("truce")) Rel = Config.Rel_Truce;
        	if(filter.equalsIgnoreCase("")) Rel = simpleFactions.getFactionRelationColor(factionName,name);
        		
        	if(!simpleFactions.factionIndex.get(i).equalsIgnoreCase(factionName) && Rel.equalsIgnoreCase(simpleFactions.getFactionRelationColor(factionName,name)))
        		factionList += "  " + simpleFactions.getFactionRelationColor(factionName,name) + "" + Config.configData.getString("faction symbol left") + name + Config.configData.getString("faction symbol right")
        			+ " " + simpleFactions.getFactionClaimedLand(name) + "/" + df.format(simpleFactions.getFactionPower(name)) + "/" + df.format(simpleFactions.getFactionPowerMax(name))   + "\n";
        }
    	
    	int lineCount = 0;
    	int pageCount = 0;
    	String pageToDisplay = "";
    	
    	for(int i = 1; i < factionList.length(); i++){
    		if(pageCount == page) pageToDisplay+= factionList.charAt(i);
    		if(factionList.substring(i-1, i).equalsIgnoreCase("\n")){
    			lineCount++;
    			if(lineCount>7){
    				lineCount = 0;
    				pageCount++;
    			}
    		}
    	}
    	
    	if(page>pageCount) {
    		sender.sendMessage("§cThere are only " + (pageCount+1) + " faction list pages!");
    	}
    	
    	if(filter.equalsIgnoreCase("ally")) sender.sendMessage("§6Filter: " + Config.Rel_Ally + "Ally");
    	if(filter.equalsIgnoreCase("truce")) sender.sendMessage("§6Filter: " + Config.Rel_Truce + "Truce");
    	if(filter.equalsIgnoreCase("enemy")) sender.sendMessage("§6Filter: " + Config.Rel_Enemy + "Enemy");
    	sender.sendMessage("§6Faction List - page " + (page+1) + "/ " + (pageCount+1) + " \n" + pageToDisplay);
    	return true;
    }
    
	public static String getFactionAt(Location location){
		String faction = "";
		simpleFactions.loadWorld(location.getWorld().getName());

    	int posX = location.getBlockX(), chunkSizeX = Config.chunkSizeX;
    	int posY = location.getBlockY(), chunkSizeY = Config.chunkSizeY;
    	int posZ = location.getBlockZ(), chunkSizeZ = Config.chunkSizeZ;
    	posX = Math.round(posX / chunkSizeX) * chunkSizeX;
    	posY = Math.round(posY / chunkSizeY) * chunkSizeY;
    	posZ = Math.round(posZ / chunkSizeZ) * chunkSizeZ;
    	
    	if(simpleFactions.boardData.has("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ))
    		faction = simpleFactions.boardData.getString("chunkX" + posX + " chunkY" + posY + " chunkZ" + posZ);
    	
		return faction;
	}
	
    /**
     * Sends out a sendMessage to everyone in a certain faction.
     * */
    public static void messageFaction(String faction, String message){
    	Collection<? extends Player> on = Bukkit.getOnlinePlayers();
    	
    	for(Player player : on){
    		simpleFactions.loadPlayer(player.getUniqueId()); 
    		if(simpleFactions.playerData.getString("faction").equalsIgnoreCase(faction)){
    			player.getPlayer().sendMessage(message);
    		}
    	}
    }
    
    public static boolean tryOpen(CommandSender sender){
    	
    	simpleFactions.loadPlayer(((Player) sender).getUniqueId());
    	
    	if(!simpleFactions.playerData.getString("factionRank").equalsIgnoreCase("officer") && !simpleFactions.playerData.getString("factionRank").equalsIgnoreCase("leader")){
    		sender.sendMessage("§c" + Language.getMessage("You aren't a high enough rank to do this."));
    		return true; 
    	}
    	
    	simpleFactions.loadFaction(simpleFactions.playerData.getString("faction"));
    	
    	String open = simpleFactions.factionData.getString("open");
    	if(open.equalsIgnoreCase("true")){
    		open = "false";
    		simpleFactions.factionData.put("open", "false");
    	}else{
    		open = "true";
    		simpleFactions.factionData.put("open", "true");
    	}
    	
    	simpleFactions.saveFaction(simpleFactions.factionData);
    	messageFaction(simpleFactions.playerData.getString("faction"), "§bYour faction is now set to " + open + ".");
    	
    	return true;
    }
}
