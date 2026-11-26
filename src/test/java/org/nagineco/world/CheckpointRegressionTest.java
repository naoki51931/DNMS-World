package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nagineco.doppel.NaoActionLearning.Action;
import java.nio.file.*;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
class CheckpointRegressionTest {
 @TempDir Path dir;
 @Test void populatedV4()throws Exception{
  var a=new NaoSimulation(256);a.setRewardEnabled(true);
  for(int i=0;i<100;i++)a.tick();
  a.damage(0,5);a.damage(255,20);
  var file=dir.resolve("world.bin");a.save(file);
  var b=NaoSimulation.load(file);
  assertEquals(a.states(),b.states());assertEquals(a.ticks(),b.ticks());
  assertTrue(b.rewardEnabled());
  for(int i=0;i<256;i++){
   assertEquals(a.health(i),b.health(i));assertEquals(a.skill(i),b.skill(i));
   assertEquals(a.inventory(i),b.inventory(i));
   for(var action:Action.values()){
    assertEquals(a.confidence(i,action),b.confidence(i,action));
    assertEquals(a.rewardPreference(i,action),b.rewardPreference(i,action));
   }
  }
  for(int i=0;i<100;i++){a.tick();b.tick();}
  assertEquals(a.states(),b.states());
 }
 @Test void truncatedCheckpointRejected()throws Exception{
  var file=dir.resolve("world.bin");new NaoSimulation(2).save(file);
  byte[] bytes=Files.readAllBytes(file);
  Files.write(file,Arrays.copyOf(bytes,bytes.length-1));
  assertThrows(IOException.class,()->NaoSimulation.load(file));
 }
}
