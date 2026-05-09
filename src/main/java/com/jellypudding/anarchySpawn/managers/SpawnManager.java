package com.jellypudding.anarchySpawn.managers;

import com.jellypudding.anarchySpawn.AnarchySpawn;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class SpawnManager {

    private final AnarchySpawn plugin;
    private int spawnRadius;
    private int maxAttempts;
    private int spawnCooldown;
    private final Random random = new Random();
    private final Set<Material> unsafeBlocks = new HashSet<>();
    private final Map<UUID, Long> spawnCooldowns = new HashMap<>();

    public SpawnManager(AnarchySpawn plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        spawnRadius = config.getInt("spawn-radius", 300);
        maxAttempts = config.getInt("max-spawn-attempts", 75);
        spawnCooldown = config.getInt("spawn-cooldown", 5);

        unsafeBlocks.clear();
        List<String> unsafeBlocksList = config.getStringList("unsafe-blocks");
        for (String blockName : unsafeBlocksList) {
            try {
                unsafeBlocks.add(Material.valueOf(blockName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material name in config: " + blockName);
            }
        }
    }

    public Location findSafeSpawnLocation(World world) {
        Location bestLocation = null;
        int bestSafetyScore = -1;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = random.nextInt(spawnRadius * 2) - spawnRadius;
            int z = random.nextInt(spawnRadius * 2) - spawnRadius;
            int y = world.getHighestBlockYAt(x, z);
            Location location = new Location(world, x + 0.5, y + 1, z + 0.5);

            int safetyScore = calculateSafetyScore(location);

            if (safetyScore == 100) {
                return location;
            }

            if (safetyScore > bestSafetyScore) {
                bestSafetyScore = safetyScore;
                bestLocation = location;
            }
        }

        if (bestLocation == null) {
            int x = random.nextInt(spawnRadius * 2) - spawnRadius;
            int z = random.nextInt(spawnRadius * 2) - spawnRadius;
            bestLocation = new Location(world, x + 0.5, world.getMaxHeight() - 2, z + 0.5);
            plugin.getLogger().warning("Falling back to emergency spawn location at build height");
        }

        return bestLocation;
    }

    // Uses block coordinates directly to avoid mutating the Location object.
    private int calculateSafetyScore(Location location) {
        World world = location.getWorld();
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();

        Block feet   = world.getBlockAt(bx, by,     bz);
        Block ground = world.getBlockAt(bx, by - 1, bz);
        Block head   = world.getBlockAt(bx, by + 1, bz);

        if (!feet.getType().isAir() || !head.getType().isAir()) {
            return 0;
        }

        int score = 100;
        if (!ground.getType().isSolid()) score -= 50;
        if (ground.isLiquid())           score -= 30;
        if (unsafeBlocks.contains(ground.getType())) score -= 40;

        return Math.max(score, 0);
    }

    public boolean isOnCooldown(UUID playerId, long currentTime) {
        Long lastUsed = spawnCooldowns.get(playerId);
        if (lastUsed == null) return false;
        return (currentTime - lastUsed) / 1000 < spawnCooldown;
    }

    public long getRemainingCooldown(UUID playerId, long currentTime) {
        Long lastUsed = spawnCooldowns.get(playerId);
        if (lastUsed == null) return 0;
        return Math.max(0, spawnCooldown - (currentTime - lastUsed) / 1000);
    }

    public void setCooldown(UUID playerId, long currentTime) {
        spawnCooldowns.put(playerId, currentTime);
    }

    public void cleanupExpiredCooldowns(long currentTime) {
        long expiredThreshold = currentTime - (spawnCooldown * 1000L);
        spawnCooldowns.entrySet().removeIf(entry -> entry.getValue() < expiredThreshold);
    }

    public void clearCooldowns() {
        spawnCooldowns.clear();
    }
}
