package com.election.controller;

import com.election.dao.UserDAO;
import com.election.model.Candidate;
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
    // Sekcja dla użytkowników
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;

    // Sekcja dla wyników
    @FXML private TableView<Candidate> resultsTable;
    @FXML private TableColumn<Candidate, String> candidateColumn;
    @FXML private TableColumn<Candidate, Number> votesColumn;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();
    private final ElectionService electionService = new ElectionService();
    private final ObservableList<Candidate> candidatesData = FXCollections.observableArrayList();
    private User currentAdmin;

    @FXML
    public void initialize() {
        configureUserTable();
        configureResultsTable();
        loadInitialData();
    }

    private void configureUserTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
    }

    private void configureResultsTable() {
        candidateColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        votesColumn.setCellValueFactory(new PropertyValueFactory<>("votes"));

        // Formatowanie liczb
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
            List<Candidate> results = electionService.getCurrentResults();
            candidatesData.setAll(results);
            resultsTable.setItems(candidatesData);
            statusLabel.setText("Wyniki zaktualizowane: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            if (statusLabel != null) {
                statusLabel.setText("Wyniki zaktualizowane: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
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

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}