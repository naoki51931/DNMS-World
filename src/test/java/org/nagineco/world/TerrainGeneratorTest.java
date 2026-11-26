package org.nagineco.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.nagineco.world.WorldAccess.Block.*;
import org.nagineco.world.WorldAccess.Pos;
class TerrainGeneratorTest {
    @Test void deterministic() {
        var a=TerrainGenerator.generate(42,32); var b=TerrainGenerator.generate(42,32);
        int logs=0; int min=99,max=-1;
        for(int x=0;x<32;x++) for(int z=0;z<32;z++) {
            int grass=-1;
            for(int y=0;y<12;y++) {
                var p=new Pos(x,y,z); assertEquals(a.block(p),b.block(p));
                if(a.block(p)==LOG) logs++;
                if(a.block(p)==GRASS) grass=y;
            }
            assertTrue(grass>=0); min=Math.min(min,grass); max=Math.max(max,grass);
        }
        assertTrue(logs>0); assertTrue(max>min);
    }
    @Test void bounds() {
        assertThrows(IllegalArgumentException.class,()->TerrainGenerator.generate(0,7));
        assertThrows(IllegalArgumentException.class,()->TerrainGenerator.generate(0,129));
    }
}
