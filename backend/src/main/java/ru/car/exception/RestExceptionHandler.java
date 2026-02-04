package ru.car.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.car.enums.ErrorCode;

import java.util.Objects;

@ControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorMessage> handleAppException(AppException ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        return createAnswer(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleOtherException(Exception ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        return createAnswer(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.EMPTY_CODE, getMessage(ex), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorMessage> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.error(ex.getMessage());
        return createAnswer(HttpStatus.FORBIDDEN, ErrorCode.UNKNOWN_TOKEN, getMessage(ex), request);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorMessage> handleInsufficientAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.error(ex.getMessage());
        return createAnswer(HttpStatus.FORBIDDEN, ErrorCode.UNKNOWN_TOKEN, getMessage(ex), request);
    }


    @Override
    protected ResponseEntity handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error(ex.getMessage(), ex);
        String path = getPath(request);
        if (ex.getMessage().contains("Cannot deserialize value of type `java.util.UUID` from String")) {
            if (path.contains("qr.") || path.contains("report.send")
                    || path.contains("report/send") || path.contains("report/createDraft")) {
                return createAnswer(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_QR, ErrorCode.INVALID_QR.getDescription(), request);
            } else if (path.contains("notification/") || path.contains("report/updateDraft")) {
                return createAnswer(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_NOTIFICATION_ID, ErrorCode.INVALID_NOTIFICATION_ID.getDescription(), request);
            } else {
                return createAnswer(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ID, ErrorCode.INVALID_ID.getDescription(), request);
            }
        } else {
            return super.handleHttpMessageNotReadable(ex, headers, status, request);
        }
    }

    private ResponseEntity<ErrorMessage> createAnswer(HttpStatus status, ErrorCode code, String message, WebRequest request) {
        return ResponseEntity.status(HttpStatus.OK) //TODO по договоренности с мобильщиками
                .body(ErrorMessage.builder()
                        .method(getPath(request))
                        .result("false")
                        .error_message(message)
                        .error_code(code.getCode())
                        .status(status)
                        .build());
    }

    private String getMessage(Exception ex) {
        if (Objects.nonNull(ex.getCause())) {
            if (StringUtils.isNoneEmpty(ex.getCause().getMessage())) {
                return ex.getCause().getMessage();
            }
            return ex.getCause().getClass().getSimpleName();
        }
        if (StringUtils.isNoneEmpty(ex.getMessage()) ) {
            return ex.getMessage();
        }
        return ex.getClass().getSimpleName();
    }

    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }

}
