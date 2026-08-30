package com.werewolf.game.arena;

import com.werewolf.game.WerewolfPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private final WerewolfPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    public Arena createArena(String name) {
        Arena arena = new Arena(plugin, name);
        arenas.put(name, arena);
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
