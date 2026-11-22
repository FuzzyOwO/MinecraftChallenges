package gg.itzkatze.katze.replay;

import java.util.List;
import java.util.UUID;
import java.util.Map;

public record ReplayFrame(int tick, List<Actor> actors, List<BlockChange> blocks, List<Scene> scenes, List<Mining> mining, List<Container> containers) {
    public ReplayFrame {
        mining = mining == null ? List.of() : List.copyOf(mining);
        containers = containers == null ? List.of() : List.copyOf(containers);
    }

    public ReplayFrame(int tick, List<Actor> actors, List<BlockChange> blocks, List<Scene> scenes, List<Mining> mining) {
        this(tick, actors, blocks, scenes, mining, List.of());
    }

    public ReplayFrame(int tick, List<Actor> actors, List<BlockChange> blocks, List<Scene> scenes) {
        this(tick, actors, blocks, scenes, List.of());
    }
    boolean hasParticipant(List<UUID> participants) {
        return actors.stream().anyMatch(actor -> actor.participant(participants));
    }

    public record Actor(UUID id, UUID world, String type, String name, String snapshot,
                        String texture, String signature, double x, double y, double z,
                        float yaw, float pitch, String pose, boolean burning, boolean swinging,
                        List<String> equipment, UUID vehicle, boolean hurt, float hurtDirection, String item, PlayerState player,
                        double health, double maxHealth) {
        public Actor(UUID id, UUID world, String type, String name, String snapshot, String texture, String signature,
                     double x, double y, double z, float yaw, float pitch, String pose, boolean burning, boolean swinging, List<String> equipment) {
            this(id, world, type, name, snapshot, texture, signature, x, y, z, yaw, pitch, pose, burning, swinging, equipment, null, false, 0, null, null, 0, 0);
        }
        boolean participant(List<UUID> participants) {
            return "PLAYER".equals(type) && participants.contains(id);
        }
    }

    public record PlayerState(List<String> inventory, List<String> backpack, Integer team, double health, double maxHealth,
                              int food, int level, String world, List<String> information, Map<String, List<String>> scoreboards) {
        public PlayerState {
            scoreboards = scoreboards == null ? Map.of() : Map.copyOf(scoreboards);
        }

        public PlayerState(List<String> inventory, List<String> backpack, Integer team, double health, double maxHealth,
                           int food, int level, String world, List<String> information) {
            this(inventory, backpack, team, health, maxHealth, food, level, world, information, Map.of());
        }
    }

    public record Mining(UUID player, UUID world, int x, int y, int z, float progress) {}

    public record Container(UUID world, int x, int y, int z) {}

    public record BlockChange(UUID world, int x, int y, int z, String before, String after) {}

    public record Scene(UUID world, String environment, long time, boolean storm, boolean thunder) {}
}