package com.example.gamearrow.server;

import java.io.*;
import java.net.*;
import java.util.*;
import com.example.gamearrow.GameState;
import com.example.gamearrow.Arrow;
import com.google.gson.Gson;

public class Server {
    private static final int MAX_PLAYERS = 4;
    private final int port;
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final Gson gson = new Gson();
    private GameState state = new GameState();
    private Thread gameLoopThread;
    private final Set<String> usernames = new HashSet<>();
    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Сервер запущен на порту " + port);

        while (true) {
            Socket sock = serverSocket.accept();
            synchronized (clients) {
                if (clients.size() >= MAX_PLAYERS) {
                    sock.close();
                } else {
                    ClientHandler handler = new ClientHandler(sock);
                    clients.add(handler);
                    new Thread(handler).start();
                }
            }
        }
    }

    private boolean allReady() {
        synchronized (clients) {
            return !clients.isEmpty() && clients.stream().allMatch(c -> c.ready);
        }
    }

    private void startGameLoop() {
        if (gameLoopThread != null) return;

        state.circleBY = state.circleSY = 200;
        state.speedB = 5;
        state.speedS = 10;
        state.running = true;

        gameLoopThread = new Thread(() -> {
            try {
                while (state.running) {
                    Thread.sleep(30);

                    // двигаем круги
                    if (state.circleBY > 320 || state.circleBY < 80) state.speedB = -state.speedB;
                    if (state.circleSY > 350 || state.circleSY < 50) state.speedS = -state.speedS;
                    state.circleBY += state.speedB;
                    state.circleSY += state.speedS;

                    // обрабатываем стрелы
                    synchronized (state) {
                        Iterator<Arrow> it = state.arrows.iterator();
                        while (it.hasNext()) {
                            Arrow a = it.next();
                            a.x += a.dx;
                            if (a.x > 600) {
                                a.active = false;
                                it.remove();
                                continue;
                            }
                            if (Math.hypot(a.x - 270, a.y - state.circleBY) < 30) {
                                state.scores.merge(a.ownerId, 1, Integer::sum);
                                a.active = false;
                                it.remove();
                                continue;
                            }
                            if (Math.hypot(a.x - 360, a.y - state.circleSY) < 15) {
                                state.scores.merge(a.ownerId, 2, Integer::sum);
                                a.active = false;
                                it.remove();
                            }
                        }
                    }

                    String json = gson.toJson(state);
                    synchronized (clients) {
                        for (ClientHandler c : clients) {
                            c.send(json);
                        }
                    }
                }
            } catch (InterruptedException ignored) {
            }
        });
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();
    }

    private class ClientHandler implements Runnable {
        private final Socket sock;
        private final PrintWriter out;
        private final BufferedReader in;
        private String clientId; // теперь не final
        boolean ready = false;
        private static final double[] START_Y_POSITIONS = {100, 140, 180, 220};

        ClientHandler(Socket sock) throws IOException {
            this.sock = sock;
            this.out = new PrintWriter(sock.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            // Чтение имени пользователя
            String requestedLine = in.readLine();
            if (requestedLine == null || !requestedLine.startsWith("NAME:")) {
                out.println("ERROR:Invalid name format");
                sock.close();
                throw new IOException("Invalid name format");
            }

            String name = requestedLine.substring(5).trim();
            synchronized (usernames) {
                if (usernames.contains(name)) {
                    out.println("ERROR:Name already taken");
                    sock.close();
                    throw new IOException("Name already taken");
                } else {
                    usernames.add(name);
                    this.clientId = name;
                }
            }

            // Присваиваем стартовую позицию и добавляем в состояние
            synchronized (state) {
                state.scores.put(clientId, 0);
                double assignedY = START_Y_POSITIONS[clients.size()];
                state.playerYPositions.put(clientId, assignedY);
            }

            out.println("ID:" + clientId);
        }

        void send(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    switch (line) {
                        case "READY" -> {
                            ready = true;
                            if (allReady()) startGameLoop();
                        }
                        case "SHOOT" -> {
                            synchronized (state) {
                                double y = state.playerYPositions.getOrDefault(clientId, 200.0);
                                state.arrows.add(new Arrow(clientId, 75, y, 15));
                            }
                        }
                        case "PAUSE" -> state.running = false;
                        default -> { /* игнор */ }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    sock.close();
                } catch (IOException ignored) {}

                synchronized (clients) {
                    clients.remove(this);
                }
                synchronized (usernames) {
                    usernames.remove(clientId);
                }
            }
        }
    }
}