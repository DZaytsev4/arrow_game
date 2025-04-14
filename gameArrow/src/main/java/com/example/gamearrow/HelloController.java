package com.example.gamearrow;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import java.util.List;
import java.util.ArrayList;

import java.awt.*;

public class HelloController {

    public Pane pane;

    public Label countShot;
    public Label countScore;

    public Circle circleB;
    public Circle circleS;

    boolean Start = false;
    boolean Pause = false;
    boolean WasPause = false;
    boolean isArrowActive = false;

    int cShot;
    int cScore;

    Thread targetThread = null;
    double speedB;
    double speedS;
    double speedA = 10;

    private void UpdateScore() {
        countScore.setText("Cчёт игрока: " + cScore);
    }

    private void UpdateShot() {
        countShot.setText("Выстрелы: " + cShot);
    }

    private void animationGame () {
        targetThread = new Thread(() -> {
            List<Line> arrowsToRemove = new ArrayList<>();
            while (Start && !Pause) {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    break;
                }

                if (circleB.getLayoutY() > 320 || circleB.getLayoutY() < 80) {
                    speedB = -speedB;
                }
                if (circleS.getLayoutY() > 350 || circleS.getLayoutY() < 50) {
                    speedS = -speedS;
                }

                javafx.application.Platform.runLater(() -> {
                    circleB.setLayoutY(circleB.getLayoutY() + speedB);
                    circleS.setLayoutY(circleS.getLayoutY() + speedS);
                });

                for (javafx.scene.Node node : pane.getChildren()) {
                    if (node instanceof Line) {
                        Line arrowS = (Line) node;
                        javafx.application.Platform.runLater(() -> {
                            arrowS.setLayoutX(arrowS.getLayoutX() + speedA);
                            checkHit(arrowS, arrowsToRemove);
                            if (arrowS.getLayoutX() > 600) {
                                arrowsToRemove.add(arrowS);
                                isArrowActive = false;
                            }
                        });
                    }
                }

                javafx.application.Platform.runLater(() -> {
                    pane.getChildren().removeAll(arrowsToRemove);
                });

                arrowsToRemove.clear();
            }
        });

        targetThread.setDaemon(true);
        targetThread.start();
    }

    private void checkHit(Line arrowS, List<Line> arrowsToRemove) {
        if (checkCollision(arrowS, circleB)) {
            cScore += 1;
            UpdateScore();
            arrowsToRemove.add(arrowS);
            isArrowActive = false;
            return;
        }

        if (checkCollision(arrowS, circleS)) {
            cScore += 2;
            UpdateScore();
            arrowsToRemove.add(arrowS);
            isArrowActive = false;
        }
    }
    private boolean checkCollision(Line arrowS, Circle circle) {
        Shape intersection = Shape.intersect(arrowS, circle);
        return intersection.getBoundsInLocal().getWidth() != -1;
    }

    public void startGame(ActionEvent actionEvent) {
        cScore = 0;
        cShot = 0;
        UpdateScore();
        UpdateShot();
        circleB.setLayoutY(200);
        circleS.setLayoutY(200);
        speedB = 5;
        speedS = 10;
        Start = true;
        Pause = false;
        isArrowActive = false;
        animationGame();
    }

    public void pausedGame(ActionEvent actionEvent) {
        if (!WasPause) {
            Pause = true;
            WasPause = true;
        }
        else {
            Pause = false;
            WasPause = false;
            animationGame();
        }
    }

    public void shot(ActionEvent actionEvent) {
        if (Start && !Pause &&  !isArrowActive) {
            Line arrowS = new Line();
            arrowS.setLayoutY(200);
            arrowS.setLayoutX(75);
            arrowS.setStrokeWidth(1);
            arrowS.setEndX(20);
            pane.getChildren().add(arrowS);
            isArrowActive = true;
            cShot += 1;
            UpdateShot();
        }
    }

    public void endGame(ActionEvent actionEvent) {
        Start = false;
    }

}