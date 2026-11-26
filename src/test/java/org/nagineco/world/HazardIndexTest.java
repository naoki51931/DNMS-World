package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import org.nagineco.world.WorldAccess.Pos;
import static org.junit.jupiter.api.Assertions.*;
class HazardIndexTest {
 @TempDir Path dir;
 @Test void allUniqueHazardsRemainAvailable()throws Exception{
  var file=dir.resolve("integrated.md");
  var data=new StringBuilder();
  for(int i=0;i<256;i++)data.append("- nao=01 day=1 tick=").append(i).append(" position=(").append(i).append(",1,0) action=STAY emotion=AFRAID reason=SAFETY\n");
  data.append("- nao=02 day=2 tick=0 position=(0,1,0) action=STAY emotion=AFRAID reason=SAFETY\n");
  Files.writeString(file,data);
  var search=new SharedMemorySearch(file);
  assertEquals(100,search.search("恐怖",100).size());
  assertEquals(256,search.hazardEvents().size());
  var decision=new MemoryDecision(search.hazardEvents());
  assertEquals(3.0,decision.hazard(new Pos(0,1,0)));
  assertEquals(3.0,decision.hazard(new Pos(255,1,0)));
  assertEquals(0.0,decision.hazard(new Pos(256,1,0)));
 }
}
