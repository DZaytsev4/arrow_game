package com.example.gamearrow.model;

import com.google.gson.annotations.Expose;

public class PlayerStats {
    @Expose
    private final String playerName;

    @Expose
    private final int victories;

    public PlayerStats(String playerName, int victories) {
        this.playerName = playerName;
        this.victories = victories;
    }

    public String getPlayerName() { return playerName; }
    public int getVictories() { return victories; }
}