"""NotifyClient: NotifyHub gRPC 客户端薄封装。

发布、订阅、Admin（代码配置推送平台）三类操作；token 通过 metadata 注入。
"""

from __future__ import annotations

from typing import Callable, Iterable, Optional

import grpc

from .notify.v1 import notify_pb2 as pb
from .notify.v1 import notify_pb2_grpc as pb_grpc


class NotifyError(Exception):
    """服务端返回的错误（对应 gRPC Status）。"""

    def __init__(self, code: grpc.StatusCode, details: str):
        self.code = code
        self.details = details
        super().__init__(f"{code.name}: {details}")


class Event:
    """订阅收到的事件。"""

    def __init__(self, msg: pb.Event):
        self.topic = msg.topic
        self.title = msg.title
        self.content = msg.content
        self.params = dict(msg.params)
        self.event_id = msg.event_id
        self.timestamp = msg.timestamp

    def __repr__(self) -> str:  # pragma: no cover
        return f"Event(topic={self.topic!r}, title={self.title!r}, event_id={self.event_id!r})"


class PublishAck:
    """发布回执。"""

    def __init__(self, ack: pb.PublishAck):
        self.event_id = ack.event_id
        self.accepted = ack.accepted
        self.deduplicated = ack.deduplicated
        self.matched_platforms = list(ack.matched_platforms)
        self.error = ack.error

    def __repr__(self) -> str:  # pragma: no cover
        return (f"PublishAck(accepted={self.accepted}, deduplicated={self.deduplicated}, "
                f"matched={self.matched_platforms})")


class Subscription:
    """订阅句柄：close() 主动退订。"""

    def __init__(self, call: grpc.Future | object, close: Callable[[], None]):
        self._call = call
        self._close = close

    def cancel(self) -> None:
        self._close()

    close = cancel


def _wrap_grpc_error(fn):
    def inner(*args, **kwargs):
        try:
            return fn(*args, **kwargs)
        except grpc.RpcError as e:
            raise NotifyError(e.code(), e.details()) from e
    return inner


class NotifyClient:
    """与 NotifyHub 服务端的连接。线程安全，可在多线程间共享。"""

    def __init__(self, target: str, token: Optional[str] = None):
        """target 形如 "localhost:9987"。token 为服务端 auth.tokens 中的一项。"""
        self._token = token
        self._raw_channel = grpc.insecure_channel(target)
        if token:
            # 用拦截器把 x-api-token 注入每次调用
            self._channel = grpc.intercept_channel(self._raw_channel, self._make_interceptor(token))
        else:
            self._channel = self._raw_channel
        self._stub = pb_grpc.NotifyStub(self._channel)

    @staticmethod
    def _make_interceptor(token: str):
        class _Invoker(grpc.UnaryUnaryClientInterceptor,
                       grpc.UnaryStreamClientInterceptor,
                       grpc.StreamUnaryClientInterceptor,
                       grpc.StreamStreamClientInterceptor):
            def intercept_unary_unary(self, continuation, client_call_details, request):
                return continuation(self._with_token(client_call_details), request)

            def intercept_unary_stream(self, continuation, client_call_details, request):
                return continuation(self._with_token(client_call_details), request)

            def intercept_stream_unary(self, continuation, client_call_details, request_iterator):
                return continuation(self._with_token(client_call_details), request_iterator)

            def intercept_stream_stream(self, continuation, client_call_details, request_iterator):
                return continuation(self._with_token(client_call_details), request_iterator)

            @staticmethod
            def _with_token(client_call_details):
                md = list(client_call_details.metadata or [])
                md.append(("x-api-token", token))
                return client_call_details._replace(metadata=md)

        return _Invoker()

    # ---------- 发布 ----------

    @_wrap_grpc_error
    def publish(self, topic: str, title: str = "", content: str = "",
                params: Optional[dict] = None,
                platforms: Optional[Iterable[str]] = None,
                dedup_key: str = "",
                skip_subscribers: bool = False,
                skip_platforms: bool = False,
                timeout: float = 10.0) -> PublishAck:
        """发布一条通知：按路由推送平台 + 广播给订阅者。"""
        req = pb.PublishRequest(
            topic=topic, title=title, content=content,
            params=params or {},
            platforms=list(platforms or []),
            options=pb.Options(dedup_key=dedup_key,
                               skip_subscribers=skip_subscribers,
                               skip_platforms=skip_platforms),
        )
        return PublishAck(self._stub.Publish(req, timeout=timeout))

    # ---------- 批量发布（双向流） ----------

    @_wrap_grpc_error
    def publish_batch(self, requests, timeout: float = 10.0) -> list:
        """批量发布（双向流）：逐条发送并收集回执。

        requests 为 pb.PublishRequest 的可迭代对象；单条错误（topic 为空、平台不存在）
        不中断流，对应回执的 accepted=False 且带 error。
        """
        return [PublishAck(ack) for ack in self._stub.PublishStream(iter(requests), timeout=timeout)]

    def publish_stream(self, requests):
        """底层双向流：返回回执迭代器，由调用方自行消费（适合边生成边发送）。"""
        return self._stub.PublishStream(iter(requests))

    # ---------- 订阅 ----------

    def subscribe(self, topics: Iterable[str], on_event: Callable[[Event], None],
                  on_error: Optional[Callable[[Exception], None]] = None) -> Subscription:
        """订阅主题，回调在后台线程触发。"""
        import threading

        call = self._stub.Subscribe(pb.SubscribeRequest(topics=list(topics)))
        stop = threading.Event()
        finished = threading.Event()

        def pump():
            try:
                for msg in call:
                    if stop.is_set():
                        break
                    on_event(Event(msg))
            except grpc.RpcError as e:
                if e.code() != grpc.StatusCode.CANCELLED and on_error:
                    on_error(e)
            finally:
                finished.set()

        t = threading.Thread(target=pump, daemon=True, name="notifyhub-sub")
        t.start()

        def close():
            stop.set()
            call.cancel()

        return Subscription(call, close)

    # ---------- Admin ----------

    @_wrap_grpc_error
    def upsert_platform(self, name: str, type: str, webhook: str,
                        secret: str = "", topics: Optional[Iterable[str]] = None,
                        template: str = "", at_mobiles: Optional[Iterable[str]] = None,
                        extra: Optional[dict] = None,
                        rate_limit_qps: int = 0,
                        timeout: float = 10.0) -> dict:
        """用代码注册/更新推送平台（运行时生效，重启后需重新注册或写入配置文件）。"""
        cfg = pb.PlatformConfig(
            name=name, type=type, webhook=webhook, secret=secret,
            topics=list(topics or ["*"]), template=template,
            at_mobiles=list(at_mobiles or []), extra=extra or {},
            rate_limit_qps=rate_limit_qps)
        out = self._stub.UpsertPlatform(cfg, timeout=timeout)
        return {"name": out.name, "type": out.type, "topics": list(out.topics)}

    @_wrap_grpc_error
    def list_platforms(self, timeout: float = 10.0) -> list:
        out = self._stub.ListPlatforms(pb.Empty(), timeout=timeout)
        return [{"name": p.name, "type": p.type, "webhook": p.webhook,
                 "topics": list(p.topics)} for p in out.platforms]

    @_wrap_grpc_error
    def remove_platform(self, name: str, timeout: float = 10.0) -> None:
        self._stub.RemovePlatform(pb.PlatformRef(name=name), timeout=timeout)

    @_wrap_grpc_error
    def ping(self, timeout: float = 5.0) -> dict:
        out = self._stub.Ping(pb.Empty(), timeout=timeout)
        return {"version": out.version, "uptime_seconds": out.uptime_seconds}

    # ---------- 生命周期 ----------

    def close(self):
        self._raw_channel.close()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()
