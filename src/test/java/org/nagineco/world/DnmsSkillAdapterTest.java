package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.nagineco.doppel.core.*;
import org.nagineco.world.WorldAccess.Pos;
import static org.junit.jupiter.api.Assertions.*;
import static org.nagineco.world.WorldAccess.Block.*;
class DnmsSkillAdapterTest {
 @Test void collectionMatchesUpstreamForEverySlot(){
  var skill=new PhysicalSkillSync.Skill(50,50,50,50,50,50);
  for(int slot=0;slot<256;slot++){
   var w=new MemoryWorld();var work=new WorkSession(w);var a=new DnmsSkillAdapter(work);
   var from=new Pos(0,1,0);var target=new Pos(1,1,0);
   w.compareAndSet(target,AIR,LOG);
   boolean expected=PhysicalWorkPerformance.canExecuteCollection(skill,slot,0);
   assertEquals(expected,a.gather(slot,0,skill,from,target));
   assertEquals(expected?AIR:LOG,w.block(target));
   assertEquals(expected?1:0,work.count("nao-"+slot,LOG));
   assertEquals(PhysicalMovementPerformance.stepDistance(skill),a.movementStep(skill));
  }
 }
}
