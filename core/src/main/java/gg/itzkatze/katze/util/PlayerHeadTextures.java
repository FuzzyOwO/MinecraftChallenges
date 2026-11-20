package gg.itzkatze.katze.util;

import com.destroystokyo.paper.profile.ProfileProperty;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;

final class PlayerHeadTextures {
    private final Map<UUID, List<ProfileProperty>> textures = new ConcurrentHashMap<>();

    private record Texture(String name, String value, String signature) {}
    private record Stored(Map<UUID, List<Texture>> players) {}

    List<ProfileProperty> get(UUID player) { return textures.getOrDefault(player, List.of()); }

    boolean remember(UUID player, Collection<ProfileProperty> properties) {
        List<ProfileProperty> available = properties.stream().filter(property -> property.getName().equals("textures") && !property.getValue().isBlank()).toList();
        return !available.isEmpty() && !available.equals(textures.put(player, available));
    }

    void load(Path path) throws IOException {
        Stored stored = JsonFiles.read(path, Stored.class);
        if (stored == null || stored.players() == null) return;
        stored.players().forEach((player, properties) -> remember(player, properties.stream()
                .map(property -> new ProfileProperty(property.name(), property.value(), property.signature())).toList()));
    }

    void save(Path path) throws IOException {
        Map<UUID, List<Texture>> players = new LinkedHashMap<>();
        textures.forEach((player, properties) -> players.put(player, properties.stream()
                .map(property -> new Texture(property.getName(), property.getValue(), property.getSignature())).toList()));
        JsonFiles.write(path, new Stored(players));
    }
}
