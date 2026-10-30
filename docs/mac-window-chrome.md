# Native macOS window controls

`OceanWindowChrome` selects window decorations by operating system. On macOS it uses `StageStyle.UNIFIED` when supported, falling back to `DECORATED`. The system supplies the top-left traffic lights, window zoom, full screen and frame operations. The app logo and name move to the right end of the application toolbar. Windows retains the left-side brand and three custom caption buttons on the right.

With JavaFX 21, the native titlebar is separate from the application toolbar. Traffic lights sit in the system titlebar; search, workspace access and the brand appear in the toolbar below it. macOS and the JavaFX runtime control native appearance and button states. No internal Cocoa APIs are used.

Mac dialogs also retain native decorations and preserve their title, header, form content, dark client area and modality. The Mac branch does not install custom Windows caption buttons or competing edge-resize filters. Language changes keep the brand on the right.

## Verification

JavaFX integration tests cover both platform policies: stage style, brand order, Windows caption buttons, language changes and original dialog content. Running the injected Mac policy on Windows checks application behaviour, not Cocoa rendering.

The v1.0.0 package startup reports confirm `UNIFIED` on both Mac ARM64 and Mac x64. Manual visual checks are still needed for traffic-light appearance and interaction on users' systems.

To verify on a Mac, exit the previous process, sync the code and run `mvn clean javafx:run`. Check native controls, the right-side brand, window movement/resizing, green-button behaviour, Manage Folders, and confirmation dialogs.

[JavaFX 21 StageStyle documentation](https://openjfx.io/javadoc/21/javafx.graphics/javafx/stage/StageStyle.html)
