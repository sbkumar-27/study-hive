package com.studyhive.desktop.controller;

import com.studyhive.desktop.config.AppContext;
import com.studyhive.desktop.model.Community;
import com.studyhive.desktop.model.CommunityMember;
import com.studyhive.desktop.model.CreateSubjectRequest;
import com.studyhive.desktop.model.MemberRole;
import com.studyhive.desktop.model.Subject;
import com.studyhive.desktop.navigation.NavParams;
import com.studyhive.desktop.navigation.Router;
import com.studyhive.desktop.navigation.ScreenController;
import com.studyhive.desktop.util.Alerts;
import com.studyhive.desktop.util.Async;
import com.studyhive.desktop.util.ErrorText;
import com.studyhive.desktop.util.Formatters;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CommunityDetailController implements ScreenController {

    @FXML private Label nameLabel;
    @FXML private Label typeBadge;
    @FXML private Label metaLabel;
    @FXML private Label descriptionLabel;
    @FXML private Button leaveButton;
    @FXML private VBox joinCodeBox;
    @FXML private Label joinCodeLabel;
    @FXML private VBox notMemberBox;
    @FXML private VBox memberContent;
    @FXML private Button addSubjectToggle;
    @FXML private HBox subjectForm;
    @FXML private TextField subjectCodeField;
    @FXML private TextField subjectNameField;
    @FXML private Label subjectErrorLabel;
    @FXML private FlowPane subjectsContainer;
    @FXML private Label noSubjectsLabel;
    @FXML private TableView<CommunityMember> membersTable;
    @FXML private TableColumn<CommunityMember, String> memberNameColumn;
    @FXML private TableColumn<CommunityMember, String> memberUsernameColumn;
    @FXML private TableColumn<CommunityMember, String> memberRoleColumn;
    @FXML private TableColumn<CommunityMember, String> memberJoinedColumn;

    private long communityId;

    @Override
    public void onShow(NavParams params) {
        Long id = params.getLong("communityId");
        if (id == null) {
            throw new IllegalStateException("CommunityDetail requires a communityId param");
        }
        this.communityId = id;
        setupTable();
        load();
    }

    private void setupTable() {
        memberNameColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().displayName()));
        memberUsernameColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().username()));
        memberRoleColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().memberRole().name()));
        memberJoinedColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(Formatters.date(cell.getValue().joinedAt())));
    }

    private void load() {
        Async.run(
                () -> AppContext.get().communities.get(communityId),
                this::render,
                error -> Alerts.error("Couldn't load community", error));
    }

    private void render(Community community) {
        nameLabel.setText(community.name());
        typeBadge.setText(community.communityType().name());
        metaLabel.setText(community.memberCount() + " members · owned by " + community.ownerUsername());
        descriptionLabel.setText(community.description() == null ? "" : community.description());

        boolean isMember = community.currentUserRole() != null;
        boolean isOwner = community.currentUserRole() == MemberRole.OWNER;

        leaveButton.setVisible(isMember && !isOwner);
        leaveButton.setManaged(isMember && !isOwner);

        notMemberBox.setVisible(!isMember);
        notMemberBox.setManaged(!isMember);

        memberContent.setVisible(isMember);
        memberContent.setManaged(isMember);

        boolean showJoinCode = isMember && community.joinCode() != null;
        joinCodeBox.setVisible(showJoinCode);
        joinCodeBox.setManaged(showJoinCode);
        if (showJoinCode) {
            String suffix = community.hasPassword() ? "  (password protected)" : "";
            joinCodeLabel.setText("Join code: " + community.joinCode() + suffix);
        }

        if (isMember) {
            loadSubjects();
            loadMembers();
        }
    }

    private void loadSubjects() {
        Async.run(
                () -> AppContext.get().subjects.list(communityId),
                this::renderSubjects,
                error -> Alerts.error("Couldn't load subjects", error));
    }

    private void renderSubjects(List<Subject> subjects) {
        subjectsContainer.getChildren().clear();
        noSubjectsLabel.setVisible(subjects.isEmpty());
        noSubjectsLabel.setManaged(subjects.isEmpty());
        for (Subject s : subjects) {
            VBox card = new VBox(4);
            card.getStyleClass().add("card");
            card.setPrefWidth(220);
            Label code = new Label(s.subjectCode());
            code.getStyleClass().add("h3");
            Label name = new Label(s.subjectName());
            name.getStyleClass().addAll("muted", "text-sm");
            card.getChildren().addAll(code, name);
            card.setOnMouseClicked(e -> Router.get().navigate(
                    "Materials.fxml",
                    NavParams.builder()
                            .put("communityId", communityId)
                            .put("subjectId", s.subjectId())
                            .build()));
            subjectsContainer.getChildren().add(card);
        }
    }

    private void loadMembers() {
        Async.run(
                () -> AppContext.get().communities.members(communityId),
                members -> membersTable.setItems(FXCollections.observableArrayList(members)),
                error -> Alerts.error("Couldn't load members", error));
    }

    @FXML
    private void toggleSubjectForm() {
        boolean show = !subjectForm.isVisible();
        subjectForm.setVisible(show);
        subjectForm.setManaged(show);
    }

    @FXML
    private void handleAddSubject() {
        subjectErrorLabel.setVisible(false);
        subjectErrorLabel.setManaged(false);
        String code = subjectCodeField.getText();
        String name = subjectNameField.getText();
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            subjectErrorLabel.setText("Enter both a code and a name.");
            subjectErrorLabel.setVisible(true);
            subjectErrorLabel.setManaged(true);
            return;
        }
        Async.run(
                () -> AppContext.get().subjects.create(communityId, new CreateSubjectRequest(code.trim(), name.trim())),
                subject -> {
                    subjectCodeField.clear();
                    subjectNameField.clear();
                    subjectForm.setVisible(false);
                    subjectForm.setManaged(false);
                    loadSubjects();
                },
                error -> {
                    subjectErrorLabel.setText(ErrorText.of(error));
                    subjectErrorLabel.setVisible(true);
                    subjectErrorLabel.setManaged(true);
                });
    }

    @FXML
    private void handleBrowseMaterials() {
        Router.get().navigate("Materials.fxml", NavParams.builder().put("communityId", communityId).build());
    }

    @FXML
    private void handleUploadMaterial() {
        Router.get().navigate("UploadMaterial.fxml", NavParams.builder().put("communityId", communityId).build());
    }

    @FXML
    private void handleLeave() {
        if (!Alerts.confirm("Leave community", "Are you sure you want to leave this community?")) {
            return;
        }
        Async.run(
                () -> {
                    AppContext.get().communities.leave(communityId);
                    return null;
                },
                v -> Router.get().navigate("Communities.fxml"),
                error -> Alerts.error("Couldn't leave community", error));
    }
}
