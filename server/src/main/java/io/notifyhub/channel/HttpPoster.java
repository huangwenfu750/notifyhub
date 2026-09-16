package io.notifyhub.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** 出站 HTTP POST（JSON）。所有渠道适配器共用。 */
public final class HttpPoster {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http;
    private final Duration timeout;

    public HttpPoster() {
        this(Duration.ofSeconds(10));
    }

    public HttpPoster(Duration timeout) {
        this.timeout = timeout;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** @param body 以 Map 构造的 JSON 请求体 */
    public SendResult postJson(String url, Map<String, Object> body, Map<String, String> headers) {
        String json;
        try {
            json = MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            return SendResult.failure("请求体序列化失败: " + e.getMessage());
        }
        return postRawJson(url, json, headers);
    }

    /** 发送已序列化的 JSON 字符串（签名需对原始字节计算时使用）。 */
    public SendResult postRawJson(String url, String json, Map<String, String> headers) {
        try {
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, java.nio.charset.StandardCharsets.UTF_8));
            if (headers != null) headers.forEach(rb::header);
            HttpResponse<String> resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            if (status < 200 || status >= 300) {
                return SendResult.failure("HTTP " + status + ": " + abbreviate(resp.body()));
            }
            return SendResult.success(resp.body());
        } catch (java.net.http.HttpTimeoutException e) {
            return SendResult.failure("请求超时: " + url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SendResult.failure("请求被中断");
        } catch (Exception e) {
            return SendResult.failure("请求失败: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    public static String toJson(Map<String, Object> body) {
        try {
            return MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String readField(String json, String field) {
        try {
            var node = MAPPER.readTree(json);
            return node == null ? null : (node.path(field).isValueNode() ? node.path(field).asText() : null);
        } catch (Exception e) {
            return null;
        }
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
