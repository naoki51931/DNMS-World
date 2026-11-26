package org.nagineco.world;
import java.util.UUID;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nagineco.doppel.core.DnmsWorldApi;
import static org.junit.jupiter.api.Assertions.*;
class WorldApiTest {
 @TempDir Path dir;
 @Test void spawnAndRestore() throws Exception {
  var sim=new NaoSimulation(0);
  DnmsWorldApi api=sim;
  var id=UUID.randomUUID();
  var pos=new DnmsWorldApi.Position(4,1,4);
  assertEquals(DnmsWorldApi.SpawnResult.INVALID_POSITION,api.spawnNpc(id,new DnmsWorldApi.Position(0,0,0)));
  assertEquals(DnmsWorldApi.SpawnResult.CREATED,api.spawnNpc(id,pos));
  assertEquals(DnmsWorldApi.SpawnResult.ALREADY_EXISTS,api.spawnNpc(id,pos));
  assertEquals(id,api.npcs().getFirst().id());
  sim.tick();
  var file=dir.resolve("api.bin");sim.save(file);
  DnmsWorldApi restored=NaoSimulation.load(file);
  assertEquals(api.npcs(),restored.npcs());
  assertEquals(api.worldTick(),restored.worldTick());
  assertEquals(DnmsWorldApi.SpawnResult.ALREADY_EXISTS,restored.spawnNpc(id,pos));
 }
 @Test void populationLimit(){
  DnmsWorldApi api=new NaoSimulation(0);
  for(int i=0;i<256;i++)assertEquals(DnmsWorldApi.SpawnResult.CREATED,api.spawnNpc(UUID.randomUUID(),new DnmsWorldApi.Position((i%16)*12+4,1,(i/16)*12+4)));
  assertEquals(256,api.npcs().size());
  assertEquals(DnmsWorldApi.SpawnResult.CAPACITY,api.spawnNpc(UUID.randomUUID(),new DnmsWorldApi.Position(4,1,196)));
 }
}
