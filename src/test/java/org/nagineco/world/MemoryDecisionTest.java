package org.nagineco.world;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MemoryDecisionTest {
 @Test void scores(){
  var d=new MemoryDecision(List.of(
   new SharedMemorySearch.Event(1,1,20,"0,1,0","EAST","AFRAID","COLLECT"),
   new SharedMemorySearch.Event(2,1,21,"0,1,0","NORTH","NEUTRAL","COLLECT")));
  assertTrue(d.score("EAST")<d.score("WEST"));
  assertTrue(d.score("NORTH")>d.score("WEST"));
  assertEquals(0.0,new MemoryDecision(List.of()).score("EAST"));
 }
}
