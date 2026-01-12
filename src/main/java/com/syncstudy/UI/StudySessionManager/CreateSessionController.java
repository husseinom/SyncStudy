package com.syncstudy.UI.StudySessionManager;

import com.syncstudy.BL.StudySessionManager.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CreateSessionController {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> locationTypeCombo;
    @FXML private TextField locationValueField;
    @FXML private VBox proposedDatesContainer;
    @FXML private Button addDateButton;
    @FXML private Button createButton;
    @FXML private Label errorLabel;

    private Long groupId;
    private Long userId;
    private Consumer<StudySession> onCreatedCallback;
    private StudySessionFacade sessionFacade;
    private List<DateTimePicker> dateTimePickers = new ArrayList<>();

    public void initialize() {
        sessionFacade = StudySessionFacade.getInstance();

        typeCombo.getItems().addAll("Confirmed", "Proposed");
        typeCombo.setValue("Proposed");
        typeCombo.setOnAction(e -> updateUIForSessionType());

        locationTypeCombo.getItems().addAll("Physical", "Online");
        locationTypeCombo.setValue("Physical");
        locationTypeCombo.setOnAction(e -> updateLocationLabel());

        // Add initial 2 date pickers for proposed sessions
        addDateTimePicker();
        addDateTimePicker();

        updateUIForSessionType();
    }

    public void setGroupData(Long groupId, Long userId, Consumer<StudySession> callback) {
        this.groupId = groupId;
        this.userId = userId;
        this.onCreatedCallback = callback;
    }

    private void updateUIForSessionType() {
        boolean isProposed = typeCombo.getValue().equals("Proposed");
        proposedDatesContainer.setVisible(isProposed);
        proposedDatesContainer.setManaged(isProposed);
        addDateButton.setVisible(isProposed);
        addDateButton.setManaged(isProposed);
    }

    private void updateLocationLabel() {
        String type = locationTypeCombo.getValue();
        if (type.equals("Physical")) {
            locationValueField.setPromptText("Enter physical address");
        } else {
            locationValueField.setPromptText("Enter meeting link (Zoom, Teams, etc.)");
        }
    }

    @FXML
    private void handleAddDate() {
        if (dateTimePickers.size() < 4) {
            addDateTimePicker();
        } else {
            errorLabel.setText("Maximum 4 proposed dates allowed");
            errorLabel.setVisible(true);
        }
    }

    private void addDateTimePicker() {
        HBox dateBox = new HBox(10);
        dateBox.setPadding(new Insets(5));

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Select date");

        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 14);
        hourSpinner.setPrefWidth(70);

        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0, 15);
        minuteSpinner.setPrefWidth(70);

        Label timeLabel = new Label("Time:");
        Label colonLabel = new Label(":");

        Button removeButton = new Button("Remove");
        removeButton.setOnAction(e -> {
            proposedDatesContainer.getChildren().remove(dateBox);
            dateTimePickers.removeIf(dtp -> dtp.container == dateBox);
        });

        // Only show remove button if more than 2 dates
        removeButton.setVisible(dateTimePickers.size() >= 2);

        dateBox.getChildren().addAll(datePicker, timeLabel, hourSpinner, colonLabel, minuteSpinner, removeButton);
        proposedDatesContainer.getChildren().add(dateBox);

        DateTimePicker picker = new DateTimePicker(dateBox, datePicker, hourSpinner, minuteSpinner);
        dateTimePickers.add(picker);

        // Update remove button visibility for all pickers
        updateRemoveButtons();
    }

    private void updateRemoveButtons() {
        for (int i = 0; i < dateTimePickers.size(); i++) {
            Button removeBtn = (Button) dateTimePickers.get(i).container.getChildren().get(5);
            removeBtn.setVisible(dateTimePickers.size() > 2);
        }
    }

    @FXML
    private void handleCreate() {
        try {
            // Validate input
            if (titleField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Title is required");
            }

            if (locationValueField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Location is required");
            }

            StudySession session = new StudySession();
            session.setGroupId(groupId);
            session.setCreatorId(userId);
            session.setTitle(titleField.getText().trim());
            session.setDescription(descriptionArea.getText().trim());
            session.setStatus(typeCombo.getValue().equals("Confirmed") ?
                    StudySession.SessionStatus.CONFIRMED :
                    StudySession.SessionStatus.PROPOSED);

            // Set location
            StudySession.LocationType locationType = locationTypeCombo.getValue().equals("Physical") ?
                    StudySession.LocationType.PHYSICAL :
                    StudySession.LocationType.ONLINE;
            session.setLocation(new StudySession.SessionLocation(locationType, locationValueField.getText().trim()));
            session.setMaxParticipants(0);

            if (session.getStatus() == StudySession.SessionStatus.PROPOSED) {
                // Validate at least 2 dates
                if (dateTimePickers.size() < 2) {
                    throw new IllegalArgumentException("At least 2 proposed dates are required");
                }

                List<ProposedDate> dates = new ArrayList<>();
                for (DateTimePicker picker : dateTimePickers) {
                    if (picker.datePicker.getValue() != null) {
                        int hour = picker.hourSpinner.getValue();
                        int minute = picker.minuteSpinner.getValue();
                        LocalDateTime dateTime = LocalDateTime.of(
                                picker.datePicker.getValue().getYear(),
                                picker.datePicker.getValue().getMonth(),
                                picker.datePicker.getValue().getDayOfMonth(),
                                hour,
                                minute,
                                0  // seconds
                        );

                        if (dateTime.isBefore(LocalDateTime.now())) {
                            throw new IllegalArgumentException("All proposed dates must be in the future");
                        }

                        ProposedDate proposedDate = new ProposedDate();
                        proposedDate.setProposedDateTime(dateTime);
                        dates.add(proposedDate);
                    }
                }

                if (dates.size() < 2) {
                    throw new IllegalArgumentException("Please fill in at least 2 proposed dates");
                }

                session.setProposedDates(dates);
                session.setVotingDeadline(LocalDateTime.now().plusDays(7)); // 7 days to vote
            }

            StudySession created = sessionFacade.createSession(session);
            if (onCreatedCallback != null) {
                onCreatedCallback.accept(created);
            }
            ((Stage) createButton.getScene().getWindow()).close();
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    private static class DateTimePicker {
        HBox container;
        DatePicker datePicker;
        Spinner<Integer> hourSpinner;
        Spinner<Integer> minuteSpinner;

        DateTimePicker(HBox container, DatePicker datePicker, Spinner<Integer> hourSpinner, Spinner<Integer> minuteSpinner) {
            this.container = container;
            this.datePicker = datePicker;
            this.hourSpinner = hourSpinner;
            this.minuteSpinner = minuteSpinner;
        }
    }
    @FXML
    private void handleCancel() {
        ((Stage) createButton.getScene().getWindow()).close();
    }

}