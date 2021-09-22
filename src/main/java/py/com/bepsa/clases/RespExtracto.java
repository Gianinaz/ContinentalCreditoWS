/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package py.com.bepsa.clases;

import java.util.List;

/**
 *
 * @author 
 */
public class RespExtracto {
    private String codResp;
    private String MsgResp;
    private String nombreCliente;
    private String marca;
    private String clase;
    private String afinidad;
    private String moneda;
    private String lineaCredito;
    private String dispCompraNormal;
    private String dispCompraCuotas;
    private String dispAvanceEfectivo;
    private String deudaCompraNormal;
    private String deudaCompraCuotas;
    private String deudaTotal;
    private String deudaEnMora;
    private String pagoMinPendiente;
    private String fechaVtoPagoMin;
    private String fechaProxCierre;
    private List<TarConMovimientos> tarConMovimientos;
    private List<LineasDetalle> lineasDetalle;
    private String mensajeExtracto;

    public RespExtracto() {
        this.codResp = "";
        this.MsgResp = "";
        this.nombreCliente = "";
        this.marca = "";
        this.clase = "";
        this.afinidad = "";
        this.moneda = "";
        this.lineaCredito = "";
        this.dispCompraNormal = "";
        this.dispCompraCuotas = "";
        this.dispAvanceEfectivo = "";
        this.deudaCompraNormal = "";
        this.deudaCompraCuotas = "";
        this.deudaTotal = "";
        this.deudaEnMora = "";
        this.pagoMinPendiente = "";
        this.fechaVtoPagoMin = "";
        this.fechaProxCierre = "";
        this.mensajeExtracto = "";
        this.nroTransaccion = "";
    }
    private String nroTransaccion;

    public String getCodResp() {
        return codResp;
    }

    public void setCodResp(String codResp) {
        this.codResp = codResp;
    }

    public String getMsgResp() {
        return MsgResp;
    }

    public void setMsgResp(String MsgResp) {
        this.MsgResp = MsgResp;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getClase() {
        return clase;
    }

    public void setClase(String clase) {
        this.clase = clase;
    }

    public String getAfinidad() {
        return afinidad;
    }

    public void setAfinidad(String afinidad) {
        this.afinidad = afinidad;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getLineaCredito() {
        return lineaCredito;
    }

    public void setLineaCredito(String lineaCredito) {
        this.lineaCredito = lineaCredito;
    }

    public String getDispCompraNormal() {
        return dispCompraNormal;
    }

    public void setDispCompraNormal(String dispCompraNormal) {
        this.dispCompraNormal = dispCompraNormal;
    }

    public String getDispCompraCuotas() {
        return dispCompraCuotas;
    }

    public void setDispCompraCuotas(String dispCompraCuotas) {
        this.dispCompraCuotas = dispCompraCuotas;
    }

    public String getDispAvanceEfectivo() {
        return dispAvanceEfectivo;
    }

    public void setDispAvanceEfectivo(String dispAvanceEfectivo) {
        this.dispAvanceEfectivo = dispAvanceEfectivo;
    }

    public String getDeudaCompraNormal() {
        return deudaCompraNormal;
    }

    public void setDeudaCompraNormal(String deudaCompraNormal) {
        this.deudaCompraNormal = deudaCompraNormal;
    }

    public String getDeudaCompraCuotas() {
        return deudaCompraCuotas;
    }

    public void setDeudaCompraCuotas(String deudaCompraCuotas) {
        this.deudaCompraCuotas = deudaCompraCuotas;
    }

    public String getDeudaTotal() {
        return deudaTotal;
    }

    public void setDeudaTotal(String deudaTotal) {
        this.deudaTotal = deudaTotal;
    }

    public String getDeudaEnMora() {
        return deudaEnMora;
    }

    public void setDeudaEnMora(String deudaEnMora) {
        this.deudaEnMora = deudaEnMora;
    }

    public String getPagoMinPendiente() {
        return pagoMinPendiente;
    }

    public void setPagoMinPendiente(String pagoMinPendiente) {
        this.pagoMinPendiente = pagoMinPendiente;
    }

    public String getFechaVtoPagoMin() {
        return fechaVtoPagoMin;
    }

    public void setFechaVtoPagoMin(String fechaVtoPagoMin) {
        this.fechaVtoPagoMin = fechaVtoPagoMin;
    }

    public String getFechaProxCierre() {
        return fechaProxCierre;
    }

    public void setFechaProxCierre(String fechaProxCierre) {
        this.fechaProxCierre = fechaProxCierre;
    }

    public String getMensajeExtracto() {
        return mensajeExtracto;
    }

    public void setMensajeExtracto(String mensajeExtracto) {
        this.mensajeExtracto = mensajeExtracto;
    }

    public String getNroTransaccion() {
        return nroTransaccion;
    }

    public void setNroTransaccion(String nroTransaccion) {
        this.nroTransaccion = nroTransaccion;
    }

    public List<TarConMovimientos> getTarConMovimientos() {
        return tarConMovimientos;
    }

    public void setTarConMovimientos(List<TarConMovimientos> tarConMovimientos) {
        this.tarConMovimientos = tarConMovimientos;
    }

    public List<LineasDetalle> getLineasDetalle() {
        return lineasDetalle;
    }

    public void setLineasDetalle(List<LineasDetalle> lineasDetalle) {
        this.lineasDetalle = lineasDetalle;
    }
    
    

}
