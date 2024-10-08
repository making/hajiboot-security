FROM bellsoft/liberica-openjdk-debian:8 as builder
WORKDIR application
ADD ./.mvn .mvn/
ADD ./mvnw mvnw
ADD ./pom.xml pom.xml
ADD ./src src/
RUN ./mvnw -V clean package -DskipTests --no-transfer-progress && \
    cp target/*.jar application.jar && \
    java -Djarmode=layertools -jar application.jar extract

FROM bellsoft/liberica-openjre-debian:8
ARG USERNAME=spring
ARG GROUPNAME=spring
ARG UID=1002
ARG GID=1000
WORKDIR application
RUN groupadd -g $GID $GROUPNAME && \
    useradd -m -s /bin/bash -u $UID -g $GID $USERNAME
USER $USERNAME
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]