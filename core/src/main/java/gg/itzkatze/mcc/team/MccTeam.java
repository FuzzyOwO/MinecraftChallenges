package gg.itzkatze.mcc.team;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.event.PlayerTeamJoinEvent;
import gg.itzkatze.mcc.event.PlayerTeamLeaveEvent;
import gg.itzkatze.mcc.player.MccPlayer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MccTeam {

    private static final Logger logger = LoggerFactory.getLogger(MccTeam.class.getSimpleName());
    public static final List<MccTeam> teams = new ArrayList<>();
    private static final Map<ChallengeType, TeamAdapterFactory> factories = new HashMap<>();

    @Getter
    protected final List<MccPlayer> players;
    protected final int ID;
    protected boolean privateTeam = false;
    @Getter
    protected JoinRequest lastJoinRequest = null;

    public MccTeam(List<MccPlayer> players) {
        this(players, nextId(), true);
    }

    protected MccTeam(List<MccPlayer> players, int id, boolean register) {
        this.players = new ArrayList<>(players);
        this.ID = id;
        if (register) {
            teams.add(this);
        }
    }

    /**
     * Registers a factory for creating challenge-specific team wrappers.
     */
    public static void registerFactory(@NotNull ChallengeType type, @NotNull TeamAdapterFactory factory) {
        factories.put(type, factory);
    }

    /**
     * Unregisters the factory for the given challenge type.
     */
    public static void unregisterFactory(@NotNull ChallengeType type) {
        factories.remove(type);
    }

    /**
     * Creates a team using a registered challenge factory if available.
     */
    public static @NotNull MccTeam create(@NotNull List<MccPlayer> players) {
        int id = nextId();
        for (Challenge challenge : ChallengeManager.getActiveChallenges()) {
            TeamAdapterFactory factory = factories.get(challenge.getType());
            if (factory != null) {
                return factory.adapt(players, id, true);
            }
        }
        return new MccTeam(players, id, true);
    }

    /**
     * Upgrades existing teams to a challenge-specific implementation when a factory is registered.
     */
    public static void upgrade(@NotNull ChallengeType type) {
        TeamAdapterFactory factory = factories.get(type);
        if (factory == null) return;

        List<MccTeam> current = new ArrayList<>(teams);
        teams.clear();

        for (MccTeam team : current) {
            MccTeam upgraded = factory.adapt(new ArrayList<>(team.getPlayers()), team.getId(), true);
            upgraded.privateTeam = team.privateTeam;
            if (team.lastJoinRequest != null) {
                upgraded.lastJoinRequest = new JoinRequest(team.lastJoinRequest.mccPlayer(), upgraded);
            }
        }
    }

    /**
     * Downgrades existing teams to base teams when a challenge is disabled.
     */
    public static void downgrade(@NotNull ChallengeType type) {
        if (!factories.containsKey(type)) return;

        List<MccTeam> current = new ArrayList<>(teams);
        teams.clear();

        for (MccTeam team : current) {
            MccTeam base = new MccTeam(new ArrayList<>(team.getPlayers()), team.getId(), true);
            base.privateTeam = team.privateTeam;
            if (team.lastJoinRequest != null) {
                base.lastJoinRequest = new JoinRequest(team.lastJoinRequest.mccPlayer(), base);
            }
        }
    }

    private static int nextId() {
        List<Integer> usedIdList = new ArrayList<>();
        for (MccTeam team : teams) {
            usedIdList.add(team.ID);
        }

        int id = 0;
        while (usedIdList.contains(id)) {
            id++;
        }
        return id;
    }

    public static Optional<MccTeam> ofId(int id) {
        for (MccTeam team : teams) {
            if (team.getId() == id) {
                return Optional.of(team);
            }
        }
        return Optional.empty();
    }

    public void sendJoinRequest(MccPlayer mccPlayer) {
        if (!isPrivate()) {
            mccPlayer.addToTeam(this);
            return;
        }
        this.lastJoinRequest = new JoinRequest(mccPlayer, this);
        players.forEach(teamPlayer ->
                teamPlayer.sendMessage(Component.text(NamedTextColor.BLUE + mccPlayer.getName())
                        .append(Component.text(NamedTextColor.GRAY + " has requested to join your team. /team accept|reject.")))
        );
    }

    public void finishLastJoinRequest(boolean accept) {
        if (accept) {
            lastJoinRequest.accept();
        } else {
            lastJoinRequest.reject();
        }
        this.lastJoinRequest = null;
    }

    public boolean isPrivate() {
        return privateTeam;
    }

    public void setPrivate(boolean privateTeam) {
        this.privateTeam = privateTeam;
    }

    public void addPlayer(MccPlayer mccPlayer) {
        PlayerTeamJoinEvent teamJoinEvent = new PlayerTeamJoinEvent(this, mccPlayer);
        Bukkit.getPluginManager().callEvent(teamJoinEvent);
        if (teamJoinEvent.isCancelled()) {
            logger.info("PlayerTeamJoinEvent has been denied by an external plugin.");
            return;
        }

        players.forEach(teamPlayer -> teamPlayer.sendMessage(Component.text(NamedTextColor.BLUE + mccPlayer.getName() + NamedTextColor.GRAY + " has joined your team.")));
        players.add(mccPlayer);
        mccPlayer.sendMessage(Component.text(NamedTextColor.GREEN + "You have joined team " + this.getId() + "."));
    }

    public void removePlayer(MccPlayer mccPlayer) {
        PlayerTeamLeaveEvent teamLeaveEvent = new PlayerTeamLeaveEvent(this, mccPlayer);
        Bukkit.getPluginManager().callEvent(teamLeaveEvent);
        if (teamLeaveEvent.isCancelled()) {
            logger.info("PlayerTeamLeaveEvent has been denied by an external plugin.");
            return;
        }

        players.remove(mccPlayer);
        players.forEach(teamPlayer -> teamPlayer.sendMessage(Component.text(NamedTextColor.BLUE + mccPlayer.getName() + NamedTextColor.GRAY + " has left your team.")));
        if (players.isEmpty()) {
            this.delete();
        }
    }

    public void delete() {
        teams.remove(this);
    }

    public static void deleteAll() {
        for (MccTeam team : new ArrayList<>(teams)) {
            team.delete();
        }
    }

    public int getPoints() {
        int teamPoints = 0;

        for (MccPlayer teamPlayer : players) {
            teamPoints += teamPlayer.getPoints();
        }

        return teamPoints;
    }

    public int getId() {
        return ID;
    }

    public static Map<Integer, MccTeam> getPlaces() {
        Map<Integer, MccTeam> places = new HashMap<>();

        final Map<MccTeam, Integer> TEAM_POINTS_MAP = new HashMap<>();
        for (MccTeam team : teams) {
            TEAM_POINTS_MAP.put(team, team.getPoints());
        }

        List<Map.Entry<MccTeam, Integer>> sortedTeams = TEAM_POINTS_MAP.entrySet()
                .stream()
                .sorted(Map.Entry.<MccTeam, Integer>comparingByValue().reversed())
                .toList();

        int place = 1;
        for (Map.Entry<MccTeam, Integer> entry : sortedTeams) {
            places.put(place++, entry.getKey());
        }

        return places;
    }

    public static int getPlace(MccTeam team) {
        List<MccTeam> sortedTeams = new ArrayList<>(MccTeam.teams);
        sortedTeams.sort(Comparator.comparingInt(MccTeam::getPoints).reversed());

        int place = 1;
        int previousPoints = -1;

        for (int i = 0; i < sortedTeams.size(); i++) {
            MccTeam current = sortedTeams.get(i);

            if (current.getPoints() != previousPoints) {
                place = i + 1;
                previousPoints = current.getPoints();
            }

            if (current.equals(team)) {
                return place;
            }
        }

        return -1;
    }

    /**
     * Gets the current objective. Default implementation returns null.
     * Challenges should override this in their team implementations.
     */
    public Object getObjective() {
        return null;
    }

    /**
     * Called when an objective is completed. Default implementation does nothing.
     * Challenges should override this in their team implementations.
     */
    public void updateObjective(MccPlayer finisher, boolean finishLast, boolean hasBeenSkipped) {
        // Default implementation - does nothing
        // Challenge-specific team classes should override this
    }


    public record JoinRequest(MccPlayer mccPlayer, MccTeam team) {

        public void accept() {
                mccPlayer.addToTeam(team);
            }

            public void reject() {
                mccPlayer.sendMessage(Component.text(NamedTextColor.GRAY + "Your join request has been rejected."));
            }

        }

}
