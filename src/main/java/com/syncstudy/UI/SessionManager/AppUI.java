package com.syncstudy.UI.SessionManager;

import com.syncstudy.BL.SessionManager.SessionFacade;
import com.syncstudy.BL.SessionManager.User;
import com.syncstudy.UI.ChatManager.ChatController;
import com.syncstudy.PL.DatabaseInitializer;
import com.syncstudy.WS.TcpChatServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.BindException;
import java.net.URL;

public class AppUI extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Initialize database tables at startup
        DatabaseInitializer.initialize();

        // Start embedded TCP server in background (daemon) so it doesn't block JavaFX thread.
        startEmbeddedServer(9000);


        URL fxml = AppUI.class.getResource("/com/syncstudy/UI/login.fxml");
        if (fxml == null) {
            throw new IllegalStateException("FXML resource not found: /com/syncstudy/UI/login.fxml. "
                    + "Make sure the file exists under src/main/resources/com/syncstudy/UI/");
        }

        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();

        Object controller = loader.getController();
        if (controller instanceof LoginController) {
            ((LoginController) controller).setUserManager(SessionFacade.getInstance());
        }

        stage.setTitle("SyncStudy - Login");
        Scene scene = new Scene(root, 480, 320);
        applyGlobalStyles(scene);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Applique les styles CSS globaux à une scène
     * @param scene La scène à styliser
     */
    public static void applyGlobalStyles(Scene scene) {
        try {
            // Styles de base
            String baseStyles = AppUI.class.getResource("/com/syncstudy/UI/styles/base.css").toExternalForm();
            String componentStyles = AppUI.class.getResource("/com/syncstudy/UI/styles/components.css").toExternalForm();
            String layoutStyles = AppUI.class.getResource("/com/syncstudy/UI/styles/layout.css").toExternalForm();
            String loginStyles = AppUI.class.getResource("/com/syncstudy/UI/styles/login.css").toExternalForm();

            scene.getStylesheets().addAll(baseStyles, componentStyles, layoutStyles, loginStyles);
        } catch (Exception e) {
            System.err.println("Warning: Could not load some CSS files: " + e.getMessage());
        }
    }

    /**
     * Applique les styles CSS pour le module Admin
     * @param scene La scène à styliser
     */
    public static void applyAdminStyles(Scene scene) {
        applyGlobalStyles(scene);
        try {
            String adminStyles = AppUI.class.getResource("/com/syncstudy/UI/AdminManager/admin-styles.css").toExternalForm();
            if (!scene.getStylesheets().contains(adminStyles)) {
                scene.getStylesheets().add(adminStyles);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load admin CSS: " + e.getMessage());
        }
    }

    /**
     * Applique les styles CSS pour le module Group/Category
     * @param scene La scène à styliser
     */
    public static void applyGroupStyles(Scene scene) {
        applyGlobalStyles(scene);
        try {
            String groupStyles = AppUI.class.getResource("/com/syncstudy/UI/GroupManager/group-styles.css").toExternalForm();
            if (!scene.getStylesheets().contains(groupStyles)) {
                scene.getStylesheets().add(groupStyles);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load group CSS: " + e.getMessage());
        }
    }

    /**
     * Applique les styles CSS pour le module Membership
     * @param scene La scène à styliser
     */
    public static void applyMembershipStyles(Scene scene) {
        applyGlobalStyles(scene);
        try {
            String membershipStyles = AppUI.class.getResource("/com/syncstudy/UI/GroupMembership/membership-styles.css").toExternalForm();
            if (!scene.getStylesheets().contains(membershipStyles)) {
                scene.getStylesheets().add(membershipStyles);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load membership CSS: " + e.getMessage());
        }
    }

    private void startEmbeddedServer(int port) {
        Thread serverThread = new Thread(() -> {
            try {
                new TcpChatServer(port).start();
            } catch (BindException be) {
                // Port already in use — likely a separate server instance is running; proceed without failing.
                System.out.println("Embedded server: port " + port + " already in use; assuming external server is running.");
            } catch (IOException e) {
                System.err.println("Embedded server failed to start: " + e.getMessage());
            }
        }, "Embedded-TcpChatServer");
        serverThread.setDaemon(true); // allow JVM to exit when only this thread remains
        serverThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
