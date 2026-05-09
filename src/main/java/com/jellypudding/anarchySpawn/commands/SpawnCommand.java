package com.jellypudding.anarchySpawn.commands;

import com.jellypudding.anarchySpawn.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpawnCommand implements CommandExecutor, TabCompleter {

    private final SpawnManager spawnManager;

    public SpawnCommand(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        World.Environment env = player.getWorld().getEnvironment();
        if (env == World.Environment.NETHER || env == World.Environment.THE_END) {
            player.sendMessage("§cYou cannot use /spawn in the " +
                    (env == World.Environment.NETHER ? "Nether" : "End") + ".");
            return true;
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        spawnManager.cleanupExpiredCooldowns(currentTime);

        if (spawnManager.isOnCooldown(playerId, currentTime)) {
            long remaining = spawnManager.getRemainingCooldown(playerId, currentTime);
            player.sendMessage("§cYou must wait " + remaining + " more second" +
                    (remaining == 1 ? "" : "s") + " before using /spawn again.");
            return true;
        }

        Location spawnLocation = spawnManager.findSafeSpawnLocation(player.getWorld());
        player.teleport(spawnLocation);
        player.sendMessage("Teleported to a random spawn location.");
        spawnManager.setCooldown(playerId, currentTime);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        return new ArrayList<>();
    }
}
