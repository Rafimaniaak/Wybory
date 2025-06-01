package com.election.controller;

import com.election.dao.UserDAO;
import com.election.model.User;
import com.election.service.AuthService;
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

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();
    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        loadUserCountAsync();
    }

    private void loadUserCountAsync() {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return userDAO.getAllUsers().size();
            }
        };

//        task.setOnSucceeded(e -> {
//            if (task.getValue() > 0) {
//                statusLabel.setText("Zarejestrowanych użytkowników: " + task.getValue());
//            } else {
//                statusLabel.setText("Brak zarejestrowanych użytkowników");
//            }
//        });

        new Thread(task).start();
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        performLogin();
    }

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
            showError("Błąd połączenia z bazą danych");
        });

        new Thread(loginTask).start();
    }

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

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #ff4444;");
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        usernameField.clear();
        passwordField.clear();
        errorLabel.setText("");
    }
}