package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ColorUtil;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SeerRole extends Role {

    private boolean hasCheckedTonight = false;

    public SeerRole() {
        super("Seer", Team.GOOD,
                "You are a Seer! You have a book that can open a GUI to check players' teams. " +
                        "You can only check ONE player per night, and it only shows Good or Bad team. " +
                        "Use your knowledge wisely!");
    }

    @Override
    public void onNightStart(Player player) {
        hasCheckedTonight = false;
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&9Night falls! You may check one player's team."));
        player.getInventory().addItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "seer-book"));
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&eDay breaks! Your seer book is no longer usable."));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "seer-book"));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "seer-book"));
        return items;
    }

    public boolean hasCheckedTonight() {
        return hasCheckedTonight;
    }

    public void setCheckedTonight(boolean checked) {
        this.hasCheckedTonight = checked;
    }
}
