package cn.hospital.rehab.arrears.push;

import java.util.List;
import java.util.Locale;

final class PushRecordPolicy {
    private PushRecordPolicy() {}

    static List<String> databaseStatuses(String status) {
        if (status == null || status.isBlank()) return List.of();
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "SENDING", "PENDING" -> List.of("PENDING", "SENDING");
            case "RETRYING" -> List.of("RETRYING");
            case "SUCCESS", "SENT" -> List.of("SENT");
            case "FAILED" -> List.of("FAILED");
            default -> throw new IllegalArgumentException("不支持的推送状态");
        };
    }

    static String displayStatus(String status) {
        return switch (status) {
            case "PENDING", "SENDING" -> "发送中";
            case "RETRYING" -> "重试中";
            case "SENT" -> "成功";
            case "FAILED" -> "失败";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    static String summarize(String content) {
        if (content == null || content.isBlank()) return "—";
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "…";
    }

    static boolean supportedBusiness(String businessType) {
        return "ARREARS".equals(businessType) || "DISCHARGE".equals(businessType);
    }
}
