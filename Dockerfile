FROM openjdk:23
VOLUME /tmp
EXPOSE 8081
COPY build/libs/DebtsApp-0.1.0.jar debts_tg_bot.jar
ENTRYPOINT ["java","-jar","/debts_tg_bot.jar"]