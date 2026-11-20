package gg.itzkatze.katze.replay;

import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayEffectsTest {
    @Test void fastPlaybackKeepsHurtEventsFromSkippedFramesAndPauseDoesNotRepeatThem() {
        UUID player = UUID.randomUUID();
        List<Float> hurt = new ArrayList<>();
        LivingEntity actor = (LivingEntity) Proxy.newProxyInstance(LivingEntity.class.getClassLoader(), new Class<?>[]{LivingEntity.class}, (proxy, method, args) -> {
            if (method.getName().equals("playHurtAnimation")) { hurt.add((float) args[0]); return null; }
            throw new AssertionError(method.getName());
        });
        var timeline = new ReplayTimeline(List.of(frame(0, player, false), frame(1, player, true), frame(2, player, false)), 3);
        var effects = new ReplayEffects();
        timeline.moveTo(2, (block, reverse) -> fail(), effects::capture);
        effects.play(Map.of(player, actor));
        assertEquals(List.of(45f), hurt);
        var paused = new ReplayEffects();
        timeline.moveTo(2, (block, reverse) -> fail(), paused::capture);
        paused.play(Map.of(player, actor));
        assertEquals(List.of(45f), hurt);
    }

    private ReplayFrame frame(int tick, UUID player, boolean hurt) {
        var actor = new ReplayFrame.Actor(player, UUID.randomUUID(), "PLAYER", "Katze", null, null, null,
                0, 64, 0, 0, 0, "STANDING", false, false, List.of(), null, hurt, 45, null, null);
        return new ReplayFrame(tick, List.of(actor), List.of(), List.of());
    }
}
