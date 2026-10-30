package com.austin.fileflow.update;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.regex.Pattern;

/** Blocking operations are called exclusively by the update card's background worker. */
public final class UpdateService implements AutoCloseable {
    public static final URI RELEASES = URI.create("https://github.com/Austin-Sha789/FileFlow/releases");
    public static final URI API = URI.create("https://api.github.com/repos/Austin-Sha789/FileFlow/releases/latest");
    private static final String ASSET_PREFIX = "/Austin-Sha789/FileFlow/releases/download/";
    private static final long MAX_ASSET = 2L * 1024 * 1024 * 1024;
    public enum Code { NETWORK, RATE_LIMIT, BAD_RELEASE, DOWNLOAD, INTEGRITY, CANCELLED, OPEN }
    public static final class Failure extends IOException {
        private final Code code;
        public Failure(Code code) { super(code.name()); this.code = code; }
        public Code code() { return code; }
    }
    public enum Status { NO_RELEASE, CURRENT, AVAILABLE, NO_PACKAGE, UNVERIFIED }
    public record Asset(String name, URI url, long size, String sha256) { }
    public record Release(String tag, SemanticVersion version, String notes, URI page, List<Asset> assets) { }
    public record Check(Status status, Release release, Asset asset) { }
    public record Platform(String os, String arch) {
        public static Platform current() { return detect(System.getProperty("os.name", ""), System.getProperty("os.arch", "")); }
        public static Platform detect(String os, String arch) {
            os = os.toLowerCase(Locale.ROOT); arch = arch.toLowerCase(Locale.ROOT);
            return new Platform(os.contains("win") ? "windows" : os.contains("mac") ? "macos" : "unsupported",
                    Set.of("amd64", "x86_64", "x64").contains(arch) ? "x64" : Set.of("aarch64", "arm64").contains(arch) ? "arm64" : "unsupported");
        }
    }
    private final URI api;
    private final Path directory;
    private final Predicate<URI> requestPolicy, assetPolicy;
    private final int timeout;
    private final AtomicReference<HttpURLConnection> active = new AtomicReference<>();

    public UpdateService() { this(API, defaultDirectory(), UpdateService::trustedGithub, UpdateService::repositoryAsset, 15000); }
    UpdateService(URI api, Path directory, Predicate<URI> policy, int timeout) { this(api, directory, policy, policy, timeout); }
    private UpdateService(URI api, Path directory, Predicate<URI> requestPolicy, Predicate<URI> assetPolicy, int timeout) {
        this.api = api; this.directory = directory.toAbsolutePath().normalize(); this.requestPolicy = requestPolicy; this.assetPolicy = assetPolicy; this.timeout = timeout;
    }
    public static Path defaultDirectory() {
        String home = System.getProperty("user.home");
        if (Platform.current().os().equals("windows")) {
            String local = System.getenv("LOCALAPPDATA");
            return (local == null || local.isBlank() ? Path.of(home, "AppData", "Local") : Path.of(local)).resolve("FileFlow").resolve("Updates");
        }
        if (Platform.current().os().equals("macos")) return Path.of(home, "Library", "Caches", "FileFlow", "Updates");
        return Path.of(home, ".cache", "fileflow", "updates");
    }
    private static boolean secure(URI uri) { return uri != null && uri.getHost() != null && "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443); }
    static boolean repositoryAsset(URI uri) { return secure(uri) && "github.com".equalsIgnoreCase(uri.getHost()) && uri.getPath().startsWith(ASSET_PREFIX); }
    static boolean trustedGithub(URI uri) {
        if (!secure(uri)) return false;
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return repositoryAsset(uri) || (host.equals("api.github.com") && uri.getPath().startsWith("/repos/Austin-Sha789/FileFlow/"))
                || Set.of("release-assets.githubusercontent.com", "objects.githubusercontent.com", "github-releases.githubusercontent.com").contains(host);
    }
    private void cancelled(BooleanSupplier cancelled) throws Failure {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) throw new Failure(Code.CANCELLED);
    }
    private HttpURLConnection connect(URI uri, String accept, BooleanSupplier cancelled) throws IOException {
        for (int redirects = 0; redirects <= 5; redirects++) {
            cancelled(cancelled);
            if (!requestPolicy.test(uri)) throw new Failure(Code.BAD_RELEASE);
            var connection = (HttpURLConnection) uri.toURL().openConnection(); active.set(connection);
            connection.setConnectTimeout(timeout); connection.setReadTimeout(timeout); connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "FileFlow/" + BuildInfo.VERSION);
            connection.setRequestProperty("Accept", accept); connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10");
            int status;
            try { cancelled(cancelled); status = connection.getResponseCode(); }
            catch (IOException failure) { finish(connection); cancelled(cancelled); throw new Failure(Code.NETWORK); }
            if (Set.of(301, 302, 303, 307, 308).contains(status)) {
                String location = connection.getHeaderField("Location"); finish(connection);
                if (location == null) throw new Failure(Code.BAD_RELEASE);
                try { uri = uri.resolve(location); } catch (IllegalArgumentException failure) { throw new Failure(Code.BAD_RELEASE); }
                continue;
            }
            return connection;
        }
        throw new Failure(Code.NETWORK);
    }
    private void finish(HttpURLConnection connection) { active.compareAndSet(connection, null); connection.disconnect(); }
    public void cancelCurrentRequest() {
        HttpURLConnection connection = active.getAndSet(null);
        if (connection != null) CompletableFuture.runAsync(connection::disconnect);
    }
    @Override public void close() { cancelCurrentRequest(); }
    public Check check(String current, Platform platform, BooleanSupplier cancelled) throws IOException {
        HttpURLConnection connection = connect(api, "application/vnd.github+json", cancelled);
        try {
            int status = connection.getResponseCode();
            if (status == 404) return new Check(Status.NO_RELEASE, null, null);
            if (status == 403 || status == 429) throw new Failure(Code.RATE_LIMIT);
            if (status != 200) throw new Failure(Code.NETWORK);
            byte[] body;
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int read;
                while ((read = input.read(buffer)) != -1) {
                    cancelled(cancelled); if (output.size() + read > 2 * 1024 * 1024) throw new Failure(Code.BAD_RELEASE);
                    output.write(buffer, 0, read);
                }
                body = output.toByteArray();
            }
            return parse(new String(body, StandardCharsets.UTF_8), current, platform);
        } catch (Failure failure) { throw failure; }
        catch (IOException failure) { cancelled(cancelled); throw new Failure(Code.NETWORK); }
        finally { finish(connection); }
    }
    private static String string(JsonObject object, String key) { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : ""; }
    Check parse(String json, String current, Platform platform) throws Failure {
        try {
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            if ((data.has("draft") && data.get("draft").getAsBoolean()) || (data.has("prerelease") && data.get("prerelease").getAsBoolean())) return new Check(Status.NO_RELEASE, null, null);
            String tag = string(data, "tag_name"); SemanticVersion version = SemanticVersion.parse(tag);
            if (!version.pre().isEmpty()) return new Check(Status.NO_RELEASE, null, null);
            String notes = string(data, "body"); if (notes.length() > 50000) notes = notes.substring(0, 50000);
            List<Asset> assets = new ArrayList<>();
            if (data.has("assets")) for (JsonElement value : data.getAsJsonArray("assets")) {
                JsonObject object = value.getAsJsonObject();
                if (object.has("state") && !"uploaded".equals(string(object, "state"))) continue;
                String name = string(object, "name"); URI url = URI.create(string(object, "browser_download_url"));
                long size = object.get("size").getAsBigInteger().longValueExact();
                if (size <= 0 || size > MAX_ASSET || name.length() > 200 || !assetPolicy.test(url)) continue;
                String digest = string(object, "digest");
                String sha = digest.matches("(?i)sha256:[0-9a-f]{64}") ? digest.substring(7).toLowerCase(Locale.ROOT) : "";
                assets.add(new Asset(name, url, size, sha));
            }
            URI page = URI.create(RELEASES + "/tag/" + URLEncoder.encode(tag, StandardCharsets.UTF_8));
            Release release = new Release(tag, version, notes, page, List.copyOf(assets));
            if (version.compareTo(SemanticVersion.parse(current)) <= 0) return new Check(Status.CURRENT, release, null);
            Optional<Asset> selected = assets.stream().filter(asset -> score(asset, version, platform) > 0)
                    .max(Comparator.comparingInt(asset -> score(asset, version, platform)));
            if (selected.isEmpty()) return new Check(Status.NO_PACKAGE, release, null);
            if (selected.get().sha256().isEmpty()) return new Check(Status.UNVERIFIED, release, null);
            return new Check(Status.AVAILABLE, release, selected.get());
        } catch (RuntimeException malformed) { throw new Failure(Code.BAD_RELEASE); }
    }
    private static int score(Asset asset, SemanticVersion version, Platform platform) {
        if (!Set.of("windows", "macos").contains(platform.os()) || platform.arch().equals("unsupported")) return 0;
        String prefix = "(?i)^FileFlow-v?" + Pattern.quote(version.core()) + "-";
        String arch = platform.arch().equals("x64") ? "(?:x64|x86_64|amd64)" : "(?:arm64|aarch64)";
        String name = asset.name();
        int score;
        if (platform.os().equals("windows")) {
            score = name.matches(prefix + "(?:windows|win)-" + arch + "\\.msi$") ? 30
                    : name.matches(prefix + "(?:windows|win)-" + arch + "-setup\\.exe$") ? 20 : 0;
        } else {
            score = name.matches(prefix + "macos-" + arch + "\\.(?:dmg|pkg)$") ? 30
                    : name.matches(prefix + "macos-universal\\.(?:dmg|pkg)$") ? 10 : 0;
        }
        return score == 0 ? 0 : score + (asset.sha256().isEmpty() ? 0 : 100);
    }
    public Path download(Asset asset, BooleanSupplier cancelled, LongConsumer progress) throws IOException {
        if (!assetPolicy.test(asset.url()) || !asset.sha256().matches("[0-9a-f]{64}") || asset.size() <= 0 || asset.size() > MAX_ASSET) throw new Failure(Code.BAD_RELEASE);
        Files.createDirectories(directory);
        Path target = directory.resolve(asset.name()).normalize();
        if (!target.getParent().equals(directory) || !target.getFileName().toString().equals(asset.name()) || asset.name().contains("\\")) throw new Failure(Code.BAD_RELEASE);
        cancelled(cancelled);
        if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) && verified(target, asset, cancelled)) { progress.accept(asset.size()); return target; }
        if (Files.isSymbolicLink(target)) throw new Failure(Code.BAD_RELEASE);
        Path partial = Files.createTempFile(directory, "fileflow-update-", ".part");
        try {
            HttpURLConnection connection = connect(asset.url(), "application/octet-stream", cancelled);
            try {
                if (connection.getResponseCode() != 200) throw new Failure(Code.DOWNLOAD);
                long declared = connection.getContentLengthLong();
                if (declared >= 0 && declared != asset.size()) throw new Failure(Code.INTEGRITY);
                MessageDigest hash = sha256(); long total = 0; long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(30);
                try (InputStream input = connection.getInputStream(); OutputStream output = Files.newOutputStream(partial)) {
                    byte[] buffer = new byte[65536]; int read;
                    while ((read = input.read(buffer)) != -1) {
                        cancelled(cancelled); total += read;
                        if (total > asset.size() || System.nanoTime() > deadline) throw new Failure(Code.INTEGRITY);
                        output.write(buffer, 0, read); hash.update(buffer, 0, read); progress.accept(total);
                    }
                }
                cancelled(cancelled);
                if (total != asset.size() || !MessageDigest.isEqual(hash.digest(), HexFormat.of().parseHex(asset.sha256()))) throw new Failure(Code.INTEGRITY);
            } finally { finish(connection); }
            cancelled(cancelled);
            try { Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException failure) { Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING); }
            return target;
        } catch (Failure failure) { throw failure; }
        catch (IOException failure) { cancelled(cancelled); throw new Failure(Code.DOWNLOAD); }
        finally { Files.deleteIfExists(partial); }
    }
    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private boolean verified(Path file, Asset asset, BooleanSupplier cancelled) throws IOException {
        if (Files.size(file) != asset.size()) return false;
        MessageDigest hash = sha256();
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[65536]; int read;
            while ((read = input.read(buffer)) != -1) { cancelled(cancelled); hash.update(buffer, 0, read); }
        }
        return MessageDigest.isEqual(hash.digest(), HexFormat.of().parseHex(asset.sha256()));
    }
    public void verifyForInstall(Path file, Asset asset, BooleanSupplier cancelled) throws IOException {
        if (!file.toAbsolutePath().normalize().getParent().equals(directory) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !verified(file, asset, cancelled)) throw new Failure(Code.INTEGRITY);
    }
}
