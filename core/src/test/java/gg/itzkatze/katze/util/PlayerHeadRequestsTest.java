package gg.itzkatze.katze.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerHeadRequestsTest {
    @Test
    void duplicateGuiRequestsProduceOnlyOneLookupAtATimeWithGlobalSpacing() {
        var requests = new PlayerHeadRequests();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        requests.request(first, 0);
        requests.request(first, 0);
        requests.request(second, 0);
        assertEquals(first, requests.next(0));
        assertNull(requests.next(6000));
        requests.complete(first, 200, 0);
        assertNull(requests.next(4999));
        assertEquals(second, requests.next(5000));
        requests.complete(second, 200, 5000);
        assertNull(requests.next(10000));
    }

    @Test
    void rateLimitingBacksOffAllLookupsAndMissingProfilesAreNotRepeated() {
        var requests = new PlayerHeadRequests();
        UUID limited = UUID.randomUUID(), next = UUID.randomUUID();
        requests.request(limited, 0);
        assertEquals(limited, requests.next(0));
        requests.complete(limited, 429, 0);
        requests.request(limited, 1);
        requests.request(next, 1);
        long backoff = Duration.ofMinutes(15).toMillis();
        assertNull(requests.next(backoff - 1));
        assertEquals(next, requests.next(backoff));
        requests.complete(next, 404, backoff);
        requests.request(next, backoff + 5000);
        assertNull(requests.next(backoff + 5000));
        long tomorrow = backoff + Duration.ofDays(1).toMillis();
        requests.request(next, tomorrow);
        assertEquals(next, requests.next(tomorrow));
    }
}
