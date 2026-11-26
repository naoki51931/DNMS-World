package org.nagineco.world;
// Compatibility adapter only.
public final class MemoryDecision {
 private final org.nagineco.doppel.core.MemoryDecision delegate;
 public MemoryDecision(java.util.List<SharedMemorySearch.Event> events){delegate=new org.nagineco.doppel.core.MemoryDecision(events.stream().map(SharedMemorySearch.Event::core).toList());}
 public double hazard(WorldAccess.Pos p){return delegate.hazard(new org.nagineco.doppel.core.DnmsWorldApi.Position(p.x(),p.y(),p.z()));}
 public double score(String direction){return delegate.score(direction);}
}
