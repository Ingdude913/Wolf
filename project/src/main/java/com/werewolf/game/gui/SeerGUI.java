package com.werewolf.game.gui;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.util.ColorUtil;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;

public class SeerGUI {

    public static final String GUI_TITLE = ChatColor.DARK_BLUE + "Seer - Check a Player";

    private static final Map<Player, Map<Integer, Player>> guiMappings = new HashMap<>();

    public static void open(WerewolfPlugin plugin, Arena arena, Player seer) {
        Inventory inv = Bukkit.createInventory(seer, 54, GUI_TITLE);

        Map<Integer, Player> slotMap = new HashMap<>();
        int slot = 0;

        for (GamePlayer gp : arena.getAlivePlayers()) {
            if (gp.getPlayer().getUniqueId().equals(seer.getUniqueId())) continue;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(gp.getPlayer());
                meta.setDisplayName(ChatColor.GOLD + gp.getPlayer().getName());
                skull.setItemMeta(meta);
            }
            inv.setItem(slot, skull);
            slotMap.put(slot, gp.getPlayer());
            slot++;
        }

        guiMappings.put(seer, slotMap);
        seer.openInventory(inv);
    }

    public static Player getPlayerAtSlot(Player seer, int slot) {
        Map<Integer, Player> map = guiMappings.get(seer);
        if (map == null) return null;
        return map.get(slot);
    }

    public static void clearMapping(Player seer) {
        guiMappings.remove(seer);
    }

    public static boolean isSeerGUI(Player player, String title) {
        return title != null && ChatColor.stripColor(title).startsWith("Seer - Check a Player");
    }
}
