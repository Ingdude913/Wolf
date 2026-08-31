package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ColorUtil;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WitchRole extends Role {

    private boolean poisonUsed = false;
    private boolean healUsed = false;

    public WitchRole() {
        super("Witch", Team.GOOD,
                "You are a Witch! You have one bottle of poison and one bottle of heal. " +
                        "The poison kills a player, the heal saves a player. Once used, they are gone forever. " +
                        "Use them wisely during the night!");
    }

    @Override
    public void onNightStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&dNight falls! You may use your potions."));
        if (!poisonUsed) {
            player.getInventory().addItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "witch-poison"));
        }
        if (!healUsed) {
            player.getInventory().addItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "witch-heal"));
        }
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&eDay breaks! Your potions cannot be used during the day."));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "witch-poison"));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "witch-heal"));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        List<ItemStack> items = new ArrayList<>();
        if (!poisonUsed) items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "witch-poison"));
        if (!healUsed) items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "witch-heal"));
        return items;
    }

    public boolean isPoisonUsed() {
        return poisonUsed;
    }

    public boolean isHealUsed() {
        return healUsed;
    }

    public void usePoison() {
        this.poisonUsed = true;
    }

    public void useHeal() {
        this.healUsed = true;
    }
}
