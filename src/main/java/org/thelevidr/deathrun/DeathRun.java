package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class DeathRun extends JavaPlugin {

    private ConfigManager configManager;
    private GameManager gameManager;
    private DeathRunCommand commandExecutor;
    private PlayerListener playerListener;
    private MapRecognizer mapRecognizer;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.reloadAll();

        gameManager = new GameManager(this, configManager);

        mapRecognizer = new MapRecognizer(this);
        
        for (Map.Entry<String, String> entry : configManager.getAllMapWorlds().entrySet()) {
            String mapName = entry.getKey();
            String worldName = entry.getValue();
            configManager.loadSpawnLocation(mapName);
            Location spawn = configManager.getSpawnLocation();
            mapRecognizer.registerMap(mapName, worldName, spawn);
        }

        commandExecutor = new DeathRunCommand(configManager, gameManager, mapRecognizer);
        getCommand("start").setExecutor(commandExecutor);
        getCommand("dr").setExecutor(commandExecutor);

        playerListener = new PlayerListener(configManager, gameManager, mapRecognizer);
        getServer().getPluginManager().registerEvents(playerListener, this);
    }

    @Override
    public void onDisable() {
    }
}
