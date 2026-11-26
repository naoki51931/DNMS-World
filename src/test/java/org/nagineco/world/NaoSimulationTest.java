package org.nagineco.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.nagineco.world.WorldAccess.*;
import org.nagineco.doppel.core.NaoBuildingPlan;
class NaoSimulationTest {
 @Test void twentyFourCompleteShelters(){verify(24);}
 @Test void maxPopulation(){verify(256);}
 private void verify(int count){
  var sim=new NaoSimulation(count);
  for(int i=0;i<600;i++)sim.tick();
  assertEquals(600,sim.ticks());assertEquals(count,sim.states().size());
  for(var n:sim.states()){
   assertEquals(NaoSimulation.Goal.REST,n.goal(),"slot "+n.slot());
   assertEquals(8,n.collected());assertEquals(8,n.built());assertEquals(0,sim.inventory(n.slot()));
   for(var o:NaoBuildingPlan.starter())assertEquals(Block.LOG,sim.block(new Pos(n.position().x()+o.x(),1+o.y(),n.position().z()+o.z())));
  }
 }
 @Test void defaultAndBounds(){assertEquals(24,new NaoSimulation().states().size());assertThrows(IllegalArgumentException.class,()->new NaoSimulation(257));}
}
