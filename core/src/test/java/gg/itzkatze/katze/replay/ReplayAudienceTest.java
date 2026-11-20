package gg.itzkatze.katze.replay;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplayAudienceTest {
    @Test void theOriginalViewerCanLeaveAndOthersCanStillWatchControlAndJoin() {
        var audience = new ReplayAudience<String>();
        audience.add("original");
        audience.add("friend");
        assertFalse(audience.remove("original"));
        assertFalse(audience.closed());
        assertEquals(List.of("friend"), audience.viewers());
        audience.add("another friend");
        assertEquals(List.of("friend", "another friend"), audience.viewers());
    }

    @Test void replayResourcesAreReleasedExactlyOnceWhenTheLastViewerLeaves() {
        var audience = new ReplayAudience<String>();
        audience.add("original");
        audience.add("friend");
        int releases = 0;
        if (audience.remove("original")) releases++;
        if (audience.remove("friend")) releases++;
        if (audience.remove("friend")) releases++;
        assertEquals(1, releases);
        assertTrue(audience.closed());
        assertThrows(IllegalStateException.class, () -> audience.add("late joiner"));
    }
}
