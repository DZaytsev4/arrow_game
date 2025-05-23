package com.example.gamearrow.client;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {
    private final Gson gson = new Gson();

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gamearrow/hello-view.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Меткий стрелок");
        primaryStage.show();

        Client controller = loader.getController();
        controller.askNameAndConnect(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
