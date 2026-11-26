package org.nagineco.world;
import java.util.*;
/** Shared experience is a tie breaker, never evidence that a cell is safe. */
public final class MemoryDecision {
 private final Map<String,Double> scores;
 public MemoryDecision(List<SharedMemorySearch.Event> events){
  var sums=new HashMap<String,Double>();var counts=new HashMap<String,Integer>();
  for(var e:events){
   double value=e.emotion().equals("AFRAID")?-1:0.1;
   sums.merge(e.action(),value,Double::sum);counts.merge(e.action(),1,Integer::sum);
  }
  sums.replaceAll((key,value)->value/(counts.get(key)+5));
  scores=Map.copyOf(sums);
 }
 public double score(String direction){return scores.getOrDefault(direction,0.0);}
}
