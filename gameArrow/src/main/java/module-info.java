module com.example.gamearrow {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.google.gson;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.naming;

    exports com.example.gamearrow.server;
    exports com.example.gamearrow.client;
    exports com.example.gamearrow.model;

    opens com.example.gamearrow.model to
            org.hibernate.orm.core,
            com.google.gson,
            javafx.base;

    opens com.example.gamearrow to com.google.gson;
    opens com.example.gamearrow.client to javafx.fxml;
}
