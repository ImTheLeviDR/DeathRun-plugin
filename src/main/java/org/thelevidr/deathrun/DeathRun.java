package org.thelevidr.deathrun;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class DeathRun extends JavaPlugin implements CommandExecutor, Listener {

    private FileConfiguration config;
    private List<EffectPad> effectPads = new ArrayList<>();

    @Override
    public void onEnable() {
        loadConfig();
        loadAllEffectPads();
        getCommand("start").setExecutor(this);
        getCommand("dr").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadAllEffectPads() {
        effectPads.clear();
        if (!config.contains("map")) return;

        for (String mapName : config.getConfigurationSection("map").getKeys(false)) {
            String path = "map." + mapName;
            loadEffectPads(path);
        }
    }

    public void loadConfig() {
        File optionsFile = new File(getDataFolder(), "options.yml");
        if (!optionsFile.exists()) {
            saveResource("options.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(optionsFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("dr")) {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /dr reload");
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                loadConfig();
                loadAllEffectPads();
                sender.sendMessage("§aConfig reloaded!");
                return true;
            }
            sender.sendMessage("§cUnknown subcommand. Use: reload");
            return true;
        }

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
        String path = "map." + mapName;

        if (!config.contains(path + ".name")) {
            player.sendMessage("§cMap not found: " + mapName);
            getLogger().warning("Map not found: " + path + ".name");
            return true;
        }

        getLogger().info("Found map " + mapName + ", loading effects...");
        ConfigurationSection glass1 = config.getConfigurationSection(path + ".glass.1");
        ConfigurationSection glass2 = config.getConfigurationSection(path + ".glass.2");

        if (glass1 == null || glass2 == null) {
            player.sendMessage("§cMap does not have glass locations defined.");
            return true;
        }

        Location loc1 = getLocationFromSection(glass1);
        Location loc2 = getLocationFromSection(glass2);

        if (loc1 == null || loc2 == null) {
            player.sendMessage("§cInvalid location format in config.");
            return true;
        }

        setBlocksInRegion(loc1, loc2, Material.getMaterial("STAINED_GLASS"), (byte) 5);

        effectPads.clear();
        loadEffectPads(path);
        getLogger().info("Loaded " + effectPads.size() + " effect pads for map " + mapName);
        getLogger().info("Loaded " + effectPads.size() + " effect pads for map " + mapName);

        startCountdown(loc1, loc2);
        return true;
    }

    private void loadEffectPads(String path) {
        ConfigurationSection effects = config.getConfigurationSection(path + ".effects");
        if (effects == null) return;

        for (String effectType : effects.getKeys(false)) {
            ConfigurationSection effectSection = effects.getConfigurationSection(effectType);
            if (effectSection == null) continue;

            PotionEffectType potionType = effectType.equalsIgnoreCase("speed") ? PotionEffectType.SPEED :
                    effectType.equalsIgnoreCase("jump") ? PotionEffectType.JUMP : null;

            if (potionType == null) continue;

            for (String key : effectSection.getKeys(false)) {
                ConfigurationSection padSection = effectSection.getConfigurationSection(key);
                if (padSection == null) continue;

                ConfigurationSection pos1 = padSection.getConfigurationSection("pos.1");
                ConfigurationSection pos2 = padSection.getConfigurationSection("pos.2");
                if (pos1 == null || pos2 == null) continue;

                Location padLoc1 = getLocationFromSection(pos1);
                Location padLoc2 = getLocationFromSection(pos2);
                int amplifier = padSection.getInt("amplifier", 1);
                int duration = padSection.getInt("duration", 2);

                if (padLoc1 != null && padLoc2 != null) {
                    effectPads.add(new EffectPad(padLoc1, padLoc2, potionType, amplifier, duration));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        if (effectPads.isEmpty()) return;

        Player player = event.getPlayer();
        Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();

        for (EffectPad pad : effectPads) {
            if (pad.contains(blockBelow)) {
                if (!player.hasPotionEffect(pad.type)) {
                    player.addPotionEffect(new PotionEffect(pad.type, pad.duration * 20, pad.amplifier - 1), true);
                }
                break;
            }
        }
    }

    private class EffectPad {
        Location min, max;
        PotionEffectType type;
        int amplifier, duration;

        EffectPad(Location loc1, Location loc2, PotionEffectType type, int amplifier, int duration) {
            this.min = new Location(loc1.getWorld(),
                    Math.min(loc1.getBlockX(), loc2.getBlockX()),
                    Math.min(loc1.getBlockY(), loc2.getBlockY()),
                    Math.min(loc1.getBlockZ(), loc2.getBlockZ()));
            this.max = new Location(loc1.getWorld(),
                    Math.max(loc1.getBlockX(), loc2.getBlockX()),
                    Math.max(loc1.getBlockY(), loc2.getBlockY()),
                    Math.max(loc1.getBlockZ(), loc2.getBlockZ()));
            this.type = type;
            this.amplifier = amplifier;
            this.duration = duration;
        }

        boolean contains(Block block) {
            int x = block.getX(), y = block.getY(), z = block.getZ();
            return block.getWorld().equals(min.getWorld()) &&
                    x >= min.getBlockX() && x <= max.getBlockX() &&
                    y >= min.getBlockY() && y <= max.getBlockY() &&
                    z >= min.getBlockZ() && z <= max.getBlockZ();
        }
    }

    private Location getLocationFromSection(ConfigurationSection section) {
        int x = section.getInt("x");
        int y = section.getInt("y");
        int z = section.getInt("z");
        String worldName = section.getString("world");
        World world = getServer().getWorld(worldName);
        if (world != null) {
            return new Location(world, x, y, z);
        }
        return null;
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

    private void startCountdown(Location loc1, Location loc2) {
        for (Player player : getServer().getOnlinePlayers()) {
            player.sendTitle("§a3", "", 0, 20, 0);
        }

        getServer().getScheduler().runTaskLater(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                player.sendTitle("§62", "", 0, 20, 0);
            }
            getServer().getScheduler().runTaskLater(this, () -> {
                for (Player player : getServer().getOnlinePlayers()) {
                    player.sendTitle("§c1", "", 0, 20, 0);
                }
                getServer().getScheduler().runTaskLater(this, () -> {
                    for (Player player : getServer().getOnlinePlayers()) {
                        player.sendTitle("§aRUN!", "", 0, 20, 0);
                    }
                    setBlocksInRegion(loc1, loc2, Material.AIR, (byte) 0);
                    effectPads.clear();
                }, 20L);
            }, 20L);
        }, 20L);
    }

    @Override
    public void onDisable() {
    }
}