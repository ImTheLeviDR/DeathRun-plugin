package org.thelevidr.deathrun;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeathRunCommand implements CommandExecutor {
    private final ConfigManager configManager;
    private final GameManager gameManager;

    public DeathRunCommand(ConfigManager configManager, GameManager gameManager) {
        this.configManager = configManager;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("dr")) {
            return handleDrCommand(sender, args);
        }

        if (label.equalsIgnoreCase("start")) {
            return handleStartCommand(sender, args);
        }

        return false;
    }

    private boolean handleDrCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /dr reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            configManager.reloadAll();

            if (!configManager.hasValidMap()) {
                sender.sendMessage("§cError: No finish or glass coordinates set in config.");
                return true;
            }

            sender.sendMessage("§aConfig reloaded!");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Use: reload");
        return true;
    }

    private boolean handleStartCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§cPlease specify the map. /start <map name>");
            return true;
        }

        String mapName = args[0];

        if (!configManager.hasMap(mapName)) {
            player.sendMessage("§cMap not found: " + mapName);
            return true;
        }

        if (!configManager.hasGlassLocations(mapName)) {
            player.sendMessage("§cMap does not have glass locations defined.");
            return true;
        }

        gameManager.startGame(mapName);
        return true;
    }
}
