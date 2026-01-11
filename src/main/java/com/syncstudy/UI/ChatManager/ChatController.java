package com.syncstudy.UI.ChatManager;

import com.syncstudy.BL.ChatManager.ChatFacade;
import com.syncstudy.BL.ChatManager.Message;
import com.syncstudy.BL.FileManager.FileFacade;
import com.syncstudy.BL.FileManager.SharedFile;
import com.syncstudy.UI.FileManager.FileDetailsController;
import com.syncstudy.UI.FileManager.FileListController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ChatController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox messageContainer;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Button uploadFileButton;
    @FXML private Label errorLabel;
    @FXML private Label groupNameLabel;
    @FXML private Button backButton;
    @FXML private Button viewFilesButton;

    private ChatFacade messageService;
    private FileFacade fileService;
    private Long currentUserId;
    private Long currentGroupId;
    private boolean isAdmin;
    private TcpChatClient tcpClient;
    private Set<Long> displayedFileIds = new HashSet<>();

    public void initialize() {
        messageService = ChatFacade.getInstance();
        fileService = FileFacade.getInstance();
        errorLabel.setVisible(false);

        messageContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollPane.setVvalue(1.0);
        });

        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() == 0.0) {
                loadOlderMessages();
            }
        });
    }

    @FXML
    private void handleBackToGroups(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/GroupManager/GroupManager.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                stage.setScene(new javafx.scene.Scene(root));
            }
        } catch (IOException e) {
            showError("Cannot open groups: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewFiles() {
        if (currentGroupId == null) {
            showError("No group selected");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/FileManager/FileListView.fxml"));
            Parent root = loader.load();

            FileListController controller = loader.getController();
            String groupName = messageService.getGroupName(currentGroupId);
            controller.setGroupData(currentGroupId, groupName, currentUserId, isAdmin);

            Stage stage = new Stage();
            stage.setTitle("Group Files - " + groupName);
            stage.setScene(new javafx.scene.Scene(root, 900, 600));
            stage.show();
        } catch (Exception e) {
            showError("Error opening files view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUploadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File file = fileChooser.showOpenDialog(uploadFileButton.getScene().getWindow());

        if (file == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("File Description");
        dialog.setHeaderText("Upload " + file.getName());
        dialog.setContentText("Description (optional):");

        dialog.showAndWait().ifPresent(description -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return fileService.uploadFile(currentGroupId, currentUserId, file, description);
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Upload failed: " + e.getMessage()));
                    return null;
                }
            }).thenAcceptAsync(uploadedFile -> {
                if (uploadedFile != null) {
                    showSuccess("File uploaded successfully");
                    displayFileMessage(uploadedFile);

                    if (tcpClient != null) {
                        TcpChatClient.EventEnvelope envelope = new TcpChatClient.EventEnvelope();
                        envelope.type = "file";
                        envelope.data = uploadedFile;
                        tcpClient.sendEvent(envelope);
                    }
                }
            }, Platform::runLater);
        });
    }

    public void setCurrentUser(Long userId, boolean isAdmin) {
        this.currentUserId = userId;
        this.isAdmin = isAdmin;
    }

    public void setCurrentGroup(Long groupId) {
        this.currentGroupId = groupId;
        groupNameLabel.setText(getCurrentGroupName());
        displayedFileIds.clear();
        loadMessages();
        initializeRealtimeConnection();
    }

    public Long getCurrentGroupId() {
        return this.currentGroupId;
    }

    public String getCurrentGroupName() {
        try {
            return messageService.getGroupName(currentGroupId);
        } catch (Exception e) {
            return "Unknown Group";
        }
    }

    private void loadMessages() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return messageService.getMessages(currentGroupId);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error loading messages: " + e.getMessage()));
                return List.<Message>of();
            }
        }).thenAcceptAsync(messages -> {
            messageContainer.getChildren().clear();
            displayedFileIds.clear();

            if (messages.isEmpty()) {
                showEmptyState();
            } else {
                for (Message message : messages) {
                    displayMessage(message);
                }
            }

            loadGroupFiles();
            scrollToBottom();
        }, Platform::runLater);
    }

    private void loadGroupFiles() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return fileService.getFilesByGroupId(currentGroupId);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error loading files: " + e.getMessage()));
                return List.<SharedFile>of();
            }
        }).thenAcceptAsync(files -> {
            for (SharedFile file : files) {
                if (!displayedFileIds.contains(file.getId())) {
                    displayFileMessage(file);
                }
            }
            scrollToBottom();
        }, Platform::runLater);
    }

    private void loadOlderMessages() {
        // Implementation for infinite scroll
    }

    @FXML
    private void handleSendMessage() {
        String content = messageInput.getText();

        try {
            Message message = messageService.sendMessage(currentUserId, currentGroupId, content);

            if (message != null) {
                if (!messageContainer.getChildren().isEmpty()) {
                    if (messageContainer.getChildren().get(0) instanceof VBox) {
                        VBox firstChild = (VBox) messageContainer.getChildren().get(0);
                        if (!firstChild.getChildren().isEmpty()) {
                            if (firstChild.getChildren().get(0) instanceof Label) {
                                Label possibleEmptyLabel = (Label) firstChild.getChildren().get(0);
                                if ("No messages yet".equals(possibleEmptyLabel.getText())) {
                                    messageContainer.getChildren().clear();
                                }
                            }
                        }
                    }
                }

                if (tcpClient != null) {
                    TcpChatClient.EventEnvelope envelope = new TcpChatClient.EventEnvelope("new", message, null);
                    tcpClient.sendEvent(envelope);
                }
                messageInput.clear();
                errorLabel.setVisible(false);
            } else {
                showError("Failed to send message.");
            }
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void displayMessage(Message message) {
        VBox messageBox = createMessageBox(message);
        messageContainer.getChildren().add(messageBox);
    }

    private void displayFileMessage(SharedFile file) {
        if (displayedFileIds.contains(file.getId())) {
            return;
        }
        displayedFileIds.add(file.getId());

        VBox fileBox = createFileMessageBox(file);
        messageContainer.getChildren().add(fileBox);
        scrollToBottom();
    }

    private VBox createFileMessageBox(SharedFile file) {
        VBox messageBox = new VBox(5);
        messageBox.setMaxWidth(600);
        messageBox.setUserData("file_" + file.getId());

        boolean isOwnFile = file.getUploaderId().equals(currentUserId);

        HBox container = new HBox(10);
        container.setAlignment(isOwnFile ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if (!isOwnFile) {
            Circle avatar = new Circle(16);
            avatar.setFill(Color.LIGHTGREEN);
            container.getChildren().add(avatar);
        }

        VBox bubbleContainer = new VBox(3);

        // Sender name
        String uploaderName = file.getUploaderFullName() != null ?
                file.getUploaderFullName() :
                file.getUploaderUsername();
        Label senderName = new Label(uploaderName != null ? uploaderName : "Unknown");
        senderName.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        senderName.setAlignment(isOwnFile ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubbleContainer.getChildren().add(senderName);

        // Bubble with same styling as text messages
        HBox bubble = new HBox(10);
        bubble.setStyle(isOwnFile ?
                "-fx-background-color: #E3F2FD; -fx-background-radius: 10; -fx-padding: 10;" :
                "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 10; -fx-border-color: #E0E0E0; -fx-border-radius: 10;");

        // File info
        VBox fileInfo = new VBox(3);
        Label fileIcon = new Label("📎");
        fileIcon.setStyle("-fx-font-size: 20;");
        Label fileName = new Label(file.getOriginalFileName());
        fileName.setStyle("-fx-font-weight: bold;");
        Label fileSize = new Label(file.getFormattedFileSize());
        fileSize.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
        fileInfo.getChildren().addAll(fileIcon, fileName, fileSize);

        // Action buttons (View & Download)
        HBox buttonBox = new HBox(5);
        Button viewButton = new Button("View");
        viewButton.setStyle("-fx-font-size: 10; -fx-background-color: #2196F3; -fx-text-fill: white;");
        viewButton.setOnAction(e -> handleViewFileDetails(file));

        Button downloadButton = new Button("Download");
        downloadButton.setStyle("-fx-font-size: 10; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        downloadButton.setOnAction(e -> handleDownloadFile(file));

        buttonBox.getChildren().addAll(viewButton, downloadButton);

        bubble.getChildren().addAll(fileInfo, buttonBox);

        // Context menu (right-click)
        ContextMenu contextMenu = createFileContextMenu(file);
        bubble.setOnContextMenuRequested(e -> {
            contextMenu.show(bubble, e.getScreenX(), e.getScreenY());
        });

        bubbleContainer.getChildren().add(bubble);

        // Timestamp
        HBox timestampBox = new HBox(5);
        String timeText = file.getUploadTimestamp() != null ?
                file.getUploadTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        Label timestamp = new Label(timeText);
        timestamp.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
        timestampBox.getChildren().add(timestamp);

        bubbleContainer.getChildren().add(timestampBox);

        container.getChildren().add(bubbleContainer);
        messageBox.getChildren().add(container);

        return messageBox;
    }

    private ContextMenu createFileContextMenu(SharedFile file) {
        ContextMenu contextMenu = new ContextMenu();

        boolean isOwner = file.getUploaderId().equals(currentUserId);

        if (isOwner || isAdmin) {
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> handleDeleteFile(file));
            contextMenu.getItems().add(deleteItem);
        }

        return contextMenu;
    }



    private void handleViewFileDetails(SharedFile file) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/FileManager/FileDetails.fxml"));
            Parent root = loader.load();

            FileDetailsController controller = loader.getController();
            controller.setFile(file);

            Stage stage = new Stage();
            stage.setTitle("File Details - " + file.getOriginalFileName());
            stage.setScene(new javafx.scene.Scene(root, 500, 400));
            stage.show();
        } catch (Exception e) {
            showError("Error opening file details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isFileDisplayed(Long fileId) {
        return displayedFileIds.contains(fileId);
    }

    private VBox createMessageBox(Message message) {
        VBox messageBox = new VBox(5);
        messageBox.setMaxWidth(600);

        if (message == null) {
            Label error = new Label("[invalid message]");
            messageBox.getChildren().add(error);
            return messageBox;
        }

        Long senderIdObj = message.getSenderId();
        boolean isOwnMessage = senderIdObj != null && senderIdObj.equals(currentUserId);

        HBox container = new HBox(10);
        container.setAlignment(isOwnMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if (!isOwnMessage) {
            Circle avatar = new Circle(16);
            avatar.setFill(Color.LIGHTBLUE);
            container.getChildren().add(avatar);
        }

        VBox bubbleContainer = new VBox(3);

        String senderFullName = message.getSenderFullName();
        String senderUsername = message.getSenderUsername();
        String senderDisplay = (senderFullName != null && !senderFullName.isEmpty()) ? senderFullName : senderUsername;

        Label senderName = new Label(senderDisplay != null ? senderDisplay : "");
        senderName.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        senderName.setAlignment(isOwnMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        bubbleContainer.getChildren().add(senderName);

        HBox bubble = new HBox();
        bubble.setStyle(isOwnMessage ?
                "-fx-background-color: #E3F2FD; -fx-background-radius: 10; -fx-padding: 10;" :
                "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 10; -fx-border-color: #E0E0E0; -fx-border-radius: 10;");

        Text messageText = new Text(message.getContent() != null ? message.getContent() : "");
        messageText.setWrappingWidth(400);
        bubble.getChildren().add(messageText);

        bubbleContainer.getChildren().add(bubble);

        HBox timestampBox = new HBox(5);
        String timeText = message.getCreatedAt() != null ? message.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        Label timestamp = new Label(timeText);
        timestamp.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
        timestampBox.getChildren().add(timestamp);

        if (message.isEdited()) {
            Label editedLabel = new Label("Edited");
            editedLabel.setStyle("-fx-font-size: 10; -fx-text-fill: gray; -fx-font-style: italic;");
            timestampBox.getChildren().add(editedLabel);
        }

        bubbleContainer.getChildren().add(timestampBox);

        ContextMenu contextMenu = createContextMenu(message);
        bubble.setOnContextMenuRequested(e -> {
            contextMenu.show(bubble, e.getScreenX(), e.getScreenY());
        });

        container.getChildren().add(bubbleContainer);
        messageBox.getChildren().add(container);

        return messageBox;
    }

    private ContextMenu createContextMenu(Message message) {
        ContextMenu contextMenu = new ContextMenu();

        Long senderIdObj = message.getSenderId();
        boolean isOwner = senderIdObj != null && senderIdObj.equals(currentUserId);

        if (isOwner) {
            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction(e -> handleEditMessage(message));
            contextMenu.getItems().add(editItem);
        }

        if (isOwner || isAdmin) {
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> handleDeleteMessage(message));
            contextMenu.getItems().add(deleteItem);
        }

        return contextMenu;
    }

    private void handleEditMessage(Message message) {
        TextInputDialog dialog = new TextInputDialog(message.getContent());
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Edit your message");
        dialog.setContentText("Message:");

        dialog.showAndWait().ifPresent(newContent -> {
            try {
                messageService.editMessage(message.getId(), currentUserId, newContent);
                loadMessages();
                if (tcpClient != null) {
                    TcpChatClient.EventEnvelope envelope = new TcpChatClient.EventEnvelope("edit", null, message.getId());
                    tcpClient.sendEvent(envelope);
                }
                showSuccess("Message updated.");
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
    }

    private void handleDeleteMessage(Message message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Message");
        alert.setHeaderText("Delete this message?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    messageService.deleteMessage(message.getId(), currentUserId, isAdmin);
                    loadMessages();
                    if (tcpClient != null) {
                        TcpChatClient.EventEnvelope envelope = new TcpChatClient.EventEnvelope("delete", null, message.getId());
                        tcpClient.sendEvent(envelope);
                    }
                    showSuccess("Message deleted.");
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }
        });
    }

    // methods for shared files
    private void handleDownloadFile(SharedFile file) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName(file.getOriginalFileName());

        File saveLocation = fileChooser.showSaveDialog(uploadFileButton.getScene().getWindow());
        if (saveLocation == null) return;

        CompletableFuture.supplyAsync(() -> {
            try {
                File sourceFile = fileService.downloadFile(file.getId(), currentUserId);
                java.nio.file.Files.copy(sourceFile.toPath(), saveLocation.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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

    private void handleDeleteFile(SharedFile file) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete File");
        alert.setHeaderText("Delete " + file.getOriginalFileName() + "?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = fileService.deleteFile(file.getId(), currentUserId, isAdmin);
                    if (deleted) {
                        showSuccess("File deleted successfully");
                        // Remove from UI
                        messageContainer.getChildren().removeIf(node ->
                                ("file_" + file.getId()).equals(node.getUserData()));

                        // Notify other clients
                        if (tcpClient != null) {
                            TcpChatClient.EventEnvelope env = new TcpChatClient.EventEnvelope();
                            env.type = "file-delete";
                            env.id = file.getId();
                            tcpClient.sendEvent(env);
                        }
                    }
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }
        });
    }


    private void showEmptyState() {
        VBox emptyState = new VBox(10);
        emptyState.setAlignment(Pos.CENTER);
        Label noMessages = new Label("No messages yet");
        noMessages.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        Label startConversation = new Label("Start the conversation!");
        startConversation.setStyle("-fx-text-fill: gray;");
        emptyState.getChildren().addAll(noMessages, startConversation);
        messageContainer.getChildren().add(emptyState);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        System.out.println(message);
    }

    public void startRealtime(String host, int port) {
        if (tcpClient != null) return;
        tcpClient = new TcpChatClient(host, port);
        try {
            tcpClient.connect(this);
        } catch (Exception e) {
            showError("Realtime connect failed: " + e.getMessage());
        }
    }

    private void initializeRealtimeConnection() {
        if (tcpClient == null) {
            String host = com.syncstudy.WS.AppConfig.getChatHost();
            int port = com.syncstudy.WS.AppConfig.getChatPort();
            System.out.println("Connecting to chat server: " + host + ":" + port);
            startRealtime(host, port);
        }
    }

    public void stopRealtime() {
        if (tcpClient != null) {
            tcpClient.disconnect();
            tcpClient = null;
        }
    }

    public void handleRemoteEnvelope(TcpChatClient.EventEnvelope env) {
        if (env == null) return;
        switch (env.type) {
            case "new":
                if (env.message != null) {
                    displayMessage(env.message);
                }
                break;
            case "file":
                if (env.data instanceof SharedFile) {
                    SharedFile file = (SharedFile) env.data;
                    displayFileMessage(file);
                }
                break;
            case "file-delete":
                if (env.id != null) {
                    messageContainer.getChildren().removeIf(node ->
                            ("file_" + env.id).equals(node.getUserData()));
                }
                break;
            case "edit":
            case "delete":
                loadMessages();
                break;
        }
    }
}
