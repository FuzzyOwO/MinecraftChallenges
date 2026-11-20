package gg.itzkatze.katze.replay;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ReplayMining {
    private final Map<UUID, Attempt> attempts = new HashMap<>();

    private static final class Attempt {
        final Block block;
        float progress;
        Attempt(Block block) { this.block = block; }
    }

    void start(Player player, Block block) { attempts.put(player.getUniqueId(), new Attempt(block)); }
    void stop(UUID player) { attempts.remove(player); }

    void tick() {
        attempts.entrySet().removeIf(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            Attempt attempt = entry.getValue();
            if (player == null || player.isDead() || !player.getWorld().equals(attempt.block.getWorld()) || attempt.block.getType().isAir()
                    || !attempt.block.equals(player.getTargetBlockExact(6))) return true;
            attempt.progress = Math.min(0.99f, attempt.progress + Math.max(0, attempt.block.getBreakSpeed(player)));
            return false;
        });
    }

    List<ReplayFrame.Mining> snapshot() {
        return attempts.entrySet().stream().map(entry -> {
            Attempt attempt = entry.getValue();
            Block block = attempt.block;
            return new ReplayFrame.Mining(entry.getKey(), block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), attempt.progress);
        }).toList();
    }
}
