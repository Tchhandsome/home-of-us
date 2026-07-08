package com.homeofus.common.api;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

/**
 * 统一接口响应结构。
 *
 * @param <T> 数据类型
 * @author tanchaohong
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;

    private final String code;

    private final String message;

    private final T data;

    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "SUCCESS", "处理成功", data);
    }

    /**
     * 创建失败响应。
     *
     * @param code 错误码
     * @param message 错误描述
     * @return 失败响应
     */
    public static ApiResponse<Void> failed(String code, String message) {
        String safeCode = Objects.isNull(code) ? "SYSTEM_ERROR" : code;
        String safeMessage = Objects.isNull(message) ? "系统处理异常" : message;
        return new ApiResponse<>(false, safeCode, safeMessage, null);
    }
}

