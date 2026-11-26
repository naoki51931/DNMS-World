package org.nagineco.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PhysiologySnapshotTest {
 @Test void physiology(){
  var server=new NaoLiveServer();
  assertTrue(server.snapshot().contains("[0,4,1,4,\"COLLECT\",0,0,20.0,24000,3,0,false]"));
  assertTrue(server.command("damage:0:5"));
  assertTrue(server.snapshot().contains("[0,4,1,4,\"COLLECT\",0,0,15.0,24000,3,0,false]"));
  server.advance();
  assertTrue(server.snapshot().contains(",15.0,23999,3,1,false]"));
 }
}
