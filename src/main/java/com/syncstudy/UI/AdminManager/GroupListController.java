package com.syncstudy.UI.AdminManager;

import com.syncstudy.BL.GroupManager.Group;
import com.syncstudy.BL.GroupManager.GroupFacade;
import com.syncstudy.BL.GroupManager.Category;
import com.syncstudy.UI.GroupMembership.GroupMembershipFrame;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class GroupListController {

    @FXML private TableView<Group> groupsTable;
    @FXML private TableColumn<Group, Long> idColumn;
    @FXML private TableColumn<Group, String> nameColumn;
    @FXML private TableColumn<Group, String> categoryColumn;
    @FXML private TableColumn<Group, Integer> memberCountColumn;
    @FXML private TableColumn<Group, String> createdAtColumn;
    @FXML private TableColumn<Group, Void> actionsColumn;
    @FXML private TextField searchField;
    @FXML private Label infoLabel;

    private GroupFacade groupFacade;
    private ObservableList<Group> groupsList;

    @FXML
    public void initialize() {
        groupFacade = GroupFacade.getInstance();
        groupsList = FXCollections.observableArrayList();
        
        setupTable();
        loadGroups();
    }

    private void setupTable() {
        groupsTable.setItems(groupsList);
        
        idColumn.setCellValueFactory(new PropertyValueFactory<>("groupId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        memberCountColumn.setCellValueFactory(new PropertyValueFactory<>("memberCount"));
        
        categoryColumn.setCellValueFactory(cellData -> {
            Category cat = cellData.getValue().getCategory();
            return new SimpleStringProperty(cat != null ? cat.getName() : "-");
        });
        
        createdAtColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                String date = cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                return new SimpleStringProperty(date);
            }
            return new SimpleStringProperty("-");
        });
        
        setupActionsColumn();
        
        // Double-click to open membership management
        groupsTable.setRowFactory(tv -> {
            TableRow<Group> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openMembershipManagement(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button manageBtn = new Button("Gérer Membres");
            private final Button viewBtn = new Button("Voir");
            private final HBox box = new HBox(5, manageBtn, viewBtn);
            
            {
                manageBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11px;");
                viewBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 11px;");
                
                manageBtn.setOnAction(e -> {
                    Group group = getTableView().getItems().get(getIndex());
                    openMembershipManagement(group);
                });
                
                viewBtn.setOnAction(e -> {
                    Group group = getTableView().getItems().get(getIndex());
                    showGroupInfo(group);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadGroups() {
        try {
            List<Group> groups = groupFacade.filter(Optional.empty(), Optional.empty(), Optional.empty());
            groupsList.clear();
            groupsList.addAll(groups);
            infoLabel.setText("Total: " + groups.size() + " groupes");
        } catch (Exception e) {
            showError("Erreur chargement: " + e.getMessage());
        }
    }

    @FXML
    public void onSearch() {
        String search = searchField.getText();
        if (search != null && !search.trim().isEmpty()) {
            try {
                List<Group> results = groupFacade.search(List.of(search.trim()));
                groupsList.clear();
                groupsList.addAll(results);
                infoLabel.setText("Résultats: " + results.size() + " groupes");
            } catch (Exception e) {
                showError("Erreur recherche: " + e.getMessage());
            }
        } else {
            loadGroups();
        }
    }

    @FXML
    public void onRefresh() {
        searchField.clear();
        loadGroups();
    }

    private void openMembershipManagement(Group group) {
        GroupMembershipFrame frame = new GroupMembershipFrame();
        frame.show(group.getGroupId(), group.getName());
    }

    private void showGroupInfo(Group group) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info Groupe");
        alert.setHeaderText(group.getName());
        
        String info = "ID: " + group.getGroupId() + "\n" +
                     "Catégorie: " + (group.getCategory() != null ? group.getCategory().getName() : "-") + "\n" +
                     "Membres: " + group.getMemberCount() + "\n" +
                     "Description: " + (group.getDescription() != null ? group.getDescription() : "-");
        
        alert.setContentText(info);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
