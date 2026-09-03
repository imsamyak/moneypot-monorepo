package com.moneypot.shared.exception.validation;

import com.moneypot.shared.exception.BaseException;
import com.moneypot.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class ValidationException extends BaseException {

    public ValidationException(String message) {
        super(message, ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String message, ErrorCode errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String message, ErrorCode errorCode, Map<String, Object> details) {
        super(message, errorCode, HttpStatus.BAD_REQUEST, details);
    }
}
