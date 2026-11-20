package gg.itzkatze.katze.replay;

import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

final class ReplayEntitySnapshots {
    private static final Set<String> BLOCK_ATTACHED = Set.of("PAINTING", "ITEM_FRAME", "GLOW_ITEM_FRAME", "LEASH_KNOT");

    private ReplayEntitySnapshots() {}

    static String prepare(ReplayFrame.Actor definition, ReplayFrame.Actor state) {
        String snapshot = definition.snapshot();
        if (snapshot == null || !BLOCK_ATTACHED.contains(state.type()) && !snapshot.contains("Passengers") && !snapshot.contains("passengers")) return snapshot;
        try {
            CompoundBinaryTag data = TagStringIO.tagStringIO().asCompound(snapshot).remove("Passengers").remove("passengers");
            if (BLOCK_ATTACHED.contains(state.type())) {
                int[] original = data.getIntArray("block_pos");
                int[] anchor = {(int) Math.floor(state.x()), (int) Math.floor(state.y()), (int) Math.floor(state.z())};
                if (original.length == 3 && near(original, definition)) {
                    anchor[0] += original[0] - (int) Math.floor(definition.x());
                    anchor[1] += original[1] - (int) Math.floor(definition.y());
                    anchor[2] += original[2] - (int) Math.floor(definition.z());
                }
                data = data.putIntArray("block_pos", anchor).put("Pos", ListBinaryTag.listBinaryTag(BinaryTagTypes.DOUBLE,
                        List.of(DoubleBinaryTag.doubleBinaryTag(state.x()), DoubleBinaryTag.doubleBinaryTag(state.y()), DoubleBinaryTag.doubleBinaryTag(state.z()))));
            }
            return TagStringIO.tagStringIO().asString(data);
        } catch (IOException e) { throw new UncheckedIOException("Invalid replay entity snapshot", e); }
    }

    private static boolean near(int[] anchor, ReplayFrame.Actor definition) {
        return Math.abs(anchor[0] - definition.x()) < 16 && Math.abs(anchor[1] - definition.y()) < 16 && Math.abs(anchor[2] - definition.z()) < 16;
    }
}
