package com.election;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

public class HashGenerator extends Application {
    private Stage parentStage;

    public void setParentStage(Stage parentStage) {
        this.parentStage = parentStage;
    }
    @Override
    public void start(Stage stage) {
        // Konfiguracja GUI
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Generator Hashy BCrypt");
        TextField passwordField = new TextField();
        passwordField.setPromptText("Wprowadź hasło");
        passwordField.setMaxWidth(300);

        TextField hashField = new TextField();
        hashField.setPromptText("Wygenerowany hash");
        hashField.setMaxWidth(300);
        hashField.setEditable(false);

        Button generateBtn = new Button("Generuj Hash");
        Button copyBtn = new Button("Kopiuj do schowka");

        generateBtn.setOnAction(e -> {
            String password = passwordField.getText();
            if (!password.isEmpty()) {
                String hash = BCrypt.hashpw(password, BCrypt.gensalt());
                hashField.setText(hash);
            }
        });

        copyBtn.setOnAction(e -> {
            if (!hashField.getText().isEmpty()) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(hashField.getText());
                clipboard.setContent(content);
            }
        });

        root.getChildren().addAll(
                titleLabel,
                passwordField,
                hashField,
                generateBtn,
                copyBtn
        );
        // Przycisk z ikoną i tekstem
        Button backButton = new Button("← Powrót");
        //backButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("@/com/election/images/arrow-rotate-left-solid.png"))));

        backButton.setOnAction(e -> {
            if (parentStage != null) {
                parentStage.show(); // Pokazuje okno logowania
            }
            stage.close(); // Zamyka bieżące okno generatora
        });

        root.getChildren().add(backButton);

        //root.getChildren().add(backButton);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}