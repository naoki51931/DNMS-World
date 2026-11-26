package org.nagineco.world;
// DNMS-LEGACY: original implementation disabled; authoritative logic is in org.nagineco.doppel.core.MemoryDecision.
// package org.nagineco.world;
// import java.util.*;
// /** Shared experience is a tie breaker, never evidence that a cell is safe. */
// public final class MemoryDecision {
//  private final Map<String,Double> scores;
//  private final Set<org.nagineco.world.WorldAccess.Pos> hazards;
//  public MemoryDecision(List<SharedMemorySearch.Event> events){
//   var sums=new HashMap<String,Double>();var counts=new HashMap<String,Integer>();
//   var danger=new HashSet<org.nagineco.world.WorldAccess.Pos>();
//   for(var e:events){
//    if(e.action().equals("STAY")&&e.emotion().equals("AFRAID")&&e.reason().equals("SAFETY")){
//     String[] xyz=e.position().split(",");
//     if(xyz.length==3)try{danger.add(new org.nagineco.world.WorldAccess.Pos(Integer.parseInt(xyz[0].trim()),Integer.parseInt(xyz[1].trim()),Integer.parseInt(xyz[2].trim())));}catch(NumberFormatException ignored){}
//    }
//    double value=e.emotion().equals("AFRAID")?-1:0.1;
//    sums.merge(e.action(),value,Double::sum);counts.merge(e.action(),1,Integer::sum);
//   }
//   sums.replaceAll((key,value)->value/(counts.get(key)+5));
//   scores=Map.copyOf(sums);hazards=Set.copyOf(danger);
//  }
//  public double hazard(org.nagineco.world.WorldAccess.Pos p){return hazards.contains(p)?3:0;}
//  public double score(String direction){return scores.getOrDefault(direction,0.0);}
// }
// 
// Compatibility adapter only.
public final class MemoryDecision {
 private final org.nagineco.doppel.core.MemoryDecision delegate;
 public MemoryDecision(java.util.List<SharedMemorySearch.Event> events){delegate=new org.nagineco.doppel.core.MemoryDecision(events.stream().map(SharedMemorySearch.Event::core).toList());}
 public double hazard(WorldAccess.Pos p){return delegate.hazard(new org.nagineco.doppel.core.DnmsWorldApi.Position(p.x(),p.y(),p.z()));}
 public double score(String direction){return delegate.score(direction);}
}
