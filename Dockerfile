FROM bellsoft/liberica-openjdk-debian:8 as builder
WORKDIR application
RUN --mount=type=cache,target=/var/lib/apt,sharing=locked \
    --mount=type=cache,target=/var/cache/apt,sharing=locked \
    apt-get update -qq && \
    apt-get install unzip --no-install-recommends -y -qq
# or
#RUN curl -sL -o unzip.deb http://ftp.de.debian.org/debian/pool/main/u/unzip/unzip_6.0-26+deb11u1_$(uname -m | sed -e 's/x86_/amd/' -e 's/aarch/arm/').deb && \
#    dpkg -i unzip.deb && \
#    rm -f unzip.deb
RUN --mount=type=cache,target=/root/.m2/,sharing=locked \
    --mount=type=bind,target=.,readwrite \
    ./mvnw -V clean package -DskipTests --no-transfer-progress
RUN --mount=type=bind,target=. \
    java -Djarmode=layertools -jar target/*.jar extract --destination /opt && \
    bash ./docker/class_counter.sh /opt/application /opt/dependencies > /opt/class_count

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
COPY --from=builder /opt/dependencies/ ./
COPY --from=builder /opt/spring-boot-loader/ ./
COPY --from=builder /opt/snapshot-dependencies/ ./
COPY --from=builder /opt/application/ ./
COPY --from=builder /opt/class_count /opt/
COPY ./docker/entrypoint.sh ./
ENTRYPOINT ["bash", "/application/entrypoint.sh"]