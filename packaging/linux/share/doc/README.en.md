# NotifyHub Linux Distribution Package

> 中文版：[README.md](README.md)

A self-contained distribution bundling JRE 21: unpack and run, no system Java required.

## Layout

```
bin/notifyhub          launcher (foreground / daemon)
lib/*.jar              server and all dependencies
jre/                   bundled Temurin JRE 21 (linux x86_64; absent in the -nojre variant)
etc/notifyhub.yaml     default configuration
systemd/notifyhub.service
install.sh             one-shot installation (/opt/notifyhub + systemd)
share/doc/README.md    this file
```

The slim package with the `-nojre` suffix does not bundle a JRE (~10MB) and suits machines that
already have Java 21+; the launcher falls back to `JAVA_HOME` or the system `java`, and everything
else behaves identically.

## Option 1: Unpack and Run

```bash
tar -xzf notifyhub-0.1.1-linux-x86_64.tar.gz
cd notifyhub-0.1.1-linux-x86_64

cp etc/notifyhub.yaml etc/my.yaml    # edit the token / platform webhook
./bin/notifyhub --config etc/my.yaml            # foreground
./bin/notifyhub start --config etc/my.yaml      # background
./bin/notifyhub status | stop | restart
./bin/notifyhub --version
```

Environment variables: `NOTIFYHUB_CONFIG`, `NOTIFYHUB_JAVA_OPTS` (overrides the default JVM
options), `NOTIFYHUB_OPTS` (appends JVM options), `NOTIFYHUB_LOG_DIR`, `NOTIFYHUB_PID_FILE`.

## Option 2: System-Wide Installation (recommended)

```bash
sudo ./install.sh            # → /opt/notifyhub, user notifyhub, config /etc/notifyhub/notifyhub.yaml
sudo systemctl start notifyhub
sudo systemctl status notifyhub
sudo journalctl -u notifyhub -f
```

## Uninstall

```bash
sudo systemctl stop notifyhub && sudo systemctl disable notifyhub
sudo rm -f /etc/systemd/system/notifyhub.service && sudo systemctl daemon-reload
sudo rm -rf /opt/notifyhub /etc/notifyhub /var/log/notifyhub
sudo userdel notifyhub
```

## Client Integration

gRPC endpoint `host:9987`, request header `x-api-token: <token>`.
Official SDKs: Java / Python / TypeScript / Go / Spring Boot Starter — see the project's `sdks/`.

## Runtime Notes

- Default JVM options `-Xms64m -Xmx256m`; for high throughput override with
  `NOTIFYHUB_JAVA_OPTS='-Xms256m -Xmx1g'`.
- When no JRE is bundled, it falls back to `JAVA_HOME` or the system `java` (21+ required).
- Port: gRPC 9987 (TCP).
