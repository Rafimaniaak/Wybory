package com.election.controller;

import com.election.HashGenerator;
import com.election.dao.CandidateDAO;
import com.election.dao.UserDAO;
import com.election.exception.DatabaseException;
import com.election.exception.ValidationException;
import com.election.model.CandidateResult;
import com.election.model.User;
import com.election.service.ElectionService;
import com.election.service.ExportServicePDF;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.rmi.server.ExportException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Kontroler panelu administracyjnego.
 * Obsługuje zarządzanie użytkownikami, wyświetlanie wyników i narzędzia.
 */

public class AdminController {
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ElectionService electionService = new ElectionService();
    private final ObservableList<CandidateResult> candidatesData = FXCollections.observableArrayList();
    private User currentAdmin;
    private User currentEditUser;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> firstNameColumn;
    @FXML private TableColumn<User, String> lastNameColumn;
    @FXML private TableColumn<User, String> peselColumn;
    @FXML private GridPane userFormGrid;
    @FXML private GridPane identifierGrid;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField peselField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private TableView<CandidateResult> resultsTable;
    @FXML private TableColumn<CandidateResult, String> candidateColumn;
    @FXML private TableColumn<CandidateResult, Number> votesColumn;

    @FXML private BarChart<String, Number> resultsChart;
    @FXML private NumberAxis yAxis;

    @FXML private Label statusLabel;
    @FXML private TextField peselSearchField;

    // Pola dla uniwersalnego zarządzania użytkownikami
    @FXML private ComboBox<String> actionComboBox;
    @FXML private TextField identifierField;
    @FXML private Button actionButton;
    @FXML private Label userManagementStatus;

    private final ObservableList<User> masterUserList = FXCollections.observableArrayList();

    // Inicjalizuje komponenty UI i wczytuje początkowe dane.
    @FXML
    public void initialize() {
        configureUserTable();
        configureResultsTable();
        loadInitialData();

        // Inicjalizacja ComboBox z rolami
        roleComboBox.setItems(FXCollections.observableArrayList("USER", "ADMIN"));

        // Listener dla ComboBox akcji
        actionComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            switch (newVal) {
                case "Dodaj użytkownika":
                    userFormGrid.setVisible(true);
                    identifierGrid.setVisible(false);
                    actionButton.setText("Dodaj");
                    clearUserForm();
                    break;

                case "Edytuj użytkownika":
                    userFormGrid.setVisible(true);
                    identifierGrid.setVisible(true);
                    actionButton.setText("Wyszukaj");
                    clearUserForm();
                    break;

                case "Usuń użytkownika":
                    userFormGrid.setVisible(false);
                    identifierGrid.setVisible(true);
                    actionButton.setText("Usuń");
                    break;
            }
        });
    }
    private void clearUserForm() {
        firstNameField.clear();
        lastNameField.clear();
        peselField.clear();
        usernameField.clear();
        passwordField.clear();
        roleComboBox.getSelectionModel().clearSelection();
        currentEditUser = null; // Resetuj przy czyszczeniu formularza
    }

    // Obsługuje logikę zarządzania użytkownikami (dodawanie/edycja/usuwanie).
    @FXML
    private void handleUserManagement() {
        try {
            String action = actionComboBox.getValue();
            switch (action) {
                case "Dodaj użytkownika": addNewUser(); break;
                case "Edytuj użytkownika": handleEditUserFlow(); break;
                case "Usuń użytkownika": deleteUser(); break;
            }
        } catch (ValidationException | DatabaseException e) {
            userManagementStatus.setText(e.getMessage());
        }
    }
    private void handleEditUserFlow() {
        if (actionButton.getText().equals("Wyszukaj")) {
            findUserForEdit();
        } else {
            updateUser();
        }
    }

    private void deleteUser() {
        String identifier = identifierField.getText().trim();
        if (identifier.isEmpty()) {
            userManagementStatus.setText("Wprowadź ID lub PESEL!");
            return;
        }

        try {
            User user = findUserByIdentifier(identifier);
            if (user == null) {
                throw new DatabaseException("Nie znaleziono użytkownika!", null);
            }

            if (confirmDeletion(user)) {
                userDAO.deleteUser(user);
                masterUserList.remove(user);
                refreshUserTable();
                userManagementStatus.setText("Użytkownik usunięty pomyślnie!");
            } else {
                userManagementStatus.setText("Anulowano usuwanie.");
            }
        } catch (DatabaseException e) {
            userManagementStatus.setText(e.getMessage());
        }
    }

    private boolean confirmDeletion(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potwierdzenie usunięcia");
        alert.setHeaderText("Czy na pewno chcesz usunąć użytkownika?");
        alert.setContentText(user.getFirstName() + " " + user.getLastName() + " (" + user.getUsername() + ")");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    private void addNewUser() throws ValidationException, DatabaseException {
        validateUserForm();

        User newUser = createUserFromForm();
        try {
            userDAO.saveUser(newUser);
            masterUserList.add(newUser);
            refreshUserTable();
            userManagementStatus.setText("Użytkownik dodany pomyślnie!");
            clearUserForm();
        } catch (Exception e) {
            throw new DatabaseException("Błąd podczas zapisu użytkownika: " + e.getMessage(), e);
        }
    }
    private User createUserFromForm() {
        User newUser = new User();
        newUser.setFirstName(firstNameField.getText().trim());
        newUser.setLastName(lastNameField.getText().trim());
        newUser.setPesel(peselField.getText().trim());
        newUser.setUsername(usernameField.getText().trim());
        newUser.setPassword(BCrypt.hashpw(passwordField.getText(), BCrypt.gensalt()));
        newUser.setRole(roleComboBox.getValue());
        newUser.setHasVoted(false);
        return newUser;
    }

    private void findUserForEdit() {
        String identifier = identifierField.getText().trim();
        currentEditUser = findUserByIdentifier(identifier);

        if (currentEditUser != null) {
            // Wypełnij formularz danymi użytkownika
            firstNameField.setText(currentEditUser.getFirstName());
            lastNameField.setText(currentEditUser.getLastName());
            peselField.setText(currentEditUser.getPesel());
            usernameField.setText(currentEditUser.getUsername());
            roleComboBox.setValue(currentEditUser.getRole());

            // Przygotuj do aktualizacji
            actionButton.setText("Zaktualizuj");
            userManagementStatus.setText("Znaleziono użytkownika. Edytuj dane.");
        } else {
            userManagementStatus.setText("Nie znaleziono użytkownika!");
        }
    }

    private void updateUser() throws ValidationException, DatabaseException {
        String identifier = identifierField.getText().trim();
        User user = findUserByIdentifier(identifier);

        if (user == null) {
            throw new DatabaseException("Nie znaleziono użytkownika!", null);
        }

        validateUserForm();
        updateUserData(user);

        try {
            userDAO.updateUser(user);
            refreshUserTable();
            userManagementStatus.setText("Dane użytkownika zaktualizowane!");
            actionButton.setText("Wyszukaj");
            clearUserForm();
        } catch (Exception e) {
            throw new DatabaseException("Błąd aktualizacji użytkownika: " + e.getMessage(), e);
        }
    }

    private void updateUserData(User user) {
        user.setFirstName(firstNameField.getText().trim());
        user.setLastName(lastNameField.getText().trim());
        user.setPesel(peselField.getText().trim());
        user.setUsername(usernameField.getText().trim());

        if (!passwordField.getText().isEmpty()) {
            user.setPassword(BCrypt.hashpw(passwordField.getText(), BCrypt.gensalt()));
        }

        user.setRole(roleComboBox.getValue());
    }

    private void validateUserForm() throws ValidationException {
        if (fieldsAreEmpty()) {
            throw new ValidationException("Wypełnij wszystkie pola!");
        }

        validatePesel();
        validateName(firstNameField.getText().trim(), "Imię");
        validateName(lastNameField.getText().trim(), "Nazwisko");
        validateUsername();
    }

    private boolean fieldsAreEmpty() {
        return firstNameField.getText().trim().isEmpty() ||
                lastNameField.getText().trim().isEmpty() ||
                peselField.getText().trim().isEmpty() ||
                usernameField.getText().trim().isEmpty() ||
                roleComboBox.getValue() == null;
    }

    private void validatePesel() throws ValidationException {
        String pesel = peselField.getText().trim();
        if (!pesel.matches("\\d{11}")) {
            throw new ValidationException("PESEL musi mieć 11 cyfr!");
        }

        User existing = userDAO.findByPesel(pesel);
        if (existing != null && (currentEditUser == null || !existing.getId().equals(currentEditUser.getId()))) {
            throw new ValidationException("PESEL już istnieje w systemie!");
        }
    }

    private void validateName(String name, String fieldName) throws ValidationException {
        if (!name.matches("[\\p{L}\\s\\-]+")) {
            throw new ValidationException(fieldName + " może zawierać tylko litery, spacje i myślniki!");
        }
    }

    private void validateUsername() throws ValidationException {
        String username = usernameField.getText().trim();
        if (!username.matches("[a-zA-Z0-9_]+")) {
            throw new ValidationException("Login może zawierać tylko litery, cyfry i podkreślniki!");
        }
    }
    private void refreshUserTable() {
        masterUserList.setAll(userDAO.getAllUsers());
        usersTable.refresh();
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
        masterUserList.setAll(userDAO.getAllUsers());
        usersTable.setItems(masterUserList);
        refreshUserTable();
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
    private void refreshResults() {
        refreshElectionData();
    }

    private void refreshElectionData() {
        statusLabel.setText("Odświeżanie wyników...");

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
                if (votes > maxVotes) maxVotes = votes;
            }
            resultsChart.getData().add(series);
            updateYAxisRange(maxVotes);

            statusLabel.setText("Wyniki zaktualizowane: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception e) {
            statusLabel.setText("Błąd podczas aktualizacji!");
            throw new DatabaseException("Błąd odświeżania wyników", e);
        }
    }

    private void updateYAxisRange(int maxVotes) {
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(maxVotes < 5 ? 5 : maxVotes + 1);
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/election/view/login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(loader.load()));
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

    private User findUserByIdentifier(String identifier) throws ValidationException {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }

        try {
            if (identifier.matches("\\d{11}")) {
                return userDAO.findByPesel(identifier);
            } else if (identifier.matches("\\d+")) {
                Long id = Long.parseLong(identifier);
                return userDAO.getUserById(id);
            } else {
                throw new ValidationException("Nieprawidłowy format identyfikatora. Podaj PESEL (11 cyfr) lub ID (liczba).");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Nieprawidłowy format ID: " + identifier);
        } catch (DatabaseException e) {
            throw e; // Przekazujemy dalej
        } catch (Exception e) {
            throw new DatabaseException("Błąd podczas wyszukiwania użytkownika", e);
        }
    }

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

    @FXML
    private void handleShowAllUsers() {
        usersTable.setItems(masterUserList);
        peselSearchField.clear();
    }

    @FXML
    private void handleExportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz plik CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(resultsTable.getScene().getWindow());

        if (file != null) {
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8)) {

                writer.write('\uFEFF'); // BOM dla UTF-8
                writer.write("Kandydat;Liczba głosów\n");

                for (CandidateResult result : resultsTable.getItems()) {
                    writer.write(result.getName() + ";" + result.getVotes() + "\n");
                }

                writer.flush();
            } catch (IOException e) {
                showError("Błąd zapisu CSV: " + e.getMessage());
            }
        }
    }

//    Eksportuje wyniki wyborów do formatu PDF. @throws ExportException w przypadku błędu eksportu
    @FXML
    private void handleExportToPDF() throws ExportException {
        try {
            ExportServicePDF.exportToPDF(candidateDAO.getAllCandidates(), "wyniki.pdf");
            statusLabel.setText("Zapisano wyniki do PDF");
        } catch (Exception e) {
            statusLabel.setText("Błąd eksportu do PDF");
            throw new ExportException("Błąd podczas eksportu do PDF", e);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd");
        alert.setHeaderText("Wystąpił błąd");
        alert.setContentText(message);
        alert.showAndWait();
    }
}