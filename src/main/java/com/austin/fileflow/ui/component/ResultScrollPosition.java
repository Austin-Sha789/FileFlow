package com.austin.fileflow.ui.component;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;
import javafx.scene.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/** Restores the same result group and pixel offset, rather than a percentage of a shrinking list. */
public final class ResultScrollPosition {
    private static final Object GROUP = new Object();
    private record Group(Set<Path> paths, Runnable expand, BooleanSupplier expanded) { }
    private record SavedGroup(Set<Path> paths, boolean expanded) { }
    private record SavedScroll(String id, List<SavedGroup> groups, int anchor, double offset) { }
    private final List<SavedScroll> scrolls;
    private ResultScrollPosition(List<SavedScroll> scrolls) { this.scrolls = scrolls; }

    public static void markGroup(Node card, Collection<Path> paths, Runnable expand, BooleanSupplier expanded) {
        Set<Path> normalized = new HashSet<>();
        paths.forEach(path -> normalized.add(path.toAbsolutePath().normalize()));
        card.getProperties().put(GROUP, new Group(Set.copyOf(normalized), expand, expanded));
    }
    private static boolean visible(Node node) {
        for (Node parent = node; parent != null; parent = parent.getParent()) if (!parent.isVisible()) return false;
        return true;
    }
    private static List<Node> cards(ScrollPane scroll) {
        if (!(scroll.getContent() instanceof VBox content)) return List.of();
        return content.getChildren().stream().filter(node -> node.getProperties().get(GROUP) instanceof Group).toList();
    }
    public static ResultScrollPosition capture(Parent root) {
        root.applyCss(); root.layout();
        List<SavedScroll> saved = new ArrayList<>();
        for (Node node : root.lookupAll(".duplicates-scroll")) {
            if (!(node instanceof ScrollPane scroll) || !visible(scroll) || scroll.getId() == null) continue;
            List<Node> cards = cards(scroll); if (cards.isEmpty()) continue;
            double overflow = Math.max(0, scroll.getContent().getLayoutBounds().getHeight() - scroll.getViewportBounds().getHeight());
            double pixel = overflow * (scroll.getVvalue() - scroll.getVmin()) / Math.max(.0001, scroll.getVmax() - scroll.getVmin());
            int anchor = cards.size() - 1;
            for (int index = 0; index < cards.size(); index++) if (cards.get(index).getBoundsInParent().getMaxY() > pixel) { anchor = index; break; }
            List<SavedGroup> groups = new ArrayList<>();
            for (Node card : cards) {
                Group group = (Group) card.getProperties().get(GROUP);
                groups.add(new SavedGroup(group.paths(), group.expanded() != null && group.expanded().getAsBoolean()));
            }
            saved.add(new SavedScroll(scroll.getId(), groups, anchor, cards.get(anchor).getBoundsInParent().getMinY() - pixel));
        }
        return new ResultScrollPosition(saved);
    }
    private static Node match(Set<Path> paths, List<Node> cards) {
        Node best = null; int score = 0;
        for (Node card : cards) {
            Group group = (Group) card.getProperties().get(GROUP);
            int shared = 0; for (Path path : group.paths()) if (paths.contains(path)) shared++;
            if (shared > score) { best = card; score = shared; }
        }
        return best;
    }
    public void restore(Parent root) {
        for (SavedScroll saved : scrolls) {
            Node found = root.lookup("#" + saved.id());
            if (!(found instanceof ScrollPane scroll)) continue;
            List<Node> cards = cards(scroll);
            for (SavedGroup group : saved.groups()) if (group.expanded()) {
                Node card = match(group.paths(), cards);
                if (card != null && ((Group) card.getProperties().get(GROUP)).expand() != null) ((Group) card.getProperties().get(GROUP)).expand().run();
            }
        }
        root.applyCss(); root.layout();
        for (SavedScroll saved : scrolls) {
            Node found = root.lookup("#" + saved.id());
            if (!(found instanceof ScrollPane scroll)) continue;
            List<Node> cards = cards(scroll); Node anchor = null; int selected = saved.anchor();
            for (int index = saved.anchor(); index < saved.groups().size() && anchor == null; index++) {
                anchor = match(saved.groups().get(index).paths(), cards); selected = index;
            }
            for (int index = saved.anchor() - 1; index >= 0 && anchor == null; index--) {
                anchor = match(saved.groups().get(index).paths(), cards); selected = index;
            }
            if (anchor == null) continue;
            double offset = selected == saved.anchor() ? Math.max(saved.offset(), -anchor.getBoundsInParent().getHeight() + 24) : 0;
            double overflow = Math.max(0, scroll.getContent().getLayoutBounds().getHeight() - scroll.getViewportBounds().getHeight());
            double fraction = overflow <= 0 ? 0 : Math.max(0, Math.min(1, (anchor.getBoundsInParent().getMinY() - offset) / overflow));
            scroll.setVvalue(scroll.getVmin() + fraction * (scroll.getVmax() - scroll.getVmin()));
        }
    }
}
