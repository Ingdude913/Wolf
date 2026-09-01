package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MasochistRole extends Role {

    public MasochistRole() {
        super("Masochist", Team.NEUTRAL,
                "You are the Masochist! You can vote during the day, but your votes are invalid and do not count. " +
                        "Your goal is to receive the MOST votes during a daytime vote. " +
                        "If you are the most-voted player when the day vote ends, you win and the game is over! " +
                        "Act suspicious and make people vote for you!");
    }

    @Override
    public void onNightStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&9Night falls! Survive the night so you can be voted tomorrow."));
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&eDay breaks! Act suspicious and make people vote for you! Your own votes do not count."));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        return new ArrayList<>();
    }

    @Override
    public boolean isMasochist() {
        return true;
    }
}
