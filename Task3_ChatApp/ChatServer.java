package Task3_ChatApp;
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

            new Thread(() -> {
                try {
                    InputStream in = socket.getInputStream();

                    // Peek first few bytes to detect HTTP GET
                    byte[] buffer = new byte[512];
                    int len = in.read(buffer);

                    if (len <= 0) {
                        socket.close();
                        return;
                    }

                    String request = new String(buffer, 0, len);

if (request.startsWith("GET") && !request.contains("Upgrade: websocket")) {
    serveHTML(socket);
} else {
    // Pass the already-read bytes so ClientHandler can do the handshake
    new Thread(new ClientHandler(socket, buffer, len)).start();
}
                } catch (Exception e) {
                    try { socket.close(); } catch (Exception ignored) {}
                }
            }).start();
        }
    }

    // Serve index.html
    static void serveHTML(Socket socket) throws IOException {
        OutputStream out = socket.getOutputStream();

        File file = new File("index.html");

        if (!file.exists()) {
            String body = "<h2>index.html not found</h2>";
            String response =
                    "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + body.length() + "\r\n\r\n" +
                    body;

            out.write(response.getBytes());
            out.flush();
            socket.close();
            return;
        }

        byte[] data = Files.readAllBytes(file.toPath());

        String header =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + data.length + "\r\n\r\n";

        out.write(header.getBytes());
        out.write(data);
        out.flush();
        socket.close();
    }

    // Broadcast to all clients
    static void broadcastAll(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // Send to others (not sender)
    static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    // List of online users
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