package com.syncstudy.UI.ProfileManager;

import com.syncstudy.BL.ProfileManager.UserProfile;
import com.syncstudy.BL.SessionManager.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;

public class UpdateAccountController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField firstnameField;
    @FXML private TextField lastnameField;
    @FXML private TextField universityField;
    @FXML private TextField departmentField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private UserDashboardController dashboardController;
    private User currentUser;
    private UserProfile currentProfile;

    @FXML
    public void initialize() {
        // Nothing special needed here
    }

    /**
     * Set the parent dashboard controller
     */
    public void setDashboardController(UserDashboardController controller) {
        this.dashboardController = controller;
    }

    /**
     * Load current user data into the form
     */
    public void setUser(User user, UserProfile profile) {
        this.currentUser = user;
        this.currentProfile = profile;
        populateFields();
    }

    /**
     * Populate form fields with current user data
     */
    private void populateFields() {
        if (currentUser != null) {
            usernameField.setText(currentUser.getUsername());
            emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
            universityField.setText(currentUser.getUniversity() != null ? currentUser.getUniversity() : "");
            departmentField.setText(currentUser.getDepartment() != null ? currentUser.getDepartment() : "");
        }

        if (currentProfile != null) {
            firstnameField.setText(currentProfile.getFirstname() != null ? currentProfile.getFirstname() : "");
            lastnameField.setText(currentProfile.getLastname() != null ? currentProfile.getLastname() : "");
        }
    }

    /**
     * Handle update button click
     */
    @FXML
    private void handleUpdate() {
        // Validate inputs
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String firstname = firstnameField.getText().trim();
        String lastname = lastnameField.getText().trim();
        String university = universityField.getText().trim();
        String department = departmentField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Basic validation
        if (username.isEmpty() || email.isEmpty() || firstname.isEmpty() || lastname.isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }

        // Password validation
        String passwordHash = currentUser.getPasswordHash(); // Keep current password by default
        if (!password.isEmpty()) {
            if (!password.equals(confirmPassword)) {
                showError("Passwords do not match");
                return;
            }
            // Hash the new password
            passwordHash = BCrypt.hashpw(password,BCrypt.gensalt());
        }

        // Call the dashboard controller to update
        if (dashboardController != null) {
            boolean success = dashboardController.updateAccount(
                    username, passwordHash, email, firstname, lastname, university, department
            );

            if (success) {
                showSuccess("Account updated successfully!");
                if (dashboardController != null) {
                    dashboardController.clearCenterPane();
                }
            } else {
                showError("Failed to update account");
            }
        }
    }

    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
        if (dashboardController != null) {
            dashboardController.clearCenterPane();
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #dc3545;");
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #28a745;");
    }
}