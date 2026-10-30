package com.austin.fileflow.ui.component;

import static com.austin.fileflow.i18n.I18n.tr;
import com.austin.fileflow.model.StorageVolume;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.animation.*;
import javafx.beans.InvalidationListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;

/** Owned, scrollable disk cards. The owner button remains clickable to toggle the panel. */
public final class VolumePicker extends Popup {
    private final Button anchor;
    private final Function<StorageVolume, VBox> cardFactory;
    private final VBox panel = new VBox(10);
    private Scene ownerScene;
    private Window owner;
    private final EventHandler<MouseEvent> outside = this::outsidePressed;
    private void outsidePressed(MouseEvent event) {
        for (Object target = event.getTarget(); target instanceof Node node; target = node.getParent()) if (node == anchor) return;
        hide();
    }
    private final EventHandler<KeyEvent> escape = event -> { if (event.getCode() == KeyCode.ESCAPE) { hide(); event.consume(); } };
    private final InvalidationListener moved = observable -> hide();

    public VolumePicker(Button anchor, Function<StorageVolume, VBox> cardFactory) {
        this.anchor = anchor; this.cardFactory = cardFactory;
        setAutoHide(false); setHideOnEscape(true);
        panel.getStyleClass().addAll("ocean-app", "storage-volume-picker");
        panel.getStylesheets().addAll(getClass().getResource("/css/app.css").toExternalForm(), getClass().getResource("/css/ocean.css").toExternalForm());
        panel.setPrefWidth(300); panel.setMinWidth(300); panel.setMaxWidth(300);
        getContent().add(panel); getScene().setFill(Color.TRANSPARENT);
        getScene().getRoot().setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        setOnHidden(event -> {
            if (ownerScene != null) { ownerScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, outside); ownerScene.removeEventFilter(KeyEvent.KEY_PRESSED, escape); }
            if (owner != null) {
                owner.xProperty().removeListener(moved); owner.yProperty().removeListener(moved);
                owner.widthProperty().removeListener(moved); owner.heightProperty().removeListener(moved);
            }
        });
    }
    public void toggle(List<StorageVolume> volumes, java.nio.file.Path selected, Consumer<StorageVolume> choose) {
        if (isShowing()) { hide(); return; }
        Label title = new Label(tr("storage.choose_disk")); title.getStyleClass().add("section-title");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button("×"); close.getStyleClass().add("storage-picker-close"); close.setAccessibleText(tr("common.close")); close.setOnAction(event -> hide());
        HBox header = new HBox(8, title, spacer, close); header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox cards = new VBox(9); cards.getStyleClass().add("storage-volume-cards");
        for (StorageVolume volume : volumes) {
            VBox card = cardFactory.apply(volume);
            card.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("active"), volume.getRootPath().equals(selected));
            card.setFocusTraversable(true); card.setAccessibleRole(javafx.scene.AccessibleRole.BUTTON); card.setAccessibleText(volume.getName());
            card.setOnMouseClicked(event -> { if (event.getButton() == MouseButton.PRIMARY) { choose.accept(volume); hide(); } });
            card.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) { choose.accept(volume); hide(); event.consume(); } });
            cards.getChildren().add(card);
        }
        ScrollPane scroll = new ScrollPane(cards); scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("storage-volume-picker-scroll"); scroll.setMinHeight(0);
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal()); if (bounds == null) return;
        var screens = Screen.getScreensForRectangle(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
        var screen = (screens.isEmpty() ? Screen.getPrimary() : screens.get(0)).getVisualBounds();
        double viewport = Math.min(380, screen.getHeight() * .55);
        scroll.setPrefViewportHeight(Math.min(viewport, Math.max(100, volumes.size() * 112)));
        scroll.setMaxHeight(viewport); VBox.setVgrow(scroll, Priority.ALWAYS);
        panel.getChildren().setAll(header, scroll); panel.setMaxHeight(viewport + 76);
        ownerScene = anchor.getScene(); owner = ownerScene.getWindow();
        ownerScene.addEventFilter(MouseEvent.MOUSE_PRESSED, outside); ownerScene.addEventFilter(KeyEvent.KEY_PRESSED, escape);
        owner.xProperty().addListener(moved); owner.yProperty().addListener(moved);
        owner.widthProperty().addListener(moved); owner.heightProperty().addListener(moved);
        double x = Math.min(bounds.getMaxX() + 10, screen.getMaxX() - 308);
        double y = Math.max(screen.getMinY() + 8, Math.min(bounds.getMaxY() - panel.prefHeight(300), screen.getMaxY() - panel.prefHeight(300) - 8));
        show(anchor, Math.max(screen.getMinX() + 8, x), y);
        panel.setOpacity(0); panel.setTranslateX(-5);
        FadeTransition fade = new FadeTransition(Duration.millis(140), panel); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(140), panel); slide.setToX(0);
        new ParallelTransition(fade, slide).play();
    }
}
