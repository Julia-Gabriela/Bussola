package br.com.bussola.exception;

public class EtapaInvalidaException extends RuntimeException {
    public EtapaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
