package com.austin.fileflow.ui.component;

import static com.austin.fileflow.i18n.I18n.tr;

import com.austin.fileflow.service.ThumbnailLoader;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Loads only a settled, visible preview; detached/recycled views release their request. */
public final class ThumbnailView extends StackPane {
    private final ThumbnailLoader loader;
    private final Path path;
    private final int pixelWidth, pixelHeight;
    private final ImageView image = new ImageView();
    private final Label placeholder;
    private final PauseTransition settle = new PauseTransition(Duration.millis(100));
    private CompletableFuture<ThumbnailLoader.Thumbnail> request;
    private boolean failed;
    private java.util.List<ThumbnailViewportTracker> viewportTrackers = java.util.List.of();
    private final boolean retainFallbackOnFailure;

    public ThumbnailView(ThumbnailLoader loader, Path path, int width, int height, String fallback) {
        this(loader, path, width, height, fallback, false);
    }

    public ThumbnailView(ThumbnailLoader loader, Path path, int width, int height, String fallback, boolean retainFallbackOnFailure) {
        this.retainFallbackOnFailure = retainFallbackOnFailure;
        if (retainFallbackOnFailure) settle.setDuration(Duration.millis(220));
        this.loader = loader; this.path = path;
        pixelWidth = width * 2; pixelHeight = height * 2;
        setMinSize(width, height); setPrefSize(width, height); setMaxSize(width, height);
        getStyleClass().addAll("file-preview-image", "async-thumbnail");
        placeholder = new Label(fallback);
        placeholder.getStyleClass().add("file-preview-placeholder");
        image.setFitWidth(width); image.setFitHeight(height);
        image.setPreserveRatio(true); image.setSmooth(true);
        image.setMouseTransparent(true);
        getChildren().addAll(placeholder, image);
        settle.setOnFinished(event -> load());
        sceneProperty().addListener((observable, before, after) -> {
            detachViewportTrackers();
            if (after == null) {
                release(); image.setImage(null); placeholder.setVisible(true); failed = false;
            } else {
                viewportTrackers = ThumbnailViewportTracker.attach(this);
                schedule();
            }
        });
        localToSceneTransformProperty().addListener(observable -> viewportChanged());
        parentProperty().addListener((observable, before, after) -> {
            detachViewportTrackers();
            if (getScene() != null) viewportTrackers = ThumbnailViewportTracker.attach(this);
        });
        visibleProperty().addListener(observable -> { if (!isVisible()) release(); else schedule(); });
    }

    private void detachViewportTrackers() {
        for (var tracker : viewportTrackers) tracker.detach(this);
        viewportTrackers = java.util.List.of();
    }

    void viewportChanged() { if (!inViewport()) release(); else schedule(); }

    private boolean inViewport() {
        if (getScene() == null || getScene().getWindow() == null || !getScene().getWindow().isShowing()) return false;
        var bounds = localToScene(getBoundsInLocal());
        for (Node parent = this; parent != null; parent = parent.getParent()) {
            if (!parent.isVisible()) return false;
            if ((parent instanceof ScrollPane || parent instanceof ListView<?> || parent instanceof TableView<?>)
                    && !bounds.intersects(parent.localToScene(parent.getBoundsInLocal()))) return false;
        }
        return true;
    }

    private void schedule() {
        if (!failed && image.getImage() == null && request == null && inViewport()
                && settle.getStatus() != Animation.Status.RUNNING) settle.playFromStart();
    }

    @Override protected void layoutChildren() { super.layoutChildren(); schedule(); }

    private void release() {
        settle.stop();
        image.setImage(null); placeholder.setVisible(true);
        var previous = request; request = null;
        if (previous != null) previous.cancel(false);
    }

    private void load() {
        if (!inViewport() || request != null || image.getImage() != null) return;
        var future = loader.load(path, pixelWidth, pixelHeight);
        request = future;
        future.whenComplete((thumbnail, error) -> {
            Runnable apply = () -> {
                if (request != future) return;
                request = null;
                if (!inViewport()) return;
                if (error != null) {
                    if (error instanceof RejectedExecutionException || future.isCancelled()) {
                        settle.setDuration(Duration.millis(180)); settle.playFromStart();
                    } else { failed = true; if (!retainFallbackOnFailure) placeholder.setText(tr("ui.preview_unavailable")); }
                    return;
                }
                WritableImage pixels = new WritableImage(thumbnail.width(), thumbnail.height());
                pixels.getPixelWriter().setPixels(0, 0, thumbnail.width(), thumbnail.height(),
                        PixelFormat.getIntArgbInstance(), thumbnail.pixels(), 0, thumbnail.width());
                image.setImage(pixels);
                placeholder.setVisible(false);
            };
            if (Platform.isFxApplicationThread()) apply.run(); else Platform.runLater(apply);
        });
    }
}
