package com.jellypudding.anarchySpawn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class AnarchySpawn extends JavaPlugin implements Listener {
    private int spawnRadius;
    private int maxAttempts;
    private int spawnCooldown;
    private final Random random = new Random();
    private final Set<Material> unsafeBlocks = new HashSet<>();
    private final Map<UUID, Long> spawnCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("spawn")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("anarchyspawn")).setTabCompleter(this);

        // Initialise bStats
        int pluginId = 27563;
        new Metrics(this, pluginId);

        getLogger().info("AnarchySpawn has been enabled.");
    }

    @Override
    public void onDisable() {
        spawnCooldowns.clear();
        getLogger().info("AnarchySpawn has been disabled.");
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        spawnRadius = config.getInt("spawn-radius", 300);
        maxAttempts = config.getInt("max-spawn-attempts", 75);
        spawnCooldown = config.getInt("spawn-cooldown", 5);

        unsafeBlocks.clear();
        List<String> unsafeBlocksList = config.getStringList("unsafe-blocks");
        for (String blockName : unsafeBlocksList) {
            try {
                Material material = Material.valueOf(blockName.toUpperCase());
                unsafeBlocks.add(material);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid material name in config: " + blockName);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }

            World.Environment playerEnv = player.getWorld().getEnvironment();
            if (playerEnv == World.Environment.NETHER || playerEnv == World.Environment.THE_END) {
                player.sendMessage("§cYou cannot use /spawn in the " + 
                    (playerEnv == World.Environment.NETHER ? "Nether" : "End") + ".");
                return true;
            }

            // Check for cooldown.
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            // Clean up expired cooldowns to prevent memory leak.
            cleanupExpiredCooldowns(currentTime);

            if (spawnCooldowns.containsKey(playerId)) {
                long lastUsed = spawnCooldowns.get(playerId);
                long timeDiff = (currentTime - lastUsed) / 1000;
                if (timeDiff < spawnCooldown) {
                    long remainingTime = spawnCooldown - timeDiff;
                    player.sendMessage("§cYou must wait " + remainingTime + " more second" +
                        (remainingTime == 1 ? "" : "s") + " before using /spawn again.");
                    return true;
                }
            }

            Location spawnLocation = findSafeSpawnLocation(player.getWorld());
            player.teleport(spawnLocation);
            player.sendMessage("Teleported to a random spawn location!");

            spawnCooldowns.put(playerId, currentTime);
            return true;
        } else if (command.getName().equalsIgnoreCase("anarchyspawn")) {
            if (args.length == 0) {
                sender.sendMessage("Usage: /anarchyspawn reload");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("anarchyspawn.reload")) {
                    sender.sendMessage("You don't have permission to use this command!");
                    return true;
                }
                reloadConfig();
                loadConfigValues();
                sender.sendMessage("AnarchySpawn configuration reloaded.");
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("anarchyspawn") && args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("anarchyspawn.reload") && "reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            return completions;
        }
        return new ArrayList<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            Location spawnLocation = findSafeSpawnLocation(player.getWorld());
            player.teleport(spawnLocation);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.isAnchorSpawn() && !event.isBedSpawn()) {
            World overworld = event.getPlayer().getServer().getWorlds().getFirst();
            Location spawnLocation = findSafeSpawnLocation(overworld);
            event.setRespawnLocation(spawnLocation);
        }
    }

    private Location findSafeSpawnLocation(World world) {
        Location bestLocation = null;
        int bestSafetyScore = -1;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generate random coordinates within spawn radius
            int x = random.nextInt(spawnRadius * 2) - spawnRadius;
            int z = random.nextInt(spawnRadius * 2) - spawnRadius;

            // Find the highest non-air block at these coordinates
            int y = world.getHighestBlockYAt(x, z);
            Location location = new Location(world, x + 0.5, y + 1, z + 0.5);

            int safetyScore = calculateSafetyScore(location);

            // If we found a perfectly safe location, return it immediately
            if (safetyScore == 100) {
                return location;
            }

            // Keep track of the safest location we've found
            if (safetyScore > bestSafetyScore) {
                bestSafetyScore = safetyScore;
                bestLocation = location;
            }
        }

        // If we couldn't find a perfectly safe location, return the best one we found
        // If we didn't find any viable location at all, return a location at build height
        // which is not ideal but at least gives the player time to think of something.
        if (bestLocation == null) {
            int x = random.nextInt(spawnRadius * 2) - spawnRadius;
            int z = random.nextInt(spawnRadius * 2) - spawnRadius;
            bestLocation = new Location(world, x + 0.5, world.getMaxHeight() - 2, z + 0.5);
            getLogger().warning("Falling back to emergency spawn location at build height");
        }

        return bestLocation;
    }

    private int calculateSafetyScore(Location location) {
        Block feet = location.getBlock();
        Block ground = location.subtract(0, 1, 0).getBlock();
        Block head = location.add(0, 2, 0).getBlock();

        // Start with a perfect score
        int score = 100;

        // Check for basic requirements
        if (!feet.getType().isAir() || !head.getType().isAir()) {
            return 0; // Completely unsafe - can't spawn inside blocks
        }

        // Penalize based on ground conditions
        if (!ground.getType().isSolid()) {
            score -= 50;
        }
        if (ground.isLiquid()) {
            score -= 30;
        }
        if (unsafeBlocks.contains(ground.getType())) {
            score -= 40;
        }

        return Math.max(score, 0); // Don't return negative scores
    }

    private void cleanupExpiredCooldowns(long currentTime) {
        long expiredThreshold = currentTime - (spawnCooldown * 1000L);
        spawnCooldowns.entrySet().removeIf(entry -> entry.getValue() < expiredThreshold);
    }
}
