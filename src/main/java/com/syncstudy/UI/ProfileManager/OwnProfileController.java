package com.syncstudy.UI.ProfileManager;

import com.syncstudy.BL.ProfileManager.UserProfile;
import com.syncstudy.BL.SessionManager.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class OwnProfileController {
    // From diagram: uses GroupController
    private UserDashboardController controller;

    @FXML private Label fullnameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label universityLabel;
    @FXML private Label departmentLabel;
    @FXML private Label groupsCountLabel;
    @FXML private Button closeButton;

    private User currentUser;

    /**
     * Initialize the details view
     */
    @FXML
    public void initialize() {

    }

    // Method to set the main controller
    public void setController(UserDashboardController controller) {
        this.controller = controller;
    }

    public void setUser(User user) {
        this.currentUser = user;
        displayUserInfo();
    }

    /**
     * Display user information
     */
    private void displayUserInfo() {
        if (currentUser == null) {
            System.out.println("ERROR: currentUser is null!");
            return;
        }
        // Display
        usernameLabel.setText("Username: " + (currentUser.getUsername() != null ? currentUser.getUsername() : "N/A"));
        fullnameLabel.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "N/A"); // Header already says "User"
        emailLabel.setText("Email: " + (currentUser.getEmail() != null ? currentUser.getEmail() : "N/A"));
        universityLabel.setText("University: " + (currentUser.getUniversity() != null ? currentUser.getUniversity() : "N/A"));
        departmentLabel.setText("Department: " + (currentUser.getDepartment() != null ? currentUser.getDepartment() : "N/A"));
        groupsCountLabel.setText("Is in " + currentUser.getGroupsCount() + " groups");
    }

    @FXML
    private void onClose() {
        // Since this is embedded, just clear the center pane
        if (controller != null) {
            controller.clearCenterPane();
        }
    }
}
