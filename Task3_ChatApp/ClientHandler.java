package Task3_ChatApp;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.text.*;
import java.util.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private InputStream in;
    private OutputStream out;

    volatile String username;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");
    private static final String WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final byte[] preRead;
private final int preReadLen;

// Original constructor (not used anymore but keep it)
public ClientHandler(Socket socket) {
    this.socket = socket;
    this.preRead = null;
    this.preReadLen = 0;
}

// New constructor — receives the already-read bytes
public ClientHandler(Socket socket, byte[] preRead, int preReadLen) {
    this.socket = socket;
    this.preRead = preRead;
    this.preReadLen = preReadLen;
}

    @Override
    public void run() {
        try {
           // Wrap the input stream so the pre-read bytes come first
InputStream rawIn = socket.getInputStream();
if (preRead != null && preReadLen > 0) {
    in = new SequenceInputStream(
        new ByteArrayInputStream(preRead, 0, preReadLen), rawIn
    );
} else {
    in = rawIn;
}
out = socket.getOutputStream();

            // Step 1: WebSocket handshake
            if (!doHandshake()) {
                socket.close();
                return;
            }

            // Step 2: First message from client is the username
            String name = readFrame();
            if (name == null || name.isBlank()) { socket.close(); return; }
            username = name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "");
            if (username.isBlank()) username = "User" + (int)(Math.random() * 9999);

            // Step 3: Register and announce
            ChatServer.clients.add(this);
            sendMessage(event("users", "Server", ChatServer.onlineUsers()));
            System.out.println(username + " joined. Online: " + ChatServer.clients.size());
            ChatServer.broadcastAll(event("join", username, username + " joined the chat"));
            ChatServer.broadcastAll(event("users", "Server", ChatServer.onlineUsers()));

            // Step 4: Read loop — runs until client disconnects
            String frame;
            while ((frame = readFrame()) != null) {
                handleMessage(frame.trim());
            }

        } catch (IOException e) {
            // client disconnected
        } finally {
            cleanup();
        }
    }

    private void handleMessage(String text) {
        if (text.isBlank()) return;
        String ts = SDF.format(new Date());

        // Private message: /msg targetUser hello there
        if (text.startsWith("/msg ")) {
            String[] parts = text.split(" ", 3);
            if (parts.length < 3) {
                sendMessage(event("error", "Server", "Usage: /msg username message"));
                return;
            }
            String targetName = parts[1];
            String pmText = parts[2];
            ClientHandler target = findClient(targetName);
            if (target == null) {
                sendMessage(event("error", "Server", "User '" + targetName + "' not found"));
                return;
            }
            String pm = pm(username, targetName, pmText, ts);
            target.sendMessage(pm);
            sendMessage(pm); // echo to sender
            return;
        }

        // /users command
        if (text.equals("/users")) {
            sendMessage(event("users", "Server", ChatServer.onlineUsers()));
            return;
        }

        // Normal message — broadcast to everyone
        System.out.println("[" + username + "]: " + text);
        ChatServer.broadcastAll(message(username, text, ts));
    }

    private ClientHandler findClient(String name) {
        for (ClientHandler c : ChatServer.clients)
            if (name.equalsIgnoreCase(c.username)) return c;
        return null;
    }

    private void cleanup() {
        ChatServer.clients.remove(this);
        try { socket.close(); } catch (IOException ignored) {}
        if (username != null) {
            System.out.println(username + " disconnected. Online: " + ChatServer.clients.size());
            ChatServer.broadcastAll(event("leave", username, username + " left the chat"));
        }
    }

    // ── JSON builders (no external library needed) ──────────────────────────

    private String message(String user, String msg, String ts) {
        return "{\"type\":\"message\",\"username\":" + q(user) +
               ",\"message\":" + q(msg) +
               ",\"timestamp\":" + q(ts) +
               ",\"users\":" + q(ChatServer.onlineUsers()) + "}";
    }

    private String event(String type, String user, String msg) {
        return "{\"type\":" + q(type) +
               ",\"username\":" + q(user) +
               ",\"message\":" + q(msg) +
               ",\"timestamp\":" + q(SDF.format(new Date())) +
               ",\"users\":" + q(ChatServer.onlineUsers()) + "}";
    }

    private String pm(String from, String to, String msg, String ts) {
        return "{\"type\":\"pm\",\"from\":" + q(from) +
               ",\"to\":" + q(to) +
               ",\"message\":" + q(msg) +
               ",\"timestamp\":" + q(ts) + "}";
    }

    private static String q(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")
                       .replace("\n","\\n").replace("\r","\\r") + "\"";
    }

    // ── WebSocket send: synchronized so two threads can't interleave bytes ──

    synchronized void sendMessage(String text) {
        try {
            byte[] payload = text.getBytes(StandardCharsets.UTF_8);
            int len = payload.length;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buf.write(0x81); // FIN + text opcode
            if (len <= 125) {
                buf.write(len);
            } else if (len <= 65535) {
                buf.write(126);
                buf.write((len >> 8) & 0xFF);
                buf.write(len & 0xFF);
            } else {
                buf.write(127);
                for (int i = 7; i >= 0; i--) buf.write((int)((len >> (8*i)) & 0xFF));
            }
            buf.write(payload);
            out.write(buf.toByteArray());
            out.flush();
        } catch (IOException ignored) {}
    }

    // ── WebSocket receive ────────────────────────────────────────────────────

    private String readFrame() throws IOException {
        int b1 = in.read(); if (b1 == -1) return null;
        int b2 = in.read(); if (b2 == -1) return null;

        boolean masked = (b2 & 0x80) != 0;
        long payloadLen = b2 & 0x7F;

        if (payloadLen == 126) {
            payloadLen = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (payloadLen == 127) {
            payloadLen = 0;
            for (int i = 0; i < 8; i++) payloadLen = (payloadLen << 8) | (in.read() & 0xFF);
        }

        if (payloadLen > 65536) return null;

        byte[] mask = new byte[4];
        if (masked) for (int i = 0; i < 4; i++) mask[i] = (byte) in.read();

        byte[] payload = new byte[(int) payloadLen];
        int read = 0;
        while (read < payload.length) {
            int r = in.read(payload, read, payload.length - read);
            if (r == -1) return null;
            read += r;
        }

        if (masked) for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];

        if ((b1 & 0x0F) == 0x8) return null; // close frame

        return new String(payload, StandardCharsets.UTF_8);
    }

    // ── WebSocket HTTP upgrade handshake ─────────────────────────────────────

    private boolean doHandshake() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isBlank()) {
            int colon = line.indexOf(':');
            if (colon > 0)
                headers.put(line.substring(0, colon).trim().toLowerCase(),
                            line.substring(colon + 1).trim());
        }

        String key = headers.get("sec-websocket-key");
        if (key == null) return false;

        String accept;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest((key + WS_MAGIC).getBytes(StandardCharsets.UTF_8));
            accept = Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) { return false; }

        String response =
            "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
        return true;
    }
}