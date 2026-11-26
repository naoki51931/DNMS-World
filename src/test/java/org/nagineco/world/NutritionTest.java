package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class NutritionTest {
 @TempDir Path dir;
 @Test void mealsAndPersistence()throws Exception{
  var sim=new NaoSimulation(1);
  for(int i=0;i<11999;i++)sim.tick();
  assertEquals(12001,sim.satiety(0));assertEquals(3,sim.food(0));
  sim.tick();assertEquals(24000,sim.satiety(0));assertEquals(2,sim.food(0));
  var file=dir.resolve("nutrition.bin");sim.save(file);
  var restored=NaoSimulation.load(file);
  assertEquals(sim.satiety(0),restored.satiety(0));assertEquals(sim.food(0),restored.food(0));
  for(int i=0;i<12000;i++){sim.tick();restored.tick();}
  assertEquals(sim.satiety(0),restored.satiety(0));assertEquals(sim.food(0),restored.food(0));
 }
 @Test void renewableFoodAndRestore()throws Exception{
  var sim=new NaoSimulation(1);
  for(int i=0;i<45000;i++)sim.tick();
  assertTrue(sim.food(0)>0);
  var file=dir.resolve("forage.bin");sim.save(file);
  var restored=NaoSimulation.load(file);
  for(int i=0;i<72000;i++){sim.tick();restored.tick();}
  assertEquals(20.0,sim.health(0));
  assertEquals(sim.states(),restored.states());
  assertEquals(sim.food(0),restored.food(0));
  assertEquals(sim.satiety(0),restored.satiety(0));
 }
}
