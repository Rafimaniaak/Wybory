package com.election.controller;

import com.election.HashGenerator;
import com.election.dao.UserDAO;
import com.election.model.User;
import com.election.service.AuthService;
import com.election.service.VotingService;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;

public class LoginController {

    // Elementy UI z FXML
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();
    private final VotingService votingService = new VotingService();

    public boolean login(String username, String password) {
        User user = userDAO.findByUsername(username);

        if (user == null) {
            System.out.println("Użytkownik nie istnieje: " + username);
            return false;
        }

        System.out.println("=== DEBUG ===");
        System.out.println("Podane hasło: " + password);
        System.out.println("Hash z bazy:  " + user.getPassword());
        System.out.println("Wynik weryfikacji: " + BCrypt.checkpw(password, user.getPassword()));

        return BCrypt.checkpw(password, user.getPassword());
    }

    @FXML
    private void initialize() {
        // Inicjalizacja liczby użytkowników przy starcie
        loadUserCountAsync();
    }

    private void loadUserCountAsync() {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return userDAO.getAllUsers().size();
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue() > 0) {
                statusLabel.setText("Zarejestrowanych użytkowników: " + task.getValue());
            } else {
                statusLabel.setText("Brak zarejestrowanych użytkowników");
            }
        });

        new Thread(task).start();
    }
    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Wprowadź login i hasło!");
            return;
        }

        errorLabel.setText("Logowanie...");
        usernameField.setDisable(true);
        passwordField.setDisable(true);

        Task<AuthService.AuthResult> loginTask = new Task<>() {
            @Override
            protected AuthService.AuthResult call() {
                return authService.authenticate(username, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            usernameField.setDisable(false);
            passwordField.setDisable(false);

            AuthService.AuthResult result = loginTask.getValue();
            switch (result.getStatus()) {
                case SUCCESS:
                    redirectUser(result.getUser());
                    break;
                case USER_NOT_FOUND:
                    showError("Nieprawidłowy login!");
                    break;
                case INVALID_PASSWORD:
                    showError("Nieprawidłowe hasło!");
                    break;
            }
        });

        loginTask.setOnFailed(e -> {
            usernameField.setDisable(false);
            passwordField.setDisable(false);
            showError("Błąd połączenia z bazą danych");
        });

        new Thread(loginTask).start();
    }
    @FXML
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Walidacja pól wejściowych
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Proszę wypełnić wszystkie pola!");
            errorLabel.setStyle("-fx-text-fill: #ff0000;");
            return;
        }

        try {
            User user = userDAO.findByUsername(username);

            if (user == null) {
                errorLabel.setText("Nieprawidłowy login lub hasło!");
                errorLabel.setStyle("-fx-text-fill: #ff0000;");
                return;
            }

            // Debugowanie - wyświetl hash z bazy
            System.out.println("=== DEBUG LOGIN ===");
            System.out.println("Podane hasło: " + password);
            System.out.println("Hash z bazy:  " + user.getPassword());

            if (BCrypt.checkpw(password, user.getPassword())) {
                errorLabel.setText("");
                statusLabel.setText("Logowanie pomyślne!");
                statusLabel.setStyle("-fx-text-fill: #00cc00;");

                // Czekaj 1 sekundę przed przekierowaniem
                new Thread(() -> {
                    PauseTransition pause = new PauseTransition(Duration.seconds(1));
                    pause.setOnFinished(e -> redirectUser(user));
                    pause.play();
                }).start();

            } else {
                errorLabel.setText("Nieprawidłowy login lub hasło!");
                errorLabel.setStyle("-fx-text-fill: #ff0000;");
                passwordField.clear();
            }

        } catch (Exception e) {
            errorLabel.setText("Błąd połączenia z bazą danych!");
            errorLabel.setStyle("-fx-text-fill: #ff0000;");
            e.printStackTrace();
        } finally {
            passwordField.clear(); // Wyczyść hasło niezależnie od wyniku
        }
    }
    private void redirectUser(User user) {
        try {
            String fxmlPath = user.getRole().equals("ADMIN")
                    ? "/com/election/view/admin_view.fxml"
                    : "/com/election/view/user_view.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Przekazanie danych użytkownika do nowego kontrolera
            if (user.getRole().equals("ADMIN")) {
                AdminController controller = loader.getController();
                controller.initializeWithUser(user);
            } else {
                UserController controller = loader.getController();
                controller.initialize();
            }

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Błąd ładowania widoku");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #ff4444;");
    }
    @FXML
    private void handleLoginOrGenerate(ActionEvent event) {
        if ("sekretne".equals(passwordField.getText())) {
            openHashGenerator();
            ((Stage) passwordField.getScene().getWindow()).hide();
        } else {
            performLogin();
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        usernameField.clear();
        passwordField.clear();
        errorLabel.setText("");
    }

    private void openHashGenerator() {
        try {
            Stage hashStage = new Stage();
            HashGenerator hashGenerator = new HashGenerator();
            hashGenerator.setParentStage((Stage) passwordField.getScene().getWindow());
            hashGenerator.start(hashStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}