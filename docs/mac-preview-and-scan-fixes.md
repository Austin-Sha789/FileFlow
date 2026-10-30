# macOS Dock icon, video previews and temporary scans

## Behaviour

- After the main window opens, `AppIcon.installDockIcon()` sets the process-wide Dock icon through `java.awt.Taskbar`. It uses the original high-resolution PNG and runs on the AWT event thread only when macOS supports `ICON_IMAGE`.
- Video previews prefer FFmpeg. Mac discovery checks an explicit configuration, `PATH`, `/opt/homebrew/bin`, `/usr/local/bin`, and `/opt/local/bin`. GUI launchers do not need to inherit a shell's Homebrew path.
- If the engine is missing or decoding fails, Mac builds try `/usr/bin/qlmanage -t`. The Quick Look fallback uses a private temporary directory and an eight-second timeout, with the existing background queue, cancellation and poster cache. Native output is checked for image format, file size and dimensions, then resized to at most 512 × 384. Unsupported codecs can still leave a placeholder.
- Scanning a folder does not create or persist a workspace shortcut, including when the saved library is empty. Only folders explicitly saved through Manage Folders become sidebar entries. Scan results and scan history remain available.
- Previously saved entries are retained, including entries that older versions added automatically. Unwanted shortcuts can be removed through their context menu without deleting files.

## Verification

Tests cover H.264/HEVC MOV decoding, engine discovery, native fallback and cancellation, poster limits, temporary-file cleanup, and JavaFX workspace behaviour. Windows tests exercise Mac policy through injected dependencies; they do not execute native Mac APIs.

The installer workflow also launches the packaged app on Mac ARM64 and Mac x64. This checks startup, resources and SQLite, but does not establish that every video codec previews correctly. [Issue #1](https://github.com/Austin-Sha789/FileFlow/issues/1) remains open for MOV thumbnails missing on a real Mac.

For a manual check, fully exit the old process, sync the source and run `mvn clean javafx:run`. Inspect the Dock icon, MOV previews in Files and Quick Info, and whether scanning a new folder leaves saved workspace entries unchanged.

If FFmpeg is installed, `mvn -Dfileflow.videoTests=true test` also exercises the native Quick Look path on macOS. FFmpeg is used to generate the test fixture; the Mac-specific extraction test calls Quick Look directly.

## References

- [Java Taskbar API](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/Taskbar.html)
- [Apple Quick Look debugging tools](https://developer.apple.com/library/archive/documentation/UserExperience/Conceptual/Quicklook_Programming_Guide/Articles/QLDebugTest.html)
