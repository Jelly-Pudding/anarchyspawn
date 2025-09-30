
package com.jellypudding.anarchySpawn;

import org.bukkit.Bukkit;
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

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class AnarchySpawn extends JavaPlugin implements Listener {

    private int spawnRadius;
    private int maxSpawnAttempts;
    private int spawnCooldown;
    private Set<Material> unsafeBlocks;
    private final Map<UUID, Long> spawnCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("AnarchySpawn (Folia) enabled");
    }

    @Override
    public void onDisable() {
        spawnCooldowns.clear();
    }

    private void reloadLocalConfig() {
        FileConfiguration cfg = getConfig();
        this.spawnRadius = Math.max(16, cfg.getInt("spawn-radius", 300));
        this.maxSpawnAttempts = Math.max(1, cfg.getInt("max-spawn-attempts", 75));
        this.spawnCooldown = Math.max(0, cfg.getInt("spawn-cooldown", 5));

        List<String> list = cfg.getStringList("unsafe-blocks");
        Set<Material> mats = new HashSet<>();
        for (String s : list) {
            try {
                mats.add(Material.valueOf(s.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
        // add a few sensible defaults if none provided
        if (mats.isEmpty()) {
            mats.add(Material.LAVA);
            mats.add(Material.FIRE);
            mats.add(Material.CAMPFIRE);
            mats.add(Material.SOUL_CAMPFIRE);
            mats.add(Material.MAGMA_BLOCK);
            mats.add(Material.CACTUS);
            mats.add(Material.POWDER_SNOW);
            mats.add(Material.SWEET_BERRY_BUSH);
        }
        this.unsafeBlocks = Collections.unmodifiableSet(mats);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPlayedBefore()) {
            teleportToRandomSafe(p, result -> {
                if (!result) p.sendMessage("не вдалося знайти безпечний спавн :(");
            });
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (e.isBedSpawn() || e.isAnchorSpawn()) return;
        Player p = e.getPlayer();
        p.getScheduler().execute(this, () -> teleportToRandomSafe(p, ok -> {}), null, 1L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        if (name.equals("anarchyspawn")) {
            if (!(sender.hasPermission("anarchyspawn.reload"))) {
                sender.sendMessage("у вас немає прав");
                return true;
            }
            reloadConfig();
            reloadLocalConfig();
            sender.sendMessage("конфіг перезавантажено");
            return true;
        }
        if (name.equals("spawn")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("тільки для гравців");
                return true;
            }
            if (!p.hasPermission("anarchyspawn.spawn")) {
                p.sendMessage("у вас немає прав");
                return true;
            }
            long now = System.currentTimeMillis();
            cleanupExpiredCooldowns(now);
            Long last = spawnCooldowns.get(p.getUniqueId());
            if (last != null && now - last < (spawnCooldown * 1000L)) {
                long leftSec = Math.max(0L, (spawnCooldown * 1000L - (now - last)) / 1000L);
                p.sendMessage("зачекайте " + leftSec + " с");
                return true;
            }
            spawnCooldowns.put(p.getUniqueId(), now);
            teleportToRandomSafe(p, result -> {
                if (!result) p.sendMessage("не вдалося знайти безпечний спавн :(");
            });
            return true;
        }
        return false;
    }

    private void teleportToRandomSafe(Player p, java.util.function.Consumer<Boolean> callback) {
        if (!p.isOnline()) { callback.accept(false); return; }
        World world = p.getWorld();
        AtomicInteger attempt = new AtomicInteger(0);
        tryAttempt(p, world, attempt, callback);
    }

    private void tryAttempt(Player p, World world, AtomicInteger attempt, java.util.function.Consumer<Boolean> callback) {
        if (!p.isOnline()) { callback.accept(false); return; }
        if (attempt.incrementAndGet() > maxSpawnAttempts) { callback.accept(false); return; }

        int x = ThreadLocalRandom.current().nextInt(-spawnRadius, spawnRadius + 1);
        int z = ThreadLocalRandom.current().nextInt(-spawnRadius, spawnRadius + 1);
        final int cx = x >> 4;
        final int cz = z >> 4;

        world.getChunkAtAsyncUrgently(cx, cz).thenAccept(chunk -> {
            Bukkit.getRegionScheduler().execute(this, world, cx, cz, () -> {
                int yTop = world.getHighestBlockYAt(x, z);
                int yScan = Math.min(yTop, world.getMaxHeight() - 2);

                boolean found = false;
                int safeY = 0;

                while (yScan > world.getMinHeight()) {
                    Block ground = world.getBlockAt(x, yScan, z);
                    Material gm = ground.getType();
                    if (gm.isSolid() && !unsafeBlocks.contains(gm)) {
                        Block feet = world.getBlockAt(x, yScan + 1, z);
                        Block head = world.getBlockAt(x, yScan + 2, z);
                        if (isAiry(feet) && isAiry(head)) {
                            found = true;
                            safeY = yScan + 1;
                            break;
                        }
                    }
                    yScan--;
                }

                if (!found) {
                    Bukkit.getGlobalRegionScheduler().execute(this, () -> tryAttempt(p, world, attempt, callback));
                    return;
                }

                Location loc = new Location(world, x + 0.5, safeY, z + 0.5);
                p.getScheduler().execute(this, () -> {
                    if (!p.isOnline()) { callback.accept(false); return; }
                    p.teleportAsync(loc).thenAccept(success -> {
                        if (!success) {
                            Bukkit.getGlobalRegionScheduler().execute(this, () -> tryAttempt(p, world, attempt, callback));
                        } else {
                            callback.accept(true);
                        }
                    });
                }, null, 0L);
            });
        }).exceptionally(ex -> {
            Bukkit.getGlobalRegionScheduler().execute(this, () -> tryAttempt(p, world, attempt, callback));
            return null;
        });
    }

    private boolean isAiry(Block b) {
        Material m = b.getType();
        if (m == Material.AIR || b.isPassable()) {
            if (m == Material.WATER || m == Material.LAVA || m == Material.POWDER_SNOW) return false;
            return true;
        }
        return false;
    }

    private void cleanupExpiredCooldowns(long now) {
        long threshold = now - (spawnCooldown * 1000L);
        spawnCooldowns.entrySet().removeIf(e -> e.getValue() < threshold);
    }
}
