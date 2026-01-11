package com.syncstudy.UI.FileManager;

import com.syncstudy.BL.FileManager.FileFacade;
import com.syncstudy.BL.FileManager.SharedFile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FileListController {

    @FXML private Label groupNameLabel;
    @FXML private TextField searchBar;
    @FXML private TableView<SharedFile> filesTable;
    @FXML private TableColumn<SharedFile, String> nameColumn;
    @FXML private TableColumn<SharedFile, String> typeColumn;
    @FXML private TableColumn<SharedFile, String> sizeColumn;
    @FXML private TableColumn<SharedFile, String> uploaderColumn;
    @FXML private TableColumn<SharedFile, String> dateColumn;

    private FileFacade FileFacade;
    private Long currentGroupId;
    private Long currentUserId;
    private boolean isAdmin;
    private ObservableList<SharedFile> allFiles;

    @FXML
    public void initialize() {
        FileFacade = FileFacade.getInstance();
        allFiles = FXCollections.observableArrayList();

        // Configure table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("originalFileName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("fileType"));
        sizeColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFormattedFileSize()));
        uploaderColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getUploaderFullName() != null ? 
                cellData.getValue().getUploaderFullName() : 
                cellData.getValue().getUploaderUsername()));
        dateColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getUploadTimestamp().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

        filesTable.setItems(allFiles);
    }

    public void setGroupData(Long groupId, String groupName, Long userId, boolean isAdmin) {
        this.currentGroupId = groupId;
        this.currentUserId = userId;
        this.isAdmin = isAdmin;
        groupNameLabel.setText(groupName + " - Files");
        loadFiles();
    }

    private void loadFiles() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return FileFacade.getFilesByGroupId(currentGroupId);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error loading files: " + e.getMessage()));
                return List.<SharedFile>of();
            }
        }).thenAcceptAsync(files -> {
            allFiles.clear();
            allFiles.addAll(files);
        }, Platform::runLater);
    }

    @FXML
    private void onSearch() {
        String query = searchBar.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            loadFiles();
            return;
        }

        List<SharedFile> filtered = allFiles.stream()
                .filter(file -> file.getOriginalFileName().toLowerCase().contains(query))
                .collect(Collectors.toList());

        filesTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void onClear() {
        searchBar.clear();
        filesTable.setItems(allFiles);
    }

    @FXML
    private void onRefresh() {
        loadFiles();
    }

    @FXML
    private void onDownload() {
        SharedFile selected = filesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a file to download");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName(selected.getOriginalFileName());

        File saveLocation = fileChooser.showSaveDialog(filesTable.getScene().getWindow());
        if (saveLocation == null) return;

        CompletableFuture.supplyAsync(() -> {
            try {
                File sourceFile = FileFacade.downloadFile(selected.getId(), currentUserId);
                Files.copy(sourceFile.toPath(), saveLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (Exception e) {
                Platform.runLater(() -> showError("Download failed: " + e.getMessage()));
                return false;
            }
        }).thenAcceptAsync(success -> {
            if (success) {
                showSuccess("File downloaded successfully");
            }
        }, Platform::runLater);
    }

    @FXML
    private void onViewDetails() {
        SharedFile selected = filesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a file to view details");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/FileManager/FileDetails.fxml"));
            Parent root = loader.load();

            FileDetailsController controller = loader.getController();
            controller.setFile(selected);

            Stage stage = new Stage();
            stage.setTitle("File Details");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            showError("Error opening file details: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        SharedFile selected = filesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a file to delete");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete File");
        alert.setHeaderText("Delete " + selected.getOriginalFileName() + "?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = FileFacade.deleteFile(selected.getId(), currentUserId, isAdmin);
                    if (deleted) {
                        showSuccess("File deleted successfully");
                        loadFiles();
                    }
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) filesTable.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}