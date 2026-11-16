package ar.com.modarelli.catalogo.infrastructure.web.errors;

import ar.com.modarelli.catalogo.domain.exception.ErrorCatalogo;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class CatalogoErrorHandler {
    @ExceptionHandler(ErrorCatalogo.class)
    ProblemDetail manejar(ErrorCatalogo e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(e.estado()), e.getMessage());
    }
}
