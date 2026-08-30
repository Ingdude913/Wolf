package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class WerewolfRole extends Role {

    public WerewolfRole() {
        super("Werewolf", Team.BAD,
                "You are a Werewolf! During the night, right-click your armor item to transform: you get full netherite armor, a werewolf axe, and speed. Right-click again to untransform and become briefly invisible. During the day, blend in with the villagers.");
    }

    @Override
    public void onNightStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + "&cNight falls! You can now use your werewolf abilities.");
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + "&7Right-click your Werewolf Armor to transform. Right-click again to untransform and go invisible briefly.");
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
        player.removePotionEffect(PotionEffectType.SPEED);
        if (player.getInventory().getHelmet() != null &&
                player.getInventory().getHelmet().getType() == Material.NETHERITE_HELMET) {
            player.getInventory().setHelmet(null);
        }
        if (player.getInventory().getChestplate() != null &&
                player.getInventory().getChestplate().getType() == Material.NETHERITE_CHESTPLATE) {
            player.getInventory().setChestplate(null);
        }
        if (player.getInventory().getLeggings() != null &&
                player.getInventory().getLeggings().getType() == Material.NETHERITE_LEGGINGS) {
            player.getInventory().setLeggings(null);
        }
        if (player.getInventory().getBoots() != null &&
                player.getInventory().getBoots().getType() == Material.NETHERITE_BOOTS) {
            player.getInventory().setBoots(null);
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
