package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class RouteCacheTest {
 @TempDir Path dir;
 @Test void cacheAndRestore()throws Exception{
  var sim=new NaoSimulation(1);
  for(int i=0;i<100;i++)sim.tick();
  assertTrue(sim.routeSearches()>0);
  assertTrue(sim.routeHits()>0);
  var file=dir.resolve("world.bin");sim.save(file);
  var restored=NaoSimulation.load(file);
  assertEquals(0,restored.routeHits());
  for(int i=0;i<600;i++){sim.tick();restored.tick();assertEquals(sim.states(),restored.states());}
 }
 @Test void populationDiagnostics(){
  for(int count:new int[]{24,64,128,256}){
   var sim=new NaoSimulation(count);
   long start=System.nanoTime();
   for(int i=0;i<600;i++)sim.tick();
   double ms=(System.nanoTime()-start)/1e6;
   assertEquals(count,sim.states().size());
   assertTrue(sim.states().stream().allMatch(s->s.goal()==NaoSimulation.Goal.REST));
   System.out.printf("NPC=%d ticks=600 ms=%.3f searches=%d hits=%d%n",count,ms,sim.routeSearches(),sim.routeHits());
  }
 }
}
