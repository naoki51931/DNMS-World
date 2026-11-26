package org.nagineco.world;
// Compatibility adapter only.
public final class SharedMemorySearch {
 public record Event(int nao,int day,int tick,String position,String action,String emotion,String reason){
  org.nagineco.doppel.core.SharedMemorySearch.Event core(){return new org.nagineco.doppel.core.SharedMemorySearch.Event(nao,day,tick,position,action,emotion,reason);}
 }
 private final org.nagineco.doppel.core.SharedMemorySearch delegate;
 public SharedMemorySearch(java.nio.file.Path file)throws java.io.IOException{delegate=new org.nagineco.doppel.core.SharedMemorySearch(file);}
 private static Event local(org.nagineco.doppel.core.SharedMemorySearch.Event e){return new Event(e.nao(),e.day(),e.tick(),e.position(),e.action(),e.emotion(),e.reason());}
 public java.util.List<Event> search(String query,int limit){return delegate.search(query,limit).stream().map(SharedMemorySearch::local).toList();}
 public java.util.List<Event> hazardEvents(){return delegate.hazardEvents().stream().map(SharedMemorySearch::local).toList();}
}
