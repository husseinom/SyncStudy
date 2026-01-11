// java
package com.syncstudy.UI.SessionManager;

import com.syncstudy.BL.AdminManager.AdminFacade;
import com.syncstudy.BL.SessionManager.SessionFacade;
import com.syncstudy.BL.SessionManager.User;
import com.syncstudy.BL.SessionManager.UserManager;
import com.syncstudy.UI.AdminManager.AdminDashboardController;
import com.syncstudy.UI.ProfileManager.RegisterController;
import com.syncstudy.UI.ProfileManager.UserDashboardController;
import com.syncstudy.UI.ChatManager.ChatController;
import com.syncstudy.WS.AppConfig;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Label errorLabel;

    private SessionFacade userManager;
    private Runnable onLoginSuccess;

    @FXML
    public void initialize() {
        this.userManager = SessionFacade.getInstance(); // Always get the singleton
    }

    // injected by AppUI after FXMLLoader.load()
    public void setUserManager(SessionFacade userManager) {
        this.userManager = userManager;
    }
    public void setOnLoginSuccess(Runnable onLoginSuccess) {this.onLoginSuccess = onLoginSuccess;}


    @FXML
    private void onLogin() {
        if (userManager == null) {
            try {
                userManager = SessionFacade.getInstance();
            } catch (Exception e) {
                setMessage("Internal error: UserManager not available.");
                return;
            }
        }
        String username = usernameField.getText();
        String password = passwordField.getText();

        boolean ok = userManager.login(username, password);
        if (ok) {
            messageLabel.setText("Login successful");
            User user = userManager.getCurrentUser();
            // Navigate to chat page
            if (onLoginSuccess != null) {
                userManager.setCurrentUser(user);
                onLoginSuccess.run();
            }

            // Get user and check if admin
            if (user != null && user.isAdmin()) {
                navigateToAdminDashboard(user);
            } else {
                messageLabel.setText("Login successful! (Non-admin user)");
//                showChatPage();
               // TODO: Navigate to regular user dashboard
                navigateToGroupsDashboard(user);
            }
        } else {
            messageLabel.setText("Invalid username or password");
        }
    }

    /**
     * Navigate to admin dashboard
     */
    private void navigateToAdminDashboard(User admin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/AdminManager/AdminDashboard.fxml"));
            Parent dashboard = loader.load();

            // Set admin ID in facade
            AdminDashboardController controller = loader.getController();
            controller.setCurrentAdminId(admin.getId());

            // Switch scene
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(dashboard));
            stage.setTitle("SyncStudy - Admin Dashboard");
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.centerOnScreen();

        } catch (IOException e) {
            messageLabel.setText("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to user dashboard
     */
    private void navigateToUserDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/ProfileManager/UserDashboardView.fxml"));
            Parent dashboard = loader.load();

            // Set user ID in facade
            UserDashboardController controller = loader.getController();
            controller.setCurrentUserId(user.getId());

            // Switch scene
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(dashboard));
            stage.setTitle("SyncStudy - User Dashboard");
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.centerOnScreen();

        } catch (IOException e) {
            messageLabel.setText("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to register page
     */
    @FXML
    private void navigateToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/ProfileManager/register.fxml"));
            Parent dashboard = loader.load();

            // Set user ID in facade
            RegisterController controller = loader.getController();
            controller.setSession(userManager);

            // Switch scene
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(dashboard));
            stage.setTitle("SyncStudy - Sign up");
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.centerOnScreen();

        } catch (IOException e) {
            messageLabel.setText("Error loading register: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to user dashboard (GroupManager)
     */
    private void navigateToGroupsDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/GroupManager/GroupManager.fxml"));
            Parent groupDashboard = loader.load();

            // Switch scene
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(groupDashboard));
            stage.setTitle("SyncStudy - Groupes d'étude");
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.centerOnScreen();

        } catch (IOException e) {
            messageLabel.setText("Error loading group dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showChatPage() {
        try {
            URL fxml = AppUI.class.getResource("/com/syncstudy/UI/chat.fxml");
            if (fxml == null) {
                throw new IllegalStateException("FXML resource not found: /com/syncstudy/UI/chat.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();

            ChatController chatController = loader.getController();

            // Get logged-in user from SessionFacade
            SessionFacade sessionFacade = SessionFacade.getInstance();
            User currentUser = sessionFacade.getCurrentUser();

            chatController.setCurrentUser(currentUser.getId(), currentUser.isAdmin());
            chatController.startRealtime(AppConfig.getChatHost(), AppConfig.getChatPort());

            // Set a default group (you'll need to modify this based on your requirements)
            // For now, using group ID 1 as an example
            chatController.setCurrentGroup(1L);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("SyncStudy - Chat");
            stage.setScene(new Scene(root, 800, 600));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMessage(String msg) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        // Show success toast notification
        System.out.println(message);
    }
}
