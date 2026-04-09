FROM openjdk:17

WORKDIR /app

COPY . .

RUN javac src/*.java

CMD ["java", "-cp", "src", "ChatServer"]