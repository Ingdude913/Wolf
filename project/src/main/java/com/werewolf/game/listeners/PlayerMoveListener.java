package com.werewolf.game.listeners;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.game.Phase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final WerewolfPlugin plugin;

    public PlayerMoveListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena == null) return;
        if (arena.getPhase() != Phase.NIGHT) return;

        GamePlayer gp = arena.getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;
        if (!gp.getRole().isWerewolf()) return;
        if (!arena.isMermaidFreezeActive()) return;

        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setTo(event.getFrom());
        }
    }
}
