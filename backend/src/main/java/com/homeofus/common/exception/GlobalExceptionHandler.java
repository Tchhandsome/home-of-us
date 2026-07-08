package com.homeofus.common.exception;

import com.homeofus.common.api.ApiResponse;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * @author tanchaohong
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常。
     *
     * @param exception 校验异常
     * @return 统一失败响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidationException(Exception exception) {
        String message;
        if (exception instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validException = (MethodArgumentNotValidException) exception;
            message = validException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ":" + error.getDefaultMessage())
                    .collect(Collectors.joining(";"));
        } else {
            BindException bindException = (BindException) exception;
            message = bindException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ":" + error.getDefaultMessage())
                    .collect(Collectors.joining(";"));
        }
        return ApiResponse.failed("VALIDATION_ERROR", StringUtils.defaultIfBlank(message, "参数校验失败"));
    }

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常
     * @return 统一失败响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.failed(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理运行时异常。
     *
     * @param exception 运行时异常
     * @return 统一失败响应
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException exception) {
        log.error("业务处理发生异常", exception);
        return ApiResponse.failed("SYSTEM_ERROR", "系统处理异常");
    }
}
