package com.jellypudding.anarchySpawn;

import com.jellypudding.anarchySpawn.commands.AdminCommand;
import com.jellypudding.anarchySpawn.commands.SpawnCommand;
import com.jellypudding.anarchySpawn.listeners.PlayerEventListener;
import com.jellypudding.anarchySpawn.managers.SpawnManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class AnarchySpawn extends JavaPlugin {

    private SpawnManager spawnManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        spawnManager = new SpawnManager(this);
        spawnManager.loadConfig();

        getServer().getPluginManager().registerEvents(new PlayerEventListener(spawnManager), this);

        SpawnCommand spawnCommand = new SpawnCommand(spawnManager);
        PluginCommand spawn = Objects.requireNonNull(getCommand("spawn"));
        spawn.setExecutor(spawnCommand);
        spawn.setTabCompleter(spawnCommand);

        AdminCommand adminCommand = new AdminCommand(this, spawnManager);
        PluginCommand anarchyspawn = Objects.requireNonNull(getCommand("anarchyspawn"));
        anarchyspawn.setExecutor(adminCommand);
        anarchyspawn.setTabCompleter(adminCommand);

        new Metrics(this, 27563);
        getLogger().info("AnarchySpawn has been enabled.");
    }

    @Override
    public void onDisable() {
        if (spawnManager != null) {
            spawnManager.clearCooldowns();
        }
        getLogger().info("AnarchySpawn has been disabled.");
    }
}
