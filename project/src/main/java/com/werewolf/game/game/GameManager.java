package com.werewolf.game.game;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;

public class GameManager {

    private final WerewolfPlugin plugin;

    public GameManager(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    public WerewolfPlugin getPlugin() {
        return plugin;
    }
}
