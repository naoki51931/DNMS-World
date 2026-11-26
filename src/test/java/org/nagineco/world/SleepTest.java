package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class SleepTest {
 @TempDir Path dir;
 @Test void sleepAndRestore()throws Exception{
  var sim=new NaoSimulation(1);
  for(int i=0;i<12100;i++)sim.tick();
  assertTrue(sim.sleeping(0));
  sim.damage(0,5);
  var file=dir.resolve("sleep.bin");sim.save(file);
  var restored=NaoSimulation.load(file);
  assertTrue(restored.sleeping(0));
  assertEquals(sim.fatigue(0),restored.fatigue(0));
  for(int i=0;i<5000;i++){sim.tick();restored.tick();}
  assertFalse(sim.sleeping(0));
  assertEquals(20.0,sim.health(0));
  assertEquals(sim.health(0),restored.health(0));
  assertEquals(sim.fatigue(0),restored.fatigue(0));
  assertEquals(sim.states(),restored.states());
 }
}
