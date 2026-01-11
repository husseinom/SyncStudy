package com.syncstudy.UI.GroupManager;

import com.syncstudy.BL.SessionManager.SessionFacade;
import com.syncstudy.BL.GroupManager.GroupFacade;
import com.syncstudy.BL.GroupManager.Group;
import com.syncstudy.BL.GroupManager.Category;
import com.syncstudy.BL.SessionManager.User;
import com.syncstudy.UI.ChatManager.ChatController;
import com.syncstudy.UI.ProfileManager.UserDashboardController;
import com.syncstudy.WS.AppConfig;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

/**
 * GroupController - Control class for Group operations (from diagram)
 */
public class GroupController {
    // Attributes from diagram
    private SessionFacade session;
    private GroupFacade group;
    
    @FXML private TextField searchBar;
    @FXML private Label label;
    
    // Additional UI components (simple and effective)
    @FXML private TableView<Group> groupsTable;
    @FXML private TableColumn<Group, String> nameColumn;
    @FXML private TableColumn<Group, String> categoryColumn;
    @FXML private TableColumn<Group, Integer> membersColumn;
    @FXML private ComboBox<String> categoryFilter;
    
    private ObservableList<Group> groupsList;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        // Initialize facades as per diagram
        this.session = SessionFacade.getInstance();
        this.group = GroupFacade.getInstance();
        
        setupSimpleUI();
        loadAllGroups();
    }
    
    private void setupSimpleUI() {
        // Setup table
        groupsList = FXCollections.observableArrayList();
        groupsTable.setItems(groupsList);
        
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(cellData -> {
            Category cat = cellData.getValue().getCategory();
            return new SimpleStringProperty(cat != null ? cat.getName() : "Aucune");
        });
        membersColumn.setCellValueFactory(new PropertyValueFactory<>("memberCount"));
        
        // Setup category filter - simple list
        categoryFilter.setItems(FXCollections.observableArrayList(
            "Toutes", "Études", "Mathématiques", "Sciences", "Langues", "Informatique", 
            "Littérature", "Histoire", "Préparation aux examens", "Projet étudiant", "Autre"
        ));
        categoryFilter.setValue("Toutes");
        
        // Double-click to view details
        groupsTable.setRowFactory(tv -> {
            TableRow<Group> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onClick();
                }
            });
            return row;
        });
    }
    
    private void loadAllGroups() {
        try {
            List<Group> groups = group.filter(Optional.empty(), Optional.empty(), Optional.empty());
            groupsList.clear();
            groupsList.addAll(groups);
            
            if (label != null) {
                label.setText("Groupes disponibles: " + groups.size());
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement des groupes: " + e.getMessage());
        }
    }

    // Methods from diagram
    @FXML
    public void onSearch() {
        String searchText = searchBar.getText();
        try {
            if (searchText != null && !searchText.trim().isEmpty()) {
                List<Group> results = group.search(List.of(searchText.trim()));
                groupsList.clear();
                groupsList.addAll(results);
                label.setText("Résultats pour '" + searchText + "': " + results.size());
            } else {
                loadAllGroups();
            }
        } catch (Exception e) {
            showError("Erreur lors de la recherche: " + e.getMessage());
        }
    }
    
    @FXML
    public void onFilter() {
        String selectedCategory = categoryFilter.getValue();
        try {
            if (selectedCategory != null && !"Toutes".equals(selectedCategory)) {
                // Use search approach for filtering by category
                List<Group> allGroups = group.filter(Optional.empty(), Optional.empty(), Optional.empty());
                List<Group> filteredResults = allGroups.stream()
                    .filter(g -> g.getCategory() != null && selectedCategory.equals(g.getCategory().getName()))
                    .toList();
                    
                groupsList.clear();
                groupsList.addAll(filteredResults);
                label.setText("Groupes filtrés (" + selectedCategory + "): " + filteredResults.size());
            } else {
                loadAllGroups();
            }
        } catch (Exception e) {
            showError("Erreur lors du filtrage: " + e.getMessage());
        }
    }
    
    @FXML
    public void onClick() {
        Group selectedGroup = groupsTable.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            openGroupDetails(selectedGroup);
        } else {
            showError("Veuillez sélectionner un groupe.");
        }
    }
    @FXML
    public void onOpenChat() {
        Group selectedGroup = groupsTable.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            showError("Veuillez sélectionner un groupe.");
            return;
        }
        openChatForGroup(selectedGroup);
    }
    
    // Simple methods for UI actions
    @FXML
    private void onRefresh() {
        loadAllGroups();
    }
    
    @FXML
    private void onClear() {
        searchBar.clear();
        categoryFilter.setValue("Toutes");
        loadAllGroups();
    }

    @FXML
    private void onGoToProfile() {
        try {
            User currentUser = session.getCurrentUser();
            if (currentUser == null) {
                showError("No logged-in user found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/ProfileManager/UserDashboardView.fxml"));
            Parent dashboard = loader.load();

            UserDashboardController controller = loader.getController();
            controller.setCurrentUserId(currentUser.getId());

            Stage stage = (Stage) groupsTable.getScene().getWindow();
            stage.setScene(new Scene(dashboard));
            stage.setTitle("SyncStudy - User Dashboard");
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.centerOnScreen();

        } catch (IOException e) {
            showError("Error loading user dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    private void openGroupDetails(Group group) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/GroupManager/GroupDetails.fxml"));
            Parent root = loader.load();
            
            // Get the GroupDetailsFrame controller from the FXML
            GroupDetailsFrame detailsController = loader.getController();
            detailsController.setGroup(group); // Set the group to display
            
            Stage detailStage = new Stage();
            detailStage.setTitle("Détails: " + group.getName());
            detailStage.setScene(new Scene(root, 900, 700));
            detailStage.initModality(Modality.APPLICATION_MODAL);
            detailStage.showAndWait();
            
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture des détails: " + e.getMessage());
        }
    }

    private void openChatForGroup(Group selectedGroup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/chat.fxml"));
            Parent root = loader.load();
            Object ctrl = loader.getController();
            if (!(ctrl instanceof ChatController)) {
                showError("Chat controller not found.");
                return;
            }
            ChatController chatController = (ChatController) ctrl;

            SessionFacade sf = this.session != null ? this.session : SessionFacade.getInstance();
            User currentUser = sf.getCurrentUser();
            if (currentUser == null) {
                showError("No logged-in user found.");
                return;
            }
            chatController.setCurrentUser(currentUser.getId(), currentUser.isAdmin());
            chatController.startRealtime(AppConfig.getChatHost(), AppConfig.getChatPort());
            chatController.setCurrentGroup(selectedGroup.getGroupId());

            Stage stage = (Stage) groupsTable.getScene().getWindow();
            stage.setTitle("SyncStudy - Chat (Group " + selectedGroup.getName() + ")");
            stage.setScene(new Scene(root, 800, 600));
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Failed to open chat: " + e.getMessage());
        } catch (Exception e) {
            showError("Error opening chat: " + e.getMessage());
        }
    }


    // Getter methods for shared access
    public SessionFacade getSession() {
        return session;
    }
    
    public GroupFacade getGroupFacade() {
        return group;
    }
    
    /**
     * Method to set a group for details view
     */
    public void setGroup(Group selectedGroup) {
        // This method will be used when the same controller
        // is used for the details view
        // For now, we can show group info in the label
        if (selectedGroup != null && label != null) {
            label.setText("Groupe sélectionné: " + selectedGroup.getName());
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
