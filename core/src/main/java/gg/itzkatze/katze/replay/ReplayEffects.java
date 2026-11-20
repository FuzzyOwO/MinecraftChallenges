package gg.itzkatze.katze.replay;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class ReplayEffects {
    private final Map<UUID, Float> injuries = new HashMap<>();
    private final Set<UUID> swings = new HashSet<>();

    void capture(ReplayFrame frame) {
        for (ReplayFrame.Actor actor : frame.actors()) {
            if (actor.hurt()) injuries.put(actor.id(), actor.hurtDirection());
            if (actor.swinging()) swings.add(actor.id());
        }
    }

    void play(Map<UUID, Entity> actors) {
        injuries.forEach((id, direction) -> { if (actors.get(id) instanceof LivingEntity living) living.playHurtAnimation(direction); });
        swings.forEach(id -> { if (actors.get(id) instanceof LivingEntity living) living.swingMainHand(); });
    }
}
