package dev.poslavskyi.devices.device.api.error;

import dev.poslavskyi.devices.device.domain.DeviceNotFoundException;
import dev.poslavskyi.devices.device.domain.DeviceOperationNotAllowedException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    ProblemDetail handleNotFound(DeviceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Device not found", ex.getMessage());
    }

    @ExceptionHandler(DeviceOperationNotAllowedException.class)
    ProblemDetail handleOperationNotAllowed(DeviceOperationNotAllowedException ex) {
        return problem(HttpStatus.CONFLICT, "Operation not allowed", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "The device was modified by another request. Fetch the latest version and retry.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String detail = cause instanceof IllegalArgumentException ? cause.getMessage() : "Malformed request body";
        return problem(HttpStatus.BAD_REQUEST, "Invalid request body", detail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String detail = cause instanceof IllegalArgumentException && cause.getMessage() != null
                ? cause.getMessage()
                : "Invalid value for parameter '" + ex.getName() + "'";
        return problem(HttpStatus.BAD_REQUEST, "Invalid parameter", detail);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(title);
        pd.setDetail(detail);
        return pd;
    }
}
