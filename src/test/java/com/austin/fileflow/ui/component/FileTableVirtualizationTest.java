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
