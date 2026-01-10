package com.syncstudy.UI.GroupMembership;

import com.syncstudy.BL.SessionManager.SessionFacade;
import com.syncstudy.BL.GroupMembership.GroupMembershipFacade;
import com.syncstudy.BL.GroupMembership.GroupMember;
import com.syncstudy.BL.GroupMembership.JoinRequest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

public class GroupMembershipController {
    
    private SessionFacade session;
    private GroupMembershipFacade membership;
    private Long currentGroupId;
    
    @FXML private Label groupNameLabel;
    @FXML private TabPane tabPane;
    
    // Onglet Membres
    @FXML private TableView<GroupMember> membersTable;
    @FXML private TableColumn<GroupMember, Long> memberIdColumn;
    @FXML private TableColumn<GroupMember, Long> memberUserIdColumn;
    @FXML private TableColumn<GroupMember, String> memberRoleColumn;
    
    // Onglet Demandes
    @FXML private TableView<JoinRequest> requestsTable;
    @FXML private TableColumn<JoinRequest, Long> requestIdColumn;
    @FXML private TableColumn<JoinRequest, Long> requestUserIdColumn;
    @FXML private TableColumn<JoinRequest, String> requestMessageColumn;
    @FXML private TableColumn<JoinRequest, String> requestStatusColumn;
    
    // Onglet Bannis
    @FXML private TableView<GroupMember> bannedTable;
    @FXML private TableColumn<GroupMember, Long> bannedUserIdColumn;
    @FXML private TableColumn<GroupMember, String> bannedReasonColumn;
    
    private ObservableList<GroupMember> membersList;
    private ObservableList<JoinRequest> requestsList;
    private ObservableList<GroupMember> bannedList;

    @FXML
    public void initialize() {
        this.session = SessionFacade.getInstance();
        this.membership = GroupMembershipFacade.getInstance();
        
        setupTables();
    }
    
    private void setupTables() {
        // Table des membres
        membersList = FXCollections.observableArrayList();
        membersTable.setItems(membersList);
        memberIdColumn.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        memberUserIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        memberRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        
        // Table des demandes
        requestsList = FXCollections.observableArrayList();
        requestsTable.setItems(requestsList);
        requestIdColumn.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        requestUserIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        requestMessageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        requestStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Table des bannis
        bannedList = FXCollections.observableArrayList();
        bannedTable.setItems(bannedList);
        bannedUserIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        bannedReasonColumn.setCellValueFactory(new PropertyValueFactory<>("banReason"));
    }
    
    public void setGroupId(Long groupId, String groupName) {
        this.currentGroupId = groupId;
        if (groupNameLabel != null) {
            groupNameLabel.setText("Gestion: " + groupName);
        }
        loadData();
    }
    
    private void loadData() {
        loadMembers();
        loadPendingRequests();
        loadBannedMembers();
    }
    
    private void loadMembers() {
        try {
            System.out.println("Loading members for group ID: " + currentGroupId);
            List<GroupMember> members = membership.getGroupMembers(currentGroupId);
            System.out.println("Found " + members.size() + " members");
            membersList.clear();
            membersList.addAll(members);
        } catch (Exception e) {
            System.err.println("Error loading members: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur chargement membres: " + e.getMessage());
        }
    }
    
    private void loadPendingRequests() {
        try {
            Long userId = session.getCurrentUser().getId();
            List<JoinRequest> requests = membership.getPendingRequests(currentGroupId, userId);
            requestsList.clear();
            requestsList.addAll(requests);
        } catch (Exception e) {
            showError("Erreur chargement demandes: " + e.getMessage());
        }
    }
    
    private void loadBannedMembers() {
        try {
            Long userId = session.getCurrentUser().getId();
            List<GroupMember> banned = membership.getBannedMembers(currentGroupId, userId);
            bannedList.clear();
            bannedList.addAll(banned);
        } catch (Exception e) {
            showError("Erreur chargement bannis: " + e.getMessage());
        }
    }

    // Demander à rejoindre un groupe
    @FXML
    public void requestJoin() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Demande d'adhésion");
        dialog.setHeaderText("Rejoindre le groupe");
        dialog.setContentText("Message (optionnel):");
        
        dialog.showAndWait().ifPresent(message -> {
            try {
                Long userId = session.getCurrentUser().getId();
                membership.requestToJoinGroup(userId, currentGroupId, message);
                showInfo("Demande envoyée avec succès!");
            } catch (Exception e) {
                showError("Erreur: " + e.getMessage());
            }
        });
    }
    
    // Approuver une demande
    @FXML
    public void approveRequest() {
        JoinRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez une demande.");
            return;
        }
        
        try {
            Long userId = session.getCurrentUser().getId();
            membership.approveJoinRequest(selected.getRequestId(), userId, currentGroupId);
            showInfo("Demande approuvée!");
            loadData();
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
        }
    }
    
    // Rejeter une demande
    @FXML
    public void rejectRequest() {
        JoinRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez une demande.");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rejeter la demande");
        dialog.setContentText("Raison du rejet:");
        
        dialog.showAndWait().ifPresent(reason -> {
            try {
                Long userId = session.getCurrentUser().getId();
                membership.rejectJoinRequest(selected.getRequestId(), reason, userId, currentGroupId);
                showInfo("Demande rejetée.");
                loadData();
            } catch (Exception e) {
                showError("Erreur: " + e.getMessage());
            }
        });
    }
    
    // Quitter le groupe
    @FXML
    public void leaveGroup() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Quitter le groupe");
        confirm.setContentText("Voulez-vous vraiment quitter ce groupe?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                Long userId = session.getCurrentUser().getId();
                membership.leaveGroup(userId, currentGroupId);
                showInfo("Vous avez quitté le groupe.");
                closeWindow();
            } catch (Exception e) {
                showError("Erreur: " + e.getMessage());
            }
        }
    }
    
    // Bannir un membre
    @FXML
    public void banMember() {
        GroupMember selected = membersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez un membre.");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Bannir le membre");
        dialog.setContentText("Raison du bannissement:");
        
        dialog.showAndWait().ifPresent(reason -> {
            try {
                Long adminId = session.getCurrentUser().getId();
                membership.banMember(selected.getUserId(), currentGroupId, reason, adminId);
                showInfo("Membre banni.");
                loadData();
            } catch (Exception e) {
                showError("Erreur: " + e.getMessage());
            }
        });
    }
    
    // Débannir un membre
    @FXML
    public void unbanMember() {
        GroupMember selected = bannedTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez un membre banni.");
            return;
        }
        
        try {
            Long adminId = session.getCurrentUser().getId();
            membership.unbanMember(selected.getUserId(), currentGroupId, adminId);
            showInfo("Membre débanni.");
            loadData();
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
        }
    }
    
    // Assigner un rôle
    @FXML
    public void assignRole() {
        GroupMember selected = membersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez un membre.");
            return;
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Member", "Member", "Group Admin");
        dialog.setTitle("Assigner un rôle");
        dialog.setContentText("Nouveau rôle:");
        
        dialog.showAndWait().ifPresent(role -> {
            try {
                Long adminId = session.getCurrentUser().getId();
                membership.assignRole(selected.getUserId(), currentGroupId, role, adminId);
                showInfo("Rôle assigné.");
                loadData();
            } catch (Exception e) {
                showError("Erreur: " + e.getMessage());
            }
        });
    }
    
    @FXML
    public void onRefresh() {
        loadData();
    }
    
    @FXML
    public void onClose() {
        closeWindow();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) tabPane.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
