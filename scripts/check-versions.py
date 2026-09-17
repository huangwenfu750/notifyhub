#!/usr/bin/env python3
"""检查 / 统一各处的版本号。

版本号散落在多个文件里（四个 Gradle 模块、打包脚本、Python 包定义、服务端常量、
TypeScript 包、示例工程依赖），发版时漏改一处就会出现「包是 0.2.0、服务端报 0.1.0」。

用法（在仓库根目录跑）：

    python scripts/check-versions.py                 # 只检查各处是否一致
    python scripts/check-versions.py 0.2.0           # 检查是否已全部改成 0.2.0
    python scripts/check-versions.py 0.2.0 --set     # 直接把各处改成 0.2.0

退出码：全部一致 0；不一致或未改全 1。
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VERSION_RE = re.compile(r"\d+\.\d+\.\d+")

# (相对路径, 用于定位版本号的正则, 写入用的模板)
TARGETS: list[tuple[str, str, str]] = [
    ("protos/build.gradle.kts", r'^version = "[^"]+"', 'version = "{v}"'),
    ("sdks/java/build.gradle.kts", r'^version = "[^"]+"', 'version = "{v}"'),
    ("sdks/spring-boot/build.gradle.kts", r'^version = "[^"]+"', 'version = "{v}"'),
    ("server/build.gradle.kts", r'^version = "[^"]+"', 'version = "{v}"'),
    ("scripts/package-linux.py", r'^VERSION = "[^"]+"', 'VERSION = "{v}"'),
    ("sdks/python/pyproject.toml", r'^version = "[^"]+"', 'version = "{v}"'),
    ("sdks/python/src/notifyhub/__init__.py", r'^__version__ = "[^"]+"', '__version__ = "{v}"'),
    (
        "server/src/main/java/io/notifyhub/Server.java",
        r'String VERSION = "[^"]+"',
        'String VERSION = "{v}"',
    ),
    ("sdks/typescript/package.json", r'"version": "[^"]+"', '"version": "{v}"'),
    # 示例工程引的是已发布坐标，发版后跟着升
    (
        "examples/spring-boot/build.gradle.kts",
        r"notifyhub-spring-boot-starter:[0-9.]+",
        "notifyhub-spring-boot-starter:{v}",
    ),
]

# 文档里的版本号不做自动替换（可能是「自 x.y.z 起」这类历史表述），只提示人工确认
DOC_FILES = [
    "README.md",
    "README.en.md",
    "docs/usage.md",
    "docs/usage.en.md",
    "examples/spring-boot/README.md",
    "examples/spring-boot/README.en.md",
]


def read(path: str) -> str:
    # newline="" 保持文件原有换行，不要把 LF 文件改成 CRLF
    with open(ROOT / path, encoding="utf-8", newline="") as f:
        return f.read()


def write(path: str, text: str) -> None:
    with open(ROOT / path, "w", encoding="utf-8", newline="") as f:
        f.write(text)


def current_version(path: str, pattern: str) -> str | None:
    m = re.search(pattern, read(path), re.M)
    if not m:
        return None
    v = VERSION_RE.search(m.group(0))
    return v.group(0) if v else None


def set_version(path: str, pattern: str, template: str, version: str) -> None:
    text = read(path)
    new_text, n = re.subn(pattern, template.format(v=version), text, count=1, flags=re.M)
    if n == 0:
        raise SystemExit(f"没能在 {path} 里匹配到版本号，请检查脚本里的正则")
    write(path, new_text)


def main() -> int:
    ap = argparse.ArgumentParser(description="检查 / 统一 NotifyHub 各处版本号")
    ap.add_argument("version", nargs="?", help="期望的版本号，例如 0.2.0")
    ap.add_argument("--set", action="store_true", help="把各处改成给定版本号")
    args = ap.parse_args()

    if args.set and not args.version:
        raise SystemExit("--set 需要同时给出版本号")

    versions: dict[str, str | None] = {}
    for path, pattern, _ in TARGETS:
        versions[path] = current_version(path, pattern)

    if args.set:
        for path, pattern, template in TARGETS:
            set_version(path, pattern, template, args.version)
        print(f"已把 {len(TARGETS)} 处版本号改成 {args.version}")
        # 改完重新读一次，确认生效
        versions = {p: current_version(p, pat) for p, pat, _ in TARGETS}

    width = max(len(p) for p, _, _ in TARGETS)
    missing = [p for p, v in versions.items() if v is None]
    for path, _, _ in TARGETS:
        print(f"  {path:<{width}}  {versions[path]}")

    distinct = {v for v in versions.values() if v is not None}
    ok = True
    if missing:
        print("\n以下文件没找到版本号（脚本正则可能过时）：")
        for p in missing:
            print("  -", p)
        ok = False
    if len(distinct) > 1:
        print(f"\n版本号不一致：{sorted(distinct)}")
        for p, v in versions.items():
            if v and len(distinct) > 1:
                pass
        ok = False
    elif args.version and distinct and args.version not in distinct:
        print(f"\n当前是 {distinct.pop()}，期望 {args.version} —— 还没改全")
        ok = False

    if args.version and ok:
        print(f"\n全部一致：{args.version}")
    elif ok:
        print(f"\n全部一致：{distinct.pop()}")

    # 文档里的版本号提示（不自动改）
    if args.version and ok:
        hints = []
        for doc in DOC_FILES:
            file = ROOT / doc
            if not file.exists():
                continue
            hits = len(VERSION_RE.findall(read(doc)))
            if hits:
                hints.append(f"{doc}（{hits} 处）")
        if hints:
            print("\n文档里还有版本号提及，需人工确认（可能有历史表述，脚本不自动改）：")
            for h in hints:
                print("  -", h)

    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
