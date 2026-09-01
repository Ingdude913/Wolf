package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ColorUtil;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TricksterRole extends Role {

    public TricksterRole() {
        super("Trickster", Team.GOOD,
                "You are the Trickster! You are NOT a werewolf, but you can pretend to be one. " +
                        "Real werewolves see you as a teammate, but they don't know you are fake. " +
                        "During the night, you get the same werewolf ability items, but your axe cannot kill. " +
                        "Use your deception to confuse the werewolves and help the good team win!");
    }

    @Override
    public void onNightStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&cNight falls! The werewolves think you are one of them."));
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&7You receive werewolf items to blend in, but your axe is fake and cannot kill."));
        ItemStack armor = ItemBuilder.create(WerewolfPlugin.getInstance(), "werewolf-armor");
        player.getInventory().addItem(armor);
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&eDay breaks! Your deception items are removed."));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "werewolf-armor"));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "werewolf-armor"));
        return items;
    }

    @Override
    public boolean isTrickster() {
        return true;
    }
}
