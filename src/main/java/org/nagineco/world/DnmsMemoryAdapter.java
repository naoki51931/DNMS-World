package org.nagineco.world;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import org.nagineco.patch.memoryintegration.MemoryIntegrationPatch;
public final class DnmsMemoryAdapter {
 private final Path dir;
 private SharedMemorySearch search;
 public DnmsMemoryAdapter(Path dir){this.dir=dir;}
 public void initialize(int count)throws IOException{
  if(count<1||count>256)throw new IllegalArgumentException("count");
  Files.createDirectories(dir);
  var personality=dir.resolve("nao_personality.md");
  if(!Files.exists(personality))Files.writeString(personality,"# Nao\n"+String.join("\n",List.of("- preference.none: 5","- preference.explore: 5","- preference.build: 5","- preference.collect: 5","- preference.safety: 5","- preference.social: 5"))+"\n",StandardOpenOption.CREATE_NEW);
  for(int i=0;i<count;i++){var p=journal(i);if(!Files.exists(p))Files.writeString(p,"# Nao "+(i+1)+"\n",StandardOpenOption.CREATE_NEW);}
 }
 private Path journal(int slot){return dir.resolve(String.format(Locale.ROOT,"nao_%02d.md",slot+1));}
 public void observe(long tick,NaoSimulation.State before,NaoSimulation.State after)throws IOException{
  if(before.slot()!=after.slot())throw new IllegalArgumentException("slot");
  if(before.position().equals(after.position())&&before.goal()==after.goal())return;
  var p=after.position();var q=before.position();
  String action=p.x()>q.x()?"EAST":p.x()<q.x()?"WEST":p.z()>q.z()?"SOUTH":p.z()<q.z()?"NORTH":"STAY";
  String reason=switch(before.goal()){case REST,EAT,SLEEP -> "NONE";case FORAGE -> "COLLECT";default -> before.goal().name();};
  String line=String.format(Locale.ROOT,"- day=%d tick=%d position=(%d,%d,%d) action=%s emotion=NEUTRAL reason=%s%n",tick/24000+1,tick%24000,p.x(),p.y(),p.z(),action,reason);
  Files.writeString(journal(after.slot()),line,StandardOpenOption.APPEND);
 }
 public void recordDamage(long tick,NaoSimulation.State state)throws IOException{
  var p=state.position();
  Files.writeString(journal(state.slot()),String.format(Locale.ROOT,"- day=%d tick=%d position=(%d,%d,%d) action=STAY emotion=AFRAID reason=SAFETY%n",tick/24000+1,tick%24000,p.x(),p.y(),p.z()),StandardOpenOption.APPEND);
 }
 public String integrate()throws IOException{String result=MemoryIntegrationPatch.integrate(dir);search=null;return result;}
 public List<SharedMemorySearch.Event> search(String query,int limit)throws IOException{if(search==null)search=new SharedMemorySearch(dir.resolve("nao_integrated_memory.md"));return search.search(query,limit);}
 public List<SharedMemorySearch.Event> hazardEvents()throws IOException{
  if(search==null)search=new SharedMemorySearch(dir.resolve("nao_integrated_memory.md"));
  return search.hazardEvents();
 }
 public static int clockTick(int hour,int minute){
  if(hour<0||hour>23||minute<0||minute>59)throw new IllegalArgumentException("clock");
  return Math.floorMod((hour*60+minute-360)*24000/1440,24000);
 }
}
