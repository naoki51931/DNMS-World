package org.nagineco.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NaoLiveServerTest {
 @Test void controls(){
  var s=new NaoLiveServer();
  assertTrue(s.command("pause"));s.advance();assertTrue(s.snapshot().contains("\"tick\":0,"));
  assertTrue(s.command("4"));assertTrue(s.command("resume"));s.advance();
  assertTrue(s.snapshot().contains("\"tick\":4,"));
  assertTrue(s.command("next"));assertTrue(s.snapshot().contains("[24,"));
  assertTrue(s.snapshot().contains("\"tick\":4,"));
  assertFalse(s.command("invalid"));
  assertTrue(s.command("reset"));assertFalse(s.snapshot().contains("[24,"));
 }
 @Test void growthPreservesState(){
  var s=new NaoSimulation();for(int i=0;i<100;i++)s.tick();
  var before=s.states();for(int i=24;i<256;i++)assertTrue(s.addNao());
  assertFalse(s.addNao());assertEquals(256,s.states().size());
  assertEquals(before,s.states().subList(0,24));assertEquals(100,s.ticks());
 }
}
