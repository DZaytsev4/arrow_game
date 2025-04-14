module com.example.gamearrow {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.gamearrow to javafx.fxml;
    exports com.example.gamearrow;
}