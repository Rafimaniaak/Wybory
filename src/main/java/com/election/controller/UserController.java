package com.election.controller;

import com.election.dao.CandidateDAO;
import com.election.exception.VotingException;
import com.election.model.Candidate;
import com.election.model.CandidateResult;
import com.election.model.User;
import com.election.service.ElectionService;
import com.election.service.VotingService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.List;

public class UserController {

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<Candidate> candidateComboBox;

    private final ElectionService electionService = new ElectionService();

    @FXML
    public void initialize() { // Ta metoda jest wywoływana automatycznie
        loadCandidates();
    }

    private void loadCandidates() {
        List<Candidate> candidates = electionService.getAllCandidates();
        candidateComboBox.getItems().setAll(candidates); // Użyj setAll() zamiast addAll()
    }

    @FXML private Button submitButton;
    @FXML private Button logoutButton;
    //private final ElectionService electionService = new ElectionService();
    @FXML
    private void handleVoteSubmit(ActionEvent event) {
        Candidate selectedCandidate = candidateComboBox.getValue();

        if (selectedCandidate == null) {
            statusLabel.setText("Wybierz kandydata przed oddaniem głosu!");
            return;
        }

        try {
            // Oddaj głos
            electionService.registerVote(selectedCandidate);

            // Ukryj przycisk "Oddaj głos", pokaż "Wyloguj"
            submitButton.setVisible(false);
            logoutButton.setVisible(true);
            statusLabel.setText("Głos oddany pomyślnie! Możesz się wylogować.");

        } catch (Exception e) {
            statusLabel.setText("Błąd: " + e.getMessage());
            submitButton.setVisible(true); // Przywróć przycisk w przypadku błędu
        }
    }
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Zamknij obecne okno
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();

            // Otwórz nowe okno logowania
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/election/view/login.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setTitle("Logowanie");
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}