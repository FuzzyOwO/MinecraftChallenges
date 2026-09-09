package gg.itzkatze.mcc.event;

import lombok.Getter;
import gg.itzkatze.mcc.player.MccPlayer;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerResetEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    @Getter
    @Setter
    private boolean cancelled;

    @Getter
    private final MccPlayer player;

    public PlayerResetEvent(MccPlayer player) {
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
