package com.reliefgrid.common.exception;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Every error leaves the API as an RFC 9457 ProblemDetail. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "You do not have permission to do this");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleLock(ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Someone else changed this at the same time. Refresh and try again");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setProperty("errors", ex.getFieldErrors().stream().collect(Collectors.toMap(
                FieldError::getField,
                f -> Objects.requireNonNullElse(f.getDefaultMessage(), "invalid"),
                (a, b) -> a)));
        return ResponseEntity.badRequest().body(pd);
    }
}
