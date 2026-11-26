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
 private Path savePath;
 private DnmsMemoryAdapter memory;
 private boolean mipEnabled=true;
 private int mipTick=18000;
 private long mipDay=-1;
 private String memoryError="";
 private void newMemoryBranch()throws java.io.IOException{
  if(savePath==null)return;
  var dir=savePath.toAbsolutePath().getParent().resolve("memory-branches").resolve(java.util.UUID.randomUUID().toString());
  var next=new DnmsMemoryAdapter(dir);next.initialize(sim.states().size());memory=next;mipDay=-1;
 }
 private void integrateDue()throws java.io.IOException{
  long tick=sim.ticks(),day=tick/24000;
  if(memory!=null&&mipEnabled&&tick%24000>=mipTick&&mipDay!=day){memory.integrate();mipDay=day;}
 }

 void configureSave(Path path)throws java.io.IOException{
  savePath=path;if(Files.exists(path))sim=NaoSimulation.load(path);newMemoryBranch();checkpoint();
 }
 private Path history(){return savePath.toAbsolutePath().getParent().resolve("history");}
 synchronized void checkpoint()throws java.io.IOException{
  if(savePath==null)return;
  sim.save(history().resolve(sim.ticks()+"-"+java.util.UUID.randomUUID()+".bin"));sim.save(savePath);
 }
 synchronized boolean rewind(String id){
  if(savePath==null||!id.matches("[0-9]+-[a-f0-9-]{36}"))return false;
  try{var restored=NaoSimulation.load(history().resolve(id+".bin"));restored.save(savePath);sim=restored;newMemoryBranch();running=false;return true;}
  catch(java.io.IOException e){System.err.println(e);return false;}
 }
 synchronized String checkpoints(){
  if(savePath==null||!Files.isDirectory(history()))return "[]";
  try(var files=Files.list(history())){
   return files.map(p->p.getFileName().toString()).filter(n->n.matches("[0-9]+-[a-f0-9-]{36}\\.bin")).map(n->n.substring(0,n.length()-4)).sorted().map(n->"\""+n+"\"").collect(java.util.stream.Collectors.joining(",","[","]"));
  }catch(java.io.IOException e){return "[]";}
 }

 synchronized void save(){if(savePath!=null)try{sim.save(savePath);}catch(java.io.IOException e){System.err.println("Checkpoint failed: "+e);}}

 synchronized void advance(){if(running)for(int i=0;i<speed;i++){var before=memory==null?null:sim.states();sim.tick();
 if(memory!=null)try{var after=sim.states();for(int j=0;j<after.size();j++)memory.observe(sim.ticks(),before.get(j),after.get(j));integrateDue();memoryError="";}catch(java.io.IOException e){memoryError=e.toString();running=false;System.err.println(e);}
 if(sim.ticks()%1200==0)try{checkpoint();}catch(java.io.IOException e){System.err.println(e);}}}
 synchronized String snapshot(){
  var out=new StringBuilder("{\"tick\":").append(sim.ticks()).append(",\"running\":").append(running).append(",\"speed\":").append(speed).append(",\"mipEnabled\":").append(mipEnabled).append(",\"mipTick\":").append(mipTick).append(",\"mipDay\":").append(mipDay).append(",\"memoryError\":").append(!memoryError.isEmpty()).append(",\"naos\":[");
  boolean first=true;
  for(var n:sim.states()){
   if(!first)out.append(',');first=false;var p=n.position();
   out.append('[').append(n.slot()).append(',').append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(",\"").append(n.goal()).append("\",").append(n.collected()).append(',').append(n.built()).append(']');
  }
  return out.append("]}").toString();
 }
 synchronized boolean command(String cmd){
  if(cmd.startsWith("rewind:"))return rewind(cmd.substring(7));
  if(cmd.equals("checkpoint")){try{checkpoint();return true;}catch(java.io.IOException e){return false;}}
  if(cmd.equals("mip:on")){mipEnabled=true;return true;}
  if(cmd.equals("mip:off")){mipEnabled=false;return true;}
  if(cmd.equals("mip:run")){if(memory==null||!mipEnabled)return false;try{memory.integrate();return true;}catch(java.io.IOException e){return false;}}
  if(cmd.matches("mip:time:[0-9]{2}:[0-9]{2}")){try{mipTick=DnmsMemoryAdapter.clockTick(Integer.parseInt(cmd.substring(9,11)),Integer.parseInt(cmd.substring(12,14)));return true;}catch(IllegalArgumentException e){return false;}}
  switch(cmd){case "pause"->running=false;case "resume"->running=true;case "reset"->{sim=new NaoSimulation();try{newMemoryBranch();}catch(java.io.IOException e){memory=null;running=false;return false;}}case "next"->{if(sim.states().size()>=256)return false;try{if(memory!=null)memory.initialize(sim.states().size()+1);}catch(java.io.IOException e){return false;}return sim.addNao();}case "1","2","4"->speed=Integer.parseInt(cmd);default->{return false;}}
  return true;
 }
 private static void send(HttpExchange e,int code,String type,byte[] data)throws java.io.IOException{
  e.getResponseHeaders().set("Content-Type",type);e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(code,data.length);
  try(var body=e.getResponseBody()){body.write(data);}
 }
 private static void json(HttpExchange e,int code,String data)throws java.io.IOException{send(e,code,"application/json; charset=utf-8",data.getBytes(StandardCharsets.UTF_8));}
 public static void main(String[] args)throws Exception{
  var app=new NaoLiveServer();var root=Path.of(args.length>0?args[0]:"client/dist").toRealPath();
  app.configureSave(Path.of("world-data/latest.bin"));
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",8080),0);
  server.createContext("/api/state",e->{if(!e.getRequestMethod().equals("GET")){json(e,405,"{}");return;}json(e,200,app.snapshot());});
  server.createContext("/api/checkpoints",e->{if(!e.getRequestMethod().equals("GET")){json(e,405,"{}");return;}json(e,200,app.checkpoints());});
  server.createContext("/api/control",e->{if(!e.getRequestMethod().equals("POST")||!"dnms-local".equals(e.getRequestHeaders().getFirst("X-DNMS-Control"))){json(e,403,"{}");return;}var cmd=new String(e.getRequestBody().readNBytes(128),StandardCharsets.UTF_8);json(e,app.command(cmd)?200:400,app.snapshot());});
  server.createContext("/",e->{if(!e.getRequestMethod().equals("GET")){json(e,405,"{}");return;}
   var path=root.resolve("."+e.getRequestURI().getPath()).normalize();if(Files.isDirectory(path))path=path.resolve("index.html");
   if(!path.startsWith(root)||!Files.isRegularFile(path)||!path.toRealPath().startsWith(root)){json(e,404,"{}");return;}
   String name=path.toString();String type=name.endsWith(".js")?"text/javascript":name.endsWith(".html")?"text/html; charset=utf-8":name.endsWith(".json")?"application/json":name.endsWith(".css")?"text/css":"application/octet-stream";
   send(e,200,type,Files.readAllBytes(path));
  });
  var timer=Executors.newSingleThreadScheduledExecutor();timer.scheduleAtFixedRate(app::advance,0,50,TimeUnit.MILLISECONDS);
  server.start();Runtime.getRuntime().addShutdownHook(new Thread(()->{timer.shutdownNow();server.stop(0);app.save();}));
  System.out.println("DNMS live: http://127.0.0.1:8080/replay.html?live=1");
 }
}
