package com.austin.fileflow.ui.component;

import static com.austin.fileflow.i18n.I18n.tr;
import com.austin.fileflow.i18n.I18n;
import com.austin.fileflow.ui.OceanComponents;
import com.austin.fileflow.update.*;
import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Retained for the MainView lifetime so leaving Settings does not interrupt a download. */
public final class UpdateSettingsCard extends VBox implements AutoCloseable {
    private enum Phase { IDLE, CHECKING, CURRENT, NO_RELEASE, AVAILABLE, NO_PACKAGE, UNVERIFIED, DOWNLOADING, READY, CANCELLED, ERROR, LAUNCHING }
    private final UpdateService service;
    private final String currentVersion;
    private final Consumer<Path> installer;
    private final Consumer<URI> browser;
    private final Runnable quit;
    private final Function<Dialog<ButtonType>, Optional<ButtonType>> confirmation;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "fileflow-updates"); thread.setDaemon(true); return thread;
    });
    private final Label version = new Label(), status = new Label(), percent = new Label(), saved = new Label();
    private final Button check = button("updates.check", "primary-button"), download = button("updates.download", "secondary-button"),
            cancel = button("common.cancel", "secondary-button"), install = button("updates.install", "primary-button");
    private final ProgressBar progress = new ProgressBar();
    private final TextArea notes = new TextArea();
    private final Hyperlink releases = new Hyperlink();
    private final ChangeListener<com.austin.fileflow.i18n.AppLanguage> languageChanged = (observable, before, after) -> render();
    private Task<?> activeTask;
    private UpdateService.Check update;
    private Path downloaded;
    private Phase phase = Phase.IDLE;
    private String errorKey;
    private boolean closed;

    public UpdateSettingsCard(Function<Dialog<ButtonType>, Optional<ButtonType>> confirmation, Runnable quit) {
        this(new UpdateService(), BuildInfo.VERSION, file -> {
            try { if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) throw new IOException(); Desktop.getDesktop().open(file.toFile()); }
            catch (IOException | UnsupportedOperationException failure) { throw new IllegalStateException(failure); }
        }, uri -> {
            try { if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) throw new IOException(); Desktop.getDesktop().browse(uri); }
            catch (IOException | UnsupportedOperationException failure) { throw new IllegalStateException(failure); }
        }, quit, confirmation);
    }
    public UpdateSettingsCard(UpdateService service, String currentVersion, Consumer<Path> installer, Consumer<URI> browser,
            Runnable quit, Function<Dialog<ButtonType>, Optional<ButtonType>> confirmation) {
        super(12); this.service = service; this.currentVersion = currentVersion; this.installer = installer;
        this.browser = browser; this.quit = quit; this.confirmation = confirmation;
        setId("update-settings-card"); getStyleClass().addAll("settings-card", "update-settings-card");
        Label title = OceanComponents.label("", "settings-section-title"); title.textProperty().bind(I18n.binding("updates.title"));
        Label description = OceanComponents.label("", "settings-description"); description.textProperty().bind(I18n.binding("updates.description")); description.setWrapText(true);
        version.getStyleClass().add("settings-action-title"); status.getStyleClass().add("settings-description"); status.setId("updates-status"); status.setWrapText(true);
        notes.setId("updates-notes"); notes.setEditable(false); notes.setWrapText(true); notes.setPrefRowCount(4); notes.setMaxHeight(140); notes.getStyleClass().add("update-notes");
        saved.setWrapText(true); saved.getStyleClass().add("settings-description");
        check.setId("updates-check"); download.setId("updates-download"); cancel.setId("updates-cancel"); install.setId("updates-install");
        HBox actions = new HBox(10, check, download, install, cancel); actions.setAlignment(Pos.CENTER_LEFT);
        HBox progressRow = new HBox(10, progress, percent); HBox.setHgrow(progress, Priority.ALWAYS); progress.setMaxWidth(Double.MAX_VALUE);
        progressRow.visibleProperty().bind(progress.visibleProperty()); progressRow.managedProperty().bind(progress.visibleProperty());
        releases.textProperty().bind(I18n.binding("updates.releases")); releases.getStyleClass().add("update-release-link");
        getChildren().addAll(title, version, description, status, actions, progressRow, notes, saved, releases);
        check.setOnAction(event -> checkUpdates()); download.setOnAction(event -> downloadUpdate()); cancel.setOnAction(event -> cancel()); install.setOnAction(event -> installUpdate());
        releases.setOnAction(event -> {
            URI page = update != null && update.release() != null ? update.release().page() : UpdateService.RELEASES;
            CompletableFuture.runAsync(() -> {
                try { browser.accept(page); }
                catch (RuntimeException failure) { Platform.runLater(() -> {
                    if (closed) return;
                    if (busy()) status.setText(tr("updates.browser_failed"));
                    else { errorKey = "updates.browser_failed"; phase = Phase.ERROR; render(); }
                }); }
            });
        });
        I18n.instance().languageProperty().addListener(languageChanged); render();
    }
    private static Button button(String key, String style) {
        Button button = new Button(); button.textProperty().bind(I18n.binding(key)); button.getStyleClass().add(style); return button;
    }
    private boolean busy() { return phase == Phase.CHECKING || phase == Phase.DOWNLOADING || phase == Phase.LAUNCHING; }
    private static void show(javafx.scene.Node node, boolean show) { node.setVisible(show); node.setManaged(show); }
    private void render() {
        version.setText(tr("updates.current", currentVersion));
        String key = switch (phase) {
            case IDLE -> "updates.idle"; case CHECKING -> "updates.checking"; case CURRENT -> "updates.current_status";
            case NO_RELEASE -> "updates.no_release"; case AVAILABLE -> "updates.available"; case NO_PACKAGE -> "updates.no_package";
            case UNVERIFIED -> "updates.unverified"; case DOWNLOADING -> "updates.downloading"; case READY -> "updates.ready";
            case CANCELLED -> "updates.cancelled"; case ERROR -> errorKey == null ? "updates.network_failed" : errorKey; case LAUNCHING -> "updates.launching";
        };
        status.setText(phase == Phase.AVAILABLE ? tr(key, update.release().tag()) : tr(key));
        check.setDisable(busy());
        boolean compatible = update != null && update.status() == UpdateService.Status.AVAILABLE && update.asset() != null;
        show(download, compatible && downloaded == null && !busy()); show(install, downloaded != null && !busy());
        show(cancel, phase == Phase.CHECKING || phase == Phase.DOWNLOADING); show(progress, phase == Phase.DOWNLOADING);
        boolean hasNotes = update != null && update.release() != null && !update.release().notes().isBlank();
        show(notes, hasNotes); if (hasNotes && !notes.getText().equals(update.release().notes())) notes.setText(update.release().notes());
        show(saved, downloaded != null); if (downloaded != null) saved.setText(tr("updates.saved", downloaded));
    }
    public void checkUpdates() {
        if (closed || busy()) return;
        update = null; downloaded = null; errorKey = null; phase = Phase.CHECKING; render();
        Task<UpdateService.Check> task = new Task<>() { @Override protected UpdateService.Check call() throws Exception { return service.check(currentVersion, UpdateService.Platform.current(), this::isCancelled); } };
        task.setOnSucceeded(event -> {
            if (closed || activeTask != task) return;
            activeTask = null; update = task.getValue(); phase = switch (update.status()) {
                case NO_RELEASE -> Phase.NO_RELEASE; case CURRENT -> Phase.CURRENT; case AVAILABLE -> Phase.AVAILABLE;
                case NO_PACKAGE -> Phase.NO_PACKAGE; case UNVERIFIED -> Phase.UNVERIFIED;
            }; render();
        });
        handlers(task, false); activeTask = task; worker.execute(task);
    }
    public void downloadUpdate() {
        if (closed || busy() || update == null || update.asset() == null) return;
        UpdateService.Asset asset = update.asset(); phase = Phase.DOWNLOADING; errorKey = null;
        Task<Path> task = new Task<>() {
            @Override protected Path call() throws Exception {
                updateProgress(0, asset.size());
                return service.download(asset, this::isCancelled, count -> updateProgress(count, asset.size()));
            }
        };
        progress.progressProperty().unbind(); progress.progressProperty().bind(task.progressProperty());
        percent.textProperty().unbind(); percent.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> String.format(Locale.ROOT, "%.0f%%", Math.max(0, task.getProgress()) * 100), task.progressProperty()));
        task.setOnSucceeded(event -> { if (closed || activeTask != task) return; activeTask = null; downloaded = task.getValue(); phase = Phase.READY; render(); });
        handlers(task, false); activeTask = task; render(); worker.execute(task);
    }
    public void cancel() { if (activeTask != null) { activeTask.cancel(true); service.cancelCurrentRequest(); } }
    private void handlers(Task<?> task, boolean launching) {
        task.setOnCancelled(event -> { if (!closed && activeTask == task) { activeTask = null; phase = Phase.CANCELLED; render(); } });
        task.setOnFailed(event -> {
            if (closed || activeTask != task) return; activeTask = null;
            UpdateService.Code code = task.getException() instanceof UpdateService.Failure failure ? failure.code() : UpdateService.Code.NETWORK;
            errorKey = switch (code) {
                case RATE_LIMIT -> "updates.rate_limit"; case BAD_RELEASE -> "updates.invalid_release"; case INTEGRITY -> "updates.integrity_failed";
                case DOWNLOAD -> "updates.download_failed"; case OPEN -> "updates.open_failed"; case CANCELLED -> "updates.cancelled";
                default -> launching ? "updates.open_failed" : "updates.network_failed";
            };
            if (code == UpdateService.Code.INTEGRITY) downloaded = null;
            phase = Phase.ERROR; render();
        });
    }
    public void installUpdate() {
        if (closed || busy() || downloaded == null || update == null || update.asset() == null) return;
        ButtonType proceed = new ButtonType(tr("updates.install"), ButtonBar.ButtonData.OK_DONE);
        Alert prompt = new Alert(Alert.AlertType.CONFIRMATION, tr("updates.confirm_body"), ButtonType.CANCEL, proceed);
        prompt.setTitle(tr("updates.title")); prompt.setHeaderText(tr("updates.confirm_title"));
        if (confirmation.apply(prompt).orElse(ButtonType.CANCEL) != proceed) return;
        Path file = downloaded; UpdateService.Asset asset = update.asset(); phase = Phase.LAUNCHING; render();
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.verifyForInstall(file, asset, this::isCancelled);
                if (isCancelled()) throw new UpdateService.Failure(UpdateService.Code.CANCELLED);
                try { installer.accept(file); } catch (RuntimeException failure) { throw new UpdateService.Failure(UpdateService.Code.OPEN); }
                return null;
            }
        };
        task.setOnSucceeded(event -> { if (closed || activeTask != task) return; activeTask = null; phase = Phase.READY; render(); quit.run(); });
        handlers(task, true); activeTask = task; worker.execute(task);
    }
    @Override public void close() {
        closed = true; if (activeTask != null) activeTask.cancel(true); service.close(); worker.shutdownNow();
        I18n.instance().languageProperty().removeListener(languageChanged);
    }
}
