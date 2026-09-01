package com.werewolf.game.game;

import com.werewolf.game.roles.*;
import org.bukkit.entity.Player;

public class GamePlayer {

    private final Player player;
    private Role role;
    private boolean alive = true;
    private boolean voted = false;
    private Player votedFor = null;
    private boolean transformed = false;
    private boolean isSheriff = false;

    public GamePlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean hasVoted() {
        return voted;
    }

    public void setVoted(boolean voted) {
        this.voted = voted;
    }

    public Player getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(Player votedFor) {
        this.votedFor = votedFor;
    }

    public void resetVote() {
        this.voted = false;
        this.votedFor = null;
    }

    public boolean isTransformed() {
        return transformed;
    }

    public void setTransformed(boolean transformed) {
        this.transformed = transformed;
    }

    public WerewolfRole asWerewolf() {
        if (role instanceof WerewolfRole) return (WerewolfRole) role;
        return null;
    }

    public WitchRole asWitch() {
        if (role instanceof WitchRole) return (WitchRole) role;
        return null;
    }

    public SeerRole asSeer() {
        if (role instanceof SeerRole) return (SeerRole) role;
        return null;
    }

    public HunterRole asHunter() {
        if (role instanceof HunterRole) return (HunterRole) role;
        return null;
    }

    public TricksterRole asTrickster() {
        if (role instanceof TricksterRole) return (TricksterRole) role;
        return null;
    }

    public NinjaRole asNinja() {
        if (role instanceof NinjaRole) return (NinjaRole) role;
        return null;
    }

    public MermaidRole asMermaid() {
        if (role instanceof MermaidRole) return (MermaidRole) role;
        return null;
    }

    public MasochistRole asMasochist() {
        if (role instanceof MasochistRole) return (MasochistRole) role;
        return null;
    }

    public boolean isSheriff() {
        return isSheriff;
    }

    public void setSheriff(boolean sheriff) {
        this.isSheriff = sheriff;
    }
}
