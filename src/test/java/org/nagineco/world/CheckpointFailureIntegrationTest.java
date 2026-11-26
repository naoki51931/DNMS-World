package org.nagineco.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CheckpointFailureIntegrationTest {
    @TempDir Path dir;

    @Test void incompleteCheckpointRejectsRestartAndReleasesLock() throws Exception {
        Path save = dir.resolve("latest.bin");
        Path pointer = dir.resolve("latest-generation.txt");
        String generation;
        String world;
        try (var app = new NaoLiveServer()) {
            app.configureSave(save);
            generation = Files.readString(pointer).trim();
            world = app.identity().split("\"")[3];
        }
        Path marker = dir.resolve("history").resolve(generation + ".memory").resolve("incomplete");
        Files.writeString(marker, "pending");
        try (var failed = new NaoLiveServer()) {
            assertThrows(IOException.class, () -> failed.configureSave(save));
        }
        assertEquals(generation, Files.readString(pointer).trim());
        assertTrue(Files.exists(marker));
        Files.delete(marker);
        try (var recovered = new NaoLiveServer()) {
            recovered.configureSave(save);
            assertEquals(world, recovered.identity().split("\"")[3]);
        }
    }

    @Test void missingGenerationPointerFailsClosed() throws Exception {
        Path save = dir.resolve("latest.bin");
        Path pointer = dir.resolve("latest-generation.txt");
        String generation;
        try (var app = new NaoLiveServer()) {
            app.configureSave(save);
            generation = Files.readString(pointer).trim();
        }
        Files.delete(pointer);
        try (var failed = new NaoLiveServer()) {
            assertThrows(IOException.class, () -> failed.configureSave(save));
        }
        assertFalse(Files.exists(pointer));
        Files.writeString(pointer, generation);
        try (var recovered = new NaoLiveServer()) {
            recovered.configureSave(save);
            assertTrue(Files.isRegularFile(pointer));
        }
    }
}
