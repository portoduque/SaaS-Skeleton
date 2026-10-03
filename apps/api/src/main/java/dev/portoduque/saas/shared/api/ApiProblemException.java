package dev.portoduque.saas.shared.api;

import org.springframework.http.HttpStatus;

public final class ApiProblemException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final String safeDetail;

    public ApiProblemException(HttpStatus status, String code, String title, String safeDetail) {
        super(code);
        this.status = status;
        this.code = code;
        this.title = title;
        this.safeDetail = safeDetail;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String safeDetail() {
        return safeDetail;
    }
}
