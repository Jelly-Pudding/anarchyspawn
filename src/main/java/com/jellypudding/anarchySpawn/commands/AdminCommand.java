package com.jellypudding.anarchySpawn.commands;

import com.jellypudding.anarchySpawn.AnarchySpawn;
import com.jellypudding.anarchySpawn.managers.SpawnManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final AnarchySpawn plugin;
    private final SpawnManager spawnManager;

    public AdminCommand(AnarchySpawn plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /anarchyspawn reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("anarchyspawn.reload")) {
                sender.sendMessage("You don't have permission to use this command!");
                return true;
            }
            plugin.reloadConfig();
            spawnManager.loadConfig();
            sender.sendMessage("AnarchySpawn configuration reloaded.");
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1 && sender.hasPermission("anarchyspawn.reload") && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return new ArrayList<>();
    }
}
