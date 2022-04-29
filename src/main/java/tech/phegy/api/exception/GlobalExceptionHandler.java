package tech.phegy.api.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Bean
    public ErrorAttributes errorAttributes() {
        // Hide exception field in the return object
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
                errorAttributes.remove("exception");
                errorAttributes.remove("trace");
                return errorAttributes;
            }
        };
    }

    @ExceptionHandler(PhegyHttpException.class)
    public void handleCustomException(HttpServletResponse res, PhegyHttpException ex) throws IOException {
        if (!res.isCommitted()) {
            res.sendError(ex.getHttpStatus().value(), ex.getMessage());
        }
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public void handleUsernameNotFoundException(HttpServletResponse res, UsernameNotFoundException ex) throws IOException {
        if (!res.isCommitted()) {
            res.sendError(HttpStatus.NOT_FOUND.value(), String.format("USER_%s_NOT_FOUND", ex.getMessage()));
        }
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDeniedException(HttpServletResponse res) throws IOException {
        if (!res.isCommitted()) {
            res.sendError(HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED");
        }
    }

    @ExceptionHandler(Exception.class)
    public void handleException(HttpServletResponse res) throws IOException {
        if (!res.isCommitted()) {
            res.sendError(HttpStatus.BAD_REQUEST.value(), "SOMETHING_WENT_WRONG");
        }
    }
}
