package br.uel.invskins.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ErroControllerAdvice {
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleValidacao(ConstraintViolationException e) {
        String mensagem = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(mensagem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNaoEncontrado(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
    }
}
