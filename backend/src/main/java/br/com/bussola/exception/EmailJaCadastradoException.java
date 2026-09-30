package br.com.bussola.exception;

public class EmailJaCadastradoException extends IllegalStateException {

    public EmailJaCadastradoException(String mensagem) {
        super(mensagem);
    }
}
