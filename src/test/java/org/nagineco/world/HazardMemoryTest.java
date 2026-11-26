package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import org.nagineco.world.WorldAccess.Pos;
import static org.junit.jupiter.api.Assertions.*;
class HazardMemoryTest {
 @TempDir Path dir;
 @Test void hazardCoordinates(){
  var d=new MemoryDecision(List.of(new SharedMemorySearch.Event(1,1,0,"4,1,4","STAY","AFRAID","SAFETY")));
  assertEquals(3.0,d.hazard(new Pos(4,1,4)));
  assertEquals(0.0,d.hazard(new Pos(5,1,4)));
 }
 @Test void integrationAndRewind()throws Exception{
  var server=new NaoLiveServer();server.configureSave(dir.resolve("latest.bin"));
  String initial=Files.readString(dir.resolve("latest-generation.txt")).trim();
  assertTrue(server.command("damage:0:5"));
  assertEquals("[]",server.searchMemory("恐怖"));
  assertTrue(server.command("mip:run"));
  assertTrue(server.searchMemory("恐怖").contains("SAFETY"));
  assertTrue(server.command("rewind:"+initial));
  assertEquals("[]",server.searchMemory("恐怖"));
 }
}
