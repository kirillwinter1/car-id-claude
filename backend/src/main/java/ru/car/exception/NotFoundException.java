package ru.car.exception;

import org.springframework.http.HttpStatus;
import ru.car.enums.ErrorCode;

public class NotFoundException extends AppException {
    public NotFoundException(String message, Object ... args) {
        super(message, HttpStatus.NOT_FOUND, args);
    }

    public NotFoundException(String message, ErrorCode code, Object... args) {
        super(message, HttpStatus.NOT_FOUND, code, args);
    }

    public NotFoundException(Throwable cause) {
        super(cause, HttpStatus.NOT_FOUND);
    }
}
