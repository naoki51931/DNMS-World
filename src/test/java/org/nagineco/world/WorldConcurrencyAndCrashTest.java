package org.nagineco.world;

import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.util.UUID;
import java.util.concurrent.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorldConcurrencyAndCrashTest {
    @TempDir Path dir;

    @Test void concurrentSpawnAndRewindRejectsStaleEpoch() throws Exception {
        try (var app = new NaoLiveServer()) {
            app.configureSave(dir.resolve("latest.bin"));
            String generation = Files.readString(dir.resolve("latest-generation.txt")).trim();
            String[] identity = app.identity().split("\"");
            String token = "01234567890123456789012345678901";
            UUID id = UUID.randomUUID();
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            app.registerWorldApi(server, token);
            server.start();
            try (var pool = Executors.newFixedThreadPool(2)) {
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/npcs"))
                    .timeout(java.time.Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + token)
                    .header("X-DNMS-World", identity[3])
                    .header("X-DNMS-Epoch", identity[7])
                    .POST(HttpRequest.BodyPublishers.ofString(id + ",100,1,16")).build();
                var client = HttpClient.newHttpClient();
                var gate = new CountDownLatch(1);
                var spawn = pool.submit(() -> { gate.await(); return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode(); });
                var rewind = pool.submit(() -> { gate.await(); return app.command("rewind:" + generation); });
                gate.countDown();
                assertTrue(rewind.get(30, TimeUnit.SECONDS));
                int status = spawn.get(30, TimeUnit.SECONDS);
                assertTrue(status == 201 || status == 409, "status=" + status);
                assertFalse(app.apiNpcs().contains(id.toString()));
                assertEquals(409, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
            } finally { server.stop(0); }
        }
    }

    @Test void abruptJvmExitReleasesLockAndRestoresCheckpoint() throws Exception {
        Path save = dir.resolve("latest.bin");
        Path output = dir.resolve("child.log");
        String javaExe = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String cp = String.join(java.io.File.pathSeparator,
            Path.of(CrashWorker.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
            Path.of(NaoLiveServer.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        var child = new ProcessBuilder(javaExe, "-cp", cp, CrashWorker.class.getName(), save.toString())
            .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(child.waitFor(60, TimeUnit.SECONDS), "child timeout");
            assertEquals(23, child.exitValue(), Files.readString(output));
            String expected = Files.readString(dir.resolve("expected.txt"));
            try (var app = new NaoLiveServer()) {
                app.configureSave(save);
                assertEquals(expected, app.apiNpcs());
            }
        } finally { if (child.isAlive()) child.destroyForcibly(); }
    }

    public static class CrashWorker {
        public static void main(String[] args) throws Exception {
            Path save = Path.of(args[0]);
            var app = new NaoLiveServer();
            app.configureSave(save);
            app.advance();
            app.checkpoint();
            Files.writeString(save.resolveSibling("expected.txt"), app.apiNpcs());
            app.damage(0, 1);
            Runtime.getRuntime().halt(23);
        }
    }
}
