FROM openjdk:8

EXPOSE 80

WORKDIR /app
COPY . ./

RUN javac ./src/SimpleServer.java

WORKDIR ./bin

CMD ["java", "SimpleServer", "80"]
