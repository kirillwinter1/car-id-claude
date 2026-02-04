package ru.car.exception;

import org.springframework.http.HttpStatus;

public class MessageNotSendException extends AppException {
    public MessageNotSendException(String message, Object ... args) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, args);
    }

    public MessageNotSendException(Throwable cause) {
        super(cause, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
