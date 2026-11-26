package org.nagineco.world;
import java.util.*;
import org.nagineco.doppel.NaoActionLearning;
import org.nagineco.doppel.NaoActionLearning.Action;
import org.nagineco.doppel.core.*;
import org.nagineco.world.WorldAccess.*;
/** Deterministic flat-world integration fixture; one independent plot per NPC. */
public final class NaoSimulation {
 public enum Goal { COLLECT, BUILD, REST }
 public record State(int slot,Pos position,Goal goal,int collected,int built,long attempts){}
 private static final class Nao {
  final int slot; final Pos anchor; Pos position; int collected,built; long attempts; double travel;
  final ArrayDeque<Pos> route=new ArrayDeque<>();
  Pos routeTarget; long routeRevision=-1; double routeCaution; MemoryDecision routeMemory;
  double health=20; Action lastAction; long lastActionTick=-1;
  final EnumMap<Action,RewardEstimate> rewards=new EnumMap<>(Action.class);
  final NaoActionLearning learning=new NaoActionLearning();
  PhysicalSkillSync.Skill skill=new PhysicalSkillSync.Skill(0,0,0,0,0,0);
  Nao(int slot){this.slot=slot;anchor=new Pos((slot%16)*12+4,1,(slot/16)*12+4);position=anchor;}
 }
 private MemoryDecision memoryDecision=new MemoryDecision(List.of());
 public void setMemoryDecision(MemoryDecision decision){memoryDecision=Objects.requireNonNull(decision);}
 private boolean rewardEnabled=false;
 public boolean rewardEnabled(){return rewardEnabled;}
 public void setRewardEnabled(boolean enabled){rewardEnabled=enabled;}
 public double rewardPreference(int slot,Action action){return naos.get(slot).rewards.getOrDefault(action,new RewardEstimate()).preference();}
 public double health(int slot){return naos.get(slot).health;}
 public void damage(int slot,double amount){
  if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("damage");
  var n=naos.get(slot);double loss=Math.min(n.health,amount);n.health-=loss;
  if(rewardEnabled&&loss>0&&n.lastAction!=null&&ticks()-n.lastActionTick<=20)
   n.rewards.computeIfAbsent(n.lastAction,k->new RewardEstimate()).record(-loss);
 }
 public void heal(int slot,double amount){
  if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("heal");
  var n=naos.get(slot);if(n.health>0)n.health=Math.min(20,n.health+amount);
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
   return true;
 }
 private Pos source(Nao n,int j){return new Pos(n.anchor.x()+4+j/4,1,n.anchor.z()-2+j%4);}
 private Goal goal(Nao n){return n.built==8?Goal.REST:n.collected==8?Goal.BUILD:Goal.COLLECT;}
 private void observe(Nao n,Action action,boolean success){
  n.lastAction=action;n.lastActionTick=ticks();
  if(rewardEnabled&&(action==Action.COLLECT||action==Action.EXPLORE)){
   double reward=action==Action.COLLECT?(success?5:0):(success?1:-1);
   n.rewards.computeIfAbsent(action,k->new RewardEstimate()).record(reward);
  }
  if(n.learning.attempts().getOrDefault(action,0)<100000)n.learning.observe(action,success);
  if(!success)return;
  var k=n.skill;
  n.skill=new PhysicalSkillSync.Skill(Math.min(100,k.technique()+(action==Action.EXPLORE?0:0.2)),Math.min(100,k.coordination()+0.1),Math.min(100,k.proficiency()+(action==Action.EXPLORE?0:0.2)),Math.min(100,k.strength()+(action==Action.COLLECT?0.1:0)),Math.min(100,k.agility()+(action==Action.EXPLORE?0.1:0)),Math.min(100,k.endurance()+0.05));
 }
 public double confidence(int slot,Action action){return naos.get(slot).learning.confidence(action);}
 private void move(Nao n,Pos target){
  n.travel+=adapter.movementStep(n.skill);
  if(n.travel<1)return;n.travel-=1;
  double caution=rewardEnabled?Math.max(0,-rewardPreference(n.slot,Action.EXPLORE))/10:0;
  if(n.route.isEmpty()||!target.equals(n.routeTarget)||n.routeRevision!=world.revision()||n.routeMemory!=memoryDecision||Double.compare(n.routeCaution,caution)!=0){
   n.route.clear();
   n.route.addAll(PathFinder.route(world,n.position,target,(from,to)->{
    String direction=to.x()>from.x()?"EAST":to.x()<from.x()?"WEST":to.z()>from.z()?"SOUTH":"NORTH";
    return (Math.max(0,-memoryDecision.score(direction))+memoryDecision.hazard(to))*(1+caution);
   }));
   n.routeTarget=target;n.routeRevision=world.revision();n.routeMemory=memoryDecision;n.routeCaution=caution;
  }
  var next=n.route.pollFirst();
  if(next!=null)n.position=next;
  observe(n,Action.EXPLORE,next!=null);
 }
 public void tick(){
  for(var n:naos){
   if(n.health<=0)continue;
   if(goal(n)==Goal.COLLECT){
    var staging=new Pos(n.anchor.x()+3,1,n.anchor.z());
    if(!n.position.equals(staging)){move(n,staging);continue;}
    boolean success=adapter.gather(n.slot,n.attempts++,n.skill,n.position,source(n,n.collected));if(success)n.collected++;observe(n,Action.COLLECT,success);
   }else if(goal(n)==Goal.BUILD){
    if(!n.position.equals(n.anchor)){move(n,n.anchor);continue;}
    int previous=n.built;n.built=adapter.build(n.slot,n.attempts++,n.skill,n.position,n.anchor,n.built);observe(n,Action.BUILD,n.built>previous);
   }
  }
  world.advance();
 }
 public List<State> states(){return naos.stream().map(n->new State(n.slot,n.position,goal(n),n.collected,n.built,n.attempts)).toList();}
 public PhysicalSkillSync.Skill skill(int slot){return naos.get(slot).skill;}
 public void setSkill(int slot,PhysicalSkillSync.Skill skill){naos.get(slot).skill=Objects.requireNonNull(skill);}
 public void synchronizeSkills(int source,PhysicalSkillSync.Mode mode){
  var synced=PhysicalSkillSync.broadcast(naos.stream().map(n->n.skill).toList(),source,mode,1.0);
  for(int i=0;i<naos.size();i++)naos.get(i).skill=synced.get(i);
 }
 public long ticks(){return world.tick();}
 public int inventory(int slot){return work.count("nao-"+slot,Block.LOG);}
 public Block block(Pos p){return world.block(p);}
 public void save(java.nio.file.Path path)throws java.io.IOException{
  path=path.toAbsolutePath();java.nio.file.Files.createDirectories(path.getParent());
  var tmp=java.nio.file.Files.createTempFile(path.getParent(),"dnms-",".tmp");
  try{
   try(var out=new java.io.DataOutputStream(new java.io.BufferedOutputStream(java.nio.file.Files.newOutputStream(tmp)))){
    out.writeUTF("DNMS-WORLD-4");out.writeInt(naos.size());out.writeBoolean(rewardEnabled);world.write(out);work.write(out);
    for(var n:naos){out.writeInt(n.position.x());out.writeInt(n.position.y());out.writeInt(n.position.z());out.writeInt(n.collected);out.writeInt(n.built);out.writeLong(n.attempts);out.writeDouble(n.travel);out.writeDouble(n.health);out.writeUTF(n.lastAction==null?"":n.lastAction.name());out.writeLong(n.lastActionTick);
     var k=n.skill;for(double v:new double[]{k.technique(),k.coordination(),k.proficiency(),k.strength(),k.agility(),k.endurance()})out.writeDouble(v);for(var action:Action.values()){out.writeInt(n.learning.attempts().getOrDefault(action,0));out.writeInt(n.learning.successes().getOrDefault(action,0));var reward=n.rewards.getOrDefault(action,new RewardEstimate());out.writeInt(reward.count());out.writeDouble(reward.mean());}}
   }
   java.nio.file.Files.move(tmp,path,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
  }finally{java.nio.file.Files.deleteIfExists(tmp);}
 }
 public static NaoSimulation load(java.nio.file.Path path)throws java.io.IOException{
  try(var in=new java.io.DataInputStream(new java.io.BufferedInputStream(java.nio.file.Files.newInputStream(path)))){
   int version=switch(in.readUTF()){
    case "DNMS-WORLD-1" -> 1;
    case "DNMS-WORLD-2" -> 2;
    case "DNMS-WORLD-3" -> 3;
    case "DNMS-WORLD-4" -> 4;
    default -> throw new java.io.IOException("Unknown checkpoint format");
   };
   int count=in.readInt();if(count<1||count>256)throw new java.io.IOException("Invalid population");
   var sim=new NaoSimulation(count);if(version>=3)sim.rewardEnabled=in.readBoolean();sim.world.read(in);sim.work.read(in);
   for(var n:sim.naos){n.position=new Pos(in.readInt(),in.readInt(),in.readInt());n.collected=in.readInt();n.built=in.readInt();n.attempts=in.readLong();n.travel=in.readDouble();
    if(version>=4){n.health=in.readDouble();String action=in.readUTF();n.lastAction=action.isEmpty()?null:Action.valueOf(action);n.lastActionTick=in.readLong();if(!Double.isFinite(n.health)||n.health<0||n.health>20||n.lastActionTick< -1||n.lastActionTick>sim.ticks()||(n.lastAction==null)!=(n.lastActionTick==-1))throw new java.io.IOException("Invalid health state");}
    if(n.collected<0||n.collected>8||n.built<0||n.built>n.collected||n.attempts<0||!Double.isFinite(n.travel)||n.travel<0||n.travel>=1)throw new java.io.IOException("Invalid NPC state");
    n.skill=new PhysicalSkillSync.Skill(in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble());
    if(version>=2){var a=new EnumMap<Action,Integer>(Action.class);var b=new EnumMap<Action,Integer>(Action.class);for(var action:Action.values()){a.put(action,in.readInt());b.put(action,in.readInt());if(version>=3){var reward=new RewardEstimate();reward.restore(in.readInt(),in.readDouble());n.rewards.put(action,reward);}}n.learning.restore(a,b);}}
   if(in.read()!=-1)throw new java.io.IOException("Trailing checkpoint data");return sim;
  }catch(IllegalArgumentException e){throw new java.io.IOException("Invalid checkpoint",e);}
 }
}
