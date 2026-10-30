package com.austin.fileflow.update;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Local fixtures only; installer bytes are never executed. */
public final class UpdateFixtureServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService workers = Executors.newCachedThreadPool(runnable -> { Thread thread = new Thread(runnable, "update-fixture"); thread.setDaemon(true); return thread; });
    public final AtomicInteger status = new AtomicInteger(200);
    public final AtomicReference<String> response = new AtomicReference<>("{}");
    public final AtomicInteger downloads = new AtomicInteger();
    public final CountDownLatch stalled = new CountDownLatch(1), releaseStall = new CountDownLatch(1);
    public byte[] payload = "test installer bytes; do not execute".getBytes(StandardCharsets.UTF_8);
    public UpdateFixtureServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); server.setExecutor(workers);
        server.createContext("/latest", exchange -> send(exchange, status.get(), response.get().getBytes(StandardCharsets.UTF_8)));
        server.createContext("/asset", exchange -> { downloads.incrementAndGet(); send(exchange, 200, payload); });
        server.createContext("/redirect", exchange -> { exchange.getResponseHeaders().set("Location", "/asset"); exchange.sendResponseHeaders(302, -1); exchange.close(); });
        server.createContext("/blocked", exchange -> { exchange.getResponseHeaders().set("Location", "https://example.invalid/installer"); exchange.sendResponseHeaders(302, -1); exchange.close(); });
        server.createContext("/corrupt", exchange -> send(exchange, 200, new byte[payload.length]));
        server.createContext("/truncated", exchange -> { exchange.sendResponseHeaders(200, payload.length + 10); exchange.getResponseBody().write(payload); exchange.close(); });
        server.createContext("/stall", exchange -> {
            exchange.sendResponseHeaders(200, payload.length); exchange.getResponseBody().write(payload, 0, 1); exchange.getResponseBody().flush(); stalled.countDown();
            try { releaseStall.await(5, TimeUnit.SECONDS); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        server.createContext("/slow", exchange -> {
            try { Thread.sleep(600); send(exchange, 200, response.get().getBytes(StandardCharsets.UTF_8)); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); exchange.close(); }
        });
        server.start();
    }
    private void send(HttpExchange exchange, int code, byte[] data) throws java.io.IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(code, data.length);
        try (var output = exchange.getResponseBody()) { output.write(data); }
    }
    public URI uri(String path) { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path); }
    public UpdateService service(Path directory) { return service(directory, "/latest", 1000); }
    public UpdateService service(Path directory, String api, int timeout) {
        return new UpdateService(uri(api), directory, value -> "http".equals(value.getScheme()) && "127.0.0.1".equals(value.getHost()) && value.getPort() == server.getAddress().getPort(), timeout);
    }
    public String digest() {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload)); } catch (Exception error) { throw new AssertionError(error); }
    }
    public JsonObject asset(String name, String endpoint, boolean digest) {
        JsonObject asset = new JsonObject(); asset.addProperty("name", name); asset.addProperty("browser_download_url", uri(endpoint).toString());
        asset.addProperty("size", payload.length); asset.addProperty("state", "uploaded"); if (digest) asset.addProperty("digest", "sha256:" + digest()); return asset;
    }
    public String release(String tag, JsonObject... assets) {
        JsonObject release = new JsonObject(); release.addProperty("tag_name", tag); release.addProperty("draft", false); release.addProperty("prerelease", false);
        release.addProperty("body", "Updates\n- Faster file previews\n- 修复显示问题"); JsonArray array = new JsonArray(); for (var asset : assets) array.add(asset); release.add("assets", array); return release.toString();
    }
    public void available(String name) { response.set(release("v1.1.0", asset(name, "/asset", true))); }
    @Override public void close() { releaseStall.countDown(); server.stop(0); workers.shutdownNow(); }
}
