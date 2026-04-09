import java.io.*; //tools for reading/writing
import java.util.*; //tools for networking (sockets,ports, connections)
import java.net.*; //general tools like lists
import java.util.concurrent.*; //thread safe tools

public class ChatServer{
    

    //A thread safe list of all currently connnected clients
    private static final List<ClientHandler> connectedClients= new CopyOnWriteArrayList<>();

    // Tracks how many total users have ever joined (for giving unique IDs)
    private static int totalUsersJoined=0;

    public static void main(String args[]) throws IOException{
        int port;
String portEnv = System.getenv("PORT");

if (portEnv != null) {
    port = Integer.parseInt(portEnv);
} else {
    port = 8080; // fallback for local testing
}
  
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  CHAT SERVER STARTING...             ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("► Server running on port: " + port);
        System.out.println("► Open index.html in your browser to connect");
        System.out.println("► Press Ctrl+C to stop the server\n");

        //ServerSocket is what opens port 8080 on your computer and starts listening for connections. 
        // Without this line, no one can reach your server.
        //try ( ... ) is called a "try-with-resources" — it means Java will automatically close the ServerSocket when the program ends,
        //  even if it crashes. You don't have to manually close it.
        try(ServerSocket serverSocket=new ServerSocket(port)){

            //this loop runs forever. That's intentional! 
            // A server should never stop accepting users on its own.
            while(true){

                //this is the most important line in the whole file. It pauses the program and waits until someone connects. The moment a browser opens a WebSocket connection to port 8080,
                //  this line "unpauses" and returns a Socket object representing that one user.
                Socket clientSocket=serverSocket.accept();

                totalUsersJoined++;
                System.out.println("New connection #"+ totalUsersJoined+" from "+clientSocket.getInetAddress().getHostAddress());

                //Creating a thread per user
                ClientHandler newClient=new ClientHandler(clientSocket,totalUsersJoined);

                // Add them to our list of connected clients
                connectedClients.add(newClient);

                // Start a NEW THREAD for this client
                // This means the server can handle this client AND keep
                // accepting new connections at the same time!
                Thread clientThread = new Thread(newClient);
                clientThread.setDaemon(true); // Thread dies when server stops
                clientThread.start();
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

    public static void broadcast(String message, ClientHandler sender){
        //Loop through every connected client
        for(ClientHandler client:connectedClients){
            //send message to everyone including sender
            client.sendMessage(message);
        }
    }

    /*
    Remove a client from our list when they disconnect
     */
    public static void removeClient(ClientHandler client) {
        connectedClients.remove(client);
        System.out.println("► Client disconnected. Active users: " + connectedClients.size());
    }

    /**
    Get count of currently connected users
     */
    public static int getActiveUserCount() {
        return connectedClients.size();
    }
}