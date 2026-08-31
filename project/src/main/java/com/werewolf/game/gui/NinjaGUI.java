package com.werewolf.game.gui;

import com.werewolf.game.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NinjaGUI {

    public static final String GUI_TITLE = ChatColor.DARK_PURPLE + "Ninja - Select Ability";

    private static final Map<Player, Map<Integer, String>> guiMappings = new HashMap<>();

    public static final String ABILITY_VANISH = "vanish";
    public static final String ABILITY_SPRINT = "sprint";
    public static final String ABILITY_DECOY = "decoy";
    public static final String ABILITY_DISGUISE = "disguise";

    public static void open(Player ninja) {
        Inventory inv = Bukkit.createInventory(ninja, 27, GUI_TITLE);

        Map<Integer, String> slotMap = new HashMap<>();

        ItemStack vanish = createAbilityItem(Material.ENDER_PEARL, "&5&lVanish",
                "&7Become invisible for 8 seconds.");
        inv.setItem(11, vanish);
        slotMap.put(11, ABILITY_VANISH);

        ItemStack sprint = createAbilityItem(Material.SUGAR, "&b&lSprint",
                "&7Run extremely fast for 8 seconds.");
        inv.setItem(12, sprint);
        slotMap.put(12, ABILITY_SPRINT);

        ItemStack decoy = createAbilityItem(Material.PLAYER_HEAD, "&a&lDecoy",
                "&7Spawn a fake copy of yourself for 8 seconds.");
        inv.setItem(14, decoy);
        slotMap.put(14, ABILITY_DECOY);

        ItemStack disguise = createAbilityItem(Material.WOLF_SPAWN_EGG, "&c&lDisguise",
                "&7Appear as a fake wolf for 8 seconds.",
                "&7You will NOT appear on the wolf team list.");
        inv.setItem(15, disguise);
        slotMap.put(15, ABILITY_DISGUISE);

        guiMappings.put(ninja, slotMap);
        ninja.openInventory(inv);
    }

    public static String getAbilityAtSlot(Player player, int slot) {
        Map<Integer, String> map = guiMappings.get(player);
        if (map == null) return null;
        return map.get(slot);
    }

    public static void clearMapping(Player player) {
        guiMappings.remove(player);
    }

    public static boolean isNinjaGUI(String title) {
        return title != null && ChatColor.stripColor(title).startsWith("Ninja - Select Ability");
    }

    private static ItemStack createAbilityItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ColorUtil.color(line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
