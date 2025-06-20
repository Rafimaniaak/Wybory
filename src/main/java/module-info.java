/**
 * Moduł główny aplikacji systemu wyborczego.
 * Definiuje wymagane zależności oraz eksportowane pakiety.
 */
module com.election {
    requires java.naming;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires org.hibernate.orm.core;
    requires org.slf4j;
    requires jakarta.persistence;
    requires com.github.librepdf.openpdf;
    requires java.rmi;
    requires java.desktop;
    // ... [zależności]

    exports com.election;
    opens com.election to javafx.fxml;
    exports com.election.controller;
    exports com.election.model;
    opens com.election.controller to javafx.fxml;
    opens com.election.util to org.hibernate.orm.core;
    opens com.election.model to org.hibernate.orm.core, javafx.base;
    // ... [eksporty i otwarcia]
}
