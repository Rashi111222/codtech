FROM openjdk:17

WORKDIR /app

COPY . .

RUN javac src/ChatServer.java src/ClientHandler.java

CMD ["java", "-cp", "src", "ChatServer"]