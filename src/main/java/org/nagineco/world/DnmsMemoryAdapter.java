package org.nagineco.world;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import org.nagineco.patch.memoryintegration.MemoryIntegrationPatch;
public final class DnmsMemoryAdapter {
 private final Path dir;
 private SharedMemorySearch search;
 public DnmsMemoryAdapter(Path dir){this.dir=dir;}
 public void initialize(int count)throws IOException{org.nagineco.doppel.core.MemoryJournal.initializeWorld(dir,count,5);}
 public void observe(long tick,NaoSimulation.State before,NaoSimulation.State after)throws IOException{
  if(before.slot()!=after.slot())throw new IllegalArgumentException("slot");
  if(before.position().equals(after.position())&&before.goal()==after.goal())return;
  var p=after.position();var q=before.position();
  String action=p.x()>q.x()?"EAST":p.x()<q.x()?"WEST":p.z()>q.z()?"SOUTH":p.z()<q.z()?"NORTH":"STAY";
  String reason=switch(before.goal()){case REST,EAT,SLEEP -> "NONE";case FORAGE -> "COLLECT";default -> before.goal().name();};
  org.nagineco.doppel.core.MemoryJournal.append(dir,after.slot(),tick,p.x(),p.y(),p.z(),action,"NEUTRAL",reason);
 }
 public void recordDamage(long tick,NaoSimulation.State state)throws IOException{
  var p=state.position();
  org.nagineco.doppel.core.MemoryJournal.append(dir,state.slot(),tick,p.x(),p.y(),p.z(),"STAY","AFRAID","SAFETY");
 }
 public String integrate()throws IOException{String result=MemoryIntegrationPatch.integrate(dir);search=null;return result;}
 public List<SharedMemorySearch.Event> search(String query,int limit)throws IOException{if(search==null)search=new SharedMemorySearch(dir.resolve("nao_integrated_memory.md"));return search.search(query,limit);}
 public List<SharedMemorySearch.Event> hazardEvents()throws IOException{
  if(search==null)search=new SharedMemorySearch(dir.resolve("nao_integrated_memory.md"));
  return search.hazardEvents();
 }
 public static int clockTick(int hour,int minute){
  return org.nagineco.doppel.core.WorldLifePolicy.clockTick(hour,minute);
 }
}
