package com.werewolf.game.arena;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.*;
import com.werewolf.game.gui.RoleSelectorGUI;
import com.werewolf.game.gui.SheriffGUI;
import com.werewolf.game.roles.*;
import com.werewolf.game.util.ColorUtil;
import com.werewolf.game.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
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
    private final Map<String, Long> abilityCooldowns = new HashMap<>();

    private int transformCooldown;
    private int ninjaCooldown;
    private int electionDuration;
    private int mermaidFreezeDuration;

    private long mermaidFreezeUntil = 0;

    private boolean debugMode = false;
    private boolean firstDay = true;
    private boolean sheriffEnabled = true;

    private final Map<UUID, Integer> sheriffElectionVotes = new HashMap<>();
    private UUID sheriffId = null;

    private final Map<String, Integer> roleSelection = new HashMap<>();
    private final Map<UUID, Integer> fakeVoteCounts = new HashMap<>();

    private BossBar bossBar = null;
    private int actionBarTaskId = -1;
    private int particleTaskId = -1;
    private final Set<UUID> particleTrailPlayers = new HashSet<>();
    private ScoreboardHelper scoreboardHelper;

    public Arena(WerewolfPlugin plugin, String name, String worldName) {
        this.plugin = plugin;
        this.name = name;
        this.worldName = worldName;
        this.minPlayers = plugin.getConfig().getInt("min-players", 4);
        this.dayDuration = plugin.getConfig().getInt("day-duration", 120);
        this.nightDuration = plugin.getConfig().getInt("night-duration", 60);
        this.lobbyDuration = plugin.getConfig().getInt("lobby-duration", 30);
        this.transformCooldown = plugin.getConfig().getInt("transform-cooldown", 10);
        this.ninjaCooldown = plugin.getConfig().getInt("ninja-cooldown", 15);
        this.electionDuration = plugin.getConfig().getInt("election-duration", 30);
        this.mermaidFreezeDuration = plugin.getConfig().getInt("mermaid-freeze-duration", 15);
        this.scoreboardHelper = new ScoreboardHelper(plugin, this);
        this.scoreboardHelper.setupLobby();

        roleSelection.put("werewolf", 1);
        roleSelection.put("villager", 1);
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getPhaseTimer() {
        return phaseTimer;
    }

    public int getTaskId() {
        return taskId;
    }

    public ScoreboardHelper getScoreboardHelper() {
        return scoreboardHelper;
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

        if (spawnLocation != null) {
            player.teleport(spawnLocation);
        } else if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
        }
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(20);
        player.setFoodLevel(20);

        if (phase == Phase.LOBBY) {
            player.getInventory().setItem(getItemSlot("role-selector"), ItemBuilder.create(plugin, "role-selector"));
        }

        if (bossBar != null) {
            bossBar.addPlayer(player);
        }

        scoreboardHelper.updateLobby();

        if (players.size() >= minPlayers && taskId == -1) {
            startLobbyCountdown();
        }
    }

    public void removePlayer(Player player) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null) return;

        players.remove(gp);
        particleTrailPlayers.remove(player.getUniqueId());
        voteCounts.remove(player.getUniqueId());
        hunterTargets.remove(player.getUniqueId());

        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        Location lobby = plugin.getArenaManager().getGlobalLobby();
        if (lobby != null) {
            player.teleport(lobby);
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }

        broadcast(ChatColor.YELLOW + player.getName() + " left the arena! (" + players.size() + "/" + 16 + ")");

        if (phase == Phase.LOBBY && taskId != -1 && players.size() < minPlayers) {
            cancelTask();
            broadcast(ChatColor.RED + "Not enough players. Countdown cancelled.");
        }

        if (phase == Phase.LOBBY) {
            scoreboardHelper.updateLobby();
        }

        if (phase == Phase.DAY || phase == Phase.NIGHT || phase == Phase.SHERIFF_ELECTION) {
            scoreboardHelper.updateGame();
            if (phase != Phase.SHERIFF_ELECTION) {
                checkWinCondition();
            }
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
                    scoreboardHelper.updateLobby();
                    return;
                }
                if (phaseTimer <= 0) {
                    cancelTask();
                    taskId = -1;
                    startCountdownTitle();
                    return;
                }
                if (phaseTimer <= 10 || phaseTimer % 30 == 0) {
                    broadcast(ChatColor.GOLD + "Game starting in " + phaseTimer + " seconds!");
                }
                scoreboardHelper.updateLobby();
                phaseTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    private void startCountdownTitle() {
        final int[] count = {3};
        broadcast(ChatColor.GOLD + "Game starting!");
        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.sendTitle(ChatColor.GOLD + "" + count[0], "", 0, 20, 0);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                count[0]--;
                if (count[0] > 0) {
                    for (GamePlayer gp : players) {
                        Player p = gp.getPlayer();
                        p.sendTitle(ChatColor.GOLD + "" + count[0], "", 0, 20, 0);
                    }
                } else {
                    cancel();
                    startGame();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void startGame() {
        assignRoles();
        teleportPlayersToSpawn();
        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.setGameMode(GameMode.ADVENTURE);
            p.setHealth(20);
            p.setFoodLevel(20);
            p.sendMessage(plugin.prefix() + ChatColor.GOLD + "Your role: " + ChatColor.WHITE + gp.getRole().getName());
            p.sendMessage(plugin.prefix() + ChatColor.GRAY + gp.getRole().getDescription());
        }
        startParticleTask();
        if (sheriffEnabled) {
            startSheriffElection();
        } else {
            broadcast(ChatColor.YELLOW + "Sheriff election is disabled. The game goes straight to day!");
            firstDay = true;
            giveDayItems();
            scoreboardHelper.setupGame();
            for (GamePlayer gp : getAlivePlayers()) {
                gp.getRole().onDayStart(gp.getPlayer());
            }
            startDayPhase();
        }
    }

    private void startSheriffElection() {
        phase = Phase.SHERIFF_ELECTION;
        phaseTimer = electionDuration;
        setWorldTime(6000);
        broadcast(ChatColor.GOLD + "===== SHERIFF ELECTION =====");
        broadcast(ChatColor.YELLOW + "Vote for who should become the Sheriff! The Sheriff gets 2 votes during daytime voting.");
        broadcast(ChatColor.YELLOW + "Right-click the Vote Sheriff item to open the voting menu.");

        for (GamePlayer gp : getAlivePlayers()) {
            Player p = gp.getPlayer();
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
            p.getInventory().clear();
            p.getInventory().setItem(getItemSlot("vote-sheriff"), ItemBuilder.create(plugin, "vote-sheriff"));
            giveInfoItems(gp);
        }

        createBossBar(ChatColor.GOLD + "Sheriff Election", BarColor.YELLOW);
        scoreboardHelper.setupGame();

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (phaseTimer <= 0) {
                    cancelTask();
                    taskId = -1;
                    endSheriffElection();
                    return;
                }
                if (phaseTimer == 30 || phaseTimer == 10 || phaseTimer <= 5) {
                    broadcast(ChatColor.GOLD + "Sheriff election ends in " + phaseTimer + " seconds!");
                }
                updateBossBar();
                scoreboardHelper.updateGame();
                phaseTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    private void endSheriffElection() {
        UUID electedId = null;
        int maxVotes = 0;
        boolean tie = false;
        for (Map.Entry<UUID, Integer> entry : sheriffElectionVotes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                electedId = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxVotes) {
                tie = true;
            }
        }
        sheriffElectionVotes.clear();

        if (tie || electedId == null || maxVotes == 0) {
            broadcast(ChatColor.YELLOW + "The sheriff election was tied or no votes were cast. No sheriff elected.");
        } else {
            Player sheriff = Bukkit.getPlayer(electedId);
            if (sheriff != null) {
                GamePlayer sheriffGp = getGamePlayer(sheriff);
                if (sheriffGp != null) {
                    sheriffGp.setSheriff(true);
                    sheriffId = electedId;
                    broadcast(ChatColor.GOLD + "===== SHERIFF ELECTED =====");
                    broadcast(ChatColor.YELLOW + sheriff.getName() + " is now the Sheriff! They have 2 votes during daytime voting.");
                    sheriff.sendMessage(plugin.prefix() + ChatColor.GOLD + "You are the Sheriff! Your votes count as 2 during daytime voting.");
                }
            }
        }

        firstDay = true;
        broadcast(ChatColor.GREEN + "The game has begun! It is DAY time. Discuss and get to know each other!");
        giveDayItems();
        scoreboardHelper.updateGame();
        for (GamePlayer gp : getAlivePlayers()) {
            gp.getRole().onDayStart(gp.getPlayer());
        }
        startDayPhase();
    }

    public void castSheriffVote(Player voter, Player target) {
        GamePlayer voterGp = getGamePlayer(voter);
        GamePlayer targetGp = getGamePlayer(target);
        if (voterGp == null || targetGp == null) return;
        if (phase != Phase.SHERIFF_ELECTION) return;
        if (!voterGp.isAlive() || !targetGp.isAlive()) {
            voter.sendMessage(plugin.prefix() + ChatColor.RED + "Dead players cannot vote or be voted.");
            return;
        }
        if (voterGp.hasVoted()) {
            voter.sendMessage(plugin.prefix() + ChatColor.RED + "You have already voted! Right-click the Vote Sheriff item to change your vote.");
            return;
        }
        voterGp.setVoted(true);
        voterGp.setVotedFor(target);
        sheriffElectionVotes.merge(target.getUniqueId(), 1, Integer::sum);
        voter.sendMessage(plugin.prefix() + ChatColor.GREEN + "You voted for " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + " for Sheriff!");
        broadcast(ChatColor.YELLOW + voter.getName() + " has voted in the sheriff election.");
    }

    public void revokeSheriffVote(Player voter) {
        GamePlayer voterGp = getGamePlayer(voter);
        if (voterGp == null || !voterGp.isAlive()) return;
        if (phase != Phase.SHERIFF_ELECTION) return;
        if (!voterGp.hasVoted()) {
            voter.sendMessage(plugin.prefix() + ChatColor.RED + "You haven't voted yet!");
            return;
        }
        Player target = voterGp.getVotedFor();
        if (target != null) {
            sheriffElectionVotes.merge(target.getUniqueId(), -1, Integer::sum);
            if (sheriffElectionVotes.getOrDefault(target.getUniqueId(), 0) <= 0) {
                sheriffElectionVotes.remove(target.getUniqueId());
            }
        }
        voterGp.resetVote();
        voter.sendMessage(plugin.prefix() + ChatColor.GREEN + "Your sheriff election vote has been revoked.");
    }

    public Map<String, Integer> getRoleSelection() {
        return roleSelection;
    }

    public void adjustRoleSelection(String roleKey, int delta) {
        int current = roleSelection.getOrDefault(roleKey, 0);
        int newValue = Math.max(0, current + delta);
        if (!debugMode && (roleKey.equals("werewolf") || roleKey.equals("villager")) && newValue < 1) {
            return;
        }
        roleSelection.put(roleKey, newValue);
    }

    public void openRoleSelector(Player player) {
        RoleSelectorGUI.open(player, roleSelection, sheriffEnabled);
    }

    public boolean isSheriffEnabled() {
        return sheriffEnabled;
    }

    public void setSheriffEnabled(boolean enabled) {
        this.sheriffEnabled = enabled;
    }

    private void assignRoles() {
        List<GamePlayer> playerList = new ArrayList<>(players);
        Collections.shuffle(playerList);

        int total = playerList.size();

        int werewolfCount = roleSelection.getOrDefault("werewolf", 0);
        int villagerCount = roleSelection.getOrDefault("villager", 0);
        int witchCount = roleSelection.getOrDefault("witch", 0);
        int seerCount = roleSelection.getOrDefault("seer", 0);
        int hunterCount = roleSelection.getOrDefault("hunter", 0);
        int tricksterCount = roleSelection.getOrDefault("trickster", 0);
        int ninjaCount = roleSelection.getOrDefault("ninja", 0);
        int mermaidCount = roleSelection.getOrDefault("mermaid", 0);
        int masochistCount = roleSelection.getOrDefault("masochist", 0);

        int selectedTotal = werewolfCount + villagerCount + witchCount + seerCount + hunterCount + tricksterCount + ninjaCount + mermaidCount + masochistCount;
        if (selectedTotal > total) {
            int overflow = selectedTotal - total;
            if (masochistCount >= overflow) { masochistCount -= overflow; overflow = 0; } else { overflow -= masochistCount; masochistCount = 0; }
            if (overflow > 0 && mermaidCount >= overflow) { mermaidCount -= overflow; overflow = 0; } else { overflow -= mermaidCount; mermaidCount = 0; }
            if (overflow > 0 && ninjaCount >= overflow) { ninjaCount -= overflow; overflow = 0; } else { overflow -= ninjaCount; ninjaCount = 0; }
            if (overflow > 0 && tricksterCount >= overflow) { tricksterCount -= overflow; overflow = 0; } else { overflow -= tricksterCount; tricksterCount = 0; }
            if (overflow > 0 && hunterCount >= overflow) { hunterCount -= overflow; overflow = 0; } else { overflow -= hunterCount; hunterCount = 0; }
            if (overflow > 0 && seerCount >= overflow) { seerCount -= overflow; overflow = 0; } else { overflow -= seerCount; seerCount = 0; }
            if (overflow > 0 && witchCount >= overflow) { witchCount -= overflow; overflow = 0; } else { overflow -= witchCount; witchCount = 0; }
            if (overflow > 0 && villagerCount >= overflow) { villagerCount -= overflow; overflow = 0; } else { overflow -= villagerCount; villagerCount = 0; }
            if (overflow > 0 && werewolfCount >= overflow) { werewolfCount -= overflow; overflow = 0; } else { overflow -= werewolfCount; werewolfCount = 0; }
        }

        if (!debugMode) {
            if (werewolfCount < 1) werewolfCount = 1;
            if (villagerCount < 1) villagerCount = 1;
        }

        int index = 0;
        for (int i = 0; i < werewolfCount && index < total; i++) {
            playerList.get(index++).setRole(new WerewolfRole());
        }
        for (int i = 0; i < tricksterCount && index < total; i++) {
            playerList.get(index++).setRole(new TricksterRole());
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
        for (int i = 0; i < ninjaCount && index < total; i++) {
            playerList.get(index++).setRole(new NinjaRole());
        }
        for (int i = 0; i < mermaidCount && index < total; i++) {
            playerList.get(index++).setRole(new MermaidRole());
        }
        for (int i = 0; i < masochistCount && index < total; i++) {
            playerList.get(index++).setRole(new MasochistRole());
        }
        while (index < total) {
            playerList.get(index++).setRole(new VillagerRole());
        }

        List<String> werewolfNames = new ArrayList<>();
        for (GamePlayer gp : players) {
            if (gp.getRole().isWerewolf() || gp.getRole().isTrickster()) {
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
            if (!firstDay) {
                p.getInventory().setItem(getItemSlot("vote-sword"), ItemBuilder.create(plugin, "vote-sword"));
                p.getInventory().setItem(getItemSlot("revoke-vote"), ItemBuilder.create(plugin, "revoke-vote"));
            }
            p.getInventory().setItem(getItemSlot("skip-day"), ItemBuilder.create(plugin, "skip-day"));
            giveInfoItems(gp);
        }
    }

    private void giveInfoItems(GamePlayer gp) {
        Player p = gp.getPlayer();
        p.getInventory().setItem(getItemSlot("setup-info"), ItemBuilder.create(plugin, "setup-info"));
        p.getInventory().setItem(getItemSlot("role-info-book"), ItemBuilder.create(plugin, "role-info-book"));
        if (gp.getRole().isWerewolf()) {
            p.getInventory().setItem(getItemSlot("wolf-team"), ItemBuilder.create(plugin, "wolf-team"));
        }
    }

    private int getItemSlot(String itemKey) {
        return plugin.getConfig().getInt("items." + itemKey + ".slot", 0);
    }

    private void startDayPhase() {
        phase = Phase.DAY;
        phaseTimer = dayDuration;
        setWorldTime(6000);

        for (GamePlayer gp : getAlivePlayers()) {
            Player p = gp.getPlayer();
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
            gp.setTransformed(false);
        }
        particleTrailPlayers.clear();

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
                scoreboardHelper.updateGame();
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
        for (Map.Entry<UUID, Integer> entry : fakeVoteCounts.entrySet()) {
            voteCounts.merge(entry.getKey(), -entry.getValue(), Integer::sum);
            if (voteCounts.getOrDefault(entry.getKey(), 0) <= 0) {
                voteCounts.remove(entry.getKey());
            }
        }
        fakeVoteCounts.clear();
        scoreboardHelper.updateVotes(voteCounts);
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
        scoreboardHelper.updateVotes(voteCounts);
        if (tie || mostVoted == null) {
            broadcast(ChatColor.YELLOW + "The vote was tied. No one is eliminated.");
            return;
        }
        Player eliminated = Bukkit.getPlayer(mostVoted);
        if (eliminated == null) return;
        GamePlayer gp = getGamePlayer(eliminated);
        if (gp == null || !gp.isAlive()) return;
        if (gp.getRole().isMasochist()) {
            broadcast(ChatColor.GOLD + "===== MASOCHIST WINS =====");
            broadcast(ChatColor.YELLOW + eliminated.getName() + " received the most votes and was the Masochist! They win!");
            endGame("Masochist", eliminated.getName() + " (Masochist) received the most votes and wins!");
            return;
        }
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
        if (voterGp.getRole().isMasochist()) {
            voterGp.setVoted(true);
            voterGp.setVotedFor(target);
            voteCounts.merge(target.getUniqueId(), 1, Integer::sum);
            fakeVoteCounts.merge(target.getUniqueId(), 1, Integer::sum);
            voter.sendMessage(plugin.prefix() + ChatColor.GREEN + "You voted for " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + "!");
            voter.sendMessage(plugin.prefix() + ChatColor.DARK_GRAY + "Your vote does not count.");
            broadcast(ChatColor.YELLOW + voter.getName() + " has voted. (" + voteCounts.getOrDefault(target.getUniqueId(), 0) + " votes for " + target.getName() + ")");
            scoreboardHelper.updateVotes(voteCounts);
            return;
        }
        voterGp.setVoted(true);
        voterGp.setVotedFor(target);
        int voteWeight = voterGp.isSheriff() ? 2 : 1;
        voteCounts.merge(target.getUniqueId(), voteWeight, Integer::sum);
        voter.sendMessage(plugin.prefix() + ChatColor.GREEN + "You voted for " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + "!" + (voterGp.isSheriff() ? ChatColor.GOLD + " (Sheriff: 2 votes)" : ""));
        broadcast(ChatColor.YELLOW + voter.getName() + " has voted. (" + voteCounts.getOrDefault(target.getUniqueId(), 0) + " votes for " + target.getName() + ")");
        scoreboardHelper.updateVotes(voteCounts);
    }

    public void revokeVote(Player voter) {
        GamePlayer voterGp = getGamePlayer(voter);
        if (voterGp == null || !voterGp.isAlive()) return;
        if (!voterGp.hasVoted()) {
            voter.sendMessage(plugin.prefix() + ChatColor.RED + "You haven't voted yet!");
            return;
        }
        Player target = voterGp.getVotedFor();
        if (target != null && voterGp.getRole().isMasochist()) {
            voteCounts.merge(target.getUniqueId(), -1, Integer::sum);
            fakeVoteCounts.merge(target.getUniqueId(), -1, Integer::sum);
            if (voteCounts.getOrDefault(target.getUniqueId(), 0) <= 0) {
                voteCounts.remove(target.getUniqueId());
            }
            if (fakeVoteCounts.getOrDefault(target.getUniqueId(), 0) <= 0) {
                fakeVoteCounts.remove(target.getUniqueId());
            }
        } else if (target != null) {
            int voteWeight = voterGp.isSheriff() ? 2 : 1;
            voteCounts.merge(target.getUniqueId(), -voteWeight, Integer::sum);
            if (voteCounts.getOrDefault(target.getUniqueId(), 0) <= 0) {
                voteCounts.remove(target.getUniqueId());
            }
        }
        voterGp.resetVote();
        voter.sendMessage(plugin.prefix() + ChatColor.GREEN + "Your vote has been revoked.");
        scoreboardHelper.updateVotes(voteCounts);
    }

    private void startNightPhase() {
        phase = Phase.NIGHT;
        phaseTimer = nightDuration;
        setWorldTime(18000);

        for (GamePlayer gp : getAlivePlayers()) {
            Player p = gp.getPlayer();
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
            gp.setTransformed(false);
            p.getInventory().clear();
            gp.getRole().onNightStart(p);
            giveInfoItems(gp);
        }
        particleTrailPlayers.clear();

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
                scoreboardHelper.updateGame();
                phaseTimer--;
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    private void endNightPhase() {
        processNightDeaths();
        if (checkWinCondition()) return;
        firstDay = false;
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
        particleTrailPlayers.remove(gp.getPlayer().getUniqueId());
        Player p = gp.getPlayer();
        p.setGameMode(GameMode.SPECTATOR);
        p.getInventory().clear();
        broadcast(ChatColor.RED + p.getName() + " has been " + reason + "!");

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
        eliminatePlayer(targetGp, "killed by a werewolf");
        killer.sendMessage(plugin.prefix() + ChatColor.RED + "You have killed " + target.getName() + "!");
        checkWinCondition();
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
        if (targetGp.getRole().isTrickster()) {
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
        if (!gp.getRole().isWerewolf() && !gp.getRole().isTrickster()) return;
        String cooldownKey = player.getUniqueId() + ":transform";
        if (isOnCooldown(cooldownKey, transformCooldown, player)) return;
        setCooldown(cooldownKey);
        PlayerInventory inv = player.getInventory();

        if (gp.isTransformed()) {
            inv.setHelmet(null);
            inv.setChestplate(null);
            inv.setLeggings(null);
            inv.setBoots(null);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            inv.removeItem(ItemBuilder.create(plugin, "werewolf-axe"));
            gp.setTransformed(false);
            particleTrailPlayers.remove(player.getUniqueId());
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
        if (gp.getRole().isTrickster()) {
            axe = ItemBuilder.rename(axe, "&4&lFake Werewolf Axe &7(Cannot kill)");
        }
        if (!inv.contains(axe)) {
            inv.addItem(axe);
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        particleTrailPlayers.add(player.getUniqueId());
        gp.setTransformed(true);
        player.sendMessage(plugin.prefix() + ChatColor.RED + "You transform into a werewolf!");
    }

    private boolean checkWinCondition() {
        if (debugMode) return false;
        if (players.isEmpty()) return true;
        Set<GamePlayer> alive = getAlivePlayers();
        boolean badAlive = alive.stream().anyMatch(gp -> gp.getRole().isBad());
        boolean goodAlive = alive.stream().anyMatch(gp -> gp.getRole().isGood());
        if (alive.isEmpty()) {
            endGame("Draw", "Everyone has been eliminated!");
            return true;
        }
        if (!badAlive) {
            endGame("Good team", "All werewolves have been eliminated!");
            return true;
        }
        if (!goodAlive) {
            endGame("Bad team", "All villagers have been eliminated!");
            return true;
        }
        return false;
    }

    private void endGame(String winningTeam, String reason) {
        phase = Phase.ENDED;
        cancelTask();
        removeBossBar();
        stopActionBar();
        stopParticleTask();
        broadcast(ChatColor.GOLD + "===== GAME OVER =====");
        broadcast(ChatColor.YELLOW + reason);
        broadcast(ChatColor.GOLD + "The " + winningTeam + " wins!");

        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.sendMessage(plugin.prefix() + ChatColor.GRAY + "You were the " + gp.getRole().getName() + ".");
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setHelmet(null);
            p.getInventory().setChestplate(null);
            p.getInventory().setLeggings(null);
            p.getInventory().setBoots(null);
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            Location lobby = plugin.getArenaManager().getGlobalLobby();
            if (lobby != null) {
                p.teleport(lobby);
            } else if (lobbyLocation != null) {
                p.teleport(lobbyLocation);
            }
        }
        revealAllRoles();
        players.clear();
        voteCounts.clear();
        sheriffElectionVotes.clear();
        hunterTargets.clear();
        pendingNightDeaths.clear();
        abilityCooldowns.clear();
        roleSelection.clear();
        fakeVoteCounts.clear();
        sheriffId = null;
        mermaidFreezeUntil = 0;
        phase = Phase.LOBBY;
        scoreboardHelper.setupLobby();
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
        stopParticleTask();
        for (GamePlayer gp : players) {
            Player p = gp.getPlayer();
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setHelmet(null);
            p.getInventory().setChestplate(null);
            p.getInventory().setLeggings(null);
            p.getInventory().setBoots(null);
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            Location lobby = plugin.getArenaManager().getGlobalLobby();
            if (lobby != null) {
                p.teleport(lobby);
            } else if (lobbyLocation != null) {
                p.teleport(lobbyLocation);
            }
        }
        players.clear();
        voteCounts.clear();
        sheriffElectionVotes.clear();
        hunterTargets.clear();
        pendingNightDeaths.clear();
        abilityCooldowns.clear();
        roleSelection.clear();
        fakeVoteCounts.clear();
        sheriffId = null;
        mermaidFreezeUntil = 0;
        phase = Phase.LOBBY;
        scoreboardHelper.setupLobby();
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
        phaseTimer = 0;
        broadcast(ChatColor.AQUA + "Admin skipped the remaining day time!");
        updateBossBar();
        cancelTask();
        taskId = -1;
        endDayPhase();
    }

    public void skipNightFromCommand() {
        if (phase != Phase.NIGHT) return;
        phaseTimer = 0;
        broadcast(ChatColor.DARK_PURPLE + "Admin skipped the remaining night time!");
        updateBossBar();
        cancelTask();
        taskId = -1;
        endNightPhase();
    }

    public void skipElectionFromCommand() {
        if (phase != Phase.SHERIFF_ELECTION) return;
        phaseTimer = 0;
        broadcast(ChatColor.GOLD + "Admin skipped the remaining election time!");
        updateBossBar();
        cancelTask();
        taskId = -1;
        endSheriffElection();
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
            case "trickster":
                role = new TricksterRole();
                break;
            case "ninja":
                role = new NinjaRole();
                break;
            case "mermaid":
                role = new MermaidRole();
                break;
            case "masochist":
                role = new MasochistRole();
                break;
            default:
                return;
        }
        gp.setRole(role);
        player.sendMessage(plugin.prefix() + ChatColor.GOLD + "Your role has been set to: " + ChatColor.WHITE + role.getName());
        player.sendMessage(plugin.prefix() + ChatColor.GRAY + role.getDescription());

        if (phase == Phase.SHERIFF_ELECTION) {
            player.getInventory().clear();
            player.getInventory().setItem(getItemSlot("vote-sheriff"), ItemBuilder.create(plugin, "vote-sheriff"));
            giveInfoItems(gp);
        } else if (phase == Phase.DAY) {
            player.getInventory().clear();
            if (!firstDay) {
                player.getInventory().setItem(getItemSlot("vote-sword"), ItemBuilder.create(plugin, "vote-sword"));
                player.getInventory().setItem(getItemSlot("revoke-vote"), ItemBuilder.create(plugin, "revoke-vote"));
            }
            player.getInventory().setItem(getItemSlot("skip-day"), ItemBuilder.create(plugin, "skip-day"));
            giveInfoItems(gp);
            role.onDayStart(player);
        } else if (phase == Phase.NIGHT) {
            player.getInventory().clear();
            role.onNightStart(player);
            giveInfoItems(gp);
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

    public void sendSetupInfo(Player player) {
        int total = players.size();
        long werewolves = players.stream().filter(gp -> gp.getRole().isWerewolf()).count();
        long tricksters = players.stream().filter(gp -> gp.getRole().isTrickster()).count();
        long witches = players.stream().filter(gp -> gp.getRole() instanceof com.werewolf.game.roles.WitchRole).count();
        long seers = players.stream().filter(gp -> gp.getRole() instanceof com.werewolf.game.roles.SeerRole).count();
        long hunters = players.stream().filter(gp -> gp.getRole() instanceof com.werewolf.game.roles.HunterRole).count();
        long villagers = players.stream().filter(gp -> gp.getRole() instanceof com.werewolf.game.roles.VillagerRole).count();
        long ninjas = players.stream().filter(gp -> gp.getRole().isNinja()).count();
        long mermaids = players.stream().filter(gp -> gp.getRole().isMermaid()).count();
        long masochists = players.stream().filter(gp -> gp.getRole().isMasochist()).count();

        int dayDur = plugin.getConfig().getInt("day-duration", 120);
        int nightDur = plugin.getConfig().getInt("night-duration", 60);

        player.sendMessage(plugin.prefix() + ChatColor.DARK_AQUA + "===== Game Setup =====");
        player.sendMessage(ChatColor.RED + "Werewolves: " + ChatColor.WHITE + werewolves);
        player.sendMessage(ChatColor.GOLD + "Tricksters: " + ChatColor.WHITE + tricksters);
        player.sendMessage(ChatColor.DARK_PURPLE + "Witches: " + ChatColor.WHITE + witches);
        player.sendMessage(ChatColor.BLUE + "Seers: " + ChatColor.WHITE + seers);
        player.sendMessage(ChatColor.GOLD + "Hunters: " + ChatColor.WHITE + hunters);
        player.sendMessage(ChatColor.GREEN + "Villagers: " + ChatColor.WHITE + villagers);
        player.sendMessage(ChatColor.DARK_PURPLE + "Ninjas: " + ChatColor.WHITE + ninjas);
        player.sendMessage(ChatColor.AQUA + "Mermaids: " + ChatColor.WHITE + mermaids);
        player.sendMessage(ChatColor.DARK_GREEN + "Masochists: " + ChatColor.WHITE + masochists);
        player.sendMessage(ChatColor.WHITE + "Total Players: " + ChatColor.WHITE + total);
        player.sendMessage(ChatColor.YELLOW + "Day Duration: " + ChatColor.WHITE + dayDur + " seconds");
        player.sendMessage(ChatColor.BLUE + "Night Duration: " + ChatColor.WHITE + nightDur + " seconds");
    }

    public void sendWolfTeamInfo(Player player) {
        player.sendMessage(plugin.prefix() + ChatColor.DARK_RED + "===== Wolf Team =====");
        boolean any = false;
        for (GamePlayer gp : players) {
            if (gp.getRole().isWerewolf() || gp.getRole().isTrickster()) {
                any = true;
                String status = gp.isAlive() ? ChatColor.GREEN + "Alive" : ChatColor.RED + "Dead";
                player.sendMessage(ChatColor.RED + gp.getPlayer().getName() + ChatColor.GRAY + " - " + status);
            }
        }
        if (!any) {
            player.sendMessage(ChatColor.GRAY + "No wolf team members.");
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
        int totalDuration;
        String phaseName;
        if (phase == Phase.SHERIFF_ELECTION) {
            totalDuration = electionDuration;
            phaseName = ChatColor.GOLD + "Election";
        } else if (phase == Phase.DAY) {
            totalDuration = dayDuration;
            phaseName = ChatColor.GOLD + "Day";
        } else if (phase == Phase.NIGHT) {
            totalDuration = nightDuration;
            phaseName = ChatColor.DARK_PURPLE + "Night";
        } else {
            return;
        }
        if (totalDuration <= 0) return;
        double progress = Math.max(0.0, Math.min(1.0, (double) phaseTimer / (double) totalDuration));
        bossBar.setProgress(progress);
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
                if (phase != Phase.DAY && phase != Phase.NIGHT && phase != Phase.SHERIFF_ELECTION) {
                    cancel();
                    actionBarTaskId = -1;
                    return;
                }
                for (GamePlayer gp : players) {
                    Player p = gp.getPlayer();
                    if (!p.isOnline()) continue;
                    String text;
                    if (phase == Phase.SHERIFF_ELECTION) {
                        text = ChatColor.GOLD + "Sheriff Election" + ChatColor.GRAY + " | " + Math.max(0, phaseTimer) + "s";
                    } else if (gp.isAlive()) {
                        text = ChatColor.GOLD + "Role: " + ChatColor.WHITE + gp.getRole().getName() +
                                ChatColor.GRAY + " | " + ChatColor.AQUA + (phase == Phase.DAY ? "Day" : "Night") +
                                ChatColor.GRAY + " | " + (gp.isSheriff() ? ChatColor.GOLD + "Sheriff (2x votes)" : ChatColor.GREEN + "Alive");
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

    public void ninjaSelectAbility(Player player, String ability) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;
        NinjaRole ninjaRole = gp.asNinja();
        if (ninjaRole == null) return;
        if (ninjaRole.hasUsedAbilityTonight()) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "You have already used your ability tonight!");
            return;
        }
        ninjaRole.setSelectedAbility(ability);
        player.getInventory().setItem(getItemSlot("ninja-ability"), ItemBuilder.create(plugin, "ninja-ability"));
        String abilityName = ability.substring(0, 1).toUpperCase() + ability.substring(1);
        player.sendMessage(plugin.prefix() + ChatColor.DARK_PURPLE + "You selected: " + ChatColor.WHITE + abilityName + ChatColor.DARK_PURPLE + "! Right-click your Ninja Orb to activate it.");
    }

    public void ninjaExecuteAbility(Player player) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;
        NinjaRole ninjaRole = gp.asNinja();
        if (ninjaRole == null) return;
        String cooldownKey = player.getUniqueId() + ":ninja";
        if (isOnCooldown(cooldownKey, ninjaCooldown, player)) return;
        if (ninjaRole.hasUsedAbilityTonight()) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "You have already used your ability tonight!");
            return;
        }
        String ability = ninjaRole.getSelectedAbility();
        if (ability == null) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "Use your Ninja Book to select an ability first!");
            return;
        }
        setCooldown(cooldownKey);
        ninjaRole.setAbilityUsedTonight(true);
        player.getInventory().removeItem(ItemBuilder.create(plugin, "ninja-book"));
        player.getInventory().removeItem(ItemBuilder.create(plugin, "ninja-ability"));

        int durationTicks = 160;

        switch (ability) {
            case "vanish":
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0, false, false));
                particleTrailPlayers.add(player.getUniqueId());
                player.sendMessage(plugin.prefix() + ChatColor.DARK_PURPLE + "You vanish into the shadows for 8 seconds!");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        particleTrailPlayers.remove(player.getUniqueId());
                    }
                }.runTaskLater(plugin, durationTicks);
                break;
            case "sprint":
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 4, false, false));
                player.sendMessage(plugin.prefix() + ChatColor.DARK_PURPLE + "You feel a burst of speed for 8 seconds!");
                break;
            case "decoy":
                Location loc = player.getLocation();
                ArmorStand decoy = loc.getWorld().spawn(loc, ArmorStand.class);
                decoy.setVisible(false);
                decoy.setCustomName(player.getName());
                decoy.setCustomNameVisible(true);
                decoy.setGravity(false);
                decoy.setMarker(true);
                player.sendMessage(plugin.prefix() + ChatColor.DARK_PURPLE + "You spawned a decoy for 8 seconds!");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (decoy != null && !decoy.isDead()) {
                            decoy.remove();
                        }
                    }
                }.runTaskLater(plugin, durationTicks);
                break;
            case "disguise":
                ItemStack fakeHelmet = new ItemStack(Material.NETHERITE_HELMET);
                ItemStack fakeChest = new ItemStack(Material.NETHERITE_CHESTPLATE);
                ItemStack fakeLegs = new ItemStack(Material.NETHERITE_LEGGINGS);
                ItemStack fakeBoots = new ItemStack(Material.NETHERITE_BOOTS);
                PlayerInventory inv = player.getInventory();
                ItemStack oldHelmet = inv.getHelmet();
                ItemStack oldChest = inv.getChestplate();
                ItemStack oldLegs = inv.getLeggings();
                ItemStack oldBoots = inv.getBoots();
                inv.setHelmet(fakeHelmet);
                inv.setChestplate(fakeChest);
                inv.setLeggings(fakeLegs);
                inv.setBoots(fakeBoots);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0, false, false));
                particleTrailPlayers.add(player.getUniqueId());
                player.sendMessage(plugin.prefix() + ChatColor.DARK_PURPLE + "You disguise as a wolf for 8 seconds! You look like a werewolf but won't appear on the wolf team list.");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            inv.setHelmet(oldHelmet);
                            inv.setChestplate(oldChest);
                            inv.setLeggings(oldLegs);
                            inv.setBoots(oldBoots);
                        }
                        particleTrailPlayers.remove(player.getUniqueId());
                    }
                }.runTaskLater(plugin, durationTicks);
                break;
            default:
                break;
        }
    }

    public void mermaidSing(Player player) {
        GamePlayer gp = getGamePlayer(player);
        if (gp == null || !gp.isAlive()) return;
        MermaidRole mermaidRole = gp.asMermaid();
        if (mermaidRole == null) return;
        if (mermaidRole.hasSungTonight()) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "You have already sung tonight!");
            return;
        }
        mermaidRole.setSungTonight(true);
        mermaidFreezeUntil = System.currentTimeMillis() + (mermaidFreezeDuration * 1000L);
        player.getInventory().removeItem(ItemBuilder.create(plugin, "mermaid-shell"));
        player.sendMessage(plugin.prefix() + ChatColor.AQUA + "You sing the Mermaid's song! The werewolves are frozen in place for " + mermaidFreezeDuration + " seconds!");
        broadcast(ChatColor.AQUA + "A haunting melody echoes through the night... The werewolves are frozen in place!");
        for (GamePlayer wgp : players) {
            if (wgp.isAlive() && wgp.getRole().isWerewolf()) {
                wgp.getPlayer().sendMessage(plugin.prefix() + ChatColor.AQUA + "The Mermaid's song has frozen you! You cannot move for " + mermaidFreezeDuration + " seconds!");
            }
        }
    }

    public boolean isMermaidFreezeActive() {
        return mermaidFreezeUntil > 0 && System.currentTimeMillis() < mermaidFreezeUntil;
    }

    private boolean isOnCooldown(String key, int cooldownSeconds, Player player) {
        Long lastUsed = abilityCooldowns.get(key);
        if (lastUsed == null) return false;
        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000L;
        long remaining = cooldownSeconds - elapsed;
        if (remaining > 0) {
            player.sendMessage(plugin.prefix() + ChatColor.RED + "Ability on cooldown! " + remaining + "s remaining.");
            return true;
        }
        return false;
    }

    private void setCooldown(String key) {
        abilityCooldowns.put(key, System.currentTimeMillis());
    }

    private void startParticleTask() {
        particleTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID id : particleTrailPlayers) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null && p.isOnline()) {
                        Location loc = p.getLocation().add(0, 1, 0);
                        p.getWorld().spawnParticle(Particle.REDSTONE, loc, 5, 0.3, 0.6, 0.3, new Particle.DustOptions(Color.RED, 1.0f));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L).getTaskId();
    }

    private void stopParticleTask() {
        if (particleTaskId != -1) {
            Bukkit.getScheduler().cancelTask(particleTaskId);
            particleTaskId = -1;
        }
        particleTrailPlayers.clear();
    }

    public void broadcast(String message) {
        String prefixed = ColorUtil.color(message);
        for (GamePlayer gp : players) {
            gp.getPlayer().sendMessage(prefixed);
        }
    }

}
