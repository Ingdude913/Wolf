package com.werewolf.game.listeners;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.game.Phase;
import com.werewolf.game.gui.NinjaGUI;
import com.werewolf.game.gui.RoleSelectorGUI;
import com.werewolf.game.gui.SeerGUI;
import com.werewolf.game.gui.SheriffGUI;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    private final WerewolfPlugin plugin;

    public PlayerInteractListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena == null) return;

        GamePlayer gp = arena.getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (ItemBuilder.isItemKey(plugin, event.getItem(), "role-selector")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.LOBBY) {
                    arena.openRoleSelector(player);
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "vote-sheriff")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.SHERIFF_ELECTION) {
                    SheriffGUI.open(plugin, arena, player);
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "revoke-vote")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.DAY) {
                    arena.revokeVote(player);
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "skip-day")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.DAY) {
                    arena.skipDay(player);
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "seer-book")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.NIGHT) {
                    SeerGUI.open(plugin, arena, player);
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "werewolf-armor")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.NIGHT) {
                    arena.werewolfTransform(player);
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "hunter-target")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.NIGHT) {
                    player.sendMessage(plugin.prefix() + ChatColor.GOLD + "Right-click a player to select your target.");
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "witch-poison")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.NIGHT) {
                    player.sendMessage(plugin.prefix() + ChatColor.DARK_PURPLE + "Right-click a player to poison them.");
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "witch-heal")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.NIGHT) {
                    player.sendMessage(plugin.prefix() + ChatColor.GREEN + "Right-click a player to heal them.");
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "role-info-book")) {
                event.setCancelled(true);
                player.sendMessage(plugin.prefix() + ChatColor.GOLD + "Your role: " + ChatColor.WHITE + gp.getRole().getName());
                player.sendMessage(plugin.prefix() + ChatColor.GRAY + gp.getRole().getDescription());
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "setup-info")) {
                event.setCancelled(true);
                arena.sendSetupInfo(player);
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "ninja-book")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.NIGHT) {
                    NinjaGUI.open(player);
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "ninja-ability")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.NIGHT) {
                    arena.ninjaExecuteAbility(player);
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "wolf-team")) {
                event.setCancelled(true);
                if (gp.getRole().isWerewolf()) {
                    arena.sendWolfTeamInfo(player);
                }
                return;
            }

            if (ItemBuilder.isItemKey(plugin, event.getItem(), "mermaid-shell")) {
                event.setCancelled(true);
                if (arena.getPhase() == Phase.NIGHT) {
                    arena.mermaidSing(player);
                }
                return;
            }
        }
    }
}
