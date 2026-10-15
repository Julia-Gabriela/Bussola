package br.com.bussola.exception;

public class RegistroAnaliseNaoEncontradoException extends RuntimeException {
    public RegistroAnaliseNaoEncontradoException() {
        super("Registro não encontrado nesta decisão.");
    }
}
