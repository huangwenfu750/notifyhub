# Docker 部署

> English: [README.en.md](README.en.md)

运行时镜像，直接放入 `scripts/package-linux.sh` 产出的发行包，不在容器里编译，
适合小内存机器（镜像内只跑一个 JRE + 服务端 jar）。

## 1. 准备文件

```bash
./scripts/package-linux.sh --no-jre
cp build/linux/notifyhub-0.1.0-linux-x86_64-nojre.tar.gz packaging/docker/
cp config.example.yaml packaging/docker/config.yaml   # 改 token 与 platforms
cd packaging/docker
docker compose up -d --build
```

`config.yaml` 会被只读挂载到容器里的 `/etc/notifyhub/config.yaml`。

## 2. 常用命令

```bash
docker compose ps                 # 状态与 healthcheck
docker compose logs -f            # 日志
docker compose restart            # 改完配置重启
docker compose down               # 停止并删除容器
```

## 3. 目录说明

- `Dockerfile`：`eclipse-temurin:21-jre` + 发行包，非 root 用户 `notifyhub` 运行
- `docker-compose.yml`：端口 9987、配置只读挂载、`restart: unless-stopped`、TCP 健康检查
- `config.yaml`：不进版本库，需自行准备

## 4. 从源码构建镜像

仓库根目录的 `Dockerfile` / `docker-compose.yml` 走的是容器内 Gradle 构建
（`docker compose up --build`），适合 CI；本机资源紧张时用本目录的运行时镜像更快。
