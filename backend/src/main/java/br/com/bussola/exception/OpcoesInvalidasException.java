package br.com.bussola.exception;

public class OpcoesInvalidasException extends RuntimeException {
    public OpcoesInvalidasException(String mensagem) {
        super(mensagem);
    }
}
