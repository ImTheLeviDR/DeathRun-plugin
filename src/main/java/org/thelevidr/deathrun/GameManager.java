package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GameManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private PersonalBestManager pbManager;
    private StrafeManager strafeManager;
    private long gameStartTime = 0;
    private boolean isGameRunning = false;
    private Location glassOrigin;
    private int glassConstant;
    private int glassParam1;
    private int glassParam2;
    private String currentMapName;
    private int actionBarTaskId = -1;

    public GameManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void setPbManager(PersonalBestManager pbManager) {
        this.pbManager = pbManager;
    }

    public void setStrafeManager(StrafeManager strafeManager) {
        this.strafeManager = strafeManager;
    }

    public StrafeManager getStrafeManager() {
        return strafeManager;
    }

    public void startGame(String mapName) {
        this.currentMapName = mapName;
        String path = "map." + mapName;

        configManager.loadMapData(mapName);
        
        glassOrigin = configManager.getGlassOrigin();
        glassConstant = configManager.getGlassConstant();
        glassParam1 = configManager.getGlassParam1();
        glassParam2 = configManager.getGlassParam2();

        if (glassOrigin == null) return;

        setBlocksByConstant(glassOrigin, glassConstant, glassParam1, glassParam2, Material.getMaterial("STAINED_GLASS"), (byte) 5);

        startCountdown();
    }

    public void reload() {
        gameStartTime = 0;
        isGameRunning = false;
        stopActionBarTimer();
    }

    private void startActionBarTimer() {
        if (actionBarTaskId != -1) return;
        actionBarTaskId = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!isGameRunning || gameStartTime == 0) return;
            long elapsed = System.currentTimeMillis() - gameStartTime;
            long minutes = (elapsed / 60000) % 60;
            long seconds = (elapsed / 1000) % 60;
            long millis = elapsed % 1000;
            String timeStr = String.format("%02d:%02d:%03d", minutes, seconds, millis);
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendActionBar("§e" + timeStr);
            }
        }, 0L, 1L).getTaskId();
    }

    public void stopActionBarTimerPublic() {
        if (actionBarTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(actionBarTaskId);
            actionBarTaskId = -1;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendActionBar("");
        }
    }

    private void stopActionBarTimer() {
        stopActionBarTimerPublic();
    }

    private void startCountdown() {
        // We do all the heavy lifting (teleports) first
        Location spawn = configManager.getSpawnLocation();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (spawn != null && player.getLocation().distance(spawn) > 10) {
                player.teleport(spawn);
            }
        }

        // Capture the time AFTER the laggy teleports
        final long countdownStartTime = System.currentTimeMillis();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§a➌", "§7seconds to start!", 0, 21, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.5F);
        }

        final int[] taskIdHolder = new int[1];
        final boolean[] twoSent = {false};
        final boolean[] oneSent = {false};

        taskIdHolder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long elapsed = System.currentTimeMillis() - countdownStartTime;

            if (elapsed >= 1000 && !twoSent[0]) {
                twoSent[0] = true;
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendTitle("§6➋", "§7seconds to start!", 0, 21, 0);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.75F);
                }
            } else if (elapsed >= 2000 && !oneSent[0]) {
                oneSent[0] = true;
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendTitle("§e➊", "§7seconds to start!", 0, 21, 0);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                }
            } else if (elapsed >= 3000) {
                // Cancel task
                plugin.getServer().getScheduler().cancelTask(taskIdHolder[0]);

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    // Hardcode 3000ms visual, or just say "GO!"
                    player.sendTitle("§aRUN!", "", 0, 20, 0);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_AMBIENT, 1.0F, 1.0F);
                    if (strafeManager != null) {
                        strafeManager.giveStrafes(player);
                    }
                }
                if (glassOrigin != null) {
                    setBlocksByConstant(glassOrigin, glassConstant, glassParam1, glassParam2, Material.AIR, (byte) 0);
                }

                // Set start time to exactly NOW so the race time is 100% accurate
                gameStartTime = System.currentTimeMillis();
                isGameRunning = true;
                startActionBarTimer();
            }
        }, 1L, 1L).getTaskId();
    }

    private void setBlocksInRegion(Location loc1, Location loc2, Material material, byte data) {
        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = loc1.getWorld().getBlockAt(x, y, z);
                    block.setType(material);
                    if (material != Material.AIR) {
                        block.setData(data);
                    }
                }
            }
        }
    }

    private void setBlocksByConstant(Location origin, int constant, int param1, int param2, Material material, byte data) {
        if (origin == null) return;
        
        int x = origin.getBlockX();
        int y = origin.getBlockY();
        int z = origin.getBlockZ();
        World world = origin.getWorld();

        boolean addingGlass = (material != Material.AIR);

        for (int i = 0; i < param1; i++) {
            for (int j = 0; j < param2; j++) {
                int bx = x, by = y, bz = z;
                switch (constant) {
                    case 1:
                        by = y + i;
                        bz = z + j;
                        break;
                    case 2:
                        bx = x + i;
                        bz = z + j;
                        break;
                    case 3:
                        bx = x + i;
                        by = y + j;
                        break;
                }
                Block block = world.getBlockAt(bx, by, bz);
                
                if (addingGlass) {
                    if (block.getType() == Material.AIR || block.getType() == Material.STAINED_GLASS) {
                        block.setType(material);
                        block.setData(data);
                    }
                } else {
                    if (block.getType() == Material.STAINED_GLASS) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    public boolean checkFinish(Player player) {
        if (!isGameRunning) return false;

        if (!configManager.hasFinishRegion()) return false;

        Location playerLoc = player.getLocation();
        
        if (!configManager.isInFinishRegion(playerLoc)) return false;

        int x = playerLoc.getBlockX();
        int y = playerLoc.getBlockY();
        int z = playerLoc.getBlockZ();
        
        Block blockAt = playerLoc.getWorld().getBlockAt(x, y, z);
        Block blockBelow = playerLoc.getWorld().getBlockAt(x, y - 1, z);

        if (blockAt.getType() == Material.PORTAL || blockBelow.getType() == Material.PORTAL) {
            finishPlayer(player);
            return true;
        }
        return false;
    }

    private void finishPlayer(Player player) {
        long elapsed = System.currentTimeMillis() - gameStartTime;
        long minutes = (elapsed / 60000) % 60;
        long seconds = (elapsed / 1000) % 60;
        long millis = elapsed % 1000;

        Location spawnLocation = configManager.getSpawnLocation();
        if (spawnLocation != null) {
            player.teleport(spawnLocation);
        }
        
        if (strafeManager != null) {
            strafeManager.removeStrafes(player);
        }

        boolean isNewPb = false;
        if (pbManager != null && currentMapName != null) {
            Long existingPb = pbManager.getPersonalBest(currentMapName, player.getUniqueId());
            if (existingPb == null || elapsed < existingPb) {
                isNewPb = true;
                pbManager.setPersonalBest(currentMapName, player.getUniqueId(), elapsed);
            }
        }

        String timeStr = String.format("%02d:%02d:%03d", minutes, seconds, millis);
        if (isNewPb) {
            player.sendMessage("§aFinish! Time: §e" + timeStr + " §a(NEW PERSONAL BEST!)");
        } else {
            player.sendMessage("§aFinish! Time: §e" + timeStr);
        }

        gameStartTime = 0;
        isGameRunning = false;
        currentMapName = null;
        stopActionBarTimer();
    }

    public boolean isGameRunning() {
        return isGameRunning;
    }
}
