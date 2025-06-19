package com.election;

import com.election.util.DataInitializer;
import com.election.util.HibernateUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.hibernate.Session;

import java.net.URL;
import java.util.Objects;

// Główna klasa aplikacji
public class Main extends Application {

    // Inicjalizuje aplikację i wyświetla ekran logowania
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Inicjalizacja Hibernate i danych startowych
        initializeHibernate();

        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/app_icon.png")));
        primaryStage.getIcons().add(icon);

        // Załaduj GUI (login.fxml)
        URL url = getClass().getResource("/com/election/view/login.fxml");
        if (url == null) {
            throw new IllegalStateException("Nie znaleziono pliku FXML: login.fxml");
        }

        Parent root = FXMLLoader.load(url);
        Scene scene = new Scene(root);

        primaryStage.setTitle("System głosowania");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Inicjalizuje połączenie z Hibernate
    private void initializeHibernate() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();

        DataInitializer.initializeIfEmpty(session);

        session.getTransaction().commit();
        session.close();

    }

    // Punkt wejścia aplikacji
    public static void main(String[] args) {
        launch(args);
    }

    // Zamyka połączenia przy wyjściu z aplikacji
    @Override
    public void stop() {
        HibernateUtil.shutdown();
        System.out.println("Aplikacja została zamknięta");
    }
}