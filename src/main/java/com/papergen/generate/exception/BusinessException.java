package com.papergen.generate.exception;

import com.papergen.generate.constant.HttpStatus;

/**
 * 业务异常，统一由 GlobalExceptionHandler 转换为 R 响应。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
