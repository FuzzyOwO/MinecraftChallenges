package gg.itzkatze.mcc.event;

import lombok.Getter;
import gg.itzkatze.mcc.player.MccPlayer;
import gg.itzkatze.mcc.util.EventLogger;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerExcludeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    @Getter
    @Setter
    private boolean cancelled;

    @Getter
    private final MccPlayer player;
    @Getter
    private boolean newExcluded;

    public PlayerExcludeEvent(MccPlayer player, boolean newExcluded) {
        this.player = player;
        this.newExcluded = newExcluded;
    }

    public void setNewExcluded(boolean newExcluded) {
        if (this.newExcluded != newExcluded) {
            EventLogger.LOGGER.info("PlayerExcludeEvent: value newExcluded has been changed to {}.", newExcluded);
            this.newExcluded = newExcluded;
        }
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
