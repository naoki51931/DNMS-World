package org.nagineco.world;
import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
public final class NaoLiveServer {
 private NaoSimulation sim=new NaoSimulation();
 private boolean running=true;
 private int speed=1;
 synchronized void advance(){if(running)for(int i=0;i<speed;i++)sim.tick();}
 synchronized String snapshot(){
  var out=new StringBuilder("{\"tick\":").append(sim.ticks()).append(",\"running\":").append(running).append(",\"speed\":").append(speed).append(",\"naos\":[");
  boolean first=true;
  for(var n:sim.states()){
   if(!first)out.append(',');first=false;var p=n.position();
   out.append('[').append(n.slot()).append(',').append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(",\"").append(n.goal()).append("\",").append(n.collected()).append(',').append(n.built()).append(']');
  }
  return out.append("]}").toString();
 }
 synchronized boolean command(String cmd){
  switch(cmd){case "pause"->running=false;case "resume"->running=true;case "reset"->sim=new NaoSimulation();case "1","2","4"->speed=Integer.parseInt(cmd);default->{return false;}}
  return true;
 }
 private static void send(HttpExchange e,int code,String type,byte[] data)throws java.io.IOException{
  e.getResponseHeaders().set("Content-Type",type);e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(code,data.length);
  try(var body=e.getResponseBody()){body.write(data);}
 }
 private static void json(HttpExchange e,int code,String data)throws java.io.IOException{send(e,code,"application/json; charset=utf-8",data.getBytes(StandardCharsets.UTF_8));}
 public static void main(String[] args)throws Exception{
  var app=new NaoLiveServer();var root=Path.of(args.length>0?args[0]:"client/dist").toRealPath();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",8080),0);
  server.createContext("/api/state",e->{if(!e.getRequestMethod().equals("GET")){json(e,405,"{}");return;}json(e,200,app.snapshot());});
  server.createContext("/api/control",e->{if(!e.getRequestMethod().equals("POST")||!"dnms-local".equals(e.getRequestHeaders().getFirst("X-DNMS-Control"))){json(e,403,"{}");return;}var cmd=new String(e.getRequestBody().readNBytes(32),StandardCharsets.UTF_8);json(e,app.command(cmd)?200:400,app.snapshot());});
  server.createContext("/",e->{if(!e.getRequestMethod().equals("GET")){json(e,405,"{}");return;}
   var path=root.resolve("."+e.getRequestURI().getPath()).normalize();if(Files.isDirectory(path))path=path.resolve("index.html");
   if(!path.startsWith(root)||!Files.isRegularFile(path)||!path.toRealPath().startsWith(root)){json(e,404,"{}");return;}
   String name=path.toString();String type=name.endsWith(".js")?"text/javascript":name.endsWith(".html")?"text/html; charset=utf-8":name.endsWith(".json")?"application/json":name.endsWith(".css")?"text/css":"application/octet-stream";
   send(e,200,type,Files.readAllBytes(path));
  });
  var timer=Executors.newSingleThreadScheduledExecutor();timer.scheduleAtFixedRate(app::advance,0,50,TimeUnit.MILLISECONDS);
  server.start();Runtime.getRuntime().addShutdownHook(new Thread(()->{timer.shutdownNow();server.stop(0);}));
  System.out.println("DNMS live: http://127.0.0.1:8080/replay.html?live=1");
 }
}
