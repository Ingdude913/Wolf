package com.werewolf.game.roles;

import com.werewolf.game.game.Team;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class Role {

    protected final String name;
    protected final Team team;
    protected final String description;

    protected Role(String name, Team team, String description) {
        this.name = name;
        this.team = team;
        this.description = description;
    }

    public abstract void onNightStart(Player player);

    public abstract void onDayStart(Player player);

    public abstract List<ItemStack> getRoleItems();

    public String getName() {
        return name;
    }

    public Team getTeam() {
        return team;
    }

    public String getDescription() {
        return description;
    }

    public boolean isWerewolf() {
        return false;
    }

    public boolean isTrickster() {
        return false;
    }

    public boolean isNinja() {
        return false;
    }

    public boolean isMermaid() {
        return false;
    }

    public boolean isMasochist() {
        return false;
    }

    public boolean canSeeWerewolves() {
        return false;
    }

    public boolean isGood() {
        return team == Team.GOOD;
    }

    public boolean isBad() {
        return team == Team.BAD;
    }
}
