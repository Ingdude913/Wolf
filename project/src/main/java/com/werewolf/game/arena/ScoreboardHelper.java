package com.werewolf.game.arena;

import com.werewolf.game.WerewolfPlugin;
import com.werewolf.game.game.GamePlayer;
import com.werewolf.game.game.Phase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardHelper {

    private final WerewolfPlugin plugin;
    private final Arena arena;
    private Scoreboard scoreboard;
    private Objective objective;

    private static final String LOBBY_OBJ = "ww_lobby";
    private static final String GAME_OBJ = "ww_game";

    private final Map<UUID, Integer> voteCounts = new HashMap<>();

    private static final String[] ENTRIES = {
        "\u00A7a\u00A7a", "\u00A7b\u00A7b", "\u00A7c\u00A7c", "\u00A7d\u00A7d",
        "\u00A7e\u00A7e", "\u00A7f\u00A7f", "\u00A7a\u00A7b", "\u00A7a\u00A7c",
        "\u00A7a\u00A7d", "\u00A7a\u00A7e", "\u00A7a\u00A7f", "\u00A7b\u00A7c",
        "\u00A7b\u00A7d", "\u00A7b\u00A7e", "\u00A7b\u00A7f", "\u00A7c\u00A7d"
    };

    public ScoreboardHelper(WerewolfPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void setupLobby() {
        if (objective != null) {
            objective.unregister();
        }
        objective = scoreboard.registerNewObjective(LOBBY_OBJ, "dummy",
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "WEREWOLF");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        updateLobby();
    }

    public void updateLobby() {
        if (objective == null || !objective.getName().equals(LOBBY_OBJ)) {
            setupLobby();
            return;
        }

        int size = arena.getPlayers().size();
        int min = arena.getMinPlayers();
        int max = 16;
        int timer = arena.getPhaseTimer();
        Phase phase = arena.getPhase();

        String status;
        String timerLine;
        if (phase == Phase.LOBBY) {
            if (arena.getTaskId() != -1 && timer > 0) {
                status = ChatColor.GREEN + "Starting";
                timerLine = ChatColor.YELLOW + "Starts in: " + ChatColor.WHITE + timer + "s";
            } else {
                status = ChatColor.YELLOW + "Waiting";
                timerLine = ChatColor.GRAY + "Waiting for players";
            }
        } else {
            status = ChatColor.RED + "In Game";
            timerLine = ChatColor.GRAY + "Game in progress";
        }

        clearEntries();

        objective.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "WEREWOLF");

        int line = 15;
        setLine(line--, ChatColor.DARK_RED + " ");
        setLine(line--, ChatColor.RED + "Arena: " + ChatColor.WHITE + arena.getName());
        setLine(line--, ChatColor.RED + "Players: " + ChatColor.WHITE + size + "/" + max);
        setLine(line--, ChatColor.RED + "Status: " + status);
        setLine(line--, ChatColor.RED + " ");
        setLine(line--, timerLine);
        setLine(line--, ChatColor.RED + "Min: " + ChatColor.WHITE + min + " players");
        setLine(line--, ChatColor.DARK_RED + " ");
        setLine(line--, ChatColor.GRAY + "Use " + ChatColor.WHITE + "/ww join");
        setLine(line--, ChatColor.DARK_GRAY + "play.werewolf.net");

        applyToPlayers();
    }

    public void setupGame() {
        if (objective != null) {
            objective.unregister();
        }
        objective = scoreboard.registerNewObjective(GAME_OBJ, "dummy",
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "WEREWOLF");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        updateGame();
    }

    public void updateGame() {
        if (objective == null || !objective.getName().equals(GAME_OBJ)) {
            setupGame();
            return;
        }

        Phase phase = arena.getPhase();
        int timer = arena.getPhaseTimer();
        int alive = arena.getAlivePlayers().size();
        int dead = arena.getDeadPlayers().size();

        String phaseName;
        ChatColor phaseColor;
        if (phase == Phase.SHERIFF_ELECTION) {
            phaseName = "Election";
            phaseColor = ChatColor.GOLD;
        } else if (phase == Phase.DAY) {
            phaseName = "Day";
            phaseColor = ChatColor.GOLD;
        } else if (phase == Phase.NIGHT) {
            phaseName = "Night";
            phaseColor = ChatColor.DARK_PURPLE;
        } else {
            phaseName = "Ended";
            phaseColor = ChatColor.RED;
        }

        clearEntries();

        objective.setDisplayName(phaseColor + "" + ChatColor.BOLD + phaseName.toUpperCase());

        int line = 15;
        setLine(line--, ChatColor.DARK_RED + " ");
        setLine(line--, phaseColor + "Phase: " + ChatColor.WHITE + phaseName);
        setLine(line--, phaseColor + "Time: " + ChatColor.WHITE + Math.max(0, timer) + "s");
        setLine(line--, ChatColor.RED + " ");
        setLine(line--, ChatColor.GREEN + "Alive: " + ChatColor.WHITE + alive);
        setLine(line--, ChatColor.RED + "Dead: " + ChatColor.WHITE + dead);
        setLine(line--, ChatColor.DARK_RED + " ");

        if (phase == Phase.DAY) {
            setLine(line--, ChatColor.GOLD + "" + ChatColor.BOLD + "Votes:");
            if (voteCounts.isEmpty()) {
                setLine(line--, ChatColor.GRAY + "No votes yet");
            } else {
                int voteShown = 0;
                for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
                    if (voteShown >= 5) break;
                    org.bukkit.entity.Player voted = Bukkit.getPlayer(entry.getKey());
                    if (voted != null) {
                        setLine(line--, ChatColor.YELLOW + voted.getName() + ": " + ChatColor.RED + entry.getValue());
                        voteShown++;
                    }
                }
            }
            setLine(line--, ChatColor.RED + " ");
        }

        setLine(line--, ChatColor.DARK_GRAY + "play.werewolf.net");

        applyToPlayers();
    }

    public void updateVotes(Map<UUID, Integer> votes) {
        voteCounts.clear();
        voteCounts.putAll(votes);
        if (objective != null && objective.getName().equals(GAME_OBJ)) {
            updateGame();
        }
    }

    public void clear() {
        for (GamePlayer gp : arena.getPlayers()) {
            gp.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        if (objective != null) {
            objective.unregister();
            objective = null;
        }
    }

    private void applyToPlayers() {
        for (GamePlayer gp : arena.getPlayers()) {
            gp.getPlayer().setScoreboard(scoreboard);
        }
    }

    private void clearEntries() {
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
    }

    private void setLine(int score, String text) {
        if (score < 0 || score >= ENTRIES.length) return;
        String entry = ENTRIES[score];
        Team team = scoreboard.getTeam("line" + score);
        if (team == null) {
            team = scoreboard.registerNewTeam("line" + score);
        }
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }
        team.setPrefix(text);
        team.setSuffix("");
        objective.getScore(entry).setScore(score);
    }
}
