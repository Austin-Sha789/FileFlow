package com.austin.fileflow.update;

import com.google.gson.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class UpdateServiceTest {
    @TempDir Path directory;
    UpdateFixtureServer server;
    @BeforeEach void start() throws Exception { server = new UpdateFixtureServer(); }
    @AfterEach void stop() { server.close(); }
    private UpdateService.Platform windows() { return UpdateService.Platform.detect("Windows 11", "amd64"); }
    private UpdateService.Asset asset(String endpoint) { return new UpdateService.Asset("FileFlow-1.1.0-windows-x64.msi", server.uri(endpoint), server.payload.length, server.digest()); }
    private void noPartFiles() throws Exception { if (Files.exists(directory)) try (var files = Files.list(directory)) { assertTrue(files.noneMatch(path -> path.toString().endsWith(".part"))); } }
    @Test void noReleaseIsNotReportedAsUpToDate() throws Exception {
        server.status.set(404); assertEquals(UpdateService.Status.NO_RELEASE, server.service(directory).check("1.0.0", windows(), () -> false).status());
    }
    @Test void newVersionAndNotesAreReturnedWithoutDownloadingAnything() throws Exception {
        server.available(asset("/asset").name()); var result = server.service(directory).check("1.0.0", windows(), () -> false);
        assertEquals(UpdateService.Status.AVAILABLE, result.status()); assertEquals("v1.1.0", result.release().tag()); assertTrue(result.release().notes().contains("修复")); assertEquals(0, server.downloads.get());
    }
    @Test void equalAndOlderReleasesNeverOfferDowngrades() throws Exception {
        var service = server.service(directory); String release = server.release("v1.1.0", server.asset(asset("/asset").name(), "/asset", true));
        assertEquals(UpdateService.Status.CURRENT, service.parse(release, "1.1.0", windows()).status());
        assertEquals(UpdateService.Status.CURRENT, service.parse(release, "1.2.0", windows()).status());
        assertEquals(UpdateService.Status.AVAILABLE, service.parse(release, "1.1.0-SNAPSHOT", windows()).status());
    }
    @Test void eachOsAndArchitectureSelectsItsOwnInstaller() throws Exception {
        String json = server.release("v1.1.0", server.asset("FileFlow-1.1.0-windows-x64.msi", "/asset", true),
                server.asset("FileFlow-1.1.0-windows-arm64-setup.exe", "/asset", true), server.asset("FileFlow-1.1.0-macos-x64.dmg", "/asset", true),
                server.asset("FileFlow-1.1.0-macos-arm64.dmg", "/asset", true), server.asset("FileFlow-1.1.0-macos-universal.pkg", "/asset", true));
        var service = server.service(directory);
        assertTrue(service.parse(json, "1.0.0", windows()).asset().name().endsWith("windows-x64.msi"));
        assertTrue(service.parse(json, "1.0.0", UpdateService.Platform.detect("Windows 11", "aarch64")).asset().name().contains("windows-arm64"));
        assertTrue(service.parse(json, "1.0.0", UpdateService.Platform.detect("Mac OS X", "arm64")).asset().name().endsWith("macos-arm64.dmg"));
        assertTrue(service.parse(json, "1.0.0", UpdateService.Platform.detect("Mac OS X", "x86_64")).asset().name().endsWith("macos-x64.dmg"));
        assertEquals(UpdateService.Status.NO_PACKAGE, service.parse(json, "1.0.0", UpdateService.Platform.detect("Linux", "amd64")).status());
    }
    @Test void unrelatedPortableOrWrongPlatformAssetsAreNotInstallers() throws Exception {
        String json = server.release("v1.1.0", server.asset("source.zip", "/asset", true), server.asset("FileFlow-1.1.0-windows-arm64.msi", "/asset", true), server.asset("FileFlow-1.1.0-windows-x64.exe", "/asset", true));
        assertEquals(UpdateService.Status.NO_PACKAGE, server.service(directory).parse(json, "1.0.0", windows()).status());
    }
    @Test void missingDigestDoesNotEnableInstallation() throws Exception {
        assertEquals(UpdateService.Status.UNVERIFIED, server.service(directory).parse(server.release("v1.1.0", server.asset(asset("/asset").name(), "/asset", false)), "1.0.0", windows()).status());
    }
    @Test void malformedDraftAndPrereleaseMetadataIsHandled() throws Exception {
        var service = server.service(directory);
        assertEquals(UpdateService.Status.NO_RELEASE, service.parse("{\"draft\":true}", "1.0.0", windows()).status());
        assertEquals(UpdateService.Status.NO_RELEASE, service.parse("{\"prerelease\":true}", "1.0.0", windows()).status());
        assertThrows(UpdateService.Failure.class, () -> service.parse("<html>oops</html>", "1.0.0", windows()));
        assertThrows(UpdateService.Failure.class, () -> service.parse("{\"tag_name\":\"latest\"}", "1.0.0", windows()));
    }
    @Test void rateLimitsAndTimeoutsAreDistinctFromNoUpdate() throws Exception {
        server.status.set(429);
        assertEquals(UpdateService.Code.RATE_LIMIT, assertThrows(UpdateService.Failure.class, () -> server.service(directory).check("1.0.0", windows(), () -> false)).code());
        assertEquals(UpdateService.Code.NETWORK, assertThrows(UpdateService.Failure.class, () -> server.service(directory, "/slow", 100).check("1.0.0", windows(), () -> false)).code());
    }
    @Test void verifiedDownloadIsAtomicReusableAndReportsProgress() throws Exception {
        var service = server.service(directory); AtomicLong progress = new AtomicLong();
        Path file = service.download(asset("/asset"), () -> false, progress::set);
        assertArrayEquals(server.payload, Files.readAllBytes(file)); assertEquals(server.payload.length, progress.get()); service.verifyForInstall(file, asset("/asset"), () -> false);
        assertEquals(file, service.download(asset("/asset"), () -> false, count -> { })); assertEquals(1, server.downloads.get()); noPartFiles();
    }
    @Test void redirectsWorkOnlyForAllowedHosts() throws Exception {
        assertArrayEquals(server.payload, Files.readAllBytes(server.service(directory).download(asset("/redirect"), () -> false, count -> { })));
        Files.delete(directory.resolve(asset("/asset").name()));
        assertEquals(UpdateService.Code.BAD_RELEASE, assertThrows(UpdateService.Failure.class, () -> server.service(directory).download(asset("/blocked"), () -> false, count -> { })).code()); noPartFiles();
    }
    @Test void corruptAndTruncatedInstallersAreNeverPublished() throws Exception {
        for (String endpoint : List.of("/corrupt", "/truncated")) {
            assertThrows(UpdateService.Failure.class, () -> server.service(directory).download(asset(endpoint), () -> false, count -> { }));
            assertFalse(Files.exists(directory.resolve(asset(endpoint).name()))); noPartFiles();
        }
    }
    @Test void tamperingAfterDownloadFailsThePreInstallCheck() throws Exception {
        var service = server.service(directory); Path file = service.download(asset("/asset"), () -> false, count -> { });
        Files.write(file, new byte[server.payload.length]);
        assertEquals(UpdateService.Code.INTEGRITY, assertThrows(UpdateService.Failure.class, () -> service.verifyForInstall(file, asset("/asset"), () -> false)).code());
    }
    @Test void cancellationDisconnectsAndCleansPartialFiles() throws Exception {
        var service = server.service(directory); AtomicBoolean cancelled = new AtomicBoolean(); ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<Path> future = worker.submit(() -> service.download(asset("/stall"), cancelled::get, count -> { }));
            assertTrue(server.stalled.await(3, TimeUnit.SECONDS)); cancelled.set(true); service.cancelCurrentRequest();
            var error = assertThrows(ExecutionException.class, () -> future.get(3, TimeUnit.SECONDS));
            assertEquals(UpdateService.Code.CANCELLED, ((UpdateService.Failure) error.getCause()).code()); noPartFiles();
            assertFalse(Files.exists(directory.resolve(asset("/stall").name())));
        } finally { service.close(); worker.shutdownNow(); }
    }
    @Test void assetPathsCannotEscapeTheUpdateDirectory() throws Exception {
        var unsafe = new UpdateService.Asset("../outside.msi", server.uri("/asset"), server.payload.length, server.digest());
        assertEquals(UpdateService.Code.BAD_RELEASE, assertThrows(UpdateService.Failure.class, () -> server.service(directory).download(unsafe, () -> false, count -> { })).code());
    }
    @Test void productionPoliciesRejectForeignRepositoriesAndInsecureRedirects() {
        assertTrue(UpdateService.repositoryAsset(URI.create("https://github.com/Austin-Sha789/FileFlow/releases/download/v1.1.0/FileFlow.msi")));
        assertFalse(UpdateService.repositoryAsset(URI.create("https://github.com/Other/Repo/releases/download/v1.1.0/FileFlow.msi")));
        assertFalse(UpdateService.trustedGithub(URI.create("http://github.com/Austin-Sha789/FileFlow/releases/download/v1.1.0/a")));
        assertFalse(UpdateService.trustedGithub(URI.create("https://github.com.evil.invalid/a")));
        assertFalse(UpdateService.trustedGithub(URI.create("https://user:pass@release-assets.githubusercontent.com/a")));
    }
}
