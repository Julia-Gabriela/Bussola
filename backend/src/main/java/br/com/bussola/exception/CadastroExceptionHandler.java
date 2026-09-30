package br.com.bussola.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CadastroExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Dados de cadastro inválidos."));
    }

    @ExceptionHandler(CadastroInvalidoException.class)
    public ResponseEntity<Map<String, String>> regraInvalida(CadastroInvalidoException exception) {
        return ResponseEntity.badRequest().body(Map.of("erro", exception.getMessage()));
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<Map<String, String>> emailDuplicado(EmailJaCadastradoException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", exception.getMessage()));
    }
}
