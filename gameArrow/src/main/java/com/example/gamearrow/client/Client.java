package com.example.gamearrow.client;

import com.example.gamearrow.Arrow;
import com.example.gamearrow.GameState;
import com.example.gamearrow.model.PlayerStats;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.util.Optional;
import javafx.scene.control.Label;

public class Client {
    @FXML private Circle circleB, circleS;
    @FXML private Pane pane;
    @FXML private VBox playersInfoBox;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String myId;
    private final Gson gson = new Gson();
    private final Color[] playerColors = {Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE};
    private Stage leaderboardStage;
    private TableView<PlayerStats> leaderboardTable;

    public void askNameAndConnect(Stage owner) {
        Platform.runLater(() -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.initOwner(owner);
            dlg.setTitle("Вход в игру");
            dlg.setHeaderText("Введите уникальное имя игрока:");
            Optional<String> opt;
            do {
                opt = dlg.showAndWait();
                if (opt.isEmpty()) {
                    Platform.exit();
                    return;
                }
                String name = opt.get().trim();
                if (name.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "Имя не может быть пустым").showAndWait();
                    continue;
                }
                if (tryConnect(name, owner)) {
                    new Thread(this::listenForMessages).start();
                    return;
                }
            } while (true);
        });
    }

    private boolean tryConnect(String name, Stage owner) {
        try {
            socket = new Socket("localhost", 12345);
            out    = new PrintWriter(socket.getOutputStream(), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("NAME:" + name);
            String resp = in.readLine();
            if (resp == null) {
                showError(owner, "Нет ответа от сервера");
                return false;
            }
            if (resp.startsWith("ERROR")) {
                showError(owner, resp.substring(6));
                socket.close();
                return false;
            }
            myId = resp.substring(3);
            return true;
        } catch (IOException e) {
            showError(owner, "Не удалось подключиться: " + e.getMessage());
            return false;
        }
    }

    private void showError(Stage owner, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg);
            a.initOwner(owner);
            a.showAndWait();
        });
    }

    private void listenForMessages() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("LEADERBOARD:")) {
                    String jsonData = line.substring(12).trim();
                    PlayerStats[] stats = gson.fromJson(jsonData, PlayerStats[].class);
                    updateLeaderboard(stats);
                    continue;
                }

                if (line.startsWith("VICTORY:")) {
                    String winner = line.substring(8);
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Победа!");
                        alert.setHeaderText(null);
                        alert.setContentText("Победил игрок: " + winner + "!");
                        alert.showAndWait();
                    });
                } else {
                    GameState state = gson.fromJson(line, GameState.class);
                    Platform.runLater(() -> updateGame(state));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void updateLeaderboard(PlayerStats[] stats) {
        Platform.runLater(() -> {
            if (leaderboardTable != null) {
                leaderboardTable.getItems().setAll(stats);
                leaderboardTable.getSortOrder().add(leaderboardTable.getColumns().get(1));
            }
        });
    }
    private void updateGame(GameState state) {
        circleB.setLayoutY(state.circleBY);
        circleS.setLayoutY(state.circleSY);

        pane.getChildren().removeIf(n -> "base".equals(n.getId()));
        int idx = 0;
        for (var e : state.playerYPositions.entrySet()) {
            Rectangle base = new Rectangle(20, 10);
            base.setId("base");
            base.setX(50);
            base.setY(e.getValue() - 5);
            base.setFill(playerColors[idx++ % playerColors.length]);
            base.setStroke(Color.BLACK);
            pane.getChildren().add(base);
        }

        pane.getChildren().removeIf(n -> n instanceof Line);
        for (Arrow a : state.arrows) {
            pane.getChildren().add(new Line(a.x - 20, a.y, a.x, a.y));
        }

        playersInfoBox.getChildren().clear();
        playersInfoBox.getChildren().add(
                new HBox(10, new Label("Игрок"), new Label("Счёт"), new Label("Выстрелов"))
        );
        for (String id : state.playerYPositions.keySet()) {
            int score = state.scores.getOrDefault(id, 0);
            int shots = state.shots.getOrDefault(id, 0);
            playersInfoBox.getChildren().add(
                    new HBox(10, new Label(id), new Label(String.valueOf(score)), new Label(String.valueOf(shots)))
            );
        }
    }

    @FXML
    private void onReadyClicked() { out.println("READY"); }
    @FXML
    private void onShootClicked() { out.println("SHOOT"); }
    @FXML
    private void onPauseClicked() { out.println("PAUSE"); }

    @FXML
    public void onLeaderboardClicked() {
        if (leaderboardStage == null) {
            createLeaderboardWindow();
        }

        out.println("GET_LEADERBOARD");
        leaderboardStage.show();
    }

    private void createLeaderboardWindow() {
        leaderboardStage = new Stage();
        leaderboardStage.setTitle("Таблица лидеров");

        leaderboardTable = new TableView<>();

        TableColumn<PlayerStats, String> nameColumn = new TableColumn<>("Игрок");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        nameColumn.setPrefWidth(150);

        TableColumn<PlayerStats, Integer> winsColumn = new TableColumn<>("Победы");
        winsColumn.setCellValueFactory(new PropertyValueFactory<>("victories"));
        winsColumn.setPrefWidth(100);

        leaderboardTable.getColumns().addAll(nameColumn, winsColumn);

        VBox vbox = new VBox(leaderboardTable);
        vbox.setPadding(new Insets(10));

        leaderboardStage.setScene(new Scene(vbox, 300, 400));
    }


    private void handleLeaderboardData(String jsonData) {
        PlayerStats[] stats = gson.fromJson(jsonData, PlayerStats[].class);
        Platform.runLater(() -> {
            if (leaderboardTable != null) {
                leaderboardTable.getItems().setAll(stats);
                leaderboardTable.sort();
            }
        });
    }
}
