package gg.itzkatze.mcc.player;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.Challenge;
import lombok.Getter;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.challenges.ChallengeObjective;
import gg.itzkatze.mcc.challenges.ChallengeType;
import gg.itzkatze.mcc.commands.framework.CallerType;
import gg.itzkatze.mcc.commands.framework.CommandCaller;
import gg.itzkatze.mcc.event.PlayerExcludeEvent;
import gg.itzkatze.mcc.game.GameLifecycleManager;
import gg.itzkatze.mcc.team.MccTeam;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MccPlayer implements CommandCaller {

    public static final List<MccPlayer> PLAYERS = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger("Mcc/" + MccPlayer.class.getSimpleName());
    private static final Map<ChallengeType, PlayerAdapterFactory> factories = new HashMap<>();

    @Getter
    private final UUID uuid;
    protected String name;
    @Getter
    private boolean excluded;

    public MccPlayer(@NotNull Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        PLAYERS.add(this);
    }

    /**
     * Registers a factory for creating challenge-specific player wrappers.
     * Called by challenge modules in their {@code onEnable}.
     */
    public static void registerFactory(@NotNull ChallengeType type, @NotNull PlayerAdapterFactory factory) {
        factories.put(type, factory);
    }

    /**
     * Unregisters the factory for the given challenge type. Called in {@code onDisable}.
     */
    public static void unregisterFactory(@NotNull ChallengeType type) {
        factories.remove(type);
    }

    /**
     * Upgrades online player wrappers to the challenge-specific type if a factory is registered.
     * Call from the challenge's {@code onEnable}.
     */
    public static void upgrade(@NotNull ChallengeType type) {
        PlayerAdapterFactory factory = factories.get(type);
        if (factory == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            Optional<MccPlayer> existing = adapt(player.getUniqueId());
            existing.ifPresent(PLAYERS::remove);
            factory.adapt(player);
        }
    }

    /**
     * Returns the player wrapper for the given Bukkit player.
     * Uses registered factories for active challenges; otherwise returns a base MccPlayer.
     */
    public static @NotNull MccPlayer adapt(Player object) {
        for (MccPlayer mccPlayer : PLAYERS) {
            if (mccPlayer.getUuid().equals(object.getUniqueId())) {
                return mccPlayer;
            }
        }
        for (Challenge challenge : ChallengeManager.getActiveChallenges()) {
            PlayerAdapterFactory factory = factories.get(challenge.getType());
            if (factory != null) {
                return factory.adapt(object); // constructor already added to BATTLE_PLAYERS
            }
        }
        return new MccPlayer(object);
    }

    public static @NotNull Optional<MccPlayer> adapt(String name) {
        for (MccPlayer mccPlayer : PLAYERS) {
            if (mccPlayer.getName().equalsIgnoreCase(name)) {
                return Optional.of(mccPlayer);
            }
        }
        return Optional.empty();
    }

    public static @NotNull Optional<MccPlayer> adapt(UUID uuid) {
        for (MccPlayer mccPlayer : PLAYERS) {
            if (mccPlayer.getUuid().equals(uuid)) {
                return Optional.of(mccPlayer);
            }
        }
        return Optional.empty();
    }

    /**
     * Replaces all player wrappers for the given challenge type with base MccPlayer.
     * Call from the challenge's {@code onDisable}.
     */
    public static void downgrade(@NotNull ChallengeType type) {
        List<MccPlayer> toReplace = new ArrayList<>();
        for (MccPlayer p : PLAYERS) {
            if (p.getChallengeType().orElse(null) == type) {
                toReplace.add(p);
            }
        }
        for (MccPlayer p : toReplace) {
            PLAYERS.remove(p);
            Player bukkit = p.getPlayer();
            if (bukkit != null) {
                PLAYERS.add(new MccPlayer(bukkit));
            }
        }
    }

    /**
     * Challenge type for this player wrapper; empty for base MccPlayer.
     */
    public @NotNull Optional<ChallengeType> getChallengeType() {
        return Optional.empty();
    }

    public static @NotNull List<MccPlayer> getOnlinePlayers() {
        List<MccPlayer> onlinePlayers = new ArrayList<>();
        for (MccPlayer mccPlayer : PLAYERS) {
            if (mccPlayer.isOffline()) continue;
            onlinePlayers.add(mccPlayer);
        }
        return onlinePlayers;
    }

    public static @NotNull List<UUID> getAllUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (MccPlayer mccPlayer : PLAYERS) {
            uuids.add(mccPlayer.uuid);
        }
        return uuids;
    }

    public static Map<Integer, MccPlayer> getPlaces() {
        Map<Integer, MccPlayer> places = new HashMap<>();
        Map<MccPlayer, Integer> pointMap = new HashMap<>();
        for (MccPlayer player : PLAYERS) {
            pointMap.put(player, player.getPoints());
        }
        List<Map.Entry<MccPlayer, Integer>> sorted = pointMap.entrySet().stream()
                .sorted(Map.Entry.<MccPlayer, Integer>comparingByValue().reversed())
                .toList();
        int place = 1;
        for (Map.Entry<MccPlayer, Integer> entry : sorted) {
            places.put(place++, entry.getKey());
        }
        return places;
    }

    public static int getPlace(MccPlayer player) {
        List<MccPlayer> sorted = new ArrayList<>(PLAYERS);
        sorted.sort(Comparator.comparingInt(MccPlayer::getPoints).reversed());
        int place = 1;
        int previousPoints = -1;
        for (int i = 0; i < sorted.size(); i++) {
            MccPlayer current = sorted.get(i);
            if (current.getPoints() != previousPoints) {
                place = i + 1;
                previousPoints = current.getPoints();
            }
            if (current.equals(player)) return place;
        }
        return -1;
    }

    public int getPoints() {
        return 0;
    }

    public void setPoints(int points) {
    }

    public int getChainProgress() {
        return 0;
    }

    public void updateObjective(boolean finishLast, boolean hasBeenSkipped) {
    }

    public void setCurrentObjective(ChallengeObjective newObjective, boolean finishLast, boolean hasBeenSkipped) {
    }

    public void addSkippedObjectiveToList(ChallengeObjective objective) {
    }

    @Override
    public CallerType callerType() {
        return CallerType.PLAYER;
    }

    @Override
    public String getName() {
        if (this.name == null) {
            if (getPlayer() == null) return "N/A";
            this.name = getPlayer().getName();
        }
        return this.name;
    }

    public Audience getAudience() {
        if (getPlayer() == null) return Audience.empty();
        return MinecraftChallenges.getBukkitAudiences().player(getPlayer());
    }

    public MccTeam getTeam() {
        for (MccTeam team : MccTeam.teams) {
            if (team.getPlayers().contains(this)) return team;
        }
        return null;
    }

    public boolean isInTeam() {
        return getTeam() != null;
    }

    public void setExcluded(boolean excluded) {
        PlayerExcludeEvent excludeEvent = new PlayerExcludeEvent(this, excluded);
        Bukkit.getPluginManager().callEvent(excludeEvent);
        if (excludeEvent.isCancelled()) {
            logger.info("PlayerExcludeEvent has been denied by an external plugin.");
            return;
        }
        this.excluded = excludeEvent.isNewExcluded();
    }

    public void leaveTeam() {
        if (isInTeam()) getTeam().removePlayer(this);
    }

    public void addToTeam(@NotNull MccTeam team) {
        leaveTeam();
        if (!isInTeam()) team.addPlayer(this);
    }

    public void sendMessage(Component message) {
        getAudience().sendMessage(message);
    }

    public void sendActionbar(Component message) {
        getAudience().sendActionBar(message);
    }

    public void playSound(Sound sound) {
        getAudience().playSound(sound);
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(getUuid());
    }

    @Override
    public boolean hasPermission(@NotNull PermissionDefault permission) {
        Player player = getPlayer();
        if (player == null) return false;
        return permission.getValue(player.isOp());
    }

    public void handleReset() {
        if (getPlayer() == null) return;
        GameLifecycleManager.sendToLobby(this);
    }

    public boolean isOffline() {
        if (getPlayer() == null) return true;
        return !getPlayer().isOnline();
    }

    public void openInventory(Inventory inventory) {
        if (getPlayer() == null) return;
        getPlayer().openInventory(inventory);
    }

    public void setScoreboard(Scoreboard scoreboard) {
        if (getPlayer() == null) return;
        getPlayer().setScoreboard(scoreboard);
    }

    public void teleport(Location location) {
        if (getPlayer() == null) return;
        getPlayer().teleport(location);
    }

    public Inventory getInventory() {
        if (getPlayer() == null) return null;
        return getPlayer().getInventory();
    }

    public World getWorld() {
        if (getPlayer() == null) return null;
        return getPlayer().getWorld();
    }

    public @Nullable Inventory getBackpack() {
        return null;
    }
}
