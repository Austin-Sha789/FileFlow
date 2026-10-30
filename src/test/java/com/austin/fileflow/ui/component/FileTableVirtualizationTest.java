package com.austin.fileflow.ui.component;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.FileType;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in desktop integration tests; headless CI can keep running the ordinary unit suite. */
@EnabledIfSystemProperty(named = "fileflow.fxTests", matches = "true")
class FileTableVirtualizationTest {
    private Stage stage;
    private TableView<FileItem> table;
    private AtomicInteger created;
    private static void onFx(Runnable task) throws Exception {
        CompletableFuture<Void> finished = new CompletableFuture<>();
        Platform.runLater(() -> { try { task.run(); finished.complete(null); } catch (Throwable error) { finished.completeExceptionally(error); } });
        finished.get(20, TimeUnit.SECONDS);
    }
    @BeforeAll static void startToolkit() throws Exception {
        CompletableFuture<Void> ready = new CompletableFuture<>();
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.complete(null); });
        ready.get(20, TimeUnit.SECONDS);
    }
    @AfterAll static void stopToolkit() { Platform.exit(); }
    @AfterEach void closeWindow() throws Exception { onFx(() -> { if (stage != null) stage.close(); }); }

    private void create(int count) {
        created = new AtomicInteger();
        table = new TableView<>();
        table.setFixedCellSize(76);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<FileItem, FileItem> column = new TableColumn<>();
        column.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyObjectWrapper<>(value.getValue()));
        column.setCellFactory(value -> { created.incrementAndGet(); return new FileRowCell(new EmptyActions()); });
        table.getColumns().add(column);
        List<FileItem> items = new ArrayList<>();
        LocalDateTime time = LocalDateTime.of(2026, 1, 1, 0, 0);
        for (int i = 0; i < count; i++) items.add(new FileItem(Path.of("fixture", "file-" + i + ".txt"), "file-" + i + ".txt", "txt", 10, time, time, FileType.DOCUMENT, false, false, Set.of()));
        table.getItems().setAll(items);
        stage = new Stage(); stage.setTitle("FileFlow virtualization test");
        stage.setScene(new Scene(new StackPane(table), 1100, 650)); stage.show();
        stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
    }
    private static int countNodes(Node node) {
        int count = 1;
        if (node instanceof Parent parent) for (Node child : parent.getChildrenUnmodifiable()) count += countNodes(child);
        return count;
    }
    @Test void hundredThousandRecordsKeepControlsBoundedDuringAccessibilityQueries() throws Exception {
        onFx(() -> create(100_000));
        onFx(() -> {
            for (int i = 0; i < 100; i++) {
                table.queryAccessibleAttribute(AccessibleAttribute.CHILDREN);
                table.queryAccessibleAttribute(AccessibleAttribute.FOCUS_ITEM);
                table.queryAccessibleAttribute(AccessibleAttribute.SELECTED_ITEMS);
            }
            stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
            assertEquals(100_000, table.getItems().size());
            assertTrue(created.get() < 40, "Offscreen file cells were materialized: " + created.get());
            assertTrue(countNodes(stage.getScene().getRoot()) < 1500, "Scene grew with the record count");
        });
    }
    @Test void lastRecordSelectionAndFilteringKeepRecycledRowsCorrect() throws Exception {
        onFx(() -> create(20_000));
        onFx(() -> { table.getSelectionModel().select(19_999); table.scrollTo(19_999); stage.getScene().getRoot().layout(); });
        onFx(() -> {
            assertEquals("file-19999.txt", table.getSelectionModel().getSelectedItem().getName());
            assertTrue(table.lookupAll(".table-cell").stream().anyMatch(node -> node instanceof FileRowCell cell && cell.getItem() == table.getItems().get(19_999)));
            assertTrue(created.get() < 40);
            FileItem last = table.getItems().get(19_999);
            table.getItems().setAll(last); table.scrollTo(0); stage.getScene().getRoot().layout();
        });
        onFx(() -> {
            assertEquals(1, table.getItems().size());
            assertTrue(table.lookupAll(".table-cell").stream().anyMatch(node -> node instanceof FileRowCell cell && cell.getItem() == table.getItems().get(0)));
            assertTrue(created.get() < 40);
        });
    }
    @Test void videoPostersAreUsedByUnifiedStoredAndQuickInfoPreviews(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        onFx(() -> {
            FilePreviewFactory previews = new FilePreviewFactory();
            try {
                var db = new com.austin.fileflow.database.DatabaseManager("jdbc:sqlite:" + directory.resolve("previews.db"));
                db.initialiseDatabase();
                var view = new com.austin.fileflow.ui.MainView(
                        new com.austin.fileflow.repository.FavouriteRepo(db), new com.austin.fileflow.repository.TagRepo(db),
                        new com.austin.fileflow.repository.ScanHistoryRepo(db), new com.austin.fileflow.service.ShortcutManager(),
                        new com.austin.fileflow.platform.FilePlatformService(), new com.austin.fileflow.service.FileScanService(),
                        new com.austin.fileflow.service.DuplicateService(), new com.austin.fileflow.service.StorageService(), previews);
                Path video = directory.resolve("clip.mp4");
                var time = LocalDateTime.of(2026, 1, 1, 0, 0);
                FileItem item = new FileItem(video, "clip.mp4", "mp4", 10, time, time, FileType.VIDEO, false, false, Set.of());
                var unified = view.getClass().getDeclaredMethod("createUnifiedFilePreview", FileItem.class);
                unified.setAccessible(true);
                assertTrue(((Node) unified.invoke(view, item)).getStyleClass().contains("video-thumbnail"));
                var stored = view.getClass().getDeclaredMethod("createStoredFilePreview", Path.class);
                stored.setAccessible(true);
                assertTrue(((Node) stored.invoke(view, video)).getStyleClass().contains("video-thumbnail"));
                var quick = view.getClass().getDeclaredMethod("createQuickInfoPreview", Path.class, String.class);
                quick.setAccessible(true);
                assertTrue(((Node) quick.invoke(view, video, "Video")).getStyleClass().contains("video-thumbnail"));
            } catch (Exception error) { throw new AssertionError(error); }
            finally { previews.close(); }
        });
    }
    @Test void favouritesThumbnailsLoadAfterScrollingToTheLastRows(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        checkScrolledThumbnails(directory, false);
    }
    @Test void similarImageThumbnailsLoadAfterScrollingPastTheFirstGridRow(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        checkScrolledThumbnails(directory, true);
    }
    private void checkScrolledThumbnails(Path directory, boolean grid) throws Exception {
        Path image = directory.resolve("preview.png");
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(80, 60, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", image.toFile());
        List<ThumbnailView> previews = new ArrayList<>();
        java.util.concurrent.atomic.AtomicReference<ScrollPane> scroll = new java.util.concurrent.atomic.AtomicReference<>();
        try (var loader = new com.austin.fileflow.service.ThumbnailLoader()) {
            onFx(() -> {
                javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(12);
                for (int row = 0; row < 12; row++) {
                    javafx.scene.layout.HBox line = new javafx.scene.layout.HBox(8);
                    for (int column = 0; column < (grid ? 3 : 1); column++) {
                        ThumbnailView preview = new ThumbnailView(loader, image, grid ? 160 : 56, grid ? 110 : 42, "loading");
                        previews.add(preview); line.getChildren().add(preview);
                    }
                    line.setMinHeight(grid ? 140 : 76); content.getChildren().add(line);
                }
                scroll.set(new ScrollPane(content)); scroll.get().setFitToWidth(true);
                stage = new Stage(); stage.setScene(new Scene(new StackPane(scroll.get()), grid ? 550 : 300, 220)); stage.show();
                stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
            });
            awaitPreview(previews.get(0));
            onFx(() -> { scroll.get().setVvalue(1); stage.getScene().getRoot().layout(); });
            awaitPreview(previews.get(previews.size() - 1));
        }
    }
    private void awaitPreview(ThumbnailView preview) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        java.util.concurrent.atomic.AtomicBoolean loaded = new java.util.concurrent.atomic.AtomicBoolean();
        while (!loaded.get() && System.nanoTime() < deadline) {
            onFx(() -> loaded.set(preview.getChildren().stream().anyMatch(node -> node instanceof javafx.scene.image.ImageView image && image.getImage() != null)));
            if (!loaded.get()) Thread.sleep(20);
        }
        assertTrue(loaded.get(), "A visible thumbnail must load after scrolling");
    }
    private static Object viewField(Object view, String name) {
        try { var field = view.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(view); }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private static void invokeView(Object view, String name, Class<?>[] types, Object... arguments) {
        try { var method = view.getClass().getDeclaredMethod(name, types); method.setAccessible(true); method.invoke(view, arguments); }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private com.austin.fileflow.ui.MainView createWorkspaceView(Path directory) {
        try {
            var db = new com.austin.fileflow.database.DatabaseManager("jdbc:sqlite:" + directory.resolve("workspace-ui.db")); db.initialiseDatabase();
            var view = new com.austin.fileflow.ui.MainView(new com.austin.fileflow.repository.FavouriteRepo(db),
                    new com.austin.fileflow.repository.TagRepo(db), new com.austin.fileflow.repository.ScanHistoryRepo(db),
                    new com.austin.fileflow.service.ShortcutManager(), new com.austin.fileflow.platform.FilePlatformService(),
                    new com.austin.fileflow.service.FileScanService(), new com.austin.fileflow.service.DuplicateService(),
                    new com.austin.fileflow.service.StorageService(), new FilePreviewFactory());
            stage = new Stage(); stage.setScene(new Scene(view.getView(), 1280, 800));
            stage.getScene().getStylesheets().addAll(getClass().getResource("/css/app.css").toExternalForm(), getClass().getResource("/css/ocean.css").toExternalForm());
            stage.show(); stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout(); return view;
        } catch (Exception error) { throw new AssertionError(error); }
    }
    private void awaitCondition(java.util.function.BooleanSupplier condition) throws Exception {
        java.util.concurrent.atomic.AtomicBoolean ready = new java.util.concurrent.atomic.AtomicBoolean();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!ready.get() && System.nanoTime() < deadline) { onFx(() -> ready.set(condition.getAsBoolean())); if (!ready.get()) Thread.sleep(20); }
        assertTrue(ready.get(), "Application did not reach the expected state");
    }
    @Test void scansAppendFoldersAndCachedResultsCanBeReopened(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        var preferences = java.util.prefs.Preferences.userNodeForPackage(com.austin.fileflow.service.WorkspaceFoldersStore.class).node("workspace-folders");
        preferences.clear();
        Path first = java.nio.file.Files.createDirectory(directory.resolve("First"));
        Path second = java.nio.file.Files.createDirectory(directory.resolve("Second"));
        java.nio.file.Files.writeString(first.resolve("one.txt"), "one"); java.nio.file.Files.writeString(second.resolve("two.txt"), "two");
        java.util.concurrent.atomic.AtomicReference<com.austin.fileflow.ui.MainView> view = new java.util.concurrent.atomic.AtomicReference<>();
        try {
            onFx(() -> { view.set(createWorkspaceView(directory)); invokeView(view.get(), "startScan", new Class<?>[]{Path.class}, first); });
            awaitCondition(() -> first.equals(viewField(view.get(), "scanRootPath")) && Boolean.FALSE.equals(viewField(view.get(), "scanning")));
            onFx(() -> invokeView(view.get(), "startScan", new Class<?>[]{Path.class}, second));
            awaitCondition(() -> second.equals(viewField(view.get(), "scanRootPath")) && Boolean.FALSE.equals(viewField(view.get(), "scanning")));
            onFx(() -> {
                var snapshot = (com.austin.fileflow.model.WorkspaceScanSnapshot) viewField(view.get(), "lastWorkspaceScanSnapshot");
                assertEquals(2, snapshot.completedFolderCount());
                assertTrue(snapshot.resultsByRoot().containsKey(first)); assertTrue(snapshot.resultsByRoot().containsKey(second));
                var folders = (com.austin.fileflow.service.ScanWorkspace) viewField(view.get(), "scanWorkspace");
                assertEquals(List.of(first, second), folders.getRoots());
                Button shortcut = (Button) stage.getScene().getRoot().lookupAll(".ocean-folder-shortcut").stream()
                        .filter(node -> node instanceof Button button && button.getText().equals("First")).findFirst().orElseThrow();
                shortcut.fire();
                assertEquals(first, viewField(view.get(), "scanRootPath")); assertEquals(false, viewField(view.get(), "scanning"));
                var result = (com.austin.fileflow.model.ScanResult) viewField(view.get(), "latestScanResult");
                assertTrue(result.getItems().stream().anyMatch(item -> item.getName().equals("one.txt")));
                stage.close(); view.set(createWorkspaceView(directory));
                assertEquals(List.of(first, second), ((com.austin.fileflow.service.ScanWorkspace) viewField(view.get(), "scanWorkspace")).getRoots());
            });
        } finally { preferences.clear(); }
    }
    @Test void onlyTheFolderAreaScrollsAndDotsNavigateTheWholeLibrary(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        java.util.concurrent.atomic.AtomicReference<com.austin.fileflow.ui.MainView> view = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<ScrollPane> scroll = new java.util.concurrent.atomic.AtomicReference<>();
        double[] navigationY = new double[1];
        onFx(() -> {
            view.set(createWorkspaceView(directory));
            var folders = (com.austin.fileflow.service.ScanWorkspace) viewField(view.get(), "scanWorkspace"); folders.clear();
            for (int index = 0; index < 30; index++) folders.addRoot(directory.resolve("Folder-" + index));
            invokeView(view.get(), "updateOceanStatus", new Class<?>[]{});
            var root = (javafx.scene.layout.BorderPane) view.get().getView(); root.applyCss(); root.layout();
            assertTrue(root.getLeft() instanceof javafx.scene.layout.VBox);
            scroll.set((ScrollPane) root.lookup(".workspace-folder-scroll"));
            assertEquals(ScrollPane.ScrollBarPolicy.NEVER, scroll.get().getVbarPolicy());
            assertEquals(30, root.lookupAll(".workspace-folder-dot").size());
            Node navigation = root.lookup(".nav-item"); navigationY[0] = navigation.localToScene(navigation.getBoundsInLocal()).getMinY();
            scroll.get().setVvalue(1); root.layout();
            assertEquals(navigationY[0], navigation.localToScene(navigation.getBoundsInLocal()).getMinY());
            assertTrue(scroll.get().getContent().getBoundsInLocal().getHeight() > scroll.get().getViewportBounds().getHeight());
            scroll.get().setVvalue(0);
            var dots = new ArrayList<>(root.lookupAll(".workspace-folder-dot"));
            dots.sort(java.util.Comparator.comparingDouble(node -> ((javafx.scene.shape.Circle) node).getCenterY()));
            Node last = dots.get(dots.size() - 1);
            last.fireEvent(new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                    javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, true,
                    new javafx.scene.input.PickResult(last, 0, 0)));
        });
        awaitCondition(() -> scroll.get().getVvalue() > .95);
        onFx(() -> {
            scroll.get().setVvalue(0);
            scroll.get().fireEvent(new javafx.scene.input.ScrollEvent(javafx.scene.input.ScrollEvent.SCROLL,
                    0, 0, 0, 0, false, false, false, false, false, false, 0, -120, 0, -120,
                    javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    javafx.scene.input.ScrollEvent.VerticalTextScrollUnits.LINES, -3, 0, null));
        });
        awaitCondition(() -> scroll.get().getVvalue() > 0);
        onFx(() -> {
            Node navigation = view.get().getView().lookup(".nav-item");
            assertEquals(navigationY[0], navigation.localToScene(navigation.getBoundsInLocal()).getMinY());
            stage.setHeight(680);
        });
        awaitCondition(() -> {
            view.get().getView().applyCss(); view.get().getView().requestLayout();
            return scroll.get().getViewportBounds().getHeight() >= 64;
        });
        onFx(() -> {
            var root = (javafx.scene.layout.BorderPane) view.get().getView(); root.applyCss(); root.layout();
            assertTrue(scroll.get().getViewportBounds().getHeight() >= 64, "The folder area must remain usable at minimum window height");
            double before = scroll.get().getHeight();
            ((Button) root.lookup(".sidebar-storage-volumes-button")).fire(); root.applyCss(); root.layout();
            assertEquals(before, scroll.get().getHeight(), 1, "Disk selection must not collapse the folder list");
            for (var window : new ArrayList<>(javafx.stage.Window.getWindows())) if (window instanceof javafx.stage.PopupWindow) window.hide();
        });
    }
    @Test void duplicateHoverKeepsADarkSurface() throws Exception {
        onFx(() -> {
            javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(new Label("Duplicate group")); header.getStyleClass().add("duplicate-group-header");
            javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(header); card.getStyleClass().add("exact-duplicate-group-card");
            StackPane root = new StackPane(card); root.getStyleClass().add("ocean-app"); stage = new Stage(); stage.setScene(new Scene(root, 600, 200));
            stage.getScene().getStylesheets().addAll(getClass().getResource("/css/app.css").toExternalForm(), getClass().getResource("/css/ocean.css").toExternalForm());
            stage.show(); header.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("hover"), true); root.applyCss(); root.layout();
            javafx.scene.paint.Color color = (javafx.scene.paint.Color) header.getBackground().getFills().get(0).getFill();
            assertTrue(color.getBrightness() < .5, "Duplicate hover must not inherit the light theme");
        });
    }
    @Test void captionButtonsReleaseTheirStateAfterMinimizeAndRestore() throws Exception {
        java.util.concurrent.atomic.AtomicReference<Button> minimize = new java.util.concurrent.atomic.AtomicReference<>();
        onFx(() -> {
            javafx.scene.layout.HBox bar = new javafx.scene.layout.HBox(); bar.getStyleClass().add("ff-command-bar");
            javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane(); root.setTop(bar); root.getStyleClass().add("ocean-app");
            stage = new Stage(); Scene scene = new Scene(root, 700, 300); scene.getStylesheets().add(getClass().getResource("/css/ocean.css").toExternalForm());
            com.austin.fileflow.ui.OceanWindowChrome.install(stage, scene); stage.setScene(scene); stage.show();
            minimize.set((Button) root.lookup(".ocean-caption-button")); minimize.get().fire();
        });
        onFx(() -> { stage.setIconified(false); stage.requestFocus(); });
        onFx(() -> {
            assertFalse(minimize.get().isFocusTraversable()); assertFalse(minimize.get().isArmed());
            assertFalse(minimize.get().isPressed()); assertFalse(minimize.get().isHover());
        });
    }
    @Test void comboBoxPopupStatesHaveDarkBackgroundsAndReadableText() throws Exception {
        List<ComboBox<?>> boxes = new ArrayList<>();
        onFx(() -> {
            ComboBox<String> choices = new ComboBox<>(); choices.getItems().addAll("Images", "Videos", "Documents");
            com.austin.fileflow.i18n.I18n.localizeChoices(choices); choices.getSelectionModel().selectFirst();
            ComboBox<Path> paths = new ComboBox<>(); paths.getItems().addAll(Path.of("Photos"), Path.of("Projects")); paths.getSelectionModel().selectFirst();
            boxes.add(choices); boxes.add(paths);
            javafx.scene.layout.HBox root = new javafx.scene.layout.HBox(choices, paths); root.getStyleClass().add("ocean-app");
            stage = new Stage(); Scene scene = new Scene(root, 650, 300);
            scene.getStylesheets().addAll(getClass().getResource("/css/app.css").toExternalForm(), getClass().getResource("/css/ocean.css").toExternalForm());
            stage.setScene(scene); stage.show();
        });
        for (ComboBox<?> box : boxes) {
            onFx(box::show);
            onFx(() -> {
                int checked = 0;
                for (var window : new ArrayList<>(javafx.stage.Window.getWindows())) {
                    if (!(window instanceof javafx.stage.PopupWindow) || !window.isShowing()) continue;
                    var root = window.getScene().getRoot(); root.applyCss(); root.layout();
                    for (Node node : root.lookupAll(".list-cell")) if (node instanceof ListCell<?> cell && !cell.isEmpty()) {
                        for (String state : List.of("hover", "selected", "focused")) {
                            cell.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass(state), true); root.applyCss();
                            var color = (javafx.scene.paint.Color) cell.getBackground().getFills().get(0).getFill();
                            assertTrue(color.getBrightness() < .9, "Light popup background in " + state);
                            assertTrue(((javafx.scene.paint.Color) cell.getTextFill()).getBrightness() > .7, "Unreadable popup text");
                            cell.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass(state), false); checked++;
                        }
                    }
                }
                assertTrue(checked >= 3); box.hide();
            });
        }
    }
    @Test void workspaceRemovalKeepsFilesOnDiskAndOtherSavedFolders(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        Path first = java.nio.file.Files.createDirectory(directory.resolve("RemoveThisShortcut"));
        Path other = java.nio.file.Files.createDirectory(directory.resolve("KeepThisShortcut"));
        Path file = java.nio.file.Files.writeString(first.resolve("keep.txt"), "keep my file");
        onFx(() -> {
            var view = createWorkspaceView(directory);
            var folders = (com.austin.fileflow.service.ScanWorkspace) viewField(view, "scanWorkspace"); folders.clear(); folders.addRoot(first); folders.addRoot(other);
            invokeView(view, "updateOceanStatus", new Class<?>[]{});
            Button shortcut = (Button) view.getView().lookupAll(".ocean-folder-shortcut").stream().filter(node -> node instanceof Button button && button.getText().equals("RemoveThisShortcut")).findFirst().orElseThrow();
            assertNotNull(shortcut.getOnContextMenuRequested());
            invokeView(view, "removeWorkspaceFolder", new Class<?>[]{Path.class}, first);
            assertEquals(List.of(other), folders.getRoots()); assertTrue(java.nio.file.Files.exists(file));
            assertEquals(List.of(other), new com.austin.fileflow.service.WorkspaceFoldersStore().load());
            try { java.util.prefs.Preferences.userNodeForPackage(com.austin.fileflow.service.WorkspaceFoldersStore.class).node("workspace-folders").clear(); }
            catch (Exception error) { throw new AssertionError(error); }
        });
    }
    @Test void diskCardPanelScrollsAndTogglesWithoutChangingTheSidebar(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        onFx(() -> {
            var view = createWorkspaceView(directory);
            Button anchor = (Button) view.getView().lookup(".sidebar-storage-volumes-button");
            java.util.concurrent.atomic.AtomicReference<com.austin.fileflow.model.StorageVolume> selected = new java.util.concurrent.atomic.AtomicReference<>();
            List<com.austin.fileflow.model.StorageVolume> volumes = new ArrayList<>();
            for (int i = 0; i < 20; i++) volumes.add(new com.austin.fileflow.model.StorageVolume(directory.resolve("drive-" + i), "Drive " + i, 1000, 400));
            var picker = new VolumePicker(anchor, volume -> {
                try { var method = view.getClass().getDeclaredMethod("createStorageVolumeCard", com.austin.fileflow.model.StorageVolume.class); method.setAccessible(true); return (javafx.scene.layout.VBox) method.invoke(view, volume); }
                catch (Exception error) { throw new AssertionError(error); }
            });
            try {
                picker.toggle(volumes, volumes.get(0).getRootPath(), selected::set);
                assertTrue(picker.isShowing()); var root = picker.getScene().getRoot(); root.applyCss(); root.layout();
                assertEquals(20, root.lookupAll(".sidebar-storage-volume-card").size());
                ScrollPane scroll = (ScrollPane) root.lookup(".storage-volume-picker-scroll");
                assertTrue(scroll.getHeight() <= 420); assertTrue(scroll.getContent().getLayoutBounds().getHeight() > scroll.getViewportBounds().getHeight());
                picker.toggle(volumes, null, selected::set); assertFalse(picker.isShowing());
                picker.toggle(volumes, null, selected::set);
                Node card = picker.getScene().getRoot().lookup(".sidebar-storage-volume-card");
                card.fireEvent(new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", javafx.scene.input.KeyCode.ENTER, false, false, false, false));
                assertNotNull(selected.get()); assertFalse(picker.isShowing());
            } finally { picker.hide(); }
        });
    }
    @Test void semanticChartColorsMatchTypesWhenSliceOrderChanges() throws Exception {
        onFx(() -> {
            var first = com.austin.fileflow.ui.OceanComponents.donut(java.util.Map.of(FileType.IMAGE, 100L, FileType.DOCUMENT, 10L), "110 B");
            var second = com.austin.fileflow.ui.OceanComponents.donut(java.util.Map.of(FileType.IMAGE, 10L, FileType.DOCUMENT, 100L), "110 B");
            javafx.scene.layout.HBox root = new javafx.scene.layout.HBox(first, second); root.getStyleClass().add("ocean-app");
            stage = new Stage(); stage.setScene(new Scene(root, 600, 300)); stage.getScene().getStylesheets().add(getClass().getResource("/css/ocean.css").toExternalForm()); stage.show(); root.applyCss(); root.layout();
            assertEquals(FileType.values().length, java.util.Arrays.stream(FileType.values()).map(com.austin.fileflow.ui.OceanComponents::typeColor).distinct().count());
            java.util.Map<String, javafx.scene.paint.Paint> paints = new java.util.HashMap<>();
            for (Node node : root.lookupAll(".ocean-donut")) {
                var chart = (javafx.scene.chart.PieChart) node;
                for (var data : chart.getData()) {
                    var paint = ((javafx.scene.layout.Region) data.getNode()).getBackground().getFills().get(0).getFill();
                    var previous = paints.putIfAbsent(data.getName(), paint); if (previous != null) assertEquals(previous, paint);
                    javafx.scene.paint.Color sample = paint instanceof javafx.scene.paint.Color color ? color : ((javafx.scene.paint.RadialGradient) paint).getStops().get(0).getColor();
                    assertEquals(javafx.scene.paint.Color.web(com.austin.fileflow.ui.OceanComponents.typeColor(FileType.valueOf(data.getName()))).getHue(), sample.getHue(), .5);
                }
            }
            assertEquals(2, paints.values().stream().distinct().count());
        });
    }
    @Test void folderManagerShowsSeveralCompactDarkRows(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        onFx(() -> {
            var view = createWorkspaceView(directory);
            var folders = (com.austin.fileflow.service.ScanWorkspace) viewField(view, "scanWorkspace"); folders.clear();
            for (int i = 0; i < 8; i++) folders.addRoot(directory.resolve("Folder-" + i));
            java.util.concurrent.atomic.AtomicReference<Throwable> problem = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicBoolean found = new java.util.concurrent.atomic.AtomicBoolean();
            Platform.runLater(() -> {
                for (var window : new ArrayList<>(javafx.stage.Window.getWindows())) {
                    if (window == stage || !window.isShowing() || window.getScene() == null) continue;
                    var root = window.getScene().getRoot();
                    if (root.lookup(".workspace-roots-scroll") == null) continue;
                    try {
                        root.applyCss(); root.layout();
                        ScrollPane scroll = (ScrollPane) root.lookup(".workspace-roots-scroll");
                        assertTrue(scroll.getViewportBounds().getHeight() >= 240);
                        assertEquals(8, root.lookupAll(".workspace-folder-row").size());
                        for (Node node : root.lookupAll(".workspace-folder-row")) {
                            var row = (javafx.scene.layout.Region) node;
                            assertTrue(row.getHeight() <= 56);
                            assertTrue(((javafx.scene.paint.Color) row.getBackground().getFills().get(0).getFill()).getBrightness() < .5);
                            Button remove = (Button) row.lookup(".button"); assertTrue(remove.getWidth() >= remove.prefWidth(-1) - 1);
                        }
                        found.set(true);
                    } catch (Throwable error) { problem.set(error); }
                    finally { window.hide(); }
                }
            });
            invokeView(view, "showScanWorkspaceDialog", new Class<?>[]{});
            if (problem.get() != null) throw new AssertionError(problem.get());
            assertTrue(found.get());
        });
    }
    @Test void textInputAndChoiceDialogPromptsDoNotInheritLightPanels(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        onFx(() -> {
            var view = createWorkspaceView(directory);
            for (Dialog<?> dialog : List.of(new TextInputDialog("Example"), new ChoiceDialog<String>("One", List.of("One", "Two")))) {
                dialog.setContentText("Folder name");
                java.util.concurrent.atomic.AtomicReference<Throwable> problem = new java.util.concurrent.atomic.AtomicReference<>();
                Platform.runLater(() -> {
                    try {
                        var root = dialog.getDialogPane(); root.applyCss(); root.layout();
                        int checked = 0;
                        for (Node node : root.lookupAll(".content")) if (node instanceof Label label) {
                            if (label.getBackground() != null) for (var fill : label.getBackground().getFills()) {
                                if (fill.getFill() instanceof javafx.scene.paint.Color color) assertTrue(color.getOpacity() < .9 || color.getBrightness() < .6);
                            }
                            assertTrue(((javafx.scene.paint.Color) label.getTextFill()).getBrightness() > .7); checked++;
                        }
                        assertTrue(checked > 0);
                    } catch (Throwable error) { problem.set(error); }
                    finally { dialog.close(); }
                });
                invokeView(view, "showOceanDialog", new Class<?>[]{Dialog.class}, dialog);
                if (problem.get() != null) throw new AssertionError(problem.get());
            }
        });
    }
    private static final class EmptyActions implements FileRowCell.Actions {
        public Node preview(FileItem item) { return new Label("TXT"); }
        public String location(FileItem item) { return "fixture"; }
        public boolean showLocation() { return false; }
        public boolean favourite(Path path) { return false; }
        public void toggleFavourite(FileItem item) { }
        public List<String> tags(FileItem item) { return List.of(); }
        public void addTag(FileItem item) { }
        public void removeTag(FileItem item, String tag) { }
        public boolean selectionMode() { return false; }
        public boolean selected(Path path) { return false; }
        public void select(Path path, boolean selected) { }
        public void animateSelection(CheckBox checkbox) { }
        public void activate(FileItem item) { }
        public ContextMenu menu(Path path) { return new ContextMenu(); }
    }
}
