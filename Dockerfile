FROM openjdk:23-jdk-slim
EXPOSE 8081
VOLUME /jar
COPY build/libs/DebtsApp-0.6.0.jar debts_tg_bot2.jar
ENTRYPOINT ["java","-Xmx256m","-XX:+UseG1GC","-jar","/debts_tg_bot2.jar"]
