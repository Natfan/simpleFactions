package com.crossedshadows.simpleFactions;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
	

}
