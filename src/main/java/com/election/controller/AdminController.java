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
import java.util.stream.Collectors;

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

    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label addUserStatusLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField peselField;

    @FXML private TextField editUserIdField;
    @FXML private TextField editUsernameField;
    @FXML private TextField editPasswordField;
    @FXML private ComboBox<String> editRoleComboBox;
    @FXML private Label editUserStatusLabel;
    @FXML private TextField editFirstNameField;
    @FXML private TextField editLastNameField;
    @FXML private TextField editPeselField;

    @FXML private TextField deleteUserIdField;
    @FXML private Label deleteUserStatusLabel;

    @FXML private TextField searchUsernameField;
    @FXML private TextField searchFirstNameField;
    @FXML private TextField searchLastNameField;
    @FXML private TextField searchPeselField;

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
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        peselColumn.setCellValueFactory(new PropertyValueFactory<>("pesel"));
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
        List<User> users = userDAO.getAllUsers();
        masterUserList.setAll(users);
        usersTable.setItems(masterUserList);
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

    @FXML
    private void handleAddUser(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleComboBox.getValue();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String pesel = peselField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || role == null || firstName.isEmpty() || lastName.isEmpty() || pesel.isEmpty()) {
            addUserStatusLabel.setText("Wszystkie pola są wymagane!");
            return;
        }

        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(role);
            user.setHasVoted(false);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPesel(pesel);

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
            String newFirstName = editFirstNameField.getText().trim();
            String newLastName = editLastNameField.getText().trim();
            String newPesel = editPeselField.getText().trim();

            if (!newUsername.isEmpty()) user.setUsername(newUsername);
            if (!newPassword.isEmpty()) user.setPassword(newPassword);
            if (newRole != null) user.setRole(newRole);
            if (!newFirstName.isEmpty()) user.setFirstName(newFirstName);
            if (!newLastName.isEmpty()) user.setLastName(newLastName);
            if (!newPesel.isEmpty()) user.setPesel(newPesel);

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

    @FXML
    private void handleSearchUsers(ActionEvent event) {
        String username = searchUsernameField.getText().trim().toLowerCase();
        String firstName = searchFirstNameField.getText().trim().toLowerCase();
        String lastName = searchLastNameField.getText().trim().toLowerCase();
        String pesel = searchPeselField.getText().trim();

        List<User> allUsers = userDAO.getAllUsers();

        List<User> filteredUsers = allUsers.stream()
                .filter(user -> (username.isEmpty() || user.getUsername().toLowerCase().contains(username)) &&
                        (firstName.isEmpty() || user.getFirstName().toLowerCase().contains(firstName)) &&
                        (lastName.isEmpty() || user.getLastName().toLowerCase().contains(lastName)) &&
                        (pesel.isEmpty() || user.getPesel().contains(pesel)))
                .collect(Collectors.toList());

        usersTable.setItems(FXCollections.observableArrayList(filteredUsers));
    }

    @FXML
    private void handleClearSearch(ActionEvent event) {
        searchUsernameField.clear();
        searchFirstNameField.clear();
        searchLastNameField.clear();
        searchPeselField.clear();
        usersTable.setItems(FXCollections.observableArrayList(userDAO.getAllUsers()));
    }
    @FXML
    private TextField peselSearchField;

    @FXML private TableColumn<User, String> firstNameColumn;
    @FXML private TableColumn<User, String> lastNameColumn;
    @FXML private TableColumn<User, String> peselColumn;

    private ObservableList<User> masterUserList = FXCollections.observableArrayList();

    @FXML
    private void handlePeselSearch() {
        String pesel = peselSearchField.getText().trim();
        if (pesel.isEmpty()) {
            usersTable.setItems(masterUserList);
            return;
        }

        ObservableList<User> filtered = masterUserList.filtered(user ->
                user.getPesel() != null && user.getPesel().contains(pesel)
        );
        usersTable.setItems(filtered);
    }


    private void clearAddUserForm() {
        usernameField.clear();
        passwordField.clear();
        roleComboBox.getSelectionModel().clearSelection();
        firstNameField.clear();
        lastNameField.clear();
        peselField.clear();
    }
    @FXML
    private void handleShowAllUsers() {
        usersTable.setItems(masterUserList);
        peselSearchField.clear();
    }

}
