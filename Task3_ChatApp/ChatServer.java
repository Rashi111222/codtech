import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

    static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("NexChat server running on port " + PORT);
        System.out.println("Open http://localhost:" + PORT);

        ServerSocket serverSocket = new ServerSocket(PORT);
        serverSocket.setReuseAddress(true);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    static void broadcastAll(String message) {
        for (ClientHandler client : clients) client.sendMessage(message);
    }

    static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients)
            if (client != sender) client.sendMessage(message);
    }

    static String onlineUsers() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler c : clients) {
            if (c.username != null) {
                if (sb.length() > 0) sb.append(",");
                sb.append(c.username);
            }
        }
        return sb.toString();
    }
}