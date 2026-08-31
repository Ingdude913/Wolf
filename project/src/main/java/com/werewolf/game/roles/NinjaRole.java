package com.werewolf.game.roles;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.Team;
import com.werewolf.game.util.ColorUtil;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NinjaRole extends Role {

    private boolean abilityUsedTonight = false;
    private String selectedAbility = null;

    public NinjaRole() {
        super("Ninja", Team.GOOD,
                "You are a Ninja! Each night, use your ability book to choose one of four abilities: " +
                        "Vanish (become invisible for a short time), Sprint (run very fast for a short time), " +
                        "Decoy (spawn a fake copy of yourself), or Disguise (appear as a fake wolf that won't show on the wolf team list). " +
                        "Then right-click your Ninja Orb to activate the chosen ability. " +
                        "You can only use ONE ability per night, and each lasts only a few seconds. " +
                        "Choose wisely!");
    }

    @Override
    public void onNightStart(Player player) {
        abilityUsedTonight = false;
        selectedAbility = null;
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&5Night falls! Use your Ninja Book to select an ability, then use the Ninja Orb to activate it."));
        player.getInventory().addItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "ninja-book"));
    }

    @Override
    public void onDayStart(Player player) {
        player.sendMessage(WerewolfPlugin.getInstance().prefix() + ColorUtil.color("&eDay breaks! Your ninja abilities are no longer available."));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "ninja-book"));
        player.getInventory().removeItem(ItemBuilder.create(WerewolfPlugin.getInstance(), "ninja-ability"));
    }

    @Override
    public List<ItemStack> getRoleItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(ItemBuilder.create(WerewolfPlugin.getInstance(), "ninja-book"));
        return items;
    }

    public boolean hasUsedAbilityTonight() {
        return abilityUsedTonight;
    }

    public void setAbilityUsedTonight(boolean used) {
        this.abilityUsedTonight = used;
    }

    public String getSelectedAbility() {
        return selectedAbility;
    }

    public void setSelectedAbility(String ability) {
        this.selectedAbility = ability;
    }

    @Override
    public boolean isNinja() {
        return true;
    }
}
