package org.nagineco.world;
import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.util.UUID;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class WorldLifecycleIntegrationTest {
 @TempDir Path dir;
 @Test void authenticatedSpawnRewindAndRestart() throws Exception {
  Path path=dir.resolve("latest.bin");
  String initial,world,epoch;
  UUID id=UUID.randomUUID();
  String token="01234567890123456789012345678901";
  try(var app=new NaoLiveServer()){
   app.configureSave(path);
   initial=Files.readString(dir.resolve("latest-generation.txt")).trim();
   String[] identity=app.identity().split("\"");
   world=identity[3];epoch=identity[7];
   try(var duplicate=new NaoLiveServer()){
    assertThrows(java.nio.channels.OverlappingFileLockException.class,()->duplicate.configureSave(path));
   }
   var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
   app.registerWorldApi(server,token);server.start();
   try{
    var uri=URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/api/v1/npcs");
    var client=HttpClient.newHttpClient();
    var request=HttpRequest.newBuilder(uri).header("Authorization","Bearer "+token)
      .header("X-DNMS-World",world).header("X-DNMS-Epoch",epoch)
      .POST(HttpRequest.BodyPublishers.ofString(id+",100,1,16")).build();
    assertEquals(201,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
    assertEquals(409,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
    assertTrue(app.apiNpcs().contains(id.toString()));
    assertTrue(app.command("rewind:"+initial));
    assertFalse(app.apiNpcs().contains(id.toString()));
    assertEquals(409,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
    assertNotEquals(epoch,app.identity().split("\"")[7]);
   }finally{server.stop(0);}
  }
  try(var restored=new NaoLiveServer()){
   restored.configureSave(path);
   assertEquals(world,restored.identity().split("\"")[3]);
   assertFalse(restored.apiNpcs().contains(id.toString()));
  }
 }
}
