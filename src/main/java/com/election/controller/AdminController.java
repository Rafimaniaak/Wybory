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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
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
import java.util.Objects;
import java.util.Optional;

// Kontroler panelu administratora
public class AdminController {
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ElectionService electionService = new ElectionService();
    private final ObservableList<CandidateResult> candidatesData = FXCollections.observableArrayList();
    public CategoryAxis xAxis;
    public Button refreshButton;
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
    @FXML private TextField peselSearchField;

    @FXML private BarChart<String, Number> resultsChart;
    @FXML private NumberAxis yAxis;

    @FXML private Label statusLabel;
    //@FXML private TextField peselSearchField;
    @FXML private TextField peselIdentifierField;

    // Pola dla uniwersalnego zarządzania użytkownikami
    @FXML private ComboBox<String> actionComboBox;
    @FXML private TextField identifierField;
    @FXML private Button actionButton;
    @FXML private Label userManagementStatus;

    @FXML private RadioButton idRadioButton;
    @FXML private RadioButton peselRadioButton;
    @FXML private TextField idField;

    private final ObservableList<User> masterUserList = FXCollections.observableArrayList();

    // Konfiguruje interfejs użytkownika i ładuje dane początkowe
    @FXML
    public void initialize() {
        configureUserTable();
        configureResultsTable();
        //loadInitialData();

        // Inicjalizacja ComboBox z rolami
        roleComboBox.setItems(FXCollections.observableArrayList("USER", "ADMIN"));
        roleComboBox.setValue("USER");
        // Ustawienie odstępu między słupkami wykresu
        resultsChart.setBarGap(5);
        // Grupa dla przycisków radiowych
        ToggleGroup searchGroup = new ToggleGroup();
        idRadioButton.setToggleGroup(searchGroup);
        peselRadioButton.setToggleGroup(searchGroup);
        idRadioButton.setSelected(true);
        // Listener dla ComboBox akcji
        actionComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            handleRefreshForm();
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

    public void refreshUserTab() {
        List<User> users = userDAO.getAllUsers();
        masterUserList.setAll(users);
        usersTable.refresh();
    }

    @FXML
    public void handleUserTabSelected(Event event) {
        if (((Tab) event.getSource()).isSelected()) {
            refreshUserTable();
        }
    }

    // Czyści formularz użytkownika
    private void clearUserForm() {
        firstNameField.clear();
        lastNameField.clear();
        peselField.clear();
        usernameField.clear();
        passwordField.clear();
        //roleComboBox.getSelectionModel().clearSelection();
        currentEditUser = null; // Resetuj przy czyszczeniu formularza
    }

    // Obsługuje główną logikę zarządzania użytkownikami
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

    // Zarządza przepływem edycji użytkownika
    private void handleEditUserFlow() {
        if (actionButton.getText().equals("Wyszukaj")) {
            findUserForEdit();
        } else {
            updateUser();
        }
    }

    // Usuwa użytkownika z systemu
    private void deleteUser() {
        String identifier = getIdentifier();

        try {
            User user = findUserByIdentifier(identifier);
            if (user == null) {
                throw new DatabaseException("Nie znaleziono użytkownika!", null);
            }

            // Sprawdź, czy użytkownik jest administratorem
            if ("ADMIN".equals(user.getRole())) {
                // Pobierz wszystkich administratorów
                List<User> admins = userDAO.getAllUsers().stream()
                        .filter(u -> "ADMIN".equals(u.getRole()))
                        .toList();

                // Blokuj usuwanie, jeśli jest tylko jeden admin
                if (admins.size() <= 1) {
                    userManagementStatus.setText("Nie można usunąć ostatniego administratora!");
                    return;
                }
            }

            if (confirmDeletion(user)) {
                userDAO.deleteUser(user);
                masterUserList.remove(user);
                refreshUserTable();
                userManagementStatus.setText("Użytkownik usunięty pomyślnie!");
                handleRefreshForm();
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
            handleRefreshForm();
        } catch (Exception e) {
            throw new DatabaseException("Błąd podczas zapisu użytkownika: " + e.getMessage(), e);
        }
    }
    // Tworzy obiekt użytkownika z danych formularza
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

    // Nowa metoda do pobierania identyfikatora
    private String getIdentifier() {
        if (idRadioButton.isSelected()) {
            return idField.getText().trim();
        } else {
            return peselIdentifierField.getText().trim();
        }
    }

    // Wyszukuje użytkownika do edycji
    private void findUserForEdit() {
        String identifier = getIdentifier();

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

    // Aktualizuje dane użytkownika w systemie
    private void updateUser() throws ValidationException, DatabaseException {
        String identifier = identifierField.getText().trim();
        User user = findUserByIdentifier(identifier);

        // Sprawdź, czy zmieniamy rolę ostatniego admina
        if ("ADMIN".equals(user.getRole()) && "USER".equals(roleComboBox.getValue())) {
            List<User> admins = userDAO.getAllUsers().stream()
                    .filter(u -> "ADMIN".equals(u.getRole()))
                    .toList();

            if (admins.size() <= 1) {
                throw new ValidationException("Nie można zmienić roli ostatniego administratora!");
            }
        }

        validateUserForm();
        updateUserData(user);

        try {
            userDAO.updateUser(user);
            refreshUserTable();
            userManagementStatus.setText("Dane użytkownika zaktualizowane!");
            actionButton.setText("Wyszukaj");
            clearUserForm();
            handleRefreshForm();
        } catch (Exception e) {
            throw new DatabaseException("Błąd aktualizacji użytkownika: " + e.getMessage(), e);
        }
    }
    // Aktualizuje właściwości obiektu użytkownika
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

    // Waliduje dane w formularzu użytkownika
    private void validateUserForm() throws ValidationException {
        if (fieldsAreEmpty()) {
            throw new ValidationException("Wypełnij wszystkie pola!");
        }
        validatePesel();
        validateName(firstNameField.getText().trim(), "Imię");
        validateName(lastNameField.getText().trim(), "Nazwisko");
        validateUsername();
        // Walidacja hasła dla nowego użytkownika
        if (actionComboBox.getValue().equals("Dodaj użytkownika")) {
            if (passwordField.getText().trim().isEmpty()) {
                throw new ValidationException("Hasło nie może być puste!");
            }
        }
        // Walidacja dla edycji i usuwania
        if (actionComboBox.getValue().equals("Edytuj użytkownika")
                || actionComboBox.getValue().equals("Usuń użytkownika")) {
            if (!idRadioButton.isSelected() && !peselRadioButton.isSelected()) {
                throw new ValidationException("Wybierz metodę identyfikacji użytkownika!");
            }
        }
    }

    // Sprawdza, czy wymagane pola są wypełnione
    private boolean fieldsAreEmpty() {
        boolean basicFieldsEmpty = firstNameField.getText().trim().isEmpty() ||
                lastNameField.getText().trim().isEmpty() ||
                peselField.getText().trim().isEmpty() ||
                usernameField.getText().trim().isEmpty() ||
                roleComboBox.getValue() == null;

        return basicFieldsEmpty ||
                (actionComboBox.getValue().equals("Dodaj użytkownika") &&
                        passwordField.getText().trim().isEmpty());
    }

    // Waliduje numer PESEL
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

    // Waliduje imię/nazwisko
    private void validateName(String name, String fieldName) throws ValidationException {
        if (!name.matches("[\\p{L}\\s\\-]+")) {
            throw new ValidationException(fieldName + " może zawierać tylko litery, spacje i myślniki!");
        }
    }

    // Waliduje nazwę użytkownika
    private void validateUsername() throws ValidationException {
        String username = usernameField.getText().trim();
        if (!username.matches("[a-zA-Z0-9_]+")) {
            throw new ValidationException("Login może zawierać tylko litery, cyfry i podkreślniki!");
        }
    }

    // Odświeża widok tabeli użytkowników
    private void refreshUserTable() {
        try {
            List<User> users = userDAO.getAllUsers();
            masterUserList.setAll(users);
            usersTable.refresh();
        } catch (DatabaseException e) {
            userManagementStatus.setText("Błąd ładowania danych: " + e.getMessage());
        }
    }

    // Konfiguruje kolumny tabeli użytkowników
    private void configureUserTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        peselColumn.setCellValueFactory(new PropertyValueFactory<>("pesel"));

        // Ustawienie źródła danych
        usersTable.setItems(masterUserList);
    }

    @FXML private TableColumn<CandidateResult, String> partyColumn;
    // Konfiguruje tabelę wyników
    private void configureResultsTable() {
        candidateColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        partyColumn.setCellValueFactory(new PropertyValueFactory<>("party"));
        votesColumn.setCellValueFactory(new PropertyValueFactory<>("votes"));

        votesColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : String.format("%,d", value.intValue()));
            }
        });
    }

    // Ładuje początkowe dane do widoków
    private void loadInitialData() {
        masterUserList.setAll(userDAO.getAllUsers());
        usersTable.setItems(masterUserList);
        refreshUserTable();
        refreshElectionData();
    }

    // Inicjalizuje kontroler z danymi zalogowanego administratora
    public void initializeWithUser(User adminUser) {
        this.currentAdmin = adminUser;
        logAdminAccess();

        Platform.runLater(() -> {
            refreshUserTable();  // Odśwież dane użytkowników
            refreshElectionData();  // Odśwież wyniki wyborów
        });
    }

    // Loguje czas logowania administratora
    private void logAdminAccess() {
        System.out.println("Administrator " + currentAdmin.getUsername()
                + " zalogowany o " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    // Odświeża widok wyników wyborów
    @FXML
    private void refreshResults() {
        refreshElectionData();
    }

    // Pobiera i aktualizuje dane wyborcze
    private void refreshElectionData() {
        statusLabel.setText("Odświeżanie wyników...");

        try {
            List<CandidateResult> results = electionService.getCurrentResults();
            candidatesData.setAll(results);
            resultsTable.setItems(candidatesData);

            // Usuń stare dane i zresetuj wykres
            resultsChart.getData().clear();
            resultsChart.setAnimated(false); // Wyłącz animacje dla stabilności
            resultsChart.layout(); // Wymuś przeliczenie układu

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            int maxVotes = 0;

            for (CandidateResult result : results) {
                int votes = result.getVotes();
                String party = result.getParty();
                if (party != null && party.length() > 15) {
                    party = party.substring(0, 12) + "...";
                }
                String candidateLabel = result.getName() + "\n(" + (party != null ? party : "brak") + ")";
                XYChart.Data<String, Number> data = new XYChart.Data<>(candidateLabel, votes);
                series.getData().add(data);
                if (votes > maxVotes) maxVotes = votes;
            }

            resultsChart.getData().add(series);
            updateYAxisRange(maxVotes);

            // Ustawienia osi
            xAxis.setTickLabelRotation(0);
            xAxis.setTickLabelFont(Font.font("System", 8));
            resultsChart.setBarGap(10);

            // Nowe: bezpośrednie ustawienie etykiet
            Platform.runLater(() -> {
                // Wymuś natychmiastowe przeliczenie układu

                resultsChart.applyCss();
                resultsChart.layout();

                // Popraw pozycjonowanie etykiet
                fixLabelPositions();
            });

            statusLabel.setText("Wyniki zaktualizowane: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception e) {
            statusLabel.setText("Błąd podczas aktualizacji!");
            throw new DatabaseException("Błąd odświeżania wyników", e);
        }
    }

    // Nowa metoda do poprawiania pozycji etykiet
    private void fixLabelPositions() {
        for (int i = 0; i < resultsChart.getData().size(); i++) {
            XYChart.Series<String, Number> series = resultsChart.getData().get(i);
            for (int j = 0; j < series.getData().size(); j++) {
                XYChart.Data<String, Number> data = series.getData().get(j);
                Node node = data.getNode();
                if (node != null) {
                    // Popraw pozycjonowanie słupka
                    node.relocate(node.getLayoutX() - 5, node.getLayoutY());
                }

                // Popraw etykiety na osi
                if (j < xAxis.getCategories().size()) {
                    String category = xAxis.getCategories().get(j);
                    for (Node axisNode : xAxis.getChildrenUnmodifiable()) {
                        if (axisNode instanceof Text) {
                            Text text = (Text) axisNode;
                            if (category.equals(text.getText())) {
                                text.setTextAlignment(TextAlignment.CENTER);
                                text.setWrappingWidth(80);
                                text.setLayoutY(text.getLayoutY() - 5); // Delikatne podniesienie
                            }
                        }
                    }
                }
            }
        }
    }

    // Aktualizuje skalę osi Y na wykresie
    private void updateYAxisRange(int maxVotes) {
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(maxVotes < 5 ? 5 : maxVotes + 1);
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
    }

    // Obsługuje wylogowywanie użytkownika
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/election/view/login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(loader.load()));
            loginStage.setTitle("Logowanie");
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/app_icon.png")));
            loginStage.getIcons().add(icon);
            loginStage.show();
        } catch (IOException e) {
            showErrorAlert("Błąd logowania", e.getMessage());
        }
    }

    // Otwiera generator hashów
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

    // Wyświetla alert błędu
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Wyszukuje użytkownika po identyfikatorze
    private User findUserByIdentifier(String identifier) throws ValidationException {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }

        try {
            if (peselRadioButton.isSelected()) {
                return userDAO.findByPesel(identifier);
            } else if (idRadioButton.isSelected()) {
                Long id = Long.parseLong(identifier);
                return userDAO.getUserById(id);
            } else {
                throw new ValidationException("Wybierz metodę wyszukiwania!");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Nieprawidłowy format ID: " + identifier);
        } catch (DatabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseException("Błąd podczas wyszukiwania użytkownika", e);
        }
    }

    // Filtruje użytkowników po numerze PESEL
    @FXML
    private void handlePeselSearch() {
        String pesel = peselSearchField.getText().trim();

        // Pobierz aktualną listę użytkowników z bazy danych
        List<User> currentUsers = userDAO.getAllUsers();
        ObservableList<User> currentList = FXCollections.observableArrayList(currentUsers);

        if (pesel.isEmpty()) {
            usersTable.setItems(currentList);
            return;
        }

        // Filtruj na aktualnej liście
        ObservableList<User> filtered = currentList.filtered(user ->
                user.getPesel() != null && user.getPesel().contains(pesel)
        );

        usersTable.setItems(filtered);
    }


    @FXML
    private void handleRefreshForm() {
        clearUserForm();
        clearIdentifierFields();
        actionButton.setText("Wyszukaj");
        userManagementStatus.setText("");
    }
    private void clearIdentifierFields() {
        idField.clear();
        peselIdentifierField.clear(); // To pole w sekcji identyfikatora
    }
    // Resetuje filtr w tabeli użytkowników
    @FXML
    private void handleShowAllUsers() {
        usersTable.setItems(masterUserList);
        peselSearchField.clear();
    }

    // Eksportuje wyniki do pliku CSV
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

    // Eksportuje wyniki do pliku PDF
    @FXML
    private void handleExportToPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz plik PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));

        // Ustaw domyślną nazwę pliku z datą
        String defaultFileName = "wyniki_wyborow_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";
        fileChooser.setInitialFileName(defaultFileName);

        File file = fileChooser.showSaveDialog(resultsTable.getScene().getWindow());

        if (file != null) {
            try {
                ExportServicePDF.exportToPDF(candidateDAO.getAllCandidates(), file.getAbsolutePath());
                statusLabel.setText("Zapisano wyniki do: " + file.getName());
            } catch (Exception e) {
                statusLabel.setText("Błąd eksportu do PDF");
                showError("Błąd podczas eksportu do PDF: " + e.getMessage());
            }
        }
    }

    // Wyświetla okno błędu
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd");
        alert.setHeaderText("Wystąpił błąd");
        alert.setContentText(message);
        alert.showAndWait();
    }
}