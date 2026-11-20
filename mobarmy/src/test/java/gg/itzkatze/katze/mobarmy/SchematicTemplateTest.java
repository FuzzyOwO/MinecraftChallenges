package gg.itzkatze.katze.mobarmy;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SchematicTemplateTest {
    @Test void blockStateFieldNamesMatchTheSchematicDataVersion() throws Exception {
        for (int version : new int[]{4325, 5005, 5006, 5023}) {
            var root = CompoundBinaryTag.builder().putInt("Version", 2).putInt("DataVersion", version)
                    .putShort("Width", (short) 3).putShort("Height", (short) 3).putShort("Length", (short) 3)
                    .put("Palette", CompoundBinaryTag.builder().putInt("minecraft:oak_log[axis=x]", 0).build())
                    .putByteArray("BlockData", new byte[27]).build();
            var template = SchematicTemplate.read(encoded(root));
            var part = template.parts().getFirst();
            var nativeRoot = BinaryTagIO.reader(8_000_000).read(new ByteArrayInputStream(part.nbt()), BinaryTagIO.Compression.GZIP);
            var state = nativeRoot.getList("palette").getCompound(0);
            String name = version >= 5006 ? "id" : "Name";
            String properties = version >= 5006 ? "properties" : "Properties";
            assertEquals(version, nativeRoot.getInt("DataVersion"));
            assertEquals("minecraft:oak_log", state.getString(name));
            assertEquals("x", state.getCompound(properties).getString("axis"));
        }
    }

    @Test void spongeThreeLoadsButDoesNotInventAMissingRoof() throws Exception {
        var blocks = CompoundBinaryTag.builder().put("Palette", CompoundBinaryTag.builder().putInt("minecraft:stone", 0).build())
                .putByteArray("Data", new byte[27]).build();
        var root = CompoundBinaryTag.builder().putInt("Version", 3).putInt("DataVersion", 5023)
                .putShort("Width", (short) 3).putShort("Height", (short) 3).putShort("Length", (short) 3).put("Blocks", blocks).build();
        var template = SchematicTemplate.read(encoded(root));
        assertThrows(IllegalStateException.class, template::roofLayer);
        assertEquals("minecraft:stone", template.block(2, 2, 2));
    }

    @Test void corruptBlockDataAndMissingAssetsFailBeforePlacement() throws Exception {
        assertThrows(IOException.class, () -> SchematicTemplate.read(null));
        var root = CompoundBinaryTag.builder().putInt("Version", 2).putShort("Width", (short) 3)
                .putShort("Height", (short) 3).putShort("Length", (short) 3)
                .put("Palette", CompoundBinaryTag.builder().putInt("minecraft:air", 0).build()).putByteArray("BlockData", new byte[2]).build();
        assertThrows(IOException.class, () -> SchematicTemplate.read(encoded(root)));
    }

    private static ByteArrayInputStream encoded(CompoundBinaryTag root) throws IOException {
        var output = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(root, output, BinaryTagIO.Compression.GZIP);
        return new ByteArrayInputStream(output.toByteArray());
    }
}
