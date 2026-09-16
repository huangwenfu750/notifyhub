# Docker Deployment

> 中文版：[README.md](README.md)

Runtime image: it drops the distribution produced by `scripts/package-linux.sh` straight into the
image and does not compile inside the container — good for small-memory machines (the image only
runs a JRE plus the server jar).

## 1. Prepare the Files

```bash
./scripts/package-linux.sh --no-jre
cp build/linux/notifyhub-0.1.0-linux-x86_64-nojre.tar.gz packaging/docker/
cp config.example.yaml packaging/docker/config.yaml   # edit tokens and platforms
cd packaging/docker
docker compose up -d --build
```

`config.yaml` is mounted read-only into the container at `/etc/notifyhub/config.yaml`.

## 2. Common Commands

```bash
docker compose ps                 # status and healthcheck
docker compose logs -f            # logs
docker compose restart            # restart after a config change
docker compose down               # stop and remove the container
```

## 3. Layout

- `Dockerfile`: `eclipse-temurin:21-jre` + the distribution, runs as the non-root `notifyhub` user
- `docker-compose.yml`: port 9987, read-only config mount, `restart: unless-stopped`, TCP health check
- `config.yaml`: not version-controlled, you must provide it

## 4. Building the Image from Source

The repository-root `Dockerfile` / `docker-compose.yml` run the Gradle build inside the container
(`docker compose up --build`), which suits CI; when local resources are tight, the runtime image in
this directory is faster.
