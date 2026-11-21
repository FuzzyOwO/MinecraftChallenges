package gg.itzkatze.katze.game.entry;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

public interface GameEntryPolicy {
    int priority();
    default boolean preparing() { return false; }
    default boolean finished() { return false; }
    Admission prepare(UUID playerId, EntryReason reason);
    void admitted(UUID playerId, EntryReason reason);
    void left(UUID playerId, LeaveReason reason);
    List<Location> spectatorTargets();
    record Admission(boolean allowed, Location destination, Component rejection) {}
}
