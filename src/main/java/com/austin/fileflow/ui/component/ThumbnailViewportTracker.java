package com.austin.fileflow.ui.component;

import java.lang.ref.WeakReference;
import java.util.*;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/** One coalesced viewport check per scroll container, including nonvirtualized image grids. */
final class ThumbnailViewportTracker {
    private static final Map<ScrollPane, ThumbnailViewportTracker> TRACKERS = new WeakHashMap<>();
    private final WeakReference<ScrollPane> owner;
    private final Set<ThumbnailView> previews = Collections.newSetFromMap(new WeakHashMap<>());
    private final InvalidationListener changed = observable -> requestCheck();
    private boolean queued;

    private ThumbnailViewportTracker(ScrollPane scroll) {
        owner = new WeakReference<>(scroll);
        scroll.vvalueProperty().addListener(changed);
        scroll.hvalueProperty().addListener(changed);
        scroll.viewportBoundsProperty().addListener(changed);
        scroll.visibleProperty().addListener(changed);
    }
    static List<ThumbnailViewportTracker> attach(ThumbnailView preview) {
        List<ThumbnailViewportTracker> attached = new ArrayList<>();
        for (Node parent = preview.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ScrollPane scroll) {
                var tracker = TRACKERS.computeIfAbsent(scroll, ThumbnailViewportTracker::new);
                tracker.previews.add(preview); attached.add(tracker); tracker.requestCheck();
            }
        }
        return attached;
    }
    void detach(ThumbnailView preview) {
        previews.remove(preview);
        ScrollPane scroll = owner.get();
        if (previews.isEmpty() && scroll != null) {
            scroll.vvalueProperty().removeListener(changed);
            scroll.hvalueProperty().removeListener(changed);
            scroll.viewportBoundsProperty().removeListener(changed);
            scroll.visibleProperty().removeListener(changed);
            TRACKERS.remove(scroll);
        }
    }
    private void requestCheck() {
        if (queued) return;
        queued = true;
        Platform.runLater(() -> {
            queued = false;
            for (ThumbnailView preview : new ArrayList<>(previews)) preview.viewportChanged();
        });
    }
}
