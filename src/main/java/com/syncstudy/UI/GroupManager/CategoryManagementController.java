package com.syncstudy.UI.GroupManager;

import com.syncstudy.BL.GroupManager.Category;
import com.syncstudy.BL.GroupManager.CategoryFacade;
import com.syncstudy.BL.GroupManager.Group;
import com.syncstudy.BL.AdminManager.AdminFacade;
import com.syncstudy.BL.SessionManager.User;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Category Management UI
 * Handles all category CRUD operations and group assignments
 */
public class CategoryManagementController {

    // FXML Components
    @FXML private TableView<Category> categoriesTable;
    @FXML private TableColumn<Category, String> iconColumn;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TableColumn<Category, String> descriptionColumn;
    @FXML private TableColumn<Category, String> adminColumn;
    @FXML private TableColumn<Category, Integer> groupsCountColumn;
    @FXML private TableColumn<Category, String> createdColumn;
    @FXML private TableColumn<Category, Void> actionsColumn;
    @FXML private TextField searchField;
    @FXML private Label totalLabel;

    // Facade
    private CategoryFacade categoryFacade;

    // Data
    private ObservableList<Category> categoriesList = FXCollections.observableArrayList();

    // Date formatter
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        categoryFacade = CategoryFacade.getInstance();
        setupTableColumns();
        setupSearch();
        loadCategories();
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        // Icon/Color column
        iconColumn.setCellValueFactory(cellData -> {
            Category cat = cellData.getValue();
            String display = (cat.getIcon() != null ? cat.getIcon() : "📁");
            return new SimpleStringProperty(display);
        });
        iconColumn.setStyle("-fx-alignment: CENTER;");
        iconColumn.setPrefWidth(50);

        // Name column
        nameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getName()));
        nameColumn.setPrefWidth(150);

        // Description column
        descriptionColumn.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescription();
            if (desc != null && desc.length() > 50) {
                desc = desc.substring(0, 50) + "...";
            }
            return new SimpleStringProperty(desc != null ? desc : "");
        });
        descriptionColumn.setPrefWidth(200);

        // Admin column
        adminColumn.setCellValueFactory(cellData -> {
            String adminName = cellData.getValue().getCategoryAdministratorName();
            return new SimpleStringProperty(adminName != null ? adminName : "Not assigned");
        });
        adminColumn.setPrefWidth(150);

        // Groups count column
        groupsCountColumn.setCellValueFactory(cellData ->
            new SimpleIntegerProperty(cellData.getValue().getGroupsCount()).asObject());
        groupsCountColumn.setStyle("-fx-alignment: CENTER;");
        groupsCountColumn.setPrefWidth(80);

        // Created date column
        createdColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DATE_FORMAT));
            }
            return new SimpleStringProperty("");
        });
        createdColumn.setPrefWidth(130);

        // Actions column
        actionsColumn.setCellFactory(column -> new TableCell<Category, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button assignBtn = new Button("Assign Groups");
            private final HBox buttons = new HBox(5, editBtn, deleteBtn, assignBtn);

            {
                buttons.setAlignment(Pos.CENTER);

                editBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 3 8;");
                deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 3 8;");
                assignBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 3 8;");

                editBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleEditCategory(category);
                });

                deleteBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(category);
                });

                assignBtn.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleAssignGroups(category);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        actionsColumn.setPrefWidth(230);

        // Set table items
        categoriesTable.setItems(categoriesList);
    }

    /**
     * Setup search functionality
     */
    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.isEmpty()) {
                    loadCategories();
                } else {
                    List<Category> filtered = categoryFacade.searchCategories(newValue);
                    categoriesList.setAll(filtered);
                    updateTotalLabel();
                }
            });
        }
    }

    /**
     * Load all categories
     */
    private void loadCategories() {
        List<Category> categories = categoryFacade.getAllCategories();
        categoriesList.setAll(categories);
        updateTotalLabel();
    }

    /**
     * Update total label
     */
    private void updateTotalLabel() {
        if (totalLabel != null) {
            totalLabel.setText("Total: " + categoriesList.size() + " categories");
        }
    }

    /**
     * Handle create category button
     */
    @FXML
    public void handleCreateCategory() {
        showCategoryDialog(null);
    }

    /**
     * Handle edit category
     */
    private void handleEditCategory(Category category) {
        showCategoryDialog(category);
    }

    /**
     * Show category dialog (create/edit)
     */
    private void showCategoryDialog(Category category) {
        boolean isEdit = category != null;

        // Create dialog
        Dialog<Category> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Category" : "Create New Category");
        dialog.setHeaderText(isEdit ? "Edit category details" : "Enter new category details");

        // Set dialog buttons
        ButtonType saveButtonType = new ButtonType(isEdit ? "Save Changes" : "Create Category", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Name field
        TextField nameField = new TextField();
        nameField.setPromptText("Category name (required)");
        if (isEdit) nameField.setText(category.getName());

        // Description field
        TextArea descField = new TextArea();
        descField.setPromptText("Description (max 200 characters)");
        descField.setPrefRowCount(3);
        descField.setWrapText(true);
        if (isEdit && category.getDescription() != null) descField.setText(category.getDescription());

        // Character counter for description
        Label charCounter = new Label("0/200");
        descField.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            charCounter.setText(len + "/200");
            if (len > 200) {
                charCounter.setTextFill(Color.RED);
            } else {
                charCounter.setTextFill(Color.GRAY);
            }
        });
        if (isEdit && category.getDescription() != null) {
            charCounter.setText(category.getDescription().length() + "/200");
        }

        // Icon/Color combo
        ComboBox<String> iconColorCombo = new ComboBox<>();
        iconColorCombo.getItems().addAll(
            "📚 Blue - #007bff",
            "🧮 Green - #28a745",
            "🔬 Cyan - #17a2b8",
            "🌍 Yellow - #ffc107",
            "💻 Purple - #6f42c1",
            "📖 Pink - #e83e8c",
            "🏛️ Orange - #fd7e14",
            "🎯 Red - #dc3545",
            "🚀 Teal - #20c997",
            "📝 Gray - #6c757d"
        );
        iconColorCombo.setPromptText("Select icon/color");
        if (isEdit && category.getIcon() != null && category.getColor() != null) {
            String current = category.getIcon() + " - " + category.getColor();
            for (String item : iconColorCombo.getItems()) {
                if (item.contains(category.getIcon()) || item.contains(category.getColor())) {
                    iconColorCombo.setValue(item);
                    break;
                }
            }
        }

        // Admin combo (get users from AdminFacade)
        ComboBox<User> adminCombo = new ComboBox<>();
        adminCombo.setPromptText("Select administrator (optional)");
        try {
            List<User> users = AdminFacade.getInstance().getAllUsers("", "All", "name");
            adminCombo.getItems().addAll(users);
            adminCombo.setCellFactory(lv -> new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    setText(empty || user == null ? "" : user.getUsername() +
                        (user.getFullName() != null ? " (" + user.getFullName() + ")" : ""));
                }
            });
            adminCombo.setButtonCell(new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    setText(empty || user == null ? "Select administrator (optional)" :
                        user.getUsername() + (user.getFullName() != null ? " (" + user.getFullName() + ")" : ""));
                }
            });

            // Set current admin if editing
            if (isEdit && category.getCategoryAdministratorId() != null) {
                for (User user : users) {
                    if (user.getId().equals(category.getCategoryAdministratorId())) {
                        adminCombo.setValue(user);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
        }

        // Error label
        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setWrapText(true);

        // Add to grid
        grid.add(new Label("Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(charCounter, 1, 2);
        grid.add(new Label("Icon/Color:"), 0, 3);
        grid.add(iconColorCombo, 1, 3);
        grid.add(new Label("Administrator:"), 0, 4);
        grid.add(adminCombo, 1, 4);
        grid.add(errorLabel, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Disable save button if name is empty
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(nameField.getText().trim().isEmpty());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            saveButton.setDisable(newVal == null || newVal.trim().isEmpty());
        });

        // Handle result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String name = nameField.getText().trim();
                    String description = descField.getText();

                    // Parse icon/color
                    String icon = null;
                    String color = null;
                    String iconColorValue = iconColorCombo.getValue();
                    if (iconColorValue != null) {
                        String[] parts = iconColorValue.split(" - ");
                        if (parts.length >= 1) {
                            icon = parts[0].trim().split(" ")[0]; // Get emoji
                        }
                        if (parts.length >= 2) {
                            color = parts[1].trim();
                        }
                    }

                    // Get admin ID
                    Long adminId = adminCombo.getValue() != null ? adminCombo.getValue().getId() : null;

                    if (isEdit) {
                        boolean success = categoryFacade.updateCategory(
                            category.getCategoryId(), name, description, icon, color, adminId);
                        if (success) {
                            showToast("Category updated successfully!", false);
                            loadCategories();
                        }
                    } else {
                        Category created = categoryFacade.createCategory(name, description, icon, color, adminId);
                        if (created != null) {
                            showToast("Category '" + name + "' created successfully!", false);
                            loadCategories();
                        }
                    }
                } catch (IllegalArgumentException e) {
                    showToast(e.getMessage(), true);
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * Handle delete category
     */
    private void handleDeleteCategory(Category category) {
        // Check if can delete
        if (!categoryFacade.canDeleteCategory(category.getCategoryId())) {
            // Show cannot delete dialog
            showCannotDeleteDialog(category);
        } else {
            // Show confirm delete dialog
            showConfirmDeleteDialog(category);
        }
    }

    /**
     * Show cannot delete dialog (category has groups)
     */
    private void showCannotDeleteDialog(Category category) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Category Cannot Be Deleted");
        dialog.setHeaderText("️ This category cannot be deleted");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        int groupCount = categoryFacade.getCategoryGroupCount(category.getCategoryId());

        Label messageLabel = new Label("This category contains " + groupCount + " groups.\n" +
            "Please reassign or remove all associated groups before deleting this category.");
        messageLabel.setWrapText(true);

        // List groups in this category
        List<Group> groups = categoryFacade.getCategoryGroups(category.getCategoryId());
        ListView<String> groupsList = new ListView<>();
        for (Group group : groups) {
            groupsList.getItems().add(group.getName() + " (" + group.getMemberCount() + " members)");
        }
        groupsList.setPrefHeight(150);

        content.getChildren().addAll(messageLabel, new Label("Groups in this category:"), groupsList);

        dialog.getDialogPane().setContent(content);

        ButtonType reassignButton = new ButtonType("Reassign Groups", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(reassignButton, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == reassignButton) {
            handleAssignGroups(category);
        }
    }

    /**
     * Show confirm delete dialog
     */
    private void showConfirmDeleteDialog(Category category) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Delete Category");
        dialog.setHeaderText("⚠️ Delete Category '" + category.getName() + "'?");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label warningLabel = new Label("This action cannot be undone.");
        warningLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");

        // Category details
        VBox details = new VBox(5);
        details.getChildren().addAll(
            new Label("Name: " + category.getName()),
            new Label("Description: " + (category.getDescription() != null ? category.getDescription() : "N/A")),
            new Label("Groups: " + category.getGroupsCount())
        );

        if (category.getCategoryAdministratorId() != null) {
            Label adminNote = new Label("Note: The Category Administrator assignment will be removed.");
            adminNote.setStyle("-fx-text-fill: #856404; -fx-font-style: italic;");
            details.getChildren().add(adminNote);
        }

        content.getChildren().addAll(warningLabel, details);

        dialog.getDialogPane().setContent(content);

        ButtonType deleteButton = new ButtonType("Delete Category", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteButton, ButtonType.CANCEL);

        // Style delete button
        Button deleteBtn = (Button) dialog.getDialogPane().lookupButton(deleteButton);
        deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");

        dialog.setResultConverter(button -> button == deleteButton);

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && result.get()) {
            try {
                boolean success = categoryFacade.deleteCategory(category.getCategoryId());
                if (success) {
                    showToast("Category deleted successfully.", false);
                    loadCategories();
                }
            } catch (Exception e) {
                showToast("Error: " + e.getMessage(), true);
            }
        }
    }

    /**
     * Handle assign groups to category
     */
    private void handleAssignGroups(Category category) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Assign Groups");
        dialog.setHeaderText("Assign Groups to " + category.getName());
        dialog.getDialogPane().setPrefWidth(600);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Current groups in category
        Label currentLabel = new Label("Groups already in this category:");
        currentLabel.setStyle("-fx-font-weight: bold;");

        List<Group> currentGroups = categoryFacade.getCategoryGroups(category.getCategoryId());
        ListView<Group> currentGroupsList = new ListView<>();
        currentGroupsList.getItems().addAll(currentGroups);
        currentGroupsList.setPrefHeight(120);
        currentGroupsList.setCellFactory(lv -> new ListCell<Group>() {
            private final Button removeBtn = new Button("Remove");
            private final HBox hbox = new HBox(10);
            private final Label label = new Label();

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(label, Priority.ALWAYS);
                removeBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 10px;");
                hbox.getChildren().addAll(label, removeBtn);

                removeBtn.setOnAction(e -> {
                    Group group = getItem();
                    if (group != null) {
                        categoryFacade.removeGroupFromCategory(group.getGroupId());
                        currentGroupsList.getItems().remove(group);
                        // Refresh available groups
                        loadAvailableGroups((ListView<Group>) hbox.getScene().lookup("#availableGroupsList"),
                            (CheckBox) hbox.getScene().lookup("#showUnassignedOnly"),
                            (TextField) hbox.getScene().lookup("#groupSearchField"));
                    }
                });
            }

            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setGraphic(null);
                } else {
                    label.setText(group.getName() + " (" + group.getMemberCount() + " members)");
                    setGraphic(hbox);
                }
            }
        });

        // Available groups
        Label availableLabel = new Label("Available groups:");
        availableLabel.setStyle("-fx-font-weight: bold;");

        // Search and filter
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        TextField groupSearchField = new TextField();
        groupSearchField.setId("groupSearchField");
        groupSearchField.setPromptText("Search groups...");
        groupSearchField.setPrefWidth(200);

        CheckBox showUnassignedOnly = new CheckBox("Show only unassigned groups");
        showUnassignedOnly.setId("showUnassignedOnly");

        filterBox.getChildren().addAll(groupSearchField, showUnassignedOnly);

        // Available groups list with checkboxes
        ListView<Group> availableGroupsList = new ListView<>();
        availableGroupsList.setId("availableGroupsList");
        availableGroupsList.setPrefHeight(200);
        availableGroupsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        List<Long> selectedGroupIds = new ArrayList<>();

        availableGroupsList.setCellFactory(lv -> new ListCell<Group>() {
            private final CheckBox checkBox = new CheckBox();
            private final Label label = new Label();
            private final Label categoryLabel = new Label();
            private final HBox hbox = new HBox(10);

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                categoryLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
                hbox.getChildren().addAll(checkBox, label, categoryLabel);

                checkBox.setOnAction(e -> {
                    Group group = getItem();
                    if (group != null) {
                        if (checkBox.isSelected()) {
                            if (!selectedGroupIds.contains(group.getGroupId())) {
                                selectedGroupIds.add(group.getGroupId());
                            }
                        } else {
                            selectedGroupIds.remove(group.getGroupId());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setGraphic(null);
                } else {
                    label.setText(group.getName() + " (" + group.getMemberCount() + " members)");
                    String catName = group.getCategory() != null ?
                        "Currently in: " + group.getCategory().getName() : "Unassigned";
                    categoryLabel.setText(catName);
                    checkBox.setSelected(selectedGroupIds.contains(group.getGroupId()));
                    setGraphic(hbox);
                }
            }
        });

        // Load available groups
        loadAvailableGroups(availableGroupsList, showUnassignedOnly, groupSearchField);

        // Filter listeners
        showUnassignedOnly.setOnAction(e ->
            loadAvailableGroups(availableGroupsList, showUnassignedOnly, groupSearchField));
        groupSearchField.textProperty().addListener((obs, oldVal, newVal) ->
            loadAvailableGroups(availableGroupsList, showUnassignedOnly, groupSearchField));

        content.getChildren().addAll(
            currentLabel, currentGroupsList,
            new Separator(),
            availableLabel, filterBox, availableGroupsList
        );

        dialog.getDialogPane().setContent(content);

        ButtonType assignButton = new ButtonType("Assign Selected Groups", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(assignButton, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == assignButton && !selectedGroupIds.isEmpty()) {
                boolean success = categoryFacade.assignGroups(category.getCategoryId(), selectedGroupIds);
                if (success) {
                    showToast(selectedGroupIds.size() + " groups assigned to " + category.getName() + " successfully.", false);
                    loadCategories();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * Load available groups into list
     */
    private void loadAvailableGroups(ListView<Group> listView, CheckBox unassignedOnly, TextField searchField) {
        if (listView == null) return;

        List<Group> groups;
        if (unassignedOnly != null && unassignedOnly.isSelected()) {
            groups = categoryFacade.getUnassignedGroups();
        } else {
            groups = categoryFacade.getAllGroups();
        }

        // Filter by search
        if (searchField != null && searchField.getText() != null && !searchField.getText().isEmpty()) {
            String search = searchField.getText().toLowerCase();
            groups = groups.stream()
                .filter(g -> g.getName().toLowerCase().contains(search))
                .toList();
        }

        listView.getItems().setAll(groups);
    }

    /**
     * Show toast notification
     */
    private void showToast(String message, boolean isError) {
        Alert alert = new Alert(isError ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(isError ? "Error" : "Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Refresh button handler
     */
    @FXML
    public void handleRefresh() {
        loadCategories();
        showToast("Categories refreshed.", false);
    }
}

