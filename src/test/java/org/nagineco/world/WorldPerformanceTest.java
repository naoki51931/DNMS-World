package org.nagineco.world;

import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorldPerformanceTest {
    @TempDir Path dir;

    @Test void fullPopulationMemoryIntegration() throws Exception {
        var sim = new NaoSimulation(256);
        var memory = new DnmsMemoryAdapter(dir);
        memory.initialize(256);
        for (var state : sim.states())
            for (int tick = 1; tick <= 100; tick++)
                memory.recordDamage(tick, state);
        long start = System.nanoTime();
        memory.integrate();
        double ms = (System.nanoTime() - start) / 1e6;
        Path index = dir.resolve("nao_integrated_memory.md");
        try (var lines = Files.lines(index)) {
            assertEquals(25600, lines.filter(s -> s.startsWith("- nao=")).count());
        }
        assertEquals(100, memory.search("SAFETY", 100).size());
        System.out.printf(Locale.ROOT, "MIP population=256 events=25600 elapsedMs=%.3f bytes=%d%n", ms, Files.size(index));
    }

    @Test void fullDayPopulationPerformance() throws Exception {
        var sim = new NaoSimulation(256);
        var ids = sim.npcs().stream().map(n -> n.id()).toList();
        var bean = ManagementFactory.getThreadMXBean();
        boolean cpuAvailable = bean.isCurrentThreadCpuTimeSupported() && bean.isThreadCpuTimeEnabled();
        long cpuStart = cpuAvailable ? bean.getCurrentThreadCpuTime() : 0;
        long start = System.nanoTime();
        long[] samples = new long[24000];
        long sampledHeap = 0;
        for (int i = 0; i < samples.length; i++) {
            long before = System.nanoTime();
            sim.tick();
            samples[i] = System.nanoTime() - before;
            if (i % 100 == 0)
                sampledHeap = Math.max(sampledHeap, ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        }
        double elapsedMs = (System.nanoTime() - start) / 1e6;
        double cpuMs = cpuAvailable ? (bean.getCurrentThreadCpuTime() - cpuStart) / 1e6 : -1;
        Arrays.sort(samples);
        double p95 = samples[22799] / 1e6;
        assertEquals(24000, sim.ticks());
        assertEquals(ids, sim.npcs().stream().map(n -> n.id()).toList());
        assertEquals(256, sim.states().size());
        Path save = dir.resolve("full-day.bin");
        sim.save(save);
        var restored = NaoSimulation.load(save);
        assertEquals(sim.states(), restored.states());
        assertEquals(sim.npcs(), restored.npcs());
        System.out.printf(Locale.ROOT,
            "population=256 ticks=24000 elapsedMs=%.3f threadCpuMs=%.3f sampledHeapBytes=%d p95TickMs=%.3f maxTickMs=%.3f routeSearches=%d routeHits=%d checkpointBytes=%d nativeProfilingCandidate=%b%n",
            elapsedMs, cpuMs, sampledHeap, p95, samples[23999]/1e6, sim.routeSearches(), sim.routeHits(), Files.size(save), p95 > 25);
    }
}
