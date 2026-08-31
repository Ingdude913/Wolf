package com.werewolf.game.gui;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoGUI {

    public static final String SETUP_TITLE = ChatColor.DARK_AQUA + "Game Setup";
    public static final String WOLF_TEAM_TITLE = ChatColor.DARK_RED + "Wolf Team";

    private static final Map<Player, String> openGUIs = new HashMap<>();

    public static void openSetup(WerewolfPlugin plugin, Arena arena, Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, SETUP_TITLE);

        int total = arena.getPlayers().size();
        long werewolves = arena.getPlayers().stream().filter(gp -> gp.getRole().isWerewolf()).count();
        long tricksters = arena.getPlayers().stream().filter(gp -> gp.getRole().isTrickster()).count();
        long witches = arena.getPlayers().stream().filter(gp -> gp.getRole() instanceof com.werewolf.game.roles.WitchRole).count();
        long seers = arena.getPlayers().stream().filter(gp -> gp.getRole() instanceof com.werewolf.game.roles.SeerRole).count();
        long hunters = arena.getPlayers().stream().filter(gp -> gp.getRole() instanceof com.werewolf.game.roles.HunterRole).count();
        long villagers = arena.getPlayers().stream().filter(gp -> gp.getRole() instanceof com.werewolf.game.roles.VillagerRole).count();
        long ninjas = arena.getPlayers().stream().filter(gp -> gp.getRole().isNinja()).count();

        inv.setItem(0, createInfoItem(Material.IRON_SWORD, "&cWerewolves", "&7Count: &f" + werewolves));
        inv.setItem(1, createInfoItem(Material.PAPER, "&dTricksters", "&7Count: &f" + tricksters));
        inv.setItem(2, createInfoItem(Material.POTION, "&5Witches", "&7Count: &f" + witches));
        inv.setItem(3, createInfoItem(Material.BOOK, "&9Seers", "&7Count: &f" + seers));
        inv.setItem(4, createInfoItem(Material.COMPASS, "&6Hunters", "&7Count: &f" + hunters));
        inv.setItem(5, createInfoItem(Material.STICK, "&aVillagers", "&7Count: &f" + villagers));
        inv.setItem(6, createInfoItem(Material.ENDER_PEARL, "&5Ninjas", "&7Count: &f" + ninjas));
        inv.setItem(7, createInfoItem(Material.PLAYER_HEAD, "&fTotal Players", "&7Count: &f" + total));

        int dayDur = plugin.getConfig().getInt("day-duration", 120);
        int nightDur = plugin.getConfig().getInt("night-duration", 60);
        inv.setItem(8, createInfoItem(Material.CLOCK, "&eDay Duration", "&7" + dayDur + " seconds"));
        inv.setItem(9, createInfoItem(Material.CLOCK, "&9Night Duration", "&7" + nightDur + " seconds"));

        openGUIs.put(player, "setup");
        player.openInventory(inv);
    }

    public static void openWolfTeam(Arena arena, Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, WOLF_TEAM_TITLE);

        int slot = 0;
        for (GamePlayer gp : arena.getPlayers()) {
            if (gp.getRole().isWerewolf() || gp.getRole().isTrickster()) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(gp.getPlayer());
                    String nameColor = gp.getRole().isWerewolf() ? ChatColor.RED + gp.getPlayer().getName() : ChatColor.GOLD + gp.getPlayer().getName() + " (Trickster)";
                    meta.setDisplayName(nameColor);
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + gp.getRole().getName());
                    if (gp.isAlive()) {
                        lore.add(ChatColor.GREEN + "Alive");
                    } else {
                        lore.add(ChatColor.RED + "Dead");
                    }
                    meta.setLore(lore);
                    skull.setItemMeta(meta);
                }
                inv.setItem(slot, skull);
                slot++;
            }
        }

        openGUIs.put(player, "wolfteam");
        player.openInventory(inv);
    }

    public static String getOpenGUI(Player player) {
        return openGUIs.get(player);
    }

    public static void clearOpenGUI(Player player) {
        openGUIs.remove(player);
    }

    public static boolean isInfoGUI(String title) {
        if (title == null) return false;
        String stripped = ChatColor.stripColor(title);
        return stripped.equals("Game Setup") || stripped.equals("Wolf Team");
    }

    private static ItemStack createInfoItem(Material material, String name, String... lore) {
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
