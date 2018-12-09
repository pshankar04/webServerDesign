FROM openjdk:8

EXPOSE 80

WORKDIR /app


RUN mkdir bin
COPY . ./
RUN chmod a+x bootstrap.sh
RUN ./bootstrap.sh

RUN javac ./src/SimpleServer.java -d ./bin/

WORKDIR ./bin

CMD ["java", "SimpleServer", "80"]
