FROM eclipse-temurin:17

WORKDIR /app

COPY src/ ./src/
COPY index.html ./index.html

RUN mkdir -p out && javac -d out src/ChatServer.java src/ClientHandler.java

CMD ["java", "-cp", "out", "ChatServer"]