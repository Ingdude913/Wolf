package com.werewolf.game.util;

import com.werewolf.game.WerewolfPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;

public class WorldManager {

    private final WerewolfPlugin plugin;

    public WorldManager(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    public File getWorldsFolder() {
        File folder = new File(plugin.getDataFolder(), "World");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public boolean worldFolderExists(String worldName) {
        File worldFolder = new File(getWorldsFolder(), worldName);
        return worldFolder.exists() && worldFolder.isDirectory();
    }

    public World loadWorld(String worldName) {
        if (!worldFolderExists(worldName)) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }

        File worldFolder = new File(getWorldsFolder(), worldName);
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        world = creator.createWorld();

        if (world != null) {
            plugin.getLogger().info("Loaded world '" + worldName + "' from " + worldFolder.getPath());
        }

        return world;
    }

    public World getOrLoadWorld(String worldName) {
        return loadWorld(worldName);
    }
}
