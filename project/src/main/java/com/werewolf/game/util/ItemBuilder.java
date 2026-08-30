package com.werewolf.game.util;

import com.werewolf.game.WerewolfPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    public static ItemStack create(WerewolfPlugin plugin, String itemKey) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("items." + itemKey);
        if (section == null) {
            return new ItemStack(Material.STONE);
        }
        String materialName = section.getString("material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("name", "");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack rename(ItemStack item, String name) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createNamed(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isSimilarDisplayName(ItemStack item, String displayName) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase(
                ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', displayName)));
    }

    public static boolean isItemKey(WerewolfPlugin plugin, ItemStack item, String itemKey) {
        if (item == null) return false;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("items." + itemKey);
        if (section == null) return false;
        String materialName = section.getString("material", "STONE");
        try {
            Material expected = Material.valueOf(materialName.toUpperCase());
            if (item.getType() != expected) return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
        String name = section.getString("name", "");
        return isSimilarDisplayName(item, name);
    }
}
