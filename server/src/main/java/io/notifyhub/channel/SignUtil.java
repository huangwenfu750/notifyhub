package io.notifyhub.channel;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

/** HMAC 签名工具（钉钉 / 飞书 / 通用 Webhook 共用）。 */
public final class SignUtil {

    private SignUtil() {}

    public static byte[] hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 不可用", e);
        }
    }

    /** 钉钉加签：base64(HmacSHA256(key=secret, data=timestamp + "\n" + secret))，再 urlencode。 */
    public static String dingTalkSign(String secret, long timestampMs) {
        String stringToSign = timestampMs + "\n" + secret;
        byte[] sig = hmacSha256(secret, stringToSign);
        return URLEncoder.encode(Base64.getEncoder().encodeToString(sig), StandardCharsets.UTF_8);
    }

    /**
     * 飞书加签：base64(HmacSHA256(key=timestamp + "\n" + secret, data=""))。
     * 注意飞书的密钥和数据与钉钉相反。
     */
    public static String feishuSign(String secret, long timestampSec) {
        String key = timestampSec + "\n" + secret;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 不可用", e);
        }
    }

    /** 通用 Webhook 签名：hex(HmacSHA256(key=secret, data=原始请求体))。 */
    public static String webhookSignHex(String secret, String body) {
        return HexFormat.of().formatHex(hmacSha256(secret, body));
    }
}
