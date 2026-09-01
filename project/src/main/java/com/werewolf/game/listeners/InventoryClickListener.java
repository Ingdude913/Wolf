package com.werewolf.game.listeners;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.game.Phase;
import com.werewolf.game.gui.NinjaGUI;
import com.werewolf.game.gui.RoleSelectorGUI;
import com.werewolf.game.gui.SeerGUI;
import com.werewolf.game.gui.SheriffGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class InventoryClickListener implements Listener {

    private final WerewolfPlugin plugin;

    public InventoryClickListener(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);

        Inventory inv = event.getView().getTopInventory();
        if (inv == null) return;

        String title = event.getView().getTitle();
        if (SeerGUI.isSeerGUI(player, title)) {
            event.setCancelled(true);

            if (arena == null) return;

            GamePlayer seerGp = arena.getGamePlayer(player);
            if (seerGp == null || !seerGp.isAlive()) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            Player target = SeerGUI.getPlayerAtSlot(player, slot);
            if (target == null) return;

            arena.seerCheck(player, target);
            player.closeInventory();
        } else if (NinjaGUI.isNinjaGUI(title)) {
            event.setCancelled(true);

            if (arena == null) return;

            GamePlayer ninjaGp = arena.getGamePlayer(player);
            if (ninjaGp == null || !ninjaGp.isAlive()) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            String ability = NinjaGUI.getAbilityAtSlot(player, slot);
            if (ability == null) return;

            arena.ninjaSelectAbility(player, ability);
            player.closeInventory();
        } else if (RoleSelectorGUI.isRoleSelectorGUI(title)) {
            event.setCancelled(true);

            if (arena == null) return;
            if (arena.getPhase() != Phase.LOBBY) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            String roleKey = RoleSelectorGUI.getRoleAtSlot(player, slot);
            if (roleKey == null) return;

            if (roleKey.equals("sheriff-toggle")) {
                boolean newState = !arena.isSheriffEnabled();
                arena.setSheriffEnabled(newState);
                player.sendMessage(plugin.prefix() + ChatColor.GOLD + "Sheriff Election: " + (newState ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                RoleSelectorGUI.update(player, arena.getRoleSelection(), newState);
                return;
            }

            if (event.getClick() == ClickType.LEFT) {
                arena.adjustRoleSelection(roleKey, 1);
                player.sendMessage(plugin.prefix() + ChatColor.GREEN + "Added one " + roleKey + "! Total: " + arena.getRoleSelection().getOrDefault(roleKey, 0));
                RoleSelectorGUI.update(player, arena.getRoleSelection(), arena.isSheriffEnabled());
            } else if (event.getClick() == ClickType.RIGHT) {
                arena.adjustRoleSelection(roleKey, -1);
                player.sendMessage(plugin.prefix() + ChatColor.RED + "Removed one " + roleKey + "! Total: " + arena.getRoleSelection().getOrDefault(roleKey, 0));
                RoleSelectorGUI.update(player, arena.getRoleSelection(), arena.isSheriffEnabled());
            }
        } else if (SheriffGUI.isSheriffGUI(title)) {
            event.setCancelled(true);

            if (arena == null) return;

            GamePlayer voterGp = arena.getGamePlayer(player);
            if (voterGp == null || !voterGp.isAlive()) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            Player target = SheriffGUI.getPlayerAtSlot(player, slot);
            if (target == null) return;

            arena.castSheriffVote(player, target);
            player.closeInventory();
        } else if (arena != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        if (SeerGUI.isSeerGUI(player, title)) {
            SeerGUI.clearMapping(player);
        } else if (NinjaGUI.isNinjaGUI(title)) {
            NinjaGUI.clearMapping(player);
        } else if (SheriffGUI.isSheriffGUI(title)) {
            SheriffGUI.clearMapping(player);
        } else if (RoleSelectorGUI.isRoleSelectorGUI(title)) {
            RoleSelectorGUI.clearMapping(player);
        }
    }
}
