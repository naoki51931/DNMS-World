package org.nagineco.world;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WorldHttpApiTest {
 @Test void authenticatedSpawn() throws Exception {
  var app=new NaoLiveServer();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  String token="01234567890123456789012345678901";
  app.registerWorldApi(server,token);server.start();
  try {
   var uri=URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/api/v1/npcs");
   var client=HttpClient.newHttpClient();
   assertEquals(401,client.send(HttpRequest.newBuilder(uri).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
   var id=UUID.randomUUID();
   var req=HttpRequest.newBuilder(uri).header("Authorization","Bearer "+token).POST(HttpRequest.BodyPublishers.ofString(id+",100,1,16")).build();
   assertEquals(201,client.send(req,HttpResponse.BodyHandlers.ofString()).statusCode());
   assertEquals(409,client.send(req,HttpResponse.BodyHandlers.ofString()).statusCode());
   var list=client.send(HttpRequest.newBuilder(uri).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
   assertEquals(200,list.statusCode());assertTrue(list.body().contains(id.toString()));
  }finally{server.stop(0);}
 }
}
