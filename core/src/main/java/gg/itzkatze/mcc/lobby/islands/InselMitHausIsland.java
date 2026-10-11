package gg.itzkatze.mcc.lobby.islands;

import gg.itzkatze.mcc.lobby.LobbyFeature;
import gg.itzkatze.mcc.lobby.LobbyIsland;
import gg.itzkatze.mcc.lobby.jumpandrun.JumpAndRun;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InselMitHausIsland extends LobbyIsland {

    @Override
    public @NotNull String getId() {
        return "inselmithaus";
    }

    @Override
    public @NotNull String getStructureName() {
        return "lobby/inselmithaus";
    }

    @Override
    public @NotNull Vector getSpawnPosition() {
        return new Vector(37, 9, 23);
    }

    @Override
    public @NotNull List<LobbyFeature> createFeatures() {
        return List.of(new JumpAndRun(new Vector(20, 13, 38)));
    }
}
