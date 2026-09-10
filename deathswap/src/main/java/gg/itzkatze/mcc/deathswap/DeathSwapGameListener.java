package gg.itzkatze.mcc.deathswap;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.game.ChallengeTimer;
import gg.itzkatze.mcc.game.GameTimer;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class DeathSwapGameListener implements Listener {

    private final Set<UUID> eliminatedPlayers = new HashSet<>();
    private final Random random = new Random();
    private boolean started = false;
    private boolean finished = false;
    private int swapCount = 0;
    private UUID winnerId = null;

    private ChallengeTimer elapsedTimer;
    private ChallengeTimer swapTimer;

    private int lastIntervalMinutes = -1;
    private int elapsedSeconds = 0;
    private int lastPlayedCountdownSecond = -1;

    public void initialize() {
        eliminatedPlayers.clear();
        started = false;
        finished = false;
        swapCount = 0;
        winnerId = null;
        lastIntervalMinutes = -1;
        elapsedSeconds = 0;
        lastPlayedCountdownSecond = -1;
        elapsedTimer = null;
        swapTimer = null;
    }

    public void cleanup() {
        initialize();
    }

    public void startGame() {
        initialize();
        started = true;
        elapsedTimer = new ChallengeTimer(1_000L);
        swapTimer = new ChallengeTimer(getSwapIntervalMillis());
    }

    public void runTask() {
        if (!started || finished) return;

        if (MinecraftChallenges.getTimer().getMode() != GameTimer.Mode.DISABLED
                && !MinecraftChallenges.getTimer().isRunning()) {
            elapsedTimer.resetBaseline();
            swapTimer.resetBaseline();
            return;
        }

        int intervalMinutes = Math.max(1, DeathSwapSettings.getSwapIntervalMinutes());
        if (intervalMinutes != lastIntervalMinutes) {
            lastIntervalMinutes = intervalMinutes;
            swapTimer = new ChallengeTimer(getSwapIntervalMillis());
        }

        // Tick both timers (they share the same wall clock).
        elapsedTimer.tick();
        if (elapsedTimer.hasElapsed()) {
            elapsedSeconds++;
            elapsedTimer.consumeElapsed();
        }

        swapTimer.tick();

        int secondsUntilSwap = swapTimer.getSecondsRemaining();
        if (secondsUntilSwap <= 10 && secondsUntilSwap > 0 && secondsUntilSwap != lastPlayedCountdownSecond) {
            lastPlayedCountdownSecond = secondsUntilSwap;
            net.kyori.adventure.sound.Sound sound = net.kyori.adventure.sound.Sound.sound(
                    net.kyori.adventure.key.Key.key("block.note_block.bass"),
                    net.kyori.adventure.sound.Sound.Source.MASTER,
                    20f,
                    20f
            );
            MccPlayer.getOnlinePlayers().forEach(p -> p.playSound(sound));
        } else if (secondsUntilSwap > 10) {
            lastPlayedCountdownSecond = -1;
        }

        while (swapTimer.hasElapsed()) {
            lastPlayedCountdownSecond = -1;
            boolean swapped = swapAlivePlayers();
            if (swapped) swapCount++;
            swapTimer.consumeElapsed();
            if (!swapped) break;
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!started || finished) return;
        Player player = event.getEntity();
        if (eliminatedPlayers.add(player.getUniqueId())) {
            checkForWinner();
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!eliminatedPlayers.contains(player.getUniqueId())) return;
        Bukkit.getScheduler().runTask(MinecraftChallenges.get(), () -> {
            if (player.isOnline()) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!eliminatedPlayers.contains(player.getUniqueId())) return;
        Bukkit.getScheduler().runTask(MinecraftChallenges.get(), () -> {
            if (player.isOnline()) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!started || finished) return;
        Player player = event.getPlayer();
        if (eliminatedPlayers.add(player.getUniqueId())) {
            checkForWinner();
        }
    }

    public Component getActionbar(MccPlayer player, boolean timerRunning) {
        if (!started) {
            return null;
        }

        Component message = Component.text()
                .append(Component.text("Elapsed: ").color(NamedTextColor.GRAY))
                .append(Component.text(StringUtility.formatTime(elapsedSeconds).trim()).color(NamedTextColor.AQUA))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Swaps: ").color(NamedTextColor.GRAY))
                .append(Component.text(swapCount).color(NamedTextColor.LIGHT_PURPLE))
                .build();

        int secondsUntilSwap = swapTimer != null ? swapTimer.getSecondsRemaining() : 0;
        if (secondsUntilSwap <= 10) {
            message = message.append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                    .append(Component.text("Next swap: ").color(NamedTextColor.GRAY))
                    .append(Component.text(secondsUntilSwap + "s").color(NamedTextColor.RED));
        } else if (MinecraftChallenges.getTimer().getMode() != GameTimer.Mode.DISABLED && !timerRunning) {
            message = message.append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                    .append(Component.text("Paused").color(NamedTextColor.BLUE));
        }

        return message;
    }

    private boolean swapAlivePlayers() {
        List<Player> alivePlayers = getAlivePlayers();
        if (alivePlayers.size() < 2) return false;

        Collections.shuffle(alivePlayers, random);

        for (int i = 0; i + 1 < alivePlayers.size(); i += 2) {
            Player first = alivePlayers.get(i);
            Player second = alivePlayers.get(i + 1);

            Location firstLocation = first.getLocation().clone();
            Location secondLocation = second.getLocation().clone();

            first.teleport(secondLocation);
            second.teleport(firstLocation);
        }

        return true;
    }

    private List<Player> getAlivePlayers() {
        List<Player> alivePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (eliminatedPlayers.contains(player.getUniqueId())) continue;
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            alivePlayers.add(player);
        }
        return alivePlayers;
    }

    private void checkForWinner() {
        if (finished) return;
        List<Player> alivePlayers = getAlivePlayers();
        if (alivePlayers.size() != 1) return;

        finished = true;

        Player winner = alivePlayers.getFirst();
        if (winnerId != null && winnerId.equals(winner.getUniqueId())) {
            return;
        }
        winnerId = winner.getUniqueId();
        MinecraftChallenges.broadcast(
                Component.text("Death Swap winner: ").color(NamedTextColor.GOLD)
                        .append(Component.text(winner.getName()).color(NamedTextColor.GREEN))
        );

        Title title = Title.title(
                Component.text("Death Swap").color(NamedTextColor.GOLD),
                Component.text(winner.getName() + " won!").color(NamedTextColor.GREEN),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
        }
    }

    private long getSwapIntervalMillis() {
        return Math.max(1, DeathSwapSettings.getSwapIntervalMinutes()) * 60_000L;
    }
}
