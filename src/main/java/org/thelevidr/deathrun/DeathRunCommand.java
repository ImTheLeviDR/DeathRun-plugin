package org.thelevidr.deathrun;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeathRunCommand implements CommandExecutor {
    private final ConfigManager configManager;
    private final GameManager gameManager;
    private final MapRecognizer mapRecognizer;
    private PersonalBestManager pbManager;

    public DeathRunCommand(ConfigManager configManager, GameManager gameManager, MapRecognizer mapRecognizer) {
        this(configManager, gameManager, mapRecognizer, null);
    }

    public DeathRunCommand(ConfigManager configManager, GameManager gameManager, MapRecognizer mapRecognizer, PersonalBestManager pbManager) {
        this.configManager = configManager;
        this.gameManager = gameManager;
        this.mapRecognizer = mapRecognizer;
        this.pbManager = pbManager;
    }

    public void setPbManager(PersonalBestManager pbManager) {
        this.pbManager = pbManager;
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

        if (args[0].equalsIgnoreCase("pb")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            return handlePbCommand((Player) sender, args);
        }

        sender.sendMessage("§cUnknown subcommand. Use: reload, pb");
        return true;
    }

    private boolean handlePbCommand(Player player, String[] args) {
        if (pbManager == null) {
            player.sendMessage("§cPersonal best system not available.");
            return true;
        }

        String mapName;
        if (args.length > 1) {
            mapName = args[1];
            if (!configManager.hasMap(mapName)) {
                player.sendMessage("§cMap not found: " + mapName);
                return true;
            }
        } else {
            String worldName = player.getWorld().getName();
            mapName = mapRecognizer.getMapName(worldName);
            if (mapName == null) {
                player.sendMessage("§cCould not detect map. Please specify: /dr pb <map>");
                return true;
            }
        }

        Long pb = pbManager.getPersonalBest(mapName, player.getUniqueId());
        if (pb == null) {
            player.sendMessage("§eMap: §a" + mapName + " §c- No personal best yet!");
        } else {
            player.sendMessage("§eMap: §a" + mapName + " §aPersonal Best: §e" + pbManager.formatTime(pb));
        }
        return true;
    }

    private boolean handleStartCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            String worldName = player.getWorld().getName();
            String mapName = mapRecognizer.getMapName(worldName);
            
            if (mapName == null) {
                player.sendMessage("§cCould not detect map. Please specify: /start <map name>");
                return true;
            }
            
            if (!configManager.hasGlassLocations(mapName)) {
                player.sendMessage("§cMap '" + mapName + "' does not have glass locations defined.");
                return true;
            }
            
            gameManager.startGame(mapName);
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
