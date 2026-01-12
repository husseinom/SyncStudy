package com.syncstudy.UI.StudySessionManager;

import com.syncstudy.BL.StudySessionManager.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StudySessionListController {
    @FXML private VBox sessionListContainer;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Label groupTitleLabel;

    private Long groupId;
    private Long userId;
    private boolean isAdmin;
    private StudySessionFacade sessionFacade;

    public void initialize() {
        sessionFacade = StudySessionFacade.getInstance();
        filterCombo.getItems().addAll("All", "Proposed", "Confirmed", "Upcoming", "Past");
        filterCombo.setValue("All");
        filterCombo.setOnAction(e -> loadSessions());
    }

    public void setGroupData(Long groupId, String groupName, Long userId, boolean isAdmin) {
        this.groupId = groupId;
        this.userId = userId;
        this.isAdmin = isAdmin;
        groupTitleLabel.setText(groupName + " - Study Sessions");
        loadSessions();
    }

    private void loadSessions() {
        sessionListContainer.getChildren().clear();
        String filter = filterCombo.getValue();
        List<StudySession> sessions = sessionFacade.getGroupSessions(groupId, filter);

        for (StudySession session : sessions) {
            sessionListContainer.getChildren().add(createSessionCard(session));
        }
    }

    private VBox createSessionCard(StudySession session) {
        VBox card = new VBox(10);
        card.setUserData("session_" + session.getId());
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 5; " +
                "-fx-border-color: " + session.getStatusColor() + "; -fx-border-width: 2; -fx-border-radius: 5;");

        // Title and Status
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(session.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        Label statusBadge = new Label(session.getStatus().toString());
        statusBadge.setStyle("-fx-background-color: " + session.getStatusColor() +
                "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 3;");
        headerBox.getChildren().addAll(titleLabel, statusBadge);

        // Description
        Label descLabel = new Label(session.getDescription() != null ? session.getDescription() : "No description");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: gray;");

        // Details
        VBox detailsBox = new VBox(5);
        if (session.getStatus() == StudySession.SessionStatus.PROPOSED) {
            Label voteLabel = new Label("🗳️ Voting in progress");
            detailsBox.getChildren().add(voteLabel);
            if (session.getVotingDeadline() != null) {
                Label deadlineLabel = new Label("Voting deadline: " +
                        session.getVotingDeadline().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
                deadlineLabel.setStyle("-fx-font-size: 11; -fx-text-fill: gray;");
                detailsBox.getChildren().add(deadlineLabel);
            }
        } else if (session.getSessionDate() != null) {
            Label dateLabel = new Label("📆 " +
                    session.getSessionDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            detailsBox.getChildren().add(dateLabel);
        }

        Label locationLabel = new Label("📍 " + session.getLocation().getDisplayValue());
        Label participantsLabel = new Label("👥 " + session.getParticipantCount() +
                (session.getMaxParticipants() > 0 ? "/" + session.getMaxParticipants() : "") + " participants");
        detailsBox.getChildren().addAll(locationLabel, participantsLabel);

        // Action Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        // Only show register/unregister buttons for CONFIRMED sessions
        if (session.getStatus() == StudySession.SessionStatus.CONFIRMED) {
            try {
                boolean isRegistered = sessionFacade.isUserRegistered(session.getId(), userId);
                if (isRegistered) {
                    Button unregisterBtn = new Button("Unregister");
                    unregisterBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                    unregisterBtn.setOnAction(e -> handleUnregister(session));
                    buttonBox.getChildren().add(unregisterBtn);
                } else if (sessionFacade.canUserRegister(session.getId())) {
                    Button registerBtn = new Button("Register");
                    registerBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                    registerBtn.setOnAction(e -> handleRegister(session));
                    buttonBox.getChildren().add(registerBtn);
                } else if (session.isFull()) {
                    Label fullLabel = new Label("Session Full");
                    fullLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                    buttonBox.getChildren().add(fullLabel);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // View Details button for all sessions
        Button detailsBtn = new Button("View Details");
        detailsBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        detailsBtn.setOnAction(e -> handleViewDetails(session));
        buttonBox.getChildren().add(detailsBtn);

        card.getChildren().addAll(headerBox, descLabel, detailsBox, buttonBox);
        return card;
    }

    private void handleRegister(StudySession session) {
        try {
            boolean success = sessionFacade.registerForSession(session.getId(), userId);
            if (success) {
                showSuccess("Registered successfully!");
                updateSessionCard(session.getId());
            }
        } catch (Exception e) {
            showError("Registration failed: " + e.getMessage());
        }
    }

    private void updateSessionCard(Long sessionId) {
        CompletableFuture.supplyAsync(() -> {
            try {
                // fetch current group sessions and find the updated one
                List<StudySession> sessions = sessionFacade.getGroupSessions(groupId, filterCombo.getValue());
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

            // find existing card by userData and replace it in-place
            for (javafx.scene.Node node : new ArrayList<>(sessionListContainer.getChildren())) {
                Object ud = node.getUserData();
                if (ud != null && ud.equals("session_" + sessionId)) {
                    int index = sessionListContainer.getChildren().indexOf(node);
                    VBox newCard = createSessionCard(updatedSession);
                    sessionListContainer.getChildren().set(index, newCard);
                    return;
                }
            }
            sessionListContainer.getChildren().add(createSessionCard(updatedSession));
        }, Platform::runLater);
    }

    private void handleUnregister(StudySession session) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Unregister");
        confirm.setHeaderText("Unregister from session?");
        confirm.setContentText("Are you sure you want to unregister from: " + session.getTitle());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    sessionFacade.unregisterFromSession(session.getId(), userId);
                    showSuccess("Unregistered successfully!");
                    // update only the changed card to avoid clearing the whole list
                    updateSessionCard(session.getId());
                } catch (Exception e) {
                    showError("Unregister failed: " + e.getMessage());
                }
            }
        });
    }

    private void refreshSessionList() {
        CompletableFuture.supplyAsync(() -> {
            try {
                String filter = filterCombo.getValue();
                return sessionFacade.getGroupSessions(groupId, filter);
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error loading sessions: " + e.getMessage()));
                return List.<StudySession>of();
            }
        }).thenAcceptAsync(sessions -> {
            sessionListContainer.getChildren().clear();
            if (sessions.isEmpty()) {
                showEmptyState();
            } else {
                // use createSessionCard so userData is set consistently (used by updateSessionCard)
                for (StudySession session : sessions) {
                    sessionListContainer.getChildren().add(createSessionCard(session));
                }
            }
        }, Platform::runLater);
    }

    private void showEmptyState() {
        VBox emptyState = new VBox(10);
        emptyState.setAlignment(Pos.CENTER);
        Label noMessages = new Label("No messages yet");
        noMessages.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        Label startConversation = new Label("Start the conversation!");
        startConversation.setStyle("-fx-text-fill: gray;");
        emptyState.getChildren().addAll(noMessages, startConversation);
        sessionListContainer.getChildren().add(emptyState);
    }



    private void handleConfirmSession(StudySession session) {
        try {
            ProposedDate mostVoted = sessionFacade.getMostVotedDate(session.getId());

            if (mostVoted == null) {
                showError("No votes yet. Cannot confirm session.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Session");
            confirm.setHeaderText("Confirm session with most voted date?");
            confirm.setContentText("Selected Date: " +
                    mostVoted.getProposedDateTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) +
                    "\nVotes: " + mostVoted.getVoteCount());

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        sessionFacade.finalizeProposedSession(
                                session.getId(),
                                mostVoted.getId(),
                                userId,
                                isAdmin
                        );
                        showSuccess("Session confirmed successfully!");
                        loadSessions();
                    } catch (Exception e) {
                        showError("Failed to confirm: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    private VBox displaySessionCard(StudySession session) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #E0E0E0; -fx-border-radius: 5; -fx-background-radius: 5;");
        card.setMaxWidth(Double.MAX_VALUE);

        // Header with title and status
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(session.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label statusBadge = new Label(session.getStatus().toString());
        statusBadge.setStyle(getStatusStyle(session.getStatus()));

        header.getChildren().addAll(titleLabel, statusBadge);

        // Details
        VBox details = new VBox(5);

        if (session.getDescription() != null && !session.getDescription().isEmpty()) {
            Label desc = new Label(session.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: gray;");
            details.getChildren().add(desc);
        }

        if (session.getSessionDate() != null) {
            Label date = new Label("📅 " + session.getSessionDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            details.getChildren().add(date);
        }

        Label location = new Label("📍 " + session.getLocation().getDisplayValue());
        details.getChildren().add(location);

        Label participants = new Label("👥 " + session.getParticipantCount() + " participants");
        details.getChildren().add(participants);

        card.getChildren().addAll(header, details);

        sessionListContainer.getChildren().add(card);
        return card;
    }

    private String getStatusStyle(StudySession.SessionStatus status) {
        return switch (status) {
            case PROPOSED -> "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 10;";
            case CONFIRMED -> "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 10;";
            case CANCELLED -> "-fx-background-color: #F44336; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 10;";
            case COMPLETED -> "-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 10;";
        };
    }


    private void handleViewDetails(StudySession session) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session Details");
        alert.setHeaderText(session.getTitle());

        StringBuilder content = new StringBuilder();
        content.append("Status: ").append(session.getStatus()).append("\n\n");
        content.append("Creator: ").append(session.getCreatorFullName() != null ?
                session.getCreatorFullName() : session.getCreatorUsername()).append("\n\n");

        if (session.getDescription() != null && !session.getDescription().isEmpty()) {
            content.append("Description: ").append(session.getDescription()).append("\n\n");
        }

        if (session.getStatus() == StudySession.SessionStatus.PROPOSED) {
            content.append("📊 Proposed Dates:\n");
            for (ProposedDate date : session.getProposedDates()) {
                content.append("  • ").append(date.getProposedDateTime().format(
                                DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")))
                        .append(" (").append(date.getVoteCount()).append(" votes)\n");
            }
            content.append("\n");
        } else if (session.getSessionDate() != null) {
            content.append("Date: ").append(session.getSessionDate().format(
                    DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))).append("\n\n");
        }

        content.append("Location: ").append(session.getLocation().getDisplayValue()).append("\n\n");
        content.append("Participants: ").append(session.getParticipantCount());
        if (session.getMaxParticipants() > 0) {
            content.append("/").append(session.getMaxParticipants());
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    private void handleVote(StudySession session) {
        try {
            List<ProposedDate> dates = sessionFacade.getProposedDates(session.getId());
            List<Long> userVotes = sessionFacade.getUserVotes(session.getId(), userId);

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
                            sessionFacade.voteForDate(dateId, userId, session.getId());
                        }
                    }
                    showSuccess("Votes recorded!");
                    loadSessions();
                }
            });
        } catch (Exception e) {
            showError("Error loading dates: " + e.getMessage());
        }
    }

    private void handleDeleteSession(StudySession session) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Session");
        confirm.setHeaderText("Delete " + session.getTitle() + "?");
        confirm.setContentText("This action cannot be undone. All votes and registrations will be lost.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    sessionFacade.cancelSession(session.getId(), userId, isAdmin, "Deleted by creator");
                    showSuccess("Session deleted successfully");
                    loadSessions();
                } catch (Exception e) {
                    showError("Delete failed: " + e.getMessage());
                }
            }
        });
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
