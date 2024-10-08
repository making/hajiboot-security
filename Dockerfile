FROM bellsoft/liberica-openjdk-debian:8 as builder
WORKDIR application
COPY ./target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract && rm application.jar

FROM bellsoft/liberica-openjre-debian:8
ARG USERNAME=spring
ARG GROUPNAME=spring
ARG UID=1002
ARG GID=1000
WORKDIR application
RUN groupadd -g $GID $GROUPNAME && \
    useradd -m -s /bin/bash -u $UID -g $GID $USERNAME
RUN ARCH=$(uname -m) && \
    if [ "$ARCH" = "aarch64" ]; then \
        curl -L -o jattach.tgz https://github.com/jattach/jattach/releases/download/v2.2/jattach-linux-arm64.tgz; \
    elif [ "$ARCH" = "x86_64" ]; then \
        curl -L -o jattach.tgz https://github.com/jattach/jattach/releases/download/v2.2/jattach-linux-x64.tgz; \
    else \
        echo "Unsupported architecture: $ARCH" && exit 1; \
    fi && \
    tar -xzf jattach.tgz -C /usr/local/bin && \
    rm jattach.tgz
USER $USERNAME
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF-8 -Duser.country=JP -Duser.language=ja -Duser.timezone=Asia/Tokyo -XX:+ExitOnOutOfMemoryError -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:InitialRAMPercentage=50.0
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]