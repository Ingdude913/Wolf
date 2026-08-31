package com.werewolf.game.util;

import com.werewolf.game.WerewolfPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

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

        File sourceFolder = new File(getWorldsFolder(), worldName);
        File serverWorldContainer = Bukkit.getWorldContainer();
        File targetFolder = new File(serverWorldContainer, worldName);

        try {
            deleteRecursive(targetFolder.toPath());
            File migratedRoot = new File(serverWorldContainer, "world");
            File migratedDimension = new File(migratedRoot, "dimensions/minecraft/" + worldName);
            deleteRecursive(migratedDimension.toPath());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not clean stale world '" + worldName + "': " + e.getMessage());
            return null;
        }

        try {
            copyFolder(sourceFolder.toPath(), targetFolder.toPath());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not copy world '" + worldName + "': " + e.getMessage());
            return null;
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        world = creator.createWorld();

        if (world != null) {
            plugin.getLogger().info("Loaded world '" + worldName + "' from " + sourceFolder.getPath());
        }

        return world;
    }

    public World getOrLoadWorld(String worldName) {
        return loadWorld(worldName);
    }

    private void copyFolder(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(dir);
                Path dest = target.resolve(relative);
                Files.createDirectories(dest);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Path dest = target.resolve(relative);
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
