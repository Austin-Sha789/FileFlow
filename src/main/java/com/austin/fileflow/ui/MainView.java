package com.austin.fileflow.ui;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.stage.DirectoryChooser;
import javafx.geometry.Pos;

import java.util.Comparator;
import java.util.List;

import java.io.File;
import java.io.IOException;

import java.nio.file.Path;
import java.sql.SQLException;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.ScanResult;

import com.austin.fileflow.service.FileScanService;
import com.austin.fileflow.model.FileType;
import com.austin.fileflow.repository.FavouriteRepo;

public class MainView {
    
    private final BorderPane root;

    private final Label pageTitle;

    private final VBox pageContent;

    private final Button scanButton;

    private final FileScanService fileScanService;

    private ScanResult latestScanResult;

    private final FavouriteRepo favouriteRepo;

    public MainView(FavouriteRepo favouriteRepo) {
        this.favouriteRepo = favouriteRepo;

        root = new BorderPane();
        pageTitle = new Label("Home");
        pageContent = new VBox();
        scanButton = new Button("Scan Folder");

        fileScanService = new FileScanService();

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

        scanButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Folder");

            File selectedFolder = directoryChooser.showDialog(
                root.getScene().getWindow()
            );

            if (selectedFolder == null) {
                return;
            }

            Path selectedPath = selectedFolder.toPath();

            try {
                ScanResult result = fileScanService.scan(selectedPath);
                latestScanResult = result;

                pageTitle.setText("Files");

                pageContent.getChildren().clear();
                pageContent.getChildren().add(createFilesContent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

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

        if (latestScanResult == null) {
            Label emptyLabel = new Label(
                    "Scan a folder to view file statistics."
            );

            dashboardContent.getChildren().add(emptyLabel);
            return dashboardContent;
        }

        long fileCount = latestScanResult.getItems()
                .stream()
                .filter(item -> !item.isDirectory())
                .count();

        long totalBytes = latestScanResult.getItems()
                .stream()
                .filter(item -> !item.isDirectory())
                .mapToLong(item -> item.getSizeBytes())
                .sum();


        HBox summaryRow = new HBox();
        summaryRow.getStyleClass().add("summary-grid");

        VBox filesCard = createSummaryCard("Files", String.valueOf(fileCount));
        VBox storageCard = createSummaryCard("Storage", formatSize(totalBytes));
        VBox duplicatesCard = createSummaryCard("Duplicates", "Not analysed");

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

        if (latestScanResult == null) {
            Label emptyLabel = new Label("No folder has been scanned yet.");
            filesContent.getChildren().add(emptyLabel);
            return filesContent;
        }

        TextField searchField = new TextField();
        searchField.setPromptText("Search files...");
        searchField.getStyleClass().add("search-field");

        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll(
                        "All",
                        "Images",
                        "Videos",
                        "Documents",
                        "Audio",
                        "Archives",
                        "Code"
                );

        typeFilter.setValue("All");

        ComboBox<String> sortBy = new ComboBox<>();

        sortBy.getItems().addAll(
            "Name A-Z",
            "Name Z-A",
            "Size Small-Large",
            "Size Large-Small",
            "Newest",
            "Oldest"
        );

        sortBy.setValue("Name A-Z");

        HBox controlsRow = new HBox();
        controlsRow.setSpacing(12);

        controlsRow.getChildren().addAll(
                searchField,
                typeFilter,
                sortBy
        );

        filesContent.getChildren().add(controlsRow);

        if (!latestScanResult.getIssues().isEmpty()) {
            Label issueLabel = new Label(
                        latestScanResult.getIssues().size()
                                + " item(s) could not be scanned"
            );

            issueLabel.getStyleClass().add("scan-issue");

            filesContent.getChildren().add(issueLabel);
        }

        VBox fileList = new VBox();
        fileList.setSpacing(8);

        populateFileList(fileList, "", typeFilter.getValue(), sortBy.getValue());

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    populateFileList(fileList, newValue, typeFilter.getValue(), sortBy.getValue());
                }
        );

        typeFilter.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    populateFileList(
                            fileList,
                            searchField.getText(),
                            newValue,
                            sortBy.getValue()
                    );
                }
        );

        sortBy.valueProperty().addListener(
            (observable, oldValue, newValue) -> {
                populateFileList(
                        fileList,
                        searchField.getText(),
                        typeFilter.getValue(),
                        newValue
                );
            }  
        );

        ScrollPane scrollPane = new ScrollPane(fileList);
        scrollPane.setFitToWidth(true);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        filesContent.getChildren().add(scrollPane);

        return filesContent;
    }

    private VBox createFavouritesContent() {
        VBox favouritesContent = new VBox(12);

        try {
                List<Path> favouritePaths = favouriteRepo.getAll();

                if (favouritePaths.isEmpty()) {
                        Label emptyLabel = new Label("No favourite yet.");
                        favouritesContent.getChildren().add(emptyLabel);
                        return favouritesContent;
                }

                for (Path path : favouritePaths) {
                        Label favouritLabel = new Label(path.toString());
                        favouritesContent.getChildren().add(favouritLabel);
                }
        } catch(SQLException e) {
                Label errorLabel = new Label("Unable to load favourites.");
                favouritesContent.getChildren().add(errorLabel);
                e.printStackTrace();
        }
        
        return favouritesContent;
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

    private String formatSize(long bytes) {
        double kilobytes = bytes / 1024.0;
        double megabytes = kilobytes / 1024.0;
        double gigabytes = megabytes / 1024.0;

        if (gigabytes >= 1) {
            return String.format("%.1f GB", gigabytes);
        }

        if (megabytes >= 1) {
            return String.format("%.1f MB", megabytes);
        }

        if (kilobytes >= 1) {
            return String.format("%.1f KB", kilobytes);
        }

        return bytes + " B";
    }

    private void populateFileList(
            VBox fileList, 
            String searchText, 
            String selectedType,
            String selectedSort
        ) {
        fileList.getChildren().clear();

        String keyword = searchText.toLowerCase();

        Comparator<FileItem> comparator = switch (selectedSort) {
            case "Name Z-A" ->
                        Comparator.comparing(FileItem::getName).reversed();

            case "Size Small-Large" ->
                        Comparator.comparing(FileItem::getSizeBytes);

            case "Size Large-Small" ->
                        Comparator.comparing(FileItem::getSizeBytes).reversed();

            case "Newest" ->
                        Comparator.comparing(FileItem::getLastModified).reversed();

            case "Oldest" ->
                        Comparator.comparing(FileItem::getLastModified);

            default ->
                        Comparator.comparing(FileItem::getName);
        };

        latestScanResult.getItems()
                .stream()
                .filter(item ->
                        item.getName()
                            .toLowerCase()
                            .contains(keyword)
                )
                .filter(item -> matchesType(item, selectedType))
                .sorted(comparator)
                .forEach(item -> {
                        HBox fileRow = new HBox(12);
                        fileRow.setAlignment(Pos.CENTER_LEFT);

                        Label fileLabel = new Label(
                                item.isDirectory()
                                        ? "[Folder] " + item.getName()
                                        : item.getName() + "    " + formatSize(item.getSizeBytes())
                        );
                        
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button favouriteButton = new Button();

                        try {
                                boolean isFavourite = favouriteRepo.isFavourite(item.getPath());

                                favouriteButton.setText(
                                        isFavourite ? "★" : "☆"
                                );
                        } catch (SQLException e) {
                                favouriteButton.setText("☆");
                                e.printStackTrace();
                        }

                        favouriteButton.setOnAction(event -> {
                                try {
                                        boolean isFavourite = favouriteRepo.isFavourite(item.getPath());

                                        if (isFavourite) {
                                                favouriteRepo.remove(item.getPath());
                                                favouriteButton.setText("☆");
                                        } else {
                                                favouriteRepo.add(item.getPath());
                                                favouriteButton.setText("★");
                                        }
                                } catch (SQLException e) {
                                        e.printStackTrace();
                                }
                        });

                        fileRow.getChildren().addAll(
                                fileLabel,
                                spacer,
                                favouriteButton
                        );

                        fileList.getChildren().add(fileRow);
        });
    }

    private boolean matchesType(FileItem item, String selectedType) {
        return switch (selectedType) {
            case "Images" -> item.getType() == FileType.IMAGE;
            case "Videos" -> item.getType() == FileType.VIDEO;
            case "Documents" -> item.getType() == FileType.DOCUMENT;
            case "Audio" -> item.getType() == FileType.AUDIO;
            case "Archives" -> item.getType() == FileType.ARCHIVE;
            case "Code" -> item.getType() == FileType.CODE;
            default -> true;
        };
    }
}
