package com.example.gamearrow;

public class Arrow {
    public String ownerId;
    public double x, y;
    public double dx;
    public boolean active;

    public Arrow(String ownerId, double startX, double startY, double dx) {
        this.ownerId = ownerId;
        this.x = startX;
        this.y = startY;
        this.dx = dx;
        this.active = true;
    }
}