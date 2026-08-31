package com.werewolf.game.listeners;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.gui.InfoGUI;
import com.werewolf.game.gui.NinjaGUI;
import com.werewolf.game.gui.SeerGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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

        Inventory inv = event.getView().getTopInventory();
        if (inv == null) return;

        String title = event.getView().getTitle();
        if (SeerGUI.isSeerGUI(player, title)) {
            event.setCancelled(true);

            Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
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

            Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
            if (arena == null) return;

            GamePlayer ninjaGp = arena.getGamePlayer(player);
            if (ninjaGp == null || !ninjaGp.isAlive()) return;

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) return;

            String ability = NinjaGUI.getAbilityAtSlot(player, slot);
            if (ability == null) return;

            arena.ninjaSelectAbility(player, ability);
            player.closeInventory();
        } else if (InfoGUI.isInfoGUI(title)) {
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
        } else if (InfoGUI.isInfoGUI(title)) {
            InfoGUI.clearOpenGUI(player);
        }
    }
}
