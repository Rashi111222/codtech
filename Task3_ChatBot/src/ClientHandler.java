import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;

/**
 * ═══════════════════════════════════════════════════════
 *  CLIENT HANDLER
 * ═══════════════════════════════════════════════════════
 *
 *  This class handles ONE connected user.
 *  It implements Runnable, which means it can be run inside a Thread.
 *
 *  Each instance of this class:
 *  1. Does the WebSocket "handshake" with the browser
 *  2. Reads messages sent by the user
 *  3. Broadcasts those messages to everyone else
 *  4. Cleans up when the user disconnects
 *
 *  WHY WebSocket instead of plain Socket?
 *  - Browsers can't use raw TCP sockets for security reasons
 *  - WebSocket is a protocol that browsers DO support
 *  - It starts as HTTP, then "upgrades" to a persistent connection
 */
public class ClientHandler implements Runnable {

    private final Socket socket;           // The raw TCP connection
    private final int userId;              // Unique ID for this user
    private String username;               // Display name chosen by user
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;   // Tracks if handshake completed

    // WebSocket magic number used in the handshake (required by protocol)
    private static final String WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public ClientHandler(Socket socket, int userId) {
        this.socket = socket;
        this.userId = userId;
        this.username = "User" + userId; // Default name until user sets theirs
    }

    /**
     * run() is called when the Thread starts.
     * This is the main lifecycle of a client connection.
     */
    @Override
    public void run() {
        try {
           if (inputStream == null)  inputStream  = socket.getInputStream();
if (outputStream == null) outputStream = socket.getOutputStream();

            // Step 1: Do the WebSocket handshake
            // The browser sends an HTTP request first
            if (!performWebSocketHandshake()) {
                System.out.println("✗ Handshake failed for user " + userId);
                socket.close();
                return;
            }

            isConnected = true;
            System.out.println("✓ WebSocket handshake complete for user " + userId);

            // Step 2: Read messages in a loop until user disconnects
            while (!socket.isClosed()) {
                String message = readWebSocketMessage();
                if (message == null) break; // null means disconnected

                handleMessage(message);
            }

        } catch (IOException e) {
            // This is normal - happens when user closes browser tab
            System.out.println("► User " + username + " disconnected");
        } finally {
            // Step 3: Clean up
            cleanup();
        }
    }

    /**
     * WEBSOCKET HANDSHAKE
     *
     * When a browser connects, it first sends an HTTP request like:
     *   GET / HTTP/1.1
     *   Upgrade: websocket
     *   Sec-WebSocket-Key: dGhlIHNhbXBsZQ==
     *
     * We must respond with a special "101 Switching Protocols" response.
     * The key part is computing the "accept" hash from their key.
     */
    private boolean performWebSocketHandshake() throws IOException {
        // Read the HTTP request line by line
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream));

        String webSocketKey = null;
        String line;

        // Read headers until we hit an empty line (end of HTTP request)
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            // Look for the WebSocket key header
            if (line.startsWith("Sec-WebSocket-Key:")) {
                webSocketKey = line.split(":")[1].trim();
            }
        }

        if (webSocketKey == null) return false;

        // Compute the accept key using SHA-1 hash
        // This is required by the WebSocket protocol specification
        String acceptKey = computeWebSocketAcceptKey(webSocketKey);

        // Send the HTTP 101 Switching Protocols response
        String response =
                "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "\r\n";

        outputStream.write(response.getBytes("UTF-8"));
        outputStream.flush();
        return true;
    }

    /**
     * Compute the WebSocket accept key
     * Formula: Base64(SHA1(clientKey + MAGIC_STRING))
     */
    private String computeWebSocketAcceptKey(String clientKey) {
        try {
            String combined = clientKey + WS_MAGIC;
            java.security.MessageDigest sha1 =
                    java.security.MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(combined.getBytes("UTF-8"));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute WebSocket key", e);
        }
    }

    /**
     * READ A WEBSOCKET MESSAGE
     *
     * WebSocket frames have a specific binary format:
     *   Byte 0: FIN bit + opcode (0x81 = text message)
     *   Byte 1: MASK bit + payload length
     *   Bytes 2-5: Masking key (4 bytes) - browser always masks data
     *   Remaining: XOR-masked payload
     *
     * We must unmask the data to read the actual text.
     */
    private String readWebSocketMessage() throws IOException {
        int firstByte = inputStream.read();
        if (firstByte == -1) return null; // Connection closed

        int secondByte = inputStream.read();
        if (secondByte == -1) return null;

        // Check if this is a "close" frame (opcode 8)
        int opcode = firstByte & 0x0F;
        if (opcode == 8) return null; // Client wants to disconnect

        // Get payload length
        int payloadLength = secondByte & 0x7F;

        // Handle extended payload lengths (for messages > 125 bytes)
        if (payloadLength == 126) {
            payloadLength = (inputStream.read() << 8) | inputStream.read();
        } else if (payloadLength == 127) {
            // Very large messages - skip 8 bytes
            for (int i = 0; i < 8; i++) inputStream.read();
        }

        // Read the 4-byte masking key (browsers always mask their messages)
        boolean masked = (secondByte & 0x80) != 0;
        byte[] maskingKey = new byte[4];
        if (masked) {
            inputStream.read(maskingKey);
        }

        // Read the actual payload bytes
        byte[] payload = new byte[payloadLength];
        int bytesRead = 0;
        while (bytesRead < payloadLength) {
            int read = inputStream.read(payload, bytesRead, payloadLength - bytesRead);
            if (read == -1) return null;
            bytesRead += read;
        }

        // Unmask: XOR each byte with the corresponding masking key byte
        if (masked) {
            for (int i = 0; i < payloadLength; i++) {
                payload[i] ^= maskingKey[i % 4];
            }
        }

        return new String(payload, "UTF-8");
    }

    /**
     * SEND A WEBSOCKET MESSAGE
     *
     * When the SERVER sends to the browser, we do NOT mask.
     * Format: [0x81][length][payload]
     */
    public synchronized void sendMessage(String message) {
        if (!isConnected || socket.isClosed()) return;

        try {
            byte[] payload = message.getBytes("UTF-8");
            int length = payload.length;

            ByteArrayOutputStream frame = new ByteArrayOutputStream();

            // First byte: 0x81 = FIN bit set + opcode 1 (text)
            frame.write(0x81);

            // Second byte: payload length (no masking from server)
            if (length <= 125) {
                frame.write(length);
            } else if (length <= 65535) {
                frame.write(126);
                frame.write((length >> 8) & 0xFF);
                frame.write(length & 0xFF);
            }

            // Write the actual message
            frame.write(payload);

            outputStream.write(frame.toByteArray());
            outputStream.flush();

        } catch (IOException e) {
            // Client probably disconnected
            cleanup();
        }
    }

    /**
     * HANDLE INCOMING MESSAGE
     *
     * Messages from browser are in JSON format:
     *   {"type": "chat", "content": "Hello!"}
     *   {"type": "setName", "content": "Alice"}
     *
     * We parse manually (no external libraries needed!)
     */
    private void handleMessage(String rawMessage) {
        System.out.println("► Received from " + username + ": " + rawMessage);

        // Simple JSON parsing - extract "type" and "content" fields
        String type = extractJsonField(rawMessage, "type");
        String content = extractJsonField(rawMessage, "content");

        if (type == null) return;

        switch (type) {
            case "setName":
                // User is setting their display name
                String oldName = this.username;
                this.username = sanitize(content);
                String timestamp = getCurrentTime();

                // Notify everyone about the name change
                String nameJson = buildJson("system",
                        "🎉 " + oldName + " is now known as " + this.username,
                        "System", timestamp, ChatServer.getActiveUserCount());
                ChatServer.broadcast(nameJson, this);

                // Send user list update to everyone
                broadcastUserCount();
                break;

            case "chat":
                // Regular chat message - broadcast to everyone
                if (content != null && !content.trim().isEmpty()) {
                    String time = getCurrentTime();
                    String chatJson = buildJson("chat",
                            sanitize(content), this.username, time,
                            ChatServer.getActiveUserCount());
                    ChatServer.broadcast(chatJson, this);
                }
                break;

            case "ping":
                // Browser sending a keepalive ping
                sendMessage(buildJson("pong", "pong", "Server",
                        getCurrentTime(), ChatServer.getActiveUserCount()));
                break;
        }
    }

    /**
     * Build a simple JSON string without external libraries
     */
    private String buildJson(String type, String content,
                              String sender, String time, int userCount) {
        // Escape any quotes in the content to prevent JSON issues
        content = content.replace("\\", "\\\\").replace("\"", "\\\"");
        sender = sender.replace("\"", "\\\"");

        return String.format(
            "{\"type\":\"%s\",\"content\":\"%s\",\"sender\":\"%s\"," +
            "\"time\":\"%s\",\"userCount\":%d}",
            type, content, sender, time, userCount
        );
    }

    /**
     * Extract a field value from a simple JSON string
     * Example: {"type":"chat"} → extractJsonField(s, "type") → "chat"
     */
    private String extractJsonField(String json, String field) {
        String search = "\"" + field + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    /**
     * Remove potentially dangerous characters from user input
     */
    private String sanitize(String input) {
        if (input == null) return "";
        return input.trim()
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    /** Get current time as HH:mm string */
    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /** Tell all clients the new user count */
    private void broadcastUserCount() {
        String countJson = buildJson("userCount",
                String.valueOf(ChatServer.getActiveUserCount()),
                "Server", getCurrentTime(),
                ChatServer.getActiveUserCount());
        ChatServer.broadcast(countJson, null);
    }

    /** Clean up when user disconnects */
    private void cleanup() {
        if (!isConnected) return;
        isConnected = false;

        ChatServer.removeClient(this);

        // Tell everyone this user left
        String leaveJson = buildJson("system",
                "👋 " + username + " has left the chat",
                "System", getCurrentTime(),
                ChatServer.getActiveUserCount());
        ChatServer.broadcast(leaveJson, null);

        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    public String getUsername() { return username; }
    // Used by the router when it has already peeked at the stream
public ClientHandler(Socket socket, PushbackInputStream pis, int userId) {
    this.socket      = socket;
    this.inputStream = pis;
    this.userId      = userId;
    this.username    = "User" + userId;
}
}
