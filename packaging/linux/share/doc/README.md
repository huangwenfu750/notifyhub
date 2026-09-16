# NotifyHub Linux 发行包

> English: [README.en.md](README.en.md)

自带 JRE 21 的绿色发行包：解压即用，不依赖系统 Java。

## 目录结构

```
bin/notifyhub          启动器（前台/守护进程）
lib/*.jar              服务端及全部依赖
jre/                   内置 Temurin JRE 21（linux x86_64；-nojre 变体无此目录）
etc/notifyhub.yaml     默认配置
systemd/notifyhub.service
install.sh             一键安装（/opt/notifyhub + systemd）
share/doc/README.md    本文件
```

带 `-nojre` 后缀的精简包不内置 JRE（约 10MB），适合目标机已装 Java 21+ 的场景；
启动器会自动回落到 `JAVA_HOME` 或系统 `java`，其余用法完全一致。

## 方式一：解压即用

```bash
tar -xzf notifyhub-0.1.0-linux-x86_64.tar.gz
cd notifyhub-0.1.0-linux-x86_64

cp etc/notifyhub.yaml etc/my.yaml    # 改 token / 平台 webhook
./bin/notifyhub --config etc/my.yaml            # 前台
./bin/notifyhub start --config etc/my.yaml      # 后台
./bin/notifyhub status | stop | restart
./bin/notifyhub --version
```

环境变量：`NOTIFYHUB_CONFIG`、`NOTIFYHUB_JAVA_OPTS`（覆盖默认 JVM 参数）、
`NOTIFYHUB_OPTS`（追加 JVM 参数）、`NOTIFYHUB_LOG_DIR`、`NOTIFYHUB_PID_FILE`。

## 方式二：系统级安装（推荐）

```bash
sudo ./install.sh            # → /opt/notifyhub，用户 notifyhub，配置 /etc/notifyhub/notifyhub.yaml
sudo systemctl start notifyhub
sudo systemctl status notifyhub
sudo journalctl -u notifyhub -f
```

## 卸载

```bash
sudo systemctl stop notifyhub && sudo systemctl disable notifyhub
sudo rm -f /etc/systemd/system/notifyhub.service && sudo systemctl daemon-reload
sudo rm -rf /opt/notifyhub /etc/notifyhub /var/log/notifyhub
sudo userdel notifyhub
```

## 客户端接入

gRPC 端点 `host:9987`，请求头 `x-api-token: <token>`。
官方 SDK：Java / Python / TypeScript / Go / Spring Boot Starter，见项目 `sdks/`。

## 运行时说明

- 默认 JVM 参数 `-Xms64m -Xmx256m`；高吞吐场景用 `NOTIFYHUB_JAVA_OPTS='-Xms256m -Xmx1g'` 覆盖。
- 未内置 JRE 时自动回落到 `JAVA_HOME` 或系统 `java`（要求 21+）。
- 端口：gRPC 9987（TCP）。
