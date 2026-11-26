package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import org.nagineco.doppel.NaoActionLearning.Action;
import static org.junit.jupiter.api.Assertions.*;
class RewardEstimateTest {
 @TempDir Path dir;
 @Test void estimate(){
  var r=new RewardEstimate();r.record(100);r.record(-100);r.record(Double.NaN);
  assertEquals(2,r.count());assertEquals(0.0,r.mean());
  r.record(6);assertEquals(2.0,r.mean());assertEquals(0.6,r.preference(),1e-12);
 }
 @Test void optionalAndPersistent()throws Exception{
  var a=new NaoSimulation(2);assertFalse(a.rewardEnabled());
  a.setRewardEnabled(true);for(int i=0;i<100;i++)a.tick();
  assertTrue(a.rewardPreference(0,Action.COLLECT)>0);
  var path=dir.resolve("reward.bin");a.save(path);var b=NaoSimulation.load(path);
  assertTrue(b.rewardEnabled());assertEquals(a.rewardPreference(0,Action.COLLECT),b.rewardPreference(0,Action.COLLECT));
  b.setRewardEnabled(false);double old=b.rewardPreference(0,Action.COLLECT);for(int i=0;i<100;i++)b.tick();assertEquals(old,b.rewardPreference(0,Action.COLLECT));
 }
}
