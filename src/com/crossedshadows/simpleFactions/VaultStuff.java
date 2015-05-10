package com.crossedshadows.simpleFactions;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.json.JSONObject;

import net.milkbowl.vault.economy.Economy;

public class VaultStuff {
	
    public static Economy econ = null;
    public static boolean useVault = false; 
	
	private void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
        	Bukkit.getConsoleSender().sendMessage("[SimpleFactions]: Vault not found!");
            useVault = false; 
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            useVault = false; 
        }
        econ = rsp.getProvider();
        if(econ!=null){
        	useVault = true;
        }else{
        	useVault = false; 
        }
    }

	/**
	 * Adds money to a player of a given UUID
	 * */
	public void addMoneyToPlayer(UUID player, double amount){
		if(useVault){
			if(!econ.hasAccount(Bukkit.getOfflinePlayer(player)))
				econ.createPlayerAccount(Bukkit.getOfflinePlayer(player));
			
			econ.depositPlayer(Bukkit.getOfflinePlayer(player), amount);	
		}else{
			simpleFactions.loadPlayer(player);
			JSONObject playerData = simpleFactions.playerData; 
			double money = playerData.getDouble("shekels");
			money += amount;
			playerData.put("shekels", money); 
			simpleFactions.savePlayer(playerData);
		}
	}
	
	public boolean canPlayerSpendAmount(UUID player, double amount){
		if(useVault){
			if(!econ.hasAccount(Bukkit.getOfflinePlayer(player)))
				econ.createPlayerAccount(Bukkit.getOfflinePlayer(player));
			
			if(econ.getBalance(Bukkit.getOfflinePlayer(player)) > amount){
				return true; 
			}
		}else{
			simpleFactions.loadPlayer(player);
			JSONObject playerData = simpleFactions.playerData; 
			double money = playerData.getDouble("shekels");
			
			if(money > amount){
				return true; 
			}
		}
		
		return false;
	}
	
	public void playerSpendMoney(UUID player, double amount){
		if(useVault){
			if(!econ.hasAccount(Bukkit.getOfflinePlayer(player)))
				econ.createPlayerAccount(Bukkit.getOfflinePlayer(player));
			
			econ.withdrawPlayer(Bukkit.getOfflinePlayer(player), amount);	
		}else{
			simpleFactions.loadPlayer(player);
			JSONObject playerData = simpleFactions.playerData; 
			double money = playerData.getDouble("shekels");
			money -= amount;
			playerData.put("shekels", money); 
			simpleFactions.savePlayer(playerData);
		}
	}
	
	/**
	 * Adds money to a faction of a given uuid
	 * */
	public void addMoneyToFaction(String faction, UUID player, double amount){
		boolean hasEnoughMoney = false;
		
		//withdraw from player vault
		if(useVault){
			if(!econ.hasAccount(Bukkit.getOfflinePlayer(player)))
				econ.createPlayerAccount(Bukkit.getOfflinePlayer(player));
			
			if(econ.getBalance(Bukkit.getOfflinePlayer(player)) > amount){
				hasEnoughMoney = true;
				econ.withdrawPlayer(Bukkit.getOfflinePlayer(player), amount); 
			}
		}else{
			simpleFactions.loadPlayer(player);
			JSONObject playerData = simpleFactions.playerData; 
			double money = playerData.getDouble("shekels");
			if(money > amount){
				hasEnoughMoney = true; 
				money -= amount; 
				playerData.put("shekels", money);
				simpleFactions.savePlayer(playerData);
			}
		}
		
		//deposit into faction vault
		if(hasEnoughMoney){
			simpleFactions.loadFaction(faction);
			JSONObject factionData = simpleFactions.factionData; 
			double money = 0.0;
			if(factionData.has("shekels"))
				money = factionData.getDouble("shekels"); 
			money += amount; 
			factionData.put("shekels", money); 
			simpleFactions.saveFaction(factionData);
		}
	}
	
	public boolean canFactionSpendThisAmount(String faction, double amount){
		simpleFactions.loadFaction(faction);
		JSONObject factionData = simpleFactions.factionData; 
		double money = 0.0;
		if(factionData.has("shekels"))
			money = factionData.getDouble("shekels"); 
		money -= amount; 
		
		if(money > 0)
			return true; 
		
		return false; 
	}
	
	public void spendFactionMoney(String faction, UUID player, double amount){

		simpleFactions.loadFaction(faction);
		JSONObject factionData = simpleFactions.factionData; 
		double money = 0.0;
		if(factionData.has("shekels"))
			money = factionData.getDouble("shekels"); 
		money -= amount; 
		factionData.put("shekels", money); 
		simpleFactions.saveFaction(factionData);
		
	}
}
