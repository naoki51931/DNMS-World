package org.nagineco.world;

/** Engine-neutral block access; no Minecraft classes. */
public interface WorldAccess {
    record Pos(int x, int y, int z) {}
    enum Block { AIR, GRASS, DIRT, STONE, LOG }
    Block block(Pos pos);
    boolean compareAndSet(Pos pos, Block expected, Block replacement);
    long tick();
}
