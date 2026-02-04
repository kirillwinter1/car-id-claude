package ru.car.exception;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.HttpStatus;
import ru.car.enums.ErrorCode;

@Getter
public abstract class AppException extends RuntimeException {
    private HttpStatus status;
    private Object[] args;
    private ErrorCode code = ErrorCode.EMPTY_CODE;

    public AppException(String message, HttpStatus status, ErrorCode code, Object ... args) {
        super(message);
        this.args = args;
        this.status = status;
        this.code = code;
    }

    public AppException(String message, HttpStatus status, Object ... args) {
        super(message);
        this.args = args;
        this.status = status;
    }

    public AppException(Throwable cause, HttpStatus status) {
        super(cause);
        this.status = status;
    }

    @Override
    public String getMessage() {
        return ArrayUtils.isEmpty(args) ? super.getMessage() : String.format(super.getMessage(), args);
    }
}

