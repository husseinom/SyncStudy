package com.syncstudy.UI.GroupMembership;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class GroupMembershipFrame {
    
    private Stage stage;
    private GroupMembershipController controller;
    
    public GroupMembershipFrame() {
        this.stage = new Stage();
    }
    
    public void show(Long groupId, String groupName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/syncstudy/UI/GroupMembership/GroupMembership.fxml")
            );
            Parent root = loader.load();
            
            controller = loader.getController();
            controller.setGroupId(groupId, groupName);
            
            stage.setTitle("Gestion des membres - " + groupName);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void close() {
        if (stage != null) {
            stage.close();
        }
    }
}
