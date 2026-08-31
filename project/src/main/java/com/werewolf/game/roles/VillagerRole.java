package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class VillagerRole extends Role {

    public VillagerRole() {
        super("Villager", Team.GOOD,
                "You are a Villager! You have no special abilities. During the day, vote to eliminate suspected werewolves. Survive the nights!");
    }

    @Override
    public void onNightStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&9Night falls! Stay safe and hope the werewolves don't find you."));
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&eDay breaks! Discuss and vote to eliminate suspected werewolves."));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        return new ArrayList<>();
    }
}
