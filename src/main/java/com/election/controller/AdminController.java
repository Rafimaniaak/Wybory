package com.election.controller;

import com.election.HashGenerator;
import com.election.dao.UserDAO;
import com.election.model.CandidateResult;
import com.election.model.User;
import com.election.service.ElectionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;

    @FXML private TableView<CandidateResult> resultsTable;
    @FXML private TableColumn<CandidateResult, String> candidateColumn;
    @FXML private TableColumn<CandidateResult, Number> votesColumn;

    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();
    private final ElectionService electionService = new ElectionService();
    private final ObservableList<CandidateResult> candidatesData = FXCollections.observableArrayList();
    private User currentAdmin;

    @FXML
    public void initialize() {
        configureUserTable();
        configureResultsTable();
        loadInitialData();
        roleComboBox.setItems(FXCollections.observableArrayList("USER", "ADMIN"));
    }

    private void configureUserTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
    }

    private void configureResultsTable() {
        candidateColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        votesColumn.setCellValueFactory(new PropertyValueFactory<>("votes"));

        votesColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : String.format("%,d", value.intValue()));
            }
        });
    }

    private void loadInitialData() {
        usersTable.getItems().setAll(userDAO.getAllUsers());
        refreshElectionData();
    }

    public void initializeWithUser(User adminUser) {
        this.currentAdmin = adminUser;
        logAdminAccess();
    }

    private void logAdminAccess() {
        System.out.println("Administrator " + currentAdmin.getUsername()
                + " zalogowany o " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    @FXML
    private void refreshResults(ActionEvent event) {
        refreshElectionData();
    }

    private void refreshElectionData() {
        if (statusLabel != null) {
            statusLabel.setText("Odświeżanie wyników...");
        }

        try {
            List<CandidateResult> results = electionService.getCurrentResults();
            candidatesData.setAll(results);
            resultsTable.setItems(candidatesData);
            statusLabel.setText("Wyniki zaktualizowane: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("Błąd podczas aktualizacji!");
            }
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/election/view/login.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setTitle("Logowanie");
            loginStage.show();
        } catch (IOException e) {
            showErrorAlert("Błąd logowania", e.getMessage());
        }
    }

    @FXML
    private void handleOpenHashGenerator(ActionEvent event) {
        try {
            Stage adminStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Stage hashStage = new Stage();
            HashGenerator hashGenerator = new HashGenerator();
            hashGenerator.setParentStage(adminStage);
            hashGenerator.start(hashStage);
        } catch (Exception e) {
            showErrorAlert("Błąd generatora", e.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label addUserStatusLabel;
    @FXML
    private void handleAddUser(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            addUserStatusLabel.setText("Wszystkie pola są wymagane!");
            return;
        }

        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password); // lub zahashuj, jeśli nie jest jeszcze zahashowane
            user.setRole(role);
            user.setHasVoted(false);

            userDAO.saveUser(user);
            usersTable.getItems().add(user);  // od razu aktualizuj tabelę
            addUserStatusLabel.setText("Użytkownik dodany pomyślnie.");
            clearAddUserForm();
        } catch (Exception e) {
            addUserStatusLabel.setText("Błąd: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearAddUserForm() {
        usernameField.clear();
        passwordField.clear();
        roleComboBox.getSelectionModel().clearSelection();
    }

}