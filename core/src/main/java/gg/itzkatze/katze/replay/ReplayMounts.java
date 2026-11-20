package gg.itzkatze.katze.replay;

import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ReplayMounts {
    private ReplayMounts() {}

    static void detachChanged(Map<UUID, Entity> actors, List<ReplayFrame.Actor> states) {
        for (ReplayFrame.Actor state : states) {
            Entity entity = actors.get(state.id());
            Entity desired = state.vehicle() == null ? null : actors.get(state.vehicle());
            if (entity != null && entity.getVehicle() != null && entity.getVehicle() != desired) entity.leaveVehicle();
        }
    }

    static void attach(Map<UUID, Entity> actors, List<ReplayFrame.Actor> states) {
        for (ReplayFrame.Actor state : states) {
            Entity passenger = actors.get(state.id());
            Entity vehicle = state.vehicle() == null ? null : actors.get(state.vehicle());
            if (passenger != null && vehicle != null && passenger != vehicle && passenger.getVehicle() != vehicle
                    && passenger.getWorld().equals(vehicle.getWorld())) vehicle.addPassenger(passenger);
        }
    }
}
