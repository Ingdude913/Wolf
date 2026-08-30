package com.werewolf.game.listeners;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final WerewolfPlugin plugin;

    public PlayerDeathListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        PlayerDeathEvent deathEvent = event;
        Arena arena = plugin.getArenaManager().getArenaByPlayer(deathEvent.getEntity());
        if (arena == null) return;

        GamePlayer gp = arena.getGamePlayer(deathEvent.getEntity());
        if (gp == null || !gp.isAlive()) return;

        deathEvent.setDeathMessage(null);
        deathEvent.getDrops().clear();
        deathEvent.setKeepInventory(true);

        arena.eliminatePlayer(gp, "killed");
    }
}
