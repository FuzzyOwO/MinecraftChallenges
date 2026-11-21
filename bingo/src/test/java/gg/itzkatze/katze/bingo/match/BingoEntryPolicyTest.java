package gg.itzkatze.katze.bingo.match;

import gg.itzkatze.katze.bingo.board.BingoObjectiveCatalog;
import gg.itzkatze.katze.bingo.pack.BingoPackAdmission;
import gg.itzkatze.katze.bingo.pack.BingoResourcePack;
import gg.itzkatze.katze.game.entry.EntryReason;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BingoEntryPolicyTest {
    @Test void packReadinessAllowsOnlyAssignedActiveParticipant() throws Exception {
        UUID player = UUID.randomUUID();
        UUID opponent = UUID.randomUUID();
        BingoResourcePack pack = new BingoResourcePack();
        BingoMatchManager matches = new BingoMatchManager(new BingoObjectiveCatalog(), pack);
        BingoCompetitor first = new BingoCompetitor("a", "A", null, List.of(new BingoCompetitor.Member(player, "A")));
        BingoCompetitor second = new BingoCompetitor("b", "B", null, List.of(new BingoCompetitor.Member(opponent, "B")));
        BingoMatch match = new BingoMatch(List.of(first, second), new ArrayList<>(Collections.nCopies(25, "objective")), 60, 1);
        Field matchesField = BingoMatchManager.class.getDeclaredField("matches");
        matchesField.setAccessible(true);
        @SuppressWarnings("unchecked") List<BingoMatch> games = (List<BingoMatch>) matchesField.get(matches);
        games.add(match);
        BingoEntryPolicy policy = new BingoEntryPolicy(matches, pack);
        assertFalse(policy.prepare(player, EntryReason.REJOIN).allowed());
        Field admissionField = BingoResourcePack.class.getDeclaredField("admission");
        admissionField.setAccessible(true);
        BingoPackAdmission admission = (BingoPackAdmission) admissionField.get(pack);
        admission.begin(player, 0);
        admission.update(player, Status.SUCCESSFULLY_LOADED);
        assertTrue(policy.prepare(player, EntryReason.REJOIN).allowed());
        assertFalse(policy.prepare(UUID.randomUUID(), EntryReason.REJOIN).allowed());
        match.result = BingoMatch.Result.LINE;
        assertFalse(policy.prepare(player, EntryReason.REJOIN).allowed());
    }
}
