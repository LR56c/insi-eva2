package cl.iplacex.tiendaweb.domain;


public class Factura {
    private String rut;
    private String clientName;
    private double total;

    public Factura() {
    }

    public Factura(String rut, String clientName, double total) {
        this.rut = rut;
        this.clientName = clientName;
        this.total = total;
    }

    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}