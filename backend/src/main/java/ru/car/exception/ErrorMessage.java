package ru.car.exception;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class ErrorMessage {
    private String method;
    @Builder.Default
    private String result = "false";
    private String error_code;
    private String error_message;
    @Builder.Default
    private HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
}
