module com.example.gamearrow {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.google.gson;


    exports com.example.gamearrow.server;   // чтобы серверная часть могла видеть свои классы
    exports com.example.gamearrow.client;   // чтобы клиентские контроллеры работали
    // но главное — открываем пакет, где лежит GameState,
    // чтобы Gson мог к нему доступиться через reflection:
    opens com.example.gamearrow to com.google.gson;
    // если GameState в подпакете shared, то:
    // opens com.example.gamearrow.shared to com.google.gson;

    // и если используете FXML:
    opens com.example.gamearrow.client to javafx.fxml;
}
