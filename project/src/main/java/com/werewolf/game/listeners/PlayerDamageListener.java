package com.werewolf.game.listeners;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.game.Phase;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerDamageListener implements Listener {

    private final WerewolfPlugin plugin;

    public PlayerDamageListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena == null) return;

        GamePlayer gp = arena.getGamePlayer(player);
        if (gp == null) return;

        if (arena.getPhase() == Phase.LOBBY || arena.getPhase() == Phase.ENDED) {
            event.setCancelled(true);
            return;
        }

        if (!gp.isAlive()) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL ||
                event.getCause() == EntityDamageEvent.DamageCause.DROWNING ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
                event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION ||
                event.getCause() == EntityDamageEvent.DamageCause.STARVATION) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player target = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        Arena arena = plugin.getArenaManager().getArenaByPlayer(target);
        if (arena == null) return;

        GamePlayer targetGp = arena.getGamePlayer(target);
        GamePlayer attackerGp = arena.getGamePlayer(attacker);
        if (targetGp == null || attackerGp == null) {
            return;
        }

        if (arena.getPhase() == Phase.LOBBY || arena.getPhase() == Phase.ENDED) {
            event.setCancelled(true);
            return;
        }

        if (!targetGp.isAlive() || !attackerGp.isAlive()) {
            event.setCancelled(true);
            return;
        }

        if (arena.getPhase() == Phase.DAY) {
            if (ItemBuilder.isItemKey(plugin, attacker.getInventory().getItemInMainHand(), "vote-sword")) {
                event.setCancelled(true);
                arena.castVote(attacker, target);
                return;
            }
            event.setCancelled(true);
            return;
        }

        if (arena.getPhase() == Phase.NIGHT) {
            if (ItemBuilder.isItemKey(plugin, attacker.getInventory().getItemInMainHand(), "werewolf-axe")) {
                if (attackerGp.getRole().isWerewolf()) {
                    event.setCancelled(false);
                    arena.werewolfKill(attacker, target);
                    return;
                } else if (attackerGp.getRole().isLier()) {
                    event.setCancelled(true);
                    attacker.sendMessage(plugin.prefix() + ChatColor.RED + "Your axe is fake! It cannot kill.");
                    return;
                }
            }
            if (ItemBuilder.isItemKey(plugin, attacker.getInventory().getItemInMainHand(), "witch-poison")) {
                event.setCancelled(true);
                arena.witchPoison(attacker, target);
                return;
            }
            if (ItemBuilder.isItemKey(plugin, attacker.getInventory().getItemInMainHand(), "witch-heal")) {
                event.setCancelled(true);
                arena.witchHeal(attacker, target);
                return;
            }
            if (ItemBuilder.isItemKey(plugin, attacker.getInventory().getItemInMainHand(), "hunter-target")) {
                event.setCancelled(true);
                arena.hunterSelectTarget(attacker, target);
                return;
            }
            event.setCancelled(true);
        }
    }
}
