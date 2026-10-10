package com.austin.fileflow.ui;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

public class MainView {
    
    private final BorderPane root;

    private final Label pageTitle;

    private final VBox pageContent;

    private final Button scanButton;

    public MainView() {
        root = new BorderPane();
        pageTitle = new Label("Home");
        pageContent = new VBox();
        scanButton = new Button("Scan Folder");

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

        Label home = new Label("Home");
        home.getStyleClass().add("nav-item");

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

        home.setOnMouseClicked(event ->
                selectNavItem(
                        home,
                        "Home",
                        home,
                        dashboard,
                        files,
                        favourites,
                        duplicates,
                        tags,
                        history,
                        settings
                )
        );

        dashboard.setOnMouseClicked(event ->
                selectNavItem(
                        dashboard,
                        "Dashboard",
                        home,
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
                        home,
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
                        home,
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
                        home,
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
                        home,
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
                        home,
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
                        home,
                        dashboard,
                        files,
                        favourites,
                        duplicates,
                        tags,
                        history,
                        settings
                )
        );

        selectNavItem(
            home,
            "Home",
            home,
            dashboard,
            files,
            favourites,
            duplicates,
            tags,
            history,
            settings
    );

        sidebar.getChildren().addAll(
                logo,
                home,
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

        scanButton.getStyleClass().add("primary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(
                pageTitle,
                spacer,
                scanButton
        );
        topBar.getStyleClass().add("top-bar");

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

        scanButton.setVisible(!title.equals("Home"));
        scanButton.setManaged(!title.equals("Home"));

        switch (title) {
            case "Home" ->
                    pageContent.getChildren().add(createHomeContent());

            case "Dashboard" ->
                    pageContent.getChildren().add(createDashboardContent());

            case "Files" ->
                    pageContent.getChildren().add(createFilesContent());

            case "Favourites" ->
                    pageContent.getChildren().add(createFavouritesContent());

            case "Duplicates" ->
                    pageContent.getChildren().add(createDuplicatesContent());

            case "Tags" ->
                    pageContent.getChildren().add(createTagsContent());

            case "History" ->
                    pageContent.getChildren().add(createHistoryContent());

            case "Settings" ->
                    pageContent.getChildren().add(createSettingsContent());
        }
    }

    private VBox createHomeContent() {
        VBox homeContent = new VBox();
        homeContent.getStyleClass().add("home-content");

        VBox hero = new VBox();
        hero.getStyleClass().add("home-hero");

        Label heading = new Label("Your files, organised.");
        heading.getStyleClass().add("home-heading");

        Label subtitle = new Label(
                "Scan a folder to explore, organise and understand your local files."
        );
        subtitle.getStyleClass().add("home-subtitle");

        Button scanButton = new Button("Scan a folder");
        scanButton.getStyleClass().add("home-scan-button");

        hero.getChildren().addAll(
                heading,
                subtitle,
                scanButton
        );

        homeContent.getChildren().add(hero);

        Label recentTitle = new Label("Recent Scan");
        recentTitle.getStyleClass().add("section-title");

        VBox recentScanCard = new VBox();
        recentScanCard.getStyleClass().add("recent-scan-card");

        Label folderName = new Label("No recent scan");
        folderName.getStyleClass().add("recent-scan-name");

        Label folderDetails = new Label(
                "Scan a folder to see recent activity here."
        );
        folderDetails.getStyleClass().add("recent-scan-details");

        recentScanCard.getChildren().addAll(
                folderName,
                folderDetails
        );

        homeContent.getChildren().addAll(
                recentTitle,
                recentScanCard
        );
        
        return homeContent;
    }

    private VBox createDashboardContent() {
        VBox dashboardContent = new VBox();

        HBox summaryRow = new HBox();
        summaryRow.getStyleClass().add("summary-grid");

        VBox filesCard = createSummaryCard("Files", "0");
        VBox storageCard = createSummaryCard("Storage", "0 GB");
        VBox duplicatesCard = createSummaryCard("Duplicates", "0");

        HBox.setHgrow(filesCard, Priority.ALWAYS);
        HBox.setHgrow(storageCard, Priority.ALWAYS);
        HBox.setHgrow(duplicatesCard, Priority.ALWAYS);

        filesCard.setMaxWidth(Double.MAX_VALUE);
        storageCard.setMaxWidth(Double.MAX_VALUE);
        duplicatesCard.setMaxWidth(Double.MAX_VALUE);

        summaryRow.getChildren().addAll(
                filesCard,
                storageCard,
                duplicatesCard
        );

        dashboardContent.getChildren().add(summaryRow);

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
