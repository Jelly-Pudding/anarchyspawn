package com.jellypudding.anarchySpawn.listeners;

import com.jellypudding.anarchySpawn.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerEventListener implements Listener {

    private final SpawnManager spawnManager;

    public PlayerEventListener(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            Location spawnLocation = spawnManager.findSafeSpawnLocation(player.getWorld());
            player.teleport(spawnLocation);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.isAnchorSpawn() && !event.isBedSpawn()) {
            World overworld = event.getPlayer().getServer().getWorlds().getFirst();
            Location spawnLocation = spawnManager.findSafeSpawnLocation(overworld);
            event.setRespawnLocation(spawnLocation);
        }
    }
}
