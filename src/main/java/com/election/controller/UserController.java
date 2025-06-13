package com.election.controller;

import com.election.exception.ViewLoadingException;
import com.election.model.Candidate;
import com.election.model.User;
import com.election.service.ElectionService;
import com.election.service.VotingService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

// Kontroler panelu użytkownika
public class UserController {
    private User currentUser;
    private final VotingService votingService = new VotingService();
    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<Candidate> candidateComboBox;

    private final ElectionService electionService = new ElectionService();

    // Inicjalizuje kontroler i ładuje kandydatów
    @FXML
    public void initialize() { // Ta metoda jest wywoływana automatycznie
        loadCandidates();
    }

    // Inicjalizuje kontroler z danymi użytkownika
    public void initializeWithUser(User user) {
        this.currentUser = user;

        if(user.isHasVoted()) {
            submitButton.setDisable(true);
            candidateComboBox.setDisable(true);
            statusLabel.setText("Już oddałeś głos w tych wyborach!");
            statusLabel.setStyle("-fx-text-fill: #2ecc71;");
        }
    }

    // Obsługuje oddanie głosu
    @FXML
    private void handleVoteSubmit() {
        Candidate selectedCandidate = candidateComboBox.getValue();

        if (selectedCandidate == null) {
            statusLabel.setText("Wybierz kandydata przed oddaniem głosu!");
            return;
        }

        votingService.castVote(currentUser, selectedCandidate);

        submitButton.setDisable(true);
        candidateComboBox.setDisable(true);
        statusLabel.setText("Głos został pomyślnie zarejestrowany!");
        statusLabel.setStyle("-fx-text-fill: #2ecc71;");
    }

    // Ładuje listę kandydatów z bazy
    private void loadCandidates() {
        List<Candidate> candidates = electionService.getAllCandidates();
        candidateComboBox.getItems().setAll(candidates);
    }

    @FXML private Button submitButton;
    @FXML private Button logoutButton;

    // Obsługuje wylogowywanie
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/election/view/login.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setTitle("Logowanie");
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/app_icon.png")));
            loginStage.getIcons().add(icon);
            loginStage.show();
        } catch (IOException e) {
            throw new ViewLoadingException("Błąd podczas ładowania widoku logowania", e);
        }
    }
}