package org.nagineco.world;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nagineco.doppel.core.WorldIdentityPolicy;
import org.nagineco.doppel.core.DnmsWorldApi;
import static org.junit.jupiter.api.Assertions.*;

class WorldReliabilityAndLoadTest {
    @TempDir Path dir;

    @Test void failedCheckpointPreservesPublishedGeneration() throws Exception {
        Path save = dir.resolve("latest.bin");
        Path pointer = dir.resolve("latest-generation.txt");
        String snapshot;
        try (var app = new NaoLiveServer()) {
            app.configureSave(save);
            String generation = Files.readString(pointer);
            String old = System.getProperty("dnms.minFreeBytes");
            try {
                System.setProperty("dnms.minFreeBytes", Long.toString(Long.MAX_VALUE));
                assertThrows(IOException.class, app::checkpoint);
                assertEquals(generation, Files.readString(pointer));
            } finally {
                if (old == null) System.clearProperty("dnms.minFreeBytes");
                else System.setProperty("dnms.minFreeBytes", old);
            }
            assertTrue(app.command("checkpoint"));
            snapshot = app.apiNpcs();
        }
        try (var app = new NaoLiveServer()) {
            app.configureSave(save);
            assertEquals(snapshot, app.apiNpcs());
        }
    }

    @Test void mipRollbackDiscardsFutureMemoryAndAllowsReintegration() throws Exception {
        try (var app = new NaoLiveServer()) {
            app.configureSave(dir.resolve("latest.bin"));
            String generation = Files.readString(dir.resolve("latest-generation.txt")).trim();
            assertTrue(app.damage(0, 1));
            assertTrue(app.command("mip:run"));
            assertNotEquals("[]", app.searchMemory("SAFETY"));
            assertFalse(app.command("mip:run"));
            assertTrue(app.command("rewind:" + generation));
            assertEquals("[]", app.searchMemory("SAFETY"));
            assertTrue(app.command("mip:run"));
            assertEquals("[]", app.searchMemory("SAFETY"));
        }
    }

    @Test void memoryIntegrationDeduplicatesOnlyWithinOwner() throws Exception {
        var adapter = new DnmsMemoryAdapter(dir);
        adapter.initialize(24);
        var sim = new NaoSimulation(24);
        var a = sim.states().get(0);
        var b = sim.states().get(1);
        adapter.recordDamage(1, a);
        adapter.recordDamage(1, a);
        adapter.recordDamage(1, b);
        Path first = dir.resolve("nao_01.md");
        String journal = Files.readString(first);
        adapter.integrate();
        var events = adapter.search("SAFETY", 100);
        assertEquals(2, events.size());
        assertEquals(2, events.stream().map(e -> e.nao()).distinct().count());
        String index = Files.readString(dir.resolve("nao_integrated_memory.md"));
        adapter.integrate();
        assertEquals(index, Files.readString(dir.resolve("nao_integrated_memory.md")));
        assertEquals(journal, Files.readString(first));
    }

    @ParameterizedTest
    @ValueSource(ints = {24, 64, 128, 256})
    void populationLoadAndIdentityRoundTrip(int count) throws Exception {
        var sim = new NaoSimulation(24);
        while (sim.npcs().size() < count) {
            int slot = sim.npcs().size();
            assertEquals(DnmsWorldApi.SpawnResult.CREATED,
                sim.spawnNpc(UUID.randomUUID(), WorldIdentityPolicy.anchor(slot)));
        }
        var ids = sim.npcs().stream().map(n -> n.id()).toList();
        assertEquals(count, new HashSet<>(ids).size());
        if (count == 256) assertFalse(sim.addNao());
        long start = System.nanoTime();
        for (int i = 0; i < 240; i++) sim.tick();
        long elapsed = System.nanoTime() - start;
        assertEquals(240, sim.ticks());
        assertEquals(count, sim.states().size());
        assertEquals(ids, sim.npcs().stream().map(n -> n.id()).toList());
        assertTrue(sim.states().stream().anyMatch(n -> n.attempts() > 0));
        Path save = dir.resolve("population-" + count + ".bin");
        sim.save(save);
        var restored = NaoSimulation.load(save);
        assertEquals(sim.ticks(), restored.ticks());
        assertEquals(sim.states(), restored.states());
        assertEquals(sim.npcs(), restored.npcs());
        System.out.printf(Locale.ROOT, "NPCs=%d ticks=240 elapsedMs=%.3f msPerTick=%.3f checkpointBytes=%d%n",
            count, elapsed / 1e6, elapsed / 240e6, Files.size(save));
    }
}
