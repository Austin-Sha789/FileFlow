# Video thumbnails

Video posters are generated only for visible, settled previews. A dedicated low-priority worker (one concurrent process, queue length 16) runs independently of the two image workers. Offscreen requests cancel, including interrupting a running FFmpeg process. Scans do not generate video posters and no video player is created.

## Development and packaging

After cloning on Windows x64, run `./scripts/setup-video-thumbnails.ps1` once, then build/run normally with Maven. The script verifies the pinned download's SHA-256 and prepares `tools/ffmpeg/windows-x86_64.zip`. This downloaded binary archive is ignored by Git. Maven includes it as `/native/windows-x86_64.zip`, including in the application JAR. This checkout already has the component prepared.

The first video request extracts the engine on the background worker. Normal startup and thumbnail generation do not download anything. Keep the third-party notice/license in distributions. Developers may override the engine using the absolute path in `fileflow.ffmpeg` (JVM property) or `FILEFLOW_FFMPEG` (environment variable). macOS first checks FFmpeg on PATH, then /opt/homebrew/bin, /usr/local/bin and /opt/local/bin (GUI launchers often have a smaller PATH). If FFmpeg is missing or decoding fails, a background /usr/bin/qlmanage request provides a native Quick Look fallback for system-supported movies such as MOV. The native PNG is bounded and resized into the same poster cache. It uses an isolated output directory, an eight-second timeout and cancellation; it opens no preview window and downloads no engine. Quick Look codec support still depends on macOS. Bundled macOS FFmpeg distribution remains a separate task.

## Cache and failure handling

Windows: `%LOCALAPPDATA%/FileFlow/Cache/video-v1`. Mac: `~/Library/Caches/FileFlow/video-v1`. Linux: `$XDG_CACHE_HOME/fileflow/video-v1`, falling back to `~/.cache/fileflow/video-v1`. The JVM property `fileflow.previewCache` selects an isolated test cache.

The cache key includes normalized source path, size, last-modified time and algorithm version. A canonical poster (maximum 512 x 384 pixels) serves list and Quick Info sizes. Source metadata is read on the worker; videos bypass the image loader's path-only memory cache so subsequent requests see changes. An already displayed image updates when refreshed/rebound. Old posters are pruned initially and every 32 generated posters, targeting 128 MiB / 2,000 files (up to 31 posters temporary overshoot).

Each extraction has a ten-second process budget, initially seeks to one second, and retries at the beginning if a short clip produces no frame. Decoder/filter/encoder thread counts are limited to one. Unsupported/corrupt files retain the play placeholder with a bounded 60-second failure backoff. Validated posters are published atomically only if the source revision still matches. File paths are passed as ProcessBuilder arguments, without a shell.

## Verification

`mvn test` runs unit tests. `mvn -Dfileflow.videoTests=true test` additionally tests real MP4/MOV/MKV/AVI/WebM fixtures and the bundled engine. `fileflow.fxTests=true` enables existing JavaFX virtualization regressions. Environments that restrict temporary-file renames should set a writable `java.io.tmpdir` in the Surefire JVM's `argLine`.
