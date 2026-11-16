package ar.com.modarelli.catalogo.domain.exception;

public class ErrorCatalogo extends RuntimeException {
    private final int estado;

    public ErrorCatalogo(int estado, String mensaje) {
        super(estado + " " + mensaje);
        this.estado = estado;
    }

    public int estado() {
        return estado;
    }
}
