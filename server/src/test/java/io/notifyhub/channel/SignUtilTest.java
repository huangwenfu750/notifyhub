package io.notifyhub.channel;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignUtilTest {

    @Test
    void dingTalkSignIsUrlEncodedBase64Hmac() {
        // 独立重算一遍钉钉算法作对照：base64(HmacSHA256(key=secret, data=ts+"\n"+secret))
        long ts = 1710000000000L;
        String secret = "SEC7d941c9b5e2d0d1a";
        String urlEncoded = SignUtil.dingTalkSign(secret, ts);

        byte[] mac = SignUtil.hmacSha256(secret, ts + "\n" + secret);
        String expectedRaw = Base64.getEncoder().encodeToString(mac);

        // urlencode 不改变字母数字，'='、'+'、'/' 会被转义
        String expectedEncoded = java.net.URLEncoder.encode(expectedRaw, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(expectedEncoded, urlEncoded);
        assertNotEquals(expectedRaw, urlEncoded); // 确认确实经过了 urlencode
        assertTrue(urlEncoded.indexOf('%') >= 0 || expectedRaw.matches("[A-Za-z0-9+/]*"), "签名应可解码");
    }

    @Test
    void feishuSignUsesTimestampAndSecretAsKey() {
        long ts = 1710000000L;
        String secret = "feishu-secret";
        String sign = SignUtil.feishuSign(secret, ts);
        byte[] mac = SignUtil.hmacSha256(ts + "\n" + secret, "");
        assertEquals(Base64.getEncoder().encodeToString(mac), sign);
    }

    @Test
    void webhookSignIsLowercaseHex() {
        String sign = SignUtil.webhookSignHex("secret", "body");
        assertTrue(sign.matches("[0-9a-f]{64}"), "应为 64 位小写 hex: " + sign);
    }
}
