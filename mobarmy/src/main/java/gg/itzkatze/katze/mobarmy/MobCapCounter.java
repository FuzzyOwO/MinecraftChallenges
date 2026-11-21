package gg.itzkatze.katze.mobarmy;

import java.util.HashMap;
import java.util.Map;

final class MobCapCounter {

    private final Map<String, Map<String, Integer>> kills = new HashMap<>();

    int recordKill(String sideId, String mobType) {
        return kills.computeIfAbsent(sideId, key -> new HashMap<>()).merge(mobType, 1, Integer::sum);
    }

    void reset() {kills.clear();}

}
