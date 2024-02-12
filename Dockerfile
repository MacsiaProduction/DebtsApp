FROM openjdk
VOLUME /tmp
EXPOSE 8081
COPY target/DebtsApp-0.0.1-SNAPSHOT.jar debts_tg_bot.jar
ENTRYPOINT ["java","-jar","/debts_tg_bot.jar"]