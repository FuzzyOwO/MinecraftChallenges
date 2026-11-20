package gg.itzkatze.katze.util;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

final class PlayerHeadRequests {
    private final Queue<UUID> queue = new ArrayDeque<>();
    private final Set<UUID> pending = new HashSet<>();
    private final Map<UUID, Long> retryAt = new HashMap<>();
    private UUID inFlight;
    private long nextRequest;

    synchronized void request(UUID player, long now) {
        if (now >= retryAt.getOrDefault(player, 0L) && pending.add(player)) queue.add(player);
    }

    synchronized UUID next(long now) {
        if (inFlight != null || now < nextRequest) return null;
        inFlight = queue.poll();
        if (inFlight != null) nextRequest = now + Duration.ofSeconds(5).toMillis();
        return inFlight;
    }

    synchronized void complete(UUID player, int status, long now) {
        if (!player.equals(inFlight)) return;
        inFlight = null;
        pending.remove(player);
        if (status == 200) retryAt.remove(player);
        else retryAt.put(player, now + (status == 404 ? Duration.ofDays(1) : Duration.ofMinutes(15)).toMillis());
        if (status == 429) nextRequest = now + Duration.ofMinutes(15).toMillis();
        else if (status >= 500) nextRequest = Math.max(nextRequest, now + Duration.ofMinutes(1).toMillis());
    }
}
