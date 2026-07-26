package gg.itzkatze.mcc.game;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.event.TimerEndEvent;
import gg.itzkatze.mcc.event.TimerPauseEvent;
import gg.itzkatze.mcc.event.TimerSetTimeEvent;
import gg.itzkatze.mcc.event.TimerStartEvent;
import gg.itzkatze.mcc.event.TimerStartTimeChangeEvent;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.StringUtility;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class MccTimer implements GameTimer {

    private static final Logger logger = LoggerFactory.getLogger("MinecraftChallenges/" + MccTimer.class.getSimpleName());

    @Getter
    private int startTime;
    @Getter
    private int time;
    @Getter
    private boolean running;
    @Getter
    private Mode mode;
    private boolean hitZero;

    public MccTimer(int startTime, boolean running) {
        this.startTime = startTime;
        this.time = 0;
        this.running = running;
        this.mode = Mode.DISABLED;
        runTimerTask();
        runActionbarTask();
    }

    private void runActionbarTask() {
        Bukkit.getScheduler().runTaskTimer(MinecraftChallenges.get(), () ->
                MccPlayer.getOnlinePlayers().forEach(player -> player.sendActionbar(getActionbarMessage(player))),
                1L,
                3L
        );
    }

    private void runTimerTask() {
        Bukkit.getScheduler().runTaskTimer(MinecraftChallenges.get(), () -> {
            if (!running) return;

            if (mode == Mode.COUNT_UP) {
                time++;
                return;
            }

            if (mode == Mode.COUNT_DOWN) {
                time--;
                if (time < 11 && time > 0) {
                    MccPlayer.PLAYERS.forEach(p ->
                            p.playSound(Sound.sound(Key.key("block.note_block.bass"), Sound.Source.MASTER, 20, 20)));
                }

                if (time <= 0) {
                    stop();
                }
            }
        }, 20L, 20L);
    }

    private Component getActionbarMessage(MccPlayer player) {
        Challenge actionbarChallenge = ChallengeManager.getActiveChallenges().stream()
                .filter(challenge -> challenge.getActionbar(player, running, time) != null)
                .max(Comparator.comparingInt(Challenge::getActionbarPriority))
                .orElse(null);
        if (actionbarChallenge != null) {
            return actionbarChallenge.getActionbar(player, running, time);
        }

        if (mode == Mode.DISABLED) {
            return Component.empty();
        }

        if (!running && (hitZero || !ChallengeManager.isGameStarted())) {
            return Component.empty();
        }

        return running
                ? Component.text(StringUtility.formatTime(time)).color(NamedTextColor.BLUE).style(Style.style(TextDecoration.ITALIC))
                : Component.text("Timer paused.").color(NamedTextColor.BLUE).style(Style.style(TextDecoration.ITALIC));
    }

    @Override
    public void setStartTime(int newStartTime) {
        TimerStartTimeChangeEvent startTimeChangeEvent = new TimerStartTimeChangeEvent(newStartTime);
        Bukkit.getPluginManager().callEvent(startTimeChangeEvent);
        if (startTimeChangeEvent.isCancelled()) {
            logger.info("TimerStartTimeChangeEvent has been denied by an external plugin.");
            return;
        }
        this.startTime = startTimeChangeEvent.getNewStartTime();
    }

    @Override
    public void setTime(int newTime) {
        TimerSetTimeEvent setTimeEvent = new TimerSetTimeEvent(newTime);
        Bukkit.getPluginManager().callEvent(setTimeEvent);
        if (setTimeEvent.isCancelled()) {
            logger.info("TimerSetTimeEvent has been denied by an external plugin.");
            return;
        }
        this.time = setTimeEvent.getNewTime();
    }

    @Override
    public void setMode(Mode mode) {
        this.mode = mode;
        this.hitZero = false;
    }

    @Override
    public boolean hasHitZero() {
        return hitZero;
    }

    @Override
    public void start() {
        TimerStartEvent startEvent = new TimerStartEvent(this.time);
        Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) {
            logger.info("TimerStartEvent has been denied by an external plugin.");
            return;
        }
        this.time = startEvent.getStartTime();
        this.hitZero = false;
        this.running = true;
    }

    @Override
    public void pause() {
        TimerPauseEvent pauseEvent = new TimerPauseEvent(this.time);
        Bukkit.getPluginManager().callEvent(pauseEvent);
        if (pauseEvent.isCancelled()) {
            logger.info("TimerPauseEvent has been denied by an external plugin.");
            return;
        }
        this.time = pauseEvent.getPauseTime();
        this.running = false;
    }

    @Override
    public void stop() {
        TimerEndEvent endEvent = new TimerEndEvent();
        Bukkit.getPluginManager().callEvent(endEvent);
        if (endEvent.isCancelled()) {
            logger.info("TimerEndEvent has been denied by an external plugin.");
            return;
        }

        ChallengeManager.onTimerElapsed();
        this.running = false;
        hitZero = true;
    }
}
