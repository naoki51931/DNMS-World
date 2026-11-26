package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class RewindTest {
 @TempDir Path dir;
 @Test void rewind()throws Exception{
  var server=new NaoLiveServer();server.configureSave(dir.resolve("latest.bin"));
  String id;try(var files=Files.list(dir.resolve("history"))){id=files.findFirst().orElseThrow().getFileName().toString().replace(".bin","");}
  server.command("next");server.advance();assertTrue(server.snapshot().contains("[24,"));
  assertTrue(server.command("rewind:"+id));assertFalse(server.snapshot().contains("[24,"));
  assertTrue(server.snapshot().contains("\"tick\":0,"));assertTrue(server.snapshot().contains("\"running\":false"));
  assertFalse(server.command("rewind:../latest"));assertEquals(24,NaoSimulation.load(dir.resolve("latest.bin")).states().size());
 }
}
