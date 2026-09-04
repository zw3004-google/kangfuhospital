package cn.hospital.rehab.common.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(boolean success, T data, String message, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, OffsetDateTime.now());
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, null, message, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, data, message, OffsetDateTime.now());
    }
}
