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
public class RespActivacion {
    private String codResp;
    private String MsgResp;

    public RespActivacion() {
    }

    public RespActivacion(String codResp, String MsgResp) {
        this.codResp = codResp;
        this.MsgResp = MsgResp;
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
