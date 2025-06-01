package com.election;

import com.election.util.DataInitializer;
import com.election.util.HibernateUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.hibernate.Session;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Inicjalizacja Hibernate i danych startowych
        initializeHibernate();

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

    private void initializeHibernate() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();

        DataInitializer.initializeIfEmpty(session);

        session.getTransaction().commit();
        session.close();

    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        HibernateUtil.shutdown();
        System.out.println("Aplikacja została zamknięta");
    }
}