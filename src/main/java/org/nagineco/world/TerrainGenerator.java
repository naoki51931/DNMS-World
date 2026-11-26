package org.nagineco.world;
import static org.nagineco.world.WorldAccess.Block.*;
import org.nagineco.world.WorldAccess.Pos;
/** Deterministic bounded terrain; intended for fresh worlds only. */
public final class TerrainGenerator {
    private TerrainGenerator() {}
    public static MemoryWorld generate(long seed, int size) {
        if (size < 8 || size > 128) throw new IllegalArgumentException("size");
        var world = new MemoryWorld();
        var random = new java.util.Random(seed);
        for (int x=0;x<size;x++) for(int z=0;z<size;z++) {
            int h=3+(int)Math.round(1.5*Math.sin(x*0.19+seed%31)+1.5*Math.cos(z*0.17+seed%23));
            for(int y=0;y<=h;y++) world.compareAndSet(new Pos(x,y,z),AIR,y==h?GRASS:y>=h-2?DIRT:STONE);
            if(x>1 && z>1 && x<size-2 && z<size-2 && random.nextInt(35)==0)
                for(int y=h+1;y<=h+3;y++) world.compareAndSet(new Pos(x,y,z),AIR,LOG);
        }
        return world;
    }
}
