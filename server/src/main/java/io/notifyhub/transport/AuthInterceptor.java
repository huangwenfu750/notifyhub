package io.notifyhub.transport;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.util.Set;

/**
 * API token 鉴权：metadata 键 x-api-token。
 * token 白名单为空时放行所有请求；Ping 始终放行（健康检查）。
 */
public final class AuthInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> TOKEN_KEY =
            Metadata.Key.of("x-api-token", Metadata.ASCII_STRING_MARSHALLER);
    private static final String PING_METHOD = "notify.v1.Notify/Ping";

    private final Set<String> tokens;

    public AuthInterceptor(Set<String> tokens) {
        this.tokens = Set.copyOf(tokens);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        if (tokens.isEmpty() || PING_METHOD.equals(call.getMethodDescriptor().getFullMethodName())) {
            return next.startCall(call, headers);
        }
        String token = headers.get(TOKEN_KEY);
        if (token == null || !tokens.contains(token)) {
            call.close(Status.UNAUTHENTICATED.withDescription("缺少或无效的 x-api-token"), headers);
            return new ServerCall.Listener<>() {};
        }
        return next.startCall(call, headers);
    }
}
