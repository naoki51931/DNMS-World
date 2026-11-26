package org.nagineco.world;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nagineco.doppel.core.WorldIdentityPolicy;
import org.nagineco.doppel.core.DnmsWorldApi;
import static org.junit.jupiter.api.Assertions.*;

class WorldEnduranceTest {
    @TempDir Path dir;

    @Test void integratedPopulationRecovery() throws Exception {
        Path save = dir.resolve("latest.bin");
        String expected, memory, generation;
        // Bootstrap a full population without producing 232 intermediate checkpoints.
        new NaoSimulation(256).save(save);
        try (var app = new NaoLiveServer()) {
            app.configureSave(save);
            for (int i = 0; i < 240; i++) app.advance();
            assertTrue(app.damage(0, 1));
            assertTrue(app.command("mip:run"));
            memory = app.searchMemory("SAFETY");
            assertNotEquals("[]", memory);
            app.checkpoint();
            generation = Files.readString(dir.resolve("latest-generation.txt")).trim();
            expected = app.apiNpcs();
            assertTrue(app.damage(0, 2));
            assertTrue(app.command("rewind:" + generation));
            assertEquals(expected, app.apiNpcs());
            assertEquals(memory, app.searchMemory("SAFETY"));
            assertEquals(DnmsWorldApi.SpawnResult.CAPACITY,
                app.apiSpawn(UUID.randomUUID(), WorldIdentityPolicy.anchor(0)));
        }
        try (var app = new NaoLiveServer()) {
            app.configureSave(save);
            assertEquals(expected, app.apiNpcs());
            assertEquals(memory, app.searchMemory("SAFETY"));
        }
    }

    @ParameterizedTest(name = "longRunningPopulation days={0}")
    @ValueSource(ints = {7, 30})
    @EnabledIfSystemProperty(named="dnms.endurance", matches="true")
    void longRunningPopulation(int days) throws Exception {
        var sim = new NaoSimulation(256);
        var memory = new DnmsMemoryAdapter(dir.resolve("memory"));
        memory.initialize(256);
        var ids = sim.npcs().stream().map(n -> n.id()).toList();
        Path save = dir.resolve("world.bin");
        long previousBytes = 0;
        for (int day = 1; day <= days; day++) {
            long start = System.nanoTime();
            long[] samples = new long[24000];
            for (int t = 0; t < samples.length; t++) {
                long before = System.nanoTime();
                var states = sim.states();
                sim.tick();
                var after = sim.states();
                for (int n = 0; n < 256; n++) memory.observe(sim.ticks(), states.get(n), after.get(n));
                samples[t] = System.nanoTime() - before;
            }
            long mipStart = System.nanoTime();
            memory.integrate();
            double mipMs = (System.nanoTime() - mipStart) / 1e6;
            long searchStart = System.nanoTime();
            var matches = memory.search("SAFETY", 100);
            double searchMs = (System.nanoTime() - searchStart) / 1e6;
            assertTrue(matches.size() <= 100);
            long heapBytes = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            long bytes = Files.size(dir.resolve("memory/nao_integrated_memory.md"));
            assertTrue(bytes >= previousBytes, "shared index unexpectedly shrank");
            previousBytes = bytes;
            assertEquals(day * 24000L, sim.ticks());
            assertEquals(ids, sim.npcs().stream().map(n -> n.id()).toList());
            for (var n : sim.states()) {
                assertTrue(Double.isFinite(sim.health(n.slot())));
                assertTrue(sim.health(n.slot()) >= 0 && sim.health(n.slot()) <= 20);
                assertTrue(n.built() <= n.collected());
            }
            sim.save(save);
            if (day == 1 || day == days) {
                Path damaged = dir.resolve("damaged.bin");
                byte[] original = Files.readAllBytes(save);
                Files.write(damaged, Arrays.copyOf(original, Math.min(8, original.length)));
                assertThrows(java.io.IOException.class, () -> NaoSimulation.load(damaged));
                assertArrayEquals(original, Files.readAllBytes(save));
            }
            var restored = NaoSimulation.load(save);
            assertEquals(sim.states(), restored.states());
            assertEquals(sim.npcs(), restored.npcs());
            sim = restored;
            Arrays.sort(samples);
            double p95 = samples[22799] / 1e6;
            System.out.printf(Locale.ROOT,
                "days=%d day=%d population=256 elapsedMs=%.3f p95TickMs=%.3f maxTickMs=%.3f mipMs=%.3f indexBytes=%d checkpointBytes=%d profilingCandidate=%b searchMs=%.3f heapBytes=%d%n",
                days, day, (System.nanoTime()-start)/1e6, p95, samples[23999]/1e6,
                mipMs, bytes, Files.size(save), p95 > 25, searchMs, heapBytes);
        }
    }
}
