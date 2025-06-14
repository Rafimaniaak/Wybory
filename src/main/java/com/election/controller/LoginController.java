package com.election.controller;

import com.election.dao.UserDAO;
import com.election.model.User;
import com.election.service.AuthService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

// Kontroler ekranu logowania
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;
    @FXML private Button showPasswordButton;
    @FXML private TextField visiblePasswordField;
    private final Image eyeOpenImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/eye-open.png")));
    private final Image eyeClosedImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/eye-closed.png")));
    private final ImageView eyeIcon = new ImageView();
    @FXML
    private void togglePasswordVisibility() {
        if (passwordField.isVisible()) {
            // Pokaż hasło
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);

            eyeIcon.setImage(eyeClosedImage);
            showPasswordButton.setTooltip(new Tooltip("Ukryj hasło"));
        } else {
            // Ukryj hasło
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);

            eyeIcon.setImage(eyeOpenImage);
            showPasswordButton.setTooltip(new Tooltip("Pokaż hasło"));
        }
    }

    private String getPassword() {
        return passwordField.isVisible() ?
                passwordField.getText() :
                visiblePasswordField.getText();
    }

    private final UserDAO userDAO = new UserDAO();
    private final AuthService authService = new AuthService();

    // Inicjalizuje kontroler i ładuje dane
    @FXML
    private void initialize() {
        loadUserCountAsync();

        // Ustawienie obrazka dla przycisku
        eyeIcon.setImage(eyeOpenImage);
        eyeIcon.setFitWidth(16);
        eyeIcon.setFitHeight(16);
        showPasswordButton.setGraphic(eyeIcon);
        // Ustaw tooltip
        Tooltip.install(showPasswordButton, new Tooltip("Pokaż/ukryj hasło"));
        // Ukryj widoczne pole hasła na starcie
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
        // Upewnij się, że oba pola mają ten sam rozmiar
        visiblePasswordField.prefWidthProperty().bind(passwordField.widthProperty());
        visiblePasswordField.minWidthProperty().bind(passwordField.minWidthProperty());
        visiblePasswordField.maxWidthProperty().bind(passwordField.maxWidthProperty());
    }

    // Asynchronicznie ładuje liczbę użytkowników
    private void loadUserCountAsync() {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return userDAO.getAllUsers().size();
            }
        };
        new Thread(task).start();
    }
    // Obsługuje próbę logowania
    @FXML
    private void handleLogin(ActionEvent event) {
        String password = getPassword().trim();
        performLogin();
    }

    // Wykonuje proces logowania
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Proszę wypełnić wszystkie pola!");
            return;
        }

        Task<AuthService.AuthResult> loginTask = new Task<>() {
            @Override
            protected AuthService.AuthResult call() {
                return authService.authenticate(username, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            AuthService.AuthResult result = loginTask.getValue();
            switch (result.status()) {
                case SUCCESS:
                    redirectUser(result.user());
                    break;
                case USER_NOT_FOUND:
                case INVALID_PASSWORD:
                    // Ujednolicony komunikat dla obu przypadków
                    showError("Nieprawidłowy login lub hasło!");
                    break;
                default:
                    showError("Wystąpił nieoczekiwany błąd");
                    break;
            }
        });

        loginTask.setOnFailed(e -> showError("Błąd połączenia z bazą danych"));

        new Thread(loginTask).start();
    }

    // Przekierowuje użytkownika do odpowiedniego panelu
    private void redirectUser(User user) {
        try {
            String fxmlPath = user.getRole().equals("ADMIN")
                    ? "/com/election/view/admin_view.fxml"
                    : "/com/election/view/user_view.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (user.getRole().equals("ADMIN")) {
                AdminController controller = loader.getController();
                controller.initializeWithUser(user);
            } else {
                UserController controller = loader.getController();
                controller.initializeWithUser(user); // Przekaż użytkownika
            }

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

        } catch (IOException ex) {
            //noinspection CallToPrintStackTrace
            ex.printStackTrace();
            showError("Błąd ładowania widoku");
        }
    }

    // Wyświetla komunikat o błędzie
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #ff4444;");
    }

    // Resetuje formularz logowania
    @FXML
    private void handleRefresh(ActionEvent event) {
        usernameField.clear();
        passwordField.clear();
        errorLabel.setText("");
    }
}