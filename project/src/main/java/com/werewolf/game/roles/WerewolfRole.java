package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WerewolfRole extends Role {

    public WerewolfRole() {
        super("Werewolf", Team.BAD,
                "You are a Werewolf! During the night, right-click your ability to wear wolf armor and get an axe to kill the opposing team. During the day, blend in with the villagers.");
    }

    @Override
    public void onNightStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + "&cNight falls! You can now use your werewolf abilities.");
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + "&7Right-click while holding nothing (or your ability item) to transform and get your axe.");
        giveAbilityItem(player);
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + "&eDay breaks! Hide your identity and blend in with the villagers.");
        removeWerewolfGear(player);
    }

    @Override
    public List<ItemStack> getRoleItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "werewolf-axe"));
        return items;
    }

    private void giveAbilityItem(Player player) {
        ItemStack ability = ItemBuilder.create(WerewolfPlugin.getInstance(), "werewolf-armor");
        player.getInventory().addItem(ability);
    }

    private void removeWerewolfGear(Player player) {
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "werewolf-axe"));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "werewolf-armor"));
        if (player.getInventory().getChestplate() != null &&
                player.getInventory().getChestplate().getType() == Material.IRON_CHESTPLATE) {
            player.getInventory().setChestplate(null);
        }
    }

    @Override
    public boolean isWerewolf() {
        return true;
    }

    @Override
    public boolean canSeeWerewolves() {
        return true;
    }
}
