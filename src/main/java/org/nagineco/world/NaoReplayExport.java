package org.nagineco.world;
import java.nio.file.*;
import org.nagineco.world.WorldAccess.*;
public final class NaoReplayExport {
 public static void main(String[] args)throws Exception{
  var sim=new NaoSimulation();var out=new StringBuilder("[");
  for(int t=0;t<=600;t++){
   if(t>0){sim.tick();out.append(',');}
   out.append("{\"tick\":").append(t).append(",\"naos\":[");
   boolean first=true;
   for(var n:sim.states()){
    if(!first)out.append(',');first=false;
    var p=n.position();
    out.append('[').append(n.slot()).append(',').append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(",\"").append(n.goal()).append("\",").append(n.collected()).append(',').append(n.built()).append(']');
   }
   out.append("]}");
  }
  var path=Path.of(args[0]);Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,out.append(']').toString());
 }
}
