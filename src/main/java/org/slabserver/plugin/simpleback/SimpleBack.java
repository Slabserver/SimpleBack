package org.slabserver.plugin.simpleback;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleBack extends JavaPlugin implements Listener {
	private static final int MAX_HISTORY_LENGTH = 100;
	private static final String noForwardHistory = ChatColor.RED + "Nothing in forward history to return to";
	private static final String noBackHistory = ChatColor.RED + "Nothing in back history to return to";
	private static final String teleportedTo = "Teleported to %.2f %.2f %.2f (%s)";
	Map<UUID, History> locationHistory = new HashMap<>();

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equals("back")) {
			if (!(sender instanceof Player)) {
				return true;
			}
			Player player = (Player) sender;
			History history = getHistory(player);
			if (history.back.isEmpty()) {
				// nowhere to go
				player.sendMessage(noBackHistory);
			}
			else {
				Location loc = history.back.removeFirst();
				history.forward.addFirst(player.getLocation());
				history.teleportingTo = loc;
				player.teleport(loc);
				player.setPortalCooldown(10);
				player.sendMessage(String.format(teleportedTo, loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName()));
			}
		}
		
		else if (command.getName().equals("forward")) {
			if (!(sender instanceof Player)) {
				return true;
			}
			Player player = (Player) sender;
			History history = getHistory(player);
			if (history.forward.isEmpty()) {
				// nowhere to go
				player.sendMessage(noForwardHistory);
			}
			else {
				Location loc = history.forward.removeFirst();
				history.back.addFirst(player.getLocation());
				history.teleportingTo = loc;
				player.teleport(loc);
				player.setPortalCooldown(10);
				player.sendMessage(String.format(teleportedTo, loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName()));
			}
		}
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return Collections.emptyList();
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		History history = getHistory(player);
		
		if (event.getCause() == TeleportCause.PLUGIN) {
			// ignore teleport events caused by this plugin
			Location teleportingTo = history.teleportingTo;
			if (teleportingTo != null && nearlyEquals(teleportingTo, event.getTo())) {
				history.teleportingTo = null;
				return;
			}
		}
		if (event.isCancelled())
			return;
		
		switch (event.getCause()) {
		case PLUGIN:
		case COMMAND:
		case SPECTATE:
		case NETHER_PORTAL:
		case END_PORTAL:
		case END_GATEWAY:
			history.back.addFirst(event.getFrom());
			history.forward.clear();
			if (history.back.size() > MAX_HISTORY_LENGTH)
				history.back.removeLast();
			break;
		case CHORUS_FRUIT:
		case ENDER_PEARL:
		case UNKNOWN:
		default:
			break;
		}
		
	}
	
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		History history = getHistory(player);
		
		history.back.addFirst(player.getLocation());
		history.forward.clear();
		if (history.back.size() > MAX_HISTORY_LENGTH)
			history.back.removeLast();
	}
	
	private boolean nearlyEquals(Location loc1, Location loc2) {
		if (loc1 == loc2)
			return true;
		if (loc1 == null || loc2 == null)
			return false;
		return loc1.getWorld().equals(loc2.getWorld())
				&& loc1.getBlockX() == loc2.getBlockX()
				&& loc1.getBlockY() == loc2.getBlockY()
				&& loc1.getBlockZ() == loc2.getBlockZ();
	}
	
	private History getHistory(Player player) {
		return locationHistory.computeIfAbsent(player.getUniqueId(), uuid -> new History());
	}
	
	private static class History {
		ArrayDeque<Location> back = new ArrayDeque<>();
		ArrayDeque<Location> forward = new ArrayDeque<>();
		Location teleportingTo;
	}

}
