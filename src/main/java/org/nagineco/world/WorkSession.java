package org.nagineco.world;
import java.util.*;
import org.nagineco.world.WorldAccess.*;
public final class WorkSession {
 private final WorldAccess world;
 private final Map<String,EnumMap<Block,Integer>> inventories=new HashMap<>();
 private final Set<Pos> protectedBlocks=new HashSet<>();
 public WorkSession(WorldAccess world){this.world=Objects.requireNonNull(world);}
 private EnumMap<Block,Integer> inventory(String actor){return inventories.computeIfAbsent(Objects.requireNonNull(actor),k->new EnumMap<>(Block.class));}
 public synchronized int count(String actor,Block block){return inventory(actor).getOrDefault(block,0);}
 private boolean reachable(Pos from,Pos to){return Math.abs((long)from.x()-to.x())+Math.abs((long)from.y()-to.y())+Math.abs((long)from.z()-to.z())<=4;}
 public synchronized boolean gather(String actor,Pos from,Pos target){
  var inv=inventory(actor); if(!reachable(from,target)||protectedBlocks.contains(target))return false;
  Block b=world.block(target); if(b==Block.AIR||!world.compareAndSet(target,b,Block.AIR))return false;
  inv.merge(b,1,Integer::sum);return true;
 }
 public synchronized boolean place(String actor,Pos from,Pos target,Block b){
  var inv=inventory(actor);if(b==null||b==Block.AIR||!reachable(from,target)||from.equals(target)||count(actor,b)<=0)return false;
  if(target.y()==Integer.MIN_VALUE||world.block(new Pos(target.x(),target.y()-1,target.z()))==Block.AIR)return false;
  if(!world.compareAndSet(target,Block.AIR,b))return false;
  inv.put(b,count(actor,b)-1);protectedBlocks.add(target);return true;
 }
}
