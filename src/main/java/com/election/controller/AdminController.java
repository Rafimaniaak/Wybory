package com.election.controller;

import com.election.HashGenerator;
import com.election.dao.CandidateDAO;
import com.election.dao.UserDAO;
import com.election.exception.DatabaseException;
import com.election.exception.ValidationException;
import com.election.model.Candidate;
import com.election.model.CandidateResult;
import com.election.model.User;
import com.election.service.ElectionService;
import com.election.service.ExportServicePDF;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
    private boolean passwordChanged = false; // Flaga śledząca zmianę hasła

    @FXML private TableColumn<CandidateResult, String> percentColumn;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> firstNameColumn;
    @FXML private TableColumn<User, String> lastNameColumn;
    @FXML private TableColumn<User, String> peselColumn;
    @FXML private GridPane userFormGrid;
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
    @FXML private Label userManagementStatus;

    @FXML private TableView<Candidate> candidatesTable;
    @FXML private TableColumn<Candidate, Long> candidateIdColumn;
    @FXML private TableColumn<Candidate, String> candidateNameColumn;
    @FXML private TableColumn<Candidate, String> candidatePartyColumn;
    @FXML private TableColumn<Candidate, Integer> candidateVotesColumn;
    @FXML private TextField candidateNameField;
    @FXML private TextField candidatePartyField;
    @FXML private Label candidateStatusLabel;

    // Nowe pola dla filtrów
    @FXML private TextField firstNameFilter;
    @FXML private TextField lastNameFilter;
    @FXML private TextField peselFilter;
    @FXML private ComboBox<String> roleFilterComboBox;
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

        // Oznacza, że hasło mogło zostać zmienione (jeśli nie jest puste)
        if (!getPassword().isEmpty()) {
            passwordChanged = true;
        }
    }

    private String getPassword() {
        return passwordField.isVisible() ?
                passwordField.getText() :
                visiblePasswordField.getText();
    }

    private final ObservableList<Candidate> candidatesList = FXCollections.observableArrayList();
    private final ObservableList<User> masterUserList = FXCollections.observableArrayList();
    private final FilteredList<User> filteredUsers = new FilteredList<>(masterUserList);

    // Konfiguruje interfejs użytkownika i ładuje dane początkowe
    @FXML
    public void initialize() {
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
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList("USER", "ADMIN"));
            roleComboBox.setValue("USER");
        }

        // Inicjalizacja filtrów
        roleFilterComboBox.setItems(FXCollections.observableArrayList("ADMIN", "USER"));

        configureUserTable();
        configureResultsTable();
        configureCandidatesTable();
        refreshCandidatesTable();

        // Ustawienie filtrowanej listy
        usersTable.setItems(filteredUsers);

        // Listenery do automatycznego filtrowania przy zmianie wartości
        firstNameFilter.textProperty().addListener((obs, old, newVal) -> handleFilterUsers());
        lastNameFilter.textProperty().addListener((obs, old, newVal) -> handleFilterUsers());
        peselFilter.textProperty().addListener((obs, old, newVal) -> handleFilterUsers());
        roleFilterComboBox.valueProperty().addListener((obs, old, newVal) -> handleFilterUsers());

        if (resultsChart != null) {
            resultsChart.setBarGap(1);
        }

        // Ustaw podpowiedź dla pola hasła tylko wtedy, gdy użytkownik jest wybrany
        passwordField.setPromptText(null);
        visiblePasswordField.setPromptText(null);

        // Ustaw tooltip
        Tooltip.install(showPasswordButton, new Tooltip("Pokaż/ukryj hasło"));

        // Upewnij się, że oba pola mają ten sam rozmiar
        visiblePasswordField.prefWidthProperty().bind(passwordField.widthProperty());
        visiblePasswordField.minWidthProperty().bind(passwordField.minWidthProperty());
        visiblePasswordField.maxWidthProperty().bind(passwordField.maxWidthProperty());

        // Dodaj nasłuchiwanie zmian w polach hasła
        addPasswordListeners();
    }

    private void addPasswordListeners() {
        // Nasłuchuj zmian w polach hasła
        ChangeListener<String> passwordChangeListener = (obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                passwordChanged = true;
            }
        };

        passwordField.textProperty().addListener(passwordChangeListener);
        visiblePasswordField.textProperty().addListener(passwordChangeListener);
    }

    @FXML
    private void handleFilterUsers() {
        filteredUsers.setPredicate(this::matchUser);
    }

    @FXML
    private void handleClearFilters() {
        firstNameFilter.clear();
        lastNameFilter.clear();
        peselFilter.clear();
        roleFilterComboBox.getSelectionModel().clearSelection();
        filteredUsers.setPredicate(null); // Resetuj filtr
    }

    // Metoda sprawdzająca, czy użytkownik spełnia kryteria filtrowania
    private boolean matchUser(User user) {
        // Sprawdź imię
        if (!firstNameFilter.getText().isEmpty() &&
                !user.getFirstName().toLowerCase().contains(firstNameFilter.getText().toLowerCase())) {
            return false;
        }

        // Sprawdź nazwisko
        if (!lastNameFilter.getText().isEmpty() &&
                !user.getLastName().toLowerCase().contains(lastNameFilter.getText().toLowerCase())) {
            return false;
        }

        // Sprawdź PESEL
        if (!peselFilter.getText().isEmpty() &&
                !user.getPesel().contains(peselFilter.getText())) {
            return false;
        }

        // Sprawdź rolę
        if (roleFilterComboBox.getValue() != null &&
                !user.getRole().equals(roleFilterComboBox.getValue())) {
            return false;
        }

        return true;
    }

    public void refreshUserTab() {
        List<User> users = userDAO.getAllUsers();
        masterUserList.setAll(users);
        usersTable.refresh();
    }

    private void configureCandidatesTable() {
        candidateIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        candidateNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        candidatePartyColumn.setCellValueFactory(new PropertyValueFactory<>("party"));
        candidateVotesColumn.setCellValueFactory(new PropertyValueFactory<>("votes"));

        candidatesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        candidateNameField.setText(newSelection.getName());
                        candidatePartyField.setText(newSelection.getParty());
                    }
                });
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
        visiblePasswordField.clear();
        roleComboBox.setValue("USER"); // Ustaw domyślną wartość

        // Usuń podpowiedzi
        passwordField.setPromptText(null);
        visiblePasswordField.setPromptText(null);
    }

    // Czyści formularz i resetuje zaznaczenie
    @FXML
    private void handleClearUserForm() {
        clearUserForm();
        usersTable.getSelectionModel().clearSelection();
        currentEditUser = null;
        passwordChanged = false; // Resetuj flagę zmiany hasła
        userManagementStatus.setText("");
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

        // Sprawdź, czy login już istnieje
        User existing = userDAO.findByUsername(username);
        if (existing != null && (currentEditUser == null || !existing.getId().equals(currentEditUser.getId()))) {
            throw new ValidationException("Login już istnieje w systemie!");
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

        // Listener do wypełniania formularza po wybraniu użytkownika
        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        fillUserForm(newSelection);
                    }
                });
    }

    private void fillUserForm(User user) {
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        peselField.setText(user.getPesel());
        usernameField.setText(user.getUsername());
        roleComboBox.setValue(user.getRole());
        passwordField.clear(); // Nie pokazujemy hasła
        visiblePasswordField.clear();
        passwordChanged = false; // Resetuj flagę zmiany hasła

        // Ustaw podpowiedź dla hasła
        passwordField.setPromptText("(wpisz aby zmienić)");
        visiblePasswordField.setPromptText("(wpisz aby zmienić)");

        currentEditUser = user;
    }

    @FXML private TableColumn<CandidateResult, String> partyColumn;
    // Konfiguruje tabelę wyników
    private void configureResultsTable() {
        candidateColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        partyColumn.setCellValueFactory(new PropertyValueFactory<>("party"));
        votesColumn.setCellValueFactory(new PropertyValueFactory<>("votes"));

        if (votesColumn != null) {
            votesColumn.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(Number value, boolean empty) {
                    super.updateItem(value, empty);
                    setText(empty || value == null ? "" : String.format("%,d", value.intValue()));
                }
            });
        }
        // Dodajemy kolumnę z procentami
        percentColumn.setCellValueFactory(cellData -> {
            CandidateResult candidate = cellData.getValue();
            int totalVotes = candidatesData.stream().mapToInt(CandidateResult::getVotes).sum();
            double percent = totalVotes > 0 ? (candidate.getVotes() * 100.0) / totalVotes : 0;
            return new SimpleStringProperty(String.format("%.1f%%", percent));
        });

        // Formatowanie komórek
        percentColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                setAlignment(Pos.CENTER);
            }
        });
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
            // Pobierz i posortuj wyniki malejąco
            List<CandidateResult> results = electionService.getCurrentResults();
            results.sort((r1, r2) -> r2.getVotes() - r1.getVotes());
            candidatesData.setAll(results);
            resultsTable.setItems(candidatesData);

            // Oblicz sumę głosów
            int totalVotes = results.stream().mapToInt(CandidateResult::getVotes).sum();

            // Usuń stare dane i zresetuj wykres
            resultsChart.getData().clear();
            resultsChart.setAnimated(false); // Wyłącz animacje dla stabilności
            resultsChart.layout(); // Wymuś przeliczenie układu


            XYChart.Series<String, Number> series = new XYChart.Series<>();
            int maxVotes = 0;

            // Ustal paletę kolorów dla słupków
            String[] colors = {"#3498db", "#2ecc71", "#e74c3c", "#9b59b6", "#f1c40f", "#1abc9c", "#34495e"};

            for (int i = 0; i < results.size(); i++) {
                CandidateResult result = results.get(i);
                int votes = result.getVotes();
                String party = result.getParty();

                // Skróć długie nazwy partii
                if (party != null && party.length() > 15) {
                    party = party.substring(0, 12) + "...";
                }

                // Formatuj etykietę kandydata
                String[] nameParts = result.getName().split(" ", 2);
                String formattedName = nameParts.length > 1
                        ? nameParts[0] + "\n" + nameParts[1]
                        : result.getName();
                String candidateLabel = formattedName + "\n(" + (party != null ? party : "brak") + ")";
                XYChart.Data<String, Number> data = new XYChart.Data<>(candidateLabel, votes);
                series.getData().add(data);

                if (votes > maxVotes) maxVotes = votes;

                // Ustaw kolor słupka
                final String color = colors[i % colors.length];
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-bar-fill: " + color + "; -fx-bar-padding: 5px;");
                    }
                });
            }

            resultsChart.getData().add(series);
            updateYAxisRange(maxVotes);

            // Ustawienia osi
            xAxis.setTickLabelRotation(0);
            xAxis.setTickLabelFont(Font.font("System", 10));
            xAxis.setMinHeight(Region.USE_PREF_SIZE);
            xAxis.setPrefHeight(50); // Zwiększ wysokość osi X
            resultsChart.setBarGap(10);
            resultsChart.setCategoryGap(5); // Zwiększ odstęp między słupkami
            // Zmniejszenie paddingu wykresu (dolny padding zwiększony dla etykiet)
            resultsChart.setPadding(new Insets(0, 0, 10, 0)); // Zmieniony padding

            // Dodaj etykiety na słupkach
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();
                int votes = data.getYValue().intValue();
                double percent = totalVotes > 0 ? (votes * 100.0) / totalVotes : 0;
                // Zwiększ minimalną wysokość dla krótkich słupków
                double barHeight = Math.max(votes * 1.0, 5); // Minimalna wysokość 5

                // Oblicz pozycję etykiety - 50% wysokości słupka
                double labelPosition = barHeight / 2;


                Label label = new Label(String.format("%.1f%%", percent));
                label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");

                // Ustaw pozycję etykiety w środku słupka
                StackPane.setAlignment(label, Pos.CENTER);
                StackPane.setMargin(label, new Insets(-labelPosition, 0, 0, 0));
                label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;"); // Mniejsza czcionka

                if (node != null) {
                    ((StackPane) node).getChildren().add(label);
                }

                // Dodaj tooltip z pełną informacją
                Tooltip tooltip = new Tooltip(
                        "Kandydat: " + data.getXValue().split("\n")[0] +
                                "\nPartia: " + data.getXValue().split("\n")[1].replaceAll("[()]", "") +
                                "\nGłosy: " + votes +
                                "\nProcent: " + String.format("%.1f%%", percent)
                );
                Tooltip.install(node, tooltip);

                // Obsługa kliknięcia
                node.setOnMouseClicked(event -> {
                    String candidateName = data.getXValue().split("\n")[0];
                    statusLabel.setText("Kliknięto na: " + candidateName);
                });
            }

            // Aktualizuj status z sumą głosów
            statusLabel.setText(String.format(
                    "Wyniki zaktualizowane: %s | Suma głosów: %d",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    totalVotes
            ));
        } catch (Exception e) {
            statusLabel.setText("Błąd podczas aktualizacji!");
            throw new DatabaseException("Błąd odświeżania wyników", e);
        }
    }

    // Pomocnicza metoda do aktualizacji osi Y
    private void updateYAxisRange(int maxVotes) {
        if (yAxis != null) {
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(maxVotes < 5 ? 5 : maxVotes + 2); // Dodajemy margines
            yAxis.setTickUnit(maxVotes < 10 ? 1 : Math.max(1, maxVotes / 10));
            yAxis.setMinorTickVisible(false);
            xAxis.setPrefWidth(candidatesData.size() * 70);

            // Przesunięcie etykiety osi X
            Platform.runLater(() -> {
                for (Node node : xAxis.getChildrenUnmodifiable()) {
                    if (node instanceof Text text && "Kandydat".equals(text.getText())) {
                        text.setTranslateY(20); // Przesuń w dół
                    }
                }
            });
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
                        if (axisNode instanceof Text text) {
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
//    private void updateYAxisRange(int maxVotes) {
//        if (yAxis != null) {
//            yAxis.setAutoRanging(false);
//            yAxis.setLowerBound(0);
//            yAxis.setUpperBound(maxVotes < 5 ? 5 : maxVotes + 1);
//            yAxis.setTickUnit(1);
//            yAxis.setMinorTickVisible(false);
//        }
//    }

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

    // Filtruje użytkowników po numerze PESEL
    @FXML
    private void handlePeselSearch() {
        String pesel = peselSearchField.getText().trim();

        // Pobierz aktualną listę użytkowników z bazy danych
        List<User> currentUsers = userDAO.getAllUsers();
        ObservableList<User> currentList = FXCollections.observableArrayList(currentUsers);

        if (pesel.isEmpty()) {
            usersTable.setItems(masterUserList);
            return;
        }

        // Filtruj na aktualnej liście
        ObservableList<User> filtered = currentList.filtered(user ->
                user.getPesel() != null && user.getPesel().contains(pesel)
        );

        usersTable.setItems(filtered);
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

    // Metody zarządzania użytkownikami
    @FXML
    private void handleAddUser() {
        try {
            validateUserForm(true);
            User newUser = createUserFromForm();
            userDAO.saveUser(newUser);

            // Dodane: odśwież tabelę i wyświetl komunikat sukcesu
            refreshUserTable();
            userManagementStatus.setText("Użytkownik dodany pomyślnie!");
            clearUserForm();

        } catch (ValidationException | DatabaseException e) {
            userManagementStatus.setText(e.getMessage());
        } catch (Exception e) {
            userManagementStatus.setText("Nieoczekiwany błąd: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateUser() {
        if (currentEditUser == null) {
            userManagementStatus.setText("Wybierz użytkownika do edycji!");
            return;
        }

        // Sprawdź, czy dokonano jakichkolwiek zmian
        boolean changesDetected = isChangesDetected();

        if (!changesDetected) {
            userManagementStatus.setText("Nie dokonano żadnych zmian!");
            return;
        }

        try {
            validateUserForm(false);

            // Walidacja zmiany ostatniego administratora
            String newRole = roleComboBox.getValue();
            if ("ADMIN".equals(currentEditUser.getRole()) &&
                    !"ADMIN".equals(newRole)) {

                int adminCount = userDAO.countAdmins();
                if (adminCount <= 1) {
                    userManagementStatus.setText(
                            "Nie można zmienić roli ostatniego administratora!"
                    );
                    return;
                }
            }

            updateUserData(currentEditUser);
            userDAO.updateUser(currentEditUser);
            refreshUserTable();
            userManagementStatus.setText("Dane użytkownika zaktualizowane pomyślnie!");

            // Wyczyść formularz po aktualizacji
            handleClearUserForm();
        } catch (ValidationException | DatabaseException e) {
            userManagementStatus.setText(e.getMessage());
        } catch (Exception e) {
            userManagementStatus.setText("Nieoczekiwany błąd: " + e.getMessage());
        }
    }

    private boolean isChangesDetected() {
        boolean changesDetected = !firstNameField.getText().equals(currentEditUser.getFirstName());
        if (!lastNameField.getText().equals(currentEditUser.getLastName())) {
            changesDetected = true;
        }
        if (!peselField.getText().equals(currentEditUser.getPesel())) {
            changesDetected = true;
        }
        if (!usernameField.getText().equals(currentEditUser.getUsername())) {
            changesDetected = true;
        }
        if (!roleComboBox.getValue().equals(currentEditUser.getRole())) {
            changesDetected = true;
        }

        // Sprawdź zmianę hasła (jeśli użytkownik wpisał coś w pole hasła)
        if (passwordChanged || !getPassword().isEmpty()) {
            changesDetected = true;
        }
        return changesDetected;
    }

    @FXML
    private void handleDeleteUser() {
        if (currentEditUser == null) {
            userManagementStatus.setText("Wybierz użytkownika do usunięcia!");
            return;
        }

        try {
            // Walidacja ostatniego administratora
            if ("ADMIN".equals(currentEditUser.getRole())) {
                int adminCount = userDAO.countAdmins();
                if (adminCount <= 1) { // Jeśli to ostatni admin
                    userManagementStatus.setText("Nie można usunąć ostatniego administratora!");
                    return;
                }
            }

            if (confirmDeletion(currentEditUser)) {
                userDAO.deleteUser(currentEditUser);
                masterUserList.remove(currentEditUser);
                refreshUserTable();
                clearUserForm();
                userManagementStatus.setText("Użytkownik usunięty pomyślnie!");
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

    // Walidacja formularza użytkownika
    private void validateUserForm(boolean isNewUser) throws ValidationException {
        if (firstNameField.getText().trim().isEmpty() ||
                lastNameField.getText().trim().isEmpty() ||
                peselField.getText().trim().isEmpty() ||
                usernameField.getText().trim().isEmpty() ||
                roleComboBox.getValue() == null)
        {
            throw new ValidationException("Wypełnij wszystkie obowiązkowe pola!");
        }

        validatePesel();
        validateName(firstNameField.getText().trim(), "Imię");
        validateName(lastNameField.getText().trim(), "Nazwisko");
        validateUsername();

        // Walidacja hasła tylko dla nowego użytkownika
        if (isNewUser && getPassword().isEmpty()) {
            throw new ValidationException("Hasło nie może być puste!");
        }
    }

    // Tworzy obiekt użytkownika z danych formularza
    private User createUserFromForm() {
        User newUser = new User();
        newUser.setFirstName(firstNameField.getText().trim());
        newUser.setLastName(lastNameField.getText().trim());
        newUser.setPesel(peselField.getText().trim());
        newUser.setUsername(usernameField.getText().trim());
        newUser.setPassword(BCrypt.hashpw(getPassword(), BCrypt.gensalt()));
        newUser.setRole(roleComboBox.getValue());
        newUser.setHasVoted(false);
        return newUser;
    }

    // Aktualizuje dane użytkownika
    private void updateUserData(User user) {
        user.setFirstName(firstNameField.getText().trim());
        user.setLastName(lastNameField.getText().trim());
        user.setPesel(peselField.getText().trim());
        user.setUsername(usernameField.getText().trim());
        user.setRole(roleComboBox.getValue());

        // Aktualizuj hasło, tylko jeżeli zostało zmienione i nie jest puste
        if (passwordChanged && !getPassword().isEmpty()) {
            user.setPassword(BCrypt.hashpw(getPassword(), BCrypt.gensalt()));
        }
    }

    // Metody dla kandydatów
    @FXML
    private void handleAddCandidate() {
        try {
            validateCandidateForm();

            String name = candidateNameField.getText().trim();
            String party = candidatePartyField.getText().trim();

            // Walidacja unikalności
            validateCandidateUniqueness(name, party);

            Candidate candidate = new Candidate();
            candidate.setName(name);
            candidate.setParty(party);
            candidate.setVotes(0);

            candidateDAO.addCandidate(candidate);
            refreshCandidatesTable();
            candidateStatusLabel.setText("Kandydat dodany pomyślnie!");
            clearCandidateForm();
        } catch (ValidationException | DatabaseException e) {
            candidateStatusLabel.setText(e.getMessage());
        }
    }

    private void validateCandidateUniqueness(String name, String party) throws ValidationException {
        if (candidateDAO.candidateExists(name, party)) {
            throw new ValidationException("Kandydat o podanym imieniu, nazwisku i partii już istnieje!");
        }
    }

    @FXML
    private void handleUpdateCandidate() {
        Candidate selected = candidatesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            candidateStatusLabel.setText("Wybierz kandydata do edycji");
            return;
        }

        try {
            validateCandidateForm();

            String newName = candidateNameField.getText().trim();
            String newParty = candidatePartyField.getText().trim();

            // Sprawdzenie czy wprowadzono zmiany
            boolean nameChanged = !newName.equals(selected.getName());
            boolean partyChanged = !newParty.equals(selected.getParty());

            if (!nameChanged && !partyChanged) {
                candidateStatusLabel.setText("Nie dokonano żadnych zmian!");
                return;
            }

            if (nameChanged || partyChanged) {
                validateCandidateUniqueness(newName, newParty);
            }

            selected.setName(newName);
            selected.setParty(newParty);

            candidateDAO.updateCandidate(selected);
            refreshCandidatesTable();
            candidateStatusLabel.setText("Zaktualizowano kandydata!");
            clearCandidateForm();
        } catch (ValidationException | DatabaseException e) {
            candidateStatusLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleDeleteCandidate() {
        Candidate selected = candidatesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            candidateStatusLabel.setText("Wybierz kandydata do usunięcia");
            return;
        }

        if (selected.getVotes() > 0) {
            candidateStatusLabel.setText("Nie można usunąć kandydata z głosami!");
            return;
        }

        try {
            candidateDAO.deleteCandidate(selected.getId());
            refreshCandidatesTable();
            candidateStatusLabel.setText("Usunięto kandydata");
            clearCandidateForm();
        } catch (DatabaseException e) {
            candidateStatusLabel.setText("Błąd: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshCandidates() {
        refreshCandidatesTable();
    }

    @FXML
    private void handleClearCandidateForm() {
        clearCandidateForm();
        candidateStatusLabel.setText("Formularz wyczyszczony");
    }

    // Walidacja formularza kandydata
    private void validateCandidateForm() throws ValidationException {
        String name = candidateNameField.getText().trim();
        String party = candidatePartyField.getText().trim();

        if (name.isEmpty()) {
            throw new ValidationException("Imię i nazwisko kandydata nie może być puste!");
        }

        if (party.isEmpty()) {
            throw new ValidationException("Nazwa partii nie może być pusta!");
        }

        if (!name.matches("[\\p{L}\\s\\-]+")) {
            throw new ValidationException("Imię i nazwisko może zawierać tylko litery, spacje i myślniki!");
        }

        if (name.length() < 3) {
            throw new ValidationException("Imię i nazwisko musi mieć co najmniej 3 znaki!");
        }
    }

    // Metody pomocnicze
    private void refreshCandidatesTable() {
        candidatesList.setAll(candidateDAO.getAllCandidates());
        candidatesTable.setItems(candidatesList);
    }

    private void clearCandidateForm() {
        candidateNameField.clear();
        candidatePartyField.clear();
        candidatesTable.getSelectionModel().clearSelection();
    }
}