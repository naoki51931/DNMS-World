package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import org.nagineco.doppel.NaoActionLearning.Action;
import static org.junit.jupiter.api.Assertions.*;
class HealthTest {
 @TempDir Path dir;
 @Test void healthAndReward()throws Exception{
  var a=new NaoSimulation(2);a.setRewardEnabled(true);
  for(int i=0;i<9;i++)a.tick();
  double before=a.rewardPreference(0,Action.EXPLORE);
  a.damage(0,5);assertEquals(15.0,a.health(0));assertEquals(20.0,a.health(1));
  assertTrue(a.rewardPreference(0,Action.EXPLORE)<before);
  a.heal(0,100);assertEquals(20.0,a.health(0));
  a.damage(0,100);assertEquals(0.0,a.health(0));a.heal(0,10);assertEquals(0.0,a.health(0));
  var state=a.states().get(0);a.tick();assertEquals(state,a.states().get(0));
  var path=dir.resolve("health.bin");a.save(path);var b=NaoSimulation.load(path);
  assertEquals(a.health(0),b.health(0));assertEquals(a.rewardPreference(0,Action.EXPLORE),b.rewardPreference(0,Action.EXPLORE));
  assertThrows(IllegalArgumentException.class,()->b.damage(0,Double.NaN));
 }
 @Test void disabledReward(){
  var a=new NaoSimulation(1);for(int i=0;i<9;i++)a.tick();a.damage(0,5);
  assertEquals(0.0,a.rewardPreference(0,Action.EXPLORE));
 }
}
