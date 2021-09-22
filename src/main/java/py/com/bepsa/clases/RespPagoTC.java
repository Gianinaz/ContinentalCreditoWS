/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package py.com.bepsa.clases;

/**
 *
 * @author rarce
 */
public class RespPagoTC {
    private String nroTransaccion ;
    private String fechaAfecDisp ;
    private String fechaExtracto ;
    private String codResp;
    private String MsgResp;

    public RespPagoTC() {
    }

    public RespPagoTC(String nroTransaccion, String fechaAfecDisp, String fechaExtracto, String codResp, String MsgResp) {
        this.nroTransaccion = nroTransaccion;
        this.fechaAfecDisp = fechaAfecDisp;
        this.fechaExtracto = fechaExtracto;
        this.codResp = codResp;
        this.MsgResp = MsgResp;
    }

    public String getNroTransaccion() {
        return nroTransaccion;
    }

    public void setNroTransaccion(String nroTransaccion) {
        this.nroTransaccion = nroTransaccion;
    }

    public String getFechaAfecDisp() {
        return fechaAfecDisp;
    }

    public void setFechaAfecDisp(String fechaAfecDisp) {
        this.fechaAfecDisp = fechaAfecDisp;
    }

    public String getFechaExtracto() {
        return fechaExtracto;
    }

    public void setFechaExtracto(String fechaExtracto) {
        this.fechaExtracto = fechaExtracto;
    }

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
}
