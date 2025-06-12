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
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
// Kontroler ekranu logowania
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();
    private final AuthService authService = new AuthService();

    // Inicjalizuje kontroler i ładuje dane
    @FXML
    private void initialize() {
        loadUserCountAsync();
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
                    showError("Nieprawidłowy login!");
                    break;
                case INVALID_PASSWORD:
                    showError("Nieprawidłowe hasło!");
                    break;
            }
        });

        loginTask.setOnFailed(e -> {
            showError("Błąd połączenia z bazą danych");
        });

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
            ex.printStackTrace();
            showError("Błąd ładowania widoku");
        }
    }

    // Wyświetla komunikat błędu
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