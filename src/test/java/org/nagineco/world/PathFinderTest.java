package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.nagineco.world.WorldAccess.*;
import static org.junit.jupiter.api.Assertions.*;
class PathFinderTest {
 private MemoryWorld floor(){
  var w=new MemoryWorld();
  for(int x=0;x<4;x++)for(int z=0;z<3;z++)w.compareAndSet(new Pos(x,0,z),Block.AIR,Block.DIRT);
  return w;
 }
 @Test void detour(){
  var w=floor();var start=new Pos(0,1,1);var goal=new Pos(3,1,1);
  w.compareAndSet(new Pos(1,1,1),Block.AIR,Block.STONE);
  var p=start;
  for(int i=0;i<5;i++)p=PathFinder.next(w,p,goal,(a,b)->0).orElseThrow();
  assertEquals(goal,p);
 }
 @Test void unsupported(){
  var w=floor();var goal=new Pos(3,1,1);
  w.compareAndSet(new Pos(3,0,1),Block.DIRT,Block.AIR);
  assertTrue(PathFinder.next(w,new Pos(0,1,1),goal,(a,b)->0).isEmpty());
 }
 @Test void avoidsCostlyCell(){
  var w=floor();var start=new Pos(0,1,1);var goal=new Pos(3,1,1);
  var risky=new Pos(1,1,1);
  assertEquals(risky,PathFinder.next(w,start,goal,(a,b)->0).orElseThrow());
  assertNotEquals(risky,PathFinder.next(w,start,goal,(a,b)->b.equals(risky)?4:0).orElseThrow());
 }
}
