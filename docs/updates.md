# Updates and publishing

Settings → Software updates supports manual stable-release checks, installer downloads for the current system and architecture, and installation after an explicit confirmation. Network requests, downloads and integrity checks run on a background worker. Leaving Settings does not interrupt a download.

Downloads do not run automatically. Clicking Install update rechecks the file and asks the OS to open it; FileFlow exits only after that request succeeds. On macOS, opening a DMG mounts the disk image; the user completes the normal app installation steps.

## Release source and version

- The endpoint is fixed to this repository's GitHub Releases `latest` API. Drafts and prereleases are ignored.
- `pom.xml` supplies `project.version`. Maven filters only `app-version.properties`, and the app reads that resource for semantic version comparison.
- A source push does not publish an update. Run **Actions → Release installers → Run workflow** to build and publish a release.
- `.github/workflows/release.yml` calls `scripts/package-release.py` on Windows x64, Mac Apple Silicon and Mac Intel runners. Installers include a Java runtime and native icons.

## Installer names

The version in each filename must match the release tag, such as `v1.1.0`.

| Platform or format | Example |
| --- | --- |
| Windows x64 | `FileFlow-1.1.0-windows-x64.msi` |
| Windows ARM64 | `FileFlow-1.1.0-windows-arm64.msi` |
| Windows EXE installer | `FileFlow-1.1.0-windows-x64-setup.exe` |
| Mac Apple Silicon | `FileFlow-1.1.0-macos-arm64.dmg` |
| Mac Intel | `FileFlow-1.1.0-macos-x64.dmg` |
| Mac universal | `FileFlow-1.1.0-macos-universal.dmg` |

The updater also accepts Mac `.pkg` files. It prefers an exact architecture match, then a Mac universal package. Windows prefers MSI. Portable EXE files, ZIP archives and source bundles are not selected as installers.

The current build workflow produces Windows x64 MSI and separate Mac ARM64/x64 DMGs. Other names in this table describe updater support, not additional published builds.

## Integrity and download storage

Installer URLs must belong to this repository's GitHub release downloads. HTTPS redirects are restricted to GitHub and its download hosts. The app uses the API's `digest` field (`sha256:...`) and expected file size to verify downloads, and verifies the file again before opening it. Assets without SHA-256 metadata are not offered for in-app installation.

Downloads use unique temporary `.part` files and are published to the cache only after validation. Cancellation and failure clean up partial files. A complete cached installer can be reused if it still verifies.

- Windows: `%LOCALAPPDATA%/FileFlow/Updates`
- macOS: `~/Library/Caches/FileFlow/Updates`

The maximum installer size is 2 GiB. Connections and reads have a 15-second timeout; downloads have a 30-minute total limit.

## Publish a new version

1. Set the stable version in `pom.xml`, for example `1.1.0`, and add `docs/releases/1.1.0.md`.
2. Run regression tests, review known issues, and push the source.
3. Open **Actions → Release installers → Run workflow**, then enter the version without the `v` prefix.
4. The workflow tests each target, creates a self-contained application image, launches it for a startup check, and builds the native installer.
5. After all three builds succeed, the publish job verifies the expected assets and checksums. It creates a tag and draft release for the build's source commit, uploads the assets, then makes the release public.
6. Check the published downloads and updater behaviour. Do not replace a public installer with a different file under the same version; publish a new version instead.

Startup checks cover the packaged runtime, JavaFX main window, version resource and SQLite. Manual installation, uninstall and cross-version upgrade checks remain necessary. Native packages must be built on their target operating systems.

## Application data and installer identity

The database is stored outside the installation directory:

- Windows: `%LOCALAPPDATA%/FileFlow/fileflow.db`
- macOS: `~/Library/Application Support/FileFlow/fileflow.db`

When launched from a legacy source directory containing `fileflow.db`, FileFlow uses SQLite to make a consistent copy into the user data directory. The original is retained and an existing destination database is never replaced. An installed app does not search old source directories automatically. For manual migration, exit FileFlow, back up both databases, and copy the legacy file to the appropriate user data directory.

The Windows installer uses a fixed upgrade UUID. The Mac application identifier is `com.austin.fileflow`.

The first release has no developer identity signature. Mac builds have an ad-hoc signature and are not notarised by Apple, so first launch may be restricted by the OS. Developer signing and notarisation require a future certificate setup. The known MOV thumbnail issue is tracked in [Issue #1](https://github.com/Austin-Sha789/FileFlow/issues/1).

## Test coverage

Version, network and JavaFX tests cover architecture matching, no published release, rate limits, timeouts, cancellation, invalid packages, checksum failures, file changes before installation, retained tasks across Settings navigation, and the explicit install action. Tests use a local HTTP fixture and injected installer callbacks; they do not execute a real installer.
