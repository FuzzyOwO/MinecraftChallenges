package gg.itzkatze.katze.replay;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReplayWorldsTest {
    @Test void clocklessDimensionsKeepTheirTimeWhileWeatherStillUpdates() {
        for (var environment : List.of(World.Environment.NETHER, World.Environment.THE_END)) {
            var calls = new ArrayList<String>();
            World world = world(environment, false, 0, calls);
            ReplayWorlds.applyScene(world, new ReplayFrame.Scene(UUID.randomUUID(), environment.name(), 6000, true, true));
            assertEquals(List.of("storm:true", "thunder:true"), calls);
        }
    }

    @Test void normalWorldTimeUpdatesDuringPlaybackAndBackwardSeeking() {
        var calls = new ArrayList<String>();
        World world = world(World.Environment.NORMAL, false, 1000, calls);
        ReplayWorlds.applyScene(world, new ReplayFrame.Scene(UUID.randomUUID(), "NORMAL", 6000, false, false));
        ReplayWorlds.applyScene(world, new ReplayFrame.Scene(UUID.randomUUID(), "NORMAL", 500, false, false));
        assertEquals(List.of("time:6000", "time:500"), calls);
    }

    @Test void fixedTimeAndUnchangedScenesAvoidTimeMutations() {
        var calls = new ArrayList<String>();
        ReplayWorlds.applyScene(world(World.Environment.NORMAL, true, 0, calls), new ReplayFrame.Scene(UUID.randomUUID(), "NORMAL", 6000, false, false));
        ReplayWorlds.applyScene(world(World.Environment.NORMAL, false, 6000, calls), new ReplayFrame.Scene(UUID.randomUUID(), "NORMAL", 6000, false, false));
        assertTrue(calls.isEmpty());
    }

    private World world(World.Environment environment, boolean fixed, long time, List<String> calls) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getEnvironment" -> environment;
            case "isFixedTime" -> fixed;
            case "getTime" -> time;
            case "hasStorm", "isThundering" -> false;
            case "setTime" -> {
                if (environment != World.Environment.NORMAL || fixed) throw new IllegalArgumentException("Cannot set time in world without world clock");
                calls.add("time:" + args[0]);
                yield null;
            }
            case "setStorm" -> { calls.add("storm:" + args[0]); yield null; }
            case "setThundering" -> { calls.add("thunder:" + args[0]); yield null; }
            default -> throw new AssertionError(method.getName());
        });
    }
}
