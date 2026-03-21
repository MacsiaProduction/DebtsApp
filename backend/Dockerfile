FROM alpine/java:22-jdk
VOLUME /tmp
EXPOSE 8080
COPY build/libs/DebtsApp-0.1.0.jar debts_tg_bot.jar
ENTRYPOINT ["java","-jar","/debts_tg_bot.jar"]