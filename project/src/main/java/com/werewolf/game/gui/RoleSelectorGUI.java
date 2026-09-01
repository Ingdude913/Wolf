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

public class RoleSelectorGUI {

    public static final String GUI_TITLE = ChatColor.DARK_AQUA + "Role Selector - Setup the Game";

    private static final Map<Player, Map<Integer, String>> guiMappings = new HashMap<>();
    private static final Map<Player, Boolean> sheriffEnabledMap = new HashMap<>();

    public static final String ROLE_WEREWOLF = "werewolf";
    public static final String ROLE_VILLAGER = "villager";
    public static final String ROLE_WITCH = "witch";
    public static final String ROLE_SEER = "seer";
    public static final String ROLE_HUNTER = "hunter";
    public static final String ROLE_TRICKSTER = "trickster";
    public static final String ROLE_NINJA = "ninja";
    public static final String ROLE_MERMAID = "mermaid";
    public static final String ROLE_MASOCHIST = "masochist";

    public static void open(Player player, Map<String, Integer> currentSelection, boolean sheriffEnabled) {
        sheriffEnabledMap.put(player, sheriffEnabled);
        Inventory inv = Bukkit.createInventory(player, 27, GUI_TITLE);
        populateInventory(inv, player, currentSelection);
        player.openInventory(inv);
    }

    public static void update(Player player, Map<String, Integer> currentSelection, boolean sheriffEnabled) {
        sheriffEnabledMap.put(player, sheriffEnabled);
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv == null || !isRoleSelectorGUI(player.getOpenInventory().getTitle())) return;
        populateInventory(inv, player, currentSelection);
        player.updateInventory();
    }

    private static void populateInventory(Inventory inv, Player player, Map<String, Integer> currentSelection) {
        Map<Integer, String> slotMap = new HashMap<>();

        int slot = 10;
        slot = addRole(inv, slotMap, slot, Material.NETHERITE_AXE, ROLE_WEREWOLF, ChatColor.RED + "Werewolf", currentSelection.getOrDefault(ROLE_WEREWOLF, 0));
        slot = addRole(inv, slotMap, slot, Material.STONE_SWORD, ROLE_VILLAGER, ChatColor.GREEN + "Villager", currentSelection.getOrDefault(ROLE_VILLAGER, 0));
        slot = addRole(inv, slotMap, slot, Material.POTION, ROLE_WITCH, ChatColor.DARK_PURPLE + "Witch", currentSelection.getOrDefault(ROLE_WITCH, 0));
        slot = addRole(inv, slotMap, slot, Material.BOOK, ROLE_SEER, ChatColor.BLUE + "Seer", currentSelection.getOrDefault(ROLE_SEER, 0));
        slot = addRole(inv, slotMap, slot, Material.BOW, ROLE_HUNTER, ChatColor.GOLD + "Hunter", currentSelection.getOrDefault(ROLE_HUNTER, 0));
        slot = addRole(inv, slotMap, slot, Material.WOLF_SPAWN_EGG, ROLE_TRICKSTER, ChatColor.DARK_RED + "Trickster", currentSelection.getOrDefault(ROLE_TRICKSTER, 0));
        slot = addRole(inv, slotMap, slot, Material.ENDER_EYE, ROLE_NINJA, ChatColor.DARK_PURPLE + "Ninja", currentSelection.getOrDefault(ROLE_NINJA, 0));
        slot = addRole(inv, slotMap, slot, Material.NAUTILUS_SHELL, ROLE_MERMAID, ChatColor.AQUA + "Mermaid", currentSelection.getOrDefault(ROLE_MERMAID, 0));
        slot = addRole(inv, slotMap, slot, Material.CACTUS, ROLE_MASOCHIST, ChatColor.DARK_GREEN + "Masochist", currentSelection.getOrDefault(ROLE_MASOCHIST, 0));

        boolean sheriffEnabled = sheriffEnabledMap.getOrDefault(player, true);
        ItemStack sheriffToggle = new ItemStack(sheriffEnabled ? Material.EMERALD : Material.REDSTONE);
        ItemMeta sheriffMeta = sheriffToggle.getItemMeta();
        if (sheriffMeta != null) {
            sheriffMeta.setDisplayName(ColorUtil.color(ChatColor.GOLD + "Sheriff Election: " + (sheriffEnabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED")));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "When enabled, players vote for a");
            lore.add(ChatColor.GRAY + "Sheriff before the first night.");
            lore.add(ChatColor.GRAY + "The Sheriff gets 2 votes during day.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to toggle on/off");
            sheriffMeta.setLore(lore);
            sheriffToggle.setItemMeta(sheriffMeta);
        }
        inv.setItem(22, sheriffToggle);
        slotMap.put(22, "sheriff-toggle");

        guiMappings.put(player, slotMap);
    }

    private static int addRole(Inventory inv, Map<Integer, String> slotMap, int slot, Material material, String roleKey, String displayName, int count) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(displayName));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Count: " + ChatColor.WHITE + count);
            lore.add("");
            lore.add(ChatColor.YELLOW + "Left-click: " + ChatColor.GREEN + "Add one");
            lore.add(ChatColor.YELLOW + "Right-click: " + ChatColor.RED + "Remove one");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
        slotMap.put(slot, roleKey);
        return slot + 2;
    }

    public static String getRoleAtSlot(Player player, int slot) {
        Map<Integer, String> map = guiMappings.get(player);
        if (map == null) return null;
        return map.get(slot);
    }

    public static void clearMapping(Player player) {
        guiMappings.remove(player);
        sheriffEnabledMap.remove(player);
    }

    public static boolean isRoleSelectorGUI(String title) {
        return title != null && ChatColor.stripColor(title).startsWith("Role Selector");
    }
}
