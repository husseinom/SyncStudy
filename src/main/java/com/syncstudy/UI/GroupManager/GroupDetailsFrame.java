package com.syncstudy.UI.GroupManager;

import com.syncstudy.BL.GroupManager.Group;
import com.syncstudy.BL.GroupManager.Category;
import com.syncstudy.BL.GroupMembership.GroupMembershipFacade;
import com.syncstudy.BL.SessionManager.SessionFacade;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

/**
 * GroupDetailsFrame - Boundary class that uses GroupController (from diagram)
 */
public class GroupDetailsFrame {
    private GroupController controller;
    private SessionFacade session;
    private GroupMembershipFacade membership;
    
    @FXML private Label groupNameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label memberCountLabel;
    @FXML private Label createdAtLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label membershipStatusLabel;
    @FXML private Button joinButton;
    @FXML private Button leaveButton;
    @FXML private Button closeButton;
    
    private Group currentGroup;

    /**
     * Initialize the details view
     */
    @FXML
    public void initialize() {
        session = SessionFacade.getInstance();
        membership = GroupMembershipFacade.getInstance();
        
        if (descriptionArea != null) {
            descriptionArea.setEditable(false);
        }
    }
    
    // Method to set the main controller (respecting diagram relationship)
    public void setGroupController(GroupController controller) {
        this.controller = controller;
    }
    
    public void setGroup(Group group) {
        this.currentGroup = group;
        displayGroupInfo();
    }
    
    private void displayGroupInfo() {
        if (currentGroup == null) return;
        
        groupNameLabel.setText(currentGroup.getName());
        
        Category cat = currentGroup.getCategory();
        if (cat != null) {
            categoryLabel.setText("Catégorie: " + cat.getName());
        } else {
            categoryLabel.setText("Catégorie: Aucune");
        }
        
        memberCountLabel.setText("Membres: " + currentGroup.getMemberCount());
        
        if (currentGroup.getCreatedAt() != null) {
            String date = currentGroup.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            createdAtLabel.setText("Créé le: " + date);
        }
        
        String desc = currentGroup.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            descriptionArea.setText(desc);
        } else {
            descriptionArea.setText("Aucune description disponible.");
        }
        
        updateMembershipStatus();
    }
    
    private void updateMembershipStatus() {
        if (session.getCurrentUser() == null) return;
        
        Long userId = session.getCurrentUser().getId();
        String status = membership.getUserMembershipStatus(userId, currentGroup.getGroupId());
        
        switch (status) {
            case "Member":
            case "Group Admin":
            case "Administrator":
                membershipStatusLabel.setText("Vous êtes membre");
                membershipStatusLabel.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5;");
                joinButton.setVisible(false);
                leaveButton.setVisible(true);
                break;
            case "Pending":
                membershipStatusLabel.setText("Demande en attente");
                membershipStatusLabel.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5;");
                joinButton.setVisible(false);
                leaveButton.setVisible(false);
                break;
            case "Banned":
                membershipStatusLabel.setText("Vous êtes banni");
                membershipStatusLabel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5;");
                joinButton.setVisible(false);
                leaveButton.setVisible(false);
                break;
            default:
                membershipStatusLabel.setText("");
                joinButton.setVisible(true);
                leaveButton.setVisible(false);
        }
    }
    
    @FXML
    private void onRequestJoin() {
        if (currentGroup == null || session.getCurrentUser() == null) return;
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rejoindre le groupe");
        dialog.setHeaderText("Demande d'adhésion à " + currentGroup.getName());
        dialog.setContentText("Message (optionnel):");
        
        dialog.showAndWait().ifPresent(message -> {
            try {
                Long userId = session.getCurrentUser().getId();
                membership.requestToJoinGroup(userId, currentGroup.getGroupId(), message);
                showInfo("Demande envoyée! L'admin examinera votre demande.");
                updateMembershipStatus();
            } catch (Exception e) {
                showError("Erreur: " + e.getMessage());
            }
        });
    }
    
    @FXML
    private void onLeaveGroup() {
        if (currentGroup == null || session.getCurrentUser() == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Quitter le groupe");
        confirm.setHeaderText("Quitter " + currentGroup.getName() + "?");
        confirm.setContentText("Vous perdrez l'accès à tout le contenu du groupe.\n\n" +
            "• Vous serez retiré des sessions d'étude\n" +
            "• Vos messages resteront dans le groupe\n" +
            "• Vos fichiers resteront dans le groupe");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                Long userId = session.getCurrentUser().getId();
                membership.leaveGroup(userId, currentGroup.getGroupId());
                showInfo("Vous avez quitté " + currentGroup.getName());
                closeWindow();
            } catch (Exception e) {
                showError("Erreur: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void onClose() {
        closeWindow();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
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
