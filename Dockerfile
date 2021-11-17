FROM azul/zulu-openjdk:17
WORKDIR /app
COPY configurations/ ./
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

COPY src ./src

CMD ["./mvnw", "spring-boot:run"]