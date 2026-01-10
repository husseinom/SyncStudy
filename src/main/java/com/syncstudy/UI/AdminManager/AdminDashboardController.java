package com.syncstudy.UI.AdminManager;

import com.syncstudy.BL.AdminManager.AdminFacade;
import com.syncstudy.BL.SessionManager.UserManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Controller for Admin Dashboard
 * Main navigation hub for admin features
 */
public class AdminDashboardController {

    @FXML private BorderPane mainPane;
    @FXML private VBox sidebar;
    @FXML private Button usersButton;
    @FXML private Button categoriesButton;
    @FXML private Button reportsButton;
    @FXML private Button membershipButton;
    @FXML private Label welcomeLabel;

    private AdminFacade adminFacade;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        adminFacade = AdminFacade.getInstance();

        // Set welcome message
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Admin");
        }

        // Load user management by default
        handleManageUsers();
    }

    /**
     * Set the current admin user ID
     * @param adminId admin user ID
     */
    public void setCurrentAdminId(Long adminId) {
        adminFacade.setCurrentAdminId(adminId);
    }

    /**
     * Handle Manage Users navigation
     */
    @FXML
    public void handleManageUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/AdminManager/UserManagement.fxml"));
            Parent userManagement = loader.load();
            mainPane.setCenter(userManagement);

            // Update button states
            updateButtonStyles(usersButton);
        } catch (IOException e) {
            showError("Failed to load User Management: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleManageMembership() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/AdminManager/GroupList.fxml"));
            Parent groupList = loader.load();
            mainPane.setCenter(groupList);
            updateButtonStyles(membershipButton);
        } catch (IOException e) {
            showError("Failed to load Group List: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle Manage Categories navigation
     */
    @FXML
    public void handleManageCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/GroupManager/CategoryManagement.fxml"));
            Parent categoryManagement = loader.load();
            mainPane.setCenter(categoryManagement);

            // Update button states
            updateButtonStyles(categoriesButton);
        } catch (IOException e) {
            showError("Failed to load Category Management: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle Manage Reports navigation
     */
    @FXML
    public void handleManageReports() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/ReportsManager/ReportsList.fxml"));
            Parent reportsManagement = loader.load();
            mainPane.setCenter(reportsManagement);

            // Update button states
            updateButtonStyles(reportsButton);
        } catch (IOException e) {
            showError("Failed to load Reports Management: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle logout action
     */
    @FXML
    public void handleLogout() {
        adminFacade.setCurrentAdminId(null);
        UserManager.getInstance().logout();
        // Return to login screen
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/login.fxml"));
            Parent login = loader.load();
            mainPane.getScene().setRoot(login);
        } catch (IOException e) {
            showError("Failed to logout: " + e.getMessage());
        }
    }

    /**
     * Update sidebar button styles
     */
    private void updateButtonStyles(Button activeButton) {
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 20;";
        String activeStyle = "-fx-background-color: #495057; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 20;";

        // Reset all buttons
        if (usersButton != null) {
            usersButton.setStyle(defaultStyle);
        }
        if (categoriesButton != null) {
            categoriesButton.setStyle(defaultStyle);
        }
        if (reportsButton != null) {
            reportsButton.setStyle(defaultStyle);
        }
        if (membershipButton != null) {
            membershipButton.setStyle(defaultStyle);
        }

        // Set active button
        if (activeButton != null) {
            activeButton.setStyle(activeStyle);
        }
    }

    /**
     * Show error alert
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

