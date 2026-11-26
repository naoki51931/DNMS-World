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
 @Test void starvation(){
  var sim=new NaoSimulation(1);
  for(int i=0;i<60401;i++)sim.tick();
  assertEquals(0,sim.food(0));assertEquals(0,sim.satiety(0));
  assertTrue(sim.health(0)<20);
  sim.supplyFood(0,1);sim.tick();assertEquals(12000,sim.satiety(0));
 }
}
