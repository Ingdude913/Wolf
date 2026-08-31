package com.werewolf.game.arena;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.util.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private final WerewolfPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final WorldManager worldManager;
    private File arenasFile;
    private FileConfiguration arenasConfig;
    private Location globalLobby;

    public ArenaManager(WerewolfPlugin plugin) {
        this.plugin = plugin;
        this.worldManager = new WorldManager(plugin);
        loadArenasFile();
        loadGlobalLobby();
    }

    public Location getGlobalLobby() {
        return globalLobby;
    }

    public void setGlobalLobby(Location loc) {
        this.globalLobby = loc;
        if (loc != null) {
            arenasConfig.set("global-lobby.world", loc.getWorld().getName());
            arenasConfig.set("global-lobby.x", loc.getX());
            arenasConfig.set("global-lobby.y", loc.getY());
            arenasConfig.set("global-lobby.z", loc.getZ());
            arenasConfig.set("global-lobby.yaw", loc.getYaw());
            arenasConfig.set("global-lobby.pitch", loc.getPitch());
        } else {
            arenasConfig.set("global-lobby", null);
        }
        saveArenasFile();
    }

    private void loadGlobalLobby() {
        ConfigurationSection section = arenasConfig.getConfigurationSection("global-lobby");
        if (section == null) return;
        String worldName = section.getString("world");
        if (worldName == null) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = worldManager.loadWorld(worldName);
        }
        if (world == null) {
            plugin.getLogger().warning("Could not load world '" + worldName + "' for global lobby.");
            return;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        globalLobby = new Location(world, x, y, z, yaw, pitch);
        plugin.getLogger().info("Loaded global lobby in world '" + worldName + "'.");
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    private void loadArenasFile() {
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            try {
                arenasFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create arenas.yml: " + e.getMessage());
            }
        }
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
    }

    public void loadArenasFromConfig() {
        if (arenasConfig == null) return;

        ConfigurationSection root = arenasConfig.getConfigurationSection("arenas");
        if (root == null) return;

        for (String arenaName : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(arenaName);
            if (section == null) continue;

            String worldName = section.getString("world");
            if (worldName == null) continue;

            World world = worldManager.loadWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Could not load world '" + worldName + "' for arena '" + arenaName + "'. Skipping.");
                continue;
            }

            Arena arena = new Arena(plugin, arenaName, worldName);

            Location lobby = deserializeLocation(section, "lobby", world);
            Location spawn = deserializeLocation(section, "spawn", world);
            if (lobby != null) arena.setLobbyLocation(lobby);
            if (spawn != null) arena.setSpawnLocation(spawn);

            arenas.put(arenaName, arena);
            plugin.getLogger().info("Loaded arena '" + arenaName + "' (world: " + worldName + ").");
        }
    }

    public void saveArena(Arena arena) {
        if (arenasConfig == null) return;

        String path = "arenas." + arena.getName();
        arenasConfig.set(path + ".world", arena.getWorldName());

        serializeLocation(arena.getLobbyLocation(), path + ".lobby");
        serializeLocation(arena.getSpawnLocation(), path + ".spawn");

        saveArenasFile();
    }

    public void removeArenaFromConfig(String arenaName) {
        if (arenasConfig == null) return;
        arenasConfig.set("arenas." + arenaName, null);
        saveArenasFile();
    }

    private void saveArenasFile() {
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save arenas.yml: " + e.getMessage());
        }
    }

    private void serializeLocation(Location loc, String path) {
        if (loc == null) {
            arenasConfig.set(path, null);
            return;
        }
        arenasConfig.set(path + ".x", loc.getX());
        arenasConfig.set(path + ".y", loc.getY());
        arenasConfig.set(path + ".z", loc.getZ());
        arenasConfig.set(path + ".yaw", loc.getYaw());
        arenasConfig.set(path + ".pitch", loc.getPitch());
    }

    private Location deserializeLocation(ConfigurationSection parent, String key, World world) {
        ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) return null;
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Arena createArena(String name, String worldName) {
        Arena arena = new Arena(plugin, name, worldName);
        arenas.put(name, arena);
        saveArena(arena);
        return arena;
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public void deleteArena(String name) {
        Arena arena = arenas.get(name);
        if (arena != null) {
            arena.forceStop();
            arenas.remove(name);
        }
        removeArenaFromConfig(name);
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public Arena getArenaByPlayer(Player player) {
        for (Arena arena : arenas.values()) {
            if (arena.isPlayerInArena(player)) {
                return arena;
            }
        }
        return null;
    }

    public boolean arenaExists(String name) {
        return arenas.containsKey(name);
    }
}
