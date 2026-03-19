package gg.itzkatze.mcc.event;

public interface CancellableEvent {

    boolean isCancelled();

    void setCancelled(boolean cancelled);

}
