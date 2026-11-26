package org.nagineco.world;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import org.nagineco.doppel.core.PhysicalSkillSync.*;
import static org.junit.jupiter.api.Assertions.*;
class SkillCheckpointTest {
 @TempDir Path dir;
 @Test void modes()throws Exception{
  var sim=new NaoSimulation(256);
  var source=new Skill(10,20,30,40,50,60);
  var recipient=new Skill(1,2,3,4,5,6);
  sim.setSkill(0,source);sim.setSkill(255,recipient);
  sim.synchronizeSkills(0,Mode.INDIVIDUAL);assertEquals(recipient,sim.skill(255));
  sim.synchronizeSkills(0,Mode.TECHNIQUE);assertEquals(new Skill(10,20,30,4,5,6),sim.skill(255));
  sim.synchronizeSkills(0,Mode.FULL);assertEquals(source,sim.skill(255));
  sim.save(dir.resolve("skill.bin"));assertEquals(source,NaoSimulation.load(dir.resolve("skill.bin")).skill(255));
 }
 @Test void restoreSettingsAndMemory()throws Exception{
  var path=dir.resolve("latest.bin");String id;Path journal;
  try(var a=new NaoLiveServer()){a.configureSave(path);
  assertTrue(a.command("skill:mode:TECHNIQUE"));assertTrue(a.command("skill:source:23"));
  assertTrue(a.command("mip:time:06:00"));for(int i=0;i<100;i++)a.advance();
  a.checkpoint();id=Files.readString(dir.resolve("latest-generation.txt"));
  journal=dir.resolve("history").resolve(id+".memory").resolve("nao_01.md");
  assertTrue(Files.readString(journal).contains("- day="));
  }
  try(var b=new NaoLiveServer()){b.configureSave(path);
  assertTrue(b.snapshot().contains("\"skillMode\":\"TECHNIQUE\""));
  assertTrue(b.snapshot().contains("\"skillSource\":23"));
  assertTrue(b.snapshot().contains("\"mipDay\":0"));
  assertTrue(b.command("rewind:"+id));
  Files.delete(journal);assertFalse(b.command("rewind:"+id));
  }
 }
}
