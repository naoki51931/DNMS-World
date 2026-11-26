package org.nagineco.world;
import org.nagineco.doppel.core.*;
import org.nagineco.world.WorldAccess.Pos;
public final class DnmsSkillAdapter {
 private final WorkSession work;
 private final DnmsShelterAdapter shelter;
 public DnmsSkillAdapter(WorkSession work){this.work=work;this.shelter=new DnmsShelterAdapter(work);}
 public double movementStep(PhysicalSkillSync.Skill skill){return PhysicalMovementPerformance.stepDistance(skill);}
 public boolean gather(int slot,long attempt,PhysicalSkillSync.Skill skill,Pos from,Pos target){
  return PhysicalWorkPerformance.canExecuteCollection(skill,slot,attempt)&&work.gather("nao-"+slot,from,target);
 }
 public int build(int slot,long attempt,PhysicalSkillSync.Skill skill,Pos from,Pos anchor,int completed){
  return PhysicalBuildPerformance.canBuild(skill,slot,attempt)?shelter.buildNext("nao-"+slot,from,anchor,completed):completed;
 }
}
