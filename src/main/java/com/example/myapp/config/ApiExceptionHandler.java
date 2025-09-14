package com.example.myapp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({ UsernameNotFoundException.class, BadCredentialsException.class })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> unauthorized(Exception ex, HttpServletRequest req) {
        return Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", ex.getMessage(),
                "path", req.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> generic(Exception ex, HttpServletRequest req) {
        ex.printStackTrace(); // utile in dev
        return Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", ex.getMessage(),
                "path", req.getRequestURI()
        );
    }
}
