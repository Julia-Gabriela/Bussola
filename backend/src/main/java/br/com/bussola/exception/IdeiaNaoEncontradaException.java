package br.com.bussola.exception;

public class IdeiaNaoEncontradaException extends RuntimeException {
    public IdeiaNaoEncontradaException() {
        super("Ideia não encontrada nesta decisão.");
    }
}
