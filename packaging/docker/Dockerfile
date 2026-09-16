# NotifyHub 运行时镜像：直接放入已构建的 Linux 发行包，不在镜像里编译。
#
# 前置：先出包
#   ./scripts/package-linux.sh --no-jre
#   cp build/linux/notifyhub-<ver>-linux-x86_64-nojre.tar.gz packaging/docker/
#
# 构建/运行：
#   docker build --build-arg DIST_FILE=notifyhub-<ver>-linux-x86_64-nojre.tar.gz -t notifyhub:<ver> .
#   docker run -d --name notifyhub -p 9987:9987 \
#     -v "$PWD/config.yaml":/etc/notifyhub/config.yaml:ro notifyhub:<ver>
#
# 用精简包（无内置 JRE）即可：基础镜像已经提供 Java 21。
ARG JRE_IMAGE=eclipse-temurin:21-jre
FROM ${JRE_IMAGE}

ARG DIST_FILE=notifyhub-0.1.0-linux-x86_64-nojre.tar.gz
COPY ${DIST_FILE} /tmp/notifyhub.tar.gz

RUN set -eux; \
    mkdir -p /opt/notifyhub; \
    tar -xzf /tmp/notifyhub.tar.gz -C /opt/notifyhub --strip-components=1; \
    rm -f /tmp/notifyhub.tar.gz; \
    chmod +x /opt/notifyhub/bin/notifyhub; \
    groupadd -r notifyhub && useradd -r -g notifyhub -d /opt/notifyhub notifyhub; \
    chown -R notifyhub:notifyhub /opt/notifyhub

ENV NOTIFYHUB_CONFIG=/etc/notifyhub/config.yaml \
    NOTIFYHUB_JAVA_OPTS="-Xms64m -Xmx256m -XX:+UseG1GC"

EXPOSE 9987

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD bash -c '</dev/tcp/127.0.0.1/9987' || exit 1

USER notifyhub
WORKDIR /opt/notifyhub
ENTRYPOINT ["/opt/notifyhub/bin/notifyhub"]
