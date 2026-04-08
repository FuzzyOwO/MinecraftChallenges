package gg.itzkatze.mcc.game;

import gg.itzkatze.mcc.MinecraftChallenges;
import gg.itzkatze.mcc.challenges.Challenge;
import gg.itzkatze.mcc.challenges.ChallengeManager;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.event.TimerEndEvent;
import gg.itzkatze.mcc.event.TimerPauseEvent;
import gg.itzkatze.mcc.event.TimerSetTimeEvent;
import gg.itzkatze.mcc.event.TimerStartTimeChangeEvent;
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
import java.util.Timer;
import java.util.TimerTask;

public class MccTimer {

    private static final Logger logger = LoggerFactory.getLogger("ForceBattle/" + MccTimer.class.getSimpleName());

    @Getter
    private int startTime;
    @Getter
    private int time;
    @Getter
    private boolean running;
    private boolean hitZero;
    private Timer timer;
    private TimerTask timerTask;

    public MccTimer(int time, boolean running) {
        this.startTime = time;
        this.time = time;
        this.running = running;
        runTimerTask();
        runActionbarTask();
    }

    private void runActionbarTask() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                MccPlayer.getOnlinePlayers().forEach(
                        forceBattlePlayer -> forceBattlePlayer.sendActionbar(
                                getActionbarMessage(forceBattlePlayer)
                        ));
            }
        };

        Timer timer = new Timer("forcebattle/actionbar");
        timer.scheduleAtFixedRate(task, 0L, 150L);
    }

    private void runTimerTask() {
        if (timer != null) timer.cancel();
        if (timerTask != null) timerTask.cancel();

        timer = new Timer("forcebattle/timer");
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!running) return;

                time--;
                if (time < 11 && time > 0) {
                    MccPlayer.PLAYERS.forEach(battlePlayer ->
                            battlePlayer.playSound(Sound.sound(Key.key("block.note_block.bass"), Sound.Source.MASTER, 20, 20))
                    );
                }

                if (time <= 0) {
                    running = false;
                    hitZero = true;

                    Bukkit.getScheduler().runTask(MinecraftChallenges.get(), () -> {
                        TimerEndEvent timerEndEvent = new TimerEndEvent();
                        Bukkit.getPluginManager().callEvent(timerEndEvent);
                    });
                }
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0L, 1000L);
    }

    private Component getActionbarMessage(MccPlayer player) {
        Challenge actionbarChallenge = ChallengeManager.getActiveChallenges().stream()
                .filter(challenge -> challenge.getActionbar(player, running, time) != null)
                .max(Comparator.comparingInt(Challenge::getActionbarPriority))
                .orElse(null);
        if (actionbarChallenge != null) {
            return actionbarChallenge.getActionbar(player, running, time);
        }

        return running
                ? Component.text(StringUtility.formatTime(time)).color(NamedTextColor.BLUE).style(Style.style(TextDecoration.ITALIC))
                : Component.text("Timer paused.").color(NamedTextColor.BLUE).style(Style.style(TextDecoration.ITALIC));
    }

    public void setStartTime(int newStartTime) {
        TimerStartTimeChangeEvent startTimeChangeEvent = new TimerStartTimeChangeEvent(newStartTime);
        Bukkit.getPluginManager().callEvent(startTimeChangeEvent);
        if (startTimeChangeEvent.isCancelled()) {
            logger.info("TimerStartTimeChangeEvent has been denied by an external plugin.");
            return;
        }
        this.startTime = startTimeChangeEvent.getNewStartTime();
    }

    public void setTime(int newTime) {
        TimerSetTimeEvent setTimeEvent = new TimerSetTimeEvent(newTime);
        Bukkit.getPluginManager().callEvent(setTimeEvent);
        if (setTimeEvent.isCancelled()) {
            logger.info("TimerSetTimeEvent has been denied by an external plugin.");
            return;
        }
        this.time = setTimeEvent.getNewTime();
    }

    public void setRunning(boolean running) {
        if (running) {
            this.hitZero = false;
            this.running = true;
        } else {
            TimerPauseEvent pauseEvent = new TimerPauseEvent(this.time);
            Bukkit.getPluginManager().callEvent(pauseEvent);
            if (pauseEvent.isCancelled()) {
                logger.info("TimerPauseEvent has been denied by an external plugin.");
                return;
            }
            this.time = pauseEvent.getPauseTime();
            this.running = false;
        }
    }

    public boolean hasHitZero() {
        return hitZero;
    }

    public void start() {
        if (!running) {
            setRunning(true);
        }
    }

    public void pause() {
        if (running) {
            setRunning(false);
        }
    }

}
