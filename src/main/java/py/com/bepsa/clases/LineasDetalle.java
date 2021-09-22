/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package py.com.bepsa.clases;

/**
 *
 * @author jfleitas
 */
public class LineasDetalle {
    
    private String nroTarjeta;
    private String tipoTransaccion;
    private String fechaOperacion;
    private String fechaProceso;
    private String nroCupon;
    private String descripcion;
    private String cantidadCuotas;

    public LineasDetalle() {
    }
    
    public LineasDetalle(String nroTarjeta, String tipoTransaccion, String fechaOperacion, String fechaProceso, String nroCupon, String descripcion, String cantidadCuotas, String importe) {
        this.nroTarjeta = nroTarjeta;
        this.tipoTransaccion = tipoTransaccion;
        this.fechaOperacion = fechaOperacion;
        this.fechaProceso = fechaProceso;
        this.nroCupon = nroCupon;
        this.descripcion = descripcion;
        this.cantidadCuotas = cantidadCuotas;
        this.importe = importe;
    }

    private String importe;

    public String getNroTarjeta() {
        return nroTarjeta;
    }

    public void setNroTarjeta(String nroTarjeta) {
        this.nroTarjeta = nroTarjeta;
    }

    public String getTipoTransaccion() {
        return tipoTransaccion;
    }

    public void setTipoTransaccion(String tipoTransaccion) {
        this.tipoTransaccion = tipoTransaccion;
    }

    public String getFechaOperacion() {
        return fechaOperacion;
    }

    public void setFechaOperacion(String fechaOperacion) {
        this.fechaOperacion = fechaOperacion;
    }

    public String getFechaProceso() {
        return fechaProceso;
    }

    public void setFechaProceso(String fechaProceso) {
        this.fechaProceso = fechaProceso;
    }

    public String getNroCupon() {
        return nroCupon;
    }

    public void setNroCupon(String nroCupon) {
        this.nroCupon = nroCupon;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImporte() {
        return importe;
    }

    public void setImporte(String importe) {
        this.importe = importe;
    }

    public String getCantidadCuotas() {
        return cantidadCuotas;
    }

    public void setCantidadCuotas(String cantidadCuotas) {
        this.cantidadCuotas = cantidadCuotas;
    }

}
