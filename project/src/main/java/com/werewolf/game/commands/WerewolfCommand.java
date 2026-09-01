package com.werewolf.game.commands;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.arena.Arena;
import com.werewolf.game.arena.ArenaManager;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.util.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WerewolfCommand implements CommandExecutor, TabCompleter {

    private final WerewolfPlugin plugin;

    public WerewolfCommand(WerewolfPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                sendHelp(sender);
                break;
            case "join":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww join <arena>");
                    return true;
                }
                handleJoin((Player) sender, args[1]);
                break;
            case "leave":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                handleLeave((Player) sender);
                break;
            case "list":
                handleList(sender);
                break;
            case "create":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww create <world>");
                    return true;
                }
                handleCreate(sender, args[1]);
                break;
            case "delete":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww delete <arena>");
                    return true;
                }
                handleDelete(sender, args[1]);
                break;
            case "setlobby":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                handleSetLobby((Player) sender);
                break;
            case "setspawn":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww setspawn <arena>");
                    return true;
                }
                handleSetSpawn((Player) sender, args[1]);
                break;
            case "forcestart":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww forcestart <arena>");
                    return true;
                }
                handleForceStart(sender, args[1]);
                break;
            case "forcestop":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww forcestop <arena>");
                    return true;
                }
                handleForceStop(sender, args[1]);
                break;
            case "debug":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww debug <arena>");
                    return true;
                }
                handleDebug(sender, args[1]);
                break;
            case "setrole":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww setrole <player> <role>");
                    return true;
                }
                handleSetRole(sender, args[1], args[2]);
                break;
            case "skipday":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww skipday <arena>");
                    return true;
                }
                handleSkipDay(sender, args[1]);
                break;
            case "skipnight":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww skipnight <arena>");
                    return true;
                }
                handleSkipNight(sender, args[1]);
                break;
            case "skipelection":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww skipelection <arena>");
                    return true;
                }
                handleSkipElection(sender, args[1]);
                break;
            case "reveal":
                if (!sender.hasPermission("werewolf.admin")) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.prefix() + ChatColor.RED + "Usage: /ww reveal <arena>");
                    return true;
                }
                handleReveal(sender, args[1]);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void handleJoin(Player player, String arenaName) {
        ArenaManager am = plugin.getArenaManager();
        Arena arena = am.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + arenaName + " does not exist!");
            return;
        }
        Arena current = am.getArenaByPlayer(player);
        if (current != null) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "You are already in arena " + current.getName() + "! Leave first.");
            return;
        }
        arena.addPlayer(player);
    }

    private void handleLeave(Player player) {
        ArenaManager am = plugin.getArenaManager();
        Arena arena = am.getArenaByPlayer(player);
        if (arena == null) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "You are not in an arena!");
            return;
        }
        arena.removePlayer(player);
        player.sendMessage(plugin.prefix() + ChatColor.GREEN + "You left the arena.");
    }

    private void handleList(CommandSender sender) {
        ArenaManager am = plugin.getArenaManager();
        if (am.getArenas().isEmpty()) {
            sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "No arenas available.");
            return;
        }
        sender.sendMessage(plugin.prefix() + ChatColor.GOLD + "Arenas:");
        for (Arena arena : am.getArenas()) {
            sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + arena.getName() +
                    ChatColor.GRAY + " (" + arena.getPlayers().size() + " players) " +
                    ChatColor.YELLOW + arena.getPhase());
        }
    }

    private void handleCreate(CommandSender sender, String worldName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Only players can create arenas.");
            return;
        }
        Player player = (Player) sender;
        ArenaManager am = plugin.getArenaManager();
        if (am.arenaExists(worldName)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + worldName + " already exists!");
            return;
        }

        WorldManager wm = am.getWorldManager();
        if (!wm.worldFolderExists(worldName)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "World folder '" + worldName + "' not found!");
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Place your world folder in: plugins/Werewolf/World/" + worldName);
            sender.sendMessage(plugin.prefix() + ChatColor.GRAY + "Available worlds:");
            File worldsFolder = wm.getWorldsFolder();
            File[] dirs = worldsFolder.listFiles(File::isDirectory);
            if (dirs != null && dirs.length > 0) {
                for (File dir : dirs) {
                    sender.sendMessage(ChatColor.GRAY + "  - " + dir.getName());
                }
            } else {
                sender.sendMessage(ChatColor.GRAY + "  (none)");
            }
            return;
        }

        World world = wm.loadWorld(worldName);
        if (world == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Failed to load world '" + worldName + "'. Check the console for errors.");
            return;
        }

        am.createArena(worldName, worldName);
        player.teleport(world.getSpawnLocation());
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Arena " + worldName + " created with world " + worldName + "!");
        sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "You have been teleported to the world for setup.");
        sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "Use /ww setspawn " + worldName + " to set the game spawn location.");
        sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "Use /ww setlobby to set the global lobby where players wait and return after games.");
    }

    private void handleDelete(CommandSender sender, String arenaName) {
        ArenaManager am = plugin.getArenaManager();
        if (!am.arenaExists(arenaName)) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + arenaName + " does not exist!");
            return;
        }
        am.deleteArena(arenaName);
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Arena " + arenaName + " deleted.");
    }

    private void handleSetLobby(Player player) {
        plugin.getArenaManager().setGlobalLobby(player.getLocation());
        player.sendMessage(plugin.prefix() + ChatColor.GREEN + "Global lobby location set! Players will be teleported here when they join the server and when a game ends.");
    }

    private void handleSetSpawn(Player player, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + arenaName + " does not exist!");
            return;
        }
        arena.setSpawnLocation(player.getLocation());
        plugin.getArenaManager().saveArena(arena);
        player.sendMessage(plugin.prefix() + ChatColor.GREEN + "Spawn location set for arena " + arenaName + "!");
    }

    private void handleForceStart(CommandSender sender, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + arenaName + " does not exist!");
            return;
        }
        if (arena.getPhase() != com.werewolf.game.game.Phase.LOBBY) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Game already in progress!");
            return;
        }
        int minNeeded = arena.isDebugMode() ? 1 : 2;
        if (arena.getPlayers().size() < minNeeded) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Need at least " + minNeeded + " players to start!");
            return;
        }
        arena.startGame();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Game force started in arena " + arenaName + "!");
    }

    private void handleDebug(CommandSender sender, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + arenaName + " does not exist!");
            return;
        }
        arena.setDebugMode(!arena.isDebugMode());
        sender.sendMessage(plugin.prefix() + ChatColor.GOLD + "Debug mode for arena " + arenaName + ": " + (arena.isDebugMode() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        if (arena.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + ChatColor.YELLOW + "You can now force start with 1 player, assign roles with /ww setrole, skip day with /ww skipday, and see all roles with /ww reveal.");
        }
    }

    private void handleSetRole(CommandSender sender, String playerName, String roleName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Player " + playerName + " is not online!");
            return;
        }
        Arena arena = plugin.getArenaManager().getArenaByPlayer(target);
        if (arena == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Player " + playerName + " is not in an arena!");
            return;
        }
        if (!arena.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Debug mode must be enabled for that arena first! Use /ww debug <arena>");
            return;
        }
        List<String> validRoles = Arrays.asList("werewolf", "villager", "witch", "seer", "hunter", "trickster", "ninja", "mermaid", "masochist");
        if (!validRoles.contains(roleName.toLowerCase())) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Invalid role! Valid roles: " + String.join(", ", validRoles));
            return;
        }
        arena.forceSetRole(target, roleName);
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Set " + target.getName() + "'s role to " + roleName + ".");
    }

    private void handleSkipDay(CommandSender sender, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + arenaName + " does not exist!");
            return;
        }
        if (!arena.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Debug mode must be enabled for that arena first! Use /ww debug <arena>");
            return;
        }
        if (arena.getPhase() != com.werewolf.game.game.Phase.DAY) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "It is not day time!");
            return;
        }
        arena.skipDayFromCommand();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Skipped the remaining day time.");
    }

    private void handleSkipNight(CommandSender sender, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + arenaName + " does not exist!");
            return;
        }
        if (arena.getPhase() != com.werewolf.game.game.Phase.NIGHT) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "It is not night time!");
            return;
        }
        arena.skipNightFromCommand();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Skipped the remaining night time.");
    }

    private void handleSkipElection(CommandSender sender, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + arenaName + " does not exist!");
            return;
        }
        if (arena.getPhase() != com.werewolf.game.game.Phase.SHERIFF_ELECTION) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "It is not the sheriff election phase!");
            return;
        }
        arena.skipElectionFromCommand();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Skipped the remaining election time.");
    }

    private void handleReveal(CommandSender sender, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + arenaName + " does not exist!");
            return;
        }
        if (!arena.isDebugMode()) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Debug mode must be enabled for that arena first! Use /ww debug <arena>");
            return;
        }
        arena.revealRolesToSender(sender);
    }

    private void handleForceStop(CommandSender sender, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.prefix() + ChatColor.RED + "Arena " + arenaName + " does not exist!");
            return;
        }
        arena.forceStop();
        sender.sendMessage(plugin.prefix() + ChatColor.GREEN + "Game force stopped in arena " + arenaName + "!");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_RED + "===== " + ChatColor.RED + "Werewolf Commands" + ChatColor.DARK_RED + " =====");
        sender.sendMessage(ChatColor.GOLD + "/ww join <arena>" + ChatColor.GRAY + " - Join an arena");
        sender.sendMessage(ChatColor.GOLD + "/ww leave" + ChatColor.GRAY + " - Leave your current arena");
        sender.sendMessage(ChatColor.GOLD + "/ww list" + ChatColor.GRAY + " - List all arenas");
        if (sender.hasPermission("werewolf.admin")) {
            sender.sendMessage(ChatColor.GOLD + "/ww create <world>" + ChatColor.GRAY + " - Create a new arena from a world folder");
            sender.sendMessage(ChatColor.GOLD + "/ww delete <arena>" + ChatColor.GRAY + " - Delete an arena");
            sender.sendMessage(ChatColor.GOLD + "/ww setlobby" + ChatColor.GRAY + " - Set the global lobby location (players spawn here)");
            sender.sendMessage(ChatColor.GOLD + "/ww setspawn <arena>" + ChatColor.GRAY + " - Set arena game spawn location");
            sender.sendMessage(ChatColor.GOLD + "/ww forcestart <arena>" + ChatColor.GRAY + " - Force start a game");
            sender.sendMessage(ChatColor.GOLD + "/ww forcestop <arena>" + ChatColor.GRAY + " - Force stop a game");
            sender.sendMessage(ChatColor.GOLD + "/ww debug <arena>" + ChatColor.GRAY + " - Toggle debug mode (1-player start, role control)");
            sender.sendMessage(ChatColor.GOLD + "/ww setrole <player> <role>" + ChatColor.GRAY + " - Force-set a player's role (debug only)");
            sender.sendMessage(ChatColor.GOLD + "/ww skipday <arena>" + ChatColor.GRAY + " - Skip the remaining day time (debug only)");
            sender.sendMessage(ChatColor.GOLD + "/ww skipnight <arena>" + ChatColor.GRAY + " - Skip the remaining night time");
            sender.sendMessage(ChatColor.GOLD + "/ww skipelection <arena>" + ChatColor.GRAY + " - Skip the remaining sheriff election time");
            sender.sendMessage(ChatColor.GOLD + "/ww reveal <arena>" + ChatColor.GRAY + " - See all players' roles (debug only)");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("help");
            completions.add("join");
            completions.add("leave");
            completions.add("list");
            if (sender.hasPermission("werewolf.admin")) {
                completions.add("create");
                completions.add("delete");
                completions.add("setlobby");
                completions.add("setspawn");
                completions.add("forcestart");
                completions.add("forcestop");
                completions.add("debug");
                completions.add("setrole");
                completions.add("skipday");
                completions.add("skipnight");
                completions.add("skipelection");
                completions.add("reveal");
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("join") || sub.equals("delete") ||
                    sub.equals("setspawn") || sub.equals("forcestart") || sub.equals("forcestop") ||
                    sub.equals("skipday") || sub.equals("skipnight") || sub.equals("skipelection") || sub.equals("reveal") || sub.equals("debug")) {
                for (Arena arena : plugin.getArenaManager().getArenas()) {
                    completions.add(arena.getName());
                }
            } else if (sub.equals("create")) {
                WorldManager wm = plugin.getArenaManager().getWorldManager();
                File worldsFolder = wm.getWorldsFolder();
                File[] dirs = worldsFolder.listFiles(File::isDirectory);
                if (dirs != null) {
                    for (File dir : dirs) {
                        completions.add(dir.getName());
                    }
                }
            } else if (sub.equals("leave")) {
                completions.add("");
            } else if (sub.equals("setrole")) {
                for (Arena arena : plugin.getArenaManager().getArenas()) {
                    for (GamePlayer gp : arena.getPlayers()) {
                        completions.add(gp.getPlayer().getName());
                    }
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setrole")) {
                completions.add("werewolf");
                completions.add("villager");
                completions.add("witch");
                completions.add("seer");
                completions.add("hunter");
                completions.add("trickster");
                completions.add("ninja");
                completions.add("mermaid");
                completions.add("masochist");
            }
        }
        return completions;
    }
}
