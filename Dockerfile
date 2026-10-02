FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN javac BankData.java BankServer.java

EXPOSE 8080

CMD ["java", "BankServer"]
