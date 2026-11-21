package gg.itzkatze.katze.game.entry;

import gg.itzkatze.katze.challenges.ChallengeType;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.UUID;

public record ActiveGameView(UUID roundId, List<ChallengeType> modes, Component phase, int participants) {
    public ActiveGameView {
        modes = List.copyOf(modes);
    }
}
