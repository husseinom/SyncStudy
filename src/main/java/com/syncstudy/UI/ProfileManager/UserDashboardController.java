package com.syncstudy.UI.ProfileManager;

import com.syncstudy.BL.AdminManager.AdminFacade;
import com.syncstudy.BL.ProfileManager.UserProfile;
import com.syncstudy.BL.SessionManager.SessionFacade;
import com.syncstudy.BL.SessionManager.User;
import com.syncstudy.UI.SessionManager.LoginController;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class UserDashboardController {
    @FXML public Button allProfilesButton;
    @FXML public Button updateButton;
    @FXML private Button profileButton;
    @FXML private Button userInfoButton;
    @FXML private Button logoutButton;
    @FXML private Button submitUpdateButton;
    @FXML private Button deleteAccountButton;
    //change buttons
    @FXML private Label welcomeLabel;
    @FXML private Label messageLabel;
    @FXML private VBox userInfo;
    @FXML private BorderPane mainPane;
    @FXML private VBox sidebar;

    private SessionFacade session;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortByCombo;
    @FXML private TableView<User> profilesTable;
    @FXML private TableColumn<User, Boolean> selectColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private Pagination pagination;
    @FXML private Label totalUsersLabel;
    @FXML private Button backButton;

    private ObservableList<UserProfile> profilesList;
    private static final int PAGE_SIZE = 20;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        this.session = SessionFacade.getInstance();

        // Set welcome message
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, User");
            //+this.session.getCurrentUser().getFullName()
        }

        // Load other profiles by default
    }

    public void showUserInfo(User user) {

    }

    /**
     * Handle clear filters action
     */
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        statusFilter.setValue("All");
        sortByCombo.setValue("Name");
        loadUsers();
    }

    public void showOtherProfiles(List<UserProfile> profiles) {

    }

    /**
     * Show error message as an alert
     * @param msg the error message to show
     */
    public void showErrorMessage(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Handles a click on the logout button
     */
    public void handleLogout() {
        if(this.session.logout()) {
            navigateToLogin();
        }
        else {
            showErrorMessage("Logout failed");
        }
    }

    @FXML
    private void handleBackToGroups(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/GroupManager/GroupManager.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new javafx.scene.Scene(root));
            }
        } catch (IOException e) {
            showErrorMessage("Cannot open groups: " + e.getMessage());
        }
    }

    /**
     * Handle account update - Load update form
     */
    @FXML
    public void handleUpdateAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/ProfileManager/UpdateAccountForm.fxml"));
            Parent updateForm = loader.load();

            // Get the controller and pass current user data
            UpdateAccountController updateController = loader.getController();
            updateController.setDashboardController(this);

            User currentUser = session.getCurrentUser();
            UserProfile currentProfile = session.findProfile();

            if (currentUser != null) {
                updateController.setUser(currentUser, currentProfile);
            } else {
                showErrorMessage("Could not load user data");
                return;
            }

            // Load into center pane
            mainPane.setCenter(updateForm);

            // Update button styles
            updateButtonStyles(updateButton);
        } catch (IOException e) {
            showErrorMessage("Failed to load update form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Actually perform the account update (called by UpdateAccountController)
     */
    public boolean updateAccount(String username, String passwordHash, String email,
                                 String firstname, String lastname, String university, String department) {
        return session.updateAccount(username, passwordHash, email, firstname, lastname, university, department);
    }

    /**
     * Handles a click on View Profile button
     * Loads and shows the logged user's own profile
     */
    public void handleViewOwnProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/ProfileManager/OwnProfile.fxml"));
            Parent profileView = loader.load();

            // Get the controller and pass it the current user data
            OwnProfileController profileController = loader.getController();
            profileController.setController(this);
            profileController.setUser(session.getCurrentUser());

            // Load into center pane
            mainPane.setCenter(profileView);

            // Update button styles
            updateButtonStyles(profileButton);
        } catch (IOException e) {
            showErrorMessage("Failed to load profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle view other profiles - Load profiles list
     */
    @FXML
    public void handleViewOtherProfiles() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/ProfileManager/OtherProfiles.fxml"));
            Parent profilesView = loader.load();

            // Get the controller
            OtherProfilesController profilesController = loader.getController();
            profilesController.setDashboardController(this);

            // Load into center pane
            mainPane.setCenter(profilesView);

            // Update button styles
            updateButtonStyles(allProfilesButton);
        } catch (IOException e) {
            showErrorMessage("Failed to load profiles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle account deletion after click on delete account
     */
    public void handleDeleteAccount() {
        if (this.session.deleteAccount()) {
            navigateToLogin();
        }
        else {
            showErrorMessage("Error deleting account");
        }

    }

    public void setCurrentUserId(Long userId) {
        this.session.setLoggedUserId(userId);
    }

    /**
     * Navigate to login page
     */
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/login.fxml"));
            Parent dashboard = loader.load();

            // Switch scene
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(dashboard));
            stage.setTitle("SyncStudy - Login");
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.centerOnScreen();

        } catch (IOException e) {
            messageLabel.setText("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update sidebar button styles
     */
    private void updateButtonStyles(Button activeButton) {
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 20;";
        String activeStyle = "-fx-background-color: #495057; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 20;";

        // Reset all buttons
        if (profileButton != null) {
            profileButton.setStyle(defaultStyle);
        }

        // Set active button
        if (activeButton != null) {
            activeButton.setStyle(activeStyle);
        }
    }

    /**
     * Load users with current filters
     */
    private void loadUsers() {
        loadProfilesForPage(0);
        updatePagination();
    }

    /**
     * Load profiles for a specific page
     */
    private void loadProfilesForPage(int page) {
        String search = searchField.getText();
        String sortBy = getSortByValue();

        List<UserProfile> profiles = session.findAllProfiles(search,sortBy,page,PAGE_SIZE);
        profilesList.setAll(profiles);

        int total = session.getTotalProfilesCount(search);
        int start = page * PAGE_SIZE + 1;
        int end = Math.min(start + profiles.size() - 1, total);

        if (total == 0) {
            totalUsersLabel.setText("No users found");
        } else {
            totalUsersLabel.setText(String.format("Showing %d-%d of %d users", start, end, total));
        }
    }

    /**
     * Update pagination control
     */
    private void updatePagination() {
        String search = searchField.getText();
        String status = statusFilter.getValue();
        int total = session.getTotalProfilesCount(search);
        int pageCount = (int) Math.ceil((double) total / PAGE_SIZE);
        pagination.setPageCount(Math.max(1, pageCount));
    }

    /**
     * Get sort by value for DAO
     */
    private String getSortByValue() {
        String selected = sortByCombo.getValue();
        if (selected == null) return "name";
        switch (selected) {
            case "Registration Date": return "registration";
            case "Last Login": return "lastlogin";
            default: return "name";
        }
    }

    /**
     * Clear the center pane (close embedded view)
     */
    public void clearCenterPane() {
        VBox defaultView = new VBox();
        defaultView.setAlignment(Pos.CENTER);
        defaultView.getChildren().add(new Label("Select an option from the sidebar"));
        mainPane.setCenter(defaultView);
    }
}
