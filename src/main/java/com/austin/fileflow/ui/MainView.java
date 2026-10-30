package com.austin.fileflow.ui;

import static com.austin.fileflow.i18n.I18n.tr;
import com.austin.fileflow.i18n.I18n;
import com.austin.fileflow.i18n.AppLanguage;

import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;

import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
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
import javafx.stage.WindowEvent;
import javafx.geometry.Pos;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.DragEvent;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

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
import java.nio.file.LinkOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.austin.fileflow.model.FileItem;
import com.austin.fileflow.model.ScanResult;
import com.austin.fileflow.model.ScanHistoryEntry;
import com.austin.fileflow.model.StorageVolume;
import com.austin.fileflow.service.FileScanService;
import com.austin.fileflow.service.ScanWorkspace;
import com.austin.fileflow.service.FolderWatcher;
import javafx.application.Platform;
import com.austin.fileflow.service.StorageService;
import com.austin.fileflow.ui.component.FilePreviewFactory;
import com.austin.fileflow.ui.component.FileRowCell;
import com.austin.fileflow.ui.component.SlideSwitch;
import com.austin.fileflow.util.FileSizeFormatter;
import com.austin.fileflow.model.FileType;
import com.austin.fileflow.repository.FavouriteRepo;
import com.austin.fileflow.repository.TagRepo;
import com.austin.fileflow.repository.ScanHistoryRepo;
import com.austin.fileflow.platform.FilePlatformService;
import com.austin.fileflow.model.DuplicateGroup;
import com.austin.fileflow.service.DuplicateService;
import com.austin.fileflow.service.SimilarImageService;
import com.austin.fileflow.model.SimilarImagePair;
import com.austin.fileflow.model.ShortcutAction;
import com.austin.fileflow.service.ShortcutManager;
import com.austin.fileflow.model.SimilarImageGroup;
import com.austin.fileflow.model.WorkspaceScanSnapshot;
import com.austin.fileflow.service.WorkspaceScanService;
import com.austin.fileflow.platform.RecycleBinService;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.input.KeyEvent;

public class MainView {
    
    private HBox oceanPageHeader;
    private Label oceanPageHeading;
    private Label oceanPageSubtitle;
    private Label oceanFooterStatus;
    private Label oceanShortcutHint;
    private Button oceanBackButton;
    private Button oceanCancelScan;
    private VBox oceanWorkspaceRoots;
    private final List<String> oceanNavigationHistory = new ArrayList<>();
    private boolean oceanGoingBack;

    private final BorderPane root;
    private StorageVolume displayedStorageVolume;
    private VBox quickInfoPanel;
    private StackPane quickInfoImage;
    private Label quickInfoName, quickInfoType, quickInfoSize, quickInfoModified, quickInfoLocation;
    private long quickInfoGeneration;
    private java.util.concurrent.Future<?> quickInfoMetadata;
    private final java.util.concurrent.ThreadPoolExecutor metadataExecutor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0, java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.ArrayBlockingQueue<>(16), runnable -> {
                Thread thread = new Thread(runnable, "fileflow-metadata"); thread.setDaemon(true); return thread;
            });
    private final java.util.concurrent.ThreadPoolExecutor fileListExecutor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0, java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.ArrayBlockingQueue<>(1), runnable -> {
                Thread thread = new Thread(runnable, "fileflow-listing"); thread.setDaemon(true); return thread;
            });
    private java.util.concurrent.Future<?> fileListingTask;
    private long fileListingGeneration;
    private ContextMenu sharedFileMenu;
    private MenuItem sharedDeleteItem;
    private Path menuPath;

    private final PauseTransition dashboardClickDelay =
        new PauseTransition(Duration.millis(160));
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
    private List<SimilarImagePair> latestSimilarPairs = List.of();
    private final SimilarImageService similarImageService =
        new SimilarImageService();
    private boolean similarAnalysisCompleted = false;
    private Task<List<SimilarImagePair>> currentSimilarTask;
    private String similarDetectionMode = "Balanced";
    private boolean similarTabSelected = false;     
    private boolean duplicateScanning;
    private Task<List<DuplicateGroup>> currentDuplicateTask;
    private final FavouriteRepo favouriteRepo;
    private String favouritesSearchQuery = "";
    private String favouritesSortOrder = "Name A-Z";
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
    private final RecycleBinService recycleBinService =
        new RecycleBinService();
    private Path scanRootPath;
    private final ScanWorkspace scanWorkspace = new ScanWorkspace();
    // Tracks cancellation of a multi-folder scan
    private AtomicBoolean workspaceCancelRequested;

    // Holds the latest multi-folder scan results
    private WorkspaceScanSnapshot lastWorkspaceScanSnapshot;
    private final FolderWatcher folderWatcher = new FolderWatcher();
    private boolean autoRefreshPending = false;
    private final PauseTransition autoRefreshDelay = new PauseTransition(Duration.millis(1000));
    private Label storageNameLabel;
    private ProgressBar storageUsageBar;
    private Label storageUsedLabel;
    private Label storageFreeLabel;
    private Label storageRootLabel;
    private Path currentStorageRoot;
    private Path currentFolder;
    private static final String CONFIRM_BEFORE_DELETE_KEY =
        "confirmBeforeRecycleBinMove";
    private final Preferences preferences = Preferences.userNodeForPackage(MainView.class);
    private FilesViewMode filesViewMode = FilesViewMode.BROWSE;
    private boolean fileSelectionMode = false;
    private final Set<Path> selectedFilePaths = new HashSet<>();
    private boolean duplicateAnalysisCompleted;
    private final Map<Path, FileItem> scannedItemsByPath =
        new HashMap<>();
    private final Set<String> expandedTags = new HashSet<>();


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
        root.getStyleClass().add("ocean-app");
        if (I18n.instance().language() == AppLanguage.SIMPLIFIED_CHINESE) root.getStyleClass().add("language-zh");
        pageTitle = new Label("Home");
        pageContent = new VBox();
        scanButton = new Button(tr("ui.scan_folder"));
        refreshButton = new Button(tr("common.refresh"));
        refreshButton.setDisable(true);
        
        autoRefreshDelay.setOnFinished(event -> {

                // Automatic multi-root watching is not enabled yet
                if (lastWorkspaceScanSnapshot != null) {
                        autoRefreshPending = false;
                        return;
                }

                if (scanRootPath == null) {
                        return;
                }

                if (scanning) {
                        autoRefreshPending = true;
                        return;
                }

                autoRefreshPending = false;
                refreshCurrentScan();
        });

        VBox sidebar = createSidebar();
        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setPrefWidth(208);
        sidebarScroll.setMinWidth(196);
        sidebarScroll.setMaxWidth(208);
        sidebarScroll.getStyleClass().add("ocean-sidebar-scroll");
        sidebar.minHeightProperty().bind(sidebarScroll.heightProperty().subtract(2));
        root.setLeft(sidebarScroll);
        BorderPane.setMargin(sidebarScroll, new Insets(0, 8, 0, 0));
        javafx.scene.layout.Pane inspectorLayer = new javafx.scene.layout.Pane() {
            @Override protected void layoutChildren() {
                if (quickInfoPanel != null) quickInfoPanel.resizeRelocate(Math.max(0, getWidth() - 272), 0, 272, getHeight());
            }
        };
        inspectorLayer.setPickOnBounds(false);
        inspectorLayer.getStyleClass().add("quick-info-layer");
        StackPane centerLayers = new StackPane(createContent(), inspectorLayer);
        root.setCenter(centerLayers);
        root.setTop(createOceanCommandBar());
        root.setBottom(createOceanStatusBar());
        I18n.instance().languageProperty().addListener((observable, before, after) -> applyLanguageChange());
        BorderPane.setMargin(root.getTop(), new Insets(0, 0, 8, 0));
        BorderPane.setMargin(root.getBottom(), new Insets(8, 0, 0, 0));

        root.sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                        if (newScene == null) {
                                return;
                        }

                        newScene.windowProperty().addListener(
                                (windowObs, oldWindow, newWindow) -> {

                                        if (newWindow == null) {
                                                return;
                                        }

                                        newWindow.addEventHandler(
                                                WindowEvent.WINDOW_HIDDEN,
                                                event -> {

                                                        hideQuickInfo();
                                                        cancelFileListing();
                                                        fileListExecutor.shutdownNow();
                                                        metadataExecutor.shutdownNow();
                                                        filePreviewFactory.close();
                                                        autoRefreshDelay.stop();
                                                        autoRefreshPending = false;

                                                        try {
                                                                folderWatcher.close();
                                                        } catch (IOException exception) {
                                                                exception.printStackTrace();
                                                        }
                                                }
                                        );
                                }
                        );
                }
        );

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

    private SVGPath createSidebarIcon(String pathData) {

        SVGPath icon = new SVGPath();

        icon.setContent(pathData);

        icon.getStyleClass().add("sidebar-icon");

        return icon;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(208);
        sidebar.setMinWidth(0);
        sidebar.getStyleClass().add("sidebar");

        Label home = new Label(tr("nav.home"));
        home.getStyleClass().add("nav-item");

        Label dashboard = new Label(tr("nav.dashboard"));
        dashboard.getStyleClass().add("nav-item");

        filesNavItem = new Label(tr("nav.files"));
        filesNavItem.getStyleClass().add("nav-item");   

        Label favourites = new Label(tr("nav.favourites"));
        favourites.getStyleClass().add("nav-item");

        Label duplicates = new Label(tr("nav.duplicates"));
        duplicates.getStyleClass().add("nav-item");

        Label tags = new Label(tr("nav.tags"));
        tags.getStyleClass().add("nav-item");

        Label history = new Label(tr("nav.history"));
        history.getStyleClass().add("nav-item");

        Label settings = new Label(tr("nav.settings"));
        settings.getStyleClass().add("nav-item");

        home.setUserData("Home");
        home.textProperty().bind(I18n.binding("nav.home"));
        dashboard.setUserData("Dashboard");
        dashboard.textProperty().bind(I18n.binding("nav.dashboard"));
        filesNavItem.setUserData("Files");
        filesNavItem.textProperty().bind(I18n.binding("nav.files"));
        favourites.setUserData("Favourites");
        favourites.textProperty().bind(I18n.binding("nav.favourites"));
        duplicates.setUserData("Duplicates");
        duplicates.textProperty().bind(I18n.binding("nav.duplicates"));
        tags.setUserData("Tags");
        tags.textProperty().bind(I18n.binding("nav.tags"));
        history.setUserData("History");
        history.textProperty().bind(I18n.binding("nav.history"));
        settings.setUserData("Settings");
        settings.textProperty().bind(I18n.binding("nav.settings"));

        // Home
        home.setGraphic(createSidebarIcon(
                "M3 10 L12 3 L21 10 V20 H14 V14 H10 V20 H3 Z"
        ));

        // Dashboard
        dashboard.setGraphic(createSidebarIcon(
                "M3 3 H10 V10 H3 Z " +
                "M14 3 H21 V10 H14 Z " +
                "M3 14 H10 V21 H3 Z " +
                "M14 14 H21 V21 H14 Z"
        ));

        // Files
        filesNavItem.setGraphic(createSidebarIcon(
                "M3 7 V19 Q3 21 5 21 H19 " +
                "Q21 21 21 19 V9 Q21 7 19 7 " +
                "H12 L10 5 H5 Q3 5 3 7 Z"
        ));

        // Favourites
        favourites.setGraphic(createSidebarIcon(
                "M12 2 L15.1 8.3 L22 9.3 " +
                "L17 14.2 L18.2 21 L12 17.7 " +
                "L5.8 21 L7 14.2 L2 9.3 L8.9 8.3 Z"
        ));

        // Duplicates
        duplicates.setGraphic(createSidebarIcon(
                "M8 8 H21 V21 H8 Z M4 16 H3 V3 H16 V4"
        ));

        // Tags
        tags.setGraphic(createSidebarIcon(
                "M3 3 H12 L21 12 L12 21 L3 12 Z " +
                "M7 7 A1 1 0 1 0 9 7 " +
                "A1 1 0 1 0 7 7"
        ));

        // History
        history.setGraphic(createSidebarIcon(
                "M3 12 A9 9 0 1 0 6 5 " +
                "M3 4 V9 H8 M12 7 V12 L15 14"
        ));

        // Settings
        settings.setGraphic(createSidebarIcon(
                "M4 6 H20 M4 12 H20 M4 18 H20 " +
                "M9 3 V9 M16 9 V15 M9 15 V21"
        ));

        // Consistent icon and text spacing
        
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
        
        for (Label navItem : navItems) {
                navItem.setMaxWidth(Double.MAX_VALUE);
                navItem.setAlignment(Pos.CENTER_LEFT);
                navItem.setGraphicTextGap(12);
        }

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
        Label workspaceTitle = OceanComponents.label(tr("ui.your_workspace"), "ocean-sidebar-caption");
        workspaceTitle.textProperty().bind(I18n.binding("ui.your_workspace"));
        oceanWorkspaceRoots = new VBox(3);
        Button manage = OceanComponents.action(tr("ui.manage_folders"), "plus", "ocean-folder-shortcut", this::showScanWorkspaceDialog);
        manage.textProperty().bind(I18n.binding("ui.manage_folders"));
        manage.setMaxWidth(Double.MAX_VALUE);
        VBox workspace = new VBox(8, workspaceTitle, oceanWorkspaceRoots, manage);
        workspace.getStyleClass().add("ocean-sidebar-workspace");

        sidebar.getChildren().addAll(
                home,
                dashboard,
                filesNavItem,
                favourites,
                duplicates,
                tags,
                history,
                settings,
                workspace,
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

        Button storageVolumesButton = new Button();
        storageVolumesButton.getStyleClass().add("sidebar-storage-volumes-button");
        storageVolumesButton.setGraphic(createChevronRightIcon());

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

                                        storageVolumesButton.setGraphic(createChevronRightIcon());
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

                storageVolumesButton.setGraphic(
                        expanding
                                ? createChevronDownIcon()
                                : createChevronRightIcon()
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

                storageNameLabel.setText(tr("ui.storage"));
                storageRootLabel.setText("");
                storageUsageBar.setProgress(0);
                storageUsedLabel.setText(tr("ui.unavailable"));
                storageFreeLabel.setText("");

                return;
        }

        displayedStorageVolume = volume;
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
                        + tr("ui.used")
                        + FileSizeFormatter.format(volume.getTotalBytes())
                        + tr("ui.total")
        );

        storageFreeLabel.setText(
                FileSizeFormatter.format(volume.getFreeBytes())
                        + tr("ui.free")
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
                                + tr("ui.used")
                                + FileSizeFormatter.format(
                                        volume.getTotalBytes()
                                )
                                + tr("ui.total")
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
                                + tr("ui.free")
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
        VBox content = new VBox(18);
        content.getStyleClass().addAll("content", "ff-dark-shell");
        content.setMinWidth(0);
        content.setMinHeight(0);
        pageContent.setMinWidth(0);
        pageContent.setMinHeight(0);
        pageContent.setFillWidth(true);
        VBox.setVgrow(pageContent, Priority.ALWAYS);

        oceanPageHeading = OceanComponents.label("", "page-title");
        oceanPageSubtitle = OceanComponents.label("", "ocean-page-subtitle");
        oceanPageSubtitle.setWrapText(true);
        VBox titles = new VBox(5, oceanPageHeading, oceanPageSubtitle);
        titles.setMinWidth(0);
        HBox.setHgrow(titles, Priority.ALWAYS);

        scanButton.setGraphic(OceanComponents.icon("scan", 17));
        scanButton.setOnAction(event -> handleScanAction());
        scanButton.getStyleClass().add("primary-button");
        refreshButton.setGraphic(OceanComponents.icon("refresh", 16));
        refreshButton.setOnAction(event -> refreshCurrentScan());
        refreshButton.getStyleClass().add("secondary-button");
        FlowPane actions = new FlowPane(8, 8, refreshButton, scanButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPrefWrapLength(275);
        actions.setMaxWidth(300);
        actions.setMinWidth(240);
        oceanPageHeader = new HBox(16, titles, actions);
        oceanPageHeader.setAlignment(Pos.CENTER_LEFT);
        oceanPageHeader.getStyleClass().add("top-bar");
        content.getChildren().addAll(oceanPageHeader, pageContent);
        updateOceanPageHeader();
        return content;
    }

    private HBox createOceanCommandBar() {
        Label brand = OceanComponents.label("FileFlow", "ocean-brand");
        HBox brandBox = new HBox(11, AppIcon.view(40), brand);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setMinWidth(192);
        brandBox.setPrefWidth(192);

        oceanBackButton = OceanComponents.action("", "back", "ocean-icon-button", () -> {
            if (oceanNavigationHistory.isEmpty()) return;
            String previous = oceanNavigationHistory.remove(oceanNavigationHistory.size() - 1);
            oceanGoingBack = true;
            navigateOcean(previous);
            oceanGoingBack = false;
        });
        oceanBackButton.accessibleTextProperty().bind(I18n.binding("ui.previous_page"));
        oceanBackButton.setDisable(true);

        TextField commandSearch = new TextField();
        commandSearch.promptTextProperty().bind(I18n.binding("ui.search_files_in_your_workspace"));
        commandSearch.getStyleClass().add("ff-command-search-field");
        commandSearch.setMinWidth(100);
        HBox.setHgrow(commandSearch, Priority.ALWAYS);
        Button clear = OceanComponents.action("", "close", "ocean-icon-button", commandSearch::clear);
        clear.accessibleTextProperty().bind(I18n.binding("ui.clear_search"));
        clear.visibleProperty().bind(commandSearch.textProperty().isNotEmpty());
        clear.managedProperty().bind(clear.visibleProperty());
        oceanShortcutHint = OceanComponents.label(
                shortcutManager.getShortcut(ShortcutAction.FOCUS_SEARCH).getDisplayText(), "ocean-key-hint");
        HBox searchBox = new HBox(10, OceanComponents.icon("search", 18), commandSearch, clear, oceanShortcutHint);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setMaxWidth(520);
        searchBox.getStyleClass().add("ff-command-search-box");
        HBox.setHgrow(searchBox, Priority.ALWAYS);
        commandSearch.setOnAction(event -> {
            String query = commandSearch.getText().trim();
            filesViewMode = FilesViewMode.ALL_FILES;
            navigateOcean("Files");
            if (searchField != null) {
                searchField.setText(query);
                searchField.requestFocus();
            }
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button workspace = OceanComponents.action(tr("ui.workspace"), "folder", "ocean-workspace-button", this::showScanWorkspaceDialog);
        workspace.textProperty().bind(I18n.binding("ui.workspace"));
        HBox bar = new HBox(14, brandBox, oceanBackButton, searchBox, spacer, workspace);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("ff-command-bar");
        return bar;
    }

    private HBox createOceanStatusBar() {
        Region dot = new Region();
        dot.getStyleClass().add("ocean-status-dot");
        oceanFooterStatus = OceanComponents.label(tr("ui.ready_to_scan"), "ocean-status-text");
        oceanFooterStatus.visibleProperty().bind(scanStatusLabel.visibleProperty().not());
        oceanFooterStatus.managedProperty().bind(oceanFooterStatus.visibleProperty());
        scanStatusLabel.getStyleClass().add("ocean-status-text");
        scanStatusLabel.setMaxWidth(680);
        scanProgressBar.setPrefWidth(180);
        scanProgressBar.setMaxWidth(180);
        scanProgressBar.getStyleClass().add("ocean-scan-progress");
        oceanCancelScan = OceanComponents.action(tr("common.cancel"), "close", "ocean-text-button", this::handleScanAction);
        oceanCancelScan.textProperty().bind(I18n.binding("common.cancel"));
        oceanCancelScan.setVisible(false);
        oceanCancelScan.setManaged(false);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button manage = OceanComponents.action(tr("ui.manage_folders"), "folder", "ocean-text-button", this::showScanWorkspaceDialog);
        manage.textProperty().bind(I18n.binding("ui.manage_folders"));
        HBox bar = new HBox(12, dot, oceanFooterStatus, scanStatusLabel, scanProgressBar, oceanCancelScan, spacer, manage);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("ocean-status-bar");
        updateOceanStatus();
        return bar;
    }

    private void navigateOcean(String title) {
        navItems.stream().filter(item -> title.equals(item.getUserData())).findFirst()
                .ifPresent(item -> selectNavItem(item, title));
    }

    private void updateOceanPageHeader() {
        if (oceanPageHeader == null) return;
        String page = pageTitle.getText();
        oceanPageHeading.setText(switch (page) {
            case "Dashboard" -> tr("ui.the_bigger_picture");
            case "Files" -> tr("ui.your_files_in_flow");
            case "Duplicates" -> tr("ui.a_little_less_clutter");
            case "Tags" -> tr("ui.everything_has_a_place");
            case "History" -> tr("ui.right_where_you_left_it");
            case "Settings" -> tr("ui.make_it_yours");
            default -> I18n.choice(page);
        });
        oceanPageSubtitle.setText(switch (page) {
            case "Dashboard" -> tr("ui.understand_your_files_and_the_space_they_use");
            case "Files" -> tr("ui.browse_find_and_organise_your_workspace");
            case "Favourites" -> tr("ui.the_files_you_want_to_keep_close");
            case "Duplicates" -> tr("ui.find_duplicate_files_and_visually_similar_images");
            case "Tags" -> tr("ui.group_files_your_way_wherever_they_live");
            case "History" -> tr("ui.your_recent_scans_ready_for_another_look");
            case "Settings" -> tr("ui.a_few_thoughtful_controls_for_your_daily_flow");
            default -> "";
        });
        boolean visible = !page.equals("Home");
        oceanPageHeader.setVisible(visible);
        oceanPageHeader.setManaged(visible);
        if (oceanBackButton != null) oceanBackButton.setDisable(oceanNavigationHistory.isEmpty());
        if (oceanShortcutHint != null) oceanShortcutHint.setText(
                shortcutManager.getShortcut(ShortcutAction.FOCUS_SEARCH).getDisplayText());
    }

    private VBox oceanScrollable(VBox page) {
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("ocean-page-scroll");
        Platform.runLater(() -> scroll.setVvalue(0));
        VBox wrapper = new VBox(scroll);
        wrapper.setMinHeight(0);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private void updateOceanStatus() {
        if (oceanFooterStatus != null) {
            int count = lastWorkspaceScanSnapshot == null ? (scanRootPath == null ? 0 : 1)
                    : lastWorkspaceScanSnapshot.resultsByRoot().size();
            oceanFooterStatus.setText(count == 0 ? tr("ui.ready_to_scan") :
                    tr("ui.workspace_up_to_date") + count + (count == 1 ? tr("ui.folder_ba1a7") : tr("ui.folders_a61f8")));
        }
        if (oceanCancelScan != null) {
            oceanCancelScan.setVisible(scanning);
            oceanCancelScan.setManaged(scanning);
        }
        if (oceanWorkspaceRoots == null) return;
        oceanWorkspaceRoots.getChildren().clear();
        List<Path> roots = new ArrayList<>(scanWorkspace.getRoots());
        if (roots.isEmpty() && scanRootPath != null) roots.add(scanRootPath);
        for (Path path : roots.stream().limit(3).toList()) {
            Button shortcut = OceanComponents.action(getPathDisplayName(path), "folder", "ocean-folder-shortcut", () -> {
                if (lastWorkspaceScanSnapshot != null && lastWorkspaceScanSnapshot.resultsByRoot().containsKey(path)) {
                    activateWorkspaceRoot(path);
                } else if (!path.equals(scanRootPath)) {
                    startScan(path);
                }
                navigateOcean("Files");
            });
            shortcut.setTooltip(new Tooltip(path.toString()));
            shortcut.setMaxWidth(Double.MAX_VALUE);
            oceanWorkspaceRoots.getChildren().add(shortcut);
        }
    }

    private VBox createSummaryCard(String title, String value) {
        VBox card = new VBox();
        card.getStyleClass().add("summary-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("summary-card-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("summary-card-value");

        Region accent = new Region();

        accent.getStyleClass().add(
                "summary-card-accent"
        );

        HBox cardHeader = new HBox(
                9,
                accent,
                titleLabel
        );

        cardHeader.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(
                cardHeader,
                valueLabel
        );

        return card;
    }

    private void selectNavItem(Label selectedItem, String title) {
        if (!title.equals(pageTitle.getText()) && !oceanGoingBack) {
            oceanNavigationHistory.add(pageTitle.getText());
        }

        for (Label navItem : navItems) {
                navItem.getStyleClass().remove("nav-item-selected");
        }

        selectedItem.getStyleClass().add("nav-item-selected");

        pageTitle.setText(title);

        cancelFileListing();
        pageContent.getChildren().clear();

        hideQuickInfo();

        scanButton.setVisible(!title.equals("Home"));
        scanButton.setManaged(!title.equals("Home"));

        refreshButton.setVisible(!title.equals("Home"));
        refreshButton.setManaged(!title.equals("Home"));

        switch (title) {
            case "Home" ->
                    pageContent.getChildren().add(oceanScrollable(createHomeContent()));

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

           case "Favourites" -> {
                VBox favouritesContent = createFavouritesContent();

                pageContent.getChildren().add(favouritesContent);

                VBox.setVgrow(favouritesContent, Priority.ALWAYS);
            }

            case "Duplicates" -> {

                VBox duplicatesContent = createDuplicatesContent();

                pageContent.getChildren().add(duplicatesContent);

                VBox.setVgrow(
                        duplicatesContent,
                        Priority.ALWAYS
                );
            }

            case "Tags" ->
                    pageContent.getChildren().add(oceanScrollable(createTagsContent()));

            case "History" ->
                    pageContent.getChildren().add(createHistoryContent());

            case "Settings" ->
                    pageContent.getChildren().add(oceanScrollable(createSettingsContent()));
        }
        for (Node page : pageContent.getChildren()) {
            VBox.setVgrow(page, Priority.ALWAYS);
            if (page instanceof Region region) {
                region.setMinHeight(0);
                region.setMaxHeight(Double.MAX_VALUE);
            }
        }
        updateOceanPageHeader();
        updateOceanStatus();
    }

    private VBox createHomeContent() {
        VBox home = new VBox(18);
        home.getStyleClass().add("home-content");
        Label eyebrow = OceanComponents.label(tr("ui.a_little_order_a_lot_of_possibility"), "ocean-eyebrow");
        Label heading = OceanComponents.label(tr("ui.make_room_for_what_matters"), "home-heading");
        Label subtitle = OceanComponents.label(tr("ui.a_clearer_view_of_your_files_a_calmer_place_to_keep_them"), "home-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(440);
        homeScanButton = OceanComponents.action(tr("ui.scan_a_folder"), "scan", "home-scan-button", this::handleScanAction);
        Button manage = OceanComponents.action(tr("ui.manage_folders"), "folder", "secondary-button", this::showScanWorkspaceDialog);
        HBox heroActions = new HBox(10, homeScanButton, manage);
        VBox heroCopy = new VBox(12, eyebrow, heading, subtitle, heroActions);
        heroCopy.setMinWidth(0);
        HBox.setHgrow(heroCopy, Priority.ALWAYS);
        HBox hero = new HBox(20, heroCopy, OceanComponents.heroArt());
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("home-hero");
        home.getChildren().add(hero);
        home.getChildren().add(OceanComponents.section(tr("ui.pick_up_where_you_left_off"), tr("ui.all_scans"), () -> navigateOcean("History")));
        HBox recent = new HBox(12);
        recent.setFillHeight(true);
        recent.setMaxHeight(Region.USE_PREF_SIZE);
        try {
            Map<Path, ScanHistoryEntry> latestByFolder = new java.util.LinkedHashMap<>();
            scanHistoryRepo.getAll().stream()
                    .sorted(Comparator.comparing(ScanHistoryEntry::getScannedAt).reversed())
                    .forEach(entry -> latestByFolder.putIfAbsent(entry.getFolderPath(), entry));
            for (ScanHistoryEntry entry : latestByFolder.values().stream().limit(3).toList()) {
                Label name = OceanComponents.label(getPathDisplayName(entry.getFolderPath()), "recent-scan-name");
                name.setMaxWidth(Double.MAX_VALUE);
                Label detail = OceanComponents.label(String.format(tr("ui.d_files_s"), entry.getFileCount(),
                        FileSizeFormatter.format(entry.getTotalBytes())), "recent-scan-details");
                detail.setWrapText(true);
                StackPane cover = new StackPane(OceanComponents.icon("folder", 30));
                cover.getStyleClass().add("ocean-folder-cover");
                VBox card = OceanComponents.card(new VBox(8, cover, name, detail), "ocean-recent-card",
                        tr("ui.scan") + getPathDisplayName(entry.getFolderPath()) + tr("ui.again"),
                        () -> startScan(entry.getFolderPath()));
                Tooltip.install(card, new Tooltip(entry.getFolderPath() + tr("ui.last_scanned") + entry.getScannedAt().format(
                        I18n.dateTimeFormatter())));
                recent.getChildren().add(card);
            }
        } catch (SQLException exception) {
            Label error = OceanComponents.label(tr("ui.recent_scans_could_not_be_loaded_you_can_still_scan_a_folder"), "ocean-muted");
            error.setWrapText(true);
            recent.getChildren().add(error);
        }
        if (recent.getChildren().isEmpty()) {
            HBox firstScan = new HBox(14, OceanComponents.icon("folder", 30),
                    new VBox(5, OceanComponents.label(tr("ui.your_next_scan_starts_here"), "recent-scan-name"),
                            OceanComponents.label(tr("ui.scan_a_folder_to_see_recent_activity_and_file_insights"), "ocean-muted")));
            firstScan.setAlignment(Pos.CENTER_LEFT);
            firstScan.getStyleClass().add("recent-scan-card");
            HBox.setHgrow(firstScan, Priority.ALWAYS);
            firstScan.setMaxWidth(Double.MAX_VALUE);
            recent.getChildren().add(firstScan);
        }
        home.getChildren().add(recent);
        home.getChildren().add(OceanComponents.section(tr("ui.explore_your_workspace"), null, null));
        HBox explore = new HBox(12,
                OceanComponents.tile(tr("ui.large_files"), tr("ui.see_what_is_taking_up_space"), "drive", () -> navigateOcean("Dashboard")),
                OceanComponents.tile(tr("ui.find_duplicates"), tr("ui.review_copies_keep_what_matters"), "copy", () -> navigateOcean("Duplicates")),
                OceanComponents.tile(tr("ui.recently_modified"), tr("ui.jump_back_into_your_latest_files"), "clock", () -> navigateOcean("Dashboard")),
                OceanComponents.tile(tr("ui.your_favourites"), tr("ui.important_files_always_close"), "star", () -> navigateOcean("Favourites")));
        explore.setMaxHeight(Region.USE_PREF_SIZE);
        home.getChildren().add(explore);
        return home;
    }

    private VBox createDashboardContent() {
        VBox body = new VBox(16);
        body.getStyleClass().add("ocean-dashboard");
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("dashboard-scroll");
        VBox page = new VBox(scroll);
        page.setMinHeight(0);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        if (latestScanResult == null) {
            body.getChildren().add(OceanComponents.empty(tr("ui.a_clearer_view_starts_here"),
                    tr("ui.scan_a_folder_to_understand_your_files_and_the_space_they_use"), this::handleScanAction));
            return page;
        }
        List<FileItem> scannedFiles = latestScanResult.getItems().stream().filter(item -> !item.isDirectory()).toList();
        long totalBytes = scannedFiles.stream().mapToLong(FileItem::getSizeBytes).sum();
        long folders = latestScanResult.getItems().stream().filter(FileItem::isDirectory).count();
        HBox stats = new HBox(12,
                createSummaryCard(tr("nav.files"), String.format("%,d", scannedFiles.size())),
                createSummaryCard(tr("ui.total_size"), FileSizeFormatter.format(totalBytes)),
                createSummaryCard(tr("ui.folders"), String.format("%,d", folders)),
                createSummaryCard(tr("ui.scan_issues"), String.valueOf(latestScanResult.getIssues().size())));
        for (Node node : stats.getChildren()) {
            Region card = (Region) node;
            card.setMinWidth(0);
            card.setPrefWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        body.getChildren().add(stats);
        int rootCount = lastWorkspaceScanSnapshot == null ? 1 : lastWorkspaceScanSnapshot.completedFolderCount();
        long workspaceFiles = lastWorkspaceScanSnapshot == null ? scannedFiles.size() : lastWorkspaceScanSnapshot.fileCount();
        long workspaceBytes = lastWorkspaceScanSnapshot == null ? totalBytes : lastWorkspaceScanSnapshot.totalFileBytes();
        Label workspaceName = OceanComponents.label(rootCount + (rootCount == 1 ? tr("ui.scanned_folder") : tr("ui.scanned_folders")), "ocean-workspace-title");
        Label workspaceDetail = OceanComponents.label(String.format(tr("ui.d_files_s"), workspaceFiles,
                FileSizeFormatter.format(workspaceBytes)), "ocean-muted");
        if (lastWorkspaceScanSnapshot != null && !lastWorkspaceScanSnapshot.failures().isEmpty()) {
            workspaceDetail.setText(workspaceDetail.getText() + " · " + lastWorkspaceScanSnapshot.failures().size() + tr("ui.failed_roots"));
        }
        Region workspaceSpacer = new Region();
        HBox.setHgrow(workspaceSpacer, Priority.ALWAYS);
        HBox workspace = new HBox(12, OceanComponents.icon("folder", 24), workspaceName, workspaceDetail, workspaceSpacer,
                OceanComponents.action(tr("ui.view_files"), "arrow", "ocean-text-button", () -> navigateOcean("Files")));
        workspace.setAlignment(Pos.CENTER_LEFT);
        workspace.getStyleClass().add("ocean-workspace-summary");
        body.getChildren().add(workspace);

        Map<FileType, Long> bytesByType = new java.util.LinkedHashMap<>();
        Map<FileType, Long> countByType = new HashMap<>();
        for (FileItem item : scannedFiles) {
            bytesByType.merge(item.getType(), item.getSizeBytes(), Long::sum);
            countByType.merge(item.getType(), 1L, Long::sum);
        }
        VBox types = new VBox(9);
        for (FileType type : bytesByType.keySet().stream().sorted(
                Comparator.comparingLong((FileType type) -> bytesByType.get(type)).reversed()).toList()) {
            long bytes = bytesByType.get(type);
            double fraction = totalBytes == 0 ? 0 : (double) bytes / totalBytes;
            Label name = OceanComponents.label(I18n.choice(getFileTypeDisplayName(type)), "dashboard-type-name");
            Label value = OceanComponents.label(FileSizeFormatter.format(bytes), "dashboard-type-meta");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox line = new HBox(8, name, spacer, value);
            ProgressBar bar = new ProgressBar(fraction);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("dashboard-type-progress");
            VBox row = new VBox(5, line, bar);
            Tooltip.install(row, new Tooltip(String.format(tr("ui.d_files_1f_of_size"), countByType.get(type), fraction * 100)));
            types.getChildren().add(row);
        }
        if (types.getChildren().isEmpty()) types.getChildren().add(OceanComponents.label(tr("ui.no_files_in_this_folder"), "ocean-muted"));
        HBox.setHgrow(types, Priority.ALWAYS);
        types.setMinWidth(120);
        HBox breakdown = new HBox(16, OceanComponents.donut(bytesByType, FileSizeFormatter.format(totalBytes)), types);
        breakdown.setAlignment(Pos.CENTER_LEFT);
        VBox typeCard = new VBox(16, OceanComponents.label(tr("ui.file_type_breakdown"), "section-title"), breakdown);
        typeCard.getStyleClass().add("ocean-glass-card");

        Map<Path, Long> folderBytes = new HashMap<>();
        Map<Path, Long> folderCounts = new HashMap<>();
        if (scanRootPath != null) {
            for (FileItem item : scannedFiles) {
                Path relative = scanRootPath.relativize(item.getPath());
                Path top = relative.getNameCount() > 1 ? scanRootPath.resolve(relative.getName(0)) : scanRootPath;
                folderBytes.merge(top, item.getSizeBytes(), Long::sum);
                folderCounts.merge(top, 1L, Long::sum);
            }
        }
        VBox folderRows = new VBox(11);
        for (Path folder : folderBytes.keySet().stream().sorted(
                Comparator.comparingLong((Path path) -> folderBytes.get(path)).reversed()).limit(5).toList()) {
            long bytes = folderBytes.get(folder);
            double fraction = totalBytes == 0 ? 0 : (double) bytes / totalBytes;
            Label name = OceanComponents.label(folder.equals(scanRootPath) ? tr("ui.files_in_scan_root") : getPathDisplayName(folder), "dashboard-folder-name");
            name.setMinWidth(0);
            Label amount = OceanComponents.label(FileSizeFormatter.format(bytes) + String.format(" · %.0f%%", fraction * 100), "dashboard-folder-meta");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox line = new HBox(8, name, spacer, amount);
            ProgressBar bar = new ProgressBar(fraction);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("dashboard-folder-progress");
            VBox row = new VBox(5, line, bar);
            row.getStyleClass().add("ocean-folder-stat-row");
            row.setOnMouseClicked(event -> {
                currentFolder = folder;
                filesViewMode = FilesViewMode.BROWSE;
                navigateOcean("Files");
            });
            row.setOnContextMenuRequested(event -> createFileContextMenu(folder).show(row, event.getScreenX(), event.getScreenY()));
            Tooltip.install(row, new Tooltip(folder + String.format(tr("ui.d_files"), folderCounts.get(folder))));
            folderRows.getChildren().add(row);
        }
        if (folderRows.getChildren().isEmpty()) folderRows.getChildren().add(OceanComponents.label(tr("ui.no_folder_data_yet"), "ocean-muted"));
        VBox folderCard = new VBox(16, OceanComponents.label(tr("ui.top_level_folders"), "section-title"), folderRows);
        folderCard.getStyleClass().add("ocean-glass-card");
        HBox charts = new HBox(16, typeCard, folderCard);
        for (VBox card : List.of(typeCard, folderCard)) {
            card.setMinWidth(0);
            card.setPrefWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        body.getChildren().add(charts);
        if (scanRootPath != null) {
            Label active = OceanComponents.label(tr("ui.current_scan") + scanRootPath, "ocean-muted");
            active.setWrapText(true);
            body.getChildren().add(active);
        }
        VBox largest = new VBox(3);
        scannedFiles.stream().sorted(Comparator.comparingLong(FileItem::getSizeBytes).reversed()).limit(10)
                .forEach(item -> largest.getChildren().add(createOceanDashboardRow(item, false)));
        VBox recent = new VBox(3);
        scannedFiles.stream().sorted(Comparator.comparing(FileItem::getLastModified).reversed()).limit(10)
                .forEach(item -> recent.getChildren().add(createOceanDashboardRow(item, true)));
        body.getChildren().addAll(
                createCollapsibleSection(tr("ui.largest_files"), largest.getChildren().size(), largest),
                createCollapsibleSection(tr("ui.recently_modified_41bf4"), recent.getChildren().size(), recent));
        return page;
    }

    private HBox createOceanDashboardRow(FileItem item, boolean modified) {
        Label name = OceanComponents.label(item.getName(), "file-name");
        Label location = OceanComponents.label(getWorkspaceFileLocation(item.getPath()), "dashboard-file-path");
        VBox info = new VBox(3, name, location);
        info.setMinWidth(0);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label value = OceanComponents.label(modified ? formatModifiedTime(item.getLastModified())
                : FileSizeFormatter.format(item.getSizeBytes()), "file-size");
        Button more = createFileActionsButton(item.getPath());
        more.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> event.consume());
        HBox row = new HBox(12, createUnifiedFilePreview(item), info, value, more);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dashboard-file-row");
        row.setOnContextMenuRequested(event -> createFileContextMenu(item.getPath()).show(row, event.getScreenX(), event.getScreenY()));
        configureDashboardFileRow(row, item.getPath());
        return row;
    }

    private Node createFolderListPreview() {

        SVGPath folderIcon = new SVGPath();

        folderIcon.setContent(
                "M3 7 V19 Q3 21 5 21 H19 " +
                "Q21 21 21 19 V9 Q21 7 19 7 " +
                "H12 L10 5 H5 Q3 5 3 7 Z"
        );

        folderIcon.getStyleClass().add(
                "files-folder-icon"
        );

        StackPane preview = new StackPane(folderIcon);

        preview.setMinSize(56, 42);
        preview.setPrefSize(56, 42);
        preview.setMaxSize(56, 42);

        preview.getStyleClass().add(
                "files-folder-preview"
        );

        return preview;
    }

    private Node createFileTypeListPreview(FileItem item) {

        String extension = item.getExtension() == null
                ? ""
                : item.getExtension()
                        .replaceFirst("^\\.", "")
                        .toLowerCase(Locale.ROOT);

        String kind = extension.equals("pdf")
                ? "pdf"
                : item.getType().name().toLowerCase(Locale.ROOT);

        if (!Set.of(
                "video",
                "audio",
                "archive",
                "code"
        ).contains(kind)) {

                return filePreviewFactory.createFallback();
        }

        String iconPath = switch (kind) {

                case "video" ->
                        "M4 6 H15 Q17 6 17 8 V16 Q17 18 15 18 " +
                        "H4 Q2 18 2 16 V8 Q2 6 4 6 Z " +
                        "M17 10 L22 7 V17 L17 14";

                case "audio" ->
                        "M9 18 V5 L21 3 V16 " +
                        "M9 18 A3 3 0 1 1 6 15 A3 3 0 0 1 9 18 Z " +
                        "M21 16 A3 3 0 1 1 18 13 A3 3 0 0 1 21 16 Z";

                case "archive" ->
                        "M6 2 H14 L20 8 V20 Q20 22 18 22 " +
                        "H6 Q4 22 4 20 V4 Q4 2 6 2 Z " +
                        "M14 2 V8 H20 M11 4 V6 M11 8 V10 " +
                        "M11 12 V14 M9 17 H13 V20 H9 Z";

                case "code" ->
                        "M6 2 H14 L20 8 V20 Q20 22 18 22 " +
                        "H6 Q4 22 4 20 V4 Q4 2 6 2 Z " +
                        "M14 2 V8 H20 " +
                        "M10 11 L7 14 L10 17 " +
                        "M14 11 L17 14 L14 17";

                case "document", "pdf" ->
                        "M6 2 H14 L20 8 V20 Q20 22 18 22 " +
                        "H6 Q4 22 4 20 V4 Q4 2 6 2 Z " +
                        "M14 2 V8 H20 M8 13 H16 M8 17 H16";

                default ->
                        "M6 2 H14 L20 8 V20 Q20 22 18 22 " +
                        "H6 Q4 22 4 20 V4 Q4 2 6 2 Z " +
                        "M14 2 V8 H20";
        };

        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add("files-type-icon");

        StackPane preview = new StackPane(icon);

        preview.setMinSize(56, 42);
        preview.setPrefSize(56, 42);
        preview.setMaxSize(56, 42);

        preview.getStyleClass().addAll(
                "files-type-preview",
                "files-type-" + kind
        );

        return preview;
    }

    private Node createUnifiedFilePreview(FileItem item) {

        if (item.isDirectory()) {
                return createFolderListPreview();
        }

        if (item.getType() == FileType.IMAGE
                || filePreviewFactory.hasSignatureBadge(item)
                || filePreviewFactory.hasNativeAppIcon(item)) {

                return filePreviewFactory.create(item);
        }

        return createFileTypeListPreview(item);
    }

    private Node createFileTagChip(
        String tagName,
        Runnable onRemove
    ) {
        Label tagText = new Label(tagName);
        tagText.getStyleClass().add("file-tag-text");

        StackPane tagPill = new StackPane(tagText);
        tagPill.getStyleClass().add("file-tag-pill");

        Button removeBadge = new Button("×");
        removeBadge.getStyleClass().add("file-tag-remove-badge");
        removeBadge.setFocusTraversable(false);

        removeBadge.setOnAction(event -> {
                event.consume();
                onRemove.run();
        });

        Tooltip.install(
                removeBadge,
                new Tooltip(tr("ui.remove_tag_from_file"))
        );

        StackPane container = new StackPane(
                tagPill,
                removeBadge
        );

        container.getStyleClass().add("file-tag-chip");

        StackPane.setAlignment(
                removeBadge,
                Pos.TOP_RIGHT
        );

        StackPane.setMargin(
                removeBadge,
                new Insets(-5, -5, 0, 0)
        );

        return container;
    }

    private VBox createFilesContent() {

        fileSelectionMode = false;
        selectedFilePaths.clear();

        VBox filesContent = new VBox(14);

        filesContent.setMaxHeight(
                Double.MAX_VALUE
        );

        if (latestScanResult == null) {
            Label emptyLabel = new Label(tr("ui.no_folder_has_been_scanned_yet"));
            emptyLabel.getStyleClass().add("ff-empty-message");

            filesContent.getChildren().add(emptyLabel);
            return filesContent;
        }

        VBox categoriesContent = new VBox(10);
        categoriesContent.setVisible(false);
        categoriesContent.setManaged(false);

        ToggleButton browseButton = new ToggleButton(tr("ui.browse"));
        ToggleButton allFilesButton = new ToggleButton(tr("ui.all_files"));
        ToggleButton categoriesButton =
                new ToggleButton(tr("ui.categories"));

        Button selectFilesButton = new Button(tr("ui.select_files"));
        selectFilesButton.getStyleClass().add("secondary-button");

        ToggleGroup viewGroup = new ToggleGroup();

        browseButton.getStyleClass().add("files-view-toggle");
        allFilesButton.getStyleClass().add("files-view-toggle");
        categoriesButton.getStyleClass().add("files-view-toggle");

        browseButton.setToggleGroup(viewGroup);
        allFilesButton.setToggleGroup(viewGroup);
        categoriesButton.setToggleGroup(viewGroup);

        switch (filesViewMode) {
                case BROWSE -> browseButton.setSelected(true);
                case ALL_FILES -> allFilesButton.setSelected(true);
                case CATEGORIES -> categoriesButton.setSelected(true);
        }


        Button backButton = new Button();

        SVGPath backArrow = createChevronRightIcon();
        backArrow.setRotate(180);

        backButton.setGraphic(backArrow);
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
        searchField.setPromptText(tr("ui.search_files"));
        searchField.getStyleClass().add("search-field");

        Button clearFileSearchButton = new Button("×");

        clearFileSearchButton.setFocusTraversable(false);
        clearFileSearchButton.setPrefSize(24, 24);

        clearFileSearchButton.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-text-fill: #8796AC;"
                + "-fx-font-size: 19px;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 0;"
        );

        clearFileSearchButton.visibleProperty().bind(
                searchField.textProperty().isNotEmpty()
        );

        clearFileSearchButton.managedProperty().bind(
                clearFileSearchButton.visibleProperty()
        );

        clearFileSearchButton.setOnAction(event -> {
        searchField.clear();
        searchField.requestFocus();
        });

        // Put the X inside the search field
        StackPane searchFieldBox = new StackPane(
                searchField,
                clearFileSearchButton
        );

        StackPane.setAlignment(
                clearFileSearchButton,
                Pos.CENTER_RIGHT
        );

        StackPane.setMargin(
                clearFileSearchButton,
                new Insets(0, 10, 0, 0)
        );

        // Leave enough space for the X
        searchField.setPadding(
                new Insets(8, 40, 8, 12)
        );

        ComboBox<String> typeFilter = new ComboBox<>();
        I18n.localizeChoices(typeFilter);
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
        I18n.localizeChoices(sortBy);

        sortBy.getItems().addAll(
            "Name A-Z",
            "Name Z-A",
            "Size Small-Large",
            "Size Large-Small",
            "Newest",
            "Oldest"
        );

        sortBy.setValue("Name A-Z");

        typeFilter.getStyleClass().add("files-filter-combo");
        sortBy.getStyleClass().add("files-filter-combo");

        HBox controlsRow = new HBox(12);

        controlsRow.getChildren().addAll(
                searchFieldBox,
                typeFilter,
                sortBy
        );
        searchFieldBox.setMinWidth(110);
        searchField.setMinWidth(80);
        searchField.setPrefWidth(280);
        searchField.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(searchFieldBox, Priority.ALWAYS);

        typeFilter.setPrefWidth(130);
        sortBy.setPrefWidth(160);

        Region viewSpacer = new Region();
        HBox.setHgrow(viewSpacer, Priority.ALWAYS);

        HBox viewBar = new HBox(
                10,
                browseButton,
                allFilesButton,
                categoriesButton,
                viewSpacer,
                selectFilesButton
        );

        viewBar.setAlignment(Pos.CENTER_LEFT);

        controlsRow.setAlignment(Pos.CENTER_LEFT);

        VBox toolbar = new VBox(
                10,
                viewBar,
                controlsRow
        );

        toolbar.setMaxWidth(Double.MAX_VALUE);


        // Show a root selector after a workspace scan
        if (lastWorkspaceScanSnapshot != null
                && !lastWorkspaceScanSnapshot.resultsByRoot().isEmpty()) {

        List<Path> availableRoots = new ArrayList<>(
                lastWorkspaceScanSnapshot
                        .resultsByRoot()
                        .keySet()
        );

        Label workspaceLabel = new Label(
                tr("ui.workspace_1f8f2") + availableRoots.size() + tr("ui.folders_56899")
        );

        workspaceLabel.getStyleClass().add("section-title");

        ComboBox<Path> rootSelector = new ComboBox<>();

        rootSelector.getItems().setAll(availableRoots);

        rootSelector.setPromptText(tr("ui.select_scan_folder"));

        rootSelector.setPrefWidth(360);
        rootSelector.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(
                rootSelector,
                Priority.ALWAYS
        );

        rootSelector.getStyleClass().add(
                "files-filter-combo"
        );

        if (scanRootPath != null
                && availableRoots.contains(scanRootPath)) {

                rootSelector.setValue(scanRootPath);
        }

        rootSelector.setOnAction(event -> {

                Path selectedRoot = rootSelector.getValue();

                if (selectedRoot == null
                        || selectedRoot.equals(scanRootPath)) {
                return;
                }

                activateWorkspaceRoot(selectedRoot);
        });

        HBox workspaceBar = new HBox(
                14,
                workspaceLabel,
                rootSelector
        );

        workspaceBar.setAlignment(Pos.CENTER_LEFT);
        workspaceBar.setMaxWidth(Double.MAX_VALUE);

       workspaceBar.getStyleClass().add("files-workspace-bar");

        filesContent.getChildren().add(workspaceBar);
        }

        filesContent.getChildren().add(toolbar);
        filesContent.getChildren().add(browseHeader);
        ScrollPane categoriesScroll = new ScrollPane(categoriesContent);
        categoriesScroll.setFitToWidth(true);
        categoriesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoriesScroll.getStyleClass().add("ocean-page-scroll");
        categoriesScroll.visibleProperty().bind(categoriesContent.visibleProperty());
        categoriesScroll.managedProperty().bind(categoriesScroll.visibleProperty());
        VBox.setVgrow(categoriesScroll, Priority.ALWAYS);
        filesContent.getChildren().add(categoriesScroll);

        if (!latestScanResult.getIssues().isEmpty()) {
            Label issueLabel = new Label(
                        latestScanResult.getIssues().size()
                                + tr("ui.item_s_could_not_be_scanned")
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
                        tr("ui.files_loaded_but_some_saved_metadata_could_not_be_loaded")
                );
        }

        TableView<FileItem> fileList = new TableView<>();
        fileList.getStyleClass().add("files-list-view");
        fileList.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<FileItem, FileItem> fileColumn = new TableColumn<>();
        fileColumn.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyObjectWrapper<>(value.getValue()));
        fileColumn.setSortable(false);
        fileColumn.setReorderable(false);
        fileList.getColumns().add(fileColumn);

        PauseTransition quickInfoDelay =
                new PauseTransition(Duration.millis(160));

        Label selectedCountLabel = new Label(tr("ui.0_files_selected"));

        Button bulkFavouriteButton =
                new Button(tr("ui.add_to_favourites"));

        bulkFavouriteButton.getStyleClass().add(
                "secondary-button"
        );

        bulkFavouriteButton.setDisable(true);

        Button bulkTagButton = new Button(tr("ui.tag_selected"));

        bulkTagButton.getStyleClass().add("primary-button");

        // No items selected initially
        bulkTagButton.setDisable(true);

        Button bulkDeleteButton =
                new Button(tr("files.recycle"));

        bulkDeleteButton.getStyleClass().add("danger-button");
        bulkDeleteButton.setDisable(true);

        Button clearSelectionButton = new Button(tr("ui.clear"));
        clearSelectionButton.getStyleClass().add("secondary-button");

        Region selectionSpacer = new Region();
        HBox.setHgrow(selectionSpacer, Priority.ALWAYS);

        HBox selectionBar = new HBox(
                12,
                selectedCountLabel,
                selectionSpacer,
                bulkTagButton,
                bulkFavouriteButton,
                bulkDeleteButton,
                clearSelectionButton
        );

        selectionBar.setAlignment(Pos.CENTER_LEFT);
        selectionBar.getStyleClass().add("file-selection-bar");

        selectionBar.setVisible(false);
        selectionBar.setManaged(false);

        Runnable updateSelectionUi = () -> {

                int count = selectedFilePaths.size();

                selectedCountLabel.setText(
                        count + (count == 1
                                ? tr("ui.item_selected")
                                : tr("ui.items_selected"))
                );

                boolean nothingSelected = count == 0;

                bulkTagButton.setDisable(nothingSelected);
                bulkFavouriteButton.setDisable(nothingSelected);
                bulkDeleteButton.setDisable(nothingSelected);
                clearSelectionButton.setDisable(nothingSelected);

                boolean allFavourites = !nothingSelected
                        && selectedFilePaths.stream()
                                .allMatch(favouritePaths::contains);

                boolean anyFavourite = selectedFilePaths.stream()
                        .anyMatch(favouritePaths::contains);

                if (allFavourites) {
                        bulkFavouriteButton.setText(
                                tr("ui.remove_from_favourites")
                        );
                } else if (anyFavourite) {
                        bulkFavouriteButton.setText(
                                tr("ui.add_missing_to_favourites")
                        );
                } else {
                        bulkFavouriteButton.setText(
                                tr("ui.add_to_favourites")
                        );
                }
        };

        bulkTagButton.setOnAction(event ->
                showBulkTagDialog(
                        existingTags,
                        fileTags,
                        fileList
                )
        );

        bulkFavouriteButton.setOnAction(event -> {

                if (selectedFilePaths.isEmpty()) {
                        return;
                }

                List<Path> targets =
                        new ArrayList<>(selectedFilePaths);

                // Remove only when ALL selected items are favourites
                boolean removeMode = targets.stream()
                        .allMatch(favouritePaths::contains);

                int changed = 0;
                int skipped = 0;
                int failed = 0;

                for (Path path : targets) {

                        try {

                                if (removeMode) {

                                        favouriteRepo.remove(path);
                                        favouritePaths.remove(path);
                                        changed++;

                                } else {

                                        // Preserve existing favourites in mixed selections
                                        if (favouritePaths.contains(path)) {
                                        skipped++;
                                        continue;
                                        }

                                        favouriteRepo.add(path);
                                        favouritePaths.add(path);
                                        changed++;
                                }

                        } catch (SQLException exception) {

                                failed++;
                                exception.printStackTrace();
                        }
                }

                // Update stars and button label after database changes
                updateSelectionUi.run();
                fileList.refresh();

                Alert result = new Alert(
                        failed == 0
                                ? Alert.AlertType.INFORMATION
                                : Alert.AlertType.WARNING
                );

                result.setTitle(tr("ui.bulk_favourite_results"));

                result.setHeaderText(
                        removeMode
                                ? tr("ui.remove_from_favourites")
                                : tr("ui.add_to_favourites")
                );

                result.setContentText(
                        String.format(
                                tr("ui.action_s_nupdated_d_nalready_saved_d_nfailed_d"),
                                removeMode ? tr("ui.remove") : tr("common.add"),
                                changed,
                                skipped,
                                failed
                        )
                );

                showOceanDialog(result);
        });

        bulkDeleteButton.setOnAction(event ->
                showBulkRecycleBinDialog(
                        fileList,
                        updateSelectionUi
                )
        );

        clearSelectionButton.setOnAction(event -> {

                selectedFilePaths.clear();

                fileList.getSelectionModel().clearSelection();

                updateSelectionUi.run();

                fileList.refresh();
        });

        selectFilesButton.setOnAction(event -> {

                fileSelectionMode = !fileSelectionMode;

                selectedFilePaths.clear();

                quickInfoDelay.stop();
                fileList.getSelectionModel().clearSelection();

                selectionBar.setVisible(fileSelectionMode);
                selectionBar.setManaged(fileSelectionMode);

                selectFilesButton.setText(
                        fileSelectionMode ? tr("common.done") : tr("ui.select_files")
                );

                updateSelectionUi.run();

                fileList.refresh();
        });

        fileList.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldItem, newItem) -> {

                quickInfoDelay.stop();

                if (fileSelectionMode) {
                        return;
                }

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

        fileList.setFixedCellSize(76);
        fileColumn.setCellFactory(column -> new FileRowCell(new FileRowCell.Actions() {
            public Node preview(FileItem item) { return createUnifiedFilePreview(item); }
            public String location(FileItem item) { return getWorkspaceFileLocation(item.getPath()); }
            public boolean showLocation() { return filesViewMode == FilesViewMode.ALL_FILES; }
            public boolean favourite(Path path) { return favouritePaths.contains(path); }
            public List<String> tags(FileItem item) { return fileTags.getOrDefault(item.getPath(), List.of()); }
            public boolean selectionMode() { return fileSelectionMode; }
            public boolean selected(Path path) { return selectedFilePaths.contains(path); }
            public void select(Path path, boolean selected) {
                if (selected) selectedFilePaths.add(path); else selectedFilePaths.remove(path);
                quickInfoDelay.stop();
                updateSelectionUi.run();
            }
            public void animateSelection(CheckBox box) { animateFileSelection(box); }
            public ContextMenu menu(Path path) { return createFileContextMenu(path); }
            public void toggleFavourite(FileItem item) {
                Path path = item.getPath();
                try {
                    if (favouritePaths.contains(path)) { favouriteRepo.remove(path); favouritePaths.remove(path); }
                    else { favouriteRepo.add(path); favouritePaths.add(path); }
                    updateSelectionUi.run();
                } catch (SQLException exception) {
                    showDatabaseError(tr("ui.unable_to_update_favourite"), tr("ui.fileflow_could_not_save_this_favourite_change"));
                }
            }
            public void removeTag(FileItem item, String tag) {
                try {
                    tagRepo.removeTagFromFile(item.getPath(), tag);
                    List<String> saved = fileTags.get(item.getPath());
                    if (saved != null) { saved.remove(tag); if (saved.isEmpty()) fileTags.remove(item.getPath()); }
                    fileList.refresh();
                } catch (SQLException exception) {
                    showDatabaseError(tr("ui.unable_to_remove_tag"), tr("ui.fileflow_could_not_remove_this_tag_from_the_file"));
                }
            }
            public void activate(FileItem item) {
                quickInfoDelay.stop();
                if (filesViewMode == FilesViewMode.BROWSE && item.isDirectory()) {
                    currentFolder = item.getPath().toAbsolutePath().normalize();
                    updateBrowseHeader(backButton, breadcrumbBar, fileList, searchField, typeFilter, sortBy);
                    populateFileList(fileList, searchField.getText(), typeFilter.getValue(), sortBy.getValue());
                } else if (!item.isDirectory() && !fileSelectionMode) openFile(item.getPath());
            }
            public void addTag(FileItem item) {

                                Dialog<String> dialog = new Dialog<>();

                                dialog.setTitle(tr("ui.add_tag"));
                                dialog.setHeaderText(tr("ui.add_a_tag_to") + item.getName());

                                ButtonType addButtonType = new ButtonType(tr("common.add"));

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
                                        existingTagBox.setPromptText(tr("ui.no_existing_tags"));
                                }

                                TextField newTagField = new TextField();
                                newTagField.setPromptText(tr("ui.create_a_new_tag"));

                                VBox dialogContent = new VBox(
                                        10,
                                        new Label(tr("ui.existing_tag")),
                                        existingTagBox,
                                        new Label(tr("ui.or_create_a_new_tag")),
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

                                showOceanDialog(dialog).ifPresent(selectedTag -> {
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

                                                fileList.refresh();
                                        } catch (SQLException e) {
                                                showDatabaseError(
                                                        tr("ui.unable_to_add_tag"),
                                                        tr("ui.fileflow_could_not_save_this_tag_change")
                                                );

                                                e.printStackTrace();
                                        }
                                });

            }
        }));

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

        filesContent.getChildren().add(selectionBar);
        filesContent.getChildren().add(fileList);

        return filesContent;
    }

    private VBox createFavouritesContent() {

        VBox content = new VBox(18);
        content.setMinHeight(0);
        content.setMaxHeight(Double.MAX_VALUE);

        content.getStyleClass().add("favourites-content");

        try {
                List<Path> favouritePaths = favouriteRepo.getAll();

                if (favouritePaths.isEmpty()) {

                        VBox emptyState = new VBox(8);
                        emptyState.getStyleClass().add(
                                "favourites-empty-state"
                        );

                        Label emptyIcon = new Label("☆");
                        emptyIcon.getStyleClass().add(
                                "favourites-empty-icon"
                        );

                        Label emptyTitle = new Label(
                                tr("ui.nothing_saved_yet")
                        );
                        emptyTitle.getStyleClass().add(
                                "favourites-empty-title"
                        );

                        Label emptyDescription = new Label(
                                tr("ui.star_a_file_in_files_to_keep_it_here_for_quick_access")
                        );
                        emptyDescription.getStyleClass().add(
                                "favourites-empty-description"
                        );
                        emptyDescription.setWrapText(true);

                        emptyState.getChildren().addAll(
                                emptyIcon,
                                emptyTitle,
                                emptyDescription
                        );

                        content.getChildren().add(emptyState);

                        return content;
                }

                // Search
                TextField favouritesSearchField = new TextField();
                favouritesSearchField.setPromptText(tr("ui.search_favourites"));
                favouritesSearchField.setText(favouritesSearchQuery);
                favouritesSearchField.getStyleClass().add(
                        "favourites-search-field"
                );

                HBox.setHgrow(favouritesSearchField, Priority.ALWAYS);
                favouritesSearchField.setMaxWidth(Double.MAX_VALUE);

                // Sort
                ComboBox<String> sortBox = new ComboBox<>();
        I18n.localizeChoices(sortBox);

                sortBox.getItems().addAll(
                        "Name A-Z",
                        "Name Z-A",
                        "File Type"
                );

                sortBox.setValue(favouritesSortOrder);
                sortBox.setPrefWidth(150);

                sortBox.getStyleClass().add(
                        "favourites-sort-box"
                );

                // Total count
                Label countLabel = new Label(
                        favouritePaths.size() + tr("ui.saved_files")
                );

                countLabel.getStyleClass().add(
                        "favourites-count"
                );

                // Toolbar
                HBox toolbar = new HBox(
                        12,
                        favouritesSearchField,
                        sortBox,
                        countLabel
                );

                toolbar.setAlignment(Pos.CENTER_LEFT);
                toolbar.getStyleClass().add(
                        "favourites-toolbar"
                );

                // Container for file rows
                VBox rowsContainer = new VBox(10);

                // Message when a search returns no matches
                Label noMatchesLabel = new Label(
                        tr("ui.no_matching_favourites_try_another_search")
                );

                noMatchesLabel.getStyleClass().add(
                        "favourites-no-results"
                );

                // Store each row by its file path
                Map<Path, HBox> rowsByPath = new HashMap<>();

                VBox listContent = new VBox(10, rowsContainer, noMatchesLabel);

                ScrollPane favouritesScroll = new ScrollPane(listContent);
                favouritesScroll.getStyleClass().add("favourites-scroll");

                favouritesScroll.setFitToWidth(true);
                favouritesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                favouritesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

                favouritesScroll.setMinHeight(0);
                VBox.setVgrow(favouritesScroll, Priority.ALWAYS);

                content.getChildren().addAll(
                        toolbar,
                        favouritesScroll
                );

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

                        // Prevent long names and paths from expanding the row
                        fileNameLabel.setMinWidth(0);
                        fileNameLabel.setMaxWidth(Double.MAX_VALUE);
                        fileNameLabel.setTextOverrun(
                                javafx.scene.control.OverrunStyle.ELLIPSIS
                        );

                        filePathLabel.setMinWidth(0);
                        filePathLabel.setMaxWidth(Double.MAX_VALUE);
                        filePathLabel.setTextOverrun(
                                javafx.scene.control.OverrunStyle.ELLIPSIS
                        );

                        // Show full information on hover
                        fileNameLabel.setTooltip(
                                new javafx.scene.control.Tooltip(fileNameLabel.getText())
                        );

                        filePathLabel.setTooltip(
                                new javafx.scene.control.Tooltip(filePath.toString())
                        );

                        fileInfo.setMinWidth(0);
                        fileInfo.setMaxWidth(Double.MAX_VALUE);

                        HBox.setHgrow(fileInfo, Priority.ALWAYS);

                        Node filePreview = createStoredFilePreview(filePath);

                        Button favouriteButton = new Button("★");
                        favouriteButton.getStyleClass().add(
                                "favourite-button"
                        );

                        Button moreButton = createFileActionsButton(filePath);

                        // Keep action buttons together
                        HBox actionsBox = new HBox(
                                8,
                                favouriteButton,
                                moreButton
                        );

                        actionsBox.setAlignment(Pos.CENTER_RIGHT);
                        actionsBox.getStyleClass().add(
                                "favourites-row-actions"
                        );

                        actionsBox.setMinWidth(Region.USE_PREF_SIZE);

                        // Assemble the file card
                        HBox row = new HBox(
                                12,
                                filePreview,
                                fileInfo,
                                actionsBox
                        );

                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setMinWidth(0);
                        row.setMaxWidth(Double.MAX_VALUE);

                        row.getStyleClass().addAll(
                                "file-row",
                                "favourites-file-row"
                        );

                        PauseTransition clickDelay =
                        new PauseTransition(Duration.millis(160));

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
                                        // Remove favourite from database
                                        favouriteRepo.remove(filePath);

                                        // Prevent delayed Quick Info from opening
                                        clickDelay.stop();

                                        // Rebuild Favourites with the latest database state
                                        refreshCurrentPage();

                                } catch (SQLException e) {

                                        showDatabaseError(
                                                tr("ui.unable_to_update_favourite"),
                                                tr("ui.fileflow_could_not_remove_this_file_from_your_favourites")
                                        );

                                        e.printStackTrace();
                                }
                        });

                        rowsByPath.put(filePath, row);

                        rowsContainer.getChildren().add(row);
                }

                // Comparator: sort files alphabetically by name
                Comparator<Path> byName = Comparator.comparing(
                        path -> path.getFileName()
                                .toString()
                                .toLowerCase(Locale.ROOT)
                );

                // Comparator: sort files by extension, then name
                Comparator<Path> byType = Comparator
                        .comparing((Path path) -> {

                        String name = path.getFileName().toString();
                        int dot = name.lastIndexOf('.');

                        return dot > 0 && dot < name.length() - 1
                                ? name.substring(dot + 1).toLowerCase(Locale.ROOT)
                                : "";
                        })
                        .thenComparing(byName);

                // Update displayed rows based on search and sort
                Runnable updateFavourites = () -> {

                String query = favouritesSearchField.getText()
                        .trim()
                        .toLowerCase(Locale.ROOT);

                Comparator<Path> comparator = switch (sortBox.getValue()) {

                        case "Name Z-A" -> byName.reversed();

                        case "File Type" -> byType;

                        default -> byName;
                };

                List<HBox> matchingRows = favouritePaths.stream()

                        .filter(path -> {

                                String name = path.getFileName()
                                        .toString()
                                        .toLowerCase(Locale.ROOT);

                                String fullPath = path.toString()
                                        .toLowerCase(Locale.ROOT);

                                return name.contains(query)
                                        || fullPath.contains(query);
                        })

                        .sorted(comparator)

                        .map(rowsByPath::get)

                        .toList();

                rowsContainer.getChildren().setAll(matchingRows);

                boolean noMatches = matchingRows.isEmpty();

                noMatchesLabel.setVisible(noMatches);
                noMatchesLabel.setManaged(noMatches);
                };

                // React to search changes
                favouritesSearchField.textProperty().addListener(
                        (observable, oldValue, newValue) -> {

                        favouritesSearchQuery = newValue;

                        updateFavourites.run();
                        }
                );

                // React to sorting changes
                sortBox.valueProperty().addListener(
                        (observable, oldValue, newValue) -> {

                        favouritesSortOrder = newValue;

                        updateFavourites.run();
                        }
                );

                // Initial render
                updateFavourites.run();

        } catch(SQLException e) {
                content.getChildren().add(
                        createDatabaseErrorState(
                                tr("ui.favourites_are_temporarily_unavailable"),
                                tr("ui.fileflow_could_not_load_your_favourites"),
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

        content.setMinHeight(0);
        content.setMaxHeight(Double.MAX_VALUE);

        ToggleButton exactTab = new ToggleButton(tr("ui.exact_duplicates"));
        ToggleButton similarTab = new ToggleButton(tr("ui.similar_images"));

        ToggleGroup modeGroup = new ToggleGroup();

        exactTab.setToggleGroup(modeGroup);
        similarTab.setToggleGroup(modeGroup);

        exactTab.setSelected(true);

        HBox modeTabs = new HBox(8, exactTab, similarTab);

        modeTabs.getStyleClass().add(
                "duplicates-mode-tabs"
        );

        exactTab.getStyleClass().add(
                "duplicates-mode-tab"
        );

        similarTab.getStyleClass().add(
                "duplicates-mode-tab"
        );

        Button analyseButton = new Button(tr("ui.analyse_duplicates"));
        analyseButton.getStyleClass().add("duplicates-analyse-button");

        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("duplicates-status");
        statusLabel.setWrapText(true);

        HBox duplicateSummaryRow = new HBox(12);

        duplicateSummaryRow.setVisible(false);
        duplicateSummaryRow.setManaged(false);

        duplicateSummaryRow.getStyleClass().add("duplicate-summary-row");

        VBox resultsContainer = new VBox(12);

        resultsContainer.getStyleClass().add("duplicate-results");

        Label duplicateGroupsTitle =
                new Label(tr("ui.duplicate_groups"));

        duplicateGroupsTitle.getStyleClass().add(
                "section-title"
        );

        VBox.setMargin(
                duplicateGroupsTitle,
                new javafx.geometry.Insets(8, 0, 0, 0)
        );

        duplicateGroupsTitle.setVisible(false);
        duplicateGroupsTitle.setManaged(false);
        
        ScrollPane resultsScrollPane = new ScrollPane(resultsContainer);

        resultsScrollPane.setFitToWidth(true);

        resultsScrollPane.setMinHeight(0);

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
        progressBar.setMaxWidth(Double.MAX_VALUE);

        progressBar.getStyleClass().add("duplicates-progress");

        if (latestScanResult == null) {

                analyseButton.setDisable(true);

                statusLabel.setText(
                        tr("ui.scan_a_folder_before_analysing_duplicates")
                );

        } else if (duplicateAnalysisCompleted) {

                analyseButton.setText(
                        tr("ui.analyse_again")
                );

                if (latestDuplicateGroups.isEmpty()) {

                        statusLabel.setText(
                                tr("ui.no_duplicate_files_found")
                        );

                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);

                } else {

                        statusLabel.setVisible(false);
                        statusLabel.setManaged(false);
                }

        } else {

                statusLabel.setText(
                        tr("ui.no_duplicate_analysis_yet")
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
                                                                ? tr("ui.no_duplicate_candidates_found")
                                                                : String.format(
                                                                        tr("ui.hashing_d_d_files"),
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

                analyseButton.setText(tr("ui.cancel_analysis"));

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

                        analyseButton.setText(tr("ui.analyse_again"));

                        progressBar.setVisible(false);
                        progressBar.setManaged(false);

                        if (latestDuplicateGroups.isEmpty()) {

                                statusLabel.setText(
                                        tr("ui.no_duplicate_files_found")
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
                                        ? tr("ui.analyse_again")
                                        : tr("ui.analyse_duplicates")
                        );

                        progressBar.setVisible(false);
                        progressBar.setManaged(false);

                        statusLabel.setText(
                                tr("ui.duplicate_analysis_failed")
                        );

                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);

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
                                        ? tr("ui.analyse_again")
                                        : tr("ui.analyse_duplicates")
                        );

                        progressBar.setVisible(false);
                        progressBar.setManaged(false);

                        statusLabel.setText(
                                tr("ui.analysis_cancelled")
                        );

                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);
                });

                currentDuplicateTask = duplicateTask;

                Thread duplicateThread = new Thread(duplicateTask);

                duplicateThread.setDaemon(true);
                duplicateThread.start();
        });

        // Exact duplicate interface
        VBox exactContent = new VBox(18);

        exactContent.setSpacing(10);
        exactContent.setMinHeight(0);
        exactContent.setMaxHeight(Double.MAX_VALUE);

        VBox.setVgrow(
                exactContent,
                Priority.ALWAYS
        );

        // Card heading
        Label exactCardTitle = new Label(tr("ui.content_match"));
        exactCardTitle.getStyleClass().add(
                "duplicates-scan-card-title"
        );

        // Card subtitle
        Label exactCardSubtitle = new Label(
                tr("ui.compare_file_contents_to_identify_exact_duplicates")
        );

        exactCardSubtitle.setWrapText(true);
        exactCardSubtitle.getStyleClass().add(
                "duplicates-scan-card-subtitle"
        );

        // Text on the left side
        VBox exactCardText = new VBox(
                5,
                exactCardTitle,
                exactCardSubtitle
        );

        exactCardText.setMinWidth(0);
        HBox.setHgrow(exactCardText, Priority.ALWAYS);

        // Button on the right side
        analyseButton.setMinWidth(
                Region.USE_PREF_SIZE
        );

        HBox exactCardTop = new HBox(
                16,
                exactCardText,
                analyseButton
        );

        exactCardTop.setAlignment(Pos.CENTER_LEFT);

        // Analysis card
        VBox exactScanCard = new VBox(
                14,
                exactCardTop,
                progressBar,
                statusLabel
        );

        exactScanCard.getStyleClass().add(
                "duplicates-scan-card"
        );

        // Complete Exact Duplicates section
        exactContent.getChildren().addAll(
                exactScanCard,
                duplicateSummaryRow,
                duplicateGroupsTitle,
                resultsScrollPane
        );


        VBox similarContent = new VBox(10);

        similarContent.setMinHeight(0);
        similarContent.setMaxHeight(Double.MAX_VALUE);

        VBox.setVgrow(
                similarContent,
                Priority.ALWAYS
        );

        Label similarDescription = new Label(
                tr("ui.find_visually_similar_images_even_when_their_sizes_or_resolutions_differ")
        );
        similarDescription.setWrapText(true);
        similarDescription.getStyleClass().add(
                "duplicates-mode-description"
        );

        Label sensitivityLabel =
                new Label(tr("ui.detection_sensitivity"));

        ComboBox<String> sensitivityCombo =
                new ComboBox<>();
        I18n.localizeChoices(sensitivityCombo);

        sensitivityCombo.getItems().addAll(
                "Strict",
                "Balanced",
                "Broad"
        );

        sensitivityCombo.setValue(similarDetectionMode);
        sensitivityCombo.setPrefWidth(200);

        Button similarAnalyseButton =
                new Button(tr("ui.analyse_similar_images"));

        similarAnalyseButton.getStyleClass().add(
                "duplicates-analyse-button"
        );

        similarAnalyseButton.setDisable(latestScanResult == null);

        Label similarStatusLabel = new Label();

        ProgressBar similarProgressBar = new ProgressBar(0);

        similarProgressBar.setPrefWidth(360);

        similarProgressBar.setVisible(false);
        similarProgressBar.setManaged(false);

        similarProgressBar.getStyleClass().add(
                "duplicates-progress"
        );

        VBox similarResults = new VBox(12);

        ScrollPane similarResultsScroll =
                new ScrollPane(similarResults);

        similarResultsScroll.setMinHeight(0);

        VBox.setVgrow(
                similarResultsScroll,
                Priority.ALWAYS
        );

        similarResultsScroll.setFitToWidth(true);

        similarResultsScroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        similarResultsScroll.getStyleClass().add(
                "duplicates-scroll"
        );

        boolean hasSimilarResults =
                similarAnalysisCompleted
                && !latestSimilarPairs.isEmpty();

        similarResultsScroll.setVisible(hasSimilarResults);
        similarResultsScroll.setManaged(hasSimilarResults);

        if (hasSimilarResults) {
                populateSimilarImageResults(similarResults);
        }

        sensitivityCombo.setOnAction(event -> {

                String selectedMode = sensitivityCombo.getValue();

                if (selectedMode == null
                        || selectedMode.equals(similarDetectionMode)) {
                        return;
                }

                similarDetectionMode = selectedMode;

                similarAnalysisCompleted = false;
                latestSimilarPairs = List.of();

                similarResults.getChildren().clear();

                similarResultsScroll.setVisible(false);
                similarResultsScroll.setManaged(false);

                similarStatusLabel.setText(
                        tr("ui.detection_mode_changed_analyse_again")
                );
        });

        similarStatusLabel.getStyleClass().add(
                "duplicates-status"
        );

        if (latestScanResult == null) {
        similarStatusLabel.setText(
                tr("ui.scan_a_folder_before_analysing_images")
        );
        } else if (similarAnalysisCompleted) {
                similarStatusLabel.setText(
                        getSimilarImageSummary()
                );
        } else {
                similarStatusLabel.setText(
                        tr("ui.no_similar_image_analysis_yet")
                );
        }

        similarAnalyseButton.setOnAction(event -> {

                if (currentSimilarTask != null) {
                        currentSimilarTask.cancel();
                        return;
                }

                if (latestScanResult == null) {
                        return;
                }

                ScanResult sourceScan = latestScanResult;

                List<FileItem> items =
                        List.copyOf(sourceScan.getItems());

                String selectedMode = sensitivityCombo.getValue();

                final int hashThreshold = switch (selectedMode) {
                        case "Strict" -> 6;
                        case "Broad" -> 12;
                        default -> 8;
                };

                final double detailThreshold = switch (selectedMode) {
                        case "Strict" -> 0.12;
                        case "Broad" -> 0.20;
                        default -> 0.16;
                };

                similarAnalyseButton.setText(tr("ui.cancel_analysis"));

                similarProgressBar.setProgress(0);
                similarProgressBar.setVisible(true);
                similarProgressBar.setManaged(true);
                
                // Always show analysis status when a new task starts
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);

                sensitivityCombo.setDisable(true);

                similarStatusLabel.setText(
                        tr("ui.analysing_images")
                );

                Task<List<SimilarImagePair>> similarTask =
                        new Task<>() {

                                @Override
                                protected List<SimilarImagePair> call() {

                                        return similarImageService.findSimilarImages(
                                                items,
                                                hashThreshold,
                                                detailThreshold,
                                                this::isCancelled,
                                                (processed, total) -> {

                                                updateProgress(processed, Math.max(total, 1));

                                                updateMessage(
                                                        tr("ui.reading_images")
                                                                + processed + " / " + total
                                                );
                                                }
                                        );
                                }
                        };

                similarProgressBar.progressProperty().bind(
                        similarTask.progressProperty()
                );

                similarStatusLabel.textProperty().bind(
                        similarTask.messageProperty()
                );

                currentSimilarTask = similarTask;

                Runnable finishSimilarTask = () -> {

                        similarProgressBar.progressProperty().unbind();
                        similarStatusLabel.textProperty().unbind();

                        similarProgressBar.setVisible(false);
                        similarProgressBar.setManaged(false);

                        similarAnalyseButton.setText(tr("ui.analyse_similar_images"));
                        similarAnalyseButton.setDisable(latestScanResult == null);

                        sensitivityCombo.setDisable(false);

                        if (currentSimilarTask == similarTask) {
                                currentSimilarTask = null;
                        }
                };

                similarTask.setOnSucceeded(done -> {

                        finishSimilarTask.run();

                        if (latestScanResult != sourceScan) {
                                similarStatusLabel.setText(
                                        tr("ui.scan_changed_please_analyse_again")
                                );
                                return;
                        }

                        latestSimilarPairs = similarTask.getValue();

                        similarAnalysisCompleted = true;

                        // Display the matching image pairs
                        populateSimilarImageResults(similarResults);

                        boolean hasMatches = !latestSimilarPairs.isEmpty();

                        similarResultsScroll.setVisible(hasMatches);
                        similarResultsScroll.setManaged(hasMatches);

                        similarStatusLabel.setText(
                                getSimilarImageSummary()
                        );
                });

                similarTask.setOnFailed(failed -> {

                        finishSimilarTask.run();

                        similarStatusLabel.setText(
                                tr("ui.similar_image_analysis_failed")
                        );

                        similarTask.getException().printStackTrace();
                });

                similarTask.setOnCancelled(done -> {

                        finishSimilarTask.run();

                        similarStatusLabel.setText(
                                tr("ui.analysis_cancelled")
                        );
                });

                Thread similarThread = new Thread(similarTask);

                similarThread.setDaemon(true);
                similarThread.start();
        });

        // Similar Images analysis card
        Label similarCardTitle = new Label(
                tr("ui.visual_similarity")
        );

        similarCardTitle.getStyleClass().add(
                "similar-scan-card-title"
        );

        // Reuse the existing description
        VBox similarCardText = new VBox(
                4,
                similarCardTitle,
                similarDescription
        );

        similarCardText.setMinWidth(0);
        HBox.setHgrow(similarCardText, Priority.ALWAYS);

        // Title and analyse button
        HBox similarCardTop = new HBox(
                16,
                similarCardText,
                similarAnalyseButton
        );

        similarCardTop.setAlignment(Pos.CENTER_LEFT);

        similarAnalyseButton.setMinWidth(
                Region.USE_PREF_SIZE
        );

        // Sensitivity controls
        HBox sensitivityRow = new HBox(
                12,
                sensitivityLabel,
                sensitivityCombo
        );

        sensitivityRow.setAlignment(Pos.CENTER_LEFT);

        Region sensitivitySpacer = new Region();

        HBox.setHgrow(
                sensitivitySpacer,
                Priority.ALWAYS
        );

        sensitivityRow.getChildren().add(
                1,
                sensitivitySpacer
        );

        // Complete analysis card
        VBox similarScanCard = new VBox(
                12,
                similarCardTop,
                sensitivityRow,
                similarProgressBar,
                similarStatusLabel
        );

        similarScanCard.getStyleClass().add(
                "similar-scan-card"
        );

        similarProgressBar.setMaxWidth(
                Double.MAX_VALUE
        );

        similarStatusLabel.setWrapText(true);

        similarContent.getChildren().addAll(
                similarScanCard,
                similarResultsScroll
        );

        similarContent.setVisible(false);
        similarContent.setManaged(false);

        // Shared header and both analysis modes
        content.getChildren().addAll(
                modeTabs,
                exactContent,
                similarContent
        );

        // Handle mode switching
        modeGroup.selectedToggleProperty().addListener(
                (observable, oldToggle, newToggle) -> {

                if (newToggle == null) {
                        if (oldToggle != null) {
                        oldToggle.setSelected(true);
                        }
                        return;
                }

                boolean showSimilar = newToggle == similarTab;

                similarTabSelected = showSimilar;

                exactContent.setVisible(!showSimilar);
                exactContent.setManaged(!showSimilar);

                similarContent.setVisible(showSimilar);
                similarContent.setManaged(showSimilar);
                }
        );

        if (similarTabSelected) {
                similarTab.setSelected(true);
        }

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
        tagsContent.getStyleClass().add("tags-content");

        VBox tagList = new VBox(10);
        tagList.getStyleClass().add("tags-list");

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
                                        tr("ui.no_scan_history_yet")
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
                        new Button(tr("ui.clear_history"));

                clearHistoryButton.getStyleClass().add(
                        "secondary-button"
                );

                clearHistoryButton.setOnAction(event -> {

                        Alert confirmation =
                                new Alert(
                                        Alert.AlertType.CONFIRMATION
                                );

                        confirmation.setTitle(
                                tr("ui.clear_history")
                        );

                        confirmation.setHeaderText(
                                tr("ui.clear_all_scan_history")
                        );

                        confirmation.setContentText(
                                tr("ui.this_will_remove_all_scan_history_from_fileflow_your_files_will_not_be_affected")
                        );

                        ButtonType clearButtonType =
                                new ButtonType(
                                        tr("ui.clear_history"),
                                        ButtonBar.ButtonData.OK_DONE
                                );

                        ButtonType cancelButtonType =
                                new ButtonType(
                                        tr("common.cancel"),
                                        ButtonBar.ButtonData.CANCEL_CLOSE
                                );

                        confirmation
                                .getButtonTypes()
                                .setAll(
                                        cancelButtonType,
                                        clearButtonType
                                );

                        ButtonType result =
                                showOceanDialog(confirmation)
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
                                        tr("ui.unable_to_clear_history"),
                                        tr("ui.fileflow_could_not_clear_your_scan_history")
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

                Label historyTitle = new Label(tr("ui.previous_scans"));
                historyTitle.getStyleClass().add("history-page-title");

                Label historyCount = new Label(
                        historyEntries.size() == 1
                                ? tr("ui.1_scan_in_your_history")
                                : historyEntries.size() + tr("ui.scans_in_your_history")
                );
                historyCount.getStyleClass().add("history-page-subtitle");

                VBox historyHeading = new VBox(
                        4,
                        historyTitle,
                        historyCount
                );

                HBox historyActions = new HBox(
                        12,
                        historyHeading,
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
                                                tr("ui.scanned_s_d_files_s"),
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
                                
                        fileInfo.setMinWidth(0);
                        fileInfo.setMaxWidth(Double.MAX_VALUE);
                        HBox.setHgrow(fileInfo, Priority.ALWAYS);

                        folderNameLabel.setMinWidth(0);
                        folderNameLabel.setTextOverrun(
                                javafx.scene.control.OverrunStyle.ELLIPSIS
                        );

                        pathLabel.setMinWidth(0);
                        pathLabel.setTextOverrun(
                                javafx.scene.control.OverrunStyle.ELLIPSIS
                        );
                        pathLabel.setTooltip(
                                new Tooltip(entry.getFolderPath().toString())
                        );

                        SVGPath historyFolderIcon = new SVGPath();
                        historyFolderIcon.setContent(
                                "M3 7 V19 Q3 21 5 21 H19 "
                                + "Q21 21 21 19 V9 Q21 7 19 7 "
                                + "H12 L10 5 H5 Q3 5 3 7 Z"
                        );
                        historyFolderIcon.getStyleClass().add(
                                "history-folder-icon"
                        );

                        StackPane historyIconBox =
                                new StackPane(historyFolderIcon);

                        historyIconBox.setMinSize(44, 44);
                        historyIconBox.setPrefSize(44, 44);
                        historyIconBox.setMaxSize(44, 44);

                        historyIconBox.getStyleClass().add(
                                "history-icon-box"
                        );

                        Button scanAgainButton = new Button(tr("ui.scan_again"));

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
                                                tr("ui.open_folder")
                                );

                        MenuItem copyPathItem =
                                new MenuItem(
                                        tr("ui.copy_path")
                                );

                        MenuItem removeHistoryItem =
                                new MenuItem(
                                        tr("ui.remove_from_history")
                                );

                        ContextMenu historyMenu =
                                oceanMenu(
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

                        HBox actionsRow = new HBox(
                                10,
                                scanAgainButton,
                                moreButton
                        );

                        actionsRow.setAlignment(Pos.CENTER_RIGHT);

                        HBox historyCard = new HBox(
                                16,
                                historyIconBox,
                                fileInfo,
                                actionsRow
                        );

                        historyCard.setAlignment(Pos.CENTER_LEFT);
                        historyCard.setMinWidth(0);
                        historyCard.setMaxWidth(Double.MAX_VALUE);

                        historyCard.getStyleClass().add("history-card");

                        content.getChildren().add(
                                historyCard
                        );
                }

        } catch (SQLException exception) {
                content.getChildren().add(
                        createDatabaseErrorState(
                                tr("ui.history_is_temporarily_unavailable"),
                                tr("ui.fileflow_could_not_load_your_scan_history"),
                                this::refreshCurrentPage
                        )
                );

                exception.printStackTrace();
        }
                return scrollPane;
        }

    private void applyLanguageChange() {
        root.getStyleClass().remove("language-zh");
        if (I18n.instance().language() == AppLanguage.SIMPLIFIED_CHINESE) root.getStyleClass().add("language-zh");
        cancelActiveShortcutCapture();
        hideQuickInfo();
        if (quickInfoPanel != null) {
            ((javafx.scene.layout.Pane) ((StackPane) root.getCenter()).getChildren().get(1)).getChildren().remove(quickInfoPanel);
            quickInfoPanel = null;
        }
        if (sharedFileMenu != null) { sharedFileMenu.hide(); sharedFileMenu = null; }
        refreshButton.setText(tr("common.refresh"));
        scanButton.setText(scanning ? tr("ui.cancel_scan") : tr("ui.scan_folder"));
        if (homeScanButton != null) homeScanButton.setText(scanning ? tr("ui.cancel_scan") : tr("ui.scan_a_folder"));
        if (displayedStorageVolume != null) {
            storageUsedLabel.setText(FileSizeFormatter.format(displayedStorageVolume.getUsedBytes()) + tr("ui.used")
                    + FileSizeFormatter.format(displayedStorageVolume.getTotalBytes()) + tr("ui.total"));
            storageFreeLabel.setText(FileSizeFormatter.format(displayedStorageVolume.getFreeBytes()) + tr("ui.free"));
        } else if (storageNameLabel != null) {
            storageNameLabel.setText(tr("ui.storage")); storageUsedLabel.setText(tr("ui.unavailable"));
        }
        updateOceanStatus();
        refreshCurrentPage();
    }

    private VBox createLanguageSettingsCard() {
        Label title = OceanComponents.label(tr("settings.language.title"), "settings-card-title");
        Label description = OceanComponents.label(tr("settings.language.description"), "settings-card-description");
        description.setWrapText(true);
        ComboBox<AppLanguage> languages = new ComboBox<>();
        languages.getItems().setAll(AppLanguage.values());
        languages.setValue(I18n.instance().language());
        languages.setAccessibleText(tr("settings.language.label"));
        languages.setId("language-selector");
        languages.setMinWidth(180);
        languages.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(AppLanguage value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.toString());
                setStyle("-fx-text-fill: #e5efff; -fx-font-family: 'Microsoft YaHei UI';");
            }
        });
        languages.setOnAction(event -> {
            AppLanguage selected = languages.getValue();
            if (selected != null && !I18n.instance().setLanguage(selected)) showStatusWarning(tr("settings.language.save_failed"));
        });
        Label hint = OceanComponents.label(tr("settings.language.saved"), "settings-card-description");
        hint.setWrapText(true);
        VBox card = new VBox(12, title, description, languages, hint);
        card.getStyleClass().add("settings-card");
        return card;
    }

    private VBox createSettingsContent() {
        VBox content = new VBox(18);
        content.getStyleClass().add(
                "settings-content"
        );

        Label safetyTitle = new Label(tr("ui.safety"));

        safetyTitle.getStyleClass().add("settings-section-title");

        SlideSwitch confirmDeleteCheckBox = new SlideSwitch(tr("ui.confirm_before_moving_files_to_recycle_bin"));
        Label confirmDeleteTitle = OceanComponents.label(tr("ui.confirm_before_moving_files_to_recycle_bin"), "settings-action-title");
        confirmDeleteTitle.setWrapText(true);
        confirmDeleteTitle.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(confirmDeleteTitle, Priority.ALWAYS);
        HBox safetyRow = new HBox(18, confirmDeleteTitle, confirmDeleteCheckBox);
        safetyRow.setAlignment(Pos.CENTER_LEFT);

        boolean confirmBeforeDelete =
                preferences.getBoolean(
                        CONFIRM_BEFORE_DELETE_KEY,
                        true
                );

        confirmDeleteCheckBox.setSelected(
                confirmBeforeDelete
        );

        Label confirmDeleteDescription = new Label(
                tr("ui.when_enabled_fileflow_asks_for_confirmation_before_moving_an_individual_file_to_the_r")
        );

        confirmDeleteDescription.setWrapText(
                true
        );

        confirmDeleteDescription
                .getStyleClass()
                .add(
                        "settings-description"
        );

        Label deleteWarning = new Label(
                tr("ui.files_moved_to_the_recycle_bin_can_be_restored_while_they_remain_there_disk_space_may")
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
                        safetyRow,
                        confirmDeleteDescription,
                        deleteWarning
                );

        safetyCard.getStyleClass().add(
                "settings-card"
        );

        content.getChildren().addAll(
                createLanguageSettingsCard(),
                safetyTitle,
                safetyCard,
                createKeyboardShortcutsSettingsCard()
        );

        return content;
    }

    private VBox createKeyboardShortcutsSettingsCard() {

                Label titleLabel =
                        new Label(
                                tr("ui.keyboard_shortcuts")
                        );
                titleLabel.getStyleClass().add(
                        "settings-card-title"
                );

                Label descriptionLabel =
                        new Label(
                                tr("ui.customise_shortcuts_for_common_fileflow_actions")
                        );
                descriptionLabel.getStyleClass().add(
                        "settings-card-description"
                );

                VBox shortcuts =
                        new VBox(
                                8,
                                createShortcutSettingRow(
                                        tr("ui.refresh_current_scan"),
                                        ShortcutAction.REFRESH_SCAN
                                ),
                                createShortcutSettingRow(
                                        tr("ui.focus_file_search"),
                                        ShortcutAction.FOCUS_SEARCH
                                ),
                                createShortcutSettingRow(
                                        tr("ui.scan_a_new_folder"),
                                        ShortcutAction.SCAN_FOLDER
                                )
                        );

                Button resetButton =
                        new Button(
                                tr("ui.reset_to_defaults")
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
                                tr("ui.change")
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
                        tr("ui.press_a_shortcut")
                );

                changeButton.setText(
                        tr("common.cancel")
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
                                        tr("ui.use_a_modifier_key")
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
                                        tr("ui.already_in_use")
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
                                tr("ui.change")
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

        
        private void showBulkRecycleBinDialog(
                TableView<FileItem> fileList,
                Runnable updateSelectionUi
        ) {

                if (selectedFilePaths.isEmpty()) {
                        return;
                }

                if (scanning || duplicateScanning
                        || currentSimilarTask != null) {
                        showStatusWarning(
                                tr("ui.please_finish_the_current_scan_or_analysis_first")
                        );
                        return;
                }

                List<Path> targets = selectedFilePaths.stream()
                        .map(path -> path.toAbsolutePath().normalize())
                        .distinct()
                        .sorted(Comparator.comparing(Path::toString))
                        .toList();

                // First version: prevent bulk removal of directories
                for (Path path : targets) {

                        if (Files.isDirectory(
                                path,
                                LinkOption.NOFOLLOW_LINKS
                        )) {
                        showFileActionError(
                                tr("ui.folder_selected"),
                                tr("ui.bulk_recycle_bin_currently_supports_files_only"),
                                tr("ui.remove_folders_from_your_selection_before_retrying")
                                        + path
                        );
                        return;
                        }
                }

                // Validate the entire selection before moving anything
                long totalBytes = 0;

                for (Path path : targets) {

                        if (!Files.isRegularFile(
                                path,
                                LinkOption.NOFOLLOW_LINKS
                        )) {
                        showFileActionError(
                                tr("ui.invalid_selection"),
                                tr("ui.an_item_is_missing_or_is_not_a_regular_file"),
                                path.toString()
                        );
                        return;
                        }

                        try {
                        totalBytes = Math.addExact(
                                totalBytes,
                                Files.size(path)
                        );
                        } catch (IOException | ArithmeticException exception) {

                        showFileActionError(
                                tr("ui.unable_to_validate_selection"),
                                tr("ui.the_selected_files_could_not_be_verified"),
                                path + "\n" + exception.getMessage()
                        );
                        return;
                        }
                }

                // Confirmation is mandatory for bulk operations
                Alert confirmation = new Alert(
                        Alert.AlertType.WARNING
                );

                confirmation.setTitle(tr("ui.bulk_recycle_bin"));
                applyDarkDialogStyle(confirmation);
                confirmation.getDialogPane().setGraphic(null);

                applyDarkDialogStyle(confirmation);

                confirmation.setHeaderText(
                        tr("ui.move") + targets.size()
                                + tr("ui.files_to_the_recycle_bin")
                );

                confirmation.setContentText(
                        tr("ui.selected_size")
                                + FileSizeFormatter.format(totalBytes)
                                + tr("ui.files_can_be_restored_from_the_windows_recycle_bin_while_they_remain_there_fileflow_t")
                );

                TextArea details = new TextArea(
                        String.join(
                                "\n",
                                targets.stream()
                                        .map(Path::toString)
                                        .toList()
                        )
                );

                details.setEditable(false);
                details.setWrapText(false);
                details.setPrefRowCount(8);
                details.setPrefColumnCount(60);

                confirmation.getDialogPane()
                        .setExpandableContent(details);

                confirmation.getDialogPane().setExpanded(true);

                ButtonType moveButtonType = new ButtonType(
                        tr("files.recycle"),
                        ButtonBar.ButtonData.OK_DONE
                );

                confirmation.getButtonTypes().setAll(
                        ButtonType.CANCEL,
                        moveButtonType
                );

                ButtonType answer = showOceanDialog(confirmation)
                        .orElse(ButtonType.CANCEL);

                if (answer != moveButtonType) {
                        return;
                }

                List<Path> moved = new ArrayList<>();
                List<String> failures = new ArrayList<>();

                for (Path path : targets) {

                        try {

                        // Recheck at execution time
                        if (!Files.isRegularFile(
                                path,
                                LinkOption.NOFOLLOW_LINKS
                        )) {
                                throw new IOException(
                                        tr("ui.file_is_missing_or_has_changed_type")
                                );
                        }

                        // Never use permanent delete as a fallback
                        recycleBinService.moveToRecycleBin(path);

                        moved.add(path);

                        filePreviewFactory.invalidate(path);

                        cleanupDeletedFileReferences(path);

                        } catch (IOException exception) {

                        failures.add(
                                path + ": " + exception.getMessage()
                        );

                        exception.printStackTrace();
                        }
                }

                // Keep failed items selected for easier retry
                selectedFilePaths.removeAll(moved);

                updateSelectionUi.run();

                fileList.getSelectionModel().clearSelection();
                fileList.refresh();

                // Refresh the scan once, not after every file
                if (!moved.isEmpty()) {
                        refreshAfterDelete();
                }

                Alert result = new Alert(
                        failures.isEmpty()
                                ? Alert.AlertType.INFORMATION
                                : Alert.AlertType.WARNING
                );

                result.setTitle(tr("ui.bulk_recycle_bin_results"));

                result.setHeaderText(
                        moved.size() + tr("ui.file_s_moved_to_recycle_bin")
                );

                String message =
                        "Moved: " + moved.size()
                        + tr("ui.failed") + failures.size();

                if (!failures.isEmpty()) {
                        message += tr("ui.failed_items")
                                + String.join(
                                        "\n",
                                        failures.stream().limit(5).toList()
                                );
                }

                result.setContentText(message);
                showOceanDialog(result);
        }

        
        private void animateFileSelection(CheckBox checkBox) {

                if (!checkBox.isSelected()) {
                        return;
                }

                ScaleTransition animation =
                        new ScaleTransition(
                                Duration.millis(140),
                                checkBox
                        );

                animation.setFromX(0.85);
                animation.setFromY(0.85);

                animation.setToX(1.0);
                animation.setToY(1.0);

                animation.playFromStart();
        }

    
    private void showBulkTagDialog(
                List<String> existingTags,
                Map<Path, List<String>> fileTags,
                TableView<FileItem> fileList
    ) {

        if (selectedFilePaths.isEmpty()) {
                return;
        }

        // Snapshot: the selection cannot change during this operation
        List<Path> targets =
                new ArrayList<>(selectedFilePaths);

        Dialog<String> dialog = new Dialog<>();

        dialog.setTitle(tr("ui.tag_selected_items"));
        dialog.setHeaderText(
                tr("ui.add_a_tag_to") + targets.size() + tr("ui.selected_items")
        );

        dialog.initOwner(root.getScene().getWindow());

        ButtonType applyType = new ButtonType(
                tr("ui.apply_tag"),
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                applyType,
                ButtonType.CANCEL
        );

        ComboBox<String> tagBox = new ComboBox<>();
        tagBox.getItems().addAll(existingTags);
        tagBox.setPromptText(tr("ui.choose_an_existing_tag"));
        tagBox.setMaxWidth(Double.MAX_VALUE);

        TextField newTagField = new TextField();
        newTagField.setPromptText(tr("ui.or_create_a_new_tag_79919"));

        Label note = new Label(
                tr("ui.selected_folders_receive_the_tag_themselves_files_inside_folders_are_not_tagged_recur")
        );

        note.setWrapText(true);

        VBox content = new VBox(
                10,
                new Label(tr("ui.existing_tag_eea45")),
                tagBox,
                new Label(tr("ui.new_tag")),
                newTagField,
                note
        );

        content.setPrefWidth(360);

        dialog.getDialogPane().setContent(content);

        // Prevent Apply when neither option is provided
        Button applyButton = (Button)
                dialog.getDialogPane().lookupButton(applyType);

        Runnable updateApplyState = () ->
                applyButton.setDisable(
                        newTagField.getText().trim().isEmpty()
                        && tagBox.getValue() == null
                );

        newTagField.textProperty().addListener(
                (obs, oldValue, newValue) ->
                        updateApplyState.run()
        );

        tagBox.valueProperty().addListener(
                (obs, oldValue, newValue) ->
                        updateApplyState.run()
        );

        updateApplyState.run();

        dialog.setResultConverter(buttonType -> {

                if (buttonType != applyType) {
                        return null;
                }

                String newTag = newTagField.getText().trim();

                // A typed tag takes priority
                return !newTag.isEmpty()
                        ? newTag
                        : tagBox.getValue();
        });

        showOceanDialog(dialog).ifPresent(tag -> {

                int succeeded = 0;
                List<Path> failed = new ArrayList<>();

                for (Path path : targets) {

                        try {

                                // Reuse the existing TagRepo API
                                tagRepo.assignTagToFile(path, tag);
                                succeeded++;

                        } catch (SQLException exception) {

                                failed.add(path);
                                exception.printStackTrace();
                        }
                }

                if (succeeded > 0) {

                // Reload authoritative data after the operation
                        try {

                                Map<Path, List<String>> updatedTags =
                                        tagRepo.getAllFileTags();

                                fileTags.clear();
                                fileTags.putAll(updatedTags);

                        } catch (SQLException exception) {

                                showStatusWarning(
                                        tr("ui.tags_were_applied_but_the_file_list_could_not_reload_tag_metadata")
                                );

                                exception.printStackTrace();
                        }

                        if (!existingTags.contains(tag)) {
                                existingTags.add(tag);
                        }

                        fileList.refresh();
                }

                Alert result = new Alert(
                        failed.isEmpty()
                                ? Alert.AlertType.INFORMATION
                                : Alert.AlertType.WARNING
                );

                result.setTitle(tr("ui.bulk_tag_results"));

                result.setHeaderText(
                        failed.isEmpty()
                                ? tr("ui.tag_applied_successfully")
                                : tr("ui.some_items_could_not_be_tagged")
                );

                result.setContentText(
                        String.format(
                                tr("ui.tag_s_nsuccessful_d_nfailed_d"),
                                tag,
                                succeeded,
                                failed.size()
                        )
                );

                showOceanDialog(result);
        });
    }

    private void populateFileList(
            TableView<FileItem> fileList,
            String searchText, 
            String selectedType,
            String selectedSort
        ) {
        String keyword = searchText.toLowerCase(Locale.ROOT);

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

        List<FileItem> sourceItems =
                filesViewMode == FilesViewMode.ALL_FILES
                && lastWorkspaceScanSnapshot != null
                && !lastWorkspaceScanSnapshot.resultsByRoot().isEmpty()
                        ? lastWorkspaceScanSnapshot.items()
                        : latestScanResult.getItems();

        cancelFileListing();
        long generation = fileListingGeneration;
        FilesViewMode mode = filesViewMode;
        Path folder = currentFolder;
        fileListingTask = fileListExecutor.submit(() -> {
                List<FileItem> filteredItems = sourceItems.stream()
                        .filter(item -> mode == FilesViewMode.ALL_FILES ? !item.isDirectory()
                                : folder != null && folder.equals(item.getPath().getParent()))
                        .filter(item -> item.getName().toLowerCase(Locale.ROOT).contains(keyword))
                        .filter(item -> matchesType(item, selectedType))
                        .sorted(comparator)
                        .toList();
                Platform.runLater(() -> {
                        if (generation == fileListingGeneration && fileList.getScene() != null
                                && "Files".equals(pageTitle.getText())) fileList.getItems().setAll(filteredItems);
                });
        });
    }

    private void cancelFileListing() {
        fileListingGeneration++;
        if (fileListingTask != null) {
            fileListingTask.cancel(false);
            if (fileListingTask instanceof Runnable queued) fileListExecutor.remove(queued);
            fileListingTask = null;
        }
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
                                new Label(tr("ui.no_tags_yet"))
                        );
                        return;
                }

                for (String tag : tags) {
                        List<Path> files = tagRepo.getFilesForTag(tag);

                        Label tagLabel = new Label(tag);

                        Label tagCountLabel = new Label(
                                files.size() == 1
                                        ? tr("ui.1_file")
                                        : files.size() + tr("ui.files")
                        );

                        tagCountLabel.getStyleClass().add(
                                "tag-count-badge"
                        );

                        Button tagMenuButton = new Button("⋯");
                        tagMenuButton.getStyleClass().add(
                                "tag-more-button"
                        );

                        MenuItem deleteTagItem = new MenuItem(tr("ui.delete_tag"));

                        deleteTagItem.setOnAction(event -> {

                                Alert confirmation = new Alert(
                                        Alert.AlertType.CONFIRMATION
                                );

                                confirmation.setTitle(tr("ui.delete_tag"));
                                confirmation.setHeaderText(
                                        tr("ui.delete") + tag + tr("ui.")
                                );

                                confirmation.setContentText(
                                        tr("ui.this_will_remove_the_tag_from_all_files")
                                );

                                showOceanDialog(confirmation).ifPresent(buttonType -> {

                                        if (buttonType == ButtonType.OK) {

                                        try {
                                                tagRepo.deleteTag(tag);

                                                expandedTags.remove(tag);
                                                populateTagList(tagList);

                                        } catch (SQLException e) {

                                                showDatabaseError(
                                                        tr("ui.unable_to_delete_tag"),
                                                        tr("ui.fileflow_could_not_delete_this_tag")
                                                );

                                                e.printStackTrace();
                                        }
                                        }
                                });
                        });

                        ContextMenu tagMenu = oceanMenu(deleteTagItem);

                        tagMenuButton.setOnAction(event ->
                                tagMenu.show(
                                        tagMenuButton,
                                        Side.BOTTOM,
                                        0,
                                        0
                                )
                        );

                        // Do not expand the group when opening its menu
                        tagMenuButton.addEventFilter(
                                MouseEvent.MOUSE_CLICKED,
                                event -> event.consume()
                        );
                                

                        Region spacer = new Region();
                        HBox.setHgrow(spacer,Priority.ALWAYS);

                        // FileFlow tag outline icon
                        SVGPath tagIcon = new SVGPath();

                        tagIcon.setContent(
                                "M2 2 H11 L21 12 L12 21 L2 11 Z " +
                                "M7 7 a1.5 1.5 0 1 0 3 0 " +
                                "a1.5 1.5 0 1 0 -3 0"
                        );

                        tagIcon.getStyleClass().add(
                                "tag-group-icon"
                        );

                        // Expansion indicator
                        Label expandArrow = new Label();

                        expandArrow.getStyleClass().add(
                                "tag-expand-arrow"
                        );

                        HBox tagHeader = new HBox(
                                10,
                                tagIcon,
                                tagLabel,
                                tagCountLabel,
                                spacer,
                                expandArrow,
                                tagMenuButton
                        );

                        tagHeader.setAlignment(Pos.CENTER_LEFT);

                        tagHeader.setAlignment(Pos.CENTER_LEFT);

                        VBox fileList = new VBox(6);

                        boolean expanded = expandedTags.contains(tag);

                        fileList.setVisible(expanded);
                        fileList.setManaged(expanded);

                        expandArrow.setGraphic(
                                expanded
                                        ? createChevronDownIcon()
                                        : createChevronRightIcon()
                        );

                        tagHeader.setOnMouseClicked(event -> {

                                if (event.getButton() != MouseButton.PRIMARY) {
                                        return;
                                }

                                boolean shouldExpand = !fileList.isVisible();

                                fileList.setVisible(shouldExpand);
                                fileList.setManaged(shouldExpand);

                                expandArrow.setGraphic(
                                        shouldExpand
                                                ? createChevronDownIcon()
                                                : createChevronRightIcon()
                                );

                                if (shouldExpand) {
                                        expandedTags.add(tag);
                                } else {
                                        expandedTags.remove(tag);
                                }
                        });

                        if (files.isEmpty()) {
                                Label emptyLabel = new Label(tr("ui.no_files_with_this_tag"));
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

                                        // Allow long file names and paths to shrink
                                        fileNameLabel.setMinWidth(0);
                                        fileNameLabel.setMaxWidth(Double.MAX_VALUE);

                                        fileNameLabel.setTextOverrun(
                                                javafx.scene.control.OverrunStyle.ELLIPSIS
                                        );

                                        filePathLabel.setMinWidth(0);
                                        filePathLabel.setMaxWidth(Double.MAX_VALUE);

                                        filePathLabel.setTextOverrun(
                                                javafx.scene.control.OverrunStyle.ELLIPSIS
                                        );

                                        // Show full text on hover
                                        fileNameLabel.setTooltip(
                                                new javafx.scene.control.Tooltip(
                                                        filePath.getFileName().toString()
                                                )
                                        );

                                        filePathLabel.setTooltip(
                                                new javafx.scene.control.Tooltip(
                                                        filePath.toString()
                                                )
                                        );

                                        fileInfo.setMinWidth(0);
                                        fileInfo.setMaxWidth(Double.MAX_VALUE);

                                        HBox.setHgrow(fileInfo, Priority.ALWAYS);

                                        Node filePreview = createStoredFilePreview(filePath);

                                        Button removeButton = new Button("×");
                                        removeButton.getStyleClass().add("tag-remove-button");

                                        removeButton.setOnAction(event -> {
                                                try {
                                                        tagRepo.removeTagFromFile(filePath, tag);
                                                        populateTagList(tagList);
                                                } catch (SQLException e) {


                                                        showDatabaseError(
                                                                tr("ui.unable_to_remove_tag"),
                                                                tr("ui.fileflow_could_not_remove_this_tag_from_the_file")
                                                        );
                                                        e.printStackTrace();
                                                }
                                        });

                                        Button moreButton = createFileActionsButton(filePath);

                                        removeButton.setTooltip(
                                                new javafx.scene.control.Tooltip(
                                                        tr("ui.remove_this_tag_from_file")
                                                )
                                        );

                                        moreButton.setTooltip(
                                                new javafx.scene.control.Tooltip(
                                                        tr("ui.more_file_actions")
                                                )
                                        );

                                        HBox actionsBox = new HBox(
                                                8,
                                                removeButton,
                                                moreButton
                                        );

                                        actionsBox.setAlignment(Pos.CENTER_RIGHT);
                                        actionsBox.setMinWidth(Region.USE_PREF_SIZE);

                                        actionsBox.getStyleClass().add(
                                                "tags-row-actions"
                                        );

                                        HBox fileRow = new HBox(
                                                10,
                                                filePreview,
                                                fileInfo,
                                                actionsBox
                                        );

                                        fileRow.getStyleClass().add(
                                                "tag-file-row"
                                        );

                                        fileRow.setMinWidth(0);
                                        fileRow.setMaxWidth(Double.MAX_VALUE);

                                        fileRow.setAlignment(Pos.CENTER_LEFT);
                                        fileRow.setMaxWidth(Double.MAX_VALUE);

                                        PauseTransition clickDelay =
                                                new PauseTransition(Duration.millis(160));

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
                                        tr("ui.tags_are_temporarily_unavailable"),
                                        tr("ui.fileflow_could_not_load_your_tags"),
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
                directoryChooser.setTitle(tr("ui.select_folder"));

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
                                tr("ui.folder_not_found"),
                                tr("ui.this_folder_no_longer_exists"),
                                selectedPath.toString()
                        );

                        return;
                }

                if (!Files.isDirectory(selectedPath)) {

                        showScanError(
                                tr("ui.invalid_folder"),
                                tr("ui.the_selected_path_is_not_a_folder"),
                                selectedPath.toString()
                        );

                        return;
                }

                if (!Files.isReadable(selectedPath)) {

                        showScanError(
                                tr("ui.folder_cannot_be_read"),
                                tr("ui.fileflow_does_not_have_permission_to_read_this_folder"),
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
                                                        tr("ui.scanning_d_items"),
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

                        // A successful standalone scan replaces workspace browsing
                        lastWorkspaceScanSnapshot = null;

                        rebuildScannedItemIndex();

                        latestDuplicateGroups  = List.of();
                        duplicateAnalysisCompleted = false;

                        latestSimilarPairs = List.of();
                        similarAnalysisCompleted = false;

                        scanRootPath = selectedPath
                                .toAbsolutePath()
                                .normalize();

                        currentFolder = scanRootPath;

                        startWatchingScanRoot();

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
                                        tr("ui.scan_completed_but_history_could_not_be_saved")
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

        private void startWatchingScanRoot() {

                if (scanRootPath == null) {
                        return;
                }

                Path watchedPath = scanRootPath;

                try {
                        folderWatcher.start(watchedPath, () -> {

                        Platform.runLater(() -> {

                                if (watchedPath.equals(scanRootPath)) {
                                autoRefreshDelay.playFromStart();
                                }
                        });
                        });

                } catch (IOException exception) {

                        showStatusWarning(
                                tr("ui.automatic_folder_monitoring_is_unavailable")
                        );

                        exception.printStackTrace();
                }
        }

        private void reconcileSimilarResultsAfterRefresh(
                ScanResult previousScan,
                ScanResult refreshedScan
        ) {
                if (!similarAnalysisCompleted || previousScan == null) {
                        return;
                }

                Map<Path, FileItem> previousImages = new HashMap<>();

                for (FileItem item : previousScan.getItems()) {
                        if (!item.isDirectory() && item.getType() == FileType.IMAGE) {
                        previousImages.put(
                                item.getPath().toAbsolutePath().normalize(),
                                item
                        );
                        }
                }

                Set<Path> currentImages = new HashSet<>();

                for (FileItem item : refreshedScan.getItems()) {
                        if (item.isDirectory() || item.getType() != FileType.IMAGE) {
                        continue;
                        }

                        Path path = item.getPath().toAbsolutePath().normalize();
                        currentImages.add(path);

                        FileItem previous = previousImages.get(path);

                        if (previous == null
                                || previous.getSizeBytes() != item.getSizeBytes()
                                || !java.util.Objects.equals(
                                        previous.getLastModified(),
                                        item.getLastModified()
                                )) {

                        // New or modified image: existing results are no longer complete.
                        latestSimilarPairs = List.of();
                        similarAnalysisCompleted = false;
                        return;
                        }
                }

                // Only removals (or unrelated file changes): preserve existing matches.
                latestSimilarPairs = latestSimilarPairs.stream()
                        .filter(pair ->
                                currentImages.contains(
                                        pair.first().getPath()
                                                .toAbsolutePath().normalize()
                                )
                                && currentImages.contains(
                                        pair.second().getPath()
                                                .toAbsolutePath().normalize()
                                )
                        )
                        .toList();
        }

        private void reconcileExactDuplicatesAfterRefresh(
                ScanResult previousScan,
                ScanResult refreshedScan
        ) {
                if (!duplicateAnalysisCompleted
                        || previousScan == null
                        || refreshedScan == null) {
                        return;
                }

                Map<Path, FileItem> previousFiles = new HashMap<>();

                for (FileItem item : previousScan.getItems()) {
                        if (!item.isDirectory()) {
                        previousFiles.put(
                                item.getPath().toAbsolutePath().normalize(),
                                item
                        );
                        }
                }

                Set<Path> currentFiles = new HashSet<>();

                for (FileItem item : refreshedScan.getItems()) {

                        if (item.isDirectory()) {
                        continue;
                        }

                        Path path = item.getPath()
                                .toAbsolutePath()
                                .normalize();

                        currentFiles.add(path);

                        FileItem previous = previousFiles.get(path);

                        if (previous == null
                                || previous.getSizeBytes() != item.getSizeBytes()
                                || !java.util.Objects.equals(
                                        previous.getLastModified(),
                                        item.getLastModified()
                                )) {

                        latestDuplicateGroups = List.of();
                        duplicateAnalysisCompleted = false;
                        return;
                        }
                }

                // Keep groups whose files still exist.
                latestDuplicateGroups = latestDuplicateGroups.stream()
                        .filter(group -> group.getFiles().stream()
                                .allMatch(file -> currentFiles.contains(
                                        file.getPath()
                                                .toAbsolutePath()
                                                .normalize()
                                )))
                        .toList();
        }

        private void refreshCurrentScan() {

                // Workspace mode refreshes all selected roots
                if (lastWorkspaceScanSnapshot != null) {
                        refreshWorkspaceScan();
                        return;
                }

                if (scanRootPath == null || scanning) {
                        return;
                }

                setScanningState(true);

                scanStatusLabel.setText(
                        tr("ui.refreshing")
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

                        ScanResult previousScan = latestScanResult;

                        latestScanResult = refreshTask.getValue();

                        reconcileSimilarResultsAfterRefresh(
                                previousScan,
                                latestScanResult
                        );

                        reconcileExactDuplicatesAfterRefresh(
                                previousScan,
                                latestScanResult
                        );

                        rebuildScannedItemIndex();

                        filePreviewFactory.clearCache();

                        if (currentFolder == null
                                || !Files.isDirectory(currentFolder)) {

                                currentFolder = scanRootPath;
                        }

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
                                tr("ui.try_again")
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

                showOceanDialog(alert);
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
                        tr("ui.your_files_have_not_been_changed")
                );

                showOceanDialog(alert);
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

                showOceanDialog(alert);
        }

        private void handleScanFailure(
                Throwable exception,
                Path selectedPath
        ) {

                if (exception instanceof AccessDeniedException) {

                        showScanError(
                                tr("ui.permission_denied"),
                                tr("ui.fileflow_could_not_access_part_of_this_folder"),
                                exception.getMessage()
                        );

                        return;
                }

                if (exception instanceof NoSuchFileException) {

                        showScanError(
                                tr("ui.folder_changed_during_scan"),
                                tr("ui.a_file_or_folder_disappeared_while_fileflow_was_scanning"),
                                exception.getMessage()
                        );

                        return;
                }

                if (exception instanceof IOException) {

                        showScanError(
                                tr("ui.scan_failed"),
                                tr("ui.fileflow_could_not_finish_scanning_this_folder"),
                                selectedPath.toString()
                        );

                        return;
                }

                showScanError(
                        tr("ui.unexpected_scan_error"),
                        tr("ui.something_unexpected_happened_while_scanning_this_folder"),
                        selectedPath.toString()
                );

                exception.printStackTrace();
        }

        private void setScanningState(boolean scanning) {
                this.scanning = scanning;
                updateOceanStatus();

                scanButton.setText(
                        scanning ? tr("ui.cancel_scan") : tr("ui.scan_folder")
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
                                scanning ? tr("ui.cancel_scan") : tr("ui.scan_folder")
                        );
                }

                if (!scanning && autoRefreshPending) {

                        autoRefreshPending = false;

                        autoRefreshDelay.playFromStart();
                }
        }

        
        private void showScanWorkspaceDialog() {

                // Work on a temporary copy so Cancel discards changes
                ScanWorkspace draft = new ScanWorkspace();

                for (Path path : scanWorkspace.getRoots()) {
                        draft.addRoot(path);
                }

                Dialog<ButtonType> dialog = new Dialog<>();

                dialog.setTitle(tr("ui.manage_scan_folders"));
                dialog.setHeaderText(null);

                dialog.initOwner(root.getScene().getWindow());

                ButtonType saveButtonType = new ButtonType(
                        tr("ui.save_folders"),
                        ButtonBar.ButtonData.OK_DONE
                );

                ButtonType scanWorkspaceButtonType = new ButtonType(
                        tr("ui.scan_workspace"),
                        ButtonBar.ButtonData.APPLY
                );

                dialog.getDialogPane().getButtonTypes().addAll(
                        saveButtonType,
                        scanWorkspaceButtonType,
                        ButtonType.CANCEL
                );

                Label description = new Label(
                        tr("ui.drag_folders_here_or_use_add_folder_to_select_them")
                );
                description.setWrapText(true);

                Label countLabel = new Label();

                VBox rootsBox = new VBox(8);

                refreshScanWorkspaceRows(
                        rootsBox,
                        countLabel,
                        draft
                );

                ScrollPane rootsScroll = new ScrollPane(rootsBox);

                rootsScroll.getStyleClass().add("workspace-roots-scroll");
                rootsBox.getStyleClass().add("workspace-roots-box");

                rootsScroll.setFitToWidth(true);
                rootsScroll.setPrefViewportHeight(150);
                rootsScroll.setHbarPolicy(
                        ScrollPane.ScrollBarPolicy.NEVER
                );

                Label feedbackLabel = new Label();
                feedbackLabel.setWrapText(true);

                
                SVGPath dropIcon = new SVGPath();
                dropIcon.setContent(
                        "M12 3 V15 "
                        + "M7 10 L12 15 L17 10 "
                        + "M4 17 V21 H20 V17"
                );
                dropIcon.getStyleClass().add("workspace-drop-icon");

                Label dropTitle = new Label(tr("ui.drop_folders_here"));
                dropTitle.getStyleClass().add("workspace-drop-title");

                Label dropHint = new Label(
                        tr("ui.drag_one_or_multiple_folders_from_file_explorer")
                );
                dropHint.getStyleClass().add("workspace-drop-hint");
                dropHint.setWrapText(true);

                VBox dropZone = new VBox(
                        8,
                        dropIcon,
                        dropTitle,
                        dropHint
                );

                dropZone.setAlignment(Pos.CENTER);
                dropZone.setMinHeight(100);
                dropZone.setMaxWidth(Double.MAX_VALUE);
                dropZone.getStyleClass().add("workspace-drop-zone");
                
                // Shared folder-drop logic for the list and drop zone
                Function<List<File>, Integer> addDroppedFolders = files -> {

                        int added = 0;
                        int skipped = 0;

                        for (File droppedFile : files) {

                                if (!droppedFile.isDirectory()) {
                                        skipped++;
                                        continue;
                                }

                                Path path = droppedFile.toPath();

                                if (draft.addRoot(path)) {
                                        added++;
                                } else {
                                        skipped++;
                                }
                        }

                        refreshScanWorkspaceRows(
                                rootsBox,
                                countLabel,
                                draft
                        );

                        feedbackLabel.setText(
                                added + tr("ui.folder_s_added")
                                        + skipped + tr("ui.skipped")
                        );

                        return added;
                };

                // Accept folders from Windows File Explorer
                EventHandler<DragEvent> dragOverHandler = event -> {

                        Dragboard dragboard = event.getDragboard();

                        boolean containsFolders =
                                dragboard.hasFiles()
                                && dragboard.getFiles().stream()
                                        .anyMatch(File::isDirectory);

                        if (containsFolders) {
                                event.acceptTransferModes(TransferMode.COPY);
                        }

                        event.consume();
                };

                // Handle the actual drop
                EventHandler<DragEvent> dropHandler = event -> {

                        Dragboard dragboard = event.getDragboard();

                        int added = 0;

                        if (dragboard.hasFiles()) {
                                added = addDroppedFolders.apply(
                                        dragboard.getFiles()
                                );
                        }

                        dropZone.getStyleClass().remove(
                                "workspace-drop-zone-active"
                        );

                        event.setDropCompleted(added > 0);
                        event.consume();
                };

                // Preserve existing drag-and-drop on the folder list
                rootsScroll.setOnDragOver(dragOverHandler);
                rootsScroll.setOnDragDropped(dropHandler);

                // Enable the dedicated drop zone
                dropZone.setOnDragOver(event -> {

                        dragOverHandler.handle(event);

                        Dragboard dragboard = event.getDragboard();

                        if (dragboard.hasFiles()
                                && dragboard.getFiles().stream()
                                        .anyMatch(File::isDirectory)) {

                                if (!dropZone.getStyleClass().contains(
                                        "workspace-drop-zone-active"
                                )) {
                                        dropZone.getStyleClass().add(
                                                "workspace-drop-zone-active"
                                        );
                                }
                        }
                });

                dropZone.setOnDragExited(event -> {

                        dropZone.getStyleClass().remove(
                                "workspace-drop-zone-active"
                        );

                        event.consume();
                });

                dropZone.setOnDragDropped(dropHandler);

                Button addButton = new Button(tr("ui.add_folder"));
                addButton.getStyleClass().add("secondary-button");

                addButton.setOnAction(event -> {

                        DirectoryChooser chooser = new DirectoryChooser();
                        chooser.setTitle(tr("ui.add_folder_to_scan_workspace"));

                        File selectedFolder = chooser.showDialog(
                                dialog.getDialogPane().getScene().getWindow()
                        );

                        if (selectedFolder == null) {
                                return;
                        }

                        Path selectedPath = selectedFolder.toPath();

                        if (!Files.isDirectory(selectedPath)
                                || !Files.isReadable(selectedPath)) {

                        feedbackLabel.setText(
                                tr("ui.this_folder_cannot_be_accessed")
                        );
                                return;
                        }

                        int previousCount = draft.size();

                        boolean changed = draft.addRoot(selectedPath);

                        if (!changed) {

                                feedbackLabel.setText(
                                        tr("ui.this_folder_is_already_included_in_another_selected_folder")
                                );
                                return;
                        }

                        refreshScanWorkspaceRows(
                                rootsBox,
                                countLabel,
                                draft
                        );

                        if (draft.size() <= previousCount) {
                                feedbackLabel.setText(
                                        tr("ui.parent_folder_added_covered_child_folders_removed")
                                );
                        } else {
                                feedbackLabel.setText(tr("ui.folder_added"));
                        }
                });

                HBox workspaceToolbar = new HBox();
                workspaceToolbar.setAlignment(Pos.CENTER_LEFT);
                workspaceToolbar.setSpacing(12);

                Region toolbarSpacer = new Region();
                HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);

                workspaceToolbar.getChildren().addAll(
                        countLabel,
                        toolbarSpacer,
                        addButton
                );

                workspaceToolbar.getStyleClass().add("workspace-toolbar");
                countLabel.getStyleClass().add("workspace-count-label");
                addButton.getStyleClass().add("secondary-button");

                VBox dialogContent = new VBox(
                        12,
                        description,
                        dropZone,
                        workspaceToolbar,
                        rootsScroll,
                        feedbackLabel
                );

                dialogContent.getStyleClass().add("workspace-dialog-content");

                dialogContent.setPrefWidth(560);

                dialog.getDialogPane().setContent(dialogContent);

                dialog.setResultConverter(buttonType -> buttonType);

                showOceanDialog(dialog).ifPresent(result -> {

                        if (result != saveButtonType
                                && result != scanWorkspaceButtonType) {
                                return;
                        }

                        // Save the selected roots
                        List<Path> selectedRoots = draft.getRoots();

                        scanWorkspace.clear();

                        for (Path path : selectedRoots) {
                                scanWorkspace.addRoot(path);
                        }

                        updateOceanStatus();

                        // Scan only when explicitly requested
                        if (result == scanWorkspaceButtonType) {
                                startWorkspaceScan();
                        }
                });
        }

        
        private void refreshScanWorkspaceRows(
                VBox rootsBox,
                Label countLabel,
                ScanWorkspace workspace
        ) {

                rootsBox.getChildren().clear();

                List<Path> roots = workspace.getRoots();

                countLabel.setText(
                        roots.size() == 1
                                ? tr("ui.1_folder_selected")
                                : roots.size() + tr("ui.folders_selected")
                );

                if (roots.isEmpty()) {

                        Label emptyLabel = new Label(
                                tr("ui.no_folders_selected_click_add_folder_to_begin")
                        );

                        rootsBox.getChildren().add(emptyLabel);
                        return;
                }

                for (Path path : roots) {

                        Label pathLabel = new Label(path.toString());

                        pathLabel.setStyle(
                                "-fx-font-size: 13px;"
                                + "-fx-text-fill: #324866;"
                        );

                        pathLabel.setWrapText(true);
                        pathLabel.setMinWidth(0);
                        pathLabel.setMaxWidth(Double.MAX_VALUE);

                        HBox.setHgrow(
                                pathLabel,
                                Priority.ALWAYS
                        );

                        Button removeButton = new Button(tr("ui.remove"));

                        removeButton.getStyleClass().add(
                                "secondary-button"
                        );

                        removeButton.setOnAction(event -> {

                                workspace.removeRoot(path);

                                refreshScanWorkspaceRows(
                                        rootsBox,
                                        countLabel,
                                        workspace
                                );
                        });

                        HBox folderRow = new HBox(
                                12,
                                pathLabel,
                                removeButton
                        );

                        folderRow.setAlignment(Pos.CENTER_LEFT);

                        folderRow.setStyle(
                                "-fx-padding: 12 16;"
                                + "-fx-background-color: #FFFFFF;"
                                + "-fx-background-radius: 10;"
                                + "-fx-border-color: #DFE8F5;"
                                + "-fx-border-radius: 10;"
                                + "-fx-border-width: 1;"
                        );

                        rootsBox.getChildren().add(folderRow);
                }
        }
        
        private void activateWorkspaceRoot(Path rootPath) {

                if (lastWorkspaceScanSnapshot == null || scanning) {
                        return;
                }

                        Path normalizedRoot = rootPath
                                .toAbsolutePath()
                                .normalize();

                        ScanResult selectedResult =
                                lastWorkspaceScanSnapshot
                                        .resultsByRoot()
                                        .get(normalizedRoot);

                        if (selectedResult == null) {
                                return;
                }

                // Stop any pending refresh for the previous root
                autoRefreshDelay.stop();
                autoRefreshPending = false;

                // Switch the active result and browsing location
                latestScanResult = selectedResult;
                scanRootPath = normalizedRoot;
                currentFolder = normalizedRoot;

                // Results from another root must not be reused
                latestDuplicateGroups = List.of();
                duplicateAnalysisCompleted = false;

                latestSimilarPairs = List.of();
                similarAnalysisCompleted = false;

                // Rebuild the lookup for the active root
                rebuildScannedItemIndex();

                // Rebuild the file preview cache
                filePreviewFactory.clearCache();

                updateStorageCard(normalizedRoot);

                // Re-create the Files page using the selected root
                selectNavItem(filesNavItem, "Files");
        }
        
        private void startWorkspaceScan() {
                startWorkspaceScan(false);
        }

        private void refreshWorkspaceScan() {

                if (scanning || lastWorkspaceScanSnapshot == null) {
                        return;
                }

                startWorkspaceScan(true);
        }

        private void startWorkspaceScan(boolean refreshExisting) {

                if (scanning) {
                        return;
                }

                // Freeze the selected roots for this scan
                ScanWorkspace scanPlan = new ScanWorkspace();

                for (Path path : scanWorkspace.getRoots()) {
                        scanPlan.addRoot(path);
                }

                if (scanPlan.isEmpty()) {

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);

                        alert.setTitle(tr("ui.scan_workspace"));
                        alert.setHeaderText(tr("ui.no_folders_selected"));
                        alert.setContentText(
                                tr("ui.add_at_least_one_folder_before_scanning")
                        );

                        showOceanDialog(alert);
                        return;
                }
                
                // Used to preserve previous results during refresh
                WorkspaceScanSnapshot previousSnapshot =
                        lastWorkspaceScanSnapshot;

                AtomicBoolean cancelRequested = new AtomicBoolean(false);

                workspaceCancelRequested = cancelRequested;

                Task<WorkspaceScanSnapshot> workspaceTask =
                        new Task<>() {

                                @Override
                                protected WorkspaceScanSnapshot call() {

                                updateMessage(tr("ui.preparing_workspace_scan"));

                                WorkspaceScanService service =
                                        new WorkspaceScanService(fileScanService);

                                return service.scan(
                                        scanPlan,
                                        cancelRequested::get,

                                        progress -> {

                                                String folderName =
                                                        progress.root().getFileName() == null
                                                                ? progress.root().toString()
                                                                : progress.root()
                                                                        .getFileName()
                                                                        .toString();

                                                updateMessage(
                                                        String.format(
                                                                tr("ui.folder_d_d_s_d_items"),
                                                                progress.folderIndex(),
                                                                progress.totalFolders(),
                                                                folderName,
                                                                progress.processedItems()
                                                        )
                                                );
                                        }
                                );
                                }
                        };

                scanStatusLabel.textProperty().bind(
                        workspaceTask.messageProperty()
                );

                workspaceTask.setOnSucceeded(event -> {

                        scanStatusLabel.textProperty().unbind();

                        workspaceCancelRequested = null;

                        setScanningState(false);

                        WorkspaceScanSnapshot refreshed =
                                workspaceTask.getValue();

                        // Do not replace the current view if a new workspace
                        // scan produced no successfully completed folders.
                        if (!refreshExisting
                                && refreshed.resultsByRoot().isEmpty()) {

                                showWorkspaceScanSummary(refreshed);
                                return;
                        }

                        // Record only roots actually scanned successfully.
                        // A cancelled refresh does not create a new history entry.
                        if (!(refreshExisting && refreshed.cancelled())) {
                        saveWorkspaceScanHistory(refreshed);
                        }

                        // Cancelled refresh: keep the old workspace unchanged
                        if (refreshExisting && refreshed.cancelled()) {

                                Alert alert = new Alert(
                                        Alert.AlertType.INFORMATION
                                );

                                alert.setTitle(tr("ui.workspace_refresh"));
                                alert.setHeaderText(tr("ui.refresh_cancelled"));
                                alert.setContentText(
                                        tr("ui.your_previous_workspace_results_were_preserved")
                                );

                                showOceanDialog(alert);
                                return;
                        }

                        // Merge refreshed results with previous results
                        if (refreshExisting && previousSnapshot != null) {

                                lastWorkspaceScanSnapshot =
                                        WorkspaceScanSnapshot.mergeRefresh(
                                                previousSnapshot,
                                                refreshed,
                                                scanPlan.getRoots()
                                        );

                        } else {

                                lastWorkspaceScanSnapshot = refreshed;
                        }

                        WorkspaceScanSnapshot snapshot =
                                lastWorkspaceScanSnapshot;

                        if (!snapshot.resultsByRoot().isEmpty()) {

                                // Try to preserve the currently selected root
                                if (refreshExisting
                                        && scanRootPath != null
                                        && snapshot.resultsByRoot()
                                                .containsKey(scanRootPath)) {

                                latestScanResult =
                                        snapshot.resultsByRoot().get(scanRootPath);

                                // Clear analysis results based on old scan data
                                latestDuplicateGroups = List.of();
                                duplicateAnalysisCompleted = false;

                                latestSimilarPairs = List.of();
                                similarAnalysisCompleted = false;

                                filePreviewFactory.clearCache();

                                rebuildScannedItemIndex();

                                // Keep the user's current folder if it still exists
                                if (currentFolder == null
                                        || !currentFolder.startsWith(scanRootPath)
                                        || !Files.isDirectory(currentFolder)) {

                                        currentFolder = scanRootPath;
                                }

                                refreshCurrentPage();

                                } else {

                                // Initial scan, or previously selected root removed
                                Path firstRoot = snapshot.resultsByRoot()
                                        .keySet()
                                        .iterator()
                                        .next();

                                activateWorkspaceRoot(firstRoot);
                                }
                        }

                        if (refreshExisting && !refreshed.failures().isEmpty()) {

                                showStatusWarning(
                                        tr("ui.some_folders_could_not_be_refreshed_previous_results_were_preserved_where_available")
                                );
                        }

                        // Report this scan attempt, not the preserved old data
                        showWorkspaceScanSummary(refreshed);
                });

                workspaceTask.setOnFailed(event -> {

                        scanStatusLabel.textProperty().unbind();

                        workspaceCancelRequested = null;

                        setScanningState(false);

                        Throwable error = workspaceTask.getException();

                        Alert alert = new Alert(Alert.AlertType.ERROR);

                        alert.setTitle(tr("ui.workspace_scan_failed"));
                        alert.setHeaderText(
                                tr("ui.an_unexpected_error_interrupted_the_scan")
                        );

                        alert.setContentText(
                                error == null
                                        ? tr("ui.unknown_error")
                                        : String.valueOf(error.getMessage())
                        );

                        showOceanDialog(alert);
                });

                // Reuse the existing top-bar scan indicator
                setScanningState(true);

                Thread thread = new Thread(
                        workspaceTask,
                        "fileflow-workspace-scan"
                );

                thread.setDaemon(true);
                thread.start();
        }

        private void saveWorkspaceScanHistory(
                WorkspaceScanSnapshot snapshot
        ) {

                for (var entry : snapshot.resultsByRoot().entrySet()) {

                        Path rootPath = entry.getKey();
                        ScanResult result = entry.getValue();

                        long fileCount = result.getItems().stream()
                                .filter(item -> !item.isDirectory())
                                .count();

                        long totalBytes = result.getItems().stream()
                                .filter(item -> !item.isDirectory())
                                .mapToLong(FileItem::getSizeBytes)
                                .sum();

                        try {

                                scanHistoryRepo.add(
                                        rootPath,
                                        LocalDateTime.now(),
                                        fileCount,
                                        totalBytes
                                );

                        } catch (SQLException exception) {

                                showStatusWarning(
                                        tr("ui.some_workspace_scan_history_could_not_be_saved")
                                );

                                exception.printStackTrace();
                        }
                }
        }
        
        private void showWorkspaceScanSummary(
                WorkspaceScanSnapshot snapshot
        ) {

                int completed = snapshot.completedFolderCount();
                int failed = snapshot.failures().size();

                Alert.AlertType alertType =
                        failed > 0
                                ? Alert.AlertType.WARNING
                                : Alert.AlertType.INFORMATION;

                Alert alert = new Alert(alertType);

                alert.setTitle(tr("ui.workspace_scan_results"));

                if (snapshot.cancelled()) {

                        alert.setHeaderText(
                                tr("ui.workspace_scan_cancelled")
                        );

                } else if (failed > 0) {

                        alert.setHeaderText(
                                tr("ui.workspace_scan_completed_with_issues")
                        );

                } else {

                        alert.setHeaderText(
                                tr("ui.workspace_scan_completed")
                        );
                }

                String message = String.format(
                        tr("ui.folders_completed_d_nfolders_failed_d_nfiles_found_d_ntotal_file_size_d_bytes_n_nresu"),
                        completed,
                        failed,
                        snapshot.fileCount(),
                        snapshot.totalFileBytes()
                );

                alert.setContentText(message);

                if (failed > 0) {

                        StringBuilder details = new StringBuilder();

                        for (var failure : snapshot.failures()) {

                                details.append(failure.root());
                                details.append(System.lineSeparator());

                                details.append(
                                        failure.error().getMessage()
                                );

                                details.append(System.lineSeparator());
                                details.append(System.lineSeparator());
                        }

                        TextArea failureDetails = new TextArea(
                                details.toString()
                        );

                        failureDetails.setEditable(false);
                        failureDetails.setWrapText(true);
                        failureDetails.setPrefRowCount(6);

                        alert.getDialogPane().setExpandableContent(
                                failureDetails
                        );

                        alert.getDialogPane().setExpanded(false);
                }

                showOceanDialog(alert);
        }

        private void handleScanAction() {

                if (scanning) {

                        // Multi-folder scan uses cooperative cancellation.
                        if (workspaceCancelRequested != null) {

                                workspaceCancelRequested.set(true);
                                return;
                        }

                        // Preserve the existing single-folder cancellation.
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
                moreButton.setOnAction(event -> createFileContextMenu(filePath).show(moreButton, Side.BOTTOM, 0, 0));
                return moreButton;
        }

        private SVGPath createChevronRightIcon() {
                SVGPath icon = new SVGPath();

                icon.setContent(
                        "M8.22 5.22"
                        + "a.75.75 0 0 1 1.06 0"
                        + "l5.25 5.25"
                        + "a.75.75 0 0 1 0 1.06"
                        + "l-5.25 5.25"
                        + "a.75.75 0 1 1-1.06-1.06"
                        + "L12.94 11"
                        + "L8.22 6.28"
                        + "a.75.75 0 0 1 0-1.06z"
                );

                icon.getStyleClass().add("ff-chevron-icon");
                icon.setStyle("-fx-fill: #5D89F6;");

                return icon;
        }

        private SVGPath createChevronDownIcon() {
                SVGPath icon = new SVGPath();

                icon.setContent(
                        "M5.22 8.22"
                        + "a.75.75 0 0 1 1.06 0"
                        + "L11 12.94"
                        + "l4.72-4.72"
                        + "a.75.75 0 1 1 1.06 1.06"
                        + "l-5.25 5.25"
                        + "a.75.75 0 0 1-1.06 0"
                        + "L5.22 9.28"
                        + "a.75.75 0 0 1 0-1.06z"
                );

                icon.getStyleClass().add("ff-chevron-icon");
                icon.setStyle("-fx-fill: #5D89F6;");

                return icon;
        }

        private void openFile(
                Path filePath
        ) {

                if (!Files.exists(filePath)) {

                        showFileActionError(
                                tr("ui.file_no_longer_exists"),
                                tr("ui.this_file_has_been_moved_or_deleted"),
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
                                tr("ui.unable_to_open_file"),
                                tr("ui.fileflow_could_not_open_this_file"),
                                filePath.toString()
                        );

                        exception.printStackTrace();
                }
        }

        private ContextMenu oceanMenu(MenuItem... items) {
                ContextMenu menu = new ContextMenu(items);
                menu.getStyleClass().add("ocean-context-menu");
                if (I18n.instance().language() == AppLanguage.SIMPLIFIED_CHINESE) menu.getStyleClass().add("language-zh");
                menu.getScene().getStylesheets().setAll(getClass().getResource("/css/ocean-popup.css").toExternalForm());
                menu.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);
                menu.getScene().getRoot().setStyle("-fx-background-color: transparent;");
                return menu;
        }

        private ContextMenu createFileContextMenu(Path filePath) {
                if (sharedFileMenu == null) {
                        MenuItem open = new MenuItem(tr("ui.open"));
                        MenuItem show = new MenuItem(tr("ui.show_in_folder"));
                        MenuItem copy = new MenuItem(tr("ui.copy_path"));
                        sharedDeleteItem = new MenuItem(tr("files.recycle"));
                        sharedDeleteItem.getStyleClass().add("danger-menu-item");
                        open.setOnAction(event -> openFile(menuPath));
                        show.setOnAction(event -> {
                                try { filePlatformService.showInFolder(menuPath); }
                                catch (IOException error) { showFileActionError(tr("ui.unable_to_show_file"), tr("ui.could_not_open_the_containing_folder"), menuPath.toString()); }
                        });
                        copy.setOnAction(event -> {
                                try { filePlatformService.copyPath(menuPath); }
                                catch (IOException error) { showFileActionError(tr("ui.unable_to_copy_path"), error.getMessage(), menuPath.toString()); }
                        });
                        sharedDeleteItem.setOnAction(event -> handleDeleteFile(menuPath));
                        sharedFileMenu = oceanMenu(open, show, copy, new SeparatorMenuItem(), sharedDeleteItem);
                }
                sharedFileMenu.hide();
                menuPath = filePath;
                FileItem item = findScannedFileItem(filePath);
                sharedDeleteItem.setVisible(item != null && !item.isDirectory());
                if (item == null) {
                        try {
                                metadataExecutor.execute(() -> {
                                        boolean regular = Files.isRegularFile(filePath);
                                        Platform.runLater(() -> {
                                                if (filePath.equals(menuPath)) sharedDeleteItem.setVisible(regular);
                                        });
                                });
                        } catch (java.util.concurrent.RejectedExecutionException ignored) { }
                }
                return sharedFileMenu;
        }

        private <R> java.util.Optional<R> showOceanDialog(Dialog<R> dialog) {
                OceanDialogChrome.install(dialog);
                applyDarkDialogStyle(dialog);
                I18n.localizeDialog(dialog);
                if (dialog.getOwner() == null && root.getScene() != null) {
                        dialog.initOwner(root.getScene().getWindow());
                }
                dialog.getDialogPane().getScene().setFill(javafx.scene.paint.Color.web("#102038"));
                return dialog.showAndWait();
        }

        private void applyDarkDialogStyle(Dialog<?> dialog) {
                var pane = dialog.getDialogPane();

                if (!pane.getStyleClass().contains("ff-dark-dialog")) pane.getStyleClass().add("ff-dark-dialog");
                if (!pane.getStyleClass().contains("ocean-dialog")) pane.getStyleClass().add("ocean-dialog");
                if (I18n.instance().language() == AppLanguage.SIMPLIFIED_CHINESE) pane.getStyleClass().add("language-zh");

                for (String css : root.getStylesheets()) {
                        if (!pane.getStylesheets().contains(css)) {
                                pane.getStylesheets().add(css);
                        }
                }

                if (root.getScene() != null) {
                        for (String css : root.getScene().getStylesheets()) {
                                if (!pane.getStylesheets().contains(css)) {
                                        pane.getStylesheets().add(css);
                                }
                        }
                }
        }
        
        private boolean confirmMoveToRecycleBin(Path filePath) {

                boolean confirmBeforeDelete = preferences.getBoolean(
                        CONFIRM_BEFORE_DELETE_KEY,
                        true
                );

                if (!confirmBeforeDelete) {
                        return true;
                }

                Alert confirmation = new Alert(
                        Alert.AlertType.CONFIRMATION
                );

                confirmation.setTitle(tr("files.recycle"));
                applyDarkDialogStyle(confirmation);
                confirmation.getDialogPane().setGraphic(null);

                applyDarkDialogStyle(confirmation);

                confirmation.setHeaderText(
                        tr("ui.move_this_file_to_the_recycle_bin")
                );

                confirmation.setContentText(
                        filePath.getFileName()
                        + tr("ui.you_can_restore_this_file_from_the_windows_recycle_bin_while_it_remains_there")
                );

                ButtonType moveButtonType = new ButtonType(
                        tr("files.recycle"),
                        ButtonBar.ButtonData.OK_DONE
                );

                ButtonType cancelButtonType = new ButtonType(
                        tr("common.cancel"),
                        ButtonBar.ButtonData.CANCEL_CLOSE
                );

                confirmation.getButtonTypes().setAll(
                        cancelButtonType,
                        moveButtonType
                );

                CheckBox dontAskAgain =
                        new CheckBox(tr("ui.don_t_ask_me_again"));

                confirmation.getDialogPane().setExpandableContent(
                        dontAskAgain
                );

                confirmation.getDialogPane().setExpanded(true);

                ButtonType result = showOceanDialog(confirmation)
                        .orElse(cancelButtonType);

                if (result != moveButtonType) {
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
                                tr("ui.file_no_longer_exists"),
                                tr("ui.this_file_has_already_been_moved_or_deleted"),
                                filePath.toString()
                        );

                        filePreviewFactory.invalidate(filePath);

                        cleanupDeletedFileReferences(
                                filePath
                        );

                        refreshAfterDelete();

                        return;
                }

                if (!confirmMoveToRecycleBin(filePath)) {
                        return;
                }

                try {
                        recycleBinService.moveToRecycleBin(filePath);

                        filePreviewFactory.invalidate(filePath);

                        cleanupDeletedFileReferences(
                                filePath
                        );

                        refreshAfterDelete();
                        
                } catch (IOException exception) {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);

                        errorAlert.setTitle(tr("ui.delete_failed"));

                        errorAlert.setHeaderText(tr("ui.unable_to_delete_the_file"));

                        errorAlert.setContentText(exception.getMessage());

                        showOceanDialog(errorAlert);
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

                if (lastWorkspaceScanSnapshot != null) {

                if (!scanning) {
                        refreshWorkspaceScan();
                } else {
                        showStatusWarning(
                                tr("ui.workspace_scan_is_still_running_refresh_after_it_completes")
                        );
                }

                return;
                }

                if (scanRootPath == null) {

                        refreshCurrentPage();
                        return;
                }

                if (scanning) {

                        autoRefreshPending = true;
                        return;
                }

                setScanningState(true);

                boolean refreshDuplicates =
                        duplicateAnalysisCompleted && reanalyseDuplicates;


                scanStatusLabel.setText(
                        tr("ui.refreshing")
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

                        ScanResult previousScan = latestScanResult;

                        latestScanResult = refreshTask.getValue();

                        reconcileSimilarResultsAfterRefresh(
                                previousScan,
                                latestScanResult
                        );

                        reconcileExactDuplicatesAfterRefresh(
                                previousScan,
                                latestScanResult
                        );

                        rebuildScannedItemIndex();
                        
                        currentScanTask = null;

                        if (refreshDuplicates) {

                                refreshDuplicatesAfterDelete();

                        } else {

                                hideRefreshStatus();

                                refreshCurrentPage();

                                setScanningState(false);
                        }
                });


                refreshTask.setOnFailed(event -> {

                        currentScanTask = null;

                        hideRefreshStatus();

                        refreshTask
                                .getException()
                                .printStackTrace();

                        refreshCurrentPage();

                        setScanningState(false);
                });

                refreshTask.setOnCancelled(event -> {

                        currentScanTask = null;

                        hideRefreshStatus();

                        setScanningState(false);
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

                                setScanningState(false);
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

                                setScanningState(false);
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
                String currentTitle = pageTitle.getText();
                navItems.stream().filter(item -> currentTitle.equals(item.getUserData())).findFirst()
                        .ifPresent(item -> selectNavItem(item, currentTitle));
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
                TableView<FileItem> fileList,
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
                        SVGPath separator = createChevronRightIcon();

                        separator.setStyle("-fx-fill: #A3B1C5;");
                        separator.setScaleX(0.72);
                        separator.setScaleY(0.72);
                        separator.setMouseTransparent(true);

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

        private String getWorkspaceFileLocation(Path filePath) {

                Path parent = filePath
                        .toAbsolutePath()
                        .normalize()
                        .getParent();

                if (parent == null) {
                        return "";
                }

                // Workspace: locate the correct root for this file
                if (lastWorkspaceScanSnapshot != null
                        && !lastWorkspaceScanSnapshot.resultsByRoot().isEmpty()) {

                        for (Path root :
                                lastWorkspaceScanSnapshot.resultsByRoot().keySet()) {

                                Path normalizedRoot = root
                                        .toAbsolutePath()
                                        .normalize();

                                if (parent.startsWith(normalizedRoot)) {

                                        Path relativeParent =
                                                normalizedRoot.relativize(parent);

                                        String rootName =
                                                getPathDisplayName(normalizedRoot);

                                        if (relativeParent.toString().isEmpty()) {
                                        return rootName;
                                        }

                                        return rootName + " / " + relativeParent;
                                }
                        }

                        // Fallback for paths outside all scanned roots
                        return parent.toString();
                }

                // Original single-folder scan
                if (scanRootPath != null) {

                        Path normalizedRoot = scanRootPath
                                .toAbsolutePath()
                                .normalize();

                        if (parent.startsWith(normalizedRoot)) {

                        Path relativeParent =
                                normalizedRoot.relativize(parent);

                        return relativeParent.toString().isEmpty()
                                ? tr("ui.scan_root")
                                : relativeParent.toString();
                        }
                }

                return parent.toString();
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

                Label titleLabel = new Label(tr("ui.file_categories"));
                titleLabel.getStyleClass().add("categories-title");

                Label subtitleLabel = new Label(
                        tr("ui.storage_breakdown_for_this_scan")
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
                                I18n.choice(getFileTypeDisplayName(type))
                        );
                        nameLabel.getStyleClass().add("category-name");

                        Label countLabel = new Label(
                                fileCount + tr("ui.files")
                        );
                        countLabel.getStyleClass().add("category-count");

                        Label sizeLabel = new Label(
                                FileSizeFormatter.format(totalBytes)
                        );
                        sizeLabel.getStyleClass().add("category-size");

                        Label arrowLabel = new Label();
                        arrowLabel.setGraphic(createChevronRightIcon());
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
                I18n.dateTimeFormatter();

        return dateTime.format(formatter);
        }

        private VBox createCollapsibleSection(
                String title,
                int itemCount,
                VBox content
        ) {
        Label arrowLabel = new Label();
        arrowLabel.setGraphic(createChevronRightIcon());

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

                arrowLabel.setGraphic(
                        expanded
                                ? createChevronRightIcon()
                                : createChevronDownIcon()
                );
        });

        VBox section = new VBox(
                6,
                header,
                content
        );

        return section;
        }

        private String readImageResolution(Path path) {

                try (ImageInputStream input =
                        ImageIO.createImageInputStream(path.toFile())) {

                        if (input == null) {
                        return tr("ui.unknown_resolution");
                        }

                        Iterator<ImageReader> readers =
                                ImageIO.getImageReaders(input);

                        if (!readers.hasNext()) {
                        return tr("ui.unknown_resolution");
                        }

                        ImageReader reader = readers.next();

                        try {
                        reader.setInput(input, true, true);

                        int width = reader.getWidth(0);
                        int height = reader.getHeight(0);

                        return width + " × " + height;

                        } finally {
                        reader.dispose();
                        }

                } catch (IOException | RuntimeException exception) {
                        return tr("ui.unknown_resolution");
                }
        }

        private void deleteSelectedSimilarImages(
                SimilarImageGroup group,
                List<Path> selectedPaths
        ) {

                if (scanning || currentSimilarTask != null) {
                        showStatusWarning(
                                tr("ui.please_finish_the_current_analysis_or_scan_first")
                        );
                        return;
                }

                Set<Path> selected = new HashSet<>(selectedPaths);

                List<FileItem> toDelete = group.files()
                        .stream()
                        .filter(item -> selected.contains(item.getPath()))
                        .toList();

                List<FileItem> toKeep = group.files()
                        .stream()
                        .filter(item -> !selected.contains(item.getPath()))
                        .toList();

                if (toDelete.isEmpty()
                        || toKeep.isEmpty()
                        || toDelete.size() != selected.size()) {
                        return;
                }

                long reclaimableBytes = toDelete.stream()
                        .mapToLong(FileItem::getSizeBytes)
                        .sum();

                String keepPaths = String.join(
                        "\n",
                        toKeep.stream()
                                .map(item -> item.getPath().toString())
                                .toList()
                );

                String deletePaths = String.join(
                        "\n",
                        toDelete.stream()
                                .map(item -> item.getPath().toString())
                                .toList()
                );

                Alert confirmation = new Alert(Alert.AlertType.WARNING);

                confirmation.setTitle(tr("ui.resolve_similar_images"));

                confirmation.setHeaderText(
                        tr("ui.move") + toDelete.size() + tr("ui.image_s_to_recycle_bin")
                );

                confirmation.setContentText(
                        tr("ui.images_to_keep") + toKeep.size()
                                + tr("ui.images_to_delete") + toDelete.size()
                                + tr("ui.space_to_reclaim")
                                + FileSizeFormatter.format(reclaimableBytes)
                                + tr("ui.images_will_be_moved_to_the_recycle_bin_you_can_restore_them_while_they_remain_there")
                );

                TextArea details = new TextArea(
                        "KEEP:\n" + keepPaths
                                + "\n\nDELETE:\n" + deletePaths
                );

                details.setEditable(false);
                details.setWrapText(true);
                details.setPrefRowCount(10);
                details.setPrefColumnCount(55);

                confirmation.getDialogPane().setExpandableContent(details);
                confirmation.getDialogPane().setExpanded(true);

                ButtonType deleteButton = new ButtonType(
                        tr("files.recycle"),
                        ButtonBar.ButtonData.OK_DONE
                );

                confirmation.getButtonTypes().setAll(
                        ButtonType.CANCEL,
                        deleteButton
                );

                if (showOceanDialog(confirmation)
                        .orElse(ButtonType.CANCEL) != deleteButton) {
                        return;
                }

                // Do not delete anything if a kept image is already missing.
                for (FileItem item : toKeep) {

                        if (!Files.isRegularFile(item.getPath())) {
                        showFileActionError(
                                tr("ui.resolve_cancelled"),
                                tr("ui.an_image_you_wanted_to_keep_is_missing"),
                                item.getPath().toString()
                        );
                        return;
                        }
                }

                Set<Path> deletedPaths = new HashSet<>();

                for (FileItem item : toDelete) {

                        Path path = item.getPath();

                        try {

                        if (!Files.isRegularFile(path)
                                || Files.size(path) != item.getSizeBytes()) {
                                throw new IOException(
                                        tr("ui.file_is_missing_or_has_changed_since_the_scan")
                                );
                        }

                        recycleBinService.moveToRecycleBin(path);

                        deletedPaths.add(path);

                        filePreviewFactory.invalidate(path);

                        cleanupDeletedFileReferences(path);

                        } catch (IOException exception) {

                        showFileActionError(
                                tr("ui.unable_to_delete_image"),
                                tr("ui.deletion_stopped_some_files_may_already_have_been_deleted"),
                                path.toString()
                        );

                        exception.printStackTrace();
                        break;
                        }
                }

                if (!deletedPaths.isEmpty()) {

                        // Remove results involving successfully deleted files.
                        latestSimilarPairs = latestSimilarPairs.stream()
                                .filter(pair ->
                                        !deletedPaths.contains(pair.first().getPath())
                                        && !deletedPaths.contains(pair.second().getPath())
                                )
                                .toList();

                        similarAnalysisCompleted = true;

                        refreshCurrentPage();

                        // Refresh file metadata without rehashing exact duplicates.
                        refreshAfterDelete(false);
                }
        }

        private String getSimilarImageSummary() {

                if (latestSimilarPairs.isEmpty()) {
                        return tr("ui.no_similar_images_found");
                }

                List<SimilarImageGroup> groups =
                        similarImageService.groupSimilarPairs(
                                latestSimilarPairs
                        );

                long imageCount = groups.stream()
                        .flatMap(group -> group.files().stream())
                        .map(item -> item.getPath()
                                .toAbsolutePath()
                                .normalize())
                        .distinct()
                        .count();

                return String.format(
                        tr("ui.d_similar_groups_d_images"),
                        groups.size(),
                        imageCount
                );
        }
        
        private void populateSimilarImageResults(
                VBox container
        ) {
                container.getChildren().clear();

                PauseTransition similarClickDelay =
                        new PauseTransition(Duration.millis(160));

                List<SimilarImageGroup> groups =
                        similarImageService.groupSimilarPairs(
                                latestSimilarPairs
                        );

                int groupNumber = 1;

                for (SimilarImageGroup group : groups) {

                        // Group title
                        Label groupTitle = new Label(
                                String.format(tr("ui.similar_group_02d"), groupNumber)
                        );

                        groupTitle.getStyleClass().add(
                                "similar-group-title"
                        );

                        // Image count badge
                        Label groupCount = new Label(
                                group.files().size() + tr("ui.images_f514e")
                        );

                        groupCount.getStyleClass().add(
                                "similar-group-count"
                        );

                        // Responsive image grid
                        TilePane imageGrid = new TilePane();

                        imageGrid.setHgap(12);
                        imageGrid.setVgap(14);

                        imageGrid.setPrefColumns(3);
                        imageGrid.setPrefTileWidth(245);

                        imageGrid.setMinWidth(0);
                        imageGrid.setMaxWidth(Double.MAX_VALUE);

                        imageGrid.setAlignment(Pos.TOP_LEFT);
                        imageGrid.setTileAlignment(Pos.TOP_CENTER);

                        imageGrid.getStyleClass().add(
                                "similar-image-grid"
                        );

                        for (FileItem item : group.files()) {

                                Node preview = createQuickInfoPreview(
                                        item.getPath(),
                                        "Image",
                                        230,
                                        160
                                );

                                Label nameLabel = new Label(
                                        item.getName()
                                );

                                nameLabel.setWrapText(false);

                                nameLabel.setMinWidth(0);
                                nameLabel.setMaxWidth(230);

                                nameLabel.setTextOverrun(
                                        javafx.scene.control.OverrunStyle.ELLIPSIS
                                );

                                nameLabel.setTooltip(
                                        new javafx.scene.control.Tooltip(item.getName())
                                );

                                nameLabel.getStyleClass().add(
                                        "similar-image-name"
                                );

                                String fileSize = FileSizeFormatter.format(
                                        item.getSizeBytes()
                                );

                                Label resolutionLabel = new Label(tr("ui.reading_resolution"));
                                Label sizeLabel = new Label(fileSize);

                                resolutionLabel.setStyle(
                                        "-fx-font-size: 11px;" +
                                        "-fx-text-fill: #9fbfe8;"
                                );

                                sizeLabel.setStyle(
                                        "-fx-font-size: 11px;" +
                                        "-fx-text-fill: #9fbfe8;"
                                );

                                Region spacer = new Region();

                                HBox.setHgrow(spacer, Priority.ALWAYS);

                                HBox metadataRow = new HBox(
                                        resolutionLabel,
                                        spacer,
                                        sizeLabel
                                );

                                metadataRow.setAlignment(Pos.CENTER_LEFT);

                                // Match the metadata width to the image preview
                                metadataRow.prefWidthProperty().bind(
                                        ((Region) preview).prefWidthProperty()
                                );

                                // Read image dimensions in the background
                                CompletableFuture
                                        .supplyAsync(() ->
                                                readImageResolution(item.getPath())
                                        )
                                        .thenAccept(resolution ->
                                                Platform.runLater(() -> {

                                                resolutionLabel.setText(resolution);

                                                })
                                        );

                                VBox imageCard = new VBox(
                                        6,
                                        preview,
                                        nameLabel,
                                        metadataRow
                                );
                                imageCard.getStyleClass().add(
                                        "similar-image-card"
                                );

                                imageCard.setPrefWidth(245);
                                imageCard.setMinWidth(245);
                                imageCard.setMaxWidth(245);

                                imageCard.setOnMouseClicked(event -> {

                                        if (event.getButton() != MouseButton.PRIMARY) {
                                                return;
                                        }

                                        if (event.getClickCount() == 2) {

                                                similarClickDelay.stop();

                                                openFile(item.getPath());

                                                event.consume();

                                        } else if (event.getClickCount() == 1) {

                                                similarClickDelay.stop();

                                                similarClickDelay.setOnFinished(done -> {

                                                if (imageCard.getScene() == null
                                                        || !"Duplicates".equals(pageTitle.getText())) {
                                                        return;
                                                }

                                                // Do not open Quick Info after switching modes
                                                Node current = imageCard;

                                                while (current != null) {

                                                        if (!current.isVisible()) {
                                                        return;
                                                        }

                                                        current = current.getParent();
                                                }

                                                showQuickInfo(item.getPath());
                                                });

                                                similarClickDelay.playFromStart();
                                        }
                                });

                                imageCard.setOnContextMenuRequested(event -> {

                                        ContextMenu fileMenu =
                                                createFileContextMenu(item.getPath());

                                        fileMenu.show(
                                                imageCard,
                                                event.getScreenX(),
                                                event.getScreenY()
                                        );

                                        event.consume();
                                });

                                imageGrid.getChildren().add(imageCard);
                        }

                        Button resolveButton = new Button(tr("ui.resolve"));

                        resolveButton.getStyleClass().addAll(
                                "secondary-button",
                                "similar-group-resolve-button"
                        );

                        resolveButton.setMinWidth(
                                Region.USE_PREF_SIZE
                        );

                        resolveButton.setOnAction(event -> {

                        Dialog<List<Path>> dialog = new Dialog<>();

                        dialog.setTitle(tr("ui.resolve_similar_images"));
                        dialog.setHeaderText(
                                tr("ui.select_the_images_you_want_to_delete")
                        );

                        ButtonType reviewType = new ButtonType(
                                tr("ui.review_selection"),
                                ButtonBar.ButtonData.OK_DONE
                        );

                        dialog.getDialogPane().getButtonTypes().addAll(
                                reviewType,
                                ButtonType.CANCEL
                        );

                        VBox choicesBox = new VBox(12);
                        List<CheckBox> checkBoxes = new ArrayList<>();

                        for (FileItem file : group.files()) {

                                CheckBox checkBox = new CheckBox(
                                        file.getName() + " · " +
                                        FileSizeFormatter.format(file.getSizeBytes())
                                );

                                checkBox.setUserData(file.getPath());

                                Label pathLabel = new Label(
                                        file.getPath().toString()
                                );

                                pathLabel.setWrapText(true);
                                pathLabel.getStyleClass().add(
                                        "duplicate-file-path"
                                );

                                choicesBox.getChildren().add(
                                        new VBox(4, checkBox, pathLabel)
                                );

                                checkBoxes.add(checkBox);
                        }

                        ScrollPane choicesScroll = new ScrollPane(choicesBox);
                        choicesScroll.setFitToWidth(true);
                        choicesScroll.setPrefViewportHeight(320);

                        dialog.getDialogPane().setContent(choicesScroll);

                        dialog.setResultConverter(buttonType -> {

                                if (buttonType != reviewType) {
                                        return null;
                                }

                                return checkBoxes.stream()
                                        .filter(CheckBox::isSelected)
                                        .map(box -> (Path) box.getUserData())
                                        .toList();
                        });

                        showOceanDialog(dialog).ifPresent(selectedPaths -> {

                                if (selectedPaths.isEmpty()) {
                                        return;
                                }

                                if (selectedPaths.size() == group.files().size()) {

                                        showOceanDialog(new Alert(
                                                Alert.AlertType.WARNING,
                                                tr("ui.you_must_keep_at_least_one_image"),
                                                ButtonType.OK
                                        ));

                                        return;
                                }

                                deleteSelectedSimilarImages(group, selectedPaths);
                        });
                });

                        Region headerSpacer = new Region();

                        HBox.setHgrow(
                                headerSpacer,
                                Priority.ALWAYS
                        );

                        HBox groupHeader = new HBox(
                                12,
                                groupTitle,
                                groupCount,
                                headerSpacer,
                                resolveButton
                        );

                        groupHeader.getStyleClass().add(
                                "similar-image-group-header"
                        );

                        groupHeader.setAlignment(Pos.CENTER_LEFT);

                        VBox groupCard = new VBox(
                                12,
                                groupHeader,
                                imageGrid
                        );

                        groupCard.getStyleClass().addAll(
                                "duplicate-group-card",
                                "similar-image-group-card"
                        );

                        container.getChildren().add(groupCard);

                        groupNumber++;
                }
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

                int groupNumber = 1;

                for (DuplicateGroup group : sortedGroups) {

                        // Group title
                        Label groupTitle = new Label(
                                String.format(tr("ui.duplicate_group_02d"), groupNumber)
                        );

                        groupTitle.getStyleClass().add(
                                "exact-duplicate-group-title"
                        );

                        // File count and individual file size
                        Label summaryLabel = new Label(
                                String.format(
                                        tr("ui.d_copies_s_each"),
                                        group.getFiles().size(),
                                        FileSizeFormatter.format(
                                                group.getFileSizeBytes()
                                        )
                                )
                        );

                        summaryLabel.getStyleClass().add(
                                "duplicate-group-summary"
                        );

                        summaryLabel.setWrapText(true);

                        // Title + description
                        VBox groupInfo = new VBox(
                                4,
                                groupTitle,
                                summaryLabel
                        );

                        groupInfo.setMinWidth(0);
                        HBox.setHgrow(groupInfo, Priority.ALWAYS);

                        // Highlight reclaimable storage
                        Label reclaimableBadge = new Label(
                                FileSizeFormatter.format(
                                        group.getReclaimableBytes()
                                ) + tr("ui.reclaimable")
                        );

                        reclaimableBadge.getStyleClass().add(
                                "exact-duplicate-reclaimable-badge"
                        );

                        VBox fileList = new VBox(6);
                        fileList.getStyleClass().add(
                                "exact-duplicate-file-list"
                        );

                        fileList.setVisible(false);
                        fileList.setManaged(false);

                        Label arrowLabel = new Label();
                        arrowLabel.setGraphic(createChevronRightIcon());

                        arrowLabel.getStyleClass().add("duplicate-group-arrow");

                        Button resolveButton =
                                new Button(tr("ui.resolve"));

                        resolveButton.getStyleClass().addAll(
                                "secondary-button",
                                "exact-duplicate-resolve-button"
                        );

                        resolveButton.setMinWidth(
                                Region.USE_PREF_SIZE
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
                                tr("ui.resolve_duplicates")
                        );

                        dialog.setHeaderText(
                                tr("ui.choose_the_copy_you_want_to_keep")
                        );

                        dialog.setContentText(
                                tr("duplicates.keep_label")
                        );

                        showOceanDialog(dialog)
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
                                        tr("ui.resolve_duplicates")
                                );

                                confirmation.setHeaderText(
                                        tr("ui.keep_this_copy_and_delete_the_others")
                                );

                                confirmation.setContentText(
                                        String.format(
                                                tr("ui.fileflow_will_keep_n_n_s_n_nand_move_d_duplicate_copy_copies_to_the_recycle_bin_nabou"),
                                                pathToKeep,
                                                copiesToDelete,
                                                FileSizeFormatter.format(
                                                        reclaimableBytes
                                                )
                                        )
                                );

                                ButtonType resolveButtonType =
                                        new ButtonType(
                                                tr("files.recycle"),
                                                ButtonBar.ButtonData.OK_DONE
                                        );

                                ButtonType cancelButtonType =
                                        new ButtonType(
                                                tr("common.cancel"),
                                                ButtonBar.ButtonData.CANCEL_CLOSE
                                        );

                                confirmation
                                        .getButtonTypes()
                                        .setAll(
                                                cancelButtonType,
                                                resolveButtonType
                                        );

                                ButtonType result =
                                        showOceanDialog(confirmation)
                                                .orElse(
                                                        cancelButtonType
                                                );

                                if (result != resolveButtonType) {
                                        return;
                                }

                                if (!Files.isRegularFile(pathToKeep)) {

                                        showFileActionError(
                                                tr("ui.resolve_cancelled"),
                                                tr("ui.the_copy_you_chose_to_keep_is_no_longer_available"),
                                                pathToKeep.toString()
                                        );

                                        return;
                                }

                                for (Path duplicatePath : duplicatePaths) {

                                        if (duplicatePath.equals(pathToKeep)) {
                                                continue;
                                        }

                                        try {

                                                recycleBinService.moveToRecycleBin(duplicatePath);

                                                filePreviewFactory.invalidate(
                                                        duplicatePath
                                                );

                                                cleanupDeletedFileReferences(
                                                        duplicatePath
                                                );

                                        } catch (IOException exception) {

                                                showFileActionError(
                                                        tr("ui.unable_to_resolve_duplicates"),
                                                        tr("ui.fileflow_could_not_delete_one_of_the_duplicate_files"),
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
                                14,
                                arrowLabel,
                                groupInfo,
                                reclaimableBadge,
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

                                arrowLabel.setGraphic(
                                        expanded
                                                ? createChevronRightIcon()
                                                : createChevronDownIcon()
                                );
                        });

                        VBox groupCard = new VBox(
                                0,
                                groupHeader,
                                fileList
                        );

                        groupCard.getStyleClass().addAll(
                                "duplicate-group-card",
                                "exact-duplicate-group-card"
                        );

                        resultsContainer.getChildren().add(groupCard);

                        groupNumber++;

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
                        tr("ui.groups"),
                        String.format(
                                "%,d",
                                latestDuplicateGroups.size()
                        )
                );

                VBox copiesCard = createSummaryCard(
                        tr("ui.extra_copies"),
                        String.format(
                                "%,d",
                                extraCopies
                        )
                );

                VBox reclaimableCard = createSummaryCard(
                        tr("ui.reclaimable_6c5fb"),
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

                        // Prevent long names and paths from expanding the row
                        nameLabel.setMinWidth(0);
                        nameLabel.setMaxWidth(Double.MAX_VALUE);
                        nameLabel.setTextOverrun(
                                javafx.scene.control.OverrunStyle.ELLIPSIS
                        );

                        pathLabel.setMinWidth(0);
                        pathLabel.setMaxWidth(Double.MAX_VALUE);
                        pathLabel.setTextOverrun(
                                javafx.scene.control.OverrunStyle.ELLIPSIS
                        );

                        // Show complete information on hover
                        nameLabel.setTooltip(
                                new javafx.scene.control.Tooltip(item.getName())
                        );

                        pathLabel.setTooltip(
                                new javafx.scene.control.Tooltip(item.getPath().toString())
                        );

                        // Let the information area use remaining horizontal space
                        fileInfo.setMinWidth(0);
                        fileInfo.setMaxWidth(Double.MAX_VALUE);

                        HBox.setHgrow(fileInfo, Priority.ALWAYS);

                        Node filePreview =
                                createUnifiedFilePreview(item);

                        Region spacer = new Region();

                        Button moreButton = createFileActionsButton(
                                item.getPath()
                        );

                        // Keep the action button at a fixed size
                        moreButton.setMinWidth(Region.USE_PREF_SIZE);

                        // The file information now handles remaining space
                        HBox fileRow = new HBox(
                                12,
                                filePreview,
                                fileInfo,
                                moreButton
                        );

                        fileRow.setMinWidth(0);
                        fileRow.setMaxWidth(Double.MAX_VALUE);

                        fileRow.setAlignment(
                                Pos.CENTER_LEFT
                        );

                        fileRow.getStyleClass().addAll(
                                "duplicate-file-row",
                                "exact-duplicate-file-row"
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

        private void cancelQuickInfoMetadata() {
                if (quickInfoMetadata != null) {
                        quickInfoMetadata.cancel(false);
                        if (quickInfoMetadata instanceof Runnable queued) metadataExecutor.remove(queued);
                        quickInfoMetadata = null;
                }
        }

        private void hideQuickInfo() {
                quickInfoGeneration++;
                cancelQuickInfoMetadata();
                if (quickInfoPanel != null) {
                        quickInfoPanel.setVisible(false);
                        quickInfoImage.getChildren().clear();
                }
        }

        private String quickInfoType(FileItem item) {
                if (item.isDirectory()) return "Folder";
                return switch (item.getType()) {
                        case IMAGE -> "Image"; case VIDEO -> "Video"; case AUDIO -> "Audio";
                        case DOCUMENT -> "Document"; case ARCHIVE -> "Archive"; case CODE -> "Code";
                        default -> "File";
                };
        }

        private void ensureQuickInfoPanel() {
                if (quickInfoPanel != null) return;
                Label title = new Label(tr("files.quick_info"));
                title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
                Button close = new Button("×");
                close.setAccessibleText(tr("ui.close_quick_info"));
                close.getStyleClass().add("quick-info-close");
                close.setOnAction(event -> hideQuickInfo());
                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox header = new HBox(10, title, spacer, close); header.setAlignment(Pos.CENTER_LEFT);
                quickInfoName = new Label(); quickInfoName.setWrapText(true);
                quickInfoName.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
                quickInfoType = new Label(); quickInfoSize = new Label(); quickInfoModified = new Label();
                quickInfoLocation = new Label(); quickInfoLocation.setWrapText(true);
                quickInfoImage = new StackPane();
                quickInfoImage.setMinSize(220, 160); quickInfoImage.setPrefSize(220, 160); quickInfoImage.setMaxSize(220, 160);
                quickInfoPanel = new VBox(14, header, quickInfoImage, quickInfoName, new javafx.scene.control.Separator(),
                        quickInfoType, quickInfoSize, quickInfoModified, quickInfoLocation);
                quickInfoPanel.getStyleClass().add("quick-info-panel");
                quickInfoPanel.setManaged(false);
                var layers = (StackPane) root.getCenter();
                var overlay = (javafx.scene.layout.Pane) layers.getChildren().get(1);
                overlay.getChildren().add(quickInfoPanel);
                overlay.requestLayout();
        }

        private void applyQuickInfo(Path path, String type, String size, String modified) {
                quickInfoType.setText(tr("files.info.type", I18n.choice(type)));
                quickInfoSize.setText(tr("files.info.size", size));
                quickInfoModified.setText(tr("files.info.modified", modified));
                quickInfoImage.getChildren().setAll(createQuickInfoPreview(path, type));
        }

        private void showQuickInfo(Path filePath) {
                Path path = filePath.toAbsolutePath().normalize();
                cancelQuickInfoMetadata();
                long generation = ++quickInfoGeneration;
                ensureQuickInfoPanel();
                quickInfoName.setText(path.getFileName() == null ? path.toString() : path.getFileName().toString());
                quickInfoLocation.setText(tr("files.info.location", path));
                quickInfoPanel.setVisible(true);
                FileItem item = findScannedFileItem(path);
                if (item != null) {
                        String modified = item.getLastModified() == null ? tr("ui.unavailable")
                                : item.getLastModified().format(I18n.dateTimeFormatter());
                        applyQuickInfo(path, quickInfoType(item), item.isDirectory() ? "—" : FileSizeFormatter.format(item.getSizeBytes()), modified);
                        return;
                }
                applyQuickInfo(path, "File", tr("common.loading"), tr("common.loading"));
                try {
                        quickInfoMetadata = metadataExecutor.submit(() -> {
                                try {
                                        var attributes = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class);
                                        String mime = attributes.isDirectory() ? null : Files.probeContentType(path);
                                        String type = attributes.isDirectory() ? "Folder" : mime != null && mime.startsWith("image/") ? "Image"
                                                : mime != null && mime.startsWith("video/") ? "Video" : mime != null && mime.startsWith("audio/") ? "Audio" : "File";
                                        String size = attributes.isDirectory() ? "—" : FileSizeFormatter.format(attributes.size());
                                        String modified = attributes.lastModifiedTime().toInstant().atZone(java.time.ZoneId.systemDefault())
                                                .format(I18n.dateTimeFormatter());
                                        Platform.runLater(() -> {
                                                if (quickInfoGeneration == generation && quickInfoPanel.isVisible()) applyQuickInfo(path, type, size, modified);
                                        });
                                } catch (IOException | SecurityException failure) {
                                        Platform.runLater(() -> {
                                                if (quickInfoGeneration == generation && quickInfoPanel.isVisible()) applyQuickInfo(path, "File", tr("ui.unavailable"), tr("ui.unavailable"));
                                        });
                                }
                        });
                } catch (java.util.concurrent.RejectedExecutionException ignored) { applyQuickInfo(path, "File", tr("ui.unavailable"), tr("ui.unavailable")); }
        }

        private Node createQuickInfoPreview(Path path, String type) {
                return createQuickInfoPreview(path, type, 220, 160);
        }

        private Node createQuickInfoPreview(
                Path path,
                String type,
                int width,
                int height
        ) {

                StackPane previewBox = new StackPane();

                previewBox.setPrefSize(width, height);
                previewBox.setMinSize(width, height);
                previewBox.setMaxSize(width, height);

                previewBox.getStyleClass().add("quick-info-preview");

                boolean isImage =
                        type.equalsIgnoreCase("Image")
                        || type.endsWith(" Image");

                if ("Folder".equals(type)) {
                        Node folder = createFolderListPreview();
                        folder.setScaleX(2.2);
                        folder.setScaleY(2.2);
                        previewBox.getChildren().add(folder);
                } else if (isImage) {
                        return filePreviewFactory.createImagePreview(path, width, height, tr("ui.loading_preview"));
                } else {

                        String placeholder = switch (type) {
                        case "Folder" -> tr("ui.folder");
                        case "Document" -> tr("ui.document");
                        case "Video" -> tr("ui.video");
                        case "Audio" -> tr("ui.audio_1ab8e");
                        case "Archive" -> tr("ui.archive");
                        case "Code" -> tr("ui.code_16de2");
                        default -> tr("ui.file");
                        };

                        Label placeholderLabel = new Label(placeholder);

                        placeholderLabel.setStyle(
                                "-fx-font-size: 20px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: #9fbfe8;"
                        );

                        previewBox.getChildren().add(placeholderLabel);
                }

                return previewBox;
        }

        private Node createStoredFilePreview(Path filePath) {

    FileItem scannedItem = findScannedFileItem(filePath);

        if (scannedItem != null) {
                return createUnifiedFilePreview(scannedItem);
        }

        // Use FileFlow Signature Tile for stored files
        Node storedBadge = filePreviewFactory.createStoredBadge(filePath);

        if (storedBadge != null) {
                return storedBadge;
        }

        String fileName = filePath.getFileName().toString()
                .toLowerCase(Locale.ROOT);

                boolean isImage = fileName.matches(
                        ".*\\.(jpg|jpeg|png|gif|bmp)"
                );

                if (!isImage) return filePreviewFactory.createFallback();
                return filePreviewFactory.createImagePreview(filePath, 56, 42, tr("ui.image"));
        }
}
