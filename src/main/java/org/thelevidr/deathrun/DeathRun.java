package org.thelevidr.deathrun;

import org.bukkit.plugin.java.JavaPlugin;

public final class DeathRun extends JavaPlugin {

    private ConfigManager configManager;
    private GameManager gameManager;
    private DeathRunCommand commandExecutor;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.reloadAll();

        gameManager = new GameManager(this, configManager);

        commandExecutor = new DeathRunCommand(configManager, gameManager);
        getCommand("start").setExecutor(commandExecutor);
        getCommand("dr").setExecutor(commandExecutor);

        playerListener = new PlayerListener(configManager, gameManager);
        getServer().getPluginManager().registerEvents(playerListener, this);
    }

    @Override
    public void onDisable() {
    }
}
