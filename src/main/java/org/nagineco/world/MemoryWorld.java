package org.nagineco.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MemoryWorld implements WorldAccess {
    private final Map<Pos, Block> blocks = new HashMap<>();
    private long tick;
    public synchronized Block block(Pos pos) { return blocks.getOrDefault(Objects.requireNonNull(pos), Block.AIR); }
    public synchronized boolean compareAndSet(Pos pos, Block expected, Block replacement) {
        Objects.requireNonNull(expected); Objects.requireNonNull(replacement);
        if (block(pos) != expected) return false;
        if (replacement == Block.AIR) blocks.remove(pos); else blocks.put(pos, replacement);
        return true;
    }
    public synchronized long tick() { return tick; }
    public synchronized void advance() { tick = Math.incrementExact(tick); }
}
