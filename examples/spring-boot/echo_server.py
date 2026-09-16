"""本地回显服务：接收 NotifyHub 投递的 webhook，把请求体打印到终端。

用法: python echo_server.py [port]     # 默认 19800
"""
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer


class EchoHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        print("ECHO:", body.decode("utf-8", "replace"), flush=True)
        self.send_response(200)
        self.end_headers()

    def log_message(self, fmt, *args):  # 静音默认访问日志
        pass


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 19800
    print(f"echo server listening on 127.0.0.1:{port}", flush=True)
    HTTPServer(("127.0.0.1", port), EchoHandler).serve_forever()
