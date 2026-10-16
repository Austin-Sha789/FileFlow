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
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.geometry.Side;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;


import java.io.File;
import java.io.IOException;

import java.nio.file.Path;
import java.sql.SQLException;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.ScanResult;

import com.austin.fileflow.service.FileScanService;
import com.austin.fileflow.model.FileType;
import com.austin.fileflow.repository.FavouriteRepo;
import com.austin.fileflow.repository.TagRepo;
import com.austin.fileflow.platform.FilePlatformService;

public class MainView {
    
    private final BorderPane root;
    private final Label pageTitle;
    private final VBox pageContent;
    private final Button scanButton;
    private final FileScanService fileScanService;
    private ScanResult latestScanResult;
    private final FavouriteRepo favouriteRepo;
    private final TagRepo tagRepo;
    private Label filesNavItem;
    private final List<Label> navItems = new ArrayList<>();
    private Button homeScanButton;
    private boolean scanning;
    private Task<ScanResult> currentScanTask;
    private final ProgressBar scanProgressBar;
    private final Label scanStatusLabel;
    private final FilePlatformService filePlatformService;

    public MainView(
                FavouriteRepo favouriteRepo, 
                TagRepo tagRepo, 
                FilePlatformService filePlatformService
        ) {
        this.favouriteRepo = favouriteRepo;
        this.tagRepo = tagRepo;
        this.filePlatformService = filePlatformService;

        scanProgressBar = new ProgressBar();

        scanProgressBar.setProgress(
                ProgressIndicator.INDETERMINATE_PROGRESS
        );

        scanProgressBar.setVisible(false);
        scanProgressBar.setManaged(false);

        scanStatusLabel = new Label();
        scanStatusLabel.setVisible(false);
        scanStatusLabel.setManaged(false);

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

        filesNavItem = new Label("Files");
        filesNavItem.getStyleClass().add("nav-item");   

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

        navItems.addAll(List.of(
                home,
                dashboard,
                filesNavItem,
                favourites,
                duplicates,
                tags,
                history,
                settings
        ));

        filesNavItem.setOnMouseClicked(event ->
                selectNavItem(filesNavItem, "Files")
        );

        home.setOnMouseClicked(event ->
                selectNavItem(home, "Home")
        );

        dashboard.setOnMouseClicked(event ->
                selectNavItem(dashboard, "Dashboard")
        );

        favourites.setOnMouseClicked(event ->
                selectNavItem(favourites, "Favourites")
        );

        duplicates.setOnMouseClicked(event ->
                selectNavItem(duplicates, "Duplicates")
        );

        tags.setOnMouseClicked(event ->
                selectNavItem(tags, "Tags")
        );

        history.setOnMouseClicked(event ->
                selectNavItem(history, "History")
        );

        settings.setOnMouseClicked(event ->
                selectNavItem(settings, "Settings")
        );


        selectNavItem(home, "Home");

        sidebar.getChildren().addAll(
                logo,
                home,
                dashboard,
                filesNavItem,
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

        scanButton.setOnAction(event -> handleScanAction());

        scanButton.getStyleClass().add("primary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(
                12,
                pageTitle,
                spacer,
                scanStatusLabel,
                scanProgressBar,
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

    private void selectNavItem(Label selectedItem, String title) {

        for (Label navItem : navItems) {
                navItem.getStyleClass().remove("nav-item-selected");
        }

        selectedItem.getStyleClass().add("nav-item-selected");

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

        homeScanButton  = new Button("Scan a folder");
        homeScanButton.setOnAction(event -> handleScanAction());
        homeScanButton.getStyleClass().add("home-scan-button");

        hero.getChildren().addAll(
                heading,
                subtitle,
                homeScanButton
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

        HBox controlsRow = new HBox(12);

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

        Set<Path> favouritePaths = new HashSet<>();

        try {
                favouritePaths.addAll(favouriteRepo.getAll());
        } catch (SQLException e) {
                e.printStackTrace();
        }

        Map<Path, List<String>> fileTags = new HashMap<>();

        try {
                fileTags.putAll(tagRepo.getAllFileTags());
        } catch (SQLException e) {
                e.printStackTrace();
        }

        List<String> existingTags = new ArrayList<>();

        try {
                existingTags.addAll(tagRepo.getAll());
        } catch (SQLException e) {
                e.printStackTrace();
        }

        ListView<FileItem> fileList = new ListView<>();
        fileList.setCellFactory(listView -> new ListCell<FileItem>() {
                @Override
                protected void updateItem(FileItem item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                                return;
                        }

                        Label fileNameLabel = new Label(
                                item.isDirectory()
                                        ? "[Folder] " + item.getName()
                                        : item.getName()
                        );

                        Label fileSizeLabel = new Label(
                                item.isDirectory()
                                        ? "-"
                                        : formatSize(item.getSizeBytes())
                        );

                        fileNameLabel.getStyleClass().add("file-name");
                        fileSizeLabel.getStyleClass().add("file-size");

                        Button favouriteButton = new Button(
                                favouritePaths.contains(item.getPath())
                                        ? "★"
                                        : "☆"
                        );
                        favouriteButton.getStyleClass().add("favourite-button");

                        favouriteButton.setOnAction(event -> {
                                Path filePath = item.getPath();

                                try {
                                        if (favouritePaths.contains(filePath)) {
                                                favouriteRepo.remove(filePath);

                                                favouritePaths.remove(filePath);
                                                favouriteButton.setText("☆");
                                        } else {
                                                favouriteRepo.add(filePath);

                                                favouritePaths.add(filePath);
                                                favouriteButton.setText("★");
                                        }
                                } catch (SQLException e) {
                                        e.printStackTrace();
                                }
                        });

                        HBox tagContainer = new HBox(6);

                        Button tagButton = new Button("+ Tag");
                        tagButton.getStyleClass().add("add-tag-button");

                        tagButton.setOnAction(event -> {
                                Dialog<String> dialog = new Dialog<>();

                                dialog.setTitle("Add Tag");
                                dialog.setHeaderText("Add a tag to " + item.getName());

                                ButtonType addButtonType = new ButtonType("Add");

                                dialog.getDialogPane().getButtonTypes().addAll(
                                        addButtonType,
                                        ButtonType.CANCEL
                                );

                                ComboBox<String> existingTagBox = new ComboBox<>();
                                existingTagBox.getItems().addAll(existingTags);

                                if (!existingTags.isEmpty()) {
                                        existingTagBox.setValue(existingTags.get(0));
                                } else {
                                        existingTagBox.setDisable(true);
                                        existingTagBox.setPromptText("No existing tags");
                                }

                                TextField newTagField = new TextField();
                                newTagField.setPromptText("Create a new tag");

                                VBox dialogContent = new VBox(
                                        10,
                                        new Label("Existing tag:"),
                                        existingTagBox,
                                        new Label("Or create a new tag:"),
                                        newTagField
                                );

                                dialog.getDialogPane().setContent(dialogContent);

                                dialog.setResultConverter(buttonType -> {
                                        if (buttonType == addButtonType) {
                                                String newTagName = newTagField.getText().trim();

                                                if (!newTagName.isEmpty()) {
                                                        return newTagName;
                                                }

                                                return existingTagBox.getValue();
                                        }

                                        return null;
                                });

                                dialog.showAndWait().ifPresent(selectedTag -> {
                                        try {
                                                if (!existingTags.contains(selectedTag)) {
                                                        tagRepo.add(selectedTag);
                                                        existingTags.add(selectedTag);
                                                }

                                                tagRepo.addTagToFile(
                                                        item.getPath(),
                                                        selectedTag
                                                );

                                                List<String> itemTags = fileTags.computeIfAbsent(
                                                        item.getPath(),
                                                        path -> new ArrayList<>()
                                                );

                                                if (!itemTags.contains(selectedTag)) {
                                                        itemTags.add(selectedTag);
                                                }

                                                getListView().refresh();
                                        } catch (SQLException e) {
                                                e.printStackTrace();
                                        }
                                });
                        });

                        Button moreButton =
                                createFileActionsButton(item.getPath());

                        List<String> tags = fileTags.getOrDefault(
                                item.getPath(),
                                List.of()
                        );

                        for (String tag : tags) {
                                Button tagChip = new Button(tag + "x");
                                tagChip.getStyleClass().add("file-tag-chip");

                                tagChip.setOnAction(event -> {
                                        try {
                                                Path filePath = item.getPath();

                                                tagRepo.removeTagFromFile(filePath, tag);

                                                List<String> itemTags = fileTags.get(filePath);

                                                if (itemTags != null) {
                                                        itemTags.remove(tag);

                                                        if (itemTags.isEmpty()) {
                                                                fileTags.remove(filePath);
                                                        }
                                                }

                                                getListView().refresh();
                                        } catch (SQLException e) {
                                                e.printStackTrace();
                                        }
                                });

                                tagContainer.getChildren().add(tagChip);
                        }

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        HBox fileRow = new HBox(
                                12,
                                fileNameLabel,
                                fileSizeLabel,
                                tagContainer,
                                spacer,
                                tagButton,
                                favouriteButton,
                                moreButton
                        );

                        fileRow.setAlignment(Pos.CENTER_LEFT);
                        fileRow.getStyleClass().add("file-row");

                        setText(null);
                        setGraphic(fileRow);
                }
        });

        VBox.setVgrow(fileList, Priority.ALWAYS);

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

        filesContent.getChildren().add(fileList);

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
        VBox tagsContent = new VBox(12);

        VBox tagList = new VBox(8);

        populateTagList(tagList);

        tagsContent.getChildren().add(tagList);

        return tagsContent;
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
            ListView<FileItem> fileList, 
            String searchText, 
            String selectedType,
            String selectedSort
        ) {
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

        List<FileItem> filteredItems = latestScanResult.getItems()
                .stream()
                .filter(item ->
                        item.getName()
                                .toLowerCase()
                                .contains(keyword)
                )
                .filter(item ->
                        matchesType(item, selectedType)
                )
                .sorted(comparator)
                .toList();

        fileList.getItems().setAll(filteredItems);
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

    private void populateTagList(VBox tagList) {
        tagList.getChildren().clear();

        try {
                List<String> tags = tagRepo.getAll();

                if (tags.isEmpty()) {
                        tagList.getChildren().add(
                                new Label("No tags yet.")
                        );
                        return;
                }

                for (String tag : tags) {
                        List<Path> files = tagRepo.getFilesForTag(tag);

                        Label tagLabel = new Label(
                                tag + "(" + files.size() + ")"
                        );

                        Button deleteButton = new Button("Delete");

                        deleteButton.setOnAction(event -> {
                                Alert confirmation = new Alert(
                                        Alert.AlertType.CONFIRMATION
                                );

                                confirmation.setTitle("Delete Tag");
                                confirmation.setHeaderText(
                                        "Delete \"" + tag + "\"?"
                                );

                                confirmation.setContentText(
                                        "This will remove the tag from all files."
                                );

                                confirmation.showAndWait().ifPresent(buttonType -> {
                                        if (buttonType == ButtonType.OK) {
                                                try {
                                                        tagRepo.deleteTag(tag);
                                                        populateTagList(tagList);
                                                } catch (SQLException e) {
                                                        e.printStackTrace();
                                                }
                                        }
                                });
                        });
                                

                        Region spacer = new Region();
                        HBox.setHgrow(spacer,Priority.ALWAYS);

                        HBox tagHeader = new HBox(
                                10,
                                tagLabel,
                                spacer,
                                deleteButton
                        );

                        tagHeader.setAlignment(Pos.CENTER_LEFT);

                        VBox fileList = new VBox(6);

                        if (files.isEmpty()) {
                                Label emptyLabel = new Label("No files with this tag.");
                                emptyLabel.getStyleClass().add("tag-empty");

                                fileList.getChildren().add(emptyLabel);
                        } else {
                                for (Path filePath : files) {
                                        Label fileNameLabel = new Label(
                                                filePath.getFileName().toString()
                                        );

                                        Label filePathLabel = new Label(
                                                filePath.toString()
                                        );

                                        fileNameLabel.getStyleClass().add("tag-file-name");
                                        filePathLabel.getStyleClass().add("tag-file-path");

                                        VBox fileInfo = new VBox(
                                                2,
                                                fileNameLabel,
                                                filePathLabel
                                        );
                                        fileList.getChildren().add(fileInfo);
                                }
                        }

                        tagLabel.getStyleClass().add("tag-group-title");
                        deleteButton.getStyleClass().add("tag-delete-button");

                        tagHeader.getStyleClass().add("tag-header");
                        fileList.getStyleClass().add("tag-header");

                        VBox tagGroup = new VBox(
                                8,
                                tagHeader,
                                fileList
                        );

                        tagGroup.getStyleClass().add("tag-group");

                        tagList.getChildren().add(tagGroup);
                        }

                } catch (SQLException e) {
                        tagList.getChildren().add(
                                new Label("Unable to load tags.")
                        );
                        e.printStackTrace();
                }
    }

    private void populateFileTags(HBox tagContainer, Path filePath) {
        tagContainer.getChildren().clear();

        try {
                List<String> tags = tagRepo.getTagsForFile(filePath);

                for (String tag : tags) {
                        Button removeTagButton = new Button(tag + " ×");
                        removeTagButton.getStyleClass().add("file-tag-chip");
                        removeTagButton.setOnAction(event -> {
                        try {
                                tagRepo.removeTagFromFile(filePath, tag);

                                populateFileTags(
                                        tagContainer,
                                        filePath
                                );

                                } catch (SQLException e) {
                                e.printStackTrace();
                                }
                        });

                        tagContainer.getChildren().add(removeTagButton);
                }

                } catch (SQLException e) {
                        e.printStackTrace();
                }
        }
        
        private void handleScan() {
                if (scanning) {
                        return; 
                }

                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Select Folder");

                File selectedFolder = directoryChooser.showDialog(
                        root.getScene().getWindow()
                );

                if (selectedFolder == null) {
                        return;
                }

                Path selectedPath = selectedFolder.toPath();

                Task<ScanResult> scanTask = new Task<>() {
                        @Override 
                        protected ScanResult call() throws Exception {
                                return fileScanService.scan(
                                        selectedPath,
                                        this::isCancelled,
                                        count -> updateMessage(
                                                String.format(
                                                        "Scanning... %,d items",
                                                        count
                                                )
                                        )
                                );
                        }
                };

                scanStatusLabel.textProperty().bind(
                        scanTask.messageProperty()
                );

                scanTask.setOnSucceeded(event -> {
                        scanStatusLabel.textProperty().unbind();

                        setScanningState(false);
                        currentScanTask = null;

                        latestScanResult = scanTask.getValue();

                        selectNavItem(filesNavItem, "Files");
                });

                scanTask.setOnFailed(event -> {
                        scanStatusLabel.textProperty().unbind();

                        setScanningState(false);
                        currentScanTask = null;

                        scanTask.getException().printStackTrace();
                });

                scanTask.setOnCancelled(event -> {
                        scanStatusLabel.textProperty().unbind();

                        setScanningState(false);
                        currentScanTask = null;
                });

                setScanningState(true);

                currentScanTask = scanTask;

                Thread scanThread = new Thread(scanTask);
                scanThread.setDaemon(true);
                scanThread.start();
        }

        private void setScanningState(boolean scanning) {
                this.scanning = scanning;

                scanButton.setText(
                        scanning ? "Cancel Scan" : "Scan Folder" 
                );

                scanProgressBar.setVisible(scanning);
                scanProgressBar.setManaged(scanning);

                scanStatusLabel.setVisible(scanning);
                scanStatusLabel.setManaged(scanning);

                if (homeScanButton != null) {
                        homeScanButton.setDisable(scanning);
                        homeScanButton.setText(
                                scanning ? "Cancel Scan" : "Scan Folder"
                        );
                }
        }

        private void handleScanAction() {
                if (scanning) {
                        if (currentScanTask != null) {
                                currentScanTask.cancel();
                        }

                        return;
                }

                handleScan();
        }

        private Button createFileActionsButton(Path filePath) {
                Button moreButton = new Button("⋯");
                moreButton.getStyleClass().add("more-button");

                MenuItem openItem = new MenuItem("Open");
                MenuItem showInFolderItem = new MenuItem("Show in Folder");
                MenuItem copyPathItem = new MenuItem("Copy Path");

                ContextMenu fileMenu = new ContextMenu(
                        openItem,
                        showInFolderItem,
                        copyPathItem
                );

                openItem.setOnAction(event -> {
                        try {
                        filePlatformService.open(filePath);
                        } catch (IOException e) {
                        e.printStackTrace();
                        }
                });

                showInFolderItem.setOnAction(event -> {
                        try {
                        filePlatformService.showInFolder(filePath);
                        } catch (IOException e) {
                        e.printStackTrace();
                        }
                });

                copyPathItem.setOnAction(event -> {
                        try {
                        filePlatformService.copyPath(filePath);
                        } catch (IOException e) {
                        e.printStackTrace();
                        }
                });

                moreButton.setOnAction(event ->
                        fileMenu.show(
                                moreButton,
                                Side.BOTTOM,
                                0,
                                0
                        )
                );

                return moreButton;
                }
}
