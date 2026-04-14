package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public final class DeathRun extends JavaPlugin {

    private ConfigManager configManager;
    private GameManager gameManager;
    private DeathRunCommand commandExecutor;
    private PlayerListener playerListener;
    private MapRecognizer mapRecognizer;
    private PersonalBestManager pbManager;
    private StrafeManager strafeManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.reloadAll();

        gameManager = new GameManager(this, configManager);

        mapRecognizer = new MapRecognizer(this);
        
        pbManager = new PersonalBestManager(this);
        
        for (Map.Entry<String, String> entry : configManager.getAllMapWorlds().entrySet()) {
            String mapName = entry.getKey();
            String worldName = entry.getValue();
            configManager.loadSpawnLocation(mapName);
            Location spawn = configManager.getSpawnLocation();
            mapRecognizer.registerMap(mapName, worldName, spawn);
        }

        commandExecutor = new DeathRunCommand(configManager, gameManager, mapRecognizer, pbManager);
        gameManager.setPbManager(pbManager);
        
        strafeManager = new StrafeManager(this);
        getServer().getPluginManager().registerEvents(strafeManager, this);
        gameManager.setStrafeManager(strafeManager);

        getCommand("start").setExecutor(commandExecutor);
        getCommand("dr").setExecutor(commandExecutor);

        playerListener = new PlayerListener(configManager, gameManager, mapRecognizer);
        getServer().getPluginManager().registerEvents(playerListener, this);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    player.setMaxHealth(20.0);
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    @Override
    public void onDisable() {
    }
}
