package com.syncstudy.UI.ReportsManager;

import com.syncstudy.BL.ReportsManager.Report;
import com.syncstudy.BL.ReportsManager.ReportsFacade;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

/**
 * Controller for Report Details dialog
 * Handles viewing and taking actions on a single report
 */
public class ReportDetailsController {

    @FXML private Label titleLabel;
    @FXML private Label statusBadge;
    @FXML private Label reportIdLabel;
    @FXML private Label dateLabel;
    @FXML private Label reporterLabel;
    @FXML private Label reportedUserLabel;
    @FXML private Label problemTypeLabel;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea reportedContentArea;
    @FXML private VBox adminActionBox;
    @FXML private Label adminLabel;
    @FXML private Label actionDateLabel;
    @FXML private HBox actionButtonsBox;
    @FXML private Button validateBtn;
    @FXML private Button rejectBtn;

    private ReportsFacade reportsFacade;
    private Report currentReport;
    private Long currentAdminId = 1L;
    private Runnable onReportUpdated;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        reportsFacade = ReportsFacade.getInstance();
    }

    /**
     * Set the report to display
     */
    public void setReport(Report report) {
        this.currentReport = report;
        displayReport();
    }

    /**
     * Set current admin ID
     */
    public void setCurrentAdminId(Long adminId) {
        this.currentAdminId = adminId;
    }

    /**
     * Set callback for when report is updated
     */
    public void setOnReportUpdated(Runnable callback) {
        this.onReportUpdated = callback;
    }

    /**
     * Display report details
     */
    private void displayReport() {
        if (currentReport == null) return;

        titleLabel.setText("Report #" + currentReport.getReportId());
        reportIdLabel.setText(String.valueOf(currentReport.getReportId()));
        
        if (currentReport.getReportDate() != null) {
            dateLabel.setText(currentReport.getReportDate().format(DATE_FORMATTER));
        }
        
        // Reporter info
        String reporter = currentReport.getReporterUsername();
        reporterLabel.setText(reporter != null ? reporter : "User #" + currentReport.getReporterUserId());
        
        // Reported user info
        String reported = currentReport.getReportedUsername();
        reportedUserLabel.setText(reported != null ? reported : "User #" + currentReport.getReportedUserId());
        
        // Problem type
        problemTypeLabel.setText(currentReport.getProblemType());
        
        // Description
        descriptionArea.setText(currentReport.getDescription() != null ? 
            currentReport.getDescription() : "No description provided");
        
        // Reported content
        reportedContentArea.setText(currentReport.getReportedContent() != null ? 
            currentReport.getReportedContent() : "No content specified");
        
        // Status badge
        updateStatusBadge();
        
        // Show/hide action buttons and admin info based on status
        boolean isPending = currentReport.isPending();
        validateBtn.setVisible(isPending);
        validateBtn.setManaged(isPending);
        rejectBtn.setVisible(isPending);
        rejectBtn.setManaged(isPending);
        
        // Show admin action info if processed
        if (!isPending && currentReport.getAdminId() != null) {
            adminActionBox.setVisible(true);
            adminActionBox.setManaged(true);
            adminLabel.setText("Admin #" + currentReport.getAdminId());
            if (currentReport.getActionDate() != null) {
                actionDateLabel.setText(currentReport.getActionDate().format(DATE_FORMATTER));
            }
        } else {
            adminActionBox.setVisible(false);
            adminActionBox.setManaged(false);
        }
    }

    /**
     * Update status badge styling
     */
    private void updateStatusBadge() {
        String status = currentReport.getStatus();
        statusBadge.setText(status);
        
        switch (status) {
            case "Pending" -> statusBadge.setStyle(
                "-fx-background-color: #ffc107; -fx-text-fill: #212529; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold;");
            case "Validated" -> statusBadge.setStyle(
                "-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold;");
            case "Rejected" -> statusBadge.setStyle(
                "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold;");
            default -> statusBadge.setStyle(
                "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-weight: bold;");
        }
    }

    /**
     * Handle validate report
     */
    @FXML
    private void handleValidate() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Validate Report");
        confirm.setHeaderText("Validate this report?");
        confirm.setContentText("This will mark the report as validated, confirming that the reported behavior is problematic.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reportsFacade.validateReport(currentReport.getReportId(), currentAdminId);
                currentReport.setStatus("Validated");
                displayReport();
                notifyUpdate();
                showInfo("Report validated successfully.");
            } catch (Exception e) {
                showError("Error validating report: " + e.getMessage());
            }
        }
    }

    /**
     * Handle reject report
     */
    @FXML
    private void handleReject() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reject Report");
        confirm.setHeaderText("Reject this report?");
        confirm.setContentText("This will mark the report as rejected, indicating the reported behavior was not problematic.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reportsFacade.rejectReport(currentReport.getReportId(), currentAdminId);
                currentReport.setStatus("Rejected");
                displayReport();
                notifyUpdate();
                showInfo("Report rejected successfully.");
            } catch (Exception e) {
                showError("Error rejecting report: " + e.getMessage());
            }
        }
    }

    /**
     * Handle close button
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }

    /**
     * Notify parent of update
     */
    private void notifyUpdate() {
        if (onReportUpdated != null) {
            onReportUpdated.run();
        }
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
