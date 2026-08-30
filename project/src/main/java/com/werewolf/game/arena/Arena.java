package com.werewolf.game.arena;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.*;
import com.werewolf.game.roles.*;
import com.werewolf.game.util.ColorUtil;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class Arena {

    private final WerewolfPlugin plugin;
    private final String name;
    private String worldName;
    private Location lobbyLocation;
    private Location spawnLocation;

    private final Set<GamePlayer> players = new HashSet<>();
    private Phase phase = Phase.LOBBY;
    private int taskId = -1;
    private int phaseTimer = 0;
    private int minPlayers;
    private int dayDuration;
    private int nightDuration;
    private int lobbyDuration;

    private final Map<UUID, Integer> voteCounts = new HashMap<>();
    private final Map<UUID, UUID> hunterTargets = new HashMap<>();

    private boolean debugMode = false;

    private BossBar bossBar = null;
    private int actionBarTaskId = -1;

    public Arena(WerewolfPlugin plugin, String name, String worldName) {
        this.plugin = plugin;
        this.name = name;
        this.worldName = worldName;
        this.minPlayers = plugin.getConfig().getInt("min-players", 4);
        this.dayDuration = plugin.getConfig().getInt("day-duration", 120);
        this.nightDuration = plugin.getConfig().getInt("night-duration", 60);
        this.lobbyDuration = plugin.getConfig().getInt("lobby-duration", 30);
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public void setLobbyLocation(Location loc) {
        this.lobbyLocation = loc;
    }

    public void setSpawnLocation(Location loc) {
        this.spawnLocation = loc;
    }

    public Location getLobbyLocation() {
        return lobbyLocation;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public Phase getPhase() {
        return phase;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public Set<GamePlayer> getPlayers() {
        return players;
    }

    public Set<GamePlayer> getAlivePlayers() {
        return players.stream().filter(GamePlayer::isAlive).collect(Collectors.toSet());
    }

    public Set<GamePlayer> getDeadPlayers() {
        return players.stream().filter(gp -> !gp.isAlive()).collect(Collectors.toSet());
    }

    public GamePlayer getGamePlayer(Player player) {
        return players.stream().filter(gp -> gp.getPlayer().getUniqueId().equals(player.getUniqueId())).findFirst().orElse(null);
    }

    public boolean isPlayerInArena(Player player) {
        return getGamePlayer(player) != null;
    }

    public boolean isFull() {
        return players.size() >= 16;
    }

    public void addPlayer(Player player) {
        if (phase != Phase.LOBBY) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "This game has already started!");
            return;
        }
        if (isPlayerInArena(player)) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "You are already in this arena!");
            return;
        }
        if (isFull()) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "This arena is full!");
            return;
        }

        GamePlayer gp = new GamePlayer(player);
        players.add(gp);

        player.sendMessage(plugin.prefix() + ChatColor.GREEN + "You joined arena " + ChatColor.GOLD + name + ChatColor.GREEN + "!");
        broadcast(ChatColor.GREEN + player.getName() + " joined the arena! (" + players.size() + "/" + 16 + ")");

        if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
        }
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(20);
        player.setFoodLevel(20);

        if (bossBar != null) {
            bossBar.addPlayer(player);
        }

        if (players.size() >= minPlayers && taskId == -1) {
            startLobbyCountdown();
        }
    }

    public void removePlayer(Player player) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null) return;

        players.remove(gp);
        voteCounts.remove(player.getUniqueId());
        hunterTargets.remove(player.getUniqueId());

        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(player.getWorld().getSpawnLocation());

        broadcast(ChatColor.YELLOW + player.getName() + " left the arena! (" + players.size() + "/" + 16 + ")");

        if (phase == Phase.LOBBY && taskId != -1 && players.size() < minPlayers) {
            cancelTask();
            broadcast(ChatColor.RED + "Not enough players. Countdown cancelled.");
        }

        if (phase == Phase.DAY || phase == Phase.NIGHT) {
            checkWinCondition();
        }
    }

    private void startLobbyCountdown() {
        phaseTimer = lobbyDuration;
        broadcast(ChatColor.GREEN + "Minimum players reached! Game starting in " + lobbyDuration + " seconds.");

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (players.size() < minPlayers) {
                    cancelTask();
                    taskId = -1;
                    broadcast(ChatColor.RED + "Not enough players. Countdown cancelled.");
                    return;
                }
                if (phaseTimer <= 0) {
                    cancelTask();
                    taskId = -1;
                    startGame();
                    return;
                }
                if (phaseTimer <= 10 || phaseTimer % 30 == 0) {
                    broadcast(ChatColor.GOLD + "Game starting in " + phaseTimer + " seconds!");
                }
                phaseTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    public void startGame() {
        assignRoles();
        phase = Phase.DAY;
        broadcast(ChatColor.GREEN + "The game has begun! It is DAY time. Discuss and vote!");
        teleportPlayersToSpawn();
        giveDayItems();
        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.setGameMode(GameMode.ADVENTURE);
            p.setHealth(20);
            p.setFoodLevel(20);
            p.sendMessage(plugin.prefix() + ChatColor.GOLD + "Your role: " + ChatColor.WHITE + gp.getRole().getName());
            p.sendMessage(plugin.prefix() + ChatColor.GRAY + gp.getRole().getDescription());
            gp.getRole().onDayStart(p);
        }
        startDayPhase();
    }

    private void assignRoles() {
        List<GamePlayer> playerList = new ArrayList<>(players);
        Collections.shuffle(playerList);

        int total = playerList.size();
        int werewolfCount = Math.max(1, total / 4);
        int lierCount = total >= 6 ? 1 : 0;
        int witchCount = total >= 4 ? 1 : 0;
        int seerCount = total >= 4 ? 1 : 0;
        int hunterCount = total >= 5 ? 1 : 0;

        int index = 0;
        for (int i = 0; i < werewolfCount && index < total; i++) {
            playerList.get(index++).setRole(new WerewolfRole());
        }
        for (int i = 0; i < lierCount && index < total; i++) {
            playerList.get(index++).setRole(new LierRole());
        }
        for (int i = 0; i < witchCount && index < total; i++) {
            playerList.get(index++).setRole(new WitchRole());
        }
        for (int i = 0; i < seerCount && index < total; i++) {
            playerList.get(index++).setRole(new SeerRole());
        }
        for (int i = 0; i < hunterCount && index < total; i++) {
            playerList.get(index++).setRole(new HunterRole());
        }
        while (index < total) {
            playerList.get(index++).setRole(new VillagerRole());
        }

        List<String> werewolfNames = new ArrayList<>();
        for (GamePlayer gp : players) {
            if (gp.getRole().isWerewolf() || gp.getRole().isLier()) {
                werewolfNames.add(gp.getPlayer().getName());
            }
        }
        for (GamePlayer gp : players) {
            if (gp.getRole().canSeeWerewolves()) {
                Player p = gp.getPlayer();
                p.sendMessage(plugin.prefix() + ChatColor.RED + "Werewolves (your team): " + ChatColor.WHITE + String.join(", ", werewolfNames));
            }
        }
    }

    private void teleportPlayersToSpawn() {
        if (spawnLocation != null) {
            for (GamePlayer gp : players) {
                gp.getPlayer().teleport(spawnLocation);
            }
        }
    }

    private void giveDayItems() {
        for (GamePlayer gp : getAlivePlayers()) {
            Player p = gp.getPlayer();
            p.getInventory().clear();
            p.getInventory().addItem(ItemBuilder.create(plugin, "vote-sword"));
            p.getInventory().addItem(ItemBuilder.create(plugin, "revoke-vote"));
            p.getInventory().addItem(ItemBuilder.create(plugin, "skip-day"));
        }
    }

    private void startDayPhase() {
        phase = Phase.DAY;
        phaseTimer = dayDuration;
        setWorldTime(6000);

        createBossBar(ChatColor.GOLD + "Day Time", BarColor.YELLOW);
        startActionBar();

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (phaseTimer <= 0) {
                    cancelTask();
                    taskId = -1;
                    endDayPhase();
                    return;
                }
                if (phaseTimer == 30 || phaseTimer == 10 || phaseTimer <= 5) {
                    broadcast(ChatColor.GOLD + "Day ends in " + phaseTimer + " seconds!");
                }
                updateBossBar();
                phaseTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    private void endDayPhase() {
        processVotes();
        if (checkWinCondition()) return;
        startNightPhase();
    }

    private void processVotes() {
        if (voteCounts.isEmpty()) {
            broadcast(ChatColor.YELLOW + "No votes were cast. No one is eliminated.");
            return;
        }
        UUID mostVoted = null;
        int maxVotes = 0;
        boolean tie = false;
        for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                mostVoted = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxVotes) {
                tie = true;
            }
        }
        voteCounts.clear();
        for (GamePlayer gp : players) {
            gp.resetVote();
        }
        if (tie || mostVoted == null) {
            broadcast(ChatColor.YELLOW + "The vote was tied. No one is eliminated.");
            return;
        }
        Player eliminated = Bukkit.getPlayer(mostVoted);
        if (eliminated == null) return;
        GamePlayer gp = getGamePlayer(eliminated);
        if (gp == null || !gp.isAlive()) return;
        eliminatePlayer(gp, "voted out by the village");
    }

    public void castVote(Player voter, Player target) {
        GamePlayer voterGp = getGamePlayer(voter);
        GamePlayer targetGp = getGamePlayer(target);
        if (voterGp == null || targetGp == null) return;
        if (!voterGp.isAlive() || !targetGp.isAlive()) {
            voter.sendMessage(plugin.prefix() + ChatColor.RED + "Dead players cannot vote or be voted.");
            return;
        }
        if (voterGp.hasVoted()) {
            voter.sendMessage(plugin.prefix() + ChatColor.RED + "You have already voted! Use the Revoke Vote item to change your vote.");
            return;
        }
        voterGp.setVoted(true);
        voterGp.setVotedFor(target);
        voteCounts.merge(target.getUniqueId(), 1, Integer::sum);
        voter.sendMessage(plugin.prefix() + ChatColor.GREEN + "You voted for " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + "!");
        broadcast(ChatColor.YELLOW + voter.getName() + " has voted. (" + voteCounts.getOrDefault(target.getUniqueId(), 0) + " votes for " + target.getName() + ")");
    }

    public void revokeVote(Player voter) {
        GamePlayer voterGp = getGamePlayer(voter);
        if (voterGp == null || !voterGp.isAlive()) return;
        if (!voterGp.hasVoted()) {
            voter.sendMessage(plugin.prefix() + ChatColor.RED + "You haven't voted yet!");
            return;
        }
        Player target = voterGp.getVotedFor();
        if (target != null) {
            voteCounts.merge(target.getUniqueId(), -1, Integer::sum);
            if (voteCounts.getOrDefault(target.getUniqueId(), 0) <= 0) {
                voteCounts.remove(target.getUniqueId());
            }
        }
        voterGp.resetVote();
        voter.sendMessage(plugin.prefix() + ChatColor.GREEN + "Your vote has been revoked.");
    }

    private void startNightPhase() {
        phase = Phase.NIGHT;
        phaseTimer = nightDuration;
        setWorldTime(18000);

        for (GamePlayer gp : getAlivePlayers()) {
            Player p = gp.getPlayer();
            p.getInventory().clear();
            gp.getRole().onNightStart(p);
        }

        createBossBar(ChatColor.DARK_PURPLE + "Night Time", BarColor.PURPLE);

        broadcast(ChatColor.DARK_PURPLE + "Night falls! Use your abilities wisely.");

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (phaseTimer <= 0) {
                    cancelTask();
                    taskId = -1;
                    endNightPhase();
                    return;
                }
                if (phaseTimer == 30 || phaseTimer == 10 || phaseTimer <= 5) {
                    broadcast(ChatColor.DARK_PURPLE + "Night ends in " + phaseTimer + " seconds!");
                }
                updateBossBar();
                phaseTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    private void endNightPhase() {
        processNightDeaths();
        if (checkWinCondition()) return;
        startDayPhase();
        giveDayItems();
        for (GamePlayer gp : getAlivePlayers()) {
            gp.getRole().onDayStart(gp.getPlayer());
        }
        processHunterRevenge();
    }

    private final List<GamePlayer> pendingNightDeaths = new ArrayList<>();

    public void addNightDeath(GamePlayer gp) {
        if (gp != null && gp.isAlive() && !pendingNightDeaths.contains(gp)) {
            pendingNightDeaths.add(gp);
        }
    }

    private void processNightDeaths() {
        for (GamePlayer gp : pendingNightDeaths) {
            if (gp.isAlive()) {
                eliminatePlayer(gp, "killed during the night");
            }
        }
        pendingNightDeaths.clear();
    }

    private void processHunterRevenge() {
        Iterator<Map.Entry<UUID, UUID>> it = hunterTargets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            Player hunter = Bukkit.getPlayer(entry.getKey());
            Player target = Bukkit.getPlayer(entry.getValue());
            if (hunter != null && target != null) {
                GamePlayer hunterGp = getGamePlayer(hunter);
                GamePlayer targetGp = getGamePlayer(target);
                if (hunterGp != null && !hunterGp.isAlive() && targetGp != null && targetGp.isAlive()) {
                    broadcast(ChatColor.GOLD + "The Hunter's revenge strikes! " + target.getName() + " is killed!");
                    eliminatePlayer(targetGp, "killed by the Hunter's revenge");
                }
            }
            it.remove();
        }
        if (checkWinCondition()) return;
    }

    public void eliminatePlayer(GamePlayer gp, String reason) {
        if (!gp.isAlive()) return;
        gp.setAlive(false);
        Player p = gp.getPlayer();
        p.setGameMode(GameMode.SPECTATOR);
        p.getInventory().clear();
        broadcast(ChatColor.RED + p.getName() + " has been " + reason + "!");
        broadcast(ChatColor.GRAY + "They were the " + gp.getRole().getName() + ".");

        if (gp.getRole() instanceof HunterRole) {
            HunterRole hunter = (HunterRole) gp.getRole();
            Player target = hunter.getTarget();
            if (target != null) {
                hunterTargets.put(p.getUniqueId(), target.getUniqueId());
                broadcast(ChatColor.GOLD + "The Hunter " + p.getName() + " had selected " + target.getName() + " as their target!");
            }
        }
    }

    public void werewolfKill(Player killer, Player target) {
        GamePlayer killerGp = getGamePlayer(killer);
        GamePlayer targetGp = getGamePlayer(target);
        if (killerGp == null || targetGp == null) return;
        if (!killerGp.getRole().isWerewolf()) return;
        if (!killerGp.isAlive() || !targetGp.isAlive()) return;
        if (targetGp.getRole().isWerewolf()) {
            killer.sendMessage(plugin.prefix() + ChatColor.RED + "You cannot kill a fellow werewolf!");
            return;
        }
        addNightDeath(targetGp);
        killer.sendMessage(plugin.prefix() + ChatColor.RED + "You have attacked " + target.getName() + "!");
    }

    public void witchPoison(Player witch, Player target) {
        GamePlayer witchGp = getGamePlayer(witch);
        GamePlayer targetGp = getGamePlayer(target);
        if (witchGp == null || targetGp == null) return;
        WitchRole witchRole = witchGp.asWitch();
        if (witchRole == null) return;
        if (witchRole.isPoisonUsed()) {
            witch.sendMessage(plugin.prefix() + ChatColor.RED + "You have already used your poison!");
            return;
        }
        witchRole.usePoison();
        addNightDeath(targetGp);
        witch.getInventory().removeItem(ItemBuilder.create(plugin, "witch-poison"));
        witch.sendMessage(plugin.prefix() + ChatColor.DARK_PURPLE + "You used your poison on " + target.getName() + "!");
    }

    public void witchHeal(Player witch, Player target) {
        GamePlayer witchGp = getGamePlayer(witch);
        GamePlayer targetGp = getGamePlayer(target);
        if (witchGp == null || targetGp == null) return;
        WitchRole witchRole = witchGp.asWitch();
        if (witchRole == null) return;
        if (witchRole.isHealUsed()) {
            witch.sendMessage(plugin.prefix() + ChatColor.RED + "You have already used your heal!");
            return;
        }
        witchRole.useHeal();
        pendingNightDeaths.remove(targetGp);
        target.setHealth(20);
        witch.getInventory().removeItem(ItemBuilder.create(plugin, "witch-heal"));
        witch.sendMessage(plugin.prefix() + ChatColor.GREEN + "You healed " + target.getName() + "!");
    }

    public void seerCheck(Player seer, Player target) {
        GamePlayer seerGp = getGamePlayer(seer);
        GamePlayer targetGp = getGamePlayer(target);
        if (seerGp == null || targetGp == null) return;
        SeerRole seerRole = seerGp.asSeer();
        if (seerRole == null) return;
        if (seerRole.hasCheckedTonight()) {
            seer.sendMessage(plugin.prefix() + ChatColor.RED + "You have already checked a player tonight!");
            return;
        }
        seerRole.setCheckedTonight(true);
        Team team = targetGp.getRole().getTeam();
        String teamName;
        if (targetGp.getRole().isLier()) {
            teamName = ChatColor.RED + "BAD";
        } else if (team == Team.BAD) {
            teamName = ChatColor.RED + "BAD";
        } else {
            teamName = ChatColor.GREEN + "GOOD";
        }
        seer.sendMessage(plugin.prefix() + ChatColor.BLUE + target.getName() + " is on the " + teamName + ChatColor.BLUE + " team.");
    }

    public void hunterSelectTarget(Player hunter, Player target) {
        GamePlayer hunterGp = getGamePlayer(hunter);
        GamePlayer targetGp = getGamePlayer(target);
        if (hunterGp == null || targetGp == null) return;
        HunterRole hunterRole = hunterGp.asHunter();
        if (hunterRole == null) return;
        if (hunterRole.isTargetLocked()) {
            hunter.sendMessage(plugin.prefix() + ChatColor.RED + "Your target is locked for tonight!");
            return;
        }
        hunterRole.setTarget(target);
        hunter.sendMessage(plugin.prefix() + ChatColor.GOLD + "You selected " + target.getName() + " as your target. If you die, they will die too!");
    }

    public void werewolfTransform(Player player) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;
        if (!gp.getRole().isWerewolf() && !gp.getRole().isLier()) return;
        PlayerInventory inv = player.getInventory();

        if (gp.isTransformed()) {
            inv.setHelmet(null);
            inv.setChestplate(null);
            inv.setLeggings(null);
            inv.setBoots(null);
            player.removePotionEffect(PotionEffectType.SPEED);
            inv.removeItem(ItemBuilder.create(plugin, "werewolf-axe"));
            gp.setTransformed(false);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
            player.sendMessage(plugin.prefix() + ChatColor.RED + "You untransform and vanish briefly!");
            return;
        }

        ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
        ItemStack chestplate = ItemBuilder.create(plugin, "werewolf-armor");
        ItemStack leggings = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
        inv.setHelmet(helmet);
        inv.setChestplate(chestplate);
        inv.setLeggings(leggings);
        inv.setBoots(boots);

        ItemStack axe = ItemBuilder.create(plugin, "werewolf-axe");
        if (gp.getRole().isLier()) {
            axe = ItemBuilder.rename(axe, "&4&lFake Werewolf Axe &7(Cannot kill)");
        }
        if (!inv.contains(axe)) {
            inv.addItem(axe);
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        gp.setTransformed(true);
        player.sendMessage(plugin.prefix() + ChatColor.RED + "You transform into a werewolf!");
    }

    private boolean checkWinCondition() {
        if (debugMode) return false;
        long badAlive = getAlivePlayers().stream().filter(gp -> gp.getRole().isBad()).count();
        long goodAlive = getAlivePlayers().stream().filter(gp -> gp.getRole().isGood()).count();

        if (badAlive == 0) {
            endGame("Good team", "All werewolves have been eliminated!");
            return true;
        }
        if (badAlive >= goodAlive) {
            endGame("Bad team", "The werewolves have overrun the village!");
            return true;
        }
        return false;
    }

    private void endGame(String winningTeam, String reason) {
        phase = Phase.ENDED;
        cancelTask();
        removeBossBar();
        stopActionBar();
        broadcast(ChatColor.GOLD + "===== GAME OVER =====");
        broadcast(ChatColor.YELLOW + reason);
        broadcast(ChatColor.GOLD + "The " + winningTeam + " wins!");

        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.sendMessage(plugin.prefix() + ChatColor.GRAY + "You were the " + gp.getRole().getName() + ".");
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            if (lobbyLocation != null) {
                p.teleport(lobbyLocation);
            }
        }
        revealAllRoles();
        players.clear();
        voteCounts.clear();
        hunterTargets.clear();
        pendingNightDeaths.clear();
        phase = Phase.LOBBY;
    }

    private void revealAllRoles() {
        broadcast(ChatColor.GOLD + "===== ROLE REVEAL =====");
        for (GamePlayer gp : players) {
            broadcast(ChatColor.GRAY + gp.getPlayer().getName() + " was the " + gp.getRole().getName());
        }
    }

    public void forceStop() {
        cancelTask();
        removeBossBar();
        stopActionBar();
        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            if (lobbyLocation != null) {
                p.teleport(lobbyLocation);
            }
        }
        players.clear();
        voteCounts.clear();
        hunterTargets.clear();
        pendingNightDeaths.clear();
        phase = Phase.LOBBY;
        broadcast(ChatColor.RED + "The game has been force stopped.");
    }

    private void cancelTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void setWorldTime(long time) {
        if (spawnLocation != null) {
            spawnLocation.getWorld().setTime(time);
        } else if (!players.isEmpty()) {
            players.iterator().next().getPlayer().getWorld().setTime(time);
        }
    }

    public void skipDay(Player player) {
        if (phase != Phase.DAY) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "You can only skip time during the day!");
            return;
        }
        int skipAmount = Math.max(1, phaseTimer / 3);
        phaseTimer -= skipAmount;
        broadcast(ChatColor.AQUA + player.getName() + " skipped " + skipAmount + " seconds! Day ends in " + Math.max(0, phaseTimer) + " seconds.");
        player.getInventory().removeItem(ItemBuilder.create(plugin, "skip-day"));
        updateBossBar();
        if (phaseTimer <= 0) {
            cancelTask();
            taskId = -1;
            endDayPhase();
        }
    }

    public void skipDayFromCommand() {
        if (phase != Phase.DAY) return;
        int skipAmount = Math.max(1, phaseTimer / 3);
        phaseTimer -= skipAmount;
        broadcast(ChatColor.AQUA + "Admin skipped " + skipAmount + " seconds! Day ends in " + Math.max(0, phaseTimer) + " seconds.");
        updateBossBar();
        if (phaseTimer <= 0) {
            cancelTask();
            taskId = -1;
            endDayPhase();
        }
    }

    public void skipNightFromCommand() {
        if (phase != Phase.NIGHT) return;
        int skipAmount = Math.max(1, phaseTimer / 3);
        phaseTimer -= skipAmount;
        broadcast(ChatColor.DARK_PURPLE + "Admin skipped " + skipAmount + " seconds! Night ends in " + Math.max(0, phaseTimer) + " seconds.");
        updateBossBar();
        if (phaseTimer <= 0) {
            cancelTask();
            taskId = -1;
            endNightPhase();
        }
    }

    public void forceSetRole(Player player, String roleName) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null) {
            return;
        }
        Role role;
        switch (roleName.toLowerCase()) {
            case "werewolf":
                role = new WerewolfRole();
                break;
            case "villager":
                role = new VillagerRole();
                break;
            case "witch":
                role = new WitchRole();
                break;
            case "seer":
                role = new SeerRole();
                break;
            case "hunter":
                role = new HunterRole();
                break;
            case "lier":
                role = new LierRole();
                break;
            default:
                return;
        }
        gp.setRole(role);
        player.sendMessage(plugin.prefix() + ChatColor.GOLD + "Your role has been set to: " + ChatColor.WHITE + role.getName());
        player.sendMessage(plugin.prefix() + ChatColor.GRAY + role.getDescription());

        if (phase == Phase.DAY) {
            player.getInventory().clear();
            player.getInventory().addItem(ItemBuilder.create(plugin, "vote-sword"));
            player.getInventory().addItem(ItemBuilder.create(plugin, "revoke-vote"));
            player.getInventory().addItem(ItemBuilder.create(plugin, "skip-day"));
            role.onDayStart(player);
        } else if (phase == Phase.NIGHT) {
            player.getInventory().clear();
            role.onNightStart(player);
        }
    }

    public void revealRolesToSender(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== ROLE LIST =====");
        for (GamePlayer gp : players) {
            sender.sendMessage(ChatColor.GRAY + gp.getPlayer().getName() + " - " +
                    (gp.isAlive() ? ChatColor.GREEN + "ALIVE" : ChatColor.RED + "DEAD") +
                    ChatColor.GRAY + " - " + ChatColor.WHITE + gp.getRole().getName());
        }
    }

    private void createBossBar(String title, BarColor color) {
        removeBossBar();
        bossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        for (GamePlayer gp : players) {
            bossBar.addPlayer(gp.getPlayer());
        }
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        int totalDuration = (phase == Phase.DAY) ? dayDuration : nightDuration;
        if (totalDuration <= 0) return;
        double progress = Math.max(0.0, Math.min(1.0, (double) phaseTimer / (double) totalDuration));
        bossBar.setProgress(progress);
        String phaseName = (phase == Phase.DAY) ? ChatColor.GOLD + "Day" : ChatColor.DARK_PURPLE + "Night";
        bossBar.setTitle(phaseName + ChatColor.GRAY + " - " + Math.max(0, phaseTimer) + "s");
    }

    private void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    private void startActionBar() {
        stopActionBar();
        actionBarTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (phase != Phase.DAY && phase != Phase.NIGHT) {
                    cancel();
                    actionBarTaskId = -1;
                    return;
                }
                for (GamePlayer gp : players) {
                    Player p = gp.getPlayer();
                    if (!p.isOnline()) continue;
                    String text;
                    if (gp.isAlive()) {
                        text = ChatColor.GOLD + "Role: " + ChatColor.WHITE + gp.getRole().getName() +
                                ChatColor.GRAY + " | " + ChatColor.AQUA + (phase == Phase.DAY ? "Day" : "Night") +
                                ChatColor.GRAY + " | " + (gp.isAlive() ? ChatColor.GREEN + "Alive" : ChatColor.RED + "Dead");
                    } else {
                        text = ChatColor.RED + "You are dead - Spectating" +
                                ChatColor.GRAY + " | " + ChatColor.AQUA + (phase == Phase.DAY ? "Day" : "Night");
                    }
                    p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(text));
                }
            }
        }.runTaskTimer(plugin, 10L, 20L).getTaskId();
    }

    private void stopActionBar() {
        if (actionBarTaskId != -1) {
            Bukkit.getScheduler().cancelTask(actionBarTaskId);
            actionBarTaskId = -1;
        }
    }

    public void broadcast(String message) {
        String prefixed = ColorUtil.color(message);
        for (GamePlayer gp : players) {
            gp.getPlayer().sendMessage(prefixed);
        }
    }
}
