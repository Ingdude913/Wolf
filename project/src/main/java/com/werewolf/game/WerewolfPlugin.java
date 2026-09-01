package com.werewolf.game;

import com.werewolf.game.arena.Arena;
import com.werewolf.game.arena.ArenaManager;
import com.werewolf.game.commands.WerewolfCommand;
import com.werewolf.game.game.GameManager;
import com.werewolf.game.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

public class WerewolfPlugin extends JavaPlugin {

    private static WerewolfPlugin instance;

    private ArenaManager arenaManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.gameManager = new GameManager(this);

        arenaManager.getWorldManager().getWorldsFolder();

        arenaManager.loadArenasFromConfig();

        WerewolfCommand command = new WerewolfCommand(this);
        getCommand("werewolf").setExecutor(command);
        getCommand("werewolf").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDropListener(this), this);
        getServer().getPluginManager().registerEvents(new FoodLevelListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);

        getLogger().info("Werewolf plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            for (Arena arena : arenaManager.getArenas()) {
                arena.forceStop();
            }
        }
        getLogger().info("Werewolf plugin disabled!");
    }

    public static WerewolfPlugin getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public String prefix() {
        return com.werewolf.game.util.ColorUtil.color(getConfig().getString("prefix", "&8[&cWerewolf&8] &r"));
    }
}
