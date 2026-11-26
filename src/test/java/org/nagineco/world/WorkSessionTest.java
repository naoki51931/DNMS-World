package org.nagineco.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.nagineco.world.WorldAccess.Block.*;
import org.nagineco.world.WorldAccess.Pos;
class WorkSessionTest {
 @Test void gatherAndBuild(){
  var w=new MemoryWorld();var s=new WorkSession(w);
  var actor=new Pos(0,1,0);var source=new Pos(1,1,0);var target=new Pos(2,1,0);
  w.compareAndSet(source,AIR,LOG);w.compareAndSet(new Pos(2,0,0),AIR,DIRT);
  assertFalse(s.gather("B",new Pos(20,1,0),source));
  assertTrue(s.gather("A",actor,source));assertFalse(s.gather("B",actor,source));
  assertEquals(1,s.count("A",LOG));assertEquals(AIR,w.block(source));
  assertFalse(s.place("B",actor,target,LOG));
  assertFalse(s.place("A",actor,new Pos(0,3,0),LOG));
  assertTrue(s.place("A",actor,target,LOG));assertEquals(0,s.count("A",LOG));
  assertEquals(LOG,w.block(target));assertFalse(s.gather("B",actor,target));assertFalse(s.gather("A",actor,target));
 }
}
