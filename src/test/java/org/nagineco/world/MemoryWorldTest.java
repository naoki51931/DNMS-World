package org.nagineco.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.nagineco.world.WorldAccess.Block.*;
class MemoryWorldTest {
    @Test void blockMutation() {
        var world = new MemoryWorld(); var p = new WorldAccess.Pos(1,2,3);
        assertEquals(AIR, world.block(p));
        assertTrue(world.compareAndSet(p,AIR,LOG));
        assertFalse(world.compareAndSet(p,AIR,STONE));
        assertEquals(LOG,world.block(p));
        assertTrue(world.compareAndSet(p,LOG,AIR));
        assertEquals(AIR,world.block(p));
    }
    @Test void ticks() { var world = new MemoryWorld(); world.advance(); assertEquals(1L,world.tick()); }
}
