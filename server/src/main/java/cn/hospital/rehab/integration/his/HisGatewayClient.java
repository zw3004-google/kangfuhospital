package cn.hospital.rehab.integration.his;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class HisGatewayClient {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final HisProperties properties;
    private final ObjectMapper mapper;

    public HisGatewayClient(HisProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    public Page fetch(HisSyncType type, int page) {
        properties.validate();
        ObjectNode body = mapper.createObjectNode();
        body.put("TransactionCode", type.transactionCode());
        body.put("MessageId", UUID.randomUUID().toString());
        body.put("RequestTime", LocalDateTime.now().format(TIME));
        body.put("OrgCode", properties.getOrganizationCode());
        body.put("AppId", properties.getApplicationId());
        body.put("LicId", properties.getLicenseId());
        ObjectNode data = body.putObject("Data");
        data.put("page", page);
        data.put("size", properties.getPageSize());
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getGatewayUrl()))
                    .timeout(properties.getReadTimeout())
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new HisGatewayException("HIS网关HTTP错误：" + response.statusCode());
            return parse(response.body(), page, properties.getPageSize());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new HisGatewayException("HIS请求被中断", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new HisGatewayException("HIS网关调用失败", exception);
        }
    }

    Page parse(String value, int requestedPage, int requestedSize) {
        try {
            JsonNode root = mapper.readTree(value);
            String ack = text(root, "AckCode", "ackCode");
            if (ack != null && !List.of("0", "00", "0000", "SUCCESS", "success").contains(ack)) {
                String message = text(root, "AckMessage", "ackMessage", "message", "msg");
                throw new HisGatewayException("HIS业务响应失败：" + ack + (message == null ? "" : "，" + message));
            }
            JsonNode array = findArray(root);
            List<Map<String, Object>> rows = new ArrayList<>();
            if (array != null) for (JsonNode item : array) {
                if (item.isObject()) rows.add(mapper.convertValue(item, LinkedHashMap.class));
            }
            int total = integer(root, rows.size(), "total", "Total", "totalCount", "TotalCount");
            int page = integer(root, requestedPage, "page", "Page", "pageNo", "PageNo", "current");
            int size = integer(root, requestedSize, "size", "Size", "pageSize", "PageSize");
            return new Page(rows, total, page, size);
        } catch (JsonProcessingException exception) {
            throw new HisGatewayException("HIS响应不是有效JSON", exception);
        }
    }

    private static JsonNode findArray(JsonNode node) {
        if (node == null) return null;
        if (node.isArray()) return node;
        for (String name : List.of("rows", "Rows", "list", "List", "records", "Records", "items", "Items")) {
            JsonNode found = find(node, name);
            if (found != null && found.isArray()) return found;
        }
        for (String name : List.of("data", "Data", "body", "Body", "result", "Result")) {
            JsonNode child = node.get(name);
            JsonNode found = findArray(child);
            if (found != null) return found;
        }
        return null;
    }

    private static JsonNode find(JsonNode node, String name) {
        if (node == null) return null;
        JsonNode direct = node.get(name);
        if (direct != null) return direct;
        if (node.isContainerNode()) for (JsonNode child : node) {
            JsonNode found = find(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private static String text(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode node = find(root, name);
            if (node != null && !node.isNull() && node.isValueNode()) return node.asText().trim();
        }
        return null;
    }

    private static int integer(JsonNode root, int fallback, String... names) {
        String value = text(root, names);
        if (value == null || value.isBlank()) return fallback;
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; }
    }

    public record Page(List<Map<String, Object>> rows, int total, int page, int size) {}
}
