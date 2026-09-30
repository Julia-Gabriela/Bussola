package br.com.bussola.exception;

public class CadastroInvalidoException extends IllegalArgumentException {

    public CadastroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
