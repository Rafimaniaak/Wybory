package com.election;

import com.election.dao.UserDAO;
import com.election.model.User;
import com.election.util.DataInitializer;
import com.election.util.HibernateUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Debugowanie URL ścieżki FXML
        URL url = getClass().getResource("/com/election/view/login.fxml");
        System.out.println("FXML URL: " + url); // Sprawdzi, czy jest null

        // Jeśli url jest null, to znaczy, że FXML nie może zostać znaleziony
        if (url == null) {
            throw new IllegalStateException("Plik FXML nie został znaleziony pod ścieżką: /com/election/view/login.fxml");
        }

        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setTitle("System głosowania");
        primaryStage.setScene(scene);
        primaryStage.show();
    }



    // W klasie Main.java
    public static void main(String[] args) {

        //create_users(); // Tworzenie uzytkowników manualnie w main

        // Inicjalizuj SessionFactory
        UserDAO userDAO = new UserDAO();
        //userDAO.initializeSessionFactory(); // Metoda wymuszająca inicjalizację
        DataInitializer.initialize();

        List<User> users = userDAO.getAllUsers();

        System.out.println("=== Użytkownicy w bazie ===");
        for (User user : users) {
            System.out.printf(
                    "Username: %-10s | Password: %s%n",
                    user.getUsername(),
                    user.getPassword()
            );
        }

        launch(args); // Uruchomienie JavaFX
    }

    @Override
    public void stop() {
        // Zamknięcie SessionFactory przy zamykaniu aplikacji
        HibernateUtil.shutdown();
    }
    public static void create_users() {
        // Tymczasowe tworzenie użytkowników
        UserDAO userDAO = new UserDAO();

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123", BCrypt.gensalt()));
        admin.setRole("ADMIN");

        User user = new User();
        user.setUsername("user1");
        user.setPassword(BCrypt.hashpw("user123", BCrypt.gensalt()));
        user.setRole("USER");

        userDAO.save(admin);
        userDAO.save(user);
    }
}