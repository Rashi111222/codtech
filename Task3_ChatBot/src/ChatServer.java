import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
/**
 * ═══════════════════════════════════════════════════════
 *  MULTITHREADED CHAT SERVER
 *  CodTech Internship - Task 3
 * ═══════════════════════════════════════════════════════
 *
 *  HOW IT WORKS (Simple Explanation):
 *  - This server listens on a PORT (like a door)
 *  - When a new user connects, it creates a NEW THREAD for them
 *  - A Thread = an independent worker that handles one user
 *  - All threads share a "broadcast" method to send messages to everyone
 *
 *  KEY CONCEPTS USED:
 *  - ServerSocket    → Opens a port and waits for connections
 *  - Socket          → The actual connection between server and one client
 *  - Thread          → Handles each client independently (multithreading!)
 *  - CopyOnWriteArrayList → A thread-safe list (safe for multiple threads)
 */
public class ChatServer {

    // The port number our server will listen on
    // Think of it like an apartment number in a building
    private static final int PORT = Integer.parseInt(
    System.getenv("PORT") != null ? System.getenv("PORT") : "8080"
);

    // A thread-safe list that holds ALL currently connected clients
    // CopyOnWriteArrayList is used instead of ArrayList because
    // multiple threads will read/write this list simultaneously
    private static final List<ClientHandler> connectedClients =
            new CopyOnWriteArrayList<>();

    // Tracks how many total users have ever joined (for giving unique IDs)
    private static int totalUsersJoined = 0;

    public static void main(String[] args) throws IOException {

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   CODTECH CHAT SERVER STARTING...    ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("► Server running on port: " + PORT);
        System.out.println("► Open index.html in your browser to connect");
        System.out.println("► Press Ctrl+C to stop the server\n");

        // ServerSocket opens the "door" on our PORT
        // The try-with-resources ensures it closes automatically
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            // This loop runs FOREVER, always waiting for new connections
            while (true) {

                // .accept() PAUSES here until someone connects
                // When a user opens the browser, this line unpauses
               Socket clientSocket = serverSocket.accept();
totalUsersJoined++;

Thread t = new Thread(() -> {
    try {
        // peek at first 4096 bytes to check if WebSocket or plain HTTP
        PushbackInputStream pis = new PushbackInputStream(
            clientSocket.getInputStream(), 4096);
        byte[] buf = new byte[4096];
        int n = pis.read(buf);
        if (n == -1) { clientSocket.close(); return; }

        String req = new String(buf, 0, n, "UTF-8");
        pis.unread(buf, 0, n);

        if (req.contains("Upgrade: websocket") || req.contains("Upgrade: WebSocket")) {
            // WebSocket client — hand to ClientHandler
            ClientHandler ch = new ClientHandler(clientSocket, pis, totalUsersJoined);
            connectedClients.add(ch);
            ch.run();
        } else {
            // Plain browser visit — serve index.html
            serveIndexHtml(clientSocket);
        }
    } catch (IOException e) {
        try { clientSocket.close(); } catch (IOException ignored) {}
    }
});
t.setDaemon(true);
t.start();
            }
        }
    }

    /**
     * BROADCAST: Send a message to ALL connected clients
     * This is called by any ClientHandler to notify everyone
     *
     * @param message  The text to send
     * @param sender   The client who sent it (we skip sending back to them)
     */
    public static void broadcast(String message, ClientHandler sender) {
        // Loop through every connected client
        for (ClientHandler client : connectedClients) {
            // Send to everyone INCLUDING the sender (for confirmation)
            client.sendMessage(message);
        }
    }
    private static void serveIndexHtml(Socket socket) throws IOException {
    OutputStream out = socket.getOutputStream();
    java.nio.file.Path path = java.nio.file.Paths.get("index.html");
    byte[] content = java.nio.file.Files.readAllBytes(path);
    String headers =
        "HTTP/1.1 200 OK\r\n" +
        "Content-Type: text/html; charset=UTF-8\r\n" +
        "Content-Length: " + content.length + "\r\n" +
        "Connection: close\r\n\r\n";
    out.write(headers.getBytes("UTF-8"));
    out.write(content);
    out.flush();
    socket.close();
}
    /**
     * Remove a client from our list when they disconnect
     */
    public static void removeClient(ClientHandler client) {
        connectedClients.remove(client);
        System.out.println("► Client disconnected. Active users: " + connectedClients.size());
    }

    /**
     * Get count of currently connected users
     */
    public static int getActiveUserCount() {
        return connectedClients.size();
    }
}