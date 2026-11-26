package org.nagineco.world;
import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.UUID;
import org.nagineco.doppel.core.DnmsWorldApi;
public final class NaoLiveServer implements AutoCloseable {
 private NaoSimulation sim=new NaoSimulation();
 private boolean running=true;
 private int speed=1;
 private Path savePath;
 private UUID worldId;
 private UUID epoch=UUID.randomUUID();
 private Path epochPath(){return savePath.toAbsolutePath().resolveSibling("timeline-id.txt");}
 private void rotateEpoch()throws java.io.IOException{
  UUID next=UUID.randomUUID();var tmp=Files.createTempFile(epochPath().getParent(),"timeline-",".tmp");
  try{Files.writeString(tmp,next.toString());forceFile(tmp);Files.move(tmp,epochPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);epoch=next;}finally{Files.deleteIfExists(tmp);}
 }
 synchronized String identity(){return "{\"worldId\":\""+worldId+"\",\"epoch\":\""+epoch+"\"}";}

 private java.nio.channels.FileChannel saveLockChannel;
 private java.nio.channels.FileLock saveLock;
 private DnmsMemoryAdapter memory;
 private Path memoryDir;
 private boolean mipEnabled=true;
 private int mipTick=18000;
 private long mipDay=-1;
 private org.nagineco.doppel.core.PhysicalSkillSync.Mode skillMode=org.nagineco.doppel.core.PhysicalSkillSync.Mode.FULL;
 private int skillSource=0;
 private static MemoryDecision decision(DnmsMemoryAdapter memory)throws java.io.IOException{
  var events=new java.util.ArrayList<SharedMemorySearch.Event>();
  for(String direction:java.util.List.of("EAST","WEST","NORTH","SOUTH"))events.addAll(memory.search(direction,50));
  events.addAll(memory.hazardEvents());
  return new MemoryDecision(events);
 }
 private void integrate()throws java.io.IOException{
  if(sim.states().isEmpty())return;
  var dir=memoryDir.resolveSibling(UUID.randomUUID().toString());
  copyMemory(memoryDir,dir);
  var staged=new DnmsMemoryAdapter(dir);
  staged.integrate();
  var nextDecision=decision(staged);
  sim.synchronizeSkills(skillSource,skillMode);
  sim.setMemoryDecision(nextDecision);memory=staged;memoryDir=dir;
 }
 private String memoryError="";
 private boolean spawnSavePending;
 private void newMemoryBranch()throws java.io.IOException{
  if(savePath==null)return;
  var dir=savePath.toAbsolutePath().getParent().resolve("memory-branches").resolve(java.util.UUID.randomUUID().toString());
  var next=new DnmsMemoryAdapter(dir);next.initialize(sim.states().size());memory=next;memoryDir=dir;mipDay=-1;
 }
 private void integrateDue()throws java.io.IOException{
  long tick=sim.ticks(),day=tick/24000;
  if(memory!=null&&org.nagineco.doppel.core.WorldProgressPolicy.integrationDue(mipEnabled,tick,mipTick,mipDay)){integrate();mipDay=day;}
 }

 void configureSave(Path path)throws java.io.IOException{
  savePath=path;
  Files.createDirectories(path.toAbsolutePath().getParent());
  saveLockChannel=java.nio.channels.FileChannel.open(path.toAbsolutePath().resolveSibling("server.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
  try{saveLock=saveLockChannel.tryLock();if(saveLock==null)throw new java.io.IOException("World already open");}
  catch(java.io.IOException|RuntimeException e){saveLockChannel.close();throw e;}
  try{
  var identity=path.toAbsolutePath().resolveSibling("world-id.txt");
  Files.createDirectories(identity.getParent());
  try(var channel=java.nio.channels.FileChannel.open(identity.resolveSibling("world-id.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock()){
   if(!Files.exists(identity)){var backup=identity.resolveSibling("world-id.backup");if(Files.isRegularFile(backup)){UUID.fromString(Files.readString(backup).trim());Files.copy(backup,identity);forceFile(identity);} }
   if(!Files.exists(identity)){if(Files.exists(pointer())||Files.isDirectory(history()))throw new java.io.IOException("Missing world identity; restore world-id.txt from backup");var tmp=Files.createTempFile(identity.getParent(),"identity-",".tmp");try{Files.writeString(tmp,UUID.randomUUID().toString());Files.move(tmp,identity,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(tmp);}}
   worldId=UUID.fromString(Files.readString(identity).trim());
   var backup=identity.resolveSibling("world-id.backup");if(!Files.exists(backup)){Files.copy(identity,backup);forceFile(backup);}else if(!worldId.equals(UUID.fromString(Files.readString(backup).trim())))throw new java.io.IOException("World identity backup mismatch");
  }
  if(Files.exists(epochPath()))epoch=UUID.fromString(Files.readString(epochPath()).trim());else rotateEpoch();
  var pointer=pointer();
  if(Files.exists(pointer)){restore(Files.readString(pointer).trim());}
  else {if(Files.isDirectory(history())){try(var files=Files.list(history())){if(files.anyMatch(x->x.getFileName().toString().endsWith(".bin")))throw new java.io.IOException("Missing generation pointer; explicit recovery required");}}if(Files.exists(path))sim=NaoSimulation.load(path);newMemoryBranch();}
  checkpoint();
  }catch(java.io.IOException|RuntimeException e){try{saveLockChannel.close();}catch(java.io.IOException close){e.addSuppressed(close);}throw e;}
 }
 private Path history(){return savePath.toAbsolutePath().getParent().resolve("history");}
 private Path pointer(){return savePath.toAbsolutePath().resolveSibling("latest-generation.txt");}
 private static void copyMemory(Path from,Path to)throws java.io.IOException{
  Files.createDirectories(to);
  try(var files=Files.list(from)){for(var p:files.toList())if(Files.isRegularFile(p)&&p.getFileName().toString().endsWith(".md"))Files.copy(p,to.resolve(p.getFileName()));}
 }
 private static void forceFile(Path file)throws java.io.IOException{
  try(var channel=java.nio.channels.FileChannel.open(file,StandardOpenOption.WRITE)){channel.force(true);}
 }
 private void publish(String id)throws java.io.IOException{org.nagineco.doppel.core.CheckpointPolicy.publish(pointer(),id);}
 synchronized void checkpoint()throws java.io.IOException{
  if(savePath==null)return;
  long reserve=Long.getLong("dnms.minFreeBytes",268435456L);
  if(reserve<0||Files.getFileStore(savePath.toAbsolutePath().getParent()).getUsableSpace()<reserve)throw new java.io.IOException("Insufficient checkpoint disk reserve");
  String id=org.nagineco.doppel.core.CheckpointPolicy.generation(sim.ticks());
  var dir=history().resolve(id+".memory");
  try{
  Files.createDirectories(dir);
  Files.writeString(dir.resolve("incomplete"),"pending");forceFile(dir.resolve("incomplete"));
  copyMemory(memoryDir,dir);
  Files.writeString(dir.resolve("settings.txt"),mipEnabled+"\n"+mipTick+"\n"+mipDay+"\n"+skillMode.name()+"\n"+skillSource+"\n");
  sim.save(history().resolve(id+".bin"));
  try(var files=Files.list(dir)){for(var file:files.toList())forceFile(file);}
  forceFile(history().resolve(id+".bin"));
  Files.delete(dir.resolve("incomplete"));
  publish(id);
  }catch(java.io.IOException e){
   try{if(!Files.exists(pointer())||!Files.readString(pointer()).trim().equals(id)){
    Files.deleteIfExists(history().resolve(id+".bin"));
    if(Files.isDirectory(dir))try(var files=Files.walk(dir)){for(var file:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}
   }}catch(java.io.IOException cleanup){e.addSuppressed(cleanup);}
   throw e;
  }
  try{sim.save(savePath);}catch(java.io.IOException e){System.err.println("Legacy mirror failed: "+e);}
 }
 private void restore(String id)throws java.io.IOException{
  if(!org.nagineco.doppel.core.CheckpointPolicy.valid(id))throw new java.io.IOException("Invalid generation");
  var restored=NaoSimulation.load(history().resolve(id+".bin"));
  var source=history().resolve(id+".memory");
  if(Files.exists(source.resolve("incomplete")))throw new java.io.IOException("Incomplete checkpoint");
  var settings=Files.readAllLines(source.resolve("settings.txt"));
  if((settings.size()!=3&&settings.size()!=5)||!settings.get(0).matches("true|false"))throw new java.io.IOException("Invalid MIP settings");
  int tick;long day;
  try{tick=Integer.parseInt(settings.get(1));day=Long.parseLong(settings.get(2));}catch(NumberFormatException e){throw new java.io.IOException(e);}
  if(tick<0||tick>=24000||day< -1||day>restored.ticks()/24000)throw new java.io.IOException("Invalid MIP clock");
  var mode=org.nagineco.doppel.core.PhysicalSkillSync.Mode.FULL;int sourceSlot=0;
  if(settings.size()==5)try{mode=org.nagineco.doppel.core.PhysicalSkillSync.Mode.parse(settings.get(3));sourceSlot=Integer.parseInt(settings.get(4));}catch(IllegalArgumentException e){throw new java.io.IOException(e);}
  if(sourceSlot<0||(restored.states().isEmpty()?sourceSlot!=0:sourceSlot>=restored.states().size()))throw new java.io.IOException("Invalid skill source");
  var dir=savePath.toAbsolutePath().getParent().resolve("memory-branches").resolve(java.util.UUID.randomUUID().toString());
  for(int i=1;i<=restored.states().size();i++)if(!Files.isRegularFile(source.resolve(String.format(java.util.Locale.ROOT,"nao_%02d.md",i))))throw new java.io.IOException("Missing journal");
  if(!Files.isRegularFile(source.resolve("nao_personality.md")))throw new java.io.IOException("Missing personality");
  copyMemory(source,dir);
  var restoredMemory=new DnmsMemoryAdapter(dir);restored.setMemoryDecision(decision(restoredMemory));
  publish(id);
  try{restored.save(savePath);}catch(java.io.IOException e){System.err.println("Legacy mirror failed: "+e);}
  sim=restored;memoryDir=dir;memory=restoredMemory;mipEnabled=Boolean.parseBoolean(settings.get(0));mipTick=tick;mipDay=day;skillMode=mode;skillSource=sourceSlot;memoryError="";
 }
 synchronized boolean rewind(String id){
  if(savePath==null)return false;
  try{rotateEpoch();restore(id);running=false;return true;}catch(java.io.IOException e){System.err.println(e);return false;}
 }
 synchronized String checkpoints(){
  if(savePath==null||!Files.isDirectory(history()))return "[]";
  try(var files=Files.list(history())){
   return files.map(p->p.getFileName().toString()).filter(n->Files.isRegularFile(history().resolve(n.replace(".bin",".memory")+"/settings.txt"))).filter(n->n.endsWith(".bin")&&org.nagineco.doppel.core.CheckpointPolicy.valid(n.substring(0,n.length()-4))).filter(n->!Files.exists(history().resolve(n.replace(".bin",".memory")).resolve("incomplete"))).map(n->n.substring(0,n.length()-4)).sorted().map(n->"\""+n+"\"").collect(java.util.stream.Collectors.joining(",","[","]"));
  }catch(java.io.IOException e){return "[]";}
 }

 @Override public synchronized void close() throws java.io.IOException {
  if(saveLockChannel!=null){saveLockChannel.close();saveLockChannel=null;saveLock=null;}
 }
 synchronized void save(){if(savePath!=null)try{checkpoint();}catch(java.io.IOException e){System.err.println("Checkpoint failed: "+e);}}

 synchronized void advance(){if(running)for(int i=0;i<speed&&running;i++){var before=memory==null?null:sim.states();sim.tick();
 if(memory!=null)try{var after=sim.states();for(int j=0;j<after.size();j++)memory.observe(sim.ticks(),before.get(j),after.get(j));integrateDue();memoryError="";}catch(java.io.IOException e){memoryError=e.toString();running=false;System.err.println(e);}
 if(org.nagineco.doppel.core.CheckpointPolicy.due(sim.ticks()))try{persistSpawn();}catch(java.io.IOException e){System.err.println(e);}}}
 synchronized String searchMemory(String query)throws java.io.IOException{
  if(query.length()>256)throw new IllegalArgumentException("query too long");
  var out=new StringBuilder("[");
  if(memory!=null)for(var event:memory.search(query,50)){
   if(out.length()>1)out.append(',');
   out.append("{\"nao\":").append(event.nao()).append(",\"day\":").append(event.day()).append(",\"tick\":").append(event.tick())
    .append(",\"action\":\"").append(event.action()).append("\",\"emotion\":\"").append(event.emotion()).append("\",\"reason\":\"").append(event.reason()).append("\"}");
  }
  return out.append(']').toString();
 }
 private static boolean managedMode(){String token=System.getenv("DNMS_WORLD_TOKEN");return token!=null&&token.length()>=32;}
 synchronized String apiNpcs(){return org.nagineco.doppel.core.DnmsWorldHttpApi.snapshot(sim);}
 synchronized DnmsWorldApi.SpawnResult apiSpawn(UUID id,DnmsWorldApi.Position pos)throws java.io.IOException{
  var list=sim.npcs();
  if(list.stream().anyMatch(n->n.id().equals(id))){
   if(spawnSavePending)persistSpawn();
   return DnmsWorldApi.SpawnResult.ALREADY_EXISTS;
  }
  if(spawnSavePending)throw new java.io.IOException("Pending spawn persistence must be retried");
  int slot=list.size();
  if(slot>=256)return DnmsWorldApi.SpawnResult.CAPACITY;
  if(!pos.equals(org.nagineco.doppel.core.WorldIdentityPolicy.anchor(slot)))return DnmsWorldApi.SpawnResult.INVALID_POSITION;
  try{if(memory!=null)memory.initialize(slot+1);}catch(java.io.IOException e){running=false;memoryError=e.toString();throw e;}
  var result=sim.spawnNpc(id,pos);
  if(result==DnmsWorldApi.SpawnResult.CREATED)persistSpawn();
  return result;
 }
 private void persistSpawn()throws java.io.IOException{
  try{checkpoint();spawnSavePending=false;memoryError="";}
  catch(java.io.IOException e){spawnSavePending=true;running=false;memoryError=e.toString();throw e;}
 }
 void registerWorldApi(HttpServer server,String token){org.nagineco.doppel.core.DnmsWorldHttpApi.register(server,token,this::apiNpcs,this::apiSpawn,worldId,this,()->epoch);}
 synchronized String snapshot(){
  var out=new StringBuilder("{\"tick\":").append(sim.ticks()).append(",\"running\":").append(running).append(",\"speed\":").append(speed).append(",\"rewardEnabled\":").append(sim.rewardEnabled()).append(",\"mipEnabled\":").append(mipEnabled).append(",\"mipTick\":").append(mipTick).append(",\"mipDay\":").append(mipDay).append(",\"skillMode\":\"").append(skillMode).append("\",\"skillSource\":").append(skillSource).append(",\"memoryError\":").append(!memoryError.isEmpty()).append(",\"naos\":[");
  boolean first=true;
  for(var n:sim.states()){
   if(!first)out.append(',');first=false;var p=n.position();
   out.append('[').append(n.slot()).append(',').append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(",\"").append(n.goal()).append("\",").append(n.collected()).append(',').append(n.built()).append(',').append(sim.health(n.slot())).append(',').append(sim.satiety(n.slot())).append(',').append(sim.food(n.slot())).append(',').append(sim.fatigue(n.slot())).append(',').append(sim.sleeping(n.slot())).append(']');
  }
  return out.append("]}").toString();
 }
 synchronized boolean damage(int slot,double amount)throws java.io.IOException{
  if(slot<0||slot>=sim.states().size()||!Double.isFinite(amount)||amount<=0||sim.health(slot)<=0)return false;
  if(memory!=null)memory.recordDamage(sim.ticks(),sim.states().get(slot));
  sim.damage(slot,amount);return true;
 }
 synchronized boolean command(String cmd){
  if(spawnSavePending&&!cmd.equals("checkpoint")&&!cmd.equals("pause"))return false;
  if(cmd.startsWith("damage:")){
   String[] parts=cmd.split(":",-1);if(parts.length!=3)return false;
   try{return damage(Integer.parseInt(parts[1]),Double.parseDouble(parts[2]));}catch(IllegalArgumentException|java.io.IOException e){return false;}
  }
  if(cmd.startsWith("rewind:"))return rewind(cmd.substring(7));
  if(cmd.equals("checkpoint")){try{persistSpawn();return true;}catch(java.io.IOException e){return false;}}
  if(cmd.equals("mip:on")){mipEnabled=true;return true;}
  if(cmd.equals("mip:off")){mipEnabled=false;return true;}
  if(cmd.equals("mip:run")){if(memory==null||!mipEnabled||mipDay==sim.ticks()/24000)return false;try{integrate();mipDay=sim.ticks()/24000;return true;}catch(java.io.IOException e){return false;}}
  if(cmd.matches("mip:time:[0-9]{2}:[0-9]{2}")){try{mipTick=DnmsMemoryAdapter.clockTick(Integer.parseInt(cmd.substring(9,11)),Integer.parseInt(cmd.substring(12,14)));return true;}catch(IllegalArgumentException e){return false;}}
  if(cmd.startsWith("skill:mode:")){try{skillMode=org.nagineco.doppel.core.PhysicalSkillSync.Mode.parse(cmd.substring(11));return true;}catch(IllegalArgumentException e){return false;}}
  if(cmd.startsWith("skill:source:")){try{int slot=Integer.parseInt(cmd.substring(13));if(slot<0||slot>=sim.states().size())return false;skillSource=slot;return true;}catch(NumberFormatException e){return false;}}
  if(cmd.equals("reward:on")||cmd.equals("reward:off")){sim.setRewardEnabled(cmd.equals("reward:on"));return true;}
  switch(cmd){case "pause"->running=false;case "resume"->running=true;case "reset"->{if(managedMode())return false;try{if(savePath!=null)rotateEpoch();}catch(java.io.IOException e){return false;}sim=new NaoSimulation();skillSource=0;try{newMemoryBranch();}catch(java.io.IOException e){memory=null;running=false;return false;}}case "next"->{if(managedMode())return false; /* DNMS-LEGACY: local generation disabled in managed mode. */ if(sim.states().size()>=256)return false;try{if(memory!=null)memory.initialize(sim.states().size()+1);}catch(java.io.IOException e){return false;}return sim.addNao();}case "1","2","4"->speed=Integer.parseInt(cmd);default->{return false;}}
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
  app.registerWorldApi(server,System.getenv("DNMS_WORLD_TOKEN"));
  String token=System.getenv("DNMS_WORLD_TOKEN");
  if(token!=null&&token.length()>=32)server.createContext("/api/v1/world",e->{
   if(!e.getRequestURI().getPath().equals("/api/v1/world")){json(e,404,"{}");return;}
   String auth=e.getRequestHeaders().getFirst("Authorization");
   if(auth==null||!java.security.MessageDigest.isEqual(auth.getBytes(StandardCharsets.UTF_8),("Bearer "+token).getBytes(StandardCharsets.UTF_8))){json(e,401,"{}");return;}
   if(!e.getRequestMethod().equals("GET")){json(e,405,"{}");return;}
   json(e,200,app.identity());
  });
  server.createContext("/api/state",e->{if(!e.getRequestMethod().equals("GET")){json(e,405,"{}");return;}json(e,200,app.snapshot());});
  server.createContext("/api/memory/search",e->{
   if(!e.getRequestMethod().equals("GET")){json(e,405,"{}");return;}
   try{
    String raw=e.getRequestURI().getRawQuery();
    if(raw==null||!raw.startsWith("q=")||raw.length()>4096){json(e,400,"{}");return;}
    json(e,200,app.searchMemory(URLDecoder.decode(raw.substring(2),StandardCharsets.UTF_8)));
   }catch(IllegalArgumentException ex){json(e,400,"{}");}catch(java.io.IOException ex){json(e,500,"{}");}
  });
  server.createContext("/api/checkpoints",e->{if(!e.getRequestMethod().equals("GET")){json(e,405,"{}");return;}json(e,200,app.checkpoints());});
  server.createContext("/api/control",e->{if(!e.getRequestMethod().equals("POST")||!"dnms-local".equals(e.getRequestHeaders().getFirst("X-DNMS-Control"))){json(e,403,"{}");return;}var cmd=new String(e.getRequestBody().readNBytes(128),StandardCharsets.UTF_8);json(e,app.command(cmd)?200:400,app.snapshot());});
  server.createContext("/",e->{if(!e.getRequestMethod().equals("GET")){json(e,405,"{}");return;}
   var path=root.resolve("."+e.getRequestURI().getPath()).normalize();if(Files.isDirectory(path))path=path.resolve("index.html");
   if(!path.startsWith(root)||!Files.isRegularFile(path)||!path.toRealPath().startsWith(root)){json(e,404,"{}");return;}
   String name=path.toString();String type=name.endsWith(".js")?"text/javascript":name.endsWith(".html")?"text/html; charset=utf-8":name.endsWith(".json")?"application/json":name.endsWith(".css")?"text/css":"application/octet-stream";
   send(e,200,type,Files.readAllBytes(path));
  });
  var timer=Executors.newSingleThreadScheduledExecutor();timer.scheduleAtFixedRate(app::advance,0,50,TimeUnit.MILLISECONDS);
  server.start();Runtime.getRuntime().addShutdownHook(new Thread(()->{timer.shutdownNow();server.stop(0);app.save();try{app.close();}catch(java.io.IOException e){System.err.println(e);}}));
  System.out.println("DNMS live: http://127.0.0.1:8080/replay.html?live=1");
 }
}
