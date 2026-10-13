package gg.itzkatze.mcc.lobby.jumpandrun;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.Objects;
import java.util.UUID;

@Getter
final class JumpRun {

    private final Player player;
    private final UUID id;
    private final RunLook look;
    private final BitSet visited = new BitSet();
    private Cell current;
    private @Nullable Cell next;
    private HopDifficulty nextDifficulty = HopDifficulty.NORMAL;
    private @Nullable HopSteering.Leg leg;
    private int score;

    JumpRun(@NotNull Player player, @NotNull RunLook look, @NotNull Cell start, @NotNull Cell next) {
        this.player = player;
        this.id = player.getUniqueId();
        this.look = look;
        this.current = start;
        this.next = next;
    }

    void land() {
        score++;
        current = Objects.requireNonNull(next);
        next = null;
    }

    void setNext(@NotNull Cell next, @NotNull HopDifficulty difficulty) {
        this.next = next;
        this.nextDifficulty = difficulty;
    }

    void setLeg(@NotNull HopSteering.Leg leg) {
        this.leg = leg;
    }
}
