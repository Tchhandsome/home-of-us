package com.homeofus.common.exception;

import lombok.Getter;

/**
 * 业务异常。
 *
 * @author tanchaohong
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
