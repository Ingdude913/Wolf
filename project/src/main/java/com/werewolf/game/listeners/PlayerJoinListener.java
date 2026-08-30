package com.werewolf.game.listeners;

import com.werewolf.game.WerewolfPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final WerewolfPlugin plugin;

    public PlayerJoinListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(plugin.prefix() + "Welcome! Use /werewolf help to see commands.");
    }
}
