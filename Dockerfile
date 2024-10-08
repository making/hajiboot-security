FROM bellsoft/liberica-openjdk-debian:8 as builder
WORKDIR application
RUN apt-get update -qq && \
    apt-get install unzip --no-install-recommends -y -qq
COPY ./target/*.jar application.jar
COPY ./docker/class_counter.sh ./
RUN java -Djarmode=layertools -jar application.jar extract && \
    rm application.jar && \
    bash class_counter.sh application dependencies > class_count && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean
FROM bellsoft/liberica-openjre-debian:8
ARG USERNAME=spring
ARG GROUPNAME=spring
ARG UID=1002
ARG GID=1000
WORKDIR application
RUN groupadd -g $GID $GROUPNAME && \
    useradd -m -s /bin/bash -u $UID -g $GID $USERNAME
RUN curl -sL -o memory-calculator.tgz https://java-buildpack.cloudfoundry.org/memory-calculator/trusty/x86_64/memory-calculator-3.13.0_RELEASE.tar.gz && \
    tar -xzf memory-calculator.tgz -C /usr/local/bin && \
    rm -f memory-calculator.tgz
USER $USERNAME
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
COPY --from=builder application/class_count /opt/
COPY ./docker/entrypoint.sh ./
ENTRYPOINT ["bash", "/application/entrypoint.sh"]