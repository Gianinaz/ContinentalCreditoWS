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
@XmlType(name = "Cuenta")
public class Cuenta {
    //Datos de la persona
    String nroDoc;
    String tipoPers;
    String tipoDoc;
    String nombre1;
    String nombre2;
    String apellido1;
    String apellido2;
    String razonSoc;
    String denomComer;
    String paisDoc;
    String sexo;
    String fechaNac;
    String lugarNac;
    String rucExtr;
    String estadoCivil;
    String ocupacion;
    String dirEmailPer;
    String tipDocNew;
    String nroDocNew;
    //Datos de la cuenta
    String nroControl;
    String nroCuenta;
    String codAfin;
    String tipCuenta;
    String dirRecibo;
    String dirExtr1;
    String dirExtr2;
    String dirExtr3;
    String depart;
    String ciudad;
    String zona;
    String nroTel;
    String nroCel;
    String dirEmailCta;
    String codSuc;
    String nroDocOfCta;
    String tipDocOfCta;
    String ctaVip;
    String franqueo;
    String aplSegVida;
    String cobCosto;
    String tipCosto;
    String retenExtr;
    String motReten;
    String tipCierre;
    String empAdher;
    String califBCP;
    String situacion;
    String codCliEnt;
    String nroSocio;
    String bonosCobrand;
    //Datos financieros de la cuenta
    String tipLin1Norm;
    String linCredNorm;
    String tipLin2Cuota;
    String linCredCuota;
    String persTTI;
    String codTTICorriente;
    String tICorriente;
    String codTTIMora;
    String tIMora;
    String codTTIComp;
    String tIcomp;
    String persPagMin;
    String porcPagMin;
    String impoMinPagMin;
    String impoFijoPagMin;
    String ModPago;
    String tipCtaBanc;
    String ctaBanc;
    String tipPago;
    String ValCGF;
    String TipTasaFin;
    String FactMultVIP;
    String CobrandID;
    //Datos del Codeudor de la Cuenta
    String nroDocCod;
    String tipPerCod;
    String tipDocCod;
    String nombre1Cod;
    String nombre2Cod;
    String apellido1Cod;
    String apellido2Cod;
    String sexoCod;
    String fechNacCod;
    String lugNacCod;
    String estCivCod;
    String ocupacionCod;
    String dirEmailCod;
    //Datos del oficial de la cuenta
    String nroDocOfi;
    String tipPerOfi;
    String tipDocOfi;
    String nombre1Ofi;
    String nombre2Ofi;
    String apellido1Ofi;
    String apellido2Ofi;
    String sexoOfi;
    String fechNacOfi;
    String lugNacOfi;
    String estCivOfi;
    String ocupacionOfi;
    String dirEmailofi;
    //Excepciones Costos por Cuenta
    String costNoAplica;
    String aplicaImp;
    //Datos internos
    String userActualiza;
    String motNovCta;
    //Datos promocion
    String partProCupAltera;
    //Excepciones Cargos del Cierre por Cuenta
    String cargNoAplica;

    public Cuenta() {
        this.nroDoc = "";
        this.tipoPers = "";
        this.tipoDoc = "";
        this.nombre1 = "";
        this.nombre2 = "";
        this.apellido1 = "";
        this.apellido2 = "";
        this.razonSoc = "";
        this.denomComer = "";
        this.paisDoc = "";
        this.sexo = "";
        this.fechaNac = "";
        this.lugarNac = "";
        this.rucExtr = "";
        this.estadoCivil = "";
        this.ocupacion = "";
        this.dirEmailPer = "";
        this.tipDocNew = "";
        this.nroDocNew = "";
        this.nroControl = "";
        this.nroCuenta = "";
        this.codAfin = "";
        this.tipCuenta = "";
        this.dirRecibo = "";
        this.dirExtr1 = "";
        this.dirExtr2 = "";
        this.dirExtr3 = "";
        this.depart = "";
        this.ciudad = "";
        this.zona = "";
        this.nroTel = "";
        this.nroCel = "";
        this.dirEmailCta = "";
        this.codSuc = "";
        this.nroDocOfCta = "";
        this.tipDocOfCta = "";
        this.ctaVip = "";
        this.franqueo = "";
        this.aplSegVida = "";
        this.cobCosto = "";
        this.tipCosto = "";
        this.retenExtr = "";
        this.motReten = "";
        this.tipCierre = "";
        this.empAdher = "";
        this.califBCP = "";
        this.situacion = "";
        this.codCliEnt = "";
        this.nroSocio = "";
        this.bonosCobrand = "";
        this.tipLin1Norm = "";
        this.linCredNorm = "0";
        this.tipLin2Cuota = "";
        this.linCredCuota = "0";
        this.persTTI = "";
        this.codTTICorriente = "";
        this.tICorriente = "";
        this.codTTIMora = "";
        this.tIMora = "";
        this.codTTIComp = "";
        this.tIcomp = "";
        this.persPagMin = "";
        this.porcPagMin = "";
        this.impoMinPagMin = "";
        this.impoFijoPagMin = "";
        this.ModPago = "";
        this.tipCtaBanc = "";
        this.ctaBanc = "";
        this.tipPago = "";
        this.ValCGF = "";
        this.TipTasaFin = "";
        this.FactMultVIP = "";
        this.CobrandID = "";
        this.nroDocCod = "";
        this.tipPerCod = "";
        this.tipDocCod = "";
        this.nombre1Cod = "";
        this.nombre2Cod = "";
        this.apellido1Cod = "";
        this.apellido2Cod = "";
        this.sexoCod = "";
        this.fechNacCod = "";
        this.lugNacCod = "";
        this.estCivCod = "";
        this.ocupacionCod = "";
        this.dirEmailCod = "";
        this.nroDocOfi = "";
        this.tipPerOfi = "";
        this.tipDocOfi = "";
        this.nombre1Ofi = "";
        this.nombre2Ofi = "";
        this.apellido1Ofi = "";
        this.apellido2Ofi = "";
        this.sexoOfi = "";
        this.fechNacOfi = "";
        this.lugNacOfi = "";
        this.estCivOfi = "";
        this.ocupacionOfi = "";
        this.dirEmailofi = "";
        this.costNoAplica = "";
        this.aplicaImp = "";
        this.userActualiza = "";
        this.motNovCta = "";
        this.partProCupAltera = "";
        this.cargNoAplica = "";
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

    public String getRazonSoc() {
        return razonSoc;
    }

    public void setRazonSoc(String razonSoc) {
        this.razonSoc = razonSoc;
    }

    public String getDenomComer() {
        return denomComer;
    }

    public void setDenomComer(String denomComer) {
        this.denomComer = denomComer;
    }

    public String getPaisDoc() {
        return paisDoc;
    }

    public void setPaisDoc(String paisDoc) {
        this.paisDoc = paisDoc;
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

    public String getRucExtr() {
        return rucExtr;
    }

    public void setRucExtr(String rucExtr) {
        this.rucExtr = rucExtr;
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

    public String getNroCuenta() {
        return nroCuenta;
    }

    public void setNroCuenta(String nroCuenta) {
        this.nroCuenta = nroCuenta;
    }

    public String getCodAfin() {
        return codAfin;
    }

    public void setCodAfin(String codAfin) {
        this.codAfin = codAfin;
    }

    public String getTipCuenta() {
        return tipCuenta;
    }

    public void setTipCuenta(String tipCuenta) {
        this.tipCuenta = tipCuenta;
    }

    public String getDirRecibo() {
        return dirRecibo;
    }

    public void setDirRecibo(String dirRecibo) {
        this.dirRecibo = dirRecibo;
    }

    public String getDirExtr1() {
        return dirExtr1;
    }

    public void setDirExtr1(String dirExtr1) {
        this.dirExtr1 = dirExtr1;
    }

    public String getDirExtr2() {
        return dirExtr2;
    }

    public void setDirExtr2(String dirExtr2) {
        this.dirExtr2 = dirExtr2;
    }

    public String getDirExtr3() {
        return dirExtr3;
    }

    public void setDirExtr3(String dirExtr3) {
        this.dirExtr3 = dirExtr3;
    }

    public String getDepart() {
        return depart;
    }

    public void setDepart(String depart) {
        this.depart = depart;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getNroTel() {
        return nroTel;
    }

    public void setNroTel(String nroTel) {
        this.nroTel = nroTel;
    }

    public String getNroCel() {
        return nroCel;
    }

    public void setNroCel(String nroCel) {
        this.nroCel = nroCel;
    }

    public String getDirEmailCta() {
        return dirEmailCta;
    }

    public void setDirEmailCta(String dirEmailCta) {
        this.dirEmailCta = dirEmailCta;
    }

    public String getCodSuc() {
        return codSuc;
    }

    public void setCodSuc(String codSuc) {
        this.codSuc = codSuc;
    }

    public String getNroDocOfCta() {
        return nroDocOfCta;
    }

    public void setNroDocOfCta(String nroDocOfCta) {
        this.nroDocOfCta = nroDocOfCta;
    }

    public String getTipDocOfCta() {
        return tipDocOfCta;
    }

    public void setTipDocOfCta(String tipDocOfCta) {
        this.tipDocOfCta = tipDocOfCta;
    }

    public String getCtaVip() {
        return ctaVip;
    }

    public void setCtaVip(String ctaVip) {
        this.ctaVip = ctaVip;
    }

    public String getFranqueo() {
        return franqueo;
    }

    public void setFranqueo(String franqueo) {
        this.franqueo = franqueo;
    }

    public String getAplSegVida() {
        return aplSegVida;
    }

    public void setAplSegVida(String aplSegVida) {
        this.aplSegVida = aplSegVida;
    }

    public String getCobCosto() {
        return cobCosto;
    }

    public void setCobCosto(String cobCosto) {
        this.cobCosto = cobCosto;
    }

    public String getTipCosto() {
        return tipCosto;
    }

    public void setTipCosto(String tipCosto) {
        this.tipCosto = tipCosto;
    }

    public String getRetenExtr() {
        return retenExtr;
    }

    public void setRetenExtr(String retenExtr) {
        this.retenExtr = retenExtr;
    }

    public String getMotReten() {
        return motReten;
    }

    public void setMotReten(String motReten) {
        this.motReten = motReten;
    }

    public String getTipCierre() {
        return tipCierre;
    }

    public void setTipCierre(String tipCierre) {
        this.tipCierre = tipCierre;
    }

    public String getEmpAdher() {
        return empAdher;
    }

    public void setEmpAdher(String empAdher) {
        this.empAdher = empAdher;
    }

    public String getCalifBCP() {
        return califBCP;
    }

    public void setCalifBCP(String califBCP) {
        this.califBCP = califBCP;
    }

    public String getSituacion() {
        return situacion;
    }

    public void setSituacion(String situacion) {
        this.situacion = situacion;
    }

    public String getCodCliEnt() {
        return codCliEnt;
    }

    public void setCodCliEnt(String codCliEnt) {
        this.codCliEnt = codCliEnt;
    }

    public String getNroSocio() {
        return nroSocio;
    }

    public void setNroSocio(String nroSocio) {
        this.nroSocio = nroSocio;
    }

    public String getBonosCobrand() {
        return bonosCobrand;
    }

    public void setBonosCobrand(String bonosCobrand) {
        this.bonosCobrand = bonosCobrand;
    }

    public String getTipLin1Norm() {
        return tipLin1Norm;
    }

    public void setTipLin1Norm(String tipLin1Norm) {
        this.tipLin1Norm = tipLin1Norm;
    }

    public String getLinCredNorm() {
        return linCredNorm;
    }

    public void setLinCredNorm(String linCredNorm) {
        this.linCredNorm = linCredNorm;
    }

    public String getTipLin2Cuota() {
        return tipLin2Cuota;
    }

    public void setTipLin2Cuota(String tipLin2Cuota) {
        this.tipLin2Cuota = tipLin2Cuota;
    }

    public String getLinCredCuota() {
        return linCredCuota;
    }

    public void setLinCredCuota(String linCredCuota) {
        this.linCredCuota = linCredCuota;
    }

    public String getPersTTI() {
        return persTTI;
    }

    public void setPersTTI(String persTTI) {
        this.persTTI = persTTI;
    }

    public String getCodTTICorriente() {
        return codTTICorriente;
    }

    public void setCodTTICorriente(String codTTICorriente) {
        this.codTTICorriente = codTTICorriente;
    }

    public String getTICorriente() {
        return tICorriente;
    }

    public void setTICorriente(String tICorriente) {
        this.tICorriente = tICorriente;
    }

    public String getCodTTIMora() {
        return codTTIMora;
    }

    public void setCodTTIMora(String codTTIMora) {
        this.codTTIMora = codTTIMora;
    }

    public String getTIMora() {
        return tIMora;
    }

    public void setTIMora(String tIMora) {
        this.tIMora = tIMora;
    }

    public String getCodTTIComp() {
        return codTTIComp;
    }

    public void setCodTTIComp(String codTTIComp) {
        this.codTTIComp = codTTIComp;
    }

    public String gettIcomp() {
        return tIcomp;
    }

    public void settIcomp(String tIcomp) {
        this.tIcomp = tIcomp;
    }

    public String getPersPagMin() {
        return persPagMin;
    }

    public void setPersPagMin(String persPagMin) {
        this.persPagMin = persPagMin;
    }

    public String getPorcPagMin() {
        return porcPagMin;
    }

    public void setPorcPagMin(String porcPagMin) {
        this.porcPagMin = porcPagMin;
    }

    public String getImpoMinPagMin() {
        return impoMinPagMin;
    }

    public void setImpoMinPagMin(String impoMinPagMin) {
        this.impoMinPagMin = impoMinPagMin;
    }
    
    public String getImpoFijoPagMin() {
        return impoFijoPagMin;
    }

    public void setImpoFijoPagMin(String impoFijoPagMin) {
        this.impoFijoPagMin = impoFijoPagMin;
    }

    public String getModPago() {
        return ModPago;
    }

    public void setModPago(String ModPago) {
        this.ModPago = ModPago;
    }

    public String getTipCtaBanc() {
        return tipCtaBanc;
    }

    public void setTipCtaBanc(String tipCtaBanc) {
        this.tipCtaBanc = tipCtaBanc;
    }

    public String getCtaBanc() {
        return ctaBanc;
    }

    public void setCtaBanc(String ctaBanc) {
        this.ctaBanc = ctaBanc;
    }

    public String getTipPago() {
        return tipPago;
    }

    public void setTipPago(String tipPago) {
        this.tipPago = tipPago;
    }

    public String getValCGF() {
        return ValCGF;
    }

    public void setValCGF(String ValCGF) {
        this.ValCGF = ValCGF;
    }

    public String getTipTasaFin() {
        return TipTasaFin;
    }

    public void setTipTasaFin(String TipTasaFin) {
        this.TipTasaFin = TipTasaFin;
    }

    public String getFactMultVIP() {
        return FactMultVIP;
    }

    public void setFactMultVIP(String FactMultVIP) {
        this.FactMultVIP = FactMultVIP;
    }

    public String getCobrandID() {
        return CobrandID;
    }

    public void setCobrandID(String CobrandID) {
        this.CobrandID = CobrandID;
    }

    public String getNroDocCod() {
        return nroDocCod;
    }

    public void setNroDocCod(String nroDocCod) {
        this.nroDocCod = nroDocCod;
    }

    public String getTipPerCod() {
        return tipPerCod;
    }

    public void setTipPerCod(String tipPerCod) {
        this.tipPerCod = tipPerCod;
    }

    public String getTipDocCod() {
        return tipDocCod;
    }

    public void setTipDocCod(String tipDocCod) {
        this.tipDocCod = tipDocCod;
    }

    public String getNombre1Cod() {
        return nombre1Cod;
    }

    public void setNombre1Cod(String nombre1Cod) {
        this.nombre1Cod = nombre1Cod;
    }

    public String getNombre2Cod() {
        return nombre2Cod;
    }

    public void setNombre2Cod(String nombre2Cod) {
        this.nombre2Cod = nombre2Cod;
    }

    public String getApellido1Cod() {
        return apellido1Cod;
    }

    public void setApellido1Cod(String apellido1Cod) {
        this.apellido1Cod = apellido1Cod;
    }

    public String getApellido2Cod() {
        return apellido2Cod;
    }

    public void setApellido2Cod(String apellido2Cod) {
        this.apellido2Cod = apellido2Cod;
    }

    public String getSexoCod() {
        return sexoCod;
    }

    public void setSexoCod(String sexoCod) {
        this.sexoCod = sexoCod;
    }

    public String getFechNacCod() {
        return fechNacCod;
    }

    public void setFechNacCod(String fechNacCod) {
        this.fechNacCod = fechNacCod;
    }

    public String getLugNacCod() {
        return lugNacCod;
    }

    public void setLugNacCod(String lugNacCod) {
        this.lugNacCod = lugNacCod;
    }

    public String getEstCivCod() {
        return estCivCod;
    }

    public void setEstCivCod(String estCivCod) {
        this.estCivCod = estCivCod;
    }

    public String getOcupacionCod() {
        return ocupacionCod;
    }

    public void setOcupacionCod(String ocupacionCod) {
        this.ocupacionCod = ocupacionCod;
    }

    public String getDirEmailCod() {
        return dirEmailCod;
    }

    public void setDirEmailCod(String dirEmailCod) {
        this.dirEmailCod = dirEmailCod;
    }

    public String getNroDocOfi() {
        return nroDocOfi;
    }

    public void setNroDocOfi(String nroDocOfi) {
        this.nroDocOfi = nroDocOfi;
    }

    public String getTipPerOfi() {
        return tipPerOfi;
    }

    public void setTipPerOfi(String tipPerOfi) {
        this.tipPerOfi = tipPerOfi;
    }

    public String getTipDocOfi() {
        return tipDocOfi;
    }

    public void setTipDocOfi(String tipDocOfi) {
        this.tipDocOfi = tipDocOfi;
    }

    public String getNombre1Ofi() {
        return nombre1Ofi;
    }

    public void setNombre1Ofi(String nombre1Ofi) {
        this.nombre1Ofi = nombre1Ofi;
    }

    public String getNombre2Ofi() {
        return nombre2Ofi;
    }

    public void setNombre2Ofi(String nombre2Ofi) {
        this.nombre2Ofi = nombre2Ofi;
    }

    public String getApellido1Ofi() {
        return apellido1Ofi;
    }

    public void setApellido1Ofi(String apellido1Ofi) {
        this.apellido1Ofi = apellido1Ofi;
    }

    public String getApellido2Ofi() {
        return apellido2Ofi;
    }

    public void setApellido2Ofi(String apellido2Ofi) {
        this.apellido2Ofi = apellido2Ofi;
    }

    public String getSexoOfi() {
        return sexoOfi;
    }

    public void setSexoOfi(String sexoOfi) {
        this.sexoOfi = sexoOfi;
    }

    public String getFechNacOfi() {
        return fechNacOfi;
    }

    public void setFechNacOfi(String fechNacOfi) {
        this.fechNacOfi = fechNacOfi;
    }

    public String getLugNacOfi() {
        return lugNacOfi;
    }

    public void setLugNacOfi(String lugNacOfi) {
        this.lugNacOfi = lugNacOfi;
    }

    public String getEstCivOfi() {
        return estCivOfi;
    }

    public void setEstCivOfi(String estCivOfi) {
        this.estCivOfi = estCivOfi;
    }

    public String getOcupacionOfi() {
        return ocupacionOfi;
    }

    public void setOcupacionOfi(String ocupacionOfi) {
        this.ocupacionOfi = ocupacionOfi;
    }

    public String getDirEmailofi() {
        return dirEmailofi;
    }

    public void setDirEmailofi(String dirEmailofi) {
        this.dirEmailofi = dirEmailofi;
    }

    public String getCostNoAplica() {
        return costNoAplica;
    }

    public void setCostNoAplica(String costNoAplica) {
        this.costNoAplica = costNoAplica;
    }

    public String getAplicaImp() {
        return aplicaImp;
    }

    public void setAplicaImp(String aplicaImp) {
        this.aplicaImp = aplicaImp;
    }

    public String getUserActualiza() {
        return userActualiza;
    }

    public void setUserActualiza(String userActualiza) {
        this.userActualiza = userActualiza;
    }

    public String getMotNovCta() {
        return motNovCta;
    }

    public void setMotNovCta(String motNovCta) {
        this.motNovCta = motNovCta;
    }

    public String getPartProCupAltera() {
        return partProCupAltera;
    }

    public void setPartProCupAltera(String partProCupAltera) {
        this.partProCupAltera = partProCupAltera;
    }

    public String getCargNoAplica() {
        return cargNoAplica;
    }

    public void setCargNoAplica(String cargNoAplica) {
        this.cargNoAplica = cargNoAplica;
    }
    
}
