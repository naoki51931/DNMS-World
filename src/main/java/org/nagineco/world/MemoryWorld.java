package org.nagineco.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MemoryWorld implements WorldAccess {
    private final Map<Pos, Block> blocks = new HashMap<>();
    private long tick;
    private long revision;
    public synchronized long revision(){return revision;}
    public synchronized Block block(Pos pos) { return blocks.getOrDefault(Objects.requireNonNull(pos), Block.AIR); }
    public synchronized boolean compareAndSet(Pos pos, Block expected, Block replacement) {
        Objects.requireNonNull(expected); Objects.requireNonNull(replacement);
        if (block(pos) != expected) return false;
        if(expected==replacement)return true;
        revision++;
        if (replacement == Block.AIR) blocks.remove(pos); else blocks.put(pos, replacement);
        return true;
    }
    public synchronized long tick() { return tick; }
    public synchronized void advance() { tick = Math.incrementExact(tick); }
    synchronized void write(java.io.DataOutput out)throws java.io.IOException {
        out.writeLong(tick);out.writeInt(blocks.size());
        for(var e:blocks.entrySet()){var p=e.getKey();out.writeInt(p.x());out.writeInt(p.y());out.writeInt(p.z());out.writeUTF(e.getValue().name());}
    }
    synchronized void read(java.io.DataInput in)throws java.io.IOException {
        long t=in.readLong();int n=in.readInt();if(t<0||n<0||n>1000000)throw new java.io.IOException("Invalid world");
        blocks.clear();tick=t;revision++;
        for(int i=0;i<n;i++)blocks.put(new Pos(in.readInt(),in.readInt(),in.readInt()),Block.valueOf(in.readUTF()));
    }
}
