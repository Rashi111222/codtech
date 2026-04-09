#!/bin/bash
echo "╔══════════════════════════════════════╗"
echo "║   CODTECH CHAT SERVER - LAUNCHER     ║"
echo "╚══════════════════════════════════════╝"
echo ""
 
mkdir -p out
 
echo "[1/2] Compiling Java files..."
javac -d out src/ChatServer.java src/ClientHandler.java
 
if [ $? -ne 0 ]; then
    echo "✗ Compilation FAILED. Make sure Java JDK is installed."
    echo "  Install with: sudo apt install default-jdk"
    exit 1
fi
 
echo "✓ Compilation successful!"
echo ""
echo "[2/2] Starting server on port 8080..."
echo ""
echo "► Now open index.html in your browser"
echo "► Open multiple tabs to test multiple users!"
echo ""
java -cp out ChatServer
 