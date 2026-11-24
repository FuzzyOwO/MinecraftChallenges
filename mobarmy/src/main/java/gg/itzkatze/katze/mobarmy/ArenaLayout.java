package gg.itzkatze.katze.mobarmy;

public class ArenaLayout {
    final int width;
    final int height;
    final int length;
    final int floor;
    final int roofLayer;

    ArenaLayout(int width, int height, int length, int floor, int roofLayer) {
        if (width < 3 || length < 3 || roofLayer < 2 || roofLayer >= height) throw new IllegalArgumentException("Invalid arena layout");
        this.width = width;
        this.height = height;
        this.length = length;
        this.floor = floor;
        this.roofLayer = roofLayer;
    }

    int width() { return width; }
    int length() { return length; }
    int roof() { return floor + roofLayer; }
    int maxHeight() { return Math.max(floor + height, roof() + 8); }

    boolean contains(double x, double y, double z, boolean waiting) {
        return x >= 0 && x < width && z >= 0 && z < length
                && y >= floor && y < (waiting ? roof() + 8 : roof());
    }

    boolean containsMob(double x, double y, double z) {
        return x >= 0 && x < width && z >= 0 && z < length && y >= floor && y < roof();
    }
}