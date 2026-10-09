package br.com.bussola.exception;

public class DecisaoNaoEncontradaException extends RuntimeException {
    public DecisaoNaoEncontradaException() {
        super("Decisão não encontrada.");
    }
}
