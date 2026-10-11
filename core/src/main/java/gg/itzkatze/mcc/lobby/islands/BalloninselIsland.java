package gg.itzkatze.mcc.lobby.islands;

import gg.itzkatze.mcc.lobby.LobbyFeature;
import gg.itzkatze.mcc.lobby.LobbyIsland;
import gg.itzkatze.mcc.lobby.jumpandrun.JumpAndRun;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BalloninselIsland extends LobbyIsland {

    @Override
    public @NotNull String getId() {
        return "balloninsel";
    }

    @Override
    public @NotNull String getStructureName() {
        return "lobby/balloninsel";
    }

    @Override
    public @NotNull Vector getSpawnPosition() {
        return new Vector(27, 15, 23);
    }

    @Override
    public @NotNull StructureRotation getRotation() {
        return StructureRotation.COUNTERCLOCKWISE_90;
    }

    @Override
    public @NotNull List<LobbyFeature> createFeatures() {
        return List.of(new JumpAndRun(new Vector(31, 15, 23)));
    }
}
