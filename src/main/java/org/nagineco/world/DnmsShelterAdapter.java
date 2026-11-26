package org.nagineco.world;
import org.nagineco.doppel.core.NaoBuildingPlan;
import org.nagineco.world.WorldAccess.*;
/** Uses the upstream blueprint without changing its order or offsets. */
public final class DnmsShelterAdapter {
 private final WorkSession work;
 public DnmsShelterAdapter(WorkSession work){this.work=work;}
 public int buildNext(String actor,Pos from,Pos anchor,int completed){
  if(NaoBuildingPlan.remaining(completed)==0)return completed;
  var offset=NaoBuildingPlan.next(completed);
  var target=new Pos(Math.addExact(anchor.x(),offset.x()),Math.addExact(anchor.y(),offset.y()),Math.addExact(anchor.z(),offset.z()));
  return work.place(actor,from,target,Block.LOG)?completed+1:completed;
 }
}
