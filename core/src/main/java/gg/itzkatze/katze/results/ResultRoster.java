package gg.itzkatze.katze.results;

import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.replay.ReplayService;

import java.util.List;
import java.util.UUID;

public record ResultRoster(UUID id, long startedAt, List<Member> members) {
    public ResultRoster {
        members = List.copyOf(members);
    }

    public record Member(UUID id, String name) {}

    public static ResultRoster capture(ChallengeType type) {
        ResultRoster roster = new ResultRoster(UUID.randomUUID(), System.currentTimeMillis(), KatzePlayer.getOnlinePlayers().stream()
                .filter(KatzePlayer::isParticipating)
                .map(player -> new Member(player.getUuid(), player.getName())).toList());
        ReplayService.beginRound(roster.id(), roster.members().stream().map(Member::id).toList(), type);
        return roster;
    }
}
