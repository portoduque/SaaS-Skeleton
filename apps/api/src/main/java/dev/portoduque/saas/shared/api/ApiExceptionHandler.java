package dev.portoduque.saas.shared.api;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
final class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Violation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::toViolation)
                .sorted(Comparator.comparing(Violation::field).thenComparing(Violation::message))
                .toList();
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                "One or more request fields are invalid.",
                request);
        problem.setProperty("violations", violations);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Violation> violations = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error -> new Violation(
                        result.getMethodParameter().getParameterName(), message(error))))
                .sorted(Comparator.comparing(Violation::field).thenComparing(Violation::message))
                .toList();
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                "One or more request fields are invalid.",
                request);
        problem.setProperty("violations", violations);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem && problem.getProperties() != null
                && problem.getProperties().containsKey("code")) {
            return super.createResponseEntity(body, headers, status, request);
        }
        return super.createResponseEntity(genericProblem(status, request), headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
        LOG.error("Unhandled request failure type={}", exception.getClass().getName());
        ProblemDetail problem = problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Internal server error",
                "An unexpected error occurred.",
                request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(ApiProblemException.class)
    ResponseEntity<Object> handleApiProblem(ApiProblemException exception, WebRequest request) {
        ProblemDetail problem = problem(
                exception.status(),
                exception.code(),
                exception.title(),
                exception.safeDetail(),
                request);
        return ResponseEntity.status(exception.status()).body(problem);
    }

    private static ProblemDetail genericProblem(HttpStatusCode status, WebRequest request) {
        return switch (status.value()) {
            case 404 -> problem(
                    status, "NOT_FOUND", "Resource not found", "The requested resource was not found.", request);
            case 405 -> problem(
                    status,
                    "METHOD_NOT_ALLOWED",
                    "Method not allowed",
                    "The HTTP method is not supported for this resource.",
                    request);
            case 415 -> problem(
                    status,
                    "UNSUPPORTED_MEDIA_TYPE",
                    "Unsupported media type",
                    "The request content type is not supported.",
                    request);
            default -> {
                if (status.is4xxClientError()) {
                    yield problem(
                            status,
                            "INVALID_REQUEST",
                            "Invalid request",
                            "The request could not be processed.",
                            request);
                }
                yield problem(
                        status,
                        "INTERNAL_ERROR",
                        "Internal server error",
                        "An unexpected error occurred.",
                        request);
            }
        };
    }

    private static ProblemDetail problem(
            HttpStatusCode status, String code, String title, String detail, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(((ServletWebRequest) request).getRequest().getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("requestId", MDC.get(CorrelationIdFilter.MDC_KEY));
        return problem;
    }

    private static Violation toViolation(FieldError error) {
        return new Violation(error.getField(), message(error));
    }

    private static String message(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return message == null ? "is invalid" : message;
    }

    private record Violation(String field, String message) {}
}
