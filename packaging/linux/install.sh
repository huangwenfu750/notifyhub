#!/bin/sh
# NotifyHub 一键安装脚本（systemd 发行版通用）
#   sudo ./install.sh              # 安装到 /opt/notifyhub
#   sudo ./install.sh /usr/local   # 自定义前缀
set -eu

SRC=$(cd "$(dirname "$0")" && pwd)
PREFIX="${1:-/opt/notifyhub}"
CONF_DIR=/etc/notifyhub
LOG_DIR=/var/log/notifyhub
RUN_DIR=/run/notifyhub

[ "$(id -u)" -eq 0 ] || { echo "请用 root 运行：sudo ./install.sh"; exit 1; }

echo ">> 安装到 $PREFIX"
mkdir -p "$PREFIX"
cp -a "$SRC/bin" "$SRC/lib" "$SRC/etc" "$PREFIX/"
[ -d "$SRC/jre" ] && cp -a "$SRC/jre" "$PREFIX/"
[ -d "$SRC/share" ] && cp -a "$SRC/share" "$PREFIX/"
chmod +x "$PREFIX/bin/notifyhub"
# 归档在部分文件系统上可能丢失可执行位，这里兜底
[ -d "$PREFIX/jre/bin" ] && chmod +x "$PREFIX/jre/bin"/* 2>/dev/null || true
chmod +x "$PREFIX/jre/lib/jspawnhelper" "$PREFIX/jre/lib/jexec" 2>/dev/null || true

# 运行用户
if ! id notifyhub >/dev/null 2>&1; then
    echo ">> 创建系统用户 notifyhub"
    useradd --system --home-dir "$PREFIX" --shell /sbin/nologin notifyhub 2>/dev/null || \
    useradd -r -d "$PREFIX" -s /sbin/nologin notifyhub
fi

# 配置
mkdir -p "$CONF_DIR"
if [ ! -f "$CONF_DIR/notifyhub.yaml" ]; then
    cp "$PREFIX/etc/notifyhub.yaml" "$CONF_DIR/notifyhub.yaml"
    echo ">> 生成配置 $CONF_DIR/notifyhub.yaml（请修改 auth.tokens 与各平台 webhook）"
else
    echo ">> 保留既有配置 $CONF_DIR/notifyhub.yaml"
fi

# 目录与权限
mkdir -p "$LOG_DIR" "$RUN_DIR" "$PREFIX/var/log" "$PREFIX/var/run"
chown -R notifyhub:notifyhub "$LOG_DIR" "$RUN_DIR" "$PREFIX/var" 2>/dev/null || true
chown -R notifyhub:notifyhub "$PREFIX" 2>/dev/null || true
chmod 640 "$CONF_DIR/notifyhub.yaml" 2>/dev/null || true
chgrp notifyhub "$CONF_DIR/notifyhub.yaml" 2>/dev/null || true

# SELinux（部分发行版默认 enforcing，如 RHEL 系）：
# 从 home 目录拷来的文件会带 user_home_t，systemd 无法执行 → 报 203/EXEC。
# restorecon 按发行版策略恢复成 bin_t / lib_t / etc_t 即可。
if command -v restorecon >/dev/null 2>&1; then
    restorecon -R "$PREFIX" "$CONF_DIR" "$LOG_DIR" 2>/dev/null || true
    echo ">> 已恢复 SELinux 安全上下文"
fi

# systemd
if [ -d /run/systemd/system ] || command -v systemctl >/dev/null 2>&1; then
    install -m 0644 "$SRC/systemd/notifyhub.service" /etc/systemd/system/notifyhub.service
    systemctl daemon-reload
    systemctl enable notifyhub >/dev/null 2>&1 || true
    echo ">> 已安装 systemd 单元"
    echo "   启动: systemctl start notifyhub"
    echo "   状态: systemctl status notifyhub"
    echo "   自启: systemctl enable notifyhub（已执行）"
else
    echo ">> 未检测到 systemd，手动启动："
    echo "   $PREFIX/bin/notifyhub start"
fi

echo
echo "完成。默认监听 0.0.0.0:9987，配置: $CONF_DIR/notifyhub.yaml"
