package org.nagineco.world;
import java.util.*;
import java.util.function.ToDoubleBiFunction;
import org.nagineco.world.WorldAccess.*;
public final class PathFinder {
 private record Node(Pos pos,double cost,double estimate,long order){}
 private static long distance(Pos a,Pos b){return Math.abs((long)a.x()-b.x())+Math.abs((long)a.z()-b.z());}
 public static Optional<Pos> next(WorldAccess world,Pos start,Pos goal,ToDoubleBiFunction<Pos,Pos> penalty){
  var route=route(world,start,goal,penalty);
  return route.isEmpty()?Optional.empty():Optional.of(route.get(0));
 }
 public static List<Pos> route(WorldAccess world,Pos start,Pos goal,ToDoubleBiFunction<Pos,Pos> penalty){
  if(start.equals(goal)||start.y()!=goal.y())return List.of();
  var queue=new PriorityQueue<Node>(Comparator.comparingDouble(Node::estimate).thenComparingLong(Node::order));
  var costs=new HashMap<Pos,Double>();var parent=new HashMap<Pos,Pos>();
  long order=0;costs.put(start,0.0);queue.add(new Node(start,0,distance(start,goal),order++));
  int expanded=0;
  while(!queue.isEmpty()&&expanded<512){
   var n=queue.remove();if(n.cost()>costs.get(n.pos()))continue;
   if(n.pos().equals(goal)){
    var path=new ArrayList<Pos>();
    for(Pos p=goal;!p.equals(start);p=parent.get(p))path.add(p);
    Collections.reverse(path);return List.copyOf(path);
   }
   expanded++;
   for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
    var p=new Pos(n.pos().x()+d[0],start.y(),n.pos().z()+d[1]);
    if(distance(start,p)>32||world.block(p)!=Block.AIR||world.block(new Pos(p.x(),p.y()-1,p.z()))==Block.AIR)continue;
    double extra=penalty.applyAsDouble(n.pos(),p);
    if(!Double.isFinite(extra))throw new IllegalArgumentException("penalty");
    double cost=n.cost()+1+Math.max(0,Math.min(4,extra));
    if(cost>=costs.getOrDefault(p,Double.POSITIVE_INFINITY))continue;
    costs.put(p,cost);parent.put(p,n.pos());
    queue.add(new Node(p,cost,cost+distance(p,goal),order++));
   }
  }
  return List.of();
 }
}
