package ru.car.exception;

import org.springframework.http.HttpStatus;
import ru.car.enums.ErrorCode;

public class BadRequestException extends AppException {
    public BadRequestException(String message, Object ... args) {
        super(message, HttpStatus.BAD_REQUEST, args);
    }

    public BadRequestException(String message, ErrorCode code, Object ... args) {
        super(message, HttpStatus.BAD_REQUEST, code, args);
    }

    public BadRequestException(Throwable cause) {
        super(cause, HttpStatus.BAD_REQUEST);
    }
}