package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class DnmsMemoryAdapterTest {
 @TempDir Path dir;
 @Test void integration()throws Exception{
  var adapter=new DnmsMemoryAdapter(dir);adapter.initialize(24);
  var sim=new NaoSimulation();
  for(int t=0;t<100;t++){var before=sim.states();sim.tick();var after=sim.states();for(int i=0;i<24;i++)adapter.observe(sim.ticks(),before.get(i),after.get(i));}
  adapter.integrate();
  var index=Files.readString(dir.resolve("nao_integrated_memory.md"));
  assertTrue(index.contains("- nao=01 day=1"));assertTrue(index.contains("- nao=24 day=1"));
  assertTrue(index.contains("rejected entries: 0"));
  assertEquals(18000,DnmsMemoryAdapter.clockTick(0,0));assertEquals(0,DnmsMemoryAdapter.clockTick(6,0));
 }
}
