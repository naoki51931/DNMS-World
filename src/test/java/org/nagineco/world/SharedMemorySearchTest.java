package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class SharedMemorySearchTest {
 @TempDir Path dir;
 @Test void searchesIntegratedEvents()throws Exception{
  var memory=new DnmsMemoryAdapter(dir);memory.initialize(2);
  assertTrue(memory.search("採集",10).isEmpty());
  Files.writeString(dir.resolve("nao_01.md"),"- day=1 tick=20 position=(4,1,4) action=EAST emotion=NEUTRAL reason=COLLECT\n",StandardOpenOption.APPEND);
  Files.writeString(dir.resolve("nao_02.md"),"- day=1 tick=21 position=(5,1,4) action=WEST emotion=AFRAID reason=BUILD\n",StandardOpenOption.APPEND);
  memory.integrate();
  assertEquals(1,memory.search("採集した記憶",10).get(0).nao());
  assertEquals(2,memory.search("建築と恐怖",10).get(0).nao());
  assertTrue(memory.search("採集と恐怖",10).isEmpty());
  assertEquals(1,memory.search("COLLECT",1).size());
 }
}
