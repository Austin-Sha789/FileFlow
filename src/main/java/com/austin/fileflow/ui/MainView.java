package com.austin.fileflow.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.SeparatorMenuItem;
import javafx.geometry.Side;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.geometry.Pos;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import javafx.animation.PauseTransition;

import javafx.util.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.Locale;


import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.prefs.Preferences;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.ScanResult;
import com.austin.fileflow.model.ScanHistoryEntry;
import com.austin.fileflow.model.StorageVolume;
import com.austin.fileflow.service.FileScanService;
import com.austin.fileflow.service.StorageService;
import com.austin.fileflow.ui.component.FilePreviewFactory;
import com.austin.fileflow.util.FileSizeFormatter;
import com.austin.fileflow.model.FileType;
import com.austin.fileflow.repository.FavouriteRepo;
import com.austin.fileflow.repository.TagRepo;
import com.austin.fileflow.repository.ScanHistoryRepo;
import com.austin.fileflow.platform.FilePlatformService;
import com.austin.fileflow.model.DuplicateGroup;
import com.austin.fileflow.service.DuplicateService;
import com.austin.fileflow.model.ShortcutAction;
import com.austin.fileflow.service.ShortcutManager;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

public class MainView {
    
    private final BorderPane root;
    private final PauseTransition dashboardClickDelay =
        new PauseTransition(Duration.millis(280));
    private final Label pageTitle;
    private final VBox pageContent;
    private final Button scanButton;
    private final Button refreshButton;
    private final FileScanService fileScanService;
    private ScanResult latestScanResult;
    private final DuplicateService duplicateService;
    private final StorageService storageService;
    private final FilePreviewFactory filePreviewFactory;
    private List<DuplicateGroup> latestDuplicateGroups  = List.of();
    private boolean duplicateScanning;
    private Task<List<DuplicateGroup>> currentDuplicateTask;
    private final FavouriteRepo favouriteRepo;
    private final TagRepo tagRepo;
    private final ScanHistoryRepo scanHistoryRepo;
    private final ShortcutManager shortcutManager;
    private TextField searchField;
    private Label filesNavItem;
    private EventHandler<KeyEvent> activeShortcutCaptureHandler;
    private Scene activeShortcutCaptureScene;
    private Label activeShortcutCaptureLabel;
    private Button activeShortcutCaptureButton;
    private ShortcutAction activeShortcutCaptureAction;
    private String activeShortcutOriginalText;
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
    private static final String CONFIRM_BEFORE_DELETE_KEY = "confirmBeforePermanentDelete";
    private final Preferences preferences = Preferences.userNodeForPackage(MainView.class);
    private FilesViewMode filesViewMode = FilesViewMode.BROWSE;
    private boolean duplicateAnalysisCompleted;
    private final Map<Path, FileItem> scannedItemsByPath =
        new HashMap<>();


    private enum FilesViewMode {
        BROWSE,
        ALL_FILES,
        CATEGORIES
        }

    public MainView(
                FavouriteRepo favouriteRepo, 
                TagRepo tagRepo, 
                ScanHistoryRepo scanHistoryRepo,
                ShortcutManager shortcutManager,
                FilePlatformService filePlatformService,
                FileScanService fileScanService,
                DuplicateService duplicateService,
                StorageService storageService,
                FilePreviewFactory filePreviewFactory
        ) {
        this.favouriteRepo = favouriteRepo;
        this.tagRepo = tagRepo;
        this.scanHistoryRepo = scanHistoryRepo;
        this.shortcutManager = shortcutManager;
        this.filePlatformService = filePlatformService;
        this.fileScanService = fileScanService;
        this.duplicateService = duplicateService;
        this.storageService = storageService;
        this.filePreviewFactory = filePreviewFactory;

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
        refreshButton = new Button("Refresh");
        refreshButton.setDisable(true);

        root.setLeft(createSidebar());
        root.setCenter(createContent());

        root.addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {

                if (shortcutManager.matches(
                        ShortcutAction.REFRESH_SCAN,
                        event
                )) {

                        if (scanRootPath != null
                                && !scanning) {

                                refreshCurrentScan();
                        }

                        event.consume();
                        return;
                }

                if (shortcutManager.matches(
                        ShortcutAction.FOCUS_SEARCH,
                        event
                )) {

                        selectNavItem(
                                filesNavItem,
                                "Files"
                        );

                        if (searchField != null) {
                                searchField.requestFocus();
                        }

                        event.consume();
                        return;
                }

                if (shortcutManager.matches(
                        ShortcutAction.SCAN_FOLDER,
                        event
                )) {

                        if (!scanning) {
                                handleScan();
                        }

                        event.consume();
                }
                }
        );
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
                FileSizeFormatter.format(volume.getUsedBytes())
                        + " used · "
                        + FileSizeFormatter.format(volume.getTotalBytes())
                        + " total"
        );

        storageFreeLabel.setText(
                FileSizeFormatter.format(volume.getFreeBytes())
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
                        FileSizeFormatter.format(
                                volume.getUsedBytes()
                        )
                                + " used · "
                                + FileSizeFormatter.format(
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
                        FileSizeFormatter.format(
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

        refreshButton.setOnAction(
                event -> refreshCurrentScan()
        );

        refreshButton.getStyleClass().add(
                "secondary-button"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(
                12,
                pageTitle,
                spacer,
                scanStatusLabel,
                scanProgressBar,
                refreshButton,
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

        pageContent.getChildren().clear();

        root.setRight(null);

        scanButton.setVisible(!title.equals("Home"));
        scanButton.setManaged(!title.equals("Home"));

        refreshButton.setVisible(!title.equals("Home"));
        refreshButton.setManaged(!title.equals("Home"));

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
        VBox storageCard = createSummaryCard("Storage", FileSizeFormatter.format(totalBytes));
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
                        filePreviewFactory.create(item);

                Label sizeLabel = new Label(FileSizeFormatter.format(item.getSizeBytes()));
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

                row.setOnContextMenuRequested(event -> {

                        ContextMenu fileMenu =
                                createFileContextMenu(
                                        item.getPath()
                                );

                        fileMenu.show(
                                row,
                                event.getScreenX(),
                                event.getScreenY()
                        );

                        event.consume();
                });

                configureDashboardFileRow(row, item.getPath());

                moreButton.addEventFilter(
                        MouseEvent.MOUSE_CLICKED,
                        event -> event.consume()
                );

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
                        FileSizeFormatter.format(bytes)
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
                                FileSizeFormatter.format(totalBytes)
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

                Label sizeLabel = new Label(FileSizeFormatter.format(bytes));
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
                        filePreviewFactory.create(item);

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

                row.setOnContextMenuRequested(event -> {

                        ContextMenu fileMenu =
                                createFileContextMenu(
                                        item.getPath()
                                );

                        fileMenu.show(
                                row,
                                event.getScreenX(),
                                event.getScreenY()
                        );

                        event.consume();
                });

                configureDashboardFileRow(row, item.getPath());

                moreButton.addEventFilter(
                        MouseEvent.MOUSE_CLICKED,
                        event -> event.consume()
                );

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

        searchField = new TextField();
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

        HBox viewBar = new HBox(
                10,
                browseButton,
                allFilesButton,
                categoriesButton
        );

        viewBar.setAlignment(Pos.CENTER_LEFT);

        controlsRow.setAlignment(Pos.CENTER_LEFT);

        VBox toolbar = new VBox(
                10,
                viewBar,
                controlsRow
        );

        toolbar.setMaxWidth(Double.MAX_VALUE);

        filesContent.getChildren().add(toolbar);
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

        boolean metadataLoadFailed = false;

        Set<Path> favouritePaths = new HashSet<>();

        try {
                favouritePaths.addAll(favouriteRepo.getAll());
        } catch (SQLException e) {
                metadataLoadFailed = true;

                e.printStackTrace();
        }

        Map<Path, List<String>> fileTags = new HashMap<>();

        try {
                fileTags.putAll(tagRepo.getAllFileTags());
        } catch (SQLException e) {
                metadataLoadFailed = true;
                e.printStackTrace();
        }

        List<String> existingTags = new ArrayList<>();

        try {
                existingTags.addAll(tagRepo.getAll());
        } catch (SQLException e) {
                metadataLoadFailed = true;
                e.printStackTrace();
        }

        if (metadataLoadFailed) {
                showStatusWarning(
                        "Files loaded, but some saved metadata could not be loaded."
                );
        }

        ListView<FileItem> fileList = new ListView<>();

        PauseTransition quickInfoDelay =
                new PauseTransition(Duration.millis(280));

        fileList.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldItem, newItem) -> {

                quickInfoDelay.stop();

                if (newItem == null) {
                        return;
                }

                quickInfoDelay.setOnFinished(event -> {

                        if ("Files".equals(pageTitle.getText())
                                && fileList.getSelectionModel()
                                        .getSelectedItem() == newItem) {

                        showQuickInfo(newItem.getPath());
                        }
                });

                quickInfoDelay.playFromStart();
        });

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
                                        : FileSizeFormatter.format(item.getSizeBytes())
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
                                filePreviewFactory.create(item);

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
                                        showDatabaseError(
                                                "Unable to Update Favourite",
                                                "FileFlow could not save this favourite change."
                                        );

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
                                                tagRepo.assignTagToFile(
                                                        item.getPath(),
                                                        selectedTag
                                                );

                                                if (!existingTags.contains(selectedTag)) {
                                                        existingTags.add(selectedTag);
                                                }

                                                List<String> itemTags = fileTags.computeIfAbsent(
                                                        item.getPath(),
                                                        path -> new ArrayList<>()
                                                );

                                                if (!itemTags.contains(selectedTag)) {
                                                        itemTags.add(selectedTag);
                                                }

                                                getListView().refresh();
                                        } catch (SQLException e) {
                                                showDatabaseError(
                                                        "Unable to Add Tag",
                                                        "FileFlow could not save this tag change."
                                                );

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
                                                showDatabaseError(
                                                        "Unable to Remove Tag",
                                                        "FileFlow could not remove this tag from the file."
                                                );

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

                        fileRow.setOnContextMenuRequested(event -> {

                                ContextMenu fileMenu =
                                        createFileContextMenu(
                                                item.getPath()
                                        );

                                fileMenu.show(
                                        fileRow,
                                        event.getScreenX(),
                                        event.getScreenY()
                                );

                                event.consume();
                        });

                        fileRow.setOnMouseClicked(event -> {

                        if (event.getButton() != MouseButton.PRIMARY) {
                                return;
                        }

                        Node clickedNode =
                                event.getPickResult().getIntersectedNode();

                        while (clickedNode != null && clickedNode != fileRow) {

                                if (clickedNode instanceof Button) {
                                return;
                                }

                                clickedNode = clickedNode.getParent();
                        }

                        if (event.getClickCount() == 2) {

                                quickInfoDelay.stop();

                                if (filesViewMode == FilesViewMode.BROWSE
                                        && item.isDirectory()) {

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

                                } else if (!item.isDirectory()) {

                                openFile(item.getPath());
                                }

                                event.consume();

                        }
                });

                        fileRow.setAlignment(Pos.CENTER_LEFT);
                        fileRow.getStyleClass().add("file-row");

                        setText(null);
                        setGraphic(fileRow);
                }
        });

        VBox.setVgrow(fileList, Priority.ALWAYS);

        populateFileList(
                fileList, 
                "", 
                typeFilter.getValue(), 
                sortBy.getValue()
        );

        PauseTransition searchDebounce =
                new PauseTransition(
                        Duration.millis(180)
                );

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                searchDebounce.stop();

                searchDebounce.setOnFinished(event ->
                        populateFileList(
                                fileList,
                                newValue,
                                typeFilter.getValue(),
                                sortBy.getValue()
                        )
                );

                searchDebounce.playFromStart();
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

                        Node filePreview = createStoredFilePreview(filePath);

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

                        PauseTransition clickDelay =
                        new PauseTransition(Duration.millis(280));

                        clickDelay.setOnFinished(event -> {
                                showQuickInfo(filePath);
                                });

                                row.setOnMouseClicked(event -> {

                                if (event.getButton() != MouseButton.PRIMARY) {
                                        return;
                                }

                                if (event.getClickCount() == 2) {

                                        clickDelay.stop();

                                        openFile(filePath);

                                        event.consume();

                                } else if (event.getClickCount() == 1) {

                                        clickDelay.playFromStart();
                                }
                                });

                                row.setOnContextMenuRequested(event -> {

                                ContextMenu menu =
                                        createFileContextMenu(filePath);

                                menu.show(
                                        row,
                                        event.getScreenX(),
                                        event.getScreenY()
                                );

                                event.consume();
                        });

                        favouriteButton.addEventFilter(
                                MouseEvent.MOUSE_CLICKED,
                                event -> event.consume()
                        );

                        moreButton.addEventFilter(
                                MouseEvent.MOUSE_CLICKED,
                                event -> event.consume()
                        );

                        favouriteButton.setOnAction(event -> {
                                try {
                                        favouriteRepo.remove(filePath);

                                        clickDelay.stop();
                                        
                                        content.getChildren().remove(row);

                                        root.setRight(null);

                                } catch (SQLException e) {
                                        
                                        showDatabaseError(
                                                "Unable to Update Favourite",
                                                "FileFlow could not remove this file from your favourites."
                                        );

                                        e.printStackTrace();
                                }
                        });

                        content.getChildren().add(row);
                }

        } catch(SQLException e) {
                content.getChildren().add(
                        createDatabaseErrorState(
                                "Favourites are temporarily unavailable",
                                "FileFlow could not load your favourites.",
                                this::refreshCurrentPage
                        )
                );

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

    private ScrollPane createHistoryContent() {
        VBox content = new VBox(10);
        content.getStyleClass().add("history-content");

        ScrollPane scrollPane =
                new ScrollPane(content);

        scrollPane.setFitToWidth(true);

        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scrollPane.getStyleClass().add(
                "history-scroll"
        );

        VBox.setVgrow(
                scrollPane,
                Priority.ALWAYS
        );

        DateTimeFormatter historyDateFormatter =
                DateTimeFormatter.ofPattern(
                        "dd MMM yyyy, h:mm a",
                        Locale.ENGLISH
                );

        try {

                List<ScanHistoryEntry> historyEntries =
                        scanHistoryRepo.getAll();

                if (historyEntries.isEmpty()) {

                        Label emptyLabel =
                                new Label(
                                        "No scan history yet."
                                );

                        emptyLabel.getStyleClass().add(
                                "history-empty"
                        );

                        content.getChildren().add(
                                emptyLabel
                        );

                        return scrollPane;
                }

                Button clearHistoryButton =
                        new Button("Clear History");

                clearHistoryButton.getStyleClass().add(
                        "secondary-button"
                );

                clearHistoryButton.setOnAction(event -> {

                        Alert confirmation =
                                new Alert(
                                        Alert.AlertType.CONFIRMATION
                                );

                        confirmation.setTitle(
                                "Clear History"
                        );

                        confirmation.setHeaderText(
                                "Clear all scan history?"
                        );

                        confirmation.setContentText(
                                "This will remove all scan history from FileFlow. "
                                        + "Your files will not be affected."
                        );

                        ButtonType clearButtonType =
                                new ButtonType(
                                        "Clear History",
                                        ButtonBar.ButtonData.OK_DONE
                                );

                        ButtonType cancelButtonType =
                                new ButtonType(
                                        "Cancel",
                                        ButtonBar.ButtonData.CANCEL_CLOSE
                                );

                        confirmation
                                .getButtonTypes()
                                .setAll(
                                        cancelButtonType,
                                        clearButtonType
                                );

                        ButtonType result =
                                confirmation
                                        .showAndWait()
                                        .orElse(
                                                cancelButtonType
                                        );

                        if (result != clearButtonType) {
                                return;
                        }

                        try {

                                scanHistoryRepo.clearAll();

                                refreshCurrentPage();

                        } catch (SQLException exception) {

                                showDatabaseError(
                                        "Unable to Clear History",
                                        "FileFlow could not clear your scan history."
                                );

                                exception.printStackTrace();
                        }
                });

                Region historySpacer =
                        new Region();

                HBox.setHgrow(
                        historySpacer,
                        Priority.ALWAYS
                );

                HBox historyActions =
                        new HBox(
                                10,
                                historySpacer,
                                clearHistoryButton
                        );

                historyActions.setAlignment(
                        Pos.CENTER_RIGHT
                );

                content.getChildren().add(
                        historyActions
                );

                for (ScanHistoryEntry entry : historyEntries) {

                        Label folderNameLabel =
                                new Label(
                                        getPathDisplayName(
                                                entry.getFolderPath()
                                        )
                                );

                        folderNameLabel.getStyleClass().add(
                                "history-folder-name"
                        );


                        Label pathLabel =
                                new Label(
                                        entry.getFolderPath()
                                                .toString()
                                );

                        pathLabel.getStyleClass().add(
                                "history-path"
                        );


                        String scannedTime =
                                entry.getScannedAt()
                                        .format(
                                                historyDateFormatter
                                        );

                        Label detailsLabel =
                                new Label(
                                        String.format(
                                                "Scanned %s · %,d files · %s",
                                                scannedTime,
                                                entry.getFileCount(),
                                                FileSizeFormatter.format(
                                                        entry.getTotalBytes()
                                                )
                                        )
                                );

                        detailsLabel.getStyleClass().add(
                                "history-details"
                        );


                        VBox fileInfo =
                                new VBox(
                                        4,
                                        folderNameLabel,
                                        pathLabel,
                                        detailsLabel
                                );

                        Button scanAgainButton = new Button("Scan Again");

                        scanAgainButton.getStyleClass().add("history-scan-again-button");

                        scanAgainButton.setOnAction(event -> 
                                startScan(
                                        entry.getFolderPath()
                                )
                        );

                        Button moreButton =
                                new Button(
                                        "⋯"
                                );

                        moreButton.getStyleClass().add(
                                "more-button"
                        );

                        MenuItem openFolderItem =

                                new MenuItem(
                                                "Open Folder"
                                );

                        MenuItem copyPathItem =
                                new MenuItem(
                                        "Copy Path"
                                );

                        MenuItem removeHistoryItem =
                                new MenuItem(
                                        "Remove from History"
                                );

                        ContextMenu historyMenu =
                                new ContextMenu(
                                        openFolderItem,
                                        copyPathItem,
                                        new SeparatorMenuItem(),
                                        removeHistoryItem
                                );

                        openFolderItem.setOnAction(event -> {

                                try {

                                        filePlatformService.open(
                                                entry.getFolderPath()
                                        );

                                } catch (IOException exception) {

                                        exception.printStackTrace();
                                }
                        });

                        copyPathItem.setOnAction(event -> {

                                try {

                                        filePlatformService.copyPath(
                                                entry.getFolderPath()
                                        );

                                } catch (IOException exception) {

                                        exception.printStackTrace();
                                }
                        });

                        removeHistoryItem.setOnAction(event -> {

                                try {

                                        scanHistoryRepo.remove(
                                                entry.getId()
                                        );

                                        refreshCurrentPage();

                                } catch (SQLException exception) {

                                        exception.printStackTrace();
                                }
                        });

                        moreButton.setOnAction(event ->
                                historyMenu.show(
                                        moreButton,
                                        Side.BOTTOM,
                                        0,
                                        0
                                )
                        );

                        Region spacer =
                                new Region();

                        HBox.setHgrow(
                                spacer,
                                Priority.ALWAYS
                        );

                        HBox actionsRow =
                                new HBox(
                                        10,
                                        spacer,
                                        scanAgainButton,
                                        moreButton
                                );

                        actionsRow.setAlignment(
                                Pos.CENTER_RIGHT
                        );

                        VBox historyCard =
                                new VBox(
                                        8,
                                        fileInfo,
                                        actionsRow
                                );

                        historyCard.getStyleClass().add(
                                "history-card"
                        );

                        content.getChildren().add(
                                historyCard
                        );
                }

        } catch (SQLException exception) {
                content.getChildren().add(
                        createDatabaseErrorState(
                                "History is temporarily unavailable",
                                "FileFlow could not load your scan history.",
                                this::refreshCurrentPage
                        )
                );

                exception.printStackTrace();
        }
                return scrollPane;
        }

    private VBox createSettingsContent() {
        VBox content = new VBox(18);
        content.getStyleClass().add(
                "settings-content"
        );

        Label safetyTitle = new Label("Safety");

        safetyTitle.getStyleClass().add("settings-section-title");

        CheckBox confirmDeleteCheckBox =
                new CheckBox(
                        "Confirm before permanently deleting files"
                );

        boolean confirmBeforeDelete =
                preferences.getBoolean(
                        CONFIRM_BEFORE_DELETE_KEY,
                        true
                );

        confirmDeleteCheckBox.setSelected(
                confirmBeforeDelete
        );

        Label confirmDeleteDescription =
                new Label(
                        "When enabled, FileFlow asks for confirmation "
                                + "before permanently deleting a file."
                );

        confirmDeleteDescription.setWrapText(
                true
        );

        confirmDeleteDescription
                .getStyleClass()
                .add(
                        "settings-description"
        );

        Label deleteWarning =
                new Label(
                        "Deleted files are not moved to the Recycle Bin "
                                + "and cannot be restored by FileFlow."
                );

        deleteWarning.setWrapText(
                true
        );

        deleteWarning
                .getStyleClass()
                .add(
                        "settings-warning"
        );
        confirmDeleteCheckBox
                .selectedProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                        preferences.putBoolean(
                                CONFIRM_BEFORE_DELETE_KEY,
                                newValue
                        );
                        }
                );

        VBox safetyCard =
                new VBox(
                        10,
                        confirmDeleteCheckBox,
                        confirmDeleteDescription,
                        deleteWarning
                );

        safetyCard.getStyleClass().add(
                "settings-card"
        );

        content.getChildren().addAll(
                safetyTitle,
                safetyCard,
                createKeyboardShortcutsSettingsCard()
        );

        return content;
    }

    private VBox createKeyboardShortcutsSettingsCard() {

                Label titleLabel =
                        new Label(
                                "Keyboard Shortcuts"
                        );
                titleLabel.getStyleClass().add(
                        "settings-card-title"
                );

                Label descriptionLabel =
                        new Label(
                                "Customise shortcuts for common FileFlow actions."
                        );
                descriptionLabel.getStyleClass().add(
                        "settings-card-description"
                );

                VBox shortcuts =
                        new VBox(
                                8,
                                createShortcutSettingRow(
                                        "Refresh current scan",
                                        ShortcutAction.REFRESH_SCAN
                                ),
                                createShortcutSettingRow(
                                        "Focus file search",
                                        ShortcutAction.FOCUS_SEARCH
                                ),
                                createShortcutSettingRow(
                                        "Scan a new folder",
                                        ShortcutAction.SCAN_FOLDER
                                )
                        );

                Button resetButton =
                        new Button(
                                "Reset to Defaults"
                        );

                resetButton.getStyleClass().add(
                        "shortcut-reset-button"
                );

                resetButton.setOnAction(event -> {

                        cancelActiveShortcutCapture();

                        shortcutManager.resetToDefaults();

                        refreshCurrentPage();
                });

                VBox card =
                        new VBox(
                                12,
                                titleLabel,
                                descriptionLabel,
                                shortcuts,
                                resetButton
                        );

                card.getStyleClass().add(
                        "settings-card"
                );

                return card;
        }

        private HBox createShortcutSettingRow(
                String actionName,
                ShortcutAction action
        ) {

                Label actionLabel =
                        new Label(
                                actionName
                        );

                actionLabel.getStyleClass().add(
                        "shortcut-action-label"
                );

                Region spacer =
                        new Region();

                HBox.setHgrow(
                        spacer,
                        Priority.ALWAYS
                );

                Label shortcutLabel =
                        new Label(
                                shortcutManager
                                        .getShortcut(action)
                                        .getDisplayText()
                        );

                shortcutLabel.getStyleClass().add(
                        "shortcut-key-label"
                );

                Button changeButton =
                        new Button(
                                "Change"
                        );

                changeButton.getStyleClass().add(
                        "shortcut-change-button"
                );

                changeButton.setOnAction(event ->
                        beginShortcutCapture(
                                action,
                                shortcutLabel,
                                changeButton
                        )
                );

                HBox row =
                        new HBox(
                                12,
                                actionLabel,
                                spacer,
                                shortcutLabel,
                                changeButton
                        );

                row.setAlignment(
                        Pos.CENTER_LEFT
                );

                row.getStyleClass().add(
                        "shortcut-setting-row"
                );

                return row;
        }

        private void beginShortcutCapture(
                ShortcutAction action,
                Label shortcutLabel,
                Button changeButton
        ) {

                cancelActiveShortcutCapture();

                Scene scene =
                        root.getScene();

                if (scene == null) {
                        return;
                }

                activeShortcutCaptureScene =
                        scene;

                activeShortcutCaptureAction =
                        action;

                activeShortcutCaptureLabel =
                        shortcutLabel;

                activeShortcutCaptureButton =
                        changeButton;

                activeShortcutOriginalText =
                        shortcutLabel.getText();

                shortcutLabel.setText(
                        "Press a shortcut..."
                );

                changeButton.setText(
                        "Cancel"
                );

                activeShortcutCaptureHandler =
                        event -> {

                                event.consume();

                                if (event.getCode()
                                        == KeyCode.ESCAPE) {

                                cancelActiveShortcutCapture();
                                return;
                                }

                                if (event.getCode()
                                                == KeyCode.SHIFT
                                        || event.getCode()
                                                == KeyCode.CONTROL
                                        || event.getCode()
                                                == KeyCode.ALT
                                        || event.getCode()
                                                == KeyCode.META) {

                                return;
                                }

                                if (!hasShortcutModifier(event)) {

                                shortcutLabel.setText(
                                        "Use a modifier key"
                                );

                                return;
                                }

                                KeyCombination newShortcut =
                                        createShortcutCombination(
                                                event
                                        );

                                ShortcutAction conflict =
                                        shortcutManager.findConflict(
                                                action,
                                                newShortcut
                                        );

                                if (conflict != null) {

                                shortcutLabel.setText(
                                        "Already in use"
                                );

                                return;
                                }

                                shortcutManager.setShortcut(
                                        action,
                                        newShortcut
                                );

                                shortcutLabel.setText(
                                        newShortcut.getDisplayText()
                                );

                                finishShortcutCapture();
                        };

                scene.addEventFilter(
                        KeyEvent.KEY_PRESSED,
                        activeShortcutCaptureHandler
                );

                changeButton.setOnAction(event ->
                        cancelActiveShortcutCapture()
                );
        }

        private boolean hasShortcutModifier(
                KeyEvent event
        ) {

                return event.isShortcutDown()
                        || event.isControlDown()
                        || event.isAltDown()
                        || event.isMetaDown();
        }

        private KeyCombination createShortcutCombination(
                KeyEvent event
        ) {

                List<KeyCombination.Modifier> modifiers =
                        new ArrayList<>();

                if (event.isShortcutDown()) {

                        modifiers.add(
                                KeyCombination.SHORTCUT_DOWN
                        );

                } else {

                        if (event.isControlDown()) {
                        modifiers.add(
                                KeyCombination.CONTROL_DOWN
                        );
                        }

                        if (event.isMetaDown()) {
                        modifiers.add(
                                KeyCombination.META_DOWN
                        );
                        }
                }

                if (event.isShiftDown()) {
                        modifiers.add(
                                KeyCombination.SHIFT_DOWN
                        );
                }

                if (event.isAltDown()) {
                        modifiers.add(
                                KeyCombination.ALT_DOWN
                        );
                }

                return new KeyCodeCombination(
                        event.getCode(),
                        modifiers.toArray(
                                KeyCombination.Modifier[]::new
                        )
                );
        }

        private void cancelActiveShortcutCapture() {

                if (activeShortcutCaptureLabel != null
                        && activeShortcutOriginalText != null) {

                        activeShortcutCaptureLabel.setText(
                                activeShortcutOriginalText
                        );
                }

                finishShortcutCapture();
        }

        private void finishShortcutCapture() {

                if (activeShortcutCaptureScene != null
                        && activeShortcutCaptureHandler != null) {

                        activeShortcutCaptureScene.removeEventFilter(
                                KeyEvent.KEY_PRESSED,
                                activeShortcutCaptureHandler
                        );
                }

                if (activeShortcutCaptureButton != null
                        && activeShortcutCaptureLabel != null
                        && activeShortcutCaptureAction != null) {

                        Button changeButton =
                                activeShortcutCaptureButton;

                        Label shortcutLabel =
                                activeShortcutCaptureLabel;

                        ShortcutAction action =
                                activeShortcutCaptureAction;

                        changeButton.setText(
                                "Change"
                        );

                        changeButton.setOnAction(event ->
                                beginShortcutCapture(
                                        action,
                                        shortcutLabel,
                                        changeButton
                                )
                        );
                }

                activeShortcutCaptureHandler = null;
                activeShortcutCaptureScene = null;
                activeShortcutCaptureAction = null;
                activeShortcutCaptureLabel = null;
                activeShortcutCaptureButton = null;
                activeShortcutOriginalText = null;
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
                                                        showDatabaseError(
                                                                "Unable to Delete Tag",
                                                                "FileFlow could not delete this tag."
                                                        );

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

                                        Node filePreview = createStoredFilePreview(filePath);

                                        Region spacer1 = new Region();
                                        HBox.setHgrow(spacer1, Priority.ALWAYS);

                                        Button removeButton = new Button("×");
                                        removeButton.getStyleClass().add("tag-remove-button");

                                        removeButton.setOnAction(event -> {
                                                try {
                                                        tagRepo.removeTagFromFile(filePath, tag);
                                                        populateTagList(tagList);
                                                } catch (SQLException e) {


                                                        showDatabaseError(
                                                                "Unable to Remove Tag",
                                                                "FileFlow could not remove this tag from the file."
                                                        );
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

                                        PauseTransition clickDelay =
                                                new PauseTransition(Duration.millis(280));

                                        clickDelay.setOnFinished(event -> {
                                                showQuickInfo(filePath);
                                        });

                                        fileRow.setOnMouseClicked(event -> {

                                                if (event.getButton() != MouseButton.PRIMARY) {
                                                        return;
                                                }

                                                if (event.getClickCount() == 2) {

                                                        clickDelay.stop();

                                                        openFile(filePath);

                                                        event.consume();

                                                } else if (event.getClickCount() == 1) {

                                                        clickDelay.playFromStart();
                                                }
                                        });

                                        fileRow.setOnContextMenuRequested(event -> {

                                                ContextMenu menu =
                                                        createFileContextMenu(filePath);

                                                menu.show(
                                                        fileRow,
                                                        event.getScreenX(),
                                                        event.getScreenY()
                                                );

                                                event.consume();
                                        });

                                        removeButton.addEventFilter(
                                                MouseEvent.MOUSE_CLICKED,
                                                event -> event.consume()
                                        );

                                        moreButton.addEventFilter(
                                                MouseEvent.MOUSE_CLICKED,
                                                event -> event.consume()
                                        );
                                        
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
                        tagList.getChildren().clear();

                        tagList.getChildren().add(
                                createDatabaseErrorState(
                                        "Tags are temporarily unavailable",
                                        "FileFlow could not load your tags.",
                                        () -> populateTagList(tagList)
                                )
                        );

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

                startScan(
                        selectedPath
                );

        }

        private void startScan(Path selectedPath) {
                if (scanning) {
                        return;
                }

                if (!Files.exists(selectedPath)) {

                        showScanError(
                                "Folder Not Found",
                                "This folder no longer exists.",
                                selectedPath.toString()
                        );

                        return;
                }

                if (!Files.isDirectory(selectedPath)) {

                        showScanError(
                                "Invalid Folder",
                                "The selected path is not a folder.",
                                selectedPath.toString()
                        );

                        return;
                }

                if (!Files.isReadable(selectedPath)) {

                        showScanError(
                                "Folder Cannot Be Read",
                                "FileFlow does not have permission to read this folder.",
                                selectedPath.toString()
                        );

                        return;
                }

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

                        latestScanResult = scanTask.getValue();

                        rebuildScannedItemIndex();

                        latestDuplicateGroups  = List.of();
                        duplicateAnalysisCompleted = false;

                        scanRootPath = selectedPath
                                .toAbsolutePath()
                                .normalize();

                        currentFolder = scanRootPath;

                        setScanningState(false);
                        currentScanTask = null;

                        long fileCount =
                                latestScanResult.getItems()
                                        .stream()
                                        .filter(item -> !item.isDirectory())
                                        .count();

                        long totalBytes =
                                latestScanResult.getItems()
                                        .stream()
                                        .filter(item -> !item.isDirectory())
                                        .mapToLong(FileItem::getSizeBytes)
                                        .sum();

                        try {
                                scanHistoryRepo.add(
                                        scanRootPath,
                                        LocalDateTime.now(),
                                        fileCount,
                                        totalBytes
                                );
                        } catch (SQLException exception) {
                                showStatusWarning(
                                        "Scan completed, but history could not be saved."
                                );

                                exception.printStackTrace();
                        } 

                        updateStorageCard(
                                scanRootPath
                        );

                        selectNavItem(filesNavItem, "Files");
                });

                scanTask.setOnFailed(event -> {
                        scanStatusLabel.textProperty().unbind();

                        setScanningState(false);
                        currentScanTask = null;

                        Throwable exception =
                                scanTask.getException();

                        handleScanFailure(
                                exception,
                                selectedPath
                        );
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

        private void refreshCurrentScan() {

                if (scanRootPath == null || scanning) {
                        return;
                }

                setScanningState(true);

                scanStatusLabel.setText(
                        "Refreshing..."
                );

                scanStatusLabel.setVisible(true);
                scanStatusLabel.setManaged(true);

                Task<ScanResult> refreshTask =
                        new Task<>() {

                                @Override
                                protected ScanResult call()
                                        throws Exception {

                                return fileScanService.scan(
                                        scanRootPath,
                                        this::isCancelled
                                );
                                }
                        };

                currentScanTask = refreshTask;

                refreshTask.setOnSucceeded(event -> {

                        setScanningState(false);
                        currentScanTask = null;

                        latestScanResult =
                                refreshTask.getValue();

                        rebuildScannedItemIndex();

                        filePreviewFactory.clearCache();

                        if (currentFolder == null
                                || !Files.isDirectory(currentFolder)) {

                                currentFolder = scanRootPath;
                        }

                        latestDuplicateGroups =
                                List.of();

                        duplicateAnalysisCompleted =
                                false;

                        hideRefreshStatus();

                        refreshCurrentPage();
                });

                refreshTask.setOnFailed(event -> {

                        setScanningState(false);
                        currentScanTask = null;

                        hideRefreshStatus();

                        Throwable exception =
                                refreshTask.getException();

                        handleScanFailure(
                                exception,
                                scanRootPath
                        );
                });

                refreshTask.setOnCancelled(event -> {

                        setScanningState(false);
                        currentScanTask = null;

                        hideRefreshStatus();
                });

                Thread refreshThread =
                        new Thread(
                                refreshTask
                        );

                refreshThread.setDaemon(true);
                refreshThread.start();
        }

        private VBox createDatabaseErrorState(
                String title,
                String message,
                Runnable retryAction
        ) {

                Label errorTitle =
                        new Label(
                                title
                        );

                errorTitle.getStyleClass().add(
                        "database-error-title"
                );

                Label errorMessage =
                        new Label(
                                message
                        );

                errorMessage.setWrapText(
                        true
                );

                errorMessage.getStyleClass().add(
                        "database-error-message"
                );

                Button retryButton =
                        new Button(
                                "Try Again"
                        );

                retryButton.getStyleClass().add(
                        "secondary-button"
                );

                retryButton.setOnAction(event ->
                        retryAction.run()
                );

                VBox errorState =
                        new VBox(
                                8,
                                errorTitle,
                                errorMessage,
                                retryButton
                        );

                errorState.getStyleClass().add(
                        "database-error-state"
                );

                return errorState;
        }

        private void showScanError(
                String title,
                String message,
                String details
        ) {

                Alert alert =
                        new Alert(
                                Alert.AlertType.ERROR
                        );

                alert.setTitle(
                        title
                );

                alert.setHeaderText(
                        message
                );

                alert.setContentText(
                        details
                );

                alert.showAndWait();
        }

        private void showDatabaseError(
                String title,
                String message
        ) {

                Alert alert =
                        new Alert(
                                Alert.AlertType.ERROR
                        );

                alert.setTitle(
                        title
                );

                alert.setHeaderText(
                        message
                );

                alert.setContentText(
                        "Your files have not been changed."
                );

                alert.showAndWait();
        }

        private void showStatusWarning(
                String message
        ) {

                scanStatusLabel.textProperty().unbind();

                scanStatusLabel.setText(
                        message
                );

                scanStatusLabel.setVisible(true);
                scanStatusLabel.setManaged(true);

                PauseTransition delay =
                        new PauseTransition(
                                Duration.seconds(5)
                        );

                delay.setOnFinished(event -> {

                        if (!scanning) {
                        scanStatusLabel.setVisible(false);
                        scanStatusLabel.setManaged(false);
                        }
                });

                delay.play();
        }

        private void showFileActionError(
                String title,
                String message,
                String details
        ) {

                Alert alert =
                        new Alert(
                                Alert.AlertType.ERROR
                        );

                alert.setTitle(
                        title
                );

                alert.setHeaderText(
                        message
                );

                alert.setContentText(
                        details
                );

                alert.showAndWait();
        }

        private void handleScanFailure(
                Throwable exception,
                Path selectedPath
        ) {

                if (exception instanceof AccessDeniedException) {

                        showScanError(
                                "Permission Denied",
                                "FileFlow could not access part of this folder.",
                                exception.getMessage()
                        );

                        return;
                }

                if (exception instanceof NoSuchFileException) {

                        showScanError(
                                "Folder Changed During Scan",
                                "A file or folder disappeared while FileFlow was scanning.",
                                exception.getMessage()
                        );

                        return;
                }

                if (exception instanceof IOException) {

                        showScanError(
                                "Scan Failed",
                                "FileFlow could not finish scanning this folder.",
                                selectedPath.toString()
                        );

                        return;
                }

                showScanError(
                        "Unexpected Scan Error",
                        "Something unexpected happened while scanning this folder.",
                        selectedPath.toString()
                );

                exception.printStackTrace();
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

                refreshButton.setDisable(
                        scanning || scanRootPath == null
                );

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

                ContextMenu fileMenu =
                        createFileContextMenu(
                                filePath
                        );

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

        private void openFile(
                Path filePath
        ) {

                if (!Files.exists(filePath)) {

                        showFileActionError(
                                "File No Longer Exists",
                                "This file has been moved or deleted.",
                                filePath.toString()
                        );

                        filePreviewFactory.invalidate(
                                filePath
                        );

                        cleanupDeletedFileReferences(
                                filePath
                        );

                        refreshAfterDelete();

                        return;
                }

                try {

                        filePlatformService.open(
                                filePath
                        );

                } catch (IOException exception) {

                        showFileActionError(
                                "Unable to Open File",
                                "FileFlow could not open this file.",
                                filePath.toString()
                        );

                        exception.printStackTrace();
                }
        }

        private ContextMenu createFileContextMenu(
                Path filePath
        ) {

                MenuItem openItem =
                        new MenuItem("Open");

                MenuItem showInFolderItem =
                        new MenuItem("Show in Folder");

                MenuItem copyPathItem =
                        new MenuItem("Copy Path");

                ContextMenu fileMenu =
                        new ContextMenu(
                                openItem,
                                showInFolderItem,
                                copyPathItem
                        );

                if (!Files.isDirectory(filePath)) {

                        MenuItem deleteItem =
                                new MenuItem("Delete");

                        deleteItem.getStyleClass().add(
                                "danger-menu-item"
                        );

                        deleteItem.setOnAction(event ->
                                handleDeleteFile(filePath)
                        );

                        fileMenu.getItems().addAll(
                                new SeparatorMenuItem(),
                                deleteItem
                        );
                }

                openItem.setOnAction(event ->
                        openFile(filePath)
                );

                showInFolderItem.setOnAction(event -> {

                        if (!Files.exists(filePath)) {
                                showFileActionError(
                                        "File Not Found",
                                        "This file no longer exists.",
                                        filePath.toString()
                                );
                                return;
                        }

                        try {
                                filePlatformService.showInFolder(filePath);

                        } catch (IOException exception) {
                                showFileActionError(
                                        "Unable to Show File",
                                        "Could not open the containing folder.",
                                        filePath.toString()
                                );

                                exception.printStackTrace();
                        }
                });

                copyPathItem.setOnAction(event -> {

                        try {

                        filePlatformService.copyPath(
                                filePath
                        );

                        } catch (IOException exception) {

                        exception.printStackTrace();
                        }
                });

                return fileMenu;
        }

        private boolean confirmPermanentDelete(Path filePath) {
                boolean confirmBeforeDelete = preferences.getBoolean(CONFIRM_BEFORE_DELETE_KEY, true);

                if (!confirmBeforeDelete) {
                        return true;
                }

                Alert confirmation = new Alert(
                        Alert.AlertType.WARNING
                );

                confirmation.setTitle("Delete File");

                confirmation.setHeaderText("Delete this file permanently?");

                ButtonType deleteButtonType = new ButtonType(
                        "Delete Permanently",
                        ButtonBar.ButtonData.OK_DONE
                );

                ButtonType cancelButtonType =
                        new ButtonType(
                                "Cancel",
                                ButtonBar.ButtonData.CANCEL_CLOSE
                );

                confirmation.getButtonTypes().setAll(
                        cancelButtonType,
                        deleteButtonType
                );

                Label warningLabel = new Label(
                        filePath.getFileName()
                            + "\n\n"
                            + "This action cannot be undone. "
                            + "FileFlow will permanently delete "
                            + "this file instead of moving it "
                            + "to the Recycle Bin."
                );

                warningLabel.setWrapText(true);

                CheckBox dontAskAgain = new CheckBox("Don't ask me again");

                VBox confirmationContent = new VBox(
                        12,
                        warningLabel,
                        dontAskAgain
                );

                confirmation.getDialogPane()
                        .setContent(
                                confirmationContent
                        );

                Button deleteButton = (Button) confirmation
                                                .getDialogPane()
                                                .lookupButton(deleteButtonType);

                deleteButton
                        .getStyleClass()
                        .add("danger-button");

                ButtonType result = confirmation
                                        .showAndWait()
                                        .orElse(cancelButtonType);

                if (result != deleteButtonType) {
                        return false;
                }

                if (dontAskAgain.isSelected()) {
                        preferences.putBoolean(
                                CONFIRM_BEFORE_DELETE_KEY,
                                false
                        );
                }

                return true;
        }

        private void handleDeleteFile(Path filePath) {

                if (!Files.exists(filePath)) {

                        showFileActionError(
                                "File No Longer Exists",
                                "This file has already been moved or deleted.",
                                filePath.toString()
                        );

                        filePreviewFactory.invalidate(filePath);

                        cleanupDeletedFileReferences(
                                filePath
                        );

                        refreshAfterDelete();

                        return;
                }

                if (!confirmPermanentDelete(filePath)) {
                        return;
                }

                try {
                        filePlatformService.deleteFile(filePath);

                        filePreviewFactory.invalidate(filePath);

                        cleanupDeletedFileReferences(
                                filePath
                        );

                        refreshAfterDelete();
                        
                } catch (IOException exception) {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);

                        errorAlert.setTitle("Delete Failed");

                        errorAlert.setHeaderText("Unable to delete the file.");

                        errorAlert.setContentText(exception.getMessage());

                        errorAlert.showAndWait();
                }
        }

        private void cleanupDeletedFileReferences(
                        Path filePath
                ) {

                try {

                        favouriteRepo.remove(
                                filePath
                        );

                        List<String> tags =
                                tagRepo.getTagsForFile(
                                        filePath
                                );

                        for (String tag : tags) {

                        tagRepo.removeTagFromFile(
                                filePath,
                                tag
                        );
                        }

                } catch (SQLException exception) {

                        exception.printStackTrace();
                }
        }

        private void refreshAfterDelete() {
                refreshAfterDelete(true);
        }

        private void refreshAfterDelete(boolean reanalyseDuplicates) {

                if (scanRootPath == null) {

                        refreshCurrentPage();
                        return;
                }


                boolean refreshDuplicates =
                        duplicateAnalysisCompleted && reanalyseDuplicates;


                scanStatusLabel.setText(
                        "Refreshing..."
                );

                scanStatusLabel.setVisible(
                        true
                );

                scanStatusLabel.setManaged(
                        true
                );


                Task<ScanResult> refreshTask =
                        new Task<>() {

                                @Override
                                protected ScanResult call()
                                        throws Exception {

                                return fileScanService.scan(
                                        scanRootPath,
                                        this::isCancelled
                                );
                                }
                        };

                currentScanTask = refreshTask;


                refreshTask.setOnSucceeded(event -> {

                        latestScanResult =
                                refreshTask.getValue();

                        rebuildScannedItemIndex();

                        if (refreshDuplicates) {

                        refreshDuplicatesAfterDelete();

                        } else {

                        hideRefreshStatus();

                        refreshCurrentPage();
                        }
                });


                refreshTask.setOnFailed(event -> {

                        hideRefreshStatus();

                        refreshTask
                                .getException()
                                .printStackTrace();

                        refreshCurrentPage();
                });


                Thread refreshThread =
                        new Thread(
                                refreshTask
                        );

                refreshThread.setDaemon(
                        true
                );

                refreshThread.start();
        }

        private void refreshDuplicatesAfterDelete() {

                Task<List<DuplicateGroup>> duplicateRefreshTask =
                        new Task<>() {

                                @Override
                                protected List<DuplicateGroup> call() {

                                return duplicateService.findDuplicates(
                                        latestScanResult.getItems(),
                                        this::isCancelled
                                );
                                }
                        };


                duplicateRefreshTask.setOnSucceeded(
                        event -> {

                                latestDuplicateGroups =
                                        duplicateRefreshTask
                                                .getValue();

                                duplicateAnalysisCompleted =
                                        true;

                                hideRefreshStatus();

                                refreshCurrentPage();
                        }
                );


                duplicateRefreshTask.setOnFailed(
                        event -> {

                                hideRefreshStatus();

                                duplicateRefreshTask
                                        .getException()
                                        .printStackTrace();

                                duplicateAnalysisCompleted =
                                        false;

                                latestDuplicateGroups =
                                        List.of();

                                refreshCurrentPage();
                        }
                );


                Thread duplicateRefreshThread =
                        new Thread(
                                duplicateRefreshTask
                        );

                duplicateRefreshThread.setDaemon(
                        true
                );

                duplicateRefreshThread.start();
        }

        private void refreshCurrentPage() {

                String currentTitle =
                        pageTitle.getText();

                navItems.stream()
                        .filter(navItem ->
                                navItem.getText()
                                        .equals(
                                                currentTitle
                                        )
                        )
                        .findFirst()
                        .ifPresent(navItem ->
                                selectNavItem(
                                        navItem,
                                        currentTitle
                                )
                        );
        }

        private void hideRefreshStatus() {

                scanStatusLabel.setVisible(
                        false
                );

                scanStatusLabel.setManaged(
                        false
                );

                scanStatusLabel.setText("");
        }

        private FileItem findScannedFileItem(Path path) {

                if (latestScanResult == null) {
                        return null;
                }

                Path normalisedPath =
                        path.toAbsolutePath()
                                .normalize();

                return scannedItemsByPath.get(
                        normalisedPath
                );
        }

        private void rebuildScannedItemIndex() {

                scannedItemsByPath.clear();

                if (latestScanResult == null) {
                        return;
                }

                for (FileItem item : latestScanResult.getItems()) {

                        Path normalisedPath =
                                item.getPath()
                                        .toAbsolutePath()
                                        .normalize();

                        scannedItemsByPath.put(
                                normalisedPath,
                                item
                        );
                }
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
                                FileSizeFormatter.format(totalBytes)
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
                                        FileSizeFormatter.format(
                                                group.getFileSizeBytes()
                                        ),
                                        FileSizeFormatter.format(
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

                        Button resolveButton =
                                new Button("Resolve");

                        resolveButton.getStyleClass().add(
                                "secondary-button"
                        );
                        
                        resolveButton.setOnMouseClicked(
                                event -> event.consume()
                        );

                        resolveButton.setOnAction(event -> {

                        List<Path> duplicatePaths =
                                group.getFiles()
                                        .stream()
                                        .map(FileItem::getPath)
                                        .toList();

                        ChoiceDialog<Path> dialog =
                                new ChoiceDialog<>(
                                        duplicatePaths.get(0),
                                        duplicatePaths
                                );

                        dialog.setTitle(
                                "Resolve Duplicates"
                        );

                        dialog.setHeaderText(
                                "Choose the copy you want to keep"
                        );

                        dialog.setContentText(
                                "Keep:"
                        );

                        dialog.showAndWait()
                                .ifPresent(pathToKeep -> {

                                long copiesToDelete =
                                        duplicatePaths.size() - 1L;

                                long reclaimableBytes =
                                        group.getReclaimableBytes();

                                Alert confirmation =
                                        new Alert(
                                                Alert.AlertType.WARNING
                                        );

                                confirmation.setTitle(
                                        "Resolve Duplicates"
                                );

                                confirmation.setHeaderText(
                                        "Keep this copy and delete the others?"
                                );

                                confirmation.setContentText(
                                        String.format(
                                                "FileFlow will keep:%n%n%s%n%n"
                                                        + "and permanently delete %,d duplicate copy/copies.%n"
                                                        + "About %s will be reclaimed.",
                                                pathToKeep,
                                                copiesToDelete,
                                                FileSizeFormatter.format(
                                                        reclaimableBytes
                                                )
                                        )
                                );

                                ButtonType resolveButtonType =
                                        new ButtonType(
                                                "Delete Duplicates",
                                                ButtonBar.ButtonData.OK_DONE
                                        );

                                ButtonType cancelButtonType =
                                        new ButtonType(
                                                "Cancel",
                                                ButtonBar.ButtonData.CANCEL_CLOSE
                                        );

                                confirmation
                                        .getButtonTypes()
                                        .setAll(
                                                cancelButtonType,
                                                resolveButtonType
                                        );

                                ButtonType result =
                                        confirmation
                                                .showAndWait()
                                                .orElse(
                                                        cancelButtonType
                                                );

                                if (result != resolveButtonType) {
                                        return;
                                }

                                for (Path duplicatePath : duplicatePaths) {

                                        if (duplicatePath.equals(pathToKeep)) {
                                                continue;
                                        }

                                        try {

                                                filePlatformService.deleteFile(
                                                        duplicatePath
                                                );

                                                filePreviewFactory.invalidate(
                                                        duplicatePath
                                                );

                                                cleanupDeletedFileReferences(
                                                        duplicatePath
                                                );

                                        } catch (IOException exception) {

                                                showFileActionError(
                                                        "Unable to Resolve Duplicates",
                                                        "FileFlow could not delete one of the duplicate files.",
                                                        duplicatePath.toString()
                                                );

                                                exception.printStackTrace();

                                                refreshAfterDelete();

                                                return;
                                        }
                                }

                                latestDuplicateGroups = latestDuplicateGroups
                                        .stream()
                                        .filter(existingGroup -> existingGroup != group)
                                        .toList();

                                duplicateAnalysisCompleted = true;

                                refreshCurrentPage();

                                refreshAfterDelete(false);
                                });
                        }); 

                        HBox groupHeader = new HBox(
                                10,
                                arrowLabel,
                                summaryLabel,
                                headerSpacer,
                                resolveButton
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
                        FileSizeFormatter.format(reclaimableBytes)
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
                                filePreviewFactory.create(item);

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

        private void configureDashboardFileRow(HBox row, Path filePath) {

                row.setOnMouseClicked(event -> {

                        if (event.getButton() != MouseButton.PRIMARY) {
                        return;
                        }

                        // Ignore buttons inside the file row
                        Node target = event.getPickResult().getIntersectedNode();

                        while (target != null && target != row) {

                        if (target instanceof Button) {
                                dashboardClickDelay.stop();
                                return;
                        }

                        target = target.getParent();
                        }

                        if (event.getClickCount() == 2) {

                        dashboardClickDelay.stop();
                        openFile(filePath);
                        event.consume();

                        } else if (event.getClickCount() == 1) {

                        dashboardClickDelay.stop();

                        dashboardClickDelay.setOnFinished(done -> {

                                if (row.getScene() != null
                                        && "Dashboard".equals(pageTitle.getText())) {

                                showQuickInfo(filePath);
                                }
                        });

                        dashboardClickDelay.playFromStart();
                        }
                });
        }

        private void showQuickInfo(Path filePath) {

                Path path = filePath.toAbsolutePath().normalize();

                Label title = new Label("Quick Info");
                title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");

                Button closeButton = new Button("×");
                closeButton.setOnAction(event -> root.setRight(null));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox header = new HBox(10, title, spacer, closeButton);
                header.setAlignment(Pos.CENTER_LEFT);

                String fileName = path.getFileName() == null
                        ? path.toString()
                        : path.getFileName().toString();

                Label nameLabel = new Label(fileName);
                nameLabel.setWrapText(true);
                nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

                String type;

                if (Files.isDirectory(path)) {

                        type = "Folder";

                        } else {

                        FileItem scannedItem = findScannedFileItem(path);

                        if (scannedItem != null) {

                                type = switch (scannedItem.getType()) {
                                case IMAGE -> "Image";
                                case VIDEO -> "Video";
                                case AUDIO -> "Audio";
                                case DOCUMENT -> "Document";
                                case ARCHIVE -> "Archive";
                                case CODE -> "Code";
                                default -> "File";
                                };

                        } else {

                                type = "File";

                                try {
                                String mimeType = Files.probeContentType(path);

                                if (mimeType != null) {
                                        if (mimeType.startsWith("image/")) {
                                        type = "Image";
                                        } else if (mimeType.startsWith("video/")) {
                                        type = "Video";
                                        } else if (mimeType.startsWith("audio/")) {
                                        type = "Audio";
                                        }
                                }

                                } catch (IOException exception) {
                                }
                        }
                }
                String size = "Unavailable";
                String modified = "Unavailable";

                if (Files.exists(path)) {
                        try {
                        size = Files.isDirectory(path)
                                ? "—"
                                : FileSizeFormatter.format(Files.size(path));

                        modified = Files.getLastModifiedTime(path)
                                .toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern(
                                        "dd MMM yyyy, HH:mm"
                                ));

                        } catch (IOException | SecurityException exception) {
                        }
                }

                Label typeLabel = new Label("Type: " + type);
                Label sizeLabel = new Label("Size: " + size);
                Label modifiedLabel = new Label("Modified: " + modified);

                Label pathLabel = new Label("Location:\n" + path);
                pathLabel.setWrapText(true);

                Node preview = createQuickInfoPreview(path, type);

                VBox inspector = new VBox(
                        14,
                        header,
                        preview,
                        nameLabel,
                        new javafx.scene.control.Separator(),
                        typeLabel,
                        sizeLabel,
                        modifiedLabel,
                        pathLabel
                );

                inspector.setPrefWidth(290);
                inspector.setMinWidth(260);

                inspector.setStyle(
                        "-fx-padding: 20;" +
                        "-fx-background-color: -fx-control-inner-background;" +
                        "-fx-border-color: -fx-box-border;" +
                        "-fx-border-width: 0 0 0 1;"
                );

                root.setRight(inspector);
        }

        private Node createQuickInfoPreview(Path path, String type) {

                StackPane previewBox = new StackPane();

                previewBox.setPrefSize(250, 180);
                previewBox.setMinHeight(180);
                previewBox.setMaxHeight(180);

                previewBox.setStyle(
                        "-fx-background-color: #f3f4f6;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: #e5e7eb;"
                );

                boolean isImage =
                        type.equalsIgnoreCase("Image")
                        || type.endsWith(" Image");

                if (isImage && Files.isRegularFile(path)) {

                        Label loadingLabel =
                                new Label("Loading preview...");

                        previewBox.getChildren().add(loadingLabel);

                        Image image = new Image(
                                path.toUri().toString(),
                                250,
                                180,
                                true,
                                true,
                                true
                        );

                        ImageView imageView = new ImageView(image);

                        imageView.setFitWidth(250);
                        imageView.setFitHeight(180);
                        imageView.setPreserveRatio(true);
                        imageView.setSmooth(true);

                        Runnable updatePreview = () -> {

                        if (image.isError()) {

                                previewBox.getChildren().setAll(
                                        new Label("Preview unavailable")
                                );

                        } else if (image.getProgress() >= 1.0) {

                                previewBox.getChildren().setAll(imageView);
                        }
                        };

                        image.progressProperty().addListener(
                                (observable, oldValue, newValue) ->
                                        updatePreview.run()
                        );

                        image.errorProperty().addListener(
                                (observable, oldValue, newValue) ->
                                        updatePreview.run()
                        );

                        updatePreview.run();

                } else {

                        String placeholder = switch (type) {
                        case "Folder" -> "FOLDER";
                        case "Document" -> "DOCUMENT";
                        case "Video" -> "VIDEO";
                        case "Audio" -> "AUDIO";
                        case "Archive" -> "ARCHIVE";
                        case "Code" -> "CODE";
                        default -> "FILE";
                        };

                        Label placeholderLabel = new Label(placeholder);

                        placeholderLabel.setStyle(
                                "-fx-font-size: 20px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: #64748b;"
                        );

                        previewBox.getChildren().add(placeholderLabel);
                }

                return previewBox;
        }

        private Node createStoredFilePreview(Path filePath) {

                FileItem scannedItem = findScannedFileItem(filePath);

                if (scannedItem != null) {
                        return filePreviewFactory.create(scannedItem);
                }

                String fileName = filePath.getFileName().toString()
                        .toLowerCase(Locale.ROOT);

                boolean isImage = fileName.matches(
                        ".*\\.(jpg|jpeg|png|gif|bmp)"
                );

                if (!isImage || !Files.isRegularFile(filePath)) {
                        return filePreviewFactory.createFallback();
                }

                StackPane preview = new StackPane();

                preview.setPrefSize(56, 42);
                preview.setMinSize(56, 42);
                preview.setMaxSize(56, 42);

                preview.getStyleClass().addAll(
                        "file-preview",
                        "file-preview-image"
                );

                Label fallback = new Label("IMAGE");
                fallback.getStyleClass().add(
                        "file-preview-placeholder"
                );

                preview.getChildren().add(fallback);

                Image image = new Image(
                        filePath.toUri().toString(),
                        112,
                        84,
                        true,
                        true,
                        true
                );

                ImageView imageView = new ImageView(image);

                imageView.setFitWidth(56);
                imageView.setFitHeight(42);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);

                Runnable updatePreview = () -> {

                        if (image.isError()) {
                        preview.getChildren().setAll(fallback);

                        } else if (image.getProgress() >= 1.0) {
                        preview.getChildren().setAll(imageView);
                        }
                };

                image.progressProperty().addListener(
                        (observable, oldValue, newValue) ->
                                updatePreview.run()
                );

                image.errorProperty().addListener(
                        (observable, oldValue, newValue) ->
                                updatePreview.run()
                );

                updatePreview.run();

                return preview;
        }
}
