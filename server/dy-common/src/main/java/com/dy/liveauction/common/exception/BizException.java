package com.dy.liveauction.common.exception;

import lombok.Getter;

/**
 * 业务异常 —— 在 service 层抛出，GlobalExceptionHandler 统一捕获
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(String message) {
        super(message);
        this.code = 500;
    }
}
