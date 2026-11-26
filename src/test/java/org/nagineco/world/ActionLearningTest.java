package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import org.nagineco.doppel.NaoActionLearning.Action;
import org.nagineco.doppel.core.PhysicalSkillSync.Mode;
import static org.junit.jupiter.api.Assertions.*;
class ActionLearningTest {
 @TempDir Path dir;
 @Test void learnsAndRestores()throws Exception{
  var a=new NaoSimulation(24);
  assertEquals(0.5,a.confidence(0,Action.COLLECT));
  for(int i=0;i<100;i++)a.tick();
  assertTrue(a.skill(0).agility()>0);
  assertTrue(a.skill(0).technique()>0);
  assertTrue(a.confidence(0,Action.EXPLORE)>0.5);
  var path=dir.resolve("learn.bin");a.save(path);var b=NaoSimulation.load(path);
  for(int i=0;i<600;i++){a.tick();b.tick();}
  assertEquals(a.states(),b.states());
  for(int slot=0;slot<24;slot++){
   assertEquals(a.skill(slot),b.skill(slot));
   for(var action:Action.values())assertEquals(a.confidence(slot,action),b.confidence(slot,action));
  }
  assertEquals(NaoSimulation.Goal.REST,b.states().get(0).goal());
 }
 @Test void synchronizationDoesNotDuplicateEvidence(){
  var a=new NaoSimulation(2);for(int i=0;i<100;i++)a.tick();
  var before=a.confidence(1,Action.COLLECT);
  a.synchronizeSkills(0,Mode.FULL);
  assertEquals(before,a.confidence(1,Action.COLLECT));
  assertEquals(a.skill(0),a.skill(1));
 }
}
