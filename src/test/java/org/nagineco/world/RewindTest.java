package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class RewindTest {
 @TempDir Path dir;
 @Test void rewind()throws Exception{
  var server=new NaoLiveServer();server.configureSave(dir.resolve("latest.bin"));
  String id=Files.readString(dir.resolve("latest-generation.txt")).trim();
  assertTrue(Files.isRegularFile(dir.resolve("history").resolve(id+".bin")));
  assertTrue(Files.isDirectory(dir.resolve("history").resolve(id+".memory")));
  server.command("next");server.advance();assertTrue(server.snapshot().contains("[24,"));
  assertTrue(server.command("rewind:"+id));assertFalse(server.snapshot().contains("[24,"));
  assertTrue(server.snapshot().contains("\"tick\":0,"));assertTrue(server.snapshot().contains("\"running\":false"));
  assertFalse(server.command("rewind:../latest"));assertEquals(24,NaoSimulation.load(dir.resolve("latest.bin")).states().size());
 }
}
