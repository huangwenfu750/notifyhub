package io.notifyhub.channel;

/** 单次投递结果。 */
public record SendResult(boolean ok, String error, String responseBody) {

    public static SendResult success(String body) {
        return new SendResult(true, null, body);
    }

    public static SendResult failure(String error) {
        return new SendResult(false, error, null);
    }
}
