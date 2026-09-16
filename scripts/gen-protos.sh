#!/usr/bin/env bash
# 生成各语言 gRPC stub。
# 用法:
#   ./scripts/gen-protos.sh            # 默认生成 Java（server 与 java sdk 共用）
#   ./scripts/gen-protos.sh java go    # 指定语言: java | python | go
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROTO_DIR="$ROOT/proto"
GEN_LANGS=("$@")
[ ${#GEN_LANGS[@]} -eq 0 ] && GEN_LANGS=(java)

PROTOC_VERSION=4.36.1
GRPC_JAVA_VERSION=1.79.0

TOOLS="$ROOT/tools"
mkdir -p "$TOOLS"

# --- protoc 二进准备（Maven Central 直下，无需安装） ---
OS="$(uname -s)"
case "$OS" in
  Linux*)  PROTOC_OS=linux;    PROTOC_ARCH="$( [ "$(uname -m)" = aarch64 ] && echo aarch_64 || echo x86_64)" ;;
  Darwin*) PROTOC_OS=osx;      PROTOC_ARCH="$( [ "$(uname -m)" = arm64 ] && echo aarch_64 || echo x86_64)" ;;
  MINGW*|MSYS*|CYGWIN*) PROTOC_OS=windows-x86_64; PROTOC_ARCH= ;;
  *) echo "不支持的系统: $OS" >&2; exit 1 ;;
esac
if [ "$PROTOC_OS" = "windows-x86_64" ]; then
  PROTOC_URL="https://repo.maven.apache.org/maven2/com/google/protobuf/protoc/${PROTOC_VERSION}/protoc-${PROTOC_VERSION}-windows-x86_64.exe"
  PROTOC="$TOOLS/protoc.exe"
else
  PROTOC_URL="https://repo.maven.apache.org/maven2/com/google/protobuf/protoc/${PROTOC_VERSION}/protoc-${PROTOC_VERSION}-${PROTOC_OS}-${PROTOC_ARCH}.exe"
  PROTOC="$TOOLS/protoc"
fi
if [ ! -f "$PROTOC" ]; then
  echo ">> 下载 protoc ${PROTOC_VERSION}"
  curl -fsSL -o "$PROTOC" "$PROTOC_URL"
  chmod +x "$PROTOC" 2>/dev/null || true
fi

# Windows 版 protoc 是原生程序，不认 MSYS 的 /c/... 路径，必须转成 C:\...
if [ "$PROTOC_OS" = "windows-x86_64" ] && command -v cygpath >/dev/null 2>&1; then
  W() { cygpath -w "$1"; }
else
  W() { printf '%s' "$1"; }
fi

# proto 相对路径的分隔符必须跟宿主一致：Windows 原生 protoc 只认反斜杠，
# 而 Linux/macOS 上传反斜杠会被当成文件名的一部分（报 "Could not make proto path relative"）。
if [ "$PROTOC_OS" = "windows-x86_64" ]; then
  PS='\'; PROTO_REL='notify\v1\notify.proto'
else
  PS='/'; PROTO_REL='notify/v1/notify.proto'
fi

gen_java() {
  local PLUGIN="$TOOLS/protoc-gen-grpc-java.exe"
  [ "$PROTOC_OS" != "windows-x86_64" ] && PLUGIN="$TOOLS/protoc-gen-grpc-java"
  if [ ! -f "$PLUGIN" ]; then
    if [ "$PROTOC_OS" = "windows-x86_64" ]; then
      PURL="https://repo.maven.apache.org/maven2/io/grpc/protoc-gen-grpc-java/${GRPC_JAVA_VERSION}/protoc-gen-grpc-java-${GRPC_JAVA_VERSION}-windows-x86_64.exe"
    else
      PURL="https://repo.maven.apache.org/maven2/io/grpc/protoc-gen-grpc-java/${GRPC_JAVA_VERSION}/protoc-gen-grpc-java-${GRPC_JAVA_VERSION}-${PROTOC_OS}-${PROTOC_ARCH}.exe"
    fi
    echo ">> 下载 protoc-gen-grpc-java ${GRPC_JAVA_VERSION}"
    curl -fsSL -o "$PLUGIN" "$PURL"
    chmod +x "$PLUGIN" 2>/dev/null || true
  fi
  local OUT="$ROOT/protos/src/main/java"
  mkdir -p "$OUT"
  local WOUT WIN
  WOUT="$(W "$OUT")"; WIN="$(W "$PROTO_DIR")"
  "$PROTOC" -I "$WIN" \
    --plugin=protoc-gen-grpc-java="$(W "$PLUGIN")" \
    --java_out="$WOUT" --grpc-java_out="$WOUT" \
    "$WIN${PS}$PROTO_REL"
  echo ">> Java stub 已生成: protos/src/main/java"
}

gen_python() {
  python -m pip install --quiet grpcio-tools
  local OUT="$ROOT/sdks/python/src/notifyhub"
  mkdir -p "$OUT"
  # 同 Windows 版 protoc：这里的 python 也是原生程序，/c/... 会被解析成 C:\c\...
  local WOUT WIN
  WOUT="$(W "$OUT")"; WIN="$(W "$PROTO_DIR")"
  python -m grpc_tools.protoc -I "$WIN" \
    --python_out="$WOUT" --grpc_python_out="$WOUT" \
    "$WIN${PS}$PROTO_REL"
  # 生成的绝对导入 notify.v1 改写为包内导入 notifyhub.notify.v1
  python - "$WOUT" <<'PYEOF'
import sys, pathlib
out = pathlib.Path(sys.argv[1])
g = out / "notify" / "v1" / "notify_pb2_grpc.py"
s = g.read_text(encoding="utf-8")
s = s.replace("from notify.v1 import notify_pb2", "from notifyhub.notify.v1 import notify_pb2")
s = s.replace("import notify.v1.notify_pb2", "import notifyhub.notify.v1.notify_pb2")
g.write_text(s, encoding="utf-8")
for p in [out, out / "notify", out / "notify" / "v1"]:
    # 只补缺失的 __init__.py，绝不覆盖——notifyhub/__init__.py 是包的公开 API
    init = p / "__init__.py"
    if not init.exists():
        init.write_text("", encoding="utf-8")
print("imports rewritten")
PYEOF
  echo ">> Python stub 已生成: sdks/python/src/notifyhub"
}

# go install 的 @version 必须带 v 前缀，否则被当成 commit revision（报 unknown revision）
PROTOC_GEN_GO_VERSION=v1.36.5
PROTOC_GEN_GO_GRPC_VERSION=v1.5.1

# 插件可执行文件后缀（windows 为 .exe）
goplugin_ext() { [ "$PROTOC_OS" = "windows-x86_64" ] && echo ".exe" || echo ""; }

# go 插件目录（GOBIN 优先，否则 GOPATH/bin）
gobin_dir() {
  local d; d="$(go env GOBIN)"
  [ -z "$d" ] && d="$(go env GOPATH)/bin"
  echo "$d"
}

# 安装 go 插件：已存在则跳过；默认走当前 GOPROXY，失败时回落国内镜像
# （proxy.golang.org 在部分地区不可达，会 Bad Gateway）
install_go_plugin() {
  local pkg="$1" ver="$2" name dir
  name="$(basename "$pkg")"
  dir="$(gobin_dir)"
  if [ -x "$dir/$name$(goplugin_ext)" ]; then
    return 0
  fi
  echo ">> go install $pkg@$ver"
  go install "$pkg@$ver" || GOPROXY=https://goproxy.cn,direct go install "$pkg@$ver"
}

gen_go() {
  # 需要 Go 工具链: go install 两个插件后生成
  command -v go >/dev/null 2>&1 || { echo "!! 未安装 Go，请先安装: https://go.dev/dl/" >&2; return 1; }
  install_go_plugin google.golang.org/protobuf/cmd/protoc-gen-go "$PROTOC_GEN_GO_VERSION"
  install_go_plugin google.golang.org/grpc/cmd/protoc-gen-go-grpc "$PROTOC_GEN_GO_GRPC_VERSION"

  local GOBIN_DIR
  GOBIN_DIR="$(gobin_dir)"

  local OUT="$ROOT/sdks/go/gen"
  mkdir -p "$OUT"
  local WOUT WIN PLUG1 PLUG2 EXT
  WOUT="$(W "$OUT")"; WIN="$(W "$PROTO_DIR")"
  EXT="$(goplugin_ext)"
  PLUG1="$(W "$GOBIN_DIR/protoc-gen-go$EXT")"
  PLUG2="$(W "$GOBIN_DIR/protoc-gen-go-grpc$EXT")"

  # 显式传插件路径：Git Bash 里把 C:\... 拼进 PATH 不可靠
  "$PROTOC" -I "$WIN" \
    --plugin=protoc-gen-go="$PLUG1" --plugin=protoc-gen-go-grpc="$PLUG2" \
    --go_out="$WOUT" --go_opt=module=github.com/notifyhub/notifyhub-sdk-go/gen \
    --go-grpc_out="$WOUT" --go-grpc_opt=module=github.com/notifyhub/notifyhub-sdk-go/gen \
    "$WIN${PS}$PROTO_REL"
  echo ">> Go stub 已生成: sdks/go/gen"
}

for lang in "${GEN_LANGS[@]}"; do
  case "$lang" in
    java)   gen_java ;;
    python) gen_python ;;
    go)     gen_go ;;
    *)      echo "未知语言: $lang（支持 java|python|go）" >&2; exit 1 ;;
  esac
done
