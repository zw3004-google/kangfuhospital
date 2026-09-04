package cn.hospital.rehab.arrears.push;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Component
@EnableConfigurationProperties(WeComProperties.class)
public class WeComClient {
    private final WeComProperties properties;
    private final RestClient client;
    private volatile String token;
    private volatile Instant expiresAt = Instant.MIN;

    public WeComClient(WeComProperties properties) {
        this.properties = properties;
        this.client = RestClient.builder().baseUrl(properties.base()).build();
    }

    public SendResult send(String userId, String content) {
        if (properties.secret() == null || properties.secret().isBlank())
            return new SendResult(false, "CONFIG_MISSING", "未配置企业微信应用密钥");
        if (userId == null || userId.isBlank())
            return new SendResult(false, "RECIPIENT_MISSING", "接收人企微ID为空");
        try {
            Map<?, ?> response = client.post().uri("/cgi-bin/message/send?access_token=" + accessToken())
                    .body(Map.of("touser", userId, "msgtype", "text", "agentid", properties.agentId(),
                            "text", Map.of("content", content)))
                    .retrieve().body(Map.class);
            return interpret(response);
        } catch (Exception exception) {
            return new SendResult(false, "WECOM_CALL_FAILED", "企微接口调用失败：" + exception.getMessage());
        }
    }

    static SendResult interpret(Map<?, ?> response) {
        if (response == null) return new SendResult(false, "EMPTY_RESPONSE", "企微接口返回为空");
        long code = number(response, "errcode", -1);
        if (code != 0) return new SendResult(false, String.valueOf(code), String.valueOf(response.get("errmsg")));
        for (String field : new String[]{"invaliduser", "invalidparty", "invalidtag"}) {
            Object value = response.get(field);
            if (value != null && !String.valueOf(value).isBlank())
                return new SendResult(false, field.toUpperCase(), "企微返回无效接收目标：" + value);
        }
        return new SendResult(true, null, null);
    }

    private synchronized String accessToken() {
        if (token != null && Instant.now().isBefore(expiresAt)) return token;
        Map<?, ?> response = client.get()
                .uri("/cgi-bin/gettoken?corpid=" + properties.corpId() + "&corpsecret=" + properties.secret())
                .retrieve().body(Map.class);
        if (number(response, "errcode", 0) != 0)
            throw new IllegalStateException(String.valueOf(response.get("errmsg")));
        token = String.valueOf(response.get("access_token"));
        expiresAt = Instant.now().plusSeconds(number(response, "expires_in", 7200) - 120);
        return token;
    }

    private static long number(Map<?, ?> map, String key, long defaultValue) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Number number ? number.longValue() : defaultValue;
    }

    public record SendResult(boolean success, String errorCode, String error) {}
}
