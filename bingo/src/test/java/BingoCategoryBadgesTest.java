import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BingoCategoryBadgesTest {
    @Test void swordUsesTheSameInsetAsOtherIconsAndKeepsATransparentBackground(@TempDir Path directory) throws Exception {
        Path minecraft = directory.resolve("minecraft");
        Files.createDirectories(minecraft);
        BufferedImage source = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        var graphics = source.createGraphics();
        graphics.setColor(Color.GRAY);
        graphics.fillRect(0, 0, 64, 64);
        graphics.setColor(new Color(0x4AEDD4));
        graphics.fillRect(56, 0, 8, 8);
        graphics.dispose();
        for (String material : List.of("chest", "diamond_sword", "oak_sapling", "nether_star")) {
            ImageIO.write(source, "png", minecraft.resolve(material + ".png").toFile());
        }

        var badges = BingoCategoryBadges.generate(directory);
        var sword = badges.get("MOB");
        assertEquals(64, sword.getWidth());
        assertEquals(64, sword.getHeight());
        assertEquals(0xFF4AEDD4, sword.getRGB(55, 8));
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            assertEquals(badges.get("ITEM").getRGB(x, y), sword.getRGB(x, y));
            if (x < 8 || x >= 56 || y < 8 || y >= 56) assertEquals(0, sword.getRGB(x, y) >>> 24);
        }
    }
}
