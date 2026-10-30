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
