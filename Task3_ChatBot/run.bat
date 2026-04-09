@echo off
echo ╔══════════════════════════════════════╗
echo ║   CODTECH CHAT SERVER - LAUNCHER     ║
echo ╚══════════════════════════════════════╝
echo.
 
REM Step 1: Compile both Java files
echo [1/2] Compiling Java files...
javac -d out src\ChatServer.java src\ClientHandler.java
 
IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo ✗ Compilation FAILED. Make sure Java JDK is installed.
    echo   Download from: https://adoptium.net/
    pause
    exit /b 1
)
 
echo ✓ Compilation successful!
echo.
 
REM Step 2: Run the server
echo [2/2] Starting server on port 8080...
echo.
echo ► Now open  index.html  in your browser
echo ► Open multiple tabs to test multiple users!
echo.
java -cp out ChatServer
 
pause