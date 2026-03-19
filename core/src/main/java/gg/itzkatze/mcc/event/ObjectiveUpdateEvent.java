package gg.itzkatze.mcc.event;

import lombok.Getter;
import gg.itzkatze.mcc.challenges.ChallengeObjective;
import gg.itzkatze.mcc.team.MccTeam;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.EventLogger;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ObjectiveUpdateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @Getter
    private final MccPlayer mccPlayer;
    @Getter
    private final MccTeam team;
    @Getter
    private ChallengeObjective newObjective;
    private boolean cancelled;

    public ObjectiveUpdateEvent(@NotNull MccPlayer mccPlayer, @NotNull ChallengeObjective newObjective) {
        this.mccPlayer = mccPlayer;
        this.team = null;
        this.newObjective = newObjective;
    }

    public ObjectiveUpdateEvent(@NotNull MccTeam team, @NotNull ChallengeObjective newObjective) {
        this.mccPlayer = null;
        this.team = team;
        this.newObjective = newObjective;
    }

    public void setNewObjective(ChallengeObjective newObjective) {
        if (this.newObjective != newObjective) {
            EventLogger.LOGGER.info("ObjectiveUpdateEvent: newObjective changed to {}.", newObjective.getObjectiveString());
            this.newObjective = newObjective;
        }
    }

    public boolean isTeamEvent() {
        return team != null;
    }

    public boolean isPlayerEvent() {
        return mccPlayer != null;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
