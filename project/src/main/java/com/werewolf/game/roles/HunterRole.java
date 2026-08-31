package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ColorUtil;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class HunterRole extends Role {

    private Player target = null;
    private boolean targetLocked = false;

    public HunterRole() {
        super("Hunter", Team.GOOD,
                "You are a Hunter! You play like a villager, but during the night you can select a target using your compass. " +
                        "If you are killed, the player you selected will die during the next day. " +
                        "Choose your target carefully!");
    }

    @Override
    public void onNightStart(Player player) {
        targetLocked = false;
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&6Night falls! You may select a target with your compass."));
        player.getInventory().addItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "hunter-target"));
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&eDay breaks! Your target selection is locked."));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "hunter-target"));
        targetLocked = true;
    }

    @Override
    public List<ItemStack> getRoleItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "hunter-target"));
        return items;
    }

    public Player getTarget() {
        return target;
    }

    public void setTarget(Player target) {
        this.target = target;
    }

    public boolean isTargetLocked() {
        return targetLocked;
    }
}
