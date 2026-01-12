package com.syncstudy.UI.AdminManager;

import com.syncstudy.BL.AdminManager.AdminFacade;
import com.syncstudy.BL.AdminManager.BlockRecord;
import com.syncstudy.BL.AdminManager.UserActivity;
import com.syncstudy.BL.SessionManager.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for User Management panel
 * Handles user listing, blocking, unblocking, and deletion
 */
public class UserManagementController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortByCombo;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Boolean> selectColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> universityColumn;
    @FXML private TableColumn<User, String> registrationColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, String> lastLoginColumn;
    @FXML private TableColumn<User, Void> actionsColumn;
    @FXML private Pagination pagination;
    @FXML private Label totalUsersLabel;

    private AdminFacade adminFacade;
    private ObservableList<User> usersList;
    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        adminFacade = AdminFacade.getInstance();
        usersList = FXCollections.observableArrayList();

        setupFilters();
        setupTable();
        setupPagination();
        loadUsers();
    }

    /**
     * Setup filter controls
     */
    private void setupFilters() {
        // Status filter
        statusFilter.setItems(FXCollections.observableArrayList("All", "Active", "Blocked"));
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> loadUsers());

        // Sort by
        sortByCombo.setItems(FXCollections.observableArrayList("Name", "Registration Date", "Last Login"));
        sortByCombo.setValue("Name");
        sortByCombo.setOnAction(e -> loadUsers());

        // Search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            loadUsers();
        });
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        // Checkbox column
        selectColumn.setCellFactory(col -> new TableCell<User, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : checkBox);
            }
        });

        // Name column
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayName()));

        // Email column
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEmail() != null ? data.getValue().getEmail() : "-"));

        // University column
        universityColumn.setCellValueFactory(data -> {
            User user = data.getValue();
            String university = user.getUniversity() != null ? user.getUniversity() : "";
            String department = user.getDepartment() != null ? user.getDepartment() : "";
            String combined = university;
            if (!department.isEmpty()) {
                combined += (university.isEmpty() ? "" : " / ") + department;
            }
            return new SimpleStringProperty(combined.isEmpty() ? "-" : combined);
        });

        // Registration date column
        registrationColumn.setCellValueFactory(data -> {
            if (data.getValue().getRegistrationDate() != null) {
                return new SimpleStringProperty(data.getValue().getRegistrationDate().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("-");
        });

        // Status column with colored badge
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusColumn.setCellFactory(col -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.setPadding(new Insets(2, 8, 2, 8));
                    if ("Blocked".equals(status)) {
                        badge.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 4;");
                    } else {
                        badge.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 4;");
                    }
                    setGraphic(badge);
                }
            }
        });

        // Last login column
        lastLoginColumn.setCellValueFactory(data -> {
            if (data.getValue().getLastLogin() != null) {
                return new SimpleStringProperty(data.getValue().getLastLogin().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("Never");
        });

        // Actions column
        actionsColumn.setCellFactory(col -> new TableCell<User, Void>() {
            private final Button viewBtn = new Button("View");
            private final Button blockBtn = new Button("Block");
            private final Button deleteBtn = new Button("Delete");
            private final HBox buttons = new HBox(8, viewBtn, blockBtn, deleteBtn);

            {
                buttons.setAlignment(Pos.CENTER);

                // Style avec taille minimum pour éviter la troncature
                viewBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
                viewBtn.setMinWidth(60);

                blockBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
                blockBtn.setMinWidth(70);

                deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
                deleteBtn.setMinWidth(70);

                viewBtn.setOnAction(e -> handleViewActivity(getTableView().getItems().get(getIndex())));
                blockBtn.setOnAction(e -> handleBlockUnblock(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    if (user.isBlocked()) {
                        blockBtn.setText("Unblock");
                        blockBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
                    } else {
                        blockBtn.setText("Block");
                        blockBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black; -fx-font-size: 12px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
                    }
                    setGraphic(buttons);
                }
            }
        });

        usersTable.setItems(usersList);
    }

    /**
     * Setup pagination
     */
    private void setupPagination() {
        pagination.setPageFactory(pageIndex -> {
            loadUsersForPage(pageIndex);
            return new VBox(); // Return empty node, table is updated separately
        });
    }

    /**
     * Load users with current filters
     */
    private void loadUsers() {
        loadUsersForPage(0);
        updatePagination();
    }

    /**
     * Load users for a specific page
     */
    private void loadUsersForPage(int page) {
        String search = searchField.getText();
        String status = statusFilter.getValue();
        String sortBy = getSortByValue();

        List<User> users = adminFacade.getAllUsers(search, status, sortBy, page, PAGE_SIZE);
        usersList.setAll(users);

        int total = adminFacade.getTotalUsersCount(search, status);
        int start = page * PAGE_SIZE + 1;
        int end = Math.min(start + users.size() - 1, total);

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
        int total = adminFacade.getTotalUsersCount(search, status);
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
     * Handle view activity action
     */
    private void handleViewActivity(User user) {
        showUserActivityDialog(user);
    }

    /**
     * Handle block/unblock action
     */
    private void handleBlockUnblock(User user) {
        if (user.isBlocked()) {
            showUnblockDialog(user);
        } else {
            showBlockDialog(user);
        }
    }

    /**
     * Handle delete action
     */
    private void handleDelete(User user) {
        showDeleteDialog(user);
    }

    /**
     * Show block user dialog
     */
    private void showBlockDialog(User user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Block User Account");
        dialog.setHeaderText("Block " + user.getDisplayName() + "'s Account?");

        // Set button types
        ButtonType blockButtonType = new ButtonType("Block Account", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(blockButtonType, ButtonType.CANCEL);

        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label info = new Label("User: " + user.getDisplayName() + "\nEmail: " +
                (user.getEmail() != null ? user.getEmail() : "N/A"));

        Label reasonLabel = new Label("Reason for blocking:");
        ComboBox<String> reasonCombo = new ComboBox<>();
        reasonCombo.setItems(FXCollections.observableArrayList(
                "Policy violation",
                "Spam activity",
                "Inappropriate behavior",
                "Other"
        ));
        reasonCombo.setEditable(true);
        reasonCombo.setPromptText("Select or enter reason...");
        reasonCombo.setPrefWidth(300);

        content.getChildren().addAll(info, reasonLabel, reasonCombo);
        dialog.getDialogPane().setContent(content);

        // Enable/Disable block button based on reason
        Button blockButton = (Button) dialog.getDialogPane().lookupButton(blockButtonType);
        blockButton.setDisable(true);
        reasonCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            blockButton.setDisable(newVal == null || newVal.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == blockButtonType) {
                return reasonCombo.getValue();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            try {
                boolean success = adminFacade.blockUser(user.getId(), reason);
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            user.getDisplayName() + "'s account has been blocked.");
                    loadUsers();
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            }
        });
    }

    /**
     * Show unblock user dialog
     */
    private void showUnblockDialog(User user) {
        // Get block history
        List<BlockRecord> history = adminFacade.getBlockHistory(user.getId());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unblock User Account");
        alert.setHeaderText("Activate " + user.getDisplayName() + "'s Account?");

        StringBuilder content = new StringBuilder();
        if (!history.isEmpty()) {
            BlockRecord lastBlock = history.get(0);
            content.append("Last blocked: ");
            if (lastBlock.getBlockDate() != null) {
                content.append(lastBlock.getBlockDate().format(DATE_FORMATTER));
            }
            content.append("\nBlocked by: ").append(lastBlock.getAdminUsername() != null ?
                    lastBlock.getAdminUsername() : "Unknown");
            content.append("\nReason: ").append(lastBlock.getReason());
        }
        alert.setContentText(content.toString());

        ButtonType unblockButton = new ButtonType("Unblock Account");
        alert.getButtonTypes().setAll(unblockButton, ButtonType.CANCEL);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == unblockButton) {
            try {
                boolean success = adminFacade.unblockUser(user.getId());
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            user.getDisplayName() + "'s account is now active.");
                    loadUsers();
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            }
        }
    }

    /**
     * Show delete user dialog
     */
    private void showDeleteDialog(User user) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Delete User Account");
        dialog.setHeaderText("ONE MINUTE =>  CRITICAL ACTION: Delete " + user.getDisplayName() + "'s Account?");

        ButtonType deleteButtonType = new ButtonType("Delete Account", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label warning = new Label("This action cannot be undone. All user data will be permanently deleted:");
        warning.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");

        Label dataList = new Label(
                "• User profile and personal information\n" +
                "• All messages sent\n" +
                "• All files uploaded\n" +
                "• All group memberships\n" +
                "• All activity history"
        );

        Label confirmLabel = new Label("Type DELETE to confirm:");
        TextField confirmField = new TextField();
        confirmField.setPromptText("Type DELETE here");

        content.getChildren().addAll(warning, dataList, confirmLabel, confirmField);
        dialog.getDialogPane().setContent(content);

        Button deleteButton = (Button) dialog.getDialogPane().lookupButton(deleteButtonType);
        deleteButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        deleteButton.setDisable(true);

        confirmField.textProperty().addListener((obs, oldVal, newVal) -> {
            deleteButton.setDisable(!"DELETE".equals(newVal));
        });

        dialog.setResultConverter(dialogButton -> dialogButton == deleteButtonType);

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && result.get()) {
            try {
                boolean success = adminFacade.deleteUser(user.getId());
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            "User account deleted successfully.");
                    loadUsers();
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            }
        }
    }

    /**
     * Show user activity dialog
     */
    private void showUserActivityDialog(User user) {
        UserActivity activity = adminFacade.getUserActivity(user.getId());
        List<BlockRecord> blockHistory = adminFacade.getBlockHistory(user.getId());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("User Activity - " + user.getDisplayName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(600, 500);

        TabPane tabPane = new TabPane();

        // Overview tab
        Tab overviewTab = new Tab("Overview");
        overviewTab.setClosable(false);
        VBox overviewContent = new VBox(10);
        overviewContent.setPadding(new Insets(15));
        overviewContent.getChildren().addAll(
                new Label("Username: " + activity.getUsername()),
                new Label("Full Name: " + (activity.getFullName() != null ? activity.getFullName() : "N/A")),
                new Label("Email: " + (activity.getEmail() != null ? activity.getEmail() : "N/A")),
                new Label("Status: " + (activity.isBlocked() ? "Blocked" : "Active")),
                new Label("Member for: " + activity.getAccountAgeDays() + " days"),
                new Label("Last Login: " + (activity.getLastLogin() != null ?
                        activity.getLastLogin().format(DATE_FORMATTER) : "Never")),
                new Label("Engagement Score: " + activity.getEngagementScore() + "/100")
        );
        overviewTab.setContent(overviewContent);

        // Statistics tab
        Tab statsTab = new Tab("Statistics");
        statsTab.setClosable(false);
        VBox statsContent = new VBox(10);
        statsContent.setPadding(new Insets(15));
        statsContent.getChildren().addAll(
                new Label("Messages Sent: " + activity.getMessagesCount()),
                new Label("Files Uploaded: " + activity.getFilesCount()),
                new Label("Groups Joined: " + activity.getGroupsCount()),
                new Label("Sessions Created: " + activity.getSessionsCreated()),
                new Label("Sessions Attended: " + activity.getSessionsAttended()),
                new Label("Most Active Group: " + activity.getMostActiveGroup()),
                new Label("Avg Messages/Day: " + String.format("%.2f", activity.getAvgMessagesPerDay()))
        );
        statsTab.setContent(statsContent);

        // Block History tab
        Tab historyTab = new Tab("Block History");
        historyTab.setClosable(false);
        VBox historyContent = new VBox(10);
        historyContent.setPadding(new Insets(15));

        if (blockHistory.isEmpty()) {
            historyContent.getChildren().add(new Label("No block history."));
        } else {
            for (BlockRecord record : blockHistory) {
                VBox recordBox = new VBox(5);
                recordBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 4; -fx-padding: 10;");
                recordBox.getChildren().addAll(
                        new Label("Blocked: " + (record.getBlockDate() != null ?
                                record.getBlockDate().format(DATE_FORMATTER) : "N/A")),
                        new Label("By: " + (record.getAdminUsername() != null ?
                                record.getAdminUsername() : "Unknown")),
                        new Label("Reason: " + record.getReason()),
                        new Label("Status: " + (record.isActive() ? "Active" : "Resolved")),
                        new Label("Unblocked: " + (record.getUnblockDate() != null ?
                                record.getUnblockDate().format(DATE_FORMATTER) : "N/A"))
                );
                historyContent.getChildren().add(recordBox);
            }
        }

        ScrollPane scrollPane = new ScrollPane(historyContent);
        scrollPane.setFitToWidth(true);
        historyTab.setContent(scrollPane);

        tabPane.getTabs().addAll(overviewTab, statsTab, historyTab);
        dialog.getDialogPane().setContent(tabPane);

        dialog.showAndWait();
    }

    /**
     * Handle search action
     */
    @FXML
    private void handleSearch() {
        loadUsers();
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

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

