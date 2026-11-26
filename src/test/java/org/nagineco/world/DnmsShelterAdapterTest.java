package org.nagineco.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.nagineco.world.WorldAccess.Block.*;
import org.nagineco.world.WorldAccess.Pos;
import org.nagineco.doppel.core.NaoBuildingPlan;
class DnmsShelterAdapterTest {
 @Test void upstreamShelter(){
  var w=new MemoryWorld();var work=new WorkSession(w);var adapter=new DnmsShelterAdapter(work);
  var anchor=new Pos(0,1,0);
  for(var o:NaoBuildingPlan.starter())w.compareAndSet(new Pos(o.x(),0,o.z()),AIR,DIRT);
  assertEquals(0,adapter.buildNext("A",anchor,anchor,0));
  int completed=0;
  for(int i=0;i<8;i++){
   var source=new Pos(2,1,0);assertTrue(w.compareAndSet(source,AIR,LOG));
   assertTrue(work.gather("A",anchor,source));
   completed=adapter.buildNext("A",anchor,anchor,completed);
   assertEquals(i+1,completed);
  }
  assertEquals(8,adapter.buildNext("A",anchor,anchor,completed));
  assertEquals(0,work.count("A",LOG));
  for(var o:NaoBuildingPlan.starter())assertEquals(LOG,w.block(new Pos(o.x(),1+o.y(),o.z())));
 }
}
