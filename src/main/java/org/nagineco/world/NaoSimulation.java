package org.nagineco.world;
import java.util.*;
import org.nagineco.doppel.core.*;
import org.nagineco.world.WorldAccess.*;
/** Deterministic flat-world integration fixture; one independent plot per NPC. */
public final class NaoSimulation {
 public enum Goal { COLLECT, BUILD, REST }
 public record State(int slot,Pos position,Goal goal,int collected,int built,long attempts){}
 private static final class Nao {
  final int slot; final Pos anchor; Pos position; int collected,built; long attempts; double travel;
  final PhysicalSkillSync.Skill skill=new PhysicalSkillSync.Skill(0,0,0,0,0,0);
  Nao(int slot){this.slot=slot;anchor=new Pos((slot%16)*12+4,1,(slot/16)*12+4);position=anchor;}
 }
 private final MemoryWorld world=new MemoryWorld();
 private final WorkSession work=new WorkSession(world);
 private final DnmsSkillAdapter adapter=new DnmsSkillAdapter(work);
 private final List<Nao> naos=new ArrayList<>();
 public NaoSimulation(){this(24);}
 public NaoSimulation(int count){
  if(count<1||count>256)throw new IllegalArgumentException("count");
  for(int i=0;i<count;i++)addNao();
 }
 public boolean addNao(){
   if(naos.size()>=256)return false;
   var n=new Nao(naos.size());naos.add(n);
   for(int x=-2;x<=5;x++)for(int z=-2;z<=2;z++)world.compareAndSet(new Pos(n.anchor.x()+x,0,n.anchor.z()+z),Block.AIR,Block.DIRT);
   for(int j=0;j<8;j++)world.compareAndSet(source(n,j),Block.AIR,Block.LOG);
   growth.add(new Growth(world.tick(),naos.size()));
   return true;
 }
 private Pos source(Nao n,int j){return new Pos(n.anchor.x()+4+j/4,1,n.anchor.z()-2+j%4);}
 private Goal goal(Nao n){return n.built==8?Goal.REST:n.collected==8?Goal.BUILD:Goal.COLLECT;}
 private void move(Nao n,Pos target){
  n.travel+=adapter.movementStep(n.skill);
  if(n.travel<1)return;n.travel-=1;
  int dx=Integer.compare(target.x(),n.position.x()),dz=Integer.compare(target.z(),n.position.z());
  var next=new Pos(n.position.x()+dx,1,n.position.z());
  if(dx==0)next=new Pos(n.position.x(),1,n.position.z()+dz);
  if(world.block(next)==Block.AIR&&world.block(new Pos(next.x(),0,next.z()))!=Block.AIR)n.position=next;
 }
 public void tick(){
  for(var n:naos){
   if(goal(n)==Goal.COLLECT){
    var staging=new Pos(n.anchor.x()+3,1,n.anchor.z());
    if(!n.position.equals(staging)){move(n,staging);continue;}
    if(adapter.gather(n.slot,n.attempts++,n.skill,n.position,source(n,n.collected)))n.collected++;
   }else if(goal(n)==Goal.BUILD){
    if(!n.position.equals(n.anchor)){move(n,n.anchor);continue;}
    n.built=adapter.build(n.slot,n.attempts++,n.skill,n.position,n.anchor,n.built);
   }
  }
  world.advance();
 }
 public List<State> states(){return naos.stream().map(n->new State(n.slot,n.position,goal(n),n.collected,n.built,n.attempts)).toList();}
 // Version 1 stores deterministic fixture progress; replay reconstructs world and inventories.
 public record Growth(long tick,int count){}
 private final List<Growth> growth=new ArrayList<>();
 public void save(java.nio.file.Path path)throws java.io.IOException{
  var target=path.toAbsolutePath();java.nio.file.Files.createDirectories(target.getParent());
  var temp=java.nio.file.Files.createTempFile(target.getParent(),"dnms-",".tmp");
  try{
   try(var out=new java.io.DataOutputStream(java.nio.file.Files.newOutputStream(temp))){
    out.writeInt(0x444e4d53);out.writeInt(1);out.writeLong(ticks());out.writeInt(growth.size());
    for(var g:growth){out.writeLong(g.tick());out.writeInt(g.count());}
   }
   java.nio.file.Files.move(temp,target,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
  }finally{java.nio.file.Files.deleteIfExists(temp);}
 }
 public static NaoSimulation load(java.nio.file.Path path)throws java.io.IOException{
  try(var in=new java.io.DataInputStream(java.nio.file.Files.newInputStream(path))){
   if(in.readInt()!=0x444e4d53||in.readInt()!=1)throw new java.io.IOException("Invalid checkpoint version");
   long tick=in.readLong();int count=in.readInt();
   if(tick<0||tick>100000000L||count<1||count>256)throw new java.io.IOException("Invalid checkpoint bounds");
   var events=new ArrayList<Growth>();long previous=0;
   for(int i=0;i<count;i++){long t=in.readLong();int c=in.readInt();if(t<previous||t>tick||c!=i+1||(i==0&&t!=0))throw new java.io.IOException("Invalid growth event");events.add(new Growth(t,c));previous=t;}
   if(in.read()!=-1)throw new java.io.IOException("Trailing checkpoint data");
   var sim=new NaoSimulation(1);
   for(int i=1;i<count;i++){var g=events.get(i);while(sim.ticks()<g.tick())sim.tick();sim.addNao();}
   while(sim.ticks()<tick)sim.tick();
   return sim;
  }
 }
 public long ticks(){return world.tick();}
 public int inventory(int slot){return work.count("nao-"+slot,Block.LOG);}
 public Block block(Pos p){return world.block(p);}
}
