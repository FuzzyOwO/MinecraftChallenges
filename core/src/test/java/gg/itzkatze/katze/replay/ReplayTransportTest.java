package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplayTransportTest {
    @Test void multipleViewersAdvanceTheSharedTimelineOnlyOncePerServerTick() {
        var transport = new ReplayTransport();
        var timeline = new ReplayTimeline(List.of(new ReplayFrame(0, List.of(), List.of(), List.of())), 100);
        for (int viewer = 0; viewer < 4; viewer++) {
            if (transport.claimTick(100)) timeline.moveTo(timeline.position() + transport.speed(), (block, reverse) -> fail());
        }
        assertEquals(1, timeline.position());
        for (int viewer = 0; viewer < 4; viewer++) {
            if (transport.claimTick(101)) timeline.moveTo(timeline.position() + transport.speed(), (block, reverse) -> fail());
        }
        assertEquals(2, timeline.position());
    }

    @Test void pauseSpeedAndSeekingRemainSharedWhenAnotherViewerJoins() {
        var transport = new ReplayTransport();
        var audience = new ReplayAudience<String>();
        var timeline = new ReplayTimeline(List.of(new ReplayFrame(0, List.of(), List.of(), List.of())), 100);
        audience.add("first");
        timeline.moveTo(40, (block, reverse) -> fail());
        transport.togglePause();
        transport.cycleSpeed();
        audience.add("second");
        assertTrue(transport.paused());
        assertEquals(2, transport.speed());
        assertEquals(40, timeline.position());

        transport.togglePause();
        for (String viewer : audience.viewers()) {
            if (transport.claimTick(102) && !transport.paused()) timeline.moveTo(timeline.position() + transport.speed(), (block, reverse) -> fail());
        }
        assertEquals(42, timeline.position());
        timeline.moveTo(10, (block, reverse) -> fail());
        assertEquals(10, timeline.position());
        transport.pause();
        assertTrue(transport.paused());
    }
}
