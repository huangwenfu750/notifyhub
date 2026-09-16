# ---- 构建阶段 ----
FROM gradle:9.3.0-jdk21 AS build
WORKDIR /src
# settings.gradle.kts 里 include 的每个模块目录都必须拷进来，
# 少一个（例如 sdks/spring-boot）Gradle 配置阶段就会报 "project directory does not exist"。
COPY settings.gradle.kts gradle.properties build.gradle.kts ./
COPY proto/ proto/
COPY protos/ protos/
COPY server/ server/
COPY sdks/java/ sdks/java/
COPY sdks/spring-boot/ sdks/spring-boot/
# 小内存机器（<=2G）上限制 Gradle 堆，避免构建期 OOM
ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx768m"
RUN gradle :server:installDist --no-daemon

# ---- 运行阶段 ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/server/build/install/server/ ./
ENV NOTIFYHUB_CONFIG=/etc/notifyhub/config.yaml
EXPOSE 9987
ENTRYPOINT ["bin/server"]
