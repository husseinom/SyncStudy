package com.syncstudy.UI.FileManager;

import com.syncstudy.BL.FileManager.SharedFile;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class FileDetailsController {

    @FXML private Label fileNameLabel;
    @FXML private Label fileTypeLabel;
    @FXML private Label fileSizeLabel;
    @FXML private Label uploaderLabel;
    @FXML private Label uploadDateLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Button closeButton;

    private SharedFile currentFile;

    @FXML
    public void initialize() {
        if (descriptionArea != null) {
            descriptionArea.setEditable(false);
        }
    }

    public void setFile(SharedFile file) {
        this.currentFile = file;
        displayFileInfo();
    }

    private void displayFileInfo() {
        if (currentFile == null) return;

        fileNameLabel.setText(currentFile.getOriginalFileName());
        fileTypeLabel.setText(currentFile.getFileType() != null ? currentFile.getFileType().toUpperCase() : "Unknown");
        fileSizeLabel.setText(currentFile.getFormattedFileSize());

        String uploader = currentFile.getUploaderFullName() != null ?
                currentFile.getUploaderFullName() :
                currentFile.getUploaderUsername();
        uploaderLabel.setText(uploader != null ? uploader : "Unknown");

        if (currentFile.getUploadTimestamp() != null) {
            String date = currentFile.getUploadTimestamp().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            uploadDateLabel.setText(date);
        }

        String desc = currentFile.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            descriptionArea.setText(desc);
        } else {
            descriptionArea.setText("No description provided.");
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}

