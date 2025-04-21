package com.example.gamearrow.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.net.*;
import com.example.gamearrow.GameState;
import com.example.gamearrow.Arrow;
import com.google.gson.Gson;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public class Client {
    @FXML private Circle circleB;
    @FXML private Circle circleS;
    @FXML private Pane pane;
    @FXML private Label countShot;
    @FXML private Label countScore;
    @FXML private Button readyButton;
    @FXML private Button shootButton;
    @FXML private Button pauseButton;

    private String host = "localhost";
    private int port = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();
    private String myId;
    private final Color[] playerColors = {Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE};
    private final Map<String, Rectangle> playerBases = new HashMap<>();

    public Client() {
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Ввод имени пользователя
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Введите ваше имя: ");
            String name = console.readLine();
            out.println("NAME:" + name);

            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForMessages() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("ID:")) {
                    myId = msg.substring(3);
                    continue;
                }
                GameState state = gson.fromJson(msg, GameState.class);
                if (state.running) {
                    Platform.runLater(() -> {
                        circleB.setLayoutY(state.circleBY);
                        circleS.setLayoutY(state.circleSY);
                        int sc = state.scores.getOrDefault(myId, 0);
                        countScore.setText("Счёт: " + sc);
                        pane.getChildren().removeIf(n -> n instanceof Rectangle && "base".equals(n.getId()));
                        int i = 0;
                        for (Map.Entry<String, Double> entry : state.playerYPositions.entrySet()) {
                            String id = entry.getKey();
                            double y = entry.getValue();

                            Rectangle base = new Rectangle(20, 10);
                            base.setId("base");
                            base.setX(50);
                            base.setY(y - 5);
                            base.setFill(playerColors[i % playerColors.length]);
                            base.setStroke(Color.BLACK);
                            pane.getChildren().add(base);
                            i++;
                        }
                        pane.getChildren().removeIf(n -> n instanceof Line);
                        for (Arrow a : state.arrows) {
                            Line line = new Line(a.x - 20, a.y, a.x, a.y);
                            pane.getChildren().add(line);
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onReadyClicked() {
        out.println("READY");
    }

    @FXML
    private void onShootClicked() {
        out.println("SHOOT");
        Platform.runLater(() -> {
            // локально считаем выстрелы
            String[] parts = countShot.getText().split(":");
            int shots = Integer.parseInt(parts[1].trim());
            countShot.setText("Выстрелов: " + (shots + 1));
        });
    }

    @FXML
    private void onPauseClicked() {
        out.println("PAUSE");
    }
}
