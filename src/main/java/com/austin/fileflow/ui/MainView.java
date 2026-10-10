package com.austin.fileflow.ui;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

public class MainView {
    
    private final BorderPane root;

    private final Label pageTitle;

    private final VBox pageContent;

    public MainView() {
        root = new BorderPane();
        pageTitle = new Label("Dashboard");
        pageContent = new VBox();

        root.setLeft(createSidebar());
        root.setCenter(createContent());
    }

    public Parent getView() {
        return root;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(220);
        sidebar.getStyleClass().add("sidebar");

        Label logo = new Label("FileFlow");
        logo.getStyleClass().add("logo");

        Label dashboard = new Label("Dashboard");
        dashboard.getStyleClass().add("nav-item");

        Label files = new Label("Files");
        files.getStyleClass().add("nav-item");

        Label favourites = new Label("Favourites");
        favourites.getStyleClass().add("nav-item");

        Label duplicates = new Label("Duplicates");
        duplicates.getStyleClass().add("nav-item");

        Label tags = new Label("Tags");
        tags.getStyleClass().add("nav-item");

        Label history = new Label("History");
        history.getStyleClass().add("nav-item");

        Label settings = new Label("Settings");
        settings.getStyleClass().add("nav-item");

        dashboard.setOnMouseClicked(event ->
                selectNavItem(
                        dashboard,
                        "Dashboard",
                        dashboard,
                        files,
                        favourites,
                        duplicates,
                        tags,
                        history,
                        settings
                )
        );

        files.setOnMouseClicked(event ->
                selectNavItem(
                        files,
                        "Files",
                        dashboard,
                        files,
                        favourites,
                        duplicates,
                        tags,
                        history,
                        settings
                )
        );

        favourites.setOnMouseClicked(event ->
                selectNavItem(
                        favourites,
                        "Favourites",
                        dashboard,
                        files,
                        favourites,
                        duplicates,
                        tags,
                        history,
                        settings
                )
        );

        duplicates.setOnMouseClicked(event ->
                selectNavItem(
                        duplicates,
                        "Duplicates",
                        dashboard,
                        files,
                        favourites,
                        duplicates,
                        tags,
                        history,
                        settings
                )
        );

        tags.setOnMouseClicked(event ->
                selectNavItem(
                        tags,
                        "Tags",
                        dashboard,
                        files,
                        favourites,
                        duplicates,
                        tags,
                        history,
                        settings
                )
        );

        history.setOnMouseClicked(event ->
                selectNavItem(
                        history,
                        "History",
                        dashboard,
                        files,
                        favourites,
                        duplicates,
                        tags,
                        history,
                        settings
                )
        );

        settings.setOnMouseClicked(event ->
                selectNavItem(
                        settings,
                        "Settings",
                        dashboard,
                        files,
                        favourites,
                        duplicates,
                        tags,
                        history,
                        settings
                )
        );

        sidebar.getChildren().addAll(
                logo,
                dashboard,
                files,
                favourites,
                duplicates,
                tags,
                history,
                settings
        );

        return sidebar;
    }

    private VBox createContent() {
        VBox content = new VBox();
        content.getStyleClass().add("content");

        pageTitle.getStyleClass().add("page-title");

        Button scanButton = new Button("Scan Folder");
        scanButton.getStyleClass().add("primary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(
                pageTitle,
                spacer,
                scanButton
        );
        topBar.getStyleClass().add("top-bar");

        pageContent.getChildren().add(
                createDashboardContent()
        );

        content.getChildren().addAll(
                topBar,
                pageContent
        );

        return content;
    }

    private VBox createSummaryCard(String title, String value) {
        VBox card = new VBox();
        card.getStyleClass().add("summary-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("summary-card-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("summary-card-value");

        card.getChildren().addAll(
                titleLabel,
                valueLabel
        );

        return card;
    }

    private void selectNavItem(Label selectedItem, String title, Label... navItems ) {

        pageTitle.setText(title);

        for (Label item : navItems) {
            item.getStyleClass().remove("nav-item-selected");
        }

        selectedItem.getStyleClass().add("nav-item-selected");

        pageContent.getChildren().clear();

        if (title.equals("Dashboard")) {
            pageContent.getChildren().add(createDashboardContent());
        } else if (title.equals("Files")) {
            pageContent.getChildren().add(createFilesContent());
        } else if (title.equals("Favourites")) {
            pageContent.getChildren().add(createFavouritesContent());
        } else if (title.equals("Duplicates")) {
            pageContent.getChildren().add(createDuplicatesContent());
        } else if (title.equals("Tags")) {
            pageContent.getChildren().add(createTagsContent());
        } else if (title.equals("History")) {
            pageContent.getChildren().add(createHistoryContent());
        } else if (title.equals("Settings")) {
            pageContent.getChildren().add(createSettingsContent());
        }
    }

    private VBox createDashboardContent() {
        VBox dashboardContent = new VBox();

        GridPane summaryGrid = new GridPane();
        summaryGrid.getStyleClass().add("summary-grid");

        VBox filesCard = createSummaryCard("Files", "0");
        VBox storageCard = createSummaryCard("Storage", "0 GB");
        VBox duplicatesCard = createSummaryCard("Duplicates", "0");

        summaryGrid.add(filesCard, 0, 0);
        summaryGrid.add(storageCard, 1, 0);
        summaryGrid.add(duplicatesCard, 2, 0);

        dashboardContent.getChildren().add(summaryGrid);

        return dashboardContent;
    }

    private VBox createFilesContent() {
        VBox filesContent = new VBox();

        Label placeholder = new Label("Files content");
        filesContent.getChildren().add(placeholder);

        return filesContent;
    }

    private VBox createFavouritesContent() {
        VBox content = new VBox();
        content.getChildren().add(new Label("Favourites content"));
        return content;
    }

    private VBox createDuplicatesContent() {
        VBox content = new VBox();
        content.getChildren().add(new Label("Duplicates content"));
        return content;
    }

    private VBox createTagsContent() {
        VBox content = new VBox();
        content.getChildren().add(new Label("Tags content"));
        return content;
    }

    private VBox createHistoryContent() {
        VBox content = new VBox();
        content.getChildren().add(new Label("History content"));
        return content;
    }

    private VBox createSettingsContent() {
        VBox content = new VBox();
        content.getChildren().add(new Label("Settings content"));
        return content;
    }
}
