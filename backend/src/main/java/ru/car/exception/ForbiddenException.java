package ru.car.exception;

import org.springframework.http.HttpStatus;
import ru.car.enums.ErrorCode;

public class ForbiddenException extends AppException {
    public ForbiddenException(String message, Object ... args) {
        super(message, HttpStatus.FORBIDDEN, args);
    }
    public ForbiddenException(String message, ErrorCode code, Object ... args) {
        super(message, HttpStatus.FORBIDDEN, code, args);
    }

    public ForbiddenException(Throwable cause) {
        super(cause, HttpStatus.FORBIDDEN);
    }}
