package com.election.controller;

import com.election.HashGenerator;
import com.election.dao.UserDAO;
import com.election.model.CandidateResult;
import com.election.model.User;
import com.election.service.ElectionService;
import javafx.application.Platform;
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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;

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

    @FXML private BarChart<String, Number> resultsChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

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
        editRoleComboBox.setItems(FXCollections.observableArrayList("USER", "ADMIN"));

        xAxis.setTickLabelFill(Color.WHITE);
        xAxis.setLabel("Kandydat");

        yAxis.setTickLabelFill(Color.WHITE);
        yAxis.setLabel("Głosy");

        resultsChart.setTitle("Głosy wg kandydata");

        Platform.runLater(() -> {
            Node title = resultsChart.lookup(".chart-title");
            if (title != null) title.setStyle("-fx-text-fill: white;");

            Node xAxisLabel = xAxis.lookup(".axis-label");
            if (xAxisLabel != null) xAxisLabel.setStyle("-fx-text-fill: white;");

            Node yAxisLabel = yAxis.lookup(".axis-label");
            if (yAxisLabel != null) yAxisLabel.setStyle("-fx-text-fill: white;");
        });
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

            resultsChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            int maxVotes = 0;

            for (CandidateResult result : results) {
                int votes = result.getVotes();
                series.getData().add(new XYChart.Data<>(result.getName(), votes));
                if (votes > maxVotes) {
                    maxVotes = votes;
                }
            }
            resultsChart.getData().add(series);

            updateYAxisRange(maxVotes);

            if (statusLabel != null) {
                statusLabel.setText("Wyniki zaktualizowane: " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }

        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("Błąd podczas aktualizacji!");
            }
            e.printStackTrace();
        }
    }

    private void updateYAxisRange(int maxVotes) {
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(maxVotes < 5 ? 5 : maxVotes + 1);
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override
            public String toString(Number object) {
                return String.valueOf(object.intValue());
            }
        });
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

    @FXML private TextField editUserIdField;
    @FXML private TextField editUsernameField;
    @FXML private TextField editPasswordField;
    @FXML private ComboBox<String> editRoleComboBox;
    @FXML private Label editUserStatusLabel;

    @FXML private TextField deleteUserIdField;
    @FXML private Label deleteUserStatusLabel;

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
            user.setPassword(password);
            user.setRole(role);
            user.setHasVoted(false);

            userDAO.saveUser(user);
            usersTable.getItems().add(user);
            addUserStatusLabel.setText("Użytkownik dodany pomyślnie.");
            clearAddUserForm();
        } catch (Exception e) {
            addUserStatusLabel.setText("Błąd: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditUser(ActionEvent event) {
        try {
            long id = Long.parseLong(editUserIdField.getText().trim());
            User user = userDAO.getUserById(id);
            if (user == null) {
                editUserStatusLabel.setText("Nie znaleziono użytkownika.");
                return;
            }

            String newUsername = editUsernameField.getText().trim();
            String newPassword = editPasswordField.getText().trim();
            String newRole = editRoleComboBox.getValue();

            if (!newUsername.isEmpty()) user.setUsername(newUsername);
            if (!newPassword.isEmpty()) user.setPassword(newPassword);
            if (newRole != null) user.setRole(newRole);

            userDAO.updateUser(user);
            usersTable.getItems().setAll(userDAO.getAllUsers());
            editUserStatusLabel.setText("Zmieniono dane użytkownika.");
        } catch (NumberFormatException e) {
            editUserStatusLabel.setText("Nieprawidłowe ID.");
        } catch (Exception e) {
            editUserStatusLabel.setText("Błąd: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteUser(ActionEvent event) {
        try {
            long id = Long.parseLong(deleteUserIdField.getText().trim());
            User user = userDAO.getUserById(id);
            if (user == null) {
                deleteUserStatusLabel.setText("Nie znaleziono użytkownika.");
                return;
            }

            userDAO.deleteUser(user);
            usersTable.getItems().removeIf(u -> u.getId() == id);
            deleteUserStatusLabel.setText("Użytkownik usunięty.");
        } catch (NumberFormatException e) {
            deleteUserStatusLabel.setText("Nieprawidłowe ID.");
        } catch (Exception e) {
            deleteUserStatusLabel.setText("Błąd: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearAddUserForm() {
        usernameField.clear();
        passwordField.clear();
        roleComboBox.getSelectionModel().clearSelection();
    }
}
