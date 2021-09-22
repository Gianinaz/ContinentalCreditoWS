/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package py.com.bepsa.clases;

import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author rarce
 */
@XmlType(name = "Tarjeta")
public class Tarjeta {
    //Datos del Titular de la Tarjeta – Personas (Física) 
    String nroDoc;
    String tipoPers;
    String tipoDoc;
    String nombre1;
    String nombre2;
    String apellido1;
    String apellido2;
    String paisNac;
    String sexo;
    String fechaNac;
    String lugarNac;
    String estadoCivil;
    String ocupacion;
    String dirEmailPer;
    String tipDocNew;
    String nroDocNew;
    //Datos de la Tarjeta
    String nroControl;
    String idAfin;
    String idCuenta;
    String nroTarjeta;
    String tipTarjeta;
    String tipPlastico;
    String duracion;
    String nombPlastico;
    String adiPersonali;
    String nroDocPromot;
    String tipDocPromot;
    String situacion;
    String renovAuto;
    String ordena1;
    String ordena2;
    String emboza;
    //Datos del Promotor de la Tarjeta
    String nroDocProm;
    String tipPersProm;
    String tipDocProm;
    String nombre1Prom;
    String nombre2Prom;
    String apellido1Prom;
    String apellido2Prom;
    String sexoProm;
    String fechNacProm;
    String lugNacProm;
    String estadCivProm;
    String ocupaProm;
    String dirEmailProm;
    //Parámetros Tarjeta Adicional Personalizada 
    String planContad;
    String operPlanCont;
    String imporPersonal;
    String porcPersonal;
    String planAdeEfec;
    String operPlanAdeEfec;
    String imporPersonaliza;
    String porcPersonaliza;
    String permFinanciar;
    //Datos internos
    String userActualiza;
    String nroSocio;
    String motBajaTarj;

    public Tarjeta() {
        this.nroDoc = "";
        this.tipoPers = "";
        this.tipoDoc = "";
        this.nombre1 = "";
        this.nombre2 = "";
        this.apellido1 = "";
        this.apellido2 = "";
        this.paisNac = "";
        this.sexo = "";
        this.fechaNac = "";
        this.lugarNac = "";
        this.estadoCivil = "";
        this.ocupacion = "";
        this.dirEmailPer = "";
        this.tipDocNew = "";
        this.nroDocNew = "";
        this.nroControl = "";
        this.idAfin = "";
        this.idCuenta = "";
        this.nroTarjeta = "";
        this.tipTarjeta = "";
        this.tipPlastico = "";
        this.duracion = "";
        this.nombPlastico = "";
        this.adiPersonali = "";
        this.nroDocPromot = "";
        this.tipDocPromot = "";
        this.situacion = "";
        this.renovAuto = "";
        this.ordena1 = "";
        this.ordena2 = "";
        this.emboza = "";
        this.nroDocProm = "";
        this.tipPersProm = "";
        this.tipDocProm = "";
        this.nombre1Prom = "";
        this.nombre2Prom = "";
        this.apellido1Prom = "";
        this.apellido2Prom = "";
        this.sexoProm = "";
        this.fechNacProm = "";
        this.lugNacProm = "";
        this.estadCivProm = "";
        this.ocupaProm = "";
        this.dirEmailProm = "";
        this.planContad = "";
        this.operPlanCont = "";
        this.imporPersonal = "";
        this.porcPersonal = "";
        this.planAdeEfec = "";
        this.operPlanAdeEfec = "";
        this.imporPersonaliza = "";
        this.porcPersonaliza = "";
        this.permFinanciar = "";
        this.userActualiza = "";
        this.nroSocio = "";
        this.motBajaTarj = "";
    }
   
    public String getNroDoc() {
        return nroDoc;
    }

    public void setNroDoc(String nroDoc) {
        this.nroDoc = nroDoc;
    }

    public String getTipoPers() {
        return tipoPers;
    }

    public void setTipoPers(String tipoPers) {
        this.tipoPers = tipoPers;
    }

    public String getTipoDoc() {
        return tipoDoc;
    }

    public void setTipoDoc(String tipoDoc) {
        this.tipoDoc = tipoDoc;
    }

    public String getNombre1() {
        return nombre1;
    }

    public void setNombre1(String nombre1) {
        this.nombre1 = nombre1;
    }

    public String getNombre2() {
        return nombre2;
    }

    public void setNombre2(String nombre2) {
        this.nombre2 = nombre2;
    }

    public String getApellido1() {
        return apellido1;
    }

    public void setApellido1(String apellido1) {
        this.apellido1 = apellido1;
    }

    public String getApellido2() {
        return apellido2;
    }

    public void setApellido2(String apellido2) {
        this.apellido2 = apellido2;
    }

    public String getPaisNac() {
        return paisNac;
    }

    public void setPaisNac(String paisNac) {
        this.paisNac = paisNac;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getFechaNac() {
        return fechaNac;
    }

    public void setFechaNac(String fechaNac) {
        this.fechaNac = fechaNac;
    }

    public String getLugarNac() {
        return lugarNac;
    }

    public void setLugarNac(String lugarNac) {
        this.lugarNac = lugarNac;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public String getOcupacion() {
        return ocupacion;
    }

    public void setOcupacion(String ocupacion) {
        this.ocupacion = ocupacion;
    }

    public String getDirEmailPer() {
        return dirEmailPer;
    }

    public void setDirEmailPer(String dirEmailPer) {
        this.dirEmailPer = dirEmailPer;
    }

    public String getTipDocNew() {
        return tipDocNew;
    }

    public void setTipDocNew(String tipDocNew) {
        this.tipDocNew = tipDocNew;
    }

    public String getNroDocNew() {
        return nroDocNew;
    }

    public void setNroDocNew(String nroDocNew) {
        this.nroDocNew = nroDocNew;
    }

    public String getNroControl() {
        return nroControl;
    }

    public void setNroControl(String nroControl) {
        this.nroControl = nroControl;
    }

    public String getIdAfin() {
        return idAfin;
    }

    public void setIdAfin(String idAfin) {
        this.idAfin = idAfin;
    }

    public String getIdCuenta() {
        return idCuenta;
    }

    public void setIdCuenta(String idCuenta) {
        this.idCuenta = idCuenta;
    }

    public String getNroTarjeta() {
        return nroTarjeta;
    }

    public void setNroTarjeta(String nroTarjeta) {
        this.nroTarjeta = nroTarjeta;
    }

    public String getTipTarjeta() {
        return tipTarjeta;
    }

    public void setTipTarjeta(String tipTarjeta) {
        this.tipTarjeta = tipTarjeta;
    }

    public String getTipPlastico() {
        return tipPlastico;
    }

    public void setTipPlastico(String tipPlastico) {
        this.tipPlastico = tipPlastico;
    }

    public String getDuracion() {
        return duracion;
    }

    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    public String getNombPlastico() {
        return nombPlastico;
    }

    public void setNombPlastico(String nombPlastico) {
        this.nombPlastico = nombPlastico;
    }

    public String getAdiPersonali() {
        return adiPersonali;
    }

    public void setAdiPersonali(String adiPersonali) {
        this.adiPersonali = adiPersonali;
    }

    public String getNroDocPromot() {
        return nroDocPromot;
    }

    public void setNroDocPromot(String nroDocPromot) {
        this.nroDocPromot = nroDocPromot;
    }

    public String getTipDocPromot() {
        return tipDocPromot;
    }

    public void setTipDocPromot(String tipDocPromot) {
        this.tipDocPromot = tipDocPromot;
    }

    public String getSituacion() {
        return situacion;
    }

    public void setSituacion(String situacion) {
        this.situacion = situacion;
    }

    public String getRenovAuto() {
        return renovAuto;
    }

    public void setRenovAuto(String renovAuto) {
        this.renovAuto = renovAuto;
    }

    public String getOrdena1() {
        return ordena1;
    }

    public void setOrdena1(String ordena1) {
        this.ordena1 = ordena1;
    }

    public String getOrdena2() {
        return ordena2;
    }

    public void setOrdena2(String ordena2) {
        this.ordena2 = ordena2;
    }

    public String getEmboza() {
        return emboza;
    }

    public void setEmboza(String emboza) {
        this.emboza = emboza;
    }

    public String getNroDocProm() {
        return nroDocProm;
    }

    public void setNroDocProm(String nroDocProm) {
        this.nroDocProm = nroDocProm;
    }

    public String getTipPersProm() {
        return tipPersProm;
    }

    public void setTipPersProm(String tipPersProm) {
        this.tipPersProm = tipPersProm;
    }

    public String getTipDocProm() {
        return tipDocProm;
    }

    public void setTipDocProm(String tipDocProm) {
        this.tipDocProm = tipDocProm;
    }

    public String getNombre1Prom() {
        return nombre1Prom;
    }

    public void setNombre1Prom(String nombre1Prom) {
        this.nombre1Prom = nombre1Prom;
    }

    public String getNombre2Prom() {
        return nombre2Prom;
    }

    public void setNombre2Prom(String nombre2Prom) {
        this.nombre2Prom = nombre2Prom;
    }

    public String getApellido1Prom() {
        return apellido1Prom;
    }

    public void setApellido1Prom(String apellido1Prom) {
        this.apellido1Prom = apellido1Prom;
    }

    public String getApellido2Prom() {
        return apellido2Prom;
    }

    public void setApellido2Prom(String apellido2Prom) {
        this.apellido2Prom = apellido2Prom;
    }

    public String getSexoProm() {
        return sexoProm;
    }

    public void setSexoProm(String sexoProm) {
        this.sexoProm = sexoProm;
    }

    public String getFechNacProm() {
        return fechNacProm;
    }

    public void setFechNacProm(String fechNacProm) {
        this.fechNacProm = fechNacProm;
    }

    public String getLugNacProm() {
        return lugNacProm;
    }

    public void setLugNacProm(String lugNacProm) {
        this.lugNacProm = lugNacProm;
    }

    public String getEstadCivProm() {
        return estadCivProm;
    }

    public void setEstadCivProm(String estadCivProm) {
        this.estadCivProm = estadCivProm;
    }

    public String getOcupaProm() {
        return ocupaProm;
    }

    public void setOcupaProm(String ocupaProm) {
        this.ocupaProm = ocupaProm;
    }

    public String getDirEmailProm() {
        return dirEmailProm;
    }

    public void setDirEmailProm(String dirEmailProm) {
        this.dirEmailProm = dirEmailProm;
    }

    public String getPlanContad() {
        return planContad;
    }

    public void setPlanContad(String planContad) {
        this.planContad = planContad;
    }

    public String getOperPlanCont() {
        return operPlanCont;
    }

    public void setOperPlanCont(String operPlanCont) {
        this.operPlanCont = operPlanCont;
    }

    public String getImporPersonal() {
        return imporPersonal;
    }

    public void setImporPersonal(String imporPersonal) {
        this.imporPersonal = imporPersonal;
    }

    public String getPorcPersonal() {
        return porcPersonal;
    }

    public void setPorcPersonal(String porcPersonal) {
        this.porcPersonal = porcPersonal;
    }

    public String getPlanAdeEfec() {
        return planAdeEfec;
    }

    public void setPlanAdeEfec(String planAdeEfec) {
        this.planAdeEfec = planAdeEfec;
    }

    public String getOperPlanAdeEfec() {
        return operPlanAdeEfec;
    }

    public void setOperPlanAdeEfec(String operPlanAdeEfec) {
        this.operPlanAdeEfec = operPlanAdeEfec;
    }

    public String getPermFinanciar() {
        return permFinanciar;
    }

    public void setPermFinanciar(String permFinanciar) {
        this.permFinanciar = permFinanciar;
    }

    public String getUserActualiza() {
        return userActualiza;
    }

    public void setUserActualiza(String userActual) {
        this.userActualiza = userActual;
    }

    public String getNroSocio() {
        return nroSocio;
    }

    public void setNroSocio(String nroSocio) {
        this.nroSocio = nroSocio;
    }

    public String getMotBajaTarj() {
        return motBajaTarj;
    }

    public void setMotBajaTarj(String motBajaTarj) {
        this.motBajaTarj = motBajaTarj;
    }

    public String getImporPersonaliza() {
        return imporPersonaliza;
    }

    public void setImporPersonaliza(String imporPersonaliza) {
        this.imporPersonaliza = imporPersonaliza;
    }

    public String getPorcPersonaliza() {
        return porcPersonaliza;
    }

    public void setPorcPersonaliza(String porcPersonaliza) {
        this.porcPersonaliza = porcPersonaliza;
    }
}
