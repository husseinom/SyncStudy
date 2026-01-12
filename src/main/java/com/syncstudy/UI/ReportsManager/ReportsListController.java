package com.syncstudy.UI.ReportsManager;

import com.syncstudy.BL.ReportsManager.Report;
import com.syncstudy.BL.ReportsManager.ReportsFacade;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Reports List panel
 * Handles report listing, filtering and navigation to details
 */
public class ReportsListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> problemTypeFilter;
    @FXML private TableView<Report> reportsTable;
    @FXML private TableColumn<Report, String> idColumn;
    @FXML private TableColumn<Report, String> dateColumn;
    @FXML private TableColumn<Report, String> reporterColumn;
    @FXML private TableColumn<Report, String> reportedUserColumn;
    @FXML private TableColumn<Report, String> problemTypeColumn;
    @FXML private TableColumn<Report, String> descriptionColumn;
    @FXML private TableColumn<Report, String> statusColumn;
    @FXML private TableColumn<Report, Void> actionsColumn;
    @FXML private Pagination pagination;
    @FXML private Label totalReportsLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label validatedCountLabel;
    @FXML private Label rejectedCountLabel;

    private ReportsFacade reportsFacade;
    private ObservableList<Report> reportsList;
    private List<Report> allReports;
    private static final int PAGE_SIZE = 15;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Current admin ID (should be set from session)
    private Long currentAdminId = 1L;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        reportsFacade = ReportsFacade.getInstance();
        reportsList = FXCollections.observableArrayList();

        setupFilters();
        setupTable();
        setupPagination();
        loadReports();
    }

    /**
     * Set current admin ID
     */
    public void setCurrentAdminId(Long adminId) {
        this.currentAdminId = adminId;
    }

    /**
     * Setup filter controls
     */
    private void setupFilters() {
        // Status filter
        statusFilter.setItems(FXCollections.observableArrayList(
            "All", "Pending", "Validated", "Rejected"));
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> applyFilters());

        // Problem type filter
        problemTypeFilter.setItems(FXCollections.observableArrayList(
            "All", "Spam", "Harassment", "Inappropriate Content", "Other"));
        problemTypeFilter.setValue("All");
        problemTypeFilter.setOnAction(e -> applyFilters());

        // Search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        // ID column
        idColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(String.valueOf(data.getValue().getReportId())));

        // Date column
        dateColumn.setCellValueFactory(data -> {
            if (data.getValue().getReportDate() != null) {
                return new SimpleStringProperty(data.getValue().getReportDate().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("-");
        });

        // Reporter column
        reporterColumn.setCellValueFactory(data -> {
            String username = data.getValue().getReporterUsername();
            return new SimpleStringProperty(username != null ? username : "User #" + data.getValue().getReporterUserId());
        });

        // Reported user column
        reportedUserColumn.setCellValueFactory(data -> {
            String username = data.getValue().getReportedUsername();
            return new SimpleStringProperty(username != null ? username : "User #" + data.getValue().getReportedUserId());
        });

        // Problem type column
        problemTypeColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getProblemType()));
        problemTypeColumn.setCellFactory(col -> new TableCell<Report, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(type);
                    switch (type.toLowerCase()) {
                        case "spam" -> setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                        case "harassment" -> setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                        case "inappropriate content" -> setStyle("-fx-text-fill: #e83e8c; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: #6c757d; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Description column (truncated)
        descriptionColumn.setCellValueFactory(data -> {
            String desc = data.getValue().getDescription();
            if (desc != null && desc.length() > 50) {
                desc = desc.substring(0, 47) + "...";
            }
            return new SimpleStringProperty(desc != null ? desc : "-");
        });

        // Status column with colored badge
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusColumn.setCellFactory(col -> new TableCell<Report, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.setPadding(new Insets(2, 8, 2, 8));
                    switch (status) {
                        case "Pending" -> badge.setStyle("-fx-background-color: #ffc107; -fx-text-fill: #212529; -fx-background-radius: 4;");
                        case "Validated" -> badge.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 4;");
                        case "Rejected" -> badge.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 4;");
                        default -> badge.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 4;");
                    }
                    setGraphic(badge);
                }
            }
        });

        // Actions column
        actionsColumn.setCellFactory(col -> new TableCell<Report, Void>() {
            private final Button viewBtn = new Button("View");
            private final Button validateBtn = new Button("✓");
            private final Button rejectBtn = new Button("✗");
            private final HBox buttons = new HBox(8, viewBtn, validateBtn, rejectBtn);

            {
                buttons.setAlignment(javafx.geometry.Pos.CENTER);

                // Style avec taille minimum pour éviter la troncature
                viewBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
                viewBtn.setMinWidth(60);

                validateBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
                validateBtn.setMinWidth(40);

                rejectBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
                rejectBtn.setMinWidth(40);

                viewBtn.setOnAction(e -> {
                    Report report = getTableView().getItems().get(getIndex());
                    openReportDetails(report);
                });

                validateBtn.setOnAction(e -> {
                    Report report = getTableView().getItems().get(getIndex());
                    handleQuickValidate(report);
                });

                rejectBtn.setOnAction(e -> {
                    Report report = getTableView().getItems().get(getIndex());
                    handleQuickReject(report);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Report report = getTableView().getItems().get(getIndex());
                    // Hide validate/reject buttons if already processed
                    boolean isPending = "Pending".equals(report.getStatus());
                    validateBtn.setVisible(isPending);
                    rejectBtn.setVisible(isPending);
                    validateBtn.setManaged(isPending);
                    rejectBtn.setManaged(isPending);
                    setGraphic(buttons);
                }
            }
        });
    }

    /**
     * Setup pagination
     */
    private void setupPagination() {
        pagination.setPageFactory(pageIndex -> {
            updateTableForPage(pageIndex);
            return new Label(); // Dummy node
        });
    }

    /**
     * Load all reports
     */
    private void loadReports() {
        try {
            allReports = reportsFacade.getAllReports();
            updateStats();
            applyFilters();
        } catch (Exception e) {
            showError("Error loading reports: " + e.getMessage());
        }
    }

    /**
     * Update statistics labels
     */
    private void updateStats() {
        if (allReports == null) return;
        
        long pending = allReports.stream().filter(r -> "Pending".equals(r.getStatus())).count();
        long validated = allReports.stream().filter(r -> "Validated".equals(r.getStatus())).count();
        long rejected = allReports.stream().filter(r -> "Rejected".equals(r.getStatus())).count();

        pendingCountLabel.setText(String.valueOf(pending));
        validatedCountLabel.setText(String.valueOf(validated));
        rejectedCountLabel.setText(String.valueOf(rejected));
    }

    /**
     * Apply filters to reports
     */
    private void applyFilters() {
        if (allReports == null) return;

        String searchText = searchField.getText().toLowerCase();
        String statusValue = statusFilter.getValue();
        String problemType = problemTypeFilter.getValue();

        List<Report> filtered = allReports.stream()
            .filter(r -> {
                // Status filter
                if (!"All".equals(statusValue) && !statusValue.equals(r.getStatus())) {
                    return false;
                }
                // Problem type filter
                if (!"All".equals(problemType) && !problemType.equals(r.getProblemType())) {
                    return false;
                }
                // Search filter
                if (!searchText.isEmpty()) {
                    String reporter = r.getReporterUsername() != null ? r.getReporterUsername().toLowerCase() : "";
                    String reported = r.getReportedUsername() != null ? r.getReportedUsername().toLowerCase() : "";
                    String desc = r.getDescription() != null ? r.getDescription().toLowerCase() : "";
                    return reporter.contains(searchText) || reported.contains(searchText) || desc.contains(searchText);
                }
                return true;
            })
            .collect(Collectors.toList());

        reportsList.setAll(filtered);
        
        int pageCount = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        
        updateTableForPage(0);
        totalReportsLabel.setText("Showing " + filtered.size() + " reports");
    }

    /**
     * Update table for current page
     */
    private void updateTableForPage(int pageIndex) {
        int fromIndex = pageIndex * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, reportsList.size());
        
        if (fromIndex <= toIndex && fromIndex < reportsList.size()) {
            reportsTable.setItems(FXCollections.observableArrayList(
                reportsList.subList(fromIndex, toIndex)));
        } else {
            reportsTable.setItems(FXCollections.observableArrayList());
        }
    }

    /**
     * Open report details dialog
     */
    private void openReportDetails(Report report) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ReportDetails.fxml"));
            Parent root = loader.load();

            ReportDetailsController controller = loader.getController();
            controller.setReport(report);
            controller.setCurrentAdminId(currentAdminId);
            controller.setOnReportUpdated(this::loadReports);

            Stage stage = new Stage();
            stage.setTitle("Report Details - #" + report.getReportId());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (IOException e) {
            showError("Error opening report details: " + e.getMessage());
        }
    }

    /**
     * Quick validate from table
     */
    private void handleQuickValidate(Report report) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Validate Report");
        confirm.setHeaderText("Validate report #" + report.getReportId() + "?");
        confirm.setContentText("This will mark the report as validated.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reportsFacade.validateReport(report.getReportId(), currentAdminId);
                loadReports();
                showInfo("Report validated successfully.");
            } catch (Exception e) {
                showError("Error validating report: " + e.getMessage());
            }
        }
    }

    /**
     * Quick reject from table
     */
    private void handleQuickReject(Report report) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reject Report");
        confirm.setHeaderText("Reject report #" + report.getReportId() + "?");
        confirm.setContentText("This will mark the report as rejected.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reportsFacade.rejectReport(report.getReportId(), currentAdminId);
                loadReports();
                showInfo("Report rejected successfully.");
            } catch (Exception e) {
                showError("Error rejecting report: " + e.getMessage());
            }
        }
    }

    /**
     * Handle refresh button
     */
    @FXML
    private void handleRefresh() {
        loadReports();
    }

    /**
     * Handle clear filters
     */
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        statusFilter.setValue("All");
        problemTypeFilter.setValue("All");
        applyFilters();
    }

    /**
     * Show error dialog
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info dialog
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
