package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class LiveMemorySearchTest {
 @TempDir Path dir;
 @Test void searchFollowsGeneration()throws Exception{
  var server=new NaoLiveServer();
  server.configureSave(dir.resolve("latest.bin"));
  String initial=Files.readString(dir.resolve("latest-generation.txt")).trim();
  assertEquals("[]",server.searchMemory("採集"));
  for(int i=0;i<100;i++)server.advance();
  assertTrue(server.command("mip:run"));
  assertTrue(server.searchMemory("採集").contains("\"reason\":\"COLLECT\""));
  assertTrue(server.command("rewind:"+initial));
  assertEquals("[]",server.searchMemory("採集"));
  assertThrows(IllegalArgumentException.class,()->server.searchMemory("a".repeat(257)));
 }
}
