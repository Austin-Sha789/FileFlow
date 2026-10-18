package com.austin.fileflow.ui;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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
import java.util.function.Consumer;


import java.io.File;
import java.io.IOException;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.ScanResult;
import com.austin.fileflow.model.StorageVolume;
import com.austin.fileflow.service.FileScanService;
import com.austin.fileflow.service.StorageService;
import com.austin.fileflow.model.FileType;
import com.austin.fileflow.repository.FavouriteRepo;
import com.austin.fileflow.repository.TagRepo;
import com.austin.fileflow.platform.FilePlatformService;
import com.austin.fileflow.model.DuplicateGroup;
import com.austin.fileflow.service.DuplicateService;
import com.austin.fileflow.service.StorageService;

public class MainView {
    
    private final BorderPane root;
    private final Label pageTitle;
    private final VBox pageContent;
    private final Button scanButton;
    private final FileScanService fileScanService;
    private ScanResult latestScanResult;
    private final DuplicateService duplicateService;
    private final StorageService storageService;
    private List<DuplicateGroup> latestDuplicateGroups  = List.of();
    private boolean duplicateScanning;
    private Task<List<DuplicateGroup>> currentDuplicateTask;
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
    private Path scanRootPath;
    private Label storageNameLabel;
    private ProgressBar storageUsageBar;
    private Label storageUsedLabel;
    private Label storageFreeLabel;
    private Label storageRootLabel;
    private Path currentStorageRoot;
    private Path currentFolder;
    private final Map<Path, Image> imageThumbnailCache =
        new HashMap<>();
    private FilesViewMode filesViewMode = FilesViewMode.BROWSE;
    private boolean duplicateAnalysisCompleted;


    private enum FilesViewMode {
        BROWSE,
        ALL_FILES,
        CATEGORIES
        }

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
        duplicateService = new DuplicateService();
        storageService = new StorageService();

        root.setLeft(createSidebar());
        root.setCenter(createContent());

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

        Region sidebarSpacer = new Region();
        VBox.setVgrow(
                sidebarSpacer,
                Priority.ALWAYS
        );

        VBox storageCard = createStorageCard();

        sidebar.getChildren().addAll(
                logo,
                home,
                dashboard,
                filesNavItem,
                favourites,
                duplicates,
                tags,
                history,
                settings,
                sidebarSpacer,
                storageCard
        );

        return sidebar;
    }

    private VBox createStorageCard() {

        storageNameLabel = new Label();
        storageNameLabel.getStyleClass().add("sidebar-storage-name");
        storageNameLabel.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(
                storageNameLabel,
                Priority.ALWAYS
        );

        storageRootLabel = new Label();

        storageRootLabel.getStyleClass().add("sidebar-storage-root");

        Button storageVolumesButton = new Button("›");
        storageVolumesButton.getStyleClass().add("sidebar-storage-volumes-button");

        HBox storageHeader = new HBox(
                8,
                storageNameLabel,
                storageRootLabel,
                storageVolumesButton
        );

        storageHeader.setAlignment(Pos.CENTER_LEFT);

        storageUsageBar = new ProgressBar(0);
        storageUsageBar.setMaxWidth(Double.MAX_VALUE);
        storageUsageBar.getStyleClass().add("sidebar-storage-progress");

        storageUsedLabel = new Label();
        
        storageUsedLabel.getStyleClass().add("sidebar-storage-root");

        storageRootLabel.setMinWidth(Region.USE_PREF_SIZE);

        storageFreeLabel = new Label();

        storageFreeLabel.getStyleClass().add("sidebar-storage-detail");

        storageUsedLabel.setWrapText(true);
        storageFreeLabel.setWrapText(true);

        VBox storageDetails = new VBox(
                2,
                storageUsedLabel,
                storageFreeLabel
        );

        VBox storageCard =
                new VBox(
                        8,
                        storageHeader,
                        storageUsageBar,
                        storageDetails
                );

        storageCard.getStyleClass().add(
                "sidebar-storage-card"
        );

        VBox storageVolumesBox = new VBox(8);

        storageVolumesBox.getStyleClass().add("sidebar-storage-volumes");

        ScrollPane storageVolumesScroll =
                new ScrollPane(
                        storageVolumesBox
                );

        storageVolumesScroll.setFitToWidth(
                true
        );

        storageVolumesScroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        storageVolumesScroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        storageVolumesScroll.setMaxHeight(
                240
        );

        storageVolumesScroll.setPrefViewportHeight(
                240
        );

        storageVolumesScroll.setVisible(
                false
        );

        storageVolumesScroll.setManaged(
                false
        );

        storageVolumesScroll.getStyleClass().add(
                "sidebar-storage-scroll"
        );

        storageVolumesBox.setFillWidth(
                true
        );

        storageVolumesButton.setOnAction(event -> {

                boolean expanding =
                        !storageVolumesScroll.isVisible();

                if (expanding) {

                        storageVolumesBox
                                .getChildren()
                                .clear();

                        for (StorageVolume volume
                                : storageService.getAvailableVolumes()) {

                        if (currentStorageRoot != null
                                && volume.getRootPath().equals(currentStorageRoot)
                        ) {
                                continue;
                        }

                        VBox volumeCard =
                                createStorageVolumeCard(
                                        volume
                                );

                        volumeCard.setOnMouseClicked(
                                mouseEvent -> {

                                        updateStorageCard(
                                                volume.getRootPath()
                                        );

                                        storageVolumesScroll.setVisible(
                                                false
                                        );

                                        storageVolumesScroll.setManaged(
                                                false
                                        );

                                        storageVolumesButton.setText(
                                                "›"
                                        );
                                }
                        );

                        storageVolumesBox
                                .getChildren()
                                .add(
                                        volumeCard
                                );
                        }
                }

                storageVolumesScroll.setVisible(
                        expanding
                );

                storageVolumesScroll.setManaged(
                        expanding
                );

                storageVolumesButton.setText(
                        expanding ? "⌄" : "›"
                );
        });

        Path initialPath = 
                Path.of(
                        System.getProperty("user.home")
                );

        updateStorageCard(initialPath);

        VBox storageSection = new VBox(
                8,
                storageCard,
                storageVolumesScroll
        );

        return storageSection;
    }

    private void updateStorageCard(Path path) {

        StorageVolume volume =
                storageService.getVolumeForPath(path);

        if (volume == null) {

                currentStorageRoot = null;

                storageNameLabel.setText("Storage");
                storageRootLabel.setText("");
                storageUsageBar.setProgress(0);
                storageUsedLabel.setText("Unavailable");
                storageFreeLabel.setText("");

                return;
        }

        currentStorageRoot = volume.getRootPath();

        String fullName = volume.getName();

        String rootText = volume.getRootPath().toString().replace("\\", "");

        String rootLabel =  "(" + rootText + ")";

        String displayName = fullName;

        if (fullName.endsWith(rootLabel)) {

                displayName =
                        fullName.substring(
                                0,
                                fullName.length()
                                        - rootLabel.length()
                        ).trim();
        }

        storageNameLabel.setText(displayName);

        storageRootLabel.setText(rootLabel);

        storageUsageBar.setProgress(
                volume.getUsageRatio()
        );

        storageUsedLabel.setText(
                formatSize(volume.getUsedBytes())
                        + " used · "
                        + formatSize(volume.getTotalBytes())
                        + " total"
        );

        storageFreeLabel.setText(
                formatSize(volume.getFreeBytes())
                        + " free"
        );

        storageUsedLabel.setWrapText(true);
        storageFreeLabel.setWrapText(true);
    }

    private VBox createStorageVolumeCard(StorageVolume volume) {

        Label nameLabel =
                new Label(
                        volume.getName()
                );

        nameLabel.getStyleClass().add(
                "sidebar-storage-name"
        );

        nameLabel.setWrapText(true);


        ProgressBar usageBar =
                new ProgressBar(
                        volume.getUsageRatio()
                );

        usageBar.setMaxWidth(
                Double.MAX_VALUE
        );

        usageBar.getStyleClass().add(
                "sidebar-storage-progress"
        );


        Label usedLabel =
                new Label(
                        formatSize(
                                volume.getUsedBytes()
                        )
                                + " used · "
                                + formatSize(
                                        volume.getTotalBytes()
                                )
                                + " total"
                );

        usedLabel.getStyleClass().add(
                "sidebar-storage-detail"
        );

        usedLabel.setWrapText(true);


        Label freeLabel =
                new Label(
                        formatSize(
                                volume.getFreeBytes()
                        )
                                + " free"
                );

        freeLabel.getStyleClass().add(
                "sidebar-storage-detail"
        );


        VBox details =
                new VBox(
                        2,
                        usedLabel,
                        freeLabel
                );


        VBox volumeCard =
                new VBox(
                        8,
                        nameLabel,
                        usageBar,
                        details
                );

        volumeCard.getStyleClass().add(
                "sidebar-storage-volume-card"
        );

        volumeCard.setMaxWidth(
                Double.MAX_VALUE
        );

        return volumeCard;
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

        VBox.setVgrow(pageContent, Priority.ALWAYS);

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

            case "Files" -> {

                VBox filesContent =
                        createFilesContent();

                pageContent.getChildren().add(
                        filesContent
                );

                VBox.setVgrow(
                        filesContent,
                        Priority.ALWAYS
                );
        }

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
        VBox dashboardPage = new VBox();

        VBox dashboardContent = new VBox(18);

        ScrollPane scrollPane = new ScrollPane(dashboardContent);
        scrollPane.getStyleClass().add("dashboard-scroll");

        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        dashboardPage.getChildren().add(scrollPane);

        if (latestScanResult == null) {
            Label emptyLabel = new Label(
                    "Scan a folder to view file statistics."
            );

            dashboardContent.getChildren().add(emptyLabel);
            return dashboardPage;
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

        long folderCount = latestScanResult.getItems()
                .stream()
                .filter(FileItem::isDirectory)
                .count();


        HBox summaryRow = new HBox();
        summaryRow.getStyleClass().add("summary-grid");

        VBox filesCard = createSummaryCard("Files", String.valueOf(fileCount));
        VBox storageCard = createSummaryCard("Storage", formatSize(totalBytes));
        VBox foldersCard = createSummaryCard("Folders", String.valueOf(folderCount));
        VBox issuesCard =
                createSummaryCard(
                        "Scan Issues",
                        String.valueOf(latestScanResult.getIssues().size())
        );

        HBox.setHgrow(filesCard, Priority.ALWAYS);
        HBox.setHgrow(storageCard, Priority.ALWAYS);
        HBox.setHgrow(foldersCard, Priority.ALWAYS);
        HBox.setHgrow(issuesCard, Priority.ALWAYS);

        filesCard.setMaxWidth(Double.MAX_VALUE);
        storageCard.setMaxWidth(Double.MAX_VALUE);
        foldersCard.setMaxWidth(Double.MAX_VALUE);
        issuesCard.setMaxWidth(Double.MAX_VALUE);

        summaryRow.getChildren().addAll(
                filesCard,
                storageCard,
                foldersCard,
                issuesCard
        );

        dashboardContent.getChildren().add(summaryRow);

        VBox largestFilesList = new VBox(6);

        List<FileItem> largestFiles = latestScanResult.getItems()
                .stream()
                .filter(item -> !item.isDirectory())
                .sorted(
                        Comparator.comparingLong(FileItem::getSizeBytes)
                                .reversed()
                )
                .limit(10)
                .toList();

        for (FileItem item : largestFiles) {
                Label nameLabel = new Label(item.getName());
                nameLabel.getStyleClass().add("file-name");

                Label pathLabel = new Label();

                if (scanRootPath != null) {
                        Path parent = item.getPath().getParent();

                        if (parent != null) {
                                Path relativeParent = scanRootPath.relativize(parent);

                                pathLabel.setText(
                                        relativeParent.toString().isEmpty()
                                                ? "Scan root"
                                                : relativeParent.toString()
                                );
                        }
                }

                pathLabel.getStyleClass().add("dashboard-file-path");

                VBox fileInfo = new VBox(
                        3,
                        nameLabel,
                        pathLabel
                );

                Node filePreview =
                        createFilePreview(item);

                Label sizeLabel = new Label(formatSize(item.getSizeBytes()));
                sizeLabel.getStyleClass().add("file-size");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button moreButton =
                        createFileActionsButton(item.getPath());

                HBox row = new HBox(
                        12,
                        filePreview,
                        fileInfo,
                        spacer,
                        sizeLabel,
                        moreButton
                );

                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("dashboard-file-row");

                largestFilesList.getChildren().add(row);
        }

        VBox largestFilesSection = createCollapsibleSection(
                "Largest Files",
                largestFiles.size(),
                largestFilesList
        );

        Label typeBreakdownTitle = new Label("File Type Breakdown");
        typeBreakdownTitle.getStyleClass().add("section-title");

        VBox typeBreakdownList = new VBox(6);

        Map<FileType, Long> fileCountsByType = new HashMap<>();
        Map<FileType, Long> bytesByType = new HashMap<>();

        for (FileItem item : latestScanResult.getItems()) {
                if (item.isDirectory()) {
                        continue;
                }

                FileType type = item.getType();

                fileCountsByType.merge(
                        type,
                        1L,
                        Long::sum
                );

                bytesByType.merge(
                        type,
                        item.getSizeBytes(),
                        Long::sum
                );
        }

        List<FileType> dashboardTypes = List.of(FileType.values())
                .stream()
                .filter(type -> 
                        fileCountsByType.getOrDefault(type, 0L) > 0
                )
                .sorted(
                        Comparator.comparingLong(
                                (FileType type) ->
                                        bytesByType.getOrDefault(type, 0L)
                        ).reversed()
                )
                .toList();

        for (FileType type : dashboardTypes) {
                long count = fileCountsByType.getOrDefault(
                        type,
                        0L
                );

                long bytes = bytesByType.getOrDefault(
                        type,
                        0L
                );

                double percentage = totalBytes == 0
                        ? 0
                        : (double) bytes / totalBytes * 100;

                Label typeLabel = new Label(
                        getFileTypeDisplayName(type)
                );
                typeLabel.getStyleClass().add("dashboard-type-name");

                Label countLabel = new Label(
                         String.format("%,d files", count)
                );
                countLabel.getStyleClass().add(
                        "dashboard-type-meta"
                );

                Label sizeLabel = new Label(
                        formatSize(bytes)
                );
                sizeLabel.getStyleClass().add(
                        "dashboard-type-meta"
                );

                Label percentageLabel = new Label(
                        String.format("%.1f%%", percentage)
                );
                percentageLabel.getStyleClass().add(
                        "dashboard-type-percentage"
                );

                ProgressBar typeProgressBar = new ProgressBar(
                        percentage / 100.0
                );

                typeProgressBar.setPrefWidth(180);
                typeProgressBar.getStyleClass().add(
                        "dashboard-type-progress"
                );

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(
                        14,
                        typeLabel,
                        spacer,
                        countLabel,
                        sizeLabel,
                        typeProgressBar,
                        percentageLabel
                );

                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add(
                        "dashboard-type-row"
                );

                typeBreakdownList.getChildren().add(row);
        }

        dashboardContent.getChildren().addAll(
                typeBreakdownTitle,
                typeBreakdownList
        );

        if (scanRootPath != null) {
                Label currentScanTitle = new Label("Current Scan");
                currentScanTitle.getStyleClass().add("section-title");

                Label rootNameLabel = new Label(
                        getPathDisplayName(scanRootPath)
                );
                rootNameLabel.getStyleClass().add("current-scan-name");

                Label rootPathLabel = new Label(
                        scanRootPath.toString()
                );
                rootPathLabel.getStyleClass().add("current-scan-path");

                Label scanSummaryLabel = new Label(
                        String.format(
                                "%,d files · %,d folders · %s",
                                fileCount,
                                folderCount,
                                formatSize(totalBytes)
                        )
                );
                scanSummaryLabel.getStyleClass().add(
                        "current-scan-summary"
                );

                VBox scanInfo = new VBox(
                        4,
                        rootNameLabel,
                        rootPathLabel,
                        scanSummaryLabel
                );

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button moreButton = 
                        createFileActionsButton(scanRootPath);

                HBox currentScanCard = new HBox(
                        16,
                        scanInfo,
                        spacer,
                        moreButton
                );

                currentScanCard.setAlignment(Pos.CENTER_LEFT);
                currentScanCard.getStyleClass().add(
                        "current-scan-card"
                );

                dashboardContent.getChildren().addAll(
                        currentScanTitle,
                        currentScanCard
                );
        }

        Label foldersBreakdownTitle = 
                new Label("Top-level Folders");
        foldersBreakdownTitle.getStyleClass().add("section-title");

        VBox foldersBreakdownList = new VBox(6);

        Map<Path, Long> bytesByTopFolder = new HashMap<>();
        Map<Path, Long> filesByTopFolder = new HashMap<>();

        for (FileItem item : latestScanResult.getItems()) {
                if (item.isDirectory()) {
                        continue;
                }

                Path relativePath = scanRootPath.relativize(
                        item.getPath()
                );

                if (relativePath.getNameCount() < 2) {
                        continue;
                }

                Path topFolder = scanRootPath.resolve(
                        relativePath.getName(0)
                );

                bytesByTopFolder.merge(
                        topFolder,
                        item.getSizeBytes(),
                        Long::sum
                );

                filesByTopFolder.merge(
                        topFolder,
                        1L,
                        Long::sum
                );
        }

        List<Path> largestTopFolders = bytesByTopFolder.keySet()
                .stream()
                .sorted(
                        Comparator.comparingLong(
                                (Path folder) -> 
                                        bytesByTopFolder.getOrDefault(folder, 0L)        
                        ).reversed()
                )
                .limit(8)
                .toList();

        for (Path folder : largestTopFolders) {
                long bytes = bytesByTopFolder.getOrDefault(
                        folder,
                        0L
                );

                long count = filesByTopFolder.getOrDefault(
                        folder,
                        0L
                );

                double percentage = totalBytes == 0
                        ? 0
                        : (double) bytes / totalBytes * 100;

                Label nameLabel = new Label(
                        getPathDisplayName(folder)
                );
                nameLabel.getStyleClass().add(
                        "dashboard-folder-name"
                );

                Label countLabel = new Label(
                        String.format("%,d files", count)
                );
                countLabel.getStyleClass().add("dashboard-folder-meta");

                Label sizeLabel = new Label(formatSize(bytes));
                sizeLabel.getStyleClass().add("dashboard-folder-meta");

                ProgressBar progressBar = new ProgressBar(
                        percentage / 100.0
                );

                progressBar.setPrefWidth(180);
                progressBar.getStyleClass().add("dashboard-folder-progress");

                Label percentageLabel = new Label(String.format("%.1f%%", percentage));

                percentageLabel.getStyleClass().add("dashborad-folder-percentage");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button moreButton = createFileActionsButton(folder);

                HBox row = new HBox(
                        14,
                        nameLabel,
                        spacer,
                        countLabel,
                        sizeLabel,
                        progressBar,
                        percentageLabel,
                        moreButton
                );

                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("dashboard-floder-row");

                foldersBreakdownList.getChildren().add(row);
        }

        dashboardContent.getChildren().addAll(
                foldersBreakdownTitle,
                foldersBreakdownList
        );

        dashboardContent.getChildren().add(
                largestFilesSection
        );

        VBox recentFilesList = new VBox(6);

        List<FileItem> recentFiles =
                latestScanResult.getItems()
                        .stream()
                        .filter(item -> !item.isDirectory())
                        .sorted(
                                Comparator.comparing(
                                        FileItem::getLastModified
                                ).reversed()
                        )
                        .limit(10)
                        .toList();

        for (FileItem item : recentFiles) {

                Label nameLabel = new Label(item.getName());
                nameLabel.getStyleClass().add("file-name");

                Label pathLabel = new Label();

                if (scanRootPath != null) {
                        Path parent = item.getPath().getParent();

                        if (parent != null) {
                                Path relativeParent = 
                                        scanRootPath.relativize(parent);

                                pathLabel.setText(
                                        relativeParent.toString().isEmpty()
                                                ? "Scan root"
                                                : relativeParent.toString()
                                );
                                }
                        }

                pathLabel.getStyleClass().add("dashboard-file-path");

                VBox fileInfo = new VBox(
                        3,
                        nameLabel,
                        pathLabel
                );

                Node filePreview =
                        createFilePreview(item);

                Label modifiedLabel = new Label(formatModifiedTime(item.getLastModified()));

                modifiedLabel.getStyleClass().add("dashboard-modified-time");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button moreButton = createFileActionsButton(item.getPath());

                HBox row = new HBox(
                        12,
                        filePreview,
                        fileInfo,
                        spacer,
                        modifiedLabel,
                        moreButton
                );

                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("dashboard-file-row");

                recentFilesList.getChildren().add(row);

        }

        VBox recentFilesSection = createCollapsibleSection(
                "Recently Modified",
                recentFiles.size(),
                recentFilesList
        );

        dashboardContent.getChildren().add(recentFilesSection);
                
        return dashboardPage;
    }

    private VBox createFilesContent() {
        VBox filesContent = new VBox();

        filesContent.setMaxHeight(
                Double.MAX_VALUE
        );

        if (latestScanResult == null) {
            Label emptyLabel = new Label("No folder has been scanned yet.");
            filesContent.getChildren().add(emptyLabel);
            return filesContent;
        }

        VBox categoriesContent = new VBox(10);
        categoriesContent.setVisible(false);
        categoriesContent.setManaged(false);

        ToggleButton browseButton = new ToggleButton("Browse");
        ToggleButton allFilesButton = new ToggleButton("All Files");
        ToggleButton categoriesButton =
                new ToggleButton("Categories");

        ToggleGroup viewGroup = new ToggleGroup();

        browseButton.setToggleGroup(viewGroup);
        allFilesButton.setToggleGroup(viewGroup);
        categoriesButton.setToggleGroup(viewGroup);

        switch (filesViewMode) {
                case BROWSE -> browseButton.setSelected(true);
                case ALL_FILES -> allFilesButton.setSelected(true);
                case CATEGORIES -> categoriesButton.setSelected(true);
        }


        Button backButton = new Button("←");
        backButton.getStyleClass().add("breadcrumb-back");
        HBox breadcrumbBar = new HBox(6);

        HBox browseHeader = new HBox(
                10,
                backButton,
                breadcrumbBar
        );

        browseHeader.setAlignment(Pos.CENTER_LEFT);
        breadcrumbBar.setAlignment(Pos.CENTER_LEFT);

        updateFilesViewControls(browseHeader);

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
                        "Code",
                        "Other"
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

        searchField.setPrefWidth(220);
        typeFilter.setPrefWidth(140);
        sortBy.setPrefWidth(170);

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);

        HBox viewBar = new HBox(
                10,
                browseButton,
                allFilesButton,
                categoriesButton,
                toolbarSpacer,
                controlsRow
        );

        viewBar.setAlignment(Pos.CENTER_LEFT);
        viewBar.setMaxWidth(Double.MAX_VALUE);  

        controlsRow.setAlignment(Pos.CENTER_RIGHT);

        filesContent.getChildren().add(viewBar);
        filesContent.getChildren().add(browseHeader);
        filesContent.getChildren().add(categoriesContent);

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

        updateBrowseHeader(
                        backButton,
                        breadcrumbBar,
                        fileList,
                        searchField,
                        typeFilter,
                        sortBy
                );

        browseButton.setOnAction(event -> {
                filesViewMode  = FilesViewMode.BROWSE;

                searchField.clear();
                typeFilter.setValue("All");

                categoriesContent.setVisible(false);
                categoriesContent.setManaged(false);

                controlsRow.setVisible(true);
                controlsRow.setManaged(true);

                fileList.setVisible(true);
                fileList.setManaged(true);

                updateFilesViewControls(browseHeader);

                updateBrowseHeader(
                        backButton,
                        breadcrumbBar,
                        fileList,
                        searchField,
                        typeFilter,
                        sortBy
                );

                populateFileList(
                        fileList,
                        searchField.getText(), 
                        typeFilter.getValue(),
                        sortBy.getValue() 
                );
        });

        allFilesButton.setOnAction(event -> {
                filesViewMode = FilesViewMode.ALL_FILES;

                categoriesContent.setVisible(false);
                categoriesContent.setManaged(false);

                controlsRow.setVisible(true);
                controlsRow.setManaged(true);

                fileList.setVisible(true);
                fileList.setManaged(true);

                updateFilesViewControls(browseHeader);

                populateFileList(
                        fileList,
                        searchField.getText(),
                        typeFilter.getValue(),
                        sortBy.getValue()
                );
        });

        categoriesButton.setOnAction(event -> {
                filesViewMode = FilesViewMode.CATEGORIES;

                browseHeader.setVisible(false);
                browseHeader.setManaged(false);

                controlsRow.setVisible(false);
                controlsRow.setManaged(false);

                fileList.setVisible(false);
                fileList.setManaged(false);

                categoriesContent.setVisible(true);
                categoriesContent.setManaged(true);

                populateCategories(
                        categoriesContent,
                        type -> {
                                filesViewMode = FilesViewMode.ALL_FILES;

                                allFilesButton.setSelected(true);

                                categoriesContent.setVisible(false);
                                categoriesContent.setManaged(false);

                                controlsRow.setVisible(true);
                                controlsRow.setManaged(true);

                                fileList.setVisible(true);
                                fileList.setManaged(true);

                                updateFilesViewControls(browseHeader);

                                searchField.clear();

                                typeFilter.setValue(
                                        getFileTypeDisplayName(type)
                                );

                                populateFileList(
                                        fileList,
                                        searchField.getText(),
                                        typeFilter.getValue(),
                                        sortBy.getValue()
                                );
                        }
                );
        });

        backButton.setOnAction(event -> {
                if (currentFolder == null || scanRootPath == null || currentFolder.equals(scanRootPath)) {
                        return;
                }

                Path parent = currentFolder.getParent();

                if (parent != null) {
                        currentFolder = parent;

                        updateBrowseHeader(
                                backButton,
                                breadcrumbBar,
                                fileList,
                                searchField,
                                typeFilter,
                                sortBy
                        );

                        populateFileList(
                                fileList,
                                searchField.getText(),
                                typeFilter.getValue(),
                                sortBy.getValue()
                        );
                }
        });

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
                        fileNameLabel.getStyleClass().add("file-name");

                        Label fileSizeLabel = new Label(
                                item.isDirectory()
                                        ? "——"
                                        : formatSize(item.getSizeBytes())
                        );
                        fileSizeLabel.getStyleClass().add("file-size");

                        Label relativePathLabel = new Label();
                        relativePathLabel.getStyleClass().add("file-relative-path");

                        if (filesViewMode == FilesViewMode.ALL_FILES
                                && scanRootPath != null) {

                                Path parent = item.getPath().getParent();

                                if (parent != null) {
                                        Path relativeParent = scanRootPath.relativize(parent);

                                        if (relativeParent.toString().isEmpty()) {
                                                relativePathLabel.setText("Scan root");
                                        } else {
                                                relativePathLabel.setText(
                                                        relativeParent.toString()
                                                );
                                        }
                                }
                        }

                        boolean showRelativePath =
                                filesViewMode == FilesViewMode.ALL_FILES;

                        relativePathLabel.setVisible(showRelativePath);
                        relativePathLabel.setManaged(showRelativePath);

                        VBox fileInfo = new VBox(
                                3,
                                fileNameLabel,
                                relativePathLabel
                        );

                        Node filePreview =
                                createFilePreview(item);

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
                                filePreview,
                                fileInfo,
                                tagContainer,
                                spacer,
                                tagButton,
                                favouriteButton,
                                moreButton
                        );

                        fileRow.setOnMouseClicked(event -> {
                                if (filesViewMode == FilesViewMode.BROWSE
                                        && item.isDirectory()
                                        && event.getClickCount() == 2) {
                                        currentFolder = item.getPath()
                                                .toAbsolutePath()
                                                .normalize();

                                        updateBrowseHeader(
                                                backButton,
                                                breadcrumbBar,
                                                getListView(),
                                                searchField,
                                                typeFilter,
                                                sortBy
                                        );

                                        populateFileList(
                                                getListView(),
                                                searchField.getText(), 
                                                typeFilter.getValue(), 
                                                sortBy.getValue()
                                        );
                                }
                        });

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
        VBox content  = new VBox(12);

        try {
                List<Path> favouritePaths = favouriteRepo.getAll();

                if (favouritePaths.isEmpty()) {
                        content.getChildren().add(
                                new Label("No favourite files yet.")
                        );
                        return content ;
                }

                for (Path filePath : favouritePaths) {
                        Label fileNameLabel = new Label(
                                filePath.getFileName().toString()
                        );
                        fileNameLabel.getStyleClass().add("file-name");

                        Label filePathLabel = new Label(
                                filePath.toString()
                        );
                        filePathLabel.getStyleClass().add("file-path");

                        VBox fileInfo = new VBox(
                                3,
                                fileNameLabel,
                                filePathLabel
                        );

                        Node filePreview =
                                createFilePreview(filePath);

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        Button favouriteButton = new Button("★");
                        favouriteButton.getStyleClass().add(
                                "favourite-button"
                        );

                        Button moreButton = createFileActionsButton(filePath);

                        HBox row = new HBox(
                                12,
                                filePreview,
                                fileInfo,
                                spacer,
                                favouriteButton,
                                moreButton
                        );

                        row.setAlignment(Pos.CENTER_LEFT);
                        row.getStyleClass().add("file-row");

                        favouriteButton.setOnAction(event -> {
                                try {
                                        favouriteRepo.remove(filePath);
                                        content.getChildren().remove(row);
                                } catch (SQLException e) {
                                        e.printStackTrace();
                                }
                        });

                        content.getChildren().add(row);
                }

        } catch(SQLException e) {
                e.printStackTrace();
        }
        
        return content;
    }

    private VBox createDuplicatesContent() {
        VBox content = new VBox(18);
        content.getStyleClass().add("duplicates-content");

        Label heading = new Label("Duplicate Files");
        heading.getStyleClass().add("duplicates-heading");

        Label subtitle = new Label("Find identical files and see how much storage you could reclaim.");
        subtitle.getStyleClass().add("duplicates-subtitle");

        Button analyseButton = new Button("Analyse Duplicates");
        analyseButton.getStyleClass().add("duplicates-analyse-button");

        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("duplicates-status");

        HBox duplicateSummaryRow = new HBox(12);

        duplicateSummaryRow.setVisible(false);
        duplicateSummaryRow.setManaged(false);

        duplicateSummaryRow.getStyleClass().add("duplicate-summary-row");

        VBox resultsContainer = new VBox(12);

        resultsContainer.getStyleClass().add("duplicate-results");

        Label duplicateGroupsTitle =
                new Label("Duplicate Groups");

        duplicateGroupsTitle.getStyleClass().add(
                "section-title"
        );

        duplicateGroupsTitle.setVisible(false);
        duplicateGroupsTitle.setManaged(false);
        
        ScrollPane resultsScrollPane = new ScrollPane(resultsContainer);

        resultsScrollPane.setFitToWidth(true);

        resultsScrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        resultsScrollPane.getStyleClass().add("duplicates-scroll");
        
        VBox.setVgrow(
                resultsScrollPane,
                Priority.ALWAYS
        );

        resultsScrollPane.setVisible(false);
        resultsScrollPane.setManaged(false);

        ProgressBar progressBar = new ProgressBar();
        
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        progressBar.setPrefWidth(320);

        progressBar.getStyleClass().add("duplicates-progress");

        if (latestScanResult == null) {

                analyseButton.setDisable(true);

                statusLabel.setText(
                        "Scan a folder before analysing duplicates."
                );

        } else if (duplicateAnalysisCompleted) {

                analyseButton.setText(
                        "Analyse Again"
                );

                if (latestDuplicateGroups.isEmpty()) {

                        statusLabel.setText(
                                "No duplicate files found."
                        );

                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);

                } else {

                        statusLabel.setVisible(false);
                        statusLabel.setManaged(false);
                }

        } else {

                statusLabel.setText(
                        "No duplicate analysis yet."
                );
        }

        analyseButton.setOnAction(actionEvent  -> {
                if (duplicateScanning) {
                        if (currentDuplicateTask != null) {
                                currentDuplicateTask.cancel();
                        }

                        return;
                }

                Task<List<DuplicateGroup>> duplicateTask = new Task<>() {

                        @Override 
                        protected List<DuplicateGroup> call() {
                                return duplicateService.findDuplicates(
                                        latestScanResult.getItems(),

                                        this::isCancelled,

                                        (processed, total) -> {
                                                if (total > 0) {
                                                        updateProgress(
                                                                processed,
                                                                total
                                                        );
                                                }

                                                updateMessage(
                                                        total == 0
                                                                ? "No duplicate candidates found."
                                                                : String.format(
                                                                        "Hashing... %,d / %,d files",
                                                                        processed,
                                                                        total
                                                                )       
                                                );
                                        }
                                );
                        }
                };

                progressBar.progressProperty().bind(
                        duplicateTask.progressProperty()
                );

                statusLabel.textProperty().bind(
                        duplicateTask.messageProperty()
                );

                duplicateScanning = true;

                analyseButton.setText("Cancel Analysis");

                progressBar.setVisible(true);
                progressBar.setManaged(true);

                duplicateTask.setOnSucceeded(event -> {
                        progressBar.progressProperty().unbind();
                        statusLabel.textProperty().unbind();

                        duplicateScanning = false;
                        currentDuplicateTask = null;

                        latestDuplicateGroups = duplicateTask.getValue();

                        duplicateAnalysisCompleted = true;

                        populateDuplicateSummary(
                                duplicateSummaryRow
                        );

                        populateDuplicateResults(
                                resultsContainer
                        );

                        boolean hasResults =
                                !latestDuplicateGroups.isEmpty();

                        duplicateGroupsTitle.setVisible(
                                hasResults
                        );

                        duplicateGroupsTitle.setManaged(
                                hasResults
                        );

                        resultsScrollPane.setVisible(
                                hasResults
                        );

                        resultsScrollPane.setManaged(
                                hasResults
                        );

                        long reclaimableBytes = latestDuplicateGroups
                                                        .stream()
                                                        .mapToLong(
                                                                DuplicateGroup::getReclaimableBytes
                                                        )
                                                        .sum();

                        analyseButton.setText("Analyse Again");

                        progressBar.setVisible(false);
                        progressBar.setManaged(false);

                        if (latestDuplicateGroups.isEmpty()) {

                                statusLabel.setText(
                                        "No duplicate files found."
                                );

                                statusLabel.setVisible(true);
                                statusLabel.setManaged(true);

                        } else {

                                statusLabel.setVisible(false);
                                statusLabel.setManaged(false);
                        }
                });

                duplicateTask.setOnFailed(event -> {

                        progressBar.progressProperty().unbind();
                        statusLabel.textProperty().unbind();

                        duplicateScanning = false;
                        currentDuplicateTask = null;

                        analyseButton.setText(
                                duplicateAnalysisCompleted
                                        ? "Analyse Again"
                                        : "Analyse Duplicates"
                        );

                        progressBar.setVisible(false);
                        progressBar.setManaged(false);

                        statusLabel.setText(
                                "Duplicate analysis failed."
                        );

                        duplicateTask
                                .getException()
                                .printStackTrace();
                });

                duplicateTask.setOnCancelled(event -> {

                        progressBar.progressProperty().unbind();
                        statusLabel.textProperty().unbind();

                        duplicateScanning = false;
                        currentDuplicateTask = null;

                        analyseButton.setText(
                                duplicateAnalysisCompleted
                                        ? "Analyse Again"
                                        : "Analyse Duplicates"
                        );

                        progressBar.setVisible(false);
                        progressBar.setManaged(false);

                        statusLabel.setText(
                                "Analysis cancelled."
                        );
                });

                currentDuplicateTask = duplicateTask;

                Thread duplicateThread = new Thread(duplicateTask);

                duplicateThread.setDaemon(true);
                duplicateThread.start();
        });

        

        content.getChildren().addAll(
                heading,
                subtitle,
                analyseButton,
                progressBar,
                statusLabel,
                duplicateSummaryRow,
                duplicateGroupsTitle,
                resultsScrollPane
        );

        if (duplicateAnalysisCompleted) {

                populateDuplicateSummary(
                        duplicateSummaryRow
                );

                populateDuplicateResults(
                        resultsContainer
                );

                boolean hasResults =
                        !latestDuplicateGroups.isEmpty();

                duplicateGroupsTitle.setVisible(
                        hasResults
                );

                duplicateGroupsTitle.setManaged(
                        hasResults
                );

                resultsScrollPane.setVisible(
                        hasResults
                );

                resultsScrollPane.setManaged(
                        hasResults
                );
        }

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
        double terabytes = gigabytes / 1024.0;

        if (terabytes >= 1) {
                return String.format(
                        "%.1f TB",
                        terabytes
                );
        }

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
                .filter(item -> {
                        if (filesViewMode == FilesViewMode.ALL_FILES) {
                                return !item.isDirectory();
                        }

                        if (currentFolder == null) {
                                return false;
                        }

                        Path parent = item.getPath().getParent();

                        return parent != null && parent.equals(currentFolder);
                })
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
            case "Other" -> item.getType() == FileType.OTHER;
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
                                        fileNameLabel.getStyleClass().add("tag-file-name");

                                        Label filePathLabel = new Label(
                                                filePath.toString()
                                        );

                                        filePathLabel.getStyleClass().add("tag-file-path");

                                        VBox fileInfo = new VBox(
                                                3,
                                                fileNameLabel,
                                                filePathLabel
                                        );

                                        Node filePreview =
                                                createFilePreview(filePath);

                                        Region spacer1 = new Region();
                                        HBox.setHgrow(spacer1, Priority.ALWAYS);

                                        Button removeButton = new Button("×");
                                        removeButton.getStyleClass().add("tag-remove-button");

                                        removeButton.setOnAction(event -> {
                                                try {
                                                        tagRepo.removeTagFromFile(filePath, tag);
                                                        populateTagList(tagList);
                                                } catch (SQLException e) {
                                                        e.printStackTrace();
                                                }
                                        });

                                        Button moreButton = createFileActionsButton(filePath);

                                        HBox fileRow = new HBox(
                                                10,
                                                filePreview,
                                                fileInfo,
                                                spacer1,
                                                removeButton,
                                                moreButton
                                        );

                                        fileRow.setAlignment(Pos.CENTER_LEFT);
                                        fileRow.setMaxWidth(Double.MAX_VALUE);
                                        
                                        fileList.getChildren().add(fileRow );
                                }
                        }

                        tagLabel.getStyleClass().add("tag-group-title");
                        deleteButton.getStyleClass().add("tag-delete-button");

                        tagHeader.getStyleClass().add("tag-header");
                        fileList.getStyleClass().add("tag-file-list");

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
                        latestDuplicateGroups  = List.of();
                        duplicateAnalysisCompleted = false;

                        scanRootPath = selectedPath
                                .toAbsolutePath()
                                .normalize();

                        currentFolder = scanRootPath;

                        updateStorageCard(
                                scanRootPath
                        );

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

        private Node createFilePreview(FileItem item) {

                StackPane preview =
                        new StackPane();

                preview.setPrefSize(
                        56,
                        42
                );

                preview.setMinSize(
                        56,
                        42
                );

                preview.setMaxSize(
                        56,
                        42
                );

                preview.getStyleClass().add(
                        "file-preview"
                );


                if (item.isDirectory()) {

                        Label folderLabel =
                                new Label("Folder");

                        folderLabel.getStyleClass().add(
                                "file-preview-placeholder"
                        );

                        preview.getChildren().add(
                                folderLabel
                        );

                        return preview;
                }


                if (item.getType() == FileType.IMAGE) {

                        Image image =
                                imageThumbnailCache.computeIfAbsent(
                                        item.getPath(),
                                        path -> new Image(
                                                path.toUri().toString(),
                                                112,
                                                84,
                                                true,
                                                true,
                                                true
                                        )
                                );

                        ImageView imageView =
                                new ImageView(image);

                        imageView.setFitWidth(56);
                        imageView.setFitHeight(42);

                        imageView.setPreserveRatio(true);

                        preview.getChildren().add(
                                imageView
                        );

                        preview.getStyleClass().add(
                                "file-preview-image"
                        );

                        return preview;
                }


                if (item.getType() == FileType.VIDEO) {

                        Label playLabel =
                                new Label("▶");

                        playLabel.getStyleClass().add(
                                "file-preview-play"
                        );

                        preview.getChildren().add(
                                playLabel
                        );

                        preview.getStyleClass().add(
                                "file-preview-video"
                        );

                        return preview;
                }


                Label placeholder =
                        new Label(
                                item.getExtension()
                                        .toUpperCase()
                        );

                placeholder.getStyleClass().add(
                        "file-preview-placeholder"
                );

                preview.getChildren().add(
                        placeholder
                );

                return preview;
        }

        private FileItem findScannedFileItem(Path path) {

                if (latestScanResult == null) {
                        return null;
                }

                Path normalisedPath =
                        path.toAbsolutePath()
                                .normalize();

                return latestScanResult.getItems()
                        .stream()
                        .filter(item ->
                                item.getPath()
                                        .toAbsolutePath()
                                        .normalize()
                                        .equals(normalisedPath)
                        )
                        .findFirst()
                        .orElse(null);
        }

        private Node createFilePreview(Path path) {

                FileItem item =
                        findScannedFileItem(path);

                if (item != null) {
                        return createFilePreview(item);
                }

                StackPane preview =
                        new StackPane();

                preview.setPrefSize(
                        56,
                        42
                );

                preview.setMinSize(
                        56,
                        42
                );

                preview.setMaxSize(
                        56,
                        42
                );

                preview.getStyleClass().add(
                        "file-preview"
                );

                Label placeholder =
                        new Label("FILE");

                placeholder.getStyleClass().add(
                        "file-preview-placeholder"
                );

                preview.getChildren().add(
                        placeholder
                );

                return preview;
        }

        private void updateBrowseHeader(
                Button backButton,
                HBox breadcrumbBar,
                ListView<FileItem> fileList,
                TextField searchField,
                ComboBox<String> typeFilter,
                ComboBox<String> sortBy
        ){
                boolean atRoot = currentFolder == null || scanRootPath == null || currentFolder.equals(scanRootPath);

                backButton.setDisable(atRoot);

                breadcrumbBar.getChildren().clear();

                if (currentFolder == null || scanRootPath == null) {
                        return;
                }

                Button rootButton = new Button(getPathDisplayName(scanRootPath));
                rootButton.getStyleClass().add("breadcrumb-button");

                rootButton.setOnAction(event -> {
                        currentFolder = scanRootPath;

                        updateBrowseHeader(
                                backButton,
                                breadcrumbBar,
                                fileList,
                                searchField,
                                typeFilter,
                                sortBy
                        );

                        populateFileList(
                                fileList,
                                searchField.getText(),
                                typeFilter.getValue(),
                                sortBy.getValue()
                        );
                });

                breadcrumbBar.getChildren().add(rootButton);

                Path relativePath = scanRootPath.relativize(
                        currentFolder
                );

                Path accumulatedPath = scanRootPath;

                for (Path part : relativePath) {
                        Label separator = new Label(">");
                        separator.getStyleClass().add("breadcrumb-separator");

                        accumulatedPath = accumulatedPath.resolve(part);
                        
                        Path targetPath = accumulatedPath;

                        Button pathButton = new Button(part.toString());
                        pathButton.getStyleClass().add("breadcrumb-button");

                        pathButton.setOnAction(event -> {
                                currentFolder = targetPath;

                                updateBrowseHeader(
                                        backButton,
                                        breadcrumbBar,
                                        fileList,
                                        searchField,
                                        typeFilter,
                                        sortBy
                                );

                                populateFileList(
                                        fileList,
                                        searchField.getText(),
                                        typeFilter.getValue(),
                                        sortBy.getValue()
                                );
                        });

                        breadcrumbBar.getChildren().addAll(separator, pathButton);
                }

        }

        private String getPathDisplayName(Path path) {
                Path fileName = path.getFileName();

                if (fileName != null) {
                        return fileName.toString();
                }

                return path.toString();
        }

        private void updateFilesViewControls(HBox browseHeader) {
                boolean browseMode =
                        filesViewMode == FilesViewMode.BROWSE;

                browseHeader.setVisible(browseMode);
                browseHeader.setManaged(browseMode);
        }

        private void populateCategories(VBox categoriesContent, Consumer<FileType> onCategorySelected) {
                categoriesContent.getChildren().clear();

                if (latestScanResult == null) {
                        return;
                }

                long totalScannedBytes = latestScanResult.getItems()
                        .stream()
                        .filter(item -> !item.isDirectory())
                        .mapToLong(FileItem::getSizeBytes)
                        .sum();

                Label titleLabel = new Label("File Categories");
                titleLabel.getStyleClass().add("categories-title");

                Label subtitleLabel = new Label(
                        "Storage breakdown for this scan"
                );
                subtitleLabel.getStyleClass().add("categories-subtitle");

                categoriesContent.getChildren().addAll(
                        titleLabel,
                        subtitleLabel
                );

                Map<FileType, Long> fileCounts = new HashMap<>();
                Map<FileType, Long> totalBytesByType = new HashMap<>();

                for (FileItem item : latestScanResult.getItems()) {
                        if (item.isDirectory()) {
                                continue;
                        }

                        FileType type = item.getType();

                        fileCounts.merge(
                                type,
                                1L,
                                Long::sum
                        );

                        totalBytesByType.merge(
                                type,
                                item.getSizeBytes(),
                                Long::sum
                        );
                }

                        List<FileType> sortedTypes = List.of(FileType.values())
                                .stream()
                                .sorted(
                                        Comparator.comparingLong(
                                                (FileType type) ->
                                                        totalBytesByType.getOrDefault(type, 0L)
                                        ).reversed()
                                )
                                .toList();

                for (FileType type : sortedTypes) {

                        long fileCount = fileCounts.getOrDefault(
                                type,
                                0L
                        );

                        long totalBytes = totalBytesByType.getOrDefault(
                                type,
                                0L
                        );

                        double percentage = totalScannedBytes == 0
                                ? 0
                                : (double) totalBytes / totalScannedBytes * 100;

                        String percentageText =
                                percentage > 0 && percentage < 0.1
                                        ? "<0.1%"
                                        : String.format("%.1f%%", percentage);

                        Label percentageLabel = new Label(
                                percentageText
                        );

                        percentageLabel.getStyleClass().add(
                                "category-percentage"
                        );

                        if (fileCount == 0) {
                                continue;
                        }

                        Label nameLabel = new Label(
                                getFileTypeDisplayName(type)
                        );
                        nameLabel.getStyleClass().add("category-name");

                        Label countLabel = new Label(
                                fileCount + " files"
                        );
                        countLabel.getStyleClass().add("category-count");

                        Label sizeLabel = new Label(
                                formatSize(totalBytes)
                        );
                        sizeLabel.getStyleClass().add("category-size");

                        Label arrowLabel = new Label("›");
                        arrowLabel.getStyleClass().add("category-arrow");

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        HBox categoryRow = new HBox(
                                16,
                                nameLabel,
                                spacer,
                                countLabel,
                                sizeLabel,
                                percentageLabel,
                                arrowLabel
                        );

                        categoryRow.setAlignment(Pos.CENTER_LEFT);
                        categoryRow.setMaxWidth(Double.MAX_VALUE);
                        categoryRow.getStyleClass().add("category-row");
                        
                        categoryRow.setOnMouseClicked(event ->
                                onCategorySelected.accept(type)
                        );

                        categoriesContent.getChildren().add(categoryRow);
                }
        }

        private String getFileTypeDisplayName(FileType type) {
                return switch (type) {
                        case IMAGE -> "Images";
                        case VIDEO -> "Videos";
                        case AUDIO -> "Audio";
                        case DOCUMENT -> "Documents";
                        case ARCHIVE -> "Archives";
                        case CODE -> "Code";
                        case OTHER -> "Other";
                };
        }

        private String formatModifiedTime(
                LocalDateTime dateTime
        ) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "dd MMM yyyy, HH:mm"
                );

        return dateTime.format(formatter);
        }

        private VBox createCollapsibleSection(
                String title,
                int itemCount,
                VBox content
        ) {
        Label arrowLabel = new Label("›");

        Label titleLabel = new Label(
                title + " (" + itemCount + ")"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(
                8,
                arrowLabel,
                titleLabel,
                spacer
        );

        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add(
                "collapsible-section-header"
        );

        arrowLabel.getStyleClass().add(
                "collapsible-section-arrow"
        );

        titleLabel.getStyleClass().add(
                "collapsible-section-title"
        );

        content.setVisible(false);
        content.setManaged(false);

        header.setOnMouseClicked(event -> {
                boolean expanded = content.isVisible();

                content.setVisible(!expanded);
                content.setManaged(!expanded);

                arrowLabel.setText(
                        expanded ? "›" : "⌄"
                );
        });

        VBox section = new VBox(
                6,
                header,
                content
        );

        return section;
        }
        

        private void populateDuplicateResults(
                VBox resultsContainer
        ) {
                resultsContainer.getChildren().clear();

                if (!duplicateAnalysisCompleted) {
                        return;
                }

                if (latestDuplicateGroups.isEmpty()) {
                        return;
                }

                List<DuplicateGroup> sortedGroups =
                        latestDuplicateGroups
                                .stream()
                                .sorted(
                                        Comparator.comparingLong(
                                                DuplicateGroup::getReclaimableBytes
                                        ).reversed()
                                )
                                .toList();

                for (DuplicateGroup group : sortedGroups) {

                        Label summaryLabel = new Label(
                                String.format(
                                        "%,d copies · %s each · %s reclaimable",
                                        group.getFiles().size(),
                                        formatSize(
                                                group.getFileSizeBytes()
                                        ),
                                        formatSize(
                                                group.getReclaimableBytes()
                                        )
                                )
                        );

                        summaryLabel.getStyleClass().add(
                                "duplicate-group-summary"
                        );

                        VBox fileList = new VBox(6);

                        fileList.setVisible(false);
                        fileList.setManaged(false);

                        Label arrowLabel = new Label("›");

                        arrowLabel.getStyleClass().add("duplicate-group-arrow");

                        Region headerSpacer = new Region();

                        HBox.setHgrow(
                                headerSpacer,
                                Priority.ALWAYS
                        );

                        HBox groupHeader = new HBox(
                                10,
                                arrowLabel,
                                summaryLabel,
                                headerSpacer
                        );

                        groupHeader.setAlignment(Pos.CENTER_LEFT);

                        groupHeader.getStyleClass().add("duplicate-group-header");

                        groupHeader.setOnMouseClicked(event -> {
                                boolean expanded =
                                        fileList.isVisible();

                                if (!expanded
                                        && fileList.getChildren().isEmpty()) {

                                        populateDuplicateGroupFiles(
                                                fileList,
                                                group
                                        );
                                }

                                fileList.setVisible(
                                        !expanded
                                );

                                fileList.setManaged(
                                        !expanded
                                );

                                arrowLabel.setText(
                                        expanded ? "›" : "⌄"
                                );
                        });

                        VBox groupCard = new VBox(
                                0,
                                groupHeader,
                                fileList
                        );

                        groupCard.getStyleClass().add("duplicate-group-card");

                        resultsContainer.getChildren().add(groupCard);

                }
        }
        
        private void populateDuplicateSummary(
                HBox summaryRow
        ) {
                summaryRow.getChildren().clear();

                if (!duplicateAnalysisCompleted) {
                        summaryRow.setVisible(false);
                        summaryRow.setManaged(false);
                        return;
                }

                long extraCopies =
                        latestDuplicateGroups
                                .stream()
                                .mapToLong(
                                        group ->
                                                group.getFiles().size() - 1L
                                )
                                .sum();

                long reclaimableBytes =
                        latestDuplicateGroups
                                .stream()
                                .mapToLong(
                                        DuplicateGroup::getReclaimableBytes
                                )
                                .sum();

                VBox groupsCard = createSummaryCard(
                        "Groups",
                        String.format(
                                "%,d",
                                latestDuplicateGroups.size()
                        )
                );

                VBox copiesCard = createSummaryCard(
                        "Extra Copies",
                        String.format(
                                "%,d",
                                extraCopies
                        )
                );

                VBox reclaimableCard = createSummaryCard(
                        "Reclaimable",
                        formatSize(reclaimableBytes)
                );

                HBox.setHgrow(
                        groupsCard,
                        Priority.ALWAYS
                );

                HBox.setHgrow(
                        copiesCard,
                        Priority.ALWAYS
                );

                HBox.setHgrow(
                        reclaimableCard,
                        Priority.ALWAYS
                );

                groupsCard.setMaxWidth(
                        Double.MAX_VALUE
                );

                copiesCard.setMaxWidth(
                        Double.MAX_VALUE
                );

                reclaimableCard.setMaxWidth(
                        Double.MAX_VALUE
                );

                summaryRow.getChildren().addAll(
                        groupsCard,
                        copiesCard,
                        reclaimableCard
                );

                summaryRow.setVisible(true);
                summaryRow.setManaged(true);
        }

        private void populateDuplicateGroupFiles(
                VBox fileList,
                DuplicateGroup group
        ) {

                for (FileItem item : group.getFiles()) {

                        Label nameLabel =
                                new Label(item.getName());

                        nameLabel.getStyleClass().add(
                                "file-name"
                        );

                        Label pathLabel =
                                new Label(
                                        item.getPath().toString()
                                );

                        pathLabel.getStyleClass().add(
                                "duplicate-file-path"
                        );

                        VBox fileInfo = new VBox(
                                3,
                                nameLabel,
                                pathLabel
                        );

                        Node filePreview =
                                createFilePreview(item);

                        Region spacer = new Region();

                        HBox.setHgrow(
                                spacer,
                                Priority.ALWAYS
                        );

                        Button moreButton =
                                createFileActionsButton(
                                        item.getPath()
                                );

                        HBox fileRow = new HBox(
                                12,
                                filePreview,
                                fileInfo,
                                spacer,
                                moreButton
                        );

                        fileRow.setAlignment(
                                Pos.CENTER_LEFT
                        );

                        fileRow.getStyleClass().add(
                                "duplicate-file-row"
                        );

                        fileList.getChildren().add(
                                fileRow
                        );
                }
        }
}
