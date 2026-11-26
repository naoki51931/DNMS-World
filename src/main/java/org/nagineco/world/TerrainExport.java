package org.nagineco.world;
import org.nagineco.world.WorldAccess.Pos;
import java.nio.file.*;
public final class TerrainExport {
 public static void main(String[] args) throws Exception {
  int size=32; var world=TerrainGenerator.generate(42,size);
  var json=new StringBuilder("["); boolean first=true;
  for(int x=0;x<size;x++) for(int z=0;z<size;z++) for(int y=0;y<12;y++){
   var b=world.block(new Pos(x,y,z)); if(b==WorldAccess.Block.AIR) continue;
   if(!first) json.append(','); first=false;
   json.append("[").append(x).append(',').append(y).append(',').append(z).append(",\"").append(b.name()).append("\"]");
  }
  Path out=Path.of(args[0]); Files.createDirectories(out.toAbsolutePath().getParent()); Files.writeString(out,json.append(']').toString());
 }
}
