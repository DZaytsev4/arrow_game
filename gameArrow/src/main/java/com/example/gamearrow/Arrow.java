package com.example.gamearrow;

public class Arrow {
    public String ownerId;   // чей выстрел
    public double x, y;      // текущая позиция «носа» стрелы
    public double dx;        // скорость по X
    public boolean active;   // пока true — стрела в полёте

    public Arrow(String ownerId, double startX, double startY, double dx) {
        this.ownerId = ownerId;
        this.x = startX;
        this.y = startY;
        this.dx = dx;
        this.active = true;
    }
}