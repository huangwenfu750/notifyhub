#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""打包 NotifyHub Linux 发行包（自带 JRE，解压即用）。

产物: build/linux/notifyhub-<version>-linux-x86_64.tar.gz
    bin/notifyhub      启动器（前台 / start|stop|restart|status）
    lib/*.jar          服务端与全部依赖
    jre/               Temurin JRE 21 (linux x64)
    etc/notifyhub.yaml 默认配置
    systemd/notifyhub.service
    install.sh         一键安装到 /opt/notifyhub

为什么用 tarfile 而不是 tar 命令：Windows 下 chmod 对 tar 解包出来的文件经常不生效，
GNU tar 会把错误的文件模式写进归档，导致 linux 侧 jre/bin/java 没有可执行位。
这里对每个条目显式指定 mode / uid / gid，跨平台结果一致。

用法:
    python scripts/package-linux.py            # 构建 + 打包（缺 JRE 时自动下载）
    python scripts/package-linux.py --no-jre   # 精简包：不内置 JRE，用系统 java（约 10MB）
    python scripts/package-linux.py --no-stage # 只重新打包（跳过 stage 重建）
"""
import hashlib
import os
import shutil
import sys
import tarfile
import urllib.request

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
WORK = os.path.join(ROOT, "build", "linux")
TEMPLATE = os.path.join(ROOT, "packaging", "linux")
JRE_URL = ("https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/"
           "jre/hotspot/normal/eclipse")
JRE_TAR = os.path.join(WORK, "jre21.tar.gz")

VERSION = "0.1.0"
ARCH = "x86_64"

# 需要可执行位的名字 / 目录（路径用 "/" 分隔）
EXEC_NAMES = {"notifyhub", "install.sh", "jspawnhelper", "jexec"}
JUNK = ("lib/src.zip", "demo", "sample", "man")


BUNDLE_JRE = "--no-jre" not in sys.argv


def dist_name():
    return "notifyhub-%s-linux-%s%s" % (VERSION, ARCH, "" if BUNDLE_JRE else "-nojre")


def is_exec(rel):
    unix = rel.replace("\\", "/")
    base = unix.rsplit("/", 1)[-1]
    if base in EXEC_NAMES or base.endswith((".so", ".sh")):
        return True
    parts = unix.split("/")
    return len(parts) >= 2 and parts[-2] == "bin"      # bin/ 与 jre/bin/ 下的都可执行


def to_lf(path):
    if not path.endswith((".sh", ".yaml", ".yml", ".service", ".md")) \
            and os.path.basename(path) != "notifyhub":
        return
    with open(path, "rb") as f:
        data = f.read()
    new = data.replace(b"\r\n", b"\n")
    if new != data:
        with open(path, "wb") as f:
            f.write(new)


def ensure_jre():
    os.makedirs(WORK, exist_ok=True)
    if os.path.exists(JRE_TAR) and os.path.getsize(JRE_TAR) > 20 * 1024 * 1024:
        return
    print(">> 下载 Temurin JRE 21 (linux x64) ...")
    req = urllib.request.Request(JRE_URL, headers={"User-Agent": "notifyhub-packager"})
    with urllib.request.urlopen(req, timeout=600) as r, open(JRE_TAR, "wb") as f:
        shutil.copyfileobj(r, f)
    print("   %.1f MB -> %s" % (os.path.getsize(JRE_TAR) / 1048576, JRE_TAR))


def build_stage():
    stage = os.path.join(WORK, dist_name())
    jre_src = os.path.join(WORK, "jre")
    if os.path.isdir(stage):
        shutil.rmtree(stage)
    os.makedirs(stage)

    lib_src = os.path.join(ROOT, "server", "build", "install", "server", "lib")
    if not os.path.isdir(lib_src):
        raise SystemExit("缺少 %s，请先运行: gradle :server:installDist" % lib_src)
    shutil.copytree(lib_src, os.path.join(stage, "lib"))

    if BUNDLE_JRE:
        ensure_jre()
        if os.path.isdir(jre_src):
            shutil.rmtree(jre_src)
        os.makedirs(jre_src)
        with tarfile.open(JRE_TAR, "r:gz") as tf:
            for m in tf.getmembers():
                if m.issym() or m.islnk():      # legal/ 下的文档软链，Windows 无意义
                    continue
                parts = m.name.split("/", 1)
                if len(parts) < 2 or not parts[1]:
                    continue
                m.name = parts[1]
                tf.extract(m, jre_src)
        for junk in JUNK:
            p = os.path.join(jre_src, junk)
            if os.path.isdir(p):
                shutil.rmtree(p)
            elif os.path.exists(p):
                os.remove(p)
        shutil.copytree(jre_src, os.path.join(stage, "jre"))
    else:
        print("(精简包：不内置 JRE，运行时使用 JAVA_HOME 或系统 java 21+)")

    for rel in ("bin", "etc", "systemd", "share"):
        src = os.path.join(TEMPLATE, rel)
        if os.path.isdir(src):
            shutil.copytree(src, os.path.join(stage, rel), dirs_exist_ok=True)
    shutil.copy(os.path.join(TEMPLATE, "install.sh"), os.path.join(stage, "install.sh"))
    shutil.copy(os.path.join(ROOT, "config.example.yaml"),
                os.path.join(stage, "etc", "notifyhub.example.yaml"))
    shutil.copy(os.path.join(ROOT, "README.md"),
                os.path.join(stage, "share", "doc", "README.project.md"))
    if os.path.isfile(os.path.join(ROOT, "README.en.md")):
        shutil.copy(os.path.join(ROOT, "README.en.md"),
                    os.path.join(stage, "share", "doc", "README.project.en.md"))
    shutil.copy(os.path.join(ROOT, "LICENSE"),
                os.path.join(stage, "share", "doc", "LICENSE"))

    for dirpath, _dirs, files in os.walk(stage):
        for fn in files:
            to_lf(os.path.join(dirpath, fn))
    print("stage ready:", stage)
    return stage


def make_tar(stage):
    name = dist_name()
    out = os.path.join(WORK, name + ".tar.gz")
    if os.path.exists(out):
        os.remove(out)
    n = 0
    with tarfile.open(out, "w:gz", compresslevel=6) as tf:
        for dirpath, dirnames, filenames in os.walk(stage):
            dirnames.sort()
            rel_dir = os.path.relpath(dirpath, stage).replace("\\", "/")
            arc = name if rel_dir == "." else name + "/" + rel_dir
            if rel_dir != ".":
                di = tarfile.TarInfo(arc)
                di.type, di.mode, di.uid, di.gid = tarfile.DIRTYPE, 0o755, 0, 0
                di.uname = di.gname = "root"
                tf.addfile(di)
            for fn in sorted(filenames):
                full = os.path.join(dirpath, fn)
                rel = os.path.relpath(full, stage).replace("\\", "/")
                ti = tarfile.TarInfo(name + "/" + rel)
                ti.size = os.path.getsize(full)
                ti.mtime = int(os.path.getmtime(full))
                ti.mode = 0o755 if is_exec(rel) else 0o644
                ti.uid = ti.gid = 0
                ti.uname = ti.gname = "root"
                with open(full, "rb") as f:
                    tf.addfile(ti, f)
                n += 1
    print("packed %d files -> %s (%.1f MB)" % (n, out, os.path.getsize(out) / 1048576))
    digest = hashlib.sha256(open(out, "rb").read()).hexdigest()
    with open(out + ".sha256", "w", newline="\n") as f:
        f.write("%s  %s\n" % (digest, os.path.basename(out)))
    print("sha256: %s" % digest)
    return out


if __name__ == "__main__":
    if "--no-stage" in sys.argv:
        make_tar(os.path.join(WORK, dist_name()))
    else:
        make_tar(build_stage())
