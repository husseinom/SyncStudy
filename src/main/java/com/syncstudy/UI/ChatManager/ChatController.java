package com.syncstudy.UI.ChatManager;

import com.syncstudy.BL.ChatManager.ChatFacade;
import com.syncstudy.BL.ChatManager.Message;
import com.syncstudy.BL.FileManager.FileFacade;
import com.syncstudy.BL.FileManager.SharedFile;
import com.syncstudy.BL.GroupMembership.GroupMembershipFacade;
import com.syncstudy.BL.ReportsManager.ReportsFacade;
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
import com.syncstudy.BL.StudySessionManager.*;
import com.syncstudy.UI.StudySessionManager.StudySessionListController;
import com.syncstudy.UI.StudySessionManager.CreateSessionController;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    @FXML private Button leaveGroupButton;
    @FXML
    private Button viewSessionsButton;
    @FXML
    private Button proposeSessionButton;

    private ChatFacade messageService;
    private FileFacade fileService;
    private GroupMembershipFacade membershipFacade;
    private StudySessionFacade studysessionFacade;
    private Set<Long> displayedSessionIds = new HashSet<>();
    private Long currentUserId;
    private Long currentGroupId;
    private boolean isAdmin;
    private TcpChatClient tcpClient;
    private Set<Long> displayedFileIds = new HashSet<>();

    public void initialize() {
        messageService = ChatFacade.getInstance();
        fileService = FileFacade.getInstance();
        membershipFacade = GroupMembershipFacade.getInstance();
        studysessionFacade = StudySessionFacade.getInstance();
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
    private void handleViewSessions() {
        if (currentGroupId == null) {
            showError("No group selected");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/StudySessionManager/SessionListView.fxml"));
            Parent root = loader.load();

            StudySessionListController controller = loader.getController();
            String groupName = messageService.getGroupName(currentGroupId);
            controller.setGroupData(currentGroupId, groupName, currentUserId, isAdmin);

            Stage stage = new Stage();
            stage.setTitle("Study Sessions - " + groupName);
            stage.setScene(new javafx.scene.Scene(root, 900, 600));
            stage.show();
        } catch (Exception e) {
            showError("Error opening sessions view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleProposeSession() {
        if (currentGroupId == null) {
            showError("No group selected");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syncstudy/UI/StudySessionManager/CreateSessionView.fxml"));
            Parent root = loader.load();

            CreateSessionController controller = loader.getController();
            controller.setGroupData(currentGroupId, currentUserId, this::onSessionCreated);
            Stage stage = new Stage();
            stage.setTitle("Propose Study Session");
            stage.setScene(new javafx.scene.Scene(root, 600, 700));
            stage.show();
        } catch (Exception e) {
            showError("Error opening create session dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onSessionCreated(StudySession session) {
        displaySessionMessage(session);

        if (tcpClient != null) {
            TcpChatClient.EventEnvelope envelope = new TcpChatClient.EventEnvelope();
            envelope.type = "session";
            envelope.data = session;
            tcpClient.sendEvent(envelope);
        }
    }

    private void loadGroupSessions() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return studysessionFacade.getGroupSessions(currentGroupId, "All");
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error loading sessions: " + e.getMessage()));
                return List.<StudySession>of();
            }
        }).thenAcceptAsync(sessions -> {
            for (StudySession session : sessions) {
                if (!displayedSessionIds.contains(session.getId())) {
                    displaySessionMessage(session);
                }
            }
            scrollToBottom();
        }, Platform::runLater);
    }

    private VBox createSessionMessageBox(StudySession session) {
        VBox messageBox = new VBox(5);
        messageBox.setMaxWidth(700);
        messageBox.setUserData("session_" + session.getId());

        boolean isCreator = session.getCreatorId().equals(currentUserId);

        HBox container = new HBox(10);
        container.setAlignment(isCreator ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if (!isCreator) {
            Circle avatar = new Circle(16);
            avatar.setFill(Color.LIGHTCORAL);
            container.getChildren().add(avatar);
        }
        VBox bubbleContainer = new VBox(5);

        // Creator name
        String creatorName = session.getCreatorFullName() != null ?
                session.getCreatorFullName() : session.getCreatorUsername();
        Label senderName = new Label(creatorName != null ? creatorName : "Unknown");
        senderName.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        senderName.setAlignment(isCreator ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubbleContainer.getChildren().add(senderName);

        // Bubble
        VBox bubble = new VBox(8);
        bubble.setStyle(isCreator ?
                "-fx-background-color: #FFE0B2; -fx-background-radius: 10; -fx-padding: 12;" :
                "-fx-background-color: #FFF3E0; -fx-background-radius: 10; -fx-padding: 12; -fx-border-color: #FFB74D; -fx-border-radius: 10; -fx-border-width: 2;");

        // Session header
        HBox headerBox = new HBox(10);
        Label sessionIcon = new Label("📅");
        sessionIcon.setStyle("-fx-font-size: 20;");
        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(session.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label statusBadge = new Label(session.getStatus().toString());
        statusBadge.setStyle(getStatusStyle(session.getStatus()));

        titleBox.getChildren().addAll(titleLabel, statusBadge);
        headerBox.getChildren().addAll(sessionIcon, titleBox);
        bubble.getChildren().add(headerBox);

        // Session details
        VBox detailsBox = new VBox(3);

        if (session.getDescription() != null && !session.getDescription().isEmpty()) {
            Label descLabel = new Label(session.getDescription());
            descLabel.setWrapText(true);
            descLabel.setStyle("-fx-font-size: 11; -fx-text-fill: gray;");
            detailsBox.getChildren().add(descLabel);
        }
        if (session.getStatus() == StudySession.SessionStatus.PROPOSED) {
            Label dateLabel = new Label("🗳️ Voting in progress");
            dateLabel.setStyle("-fx-font-size: 11;");
            detailsBox.getChildren().add(dateLabel);

            if (session.getVotingDeadline() != null) {
                Label deadlineLabel = new Label("Voting deadline: " +
                        session.getVotingDeadline().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
                deadlineLabel.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
                detailsBox.getChildren().add(deadlineLabel);
            }
        } else if (session.getSessionDate() != null) {
            Label dateLabel = new Label("📆 " +
                    session.getSessionDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            dateLabel.setStyle("-fx-font-size: 11;");
            detailsBox.getChildren().add(dateLabel);
        }

        Label locationLabel = new Label("📍 " + session.getLocation().getDisplayValue());
        locationLabel.setStyle("-fx-font-size: 11;");
        detailsBox.getChildren().add(locationLabel);
        Label participantsLabel = new Label("👥 " + session.getParticipantCount() +
                (session.getMaxParticipants() > 0 ? "/" + session.getMaxParticipants() : "") + " participants");
        participantsLabel.setStyle("-fx-font-size: 11;");
        detailsBox.getChildren().add(participantsLabel);

        bubble.getChildren().add(detailsBox);

        // Action buttons
        HBox actionBox = new HBox(5);

        if (session.getStatus() == StudySession.SessionStatus.PROPOSED) {
            Button voteButton = new Button("Vote");
            voteButton.setStyle("-fx-font-size: 10; -fx-background-color: #FF9800; -fx-text-fill: white;");
            voteButton.setOnAction(e -> handleVoteDialog(session));
            actionBox.getChildren().add(voteButton);

            Button proposeDateButton = new Button("Propose Date");
            proposeDateButton.setStyle("-fx-font-size: 10; -fx-background-color: #9C27B0; -fx-text-fill: white;");
            proposeDateButton.setOnAction(e -> handleProposeDate(session));
            actionBox.getChildren().add(proposeDateButton);
        }
        Button viewDetailsButton = new Button("View Details");
        viewDetailsButton.setStyle("-fx-font-size: 10; -fx-background-color: #2196F3; -fx-text-fill: white;");
        viewDetailsButton.setOnAction(e -> handleViewSessionDetails(session));
        actionBox.getChildren().add(viewDetailsButton);

        try {
            boolean isRegistered = studysessionFacade.isUserRegistered(session.getId(), currentUserId);
            if (isRegistered) {
                Button unregisterButton = new Button("Unregister");
                unregisterButton.setStyle("-fx-font-size: 10; -fx-background-color: #F44336; -fx-text-fill: white;");
                unregisterButton.setOnAction(e -> handleUnregisterSession(session));
                actionBox.getChildren().add(unregisterButton);
            } else if (studysessionFacade.canUserRegister(session.getId())) {
                Button registerButton = new Button("Register");
                registerButton.setStyle("-fx-font-size: 10; -fx-background-color: #4CAF50; -fx-text-fill: white;");
                registerButton.setOnAction(e -> handleRegisterSession(session));
                actionBox.getChildren().add(registerButton);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        bubble.getChildren().add(actionBox);

        bubbleContainer.getChildren().add(bubble);

        // Timestamp
        if (session.getCreatedAt() != null) {
            Label timestamp = new Label(session.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));
            timestamp.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
            bubbleContainer.getChildren().add(timestamp);
        }
        container.getChildren().add(bubbleContainer);
        messageBox.getChildren().add(container);

        return messageBox;
    }


    private void displaySessionMessage(StudySession session) {
        if (displayedSessionIds.contains(session.getId())) {
            return;
        }
        displayedSessionIds.add(session.getId());

        VBox sessionBox = createSessionMessageBox(session);
        messageContainer.getChildren().add(sessionBox);
        scrollToBottom();
    }

    private void handleVoteDialog(StudySession session) {
        try {
            List<ProposedDate> dates = studysessionFacade.getProposedDates(session.getId());
            List<Long> userVotes = studysessionFacade.getUserVotes(session.getId(), currentUserId);

            if (dates.isEmpty()) {
                showError("No proposed dates available");
                return;
            }

            Dialog<ButtonType> voteDialog = new Dialog<>();
            voteDialog.setTitle("Vote for Date");
            voteDialog.setHeaderText("Select your preferred date(s) for: " + session.getTitle());

            VBox content = new VBox(10);
            List<CheckBox> checkBoxes = new ArrayList<>();

            for (ProposedDate date : dates) {
                CheckBox cb = new CheckBox(date.getProposedDateTime().format(
                        DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) +
                        " - " + date.getVoteCount() + " votes");
                cb.setSelected(userVotes.contains(date.getId()));
                cb.setUserData(date.getId());
                checkBoxes.add(cb);
                content.getChildren().add(cb);
            }

            voteDialog.getDialogPane().setContent(content);
            voteDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            voteDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    for (CheckBox cb : checkBoxes) {
                        Long dateId = (Long) cb.getUserData();
                        if (cb.isSelected() && !userVotes.contains(dateId)) {
                            studysessionFacade.voteForDate(dateId, currentUserId, session.getId());
                        }
                    }
                    showSuccess("Votes recorded!");

                    if (tcpClient != null) {
                        TcpChatClient.EventEnvelope envelope = new TcpChatClient.EventEnvelope();
                        envelope.type = "session-vote";
                        envelope.id = session.getId();
                        tcpClient.sendEvent(envelope);
                    }
                }
            });
        } catch (Exception e) {
            showError("Error loading dates: " + e.getMessage());
        }
    }


    private void handleProposeDate(StudySession session) {
        Dialog<ProposedDate> dialog = new Dialog<>();
        dialog.setTitle("Propose New Date");
        dialog.setHeaderText("Propose a new date for: " + session.getTitle());

        VBox content = new VBox(15);
        content.setPrefWidth(400);

        Label instruction = new Label("Select a date and time for this session:");
        instruction.setStyle("-fx-font-weight: bold;");

        HBox dateTimeBox = new HBox(10);
        dateTimeBox.setAlignment(Pos.CENTER_LEFT);

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Select date");
        datePicker.setPrefWidth(150);

        Label timeLabel = new Label("Time:");

        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 14);
        hourSpinner.setPrefWidth(70);
        hourSpinner.setEditable(true);

        Label colonLabel = new Label(":");

        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0, 15);
        minuteSpinner.setPrefWidth(70);
        minuteSpinner.setEditable(true);

        dateTimeBox.getChildren().addAll(datePicker, timeLabel, hourSpinner, colonLabel, minuteSpinner);

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);

        content.getChildren().addAll(instruction, dateTimeBox, errorLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (datePicker.getValue() == null) {
                errorLabel.setText("Please select a date");
                errorLabel.setVisible(true);
                event.consume();
                return;
            }

            int hour = hourSpinner.getValue();
            int minute = minuteSpinner.getValue();

            LocalDateTime proposedDateTime = LocalDateTime.of(
                    datePicker.getValue().getYear(),
                    datePicker.getValue().getMonth(),
                    datePicker.getValue().getDayOfMonth(),
                    hour,
                    minute,
                    0
            );

            if (proposedDateTime.isBefore(LocalDateTime.now())) {
                errorLabel.setText("Proposed date must be in the future");
                errorLabel.setVisible(true);
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK && datePicker.getValue() != null) {
                int hour = hourSpinner.getValue();
                int minute = minuteSpinner.getValue();

                LocalDateTime proposedDateTime = LocalDateTime.of(
                        datePicker.getValue().getYear(),
                        datePicker.getValue().getMonth(),
                        datePicker.getValue().getDayOfMonth(),
                        hour,
                        minute,
                        0
                );

                ProposedDate proposedDate = new ProposedDate();
                proposedDate.setProposedDateTime(proposedDateTime);
                return proposedDate;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(proposedDate -> {
            try {
                studysessionFacade.addProposedDate(session.getId(), proposedDate);
                showSuccess("Date proposed successfully!");

                StudySession updatedSession = studysessionFacade.getSession(session.getId());
                if (updatedSession != null) {
                    VBox existingBox = null;
                    for (javafx.scene.Node node : messageContainer.getChildren()) {
                        if (("session_" + session.getId()).equals(node.getUserData())) {
                            existingBox = (VBox) node;
                            break;
                        }
                    }

                    if (existingBox != null) {
                        int index = messageContainer.getChildren().indexOf(existingBox);
                        VBox newBox = createSessionMessageBox(updatedSession);
                        messageContainer.getChildren().set(index, newBox);
                    }
                }
            } catch (Exception e) {
                showError("Failed to propose date: " + e.getMessage());
            }
        });
    }



    private void handleViewSessionDetails(StudySession session) {
        // Open detailed view (you can create a separate FXML for this)
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session Details");
        alert.setHeaderText(session.getTitle());

        StringBuilder content = new StringBuilder();
        content.append("Status: ").append(session.getStatus()).append("\n");
        content.append("Creator: ").append(session.getCreatorFullName()).append("\n");

        if (session.getDescription() != null) {
            content.append("Description: ").append(session.getDescription()).append("\n");
        }
        if (session.getSessionDate() != null) {
            content.append("Date: ").append(session.getSessionDate().format(
                    DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("\n");
        }

        content.append("Location: ").append(session.getLocation().getDisplayValue()).append("\n");
        content.append("Participants: ").append(session.getParticipantCount());

        if (session.getMaxParticipants() > 0) {
            content.append("/").append(session.getMaxParticipants());
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    private void handleRegisterSession(StudySession session) {
        try {
            boolean success = studysessionFacade.registerForSession(session.getId(), currentUserId);
            if (success) {
                showSuccess("Registered successfully!");
                loadMessages();
                updateSessionMessage(session.getId());

                if (tcpClient != null) {
                    TcpChatClient.EventEnvelope env = new TcpChatClient.EventEnvelope();
                    env.type = "session-update";
                    env.id = session.getId();
                    tcpClient.sendEvent(env);
                }
            }
        } catch (Exception e) {
            showError("Registration failed: " + e.getMessage());
        }
    }

    private void handleUnregisterSession(StudySession session) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Unregister");
        confirm.setHeaderText("Unregister from session?");
        confirm.setContentText("Are you sure you want to unregister from: " + session.getTitle());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    studysessionFacade.unregisterFromSession(session.getId(), currentUserId);
                    showSuccess("Unregistered successfully!");
                    loadMessages();
                    updateSessionMessage(session.getId());


                    if (tcpClient != null) {
                        TcpChatClient.EventEnvelope env = new TcpChatClient.EventEnvelope();
                        env.type = "session-update";
                        env.id = session.getId();
                        tcpClient.sendEvent(env);
                    }
                } catch (Exception e) {
                    showError("Unregister failed: " + e.getMessage());
                }
            }
        });
    }

    private void updateSessionMessage(Long sessionId) {
        CompletableFuture.supplyAsync(() -> {
            try {
                // fetch updated sessions for the group and find the one we need
                List<StudySession> sessions = studysessionFacade.getGroupSessions(currentGroupId, "All");
                for (StudySession s : sessions) {
                    if (s.getId() != null && s.getId().equals(sessionId)) return s;
                }
                return null;
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error updating session: " + e.getMessage()));
                return null;
            }
        }).thenAcceptAsync(updatedSession -> {
            if (updatedSession == null) return;

            // find existing message node by userData and replace it in-place
            for (javafx.scene.Node node : new ArrayList<>(messageContainer.getChildren())) {
                Object ud = node.getUserData();
                if (ud != null && ud.equals("session_" + sessionId)) {
                    int index = messageContainer.getChildren().indexOf(node);
                    VBox newBox = createSessionMessageBox(updatedSession);
                    messageContainer.getChildren().set(index, newBox);
                    return;
                }
            }

            // if not found (e.g. new session), append and track it
            messageContainer.getChildren().add(createSessionMessageBox(updatedSession));
            displayedSessionIds.add(sessionId);
            scrollToBottom();
        }, Platform::runLater);
    }


    private String getStatusStyle(StudySession.SessionStatus status) {
        return switch (status) {
            case PROPOSED -> "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 9;";
            case CONFIRMED -> "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 9;";
            case CANCELLED -> "-fx-background-color: #F44336; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 9;";
            case COMPLETED -> "-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 9;";
        };
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
    private void handleLeaveGroup() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Leave Group");
        confirm.setHeaderText("Leave this group?");
        confirm.setContentText("You will no longer be able to access this group's chat and files.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                membershipFacade.leaveGroup(currentUserId, currentGroupId);
                showSuccess("You have left the group.");
                handleBackToGroups(null);
            } catch (Exception e) {
                showError("Error leaving group: " + e.getMessage());
            }
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
        displayedSessionIds.clear();
        loadMessages();
        loadGroupSessions();
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

        // Add Report button for other users' files
        if (!isOwnFile) {
            Button reportBtn = new Button("Report");
            reportBtn.setStyle("-fx-font-size: 10; -fx-background-color: #ffcccc; -fx-text-fill: #cc0000; -fx-padding: 2 8; -fx-background-radius: 3;");
            reportBtn.setTooltip(new Tooltip("Report this file"));
            reportBtn.setOnAction(e -> handleReportFile(file));
            buttonBox.getChildren().add(reportBtn);
        }

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

        // Report option (only for other users' files)
        if (!isOwner) {
            MenuItem reportItem = new MenuItem("🚨 Report");
            reportItem.setOnAction(e -> handleReportFile(file));
            contextMenu.getItems().add(reportItem);
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

        // Add Report button for other users' messages
        if (!isOwnMessage) {
            Button reportBtn = new Button("Report");
            reportBtn.setStyle("-fx-font-size: 10; -fx-background-color: #ffcccc; -fx-text-fill: #cc0000; -fx-cursor: hand; -fx-padding: 2 8; -fx-background-radius: 3;");
            reportBtn.setTooltip(new Tooltip("Report this message"));
            reportBtn.setOnAction(e -> handleReportMessage(message));
            timestampBox.getChildren().add(reportBtn);
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

        // Report option (only for other users' messages)
        if (!isOwner) {
            MenuItem reportItem = new MenuItem("🚨 Report");
            reportItem.setOnAction(e -> handleReportMessage(message));
            contextMenu.getItems().add(reportItem);
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
            case "session":
                if (env.data instanceof StudySession) {
                    StudySession session = (StudySession) env.data;
                    displaySessionMessage(session);
                }
                break;
            case "session-update":
            case "session-vote":
            case "session-register":
                if (env.id != null) {
                    // Find existing session box
                    VBox existingBox = null;
                    for (javafx.scene.Node node : messageContainer.getChildren()) {
                        if (("session_" + env.id).equals(node.getUserData())) {
                            existingBox = (VBox) node;
                            break;
                        }
                    }

                    try {
                        StudySession updated = studysessionFacade.getSession(env.id);
                        if (updated != null) {
                            if (existingBox != null) {
                                // Update in place
                                int index = messageContainer.getChildren().indexOf(existingBox);
                                messageContainer.getChildren().remove(existingBox);
                                VBox newBox = createSessionMessageBox(updated);
                                messageContainer.getChildren().add(index, newBox);
                            } else {
                                // Add new
                                displaySessionMessage(updated);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;

            case "edit":
            case "delete":
                loadMessages();
                break;
        }
    }

    /**
     * Handle report message - opens dialog to report a message
     */
    private void handleReportMessage(Message message) {
        showReportDialog(
            message.getSenderId(),
            "Message: " + (message.getContent().length() > 100
                ? message.getContent().substring(0, 100) + "..."
                : message.getContent())
        );
    }

    /**
     * Handle report file - opens dialog to report a file
     */
    private void handleReportFile(SharedFile file) {
        showReportDialog(
            file.getUploaderId(),
            "File: " + file.getOriginalFileName()
        );
    }

    /**
     * Show report dialog
     */
    private void showReportDialog(Long reportedUserId, String reportedContent) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Report Content");
        dialog.setHeaderText("Report this content");

        // Problem type choice
        ChoiceBox<String> problemTypeChoice = new ChoiceBox<>();
        problemTypeChoice.getItems().addAll("Spam", "Harassment", "Inappropriate Content", "Other");
        problemTypeChoice.setValue("Spam");

        // Description
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Describe the problem...");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);

        // Layout
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Problem Type:"),
            problemTypeChoice,
            new Label("Description:"),
            descriptionArea,
            new Label("Content being reported:"),
            new Label(reportedContent)
        );
        content.setStyle("-fx-padding: 10;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String problemType = problemTypeChoice.getValue();
                String description = descriptionArea.getText();

                if (description == null || description.trim().isEmpty()) {
                    showError("Please provide a description");
                    return;
                }

                try {
                    ReportsFacade.getInstance().createReport(
                        reportedUserId,
                        currentUserId,
                        problemType,
                        description,
                        reportedContent
                    );
                    showSuccess("Report submitted successfully. An admin will review it.");
                } catch (Exception e) {
                    showError("Error submitting report: " + e.getMessage());
                }
            }
        });
    }
}
