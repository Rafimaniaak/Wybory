module com.election {
    requires java.naming;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
//    requires javafx.web;
//
//    requires org.controlsfx.controls;
//    requires com.dlsc.formsfx;
//    requires net.synedra.validatorfx;
//    requires org.kordamp.ikonli.javafx;
//    requires org.kordamp.bootstrapfx.core;
//    requires eu.hansolo.tilesfx;
//    requires com.almasb.fxgl.all;
    //requires java.persistence;
    requires jbcrypt;
    requires org.hibernate.orm.core;
    requires org.slf4j;
    requires jakarta.persistence;
    requires com.github.librepdf.openpdf;

    exports com.election;
    opens com.election to javafx.fxml;
    exports com.election.controller;
    opens com.election.controller to javafx.fxml;
    opens com.election.util to org.hibernate.orm.core; // Jeśli HibernateUtil jest w pakiecie util
    opens com.election.model to org.hibernate.orm.core, javafx.base;
}
