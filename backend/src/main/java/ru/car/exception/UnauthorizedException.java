package ru.car.exception;

import org.springframework.http.HttpStatus;
import ru.car.enums.ErrorCode;

public class UnauthorizedException extends AppException {
    public UnauthorizedException(String message, Object... args) {
        super(message, HttpStatus.UNAUTHORIZED, args);
    }

    public UnauthorizedException(String message, ErrorCode code, Object... args) {
        super(message, HttpStatus.UNAUTHORIZED, code, args);
    }

    public UnauthorizedException(Throwable cause) {
        super(cause, HttpStatus.UNAUTHORIZED);
    }
}
