package gg.itzkatze.mcc.lobby.islands;

import gg.itzkatze.mcc.lobby.LobbyIsland;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class ClassicIsland extends LobbyIsland {

    @Override
    public @NotNull String getId() {
        return "classic";
    }

    @Override
    public @NotNull String getStructureName() {
        return "lobby/classic";
    }

    @Override
    public @NotNull Vector getSpawnPosition() {
        return new Vector(41, 10, 28);
    }

    @Override
    public @NotNull StructureRotation getRotation() {
        return StructureRotation.COUNTERCLOCKWISE_90;
    }
}
