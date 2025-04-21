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

    private boolean firstRun = true;

    private Thread gameLoopThread;

    private final Set<String> usernames = new HashSet<>();

    public Server(int port) {
        this.port = port;
    }

    private void broadcastState() {
        String json = gson.toJson(state);
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.send(json);
            }
        }
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
                    state.circleBY = 200;
                    state.circleSY = 200;
                    broadcastState();
                }
            }
        }
    }

    private boolean allReady() {
        synchronized (clients) {
            return !clients.isEmpty() && clients.stream().allMatch(c -> c.ready);
        }
    }
    private void checkVictory(String playerId) {
        int score = state.scores.getOrDefault(playerId, 0);
        if (score >= 6) {
            synchronized (state) {
                state.running = false;
                state.arrows.clear();
                state.circleBY = 200;
                state.circleSY = 200;
                state.speedB = 5;
                state.speedS = 10;

                for (String key : state.scores.keySet()) {
                    state.scores.put(key, 0);
                    state.shots.put(key, 0);
                }
            }

            synchronized (clients) {
                for (ClientHandler c : clients) {
                    c.ready = false;
                }
            }
            gameLoopThread = null;
            broadcastState();
            String victoryMsg = "VICTORY:" + playerId;
            synchronized (clients) {
                for (ClientHandler c : clients) {
                    c.send(victoryMsg);
                }
            }

        }
    }


    private void startGameLoop() {
        if (gameLoopThread != null) return;

        if (firstRun) {
            state.circleBY = state.circleSY = 200;
            state.speedB = 5;
            state.speedS = 10;
            firstRun = false;
        }

        state.running = true;

        gameLoopThread = new Thread(() -> {
            try {
                while (state.running) {
                    Thread.sleep(30);
                    if (state.circleBY > 320 || state.circleBY < 80) state.speedB = -state.speedB;
                    if (state.circleSY > 350 || state.circleSY < 50) state.speedS = -state.speedS;
                    state.circleBY += state.speedB;
                    state.circleSY += state.speedS;

                    List<Arrow> arrowsToProcess;
                    synchronized (state) {
                        arrowsToProcess = new ArrayList<>(state.arrows);
                    }

                    List<Arrow> arrowsToRemove = new ArrayList<>();
                    for (Arrow a : arrowsToProcess) {
                        a.x += a.dx;

                        boolean removeArrow = false;
                        if (a.x > 600) {
                            removeArrow = true;
                        } else if (Math.hypot(a.x - 270, a.y - state.circleBY) < 30) {
                            state.scores.merge(a.ownerId, 1, Integer::sum);
                            checkVictory(a.ownerId);
                            removeArrow = true;
                        } else if (Math.hypot(a.x - 360, a.y - state.circleSY) < 15) {
                            state.scores.merge(a.ownerId, 2, Integer::sum);
                            checkVictory(a.ownerId);
                            removeArrow = true;
                        }

                        if (removeArrow) {
                            arrowsToRemove.add(a);
                        }
                    }

                    synchronized (state) {
                        state.arrows.removeAll(arrowsToRemove);
                    }

                    String json = gson.toJson(state);
                    synchronized (clients) {
                        for (ClientHandler c : clients) c.send(json);
                    }

                    if (!state.running) break;
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
        private String clientId;
        boolean ready = false;

        private static final double[] START_Y_POSITIONS = {100, 140, 180, 220};

        ClientHandler(Socket sock) throws IOException {
            this.sock = sock;
            this.out = new PrintWriter(sock.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

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

            synchronized (state) {
                state.scores.put(clientId, 0);
                state.shots.put(clientId, 0);
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
                            if (allReady()) {
                                startGameLoop();
                            }
                        }
                        case "SHOOT" -> {
                            synchronized (state) {
                                state.shots.merge(clientId, 1, Integer::sum);
                                double y = state.playerYPositions.get(clientId);
                                state.arrows.add(new Arrow(clientId, 75, y, 15));
                            }
                        }
                        case "PAUSE" -> {
                            state.running = false;
                            gameLoopThread = null;
                            synchronized (clients) {
                                for (ClientHandler c : clients) {
                                    c.ready = false;
                                }
                            }
                        }
                        default -> {  }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { sock.close(); } catch (IOException ignored) {}
                synchronized (clients) { clients.remove(this); }
                synchronized (usernames) { usernames.remove(clientId); }
                synchronized (state) {
                    state.playerYPositions.remove(clientId);
                    state.scores.remove(clientId);
                    state.shots.remove(clientId);
                }
                Server.this.broadcastState();
            }
        }
    }
}
