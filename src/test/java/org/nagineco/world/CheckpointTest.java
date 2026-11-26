package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class CheckpointTest {
 @TempDir Path dir;
 @Test void roundTrip()throws Exception{
  var a=new NaoSimulation();for(int i=0;i<37;i++)a.tick();a.addNao();
  var file=dir.resolve("world.bin");a.save(file);var b=NaoSimulation.load(file);
  assertEquals(a.states(),b.states());assertEquals(a.ticks(),b.ticks());
  for(int i=0;i<600;i++){a.tick();b.tick();assertEquals(a.states(),b.states());}
  for(int i=0;i<25;i++)assertEquals(a.inventory(i),b.inventory(i));
 }
}
