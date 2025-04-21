package com.example.gamearrow.server;

import java.io.IOException;

public class ServerApp {
    public static void main(String[] args) throws IOException {
        Server server = new Server(12345);
        server.start();
    }
}