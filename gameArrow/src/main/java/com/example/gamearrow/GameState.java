package com.example.gamearrow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {
    public double circleBY;
    public double circleSY;
    public double speedB, speedS;
    public boolean running;
    public List<Arrow> arrows = new ArrayList<>();
    public Map<String, Integer> scores = new HashMap<>();
    public Map<String, Integer> shots = new HashMap<>();
    public Map<String, Double> playerYPositions = new HashMap<>();
}
