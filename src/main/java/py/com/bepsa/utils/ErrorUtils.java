/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package py.com.bepsa.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import py.com.bepsa.clases.Cuenta;
import py.com.bepsa.clases.Tarjeta;
import org.apache.log4j.Logger;

/**
 *
 * @author rarce
 */
public class ErrorUtils {
    private static final Logger LOGGER = Logger.getLogger(ErrorUtils.class);
    
    public static ErrorCuenta validarDatosCta (Cuenta cta) {
        ErrorCuenta ctaVerificada = new ErrorCuenta();
        String erroresCta = "";
        //Inicia validacion de los datos de la cuenta
        if (!isVacio(cta.getNroDoc())) { //alta y modificacion
            if (cta.getNroDoc().length() > 15) {
                erroresCta += "2;";
                ctaVerificada.setBandera(true);
            }
            if (isVacio(cta.getTipoDoc())) { //alta y modificacion
                erroresCta += "3;";
                ctaVerificada.setBandera(true);
            } else if (!validarTipoDoc(cta.getTipoDoc())) {
                erroresCta += "4;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNombre1())) { //alta y modificacion
            if (cta.getNombre1().length() > 15) {
                erroresCta += "6;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNombre2())) { //alta y modificacion
            if (cta.getNombre2().length() > 15) {
                erroresCta += "8;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getApellido1())) { //alta y modificacion
            if (cta.getApellido1().length() > 15) {
                erroresCta += "10;";
                ctaVerificada.setBandera(true);
            }
        }

        if (!isVacio(cta.getApellido2())) { //alta y modificacion
            if (cta.getApellido2().length() > 15) {
                erroresCta += "12;";
                ctaVerificada.setBandera(true);
            }
        }

        if (!isVacio(cta.getPaisDoc().trim())) { //alta y modificacion
            if (!isNumeric(cta.getPaisDoc())) {
                erroresCta += "14;";
                ctaVerificada.setBandera(true);
            } else if (cta.getPaisDoc().length() > 3) {
                erroresCta += "15;";
                ctaVerificada.setBandera(true);
            }
        }

        if (!isVacio(cta.getSexo())) { //alta y modificacion
            if (!cta.getSexo().equals("M") && !cta.getSexo().equals("F")) {
                erroresCta += "17;";
                ctaVerificada.setBandera(true);
            }
        }

        if (!isVacio(cta.getFechaNac())) {  //alta y modificacion
            if (!validarFecha(cta.getFechaNac())) {
                erroresCta += "19;";
                ctaVerificada.setBandera(true);
            }
        }
        //Ruc
//        if (cta.getNroDoc().isEmpty() || cta.getNroDoc() == null) {
//            erroresCta = "#;";
//            ctaVerificada.setBandera(true);
//        } else if (cta.getNroDoc().length() > 15) {
//            erroresCta = "#;";
//            ctaVerificada.setBandera(true);
//        }
        
        if (!isVacio(cta.getEstadoCivil())) {  //alta y modificacion
            if (!cta.getEstadoCivil().equals("C") && !cta.getEstadoCivil().equals("S") && !cta.getEstadoCivil().equals("D") && !cta.getEstadoCivil().equals("V")) {
                erroresCta += "22;";
                ctaVerificada.setBandera(true);
            }
        }

        if (!isVacio(cta.getOcupacion().trim())) { //alta modificacion
            //LOGGER.info(cta.getOcupacion());
            if (!isNumeric(cta.getOcupacion())) {
                erroresCta += "24;";
                ctaVerificada.setBandera(true);
            } else if (cta.getOcupacion().length() > 4) {
                erroresCta += "25;";
                ctaVerificada.setBandera(true);
            }
        } 

        if (!isVacio(cta.getNroDocNew())) { //modificacion
            if (cta.getNroDocNew().length() > 15) {
                erroresCta += "28;";
                ctaVerificada.setBandera(true);
            }
            if (isVacio(cta.getTipDocNew())) { //modificacion
                erroresCta += "29;";
                ctaVerificada.setBandera(true);
            } else if (!validarTipoDoc(cta.getTipDocNew())) {
                erroresCta += "30;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNroCuenta().trim())) { //alta modificacion
            if (!isNumeric(cta.getNroCuenta())) {
                erroresCta += "33;";
                ctaVerificada.setBandera(true);
            } else if (cta.getNroCuenta().length() > 12) {
                erroresCta += "34;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getCodAfin().trim())) { //alta modificacion
            if (!isNumeric(cta.getCodAfin())) {
                erroresCta += "36;";
                ctaVerificada.setBandera(true);
            } else if (cta.getCodAfin().length() > 3) {
                erroresCta += "37;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getDirEmailCta())) { //alta modificacion
            if (cta.getDirEmailCta().length() > 50) {
                erroresCta += "38;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getDirExtr1())) { //alta modificacion
            if (cta.getDirExtr1().length() > 50) {
                erroresCta += "39;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getDirExtr2())) { //alta modificacion
            if (cta.getDirExtr2().length() > 50) {
                erroresCta += "40;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getDirExtr3())) { //alta modificacion
            if (cta.getDirExtr3().length() > 50) {
                erroresCta = "41;";
                ctaVerificada.setBandera(true);
            }
        }
        
//        if (tipoTrx.equals("Alta")) {
//            if (isVacio(cta.getTipCuenta())) {
//                erroresCta = "#;";
//                ctaVerificada.setBandera(true);
//            } else if (!cta.getTipCuenta().equals("P") || !cta.getTipCuenta().equals("E")) {
//                erroresCta = "#;";
//                ctaVerificada.setBandera(true);
//            }

            if (!isVacio(cta.getDepart().trim())) { //alta modificacion
                if (!isNumeric(cta.getDepart())) {
                    erroresCta += "43;";
                    ctaVerificada.setBandera(true);
                } else if (cta.getDepart().length() > 3) {
                    erroresCta += "44;";
                    ctaVerificada.setBandera(true);
                }
            }
//        }
  
        if (!isVacio(cta.getCiudad().trim())) {    //alta modificacion
            if (!isNumeric(cta.getCiudad())) {
                erroresCta += "45;";
                ctaVerificada.setBandera(true);
            } else if (cta.getCiudad().length() > 6) {
                erroresCta += "46;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getZona())) {      //alta modificacion
            if (cta.getZona().length() > 10) {
                erroresCta += "48;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNroTel())) {    //alta modificacion
            if (cta.getNroTel().length() > 15) {
                erroresCta += "49;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNroCel())) {    //alta modificacion
            if (cta.getNroCel().length() > 15) {
                erroresCta += "50;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getDirEmailCta())) {   //alta modificacion
            if (cta.getDirEmailCta().length() > 50) {
                erroresCta += "51;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getCodSuc().trim())) { //para todos
            if (!isNumeric(cta.getCodSuc())) {
                erroresCta += "53;";
                ctaVerificada.setBandera(true);
            } else if (cta.getCodSuc().length() > 3) {
                erroresCta += "54;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNroDocOfCta())) {   //alta
            if (cta.getNroDocOfCta().length() > 15) {
                erroresCta += "56;";
                ctaVerificada.setBandera(true);
            }
            if (isVacio(cta.getTipDocOfCta())) { //alta
                erroresCta += "57;";
                ctaVerificada.setBandera(true);
            } else if (!validarTipoDoc(cta.getTipDocOfCta())) {
                erroresCta += "57;";
                ctaVerificada.setBandera(true);
            }
        }
       
        if (!isVacio(cta.getCobCosto())) { //alta
            if (!cta.getCobCosto().equals("S") && !cta.getCobCosto().equals("N")) {
                erroresCta += "59;";
                ctaVerificada.setBandera(true);
            }
        }

        if (!isVacio(cta.getRetenExtr())) { //alta
            if (!cta.getRetenExtr().toUpperCase().equals("S") && !cta.getRetenExtr().toUpperCase().equals("N")) {
                erroresCta += "61;";
                ctaVerificada.setBandera(true);
            }
        }

        if (!isVacio(cta.getCalifBCP())) { //alta
            if (cta.getCalifBCP().length() > 2) {
                erroresCta += "63;";
                ctaVerificada.setBandera(true);
            }
        }

        if (!isVacio(cta.getSituacion())) {
            if (!cta.getSituacion().toUpperCase().equals("A") && !cta.getSituacion().toUpperCase().equals("I") && !cta.getSituacion().toUpperCase().equals("B")) {
                erroresCta += "65;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getTipLin1Norm())) {
            if (!cta.getTipLin1Norm().equals("1")) {
                erroresCta += "66;";
                ctaVerificada.setBandera(true);
            } else {
                if (isVacio(cta.getLinCredNorm().trim())) {
                    erroresCta += "67;";
                    ctaVerificada.setBandera(true);
                } else if (!isNumeric(cta.getLinCredNorm())) {
                    erroresCta += "68;";
                    ctaVerificada.setBandera(true);
                } else if (cta.getLinCredNorm().length() > 13) {
                    erroresCta += "69;";
                    ctaVerificada.setBandera(true);
                }
            }
        }

        if (!isVacio(cta.getTipLin2Cuota())) {
            if (!cta.getTipLin2Cuota().equals("2")) {
                erroresCta += "70;";
                ctaVerificada.setBandera(true);
            } else {
                if (isVacio(cta.getLinCredCuota().trim())) {
                    erroresCta += "71;";
                    ctaVerificada.setBandera(true);
                } else if (!isNumeric(cta.getLinCredCuota())) {
                    erroresCta += "72;";
                    ctaVerificada.setBandera(true);
                } else if (cta.getLinCredCuota().length() > 13) {
                    erroresCta += "73;";
                    ctaVerificada.setBandera(true);
                }

            }
        }

        if (!isVacio(cta.getPersTTI())) { //alta
            if (!cta.getPersTTI().toUpperCase().equals("S") && !cta.getPersTTI().toUpperCase().equals("N")) {
                erroresCta += "75;";
                ctaVerificada.setBandera(true);
            } else if (cta.getPersTTI().toUpperCase().equals("S")) {
                if (cta.getCodTTICorriente().equals("1")) {
                    if (isVacio(cta.getTICorriente().trim())) {
                        erroresCta += "78;";
                        ctaVerificada.setBandera(true);
                    } else if (!isNumeric(cta.getTICorriente())) {
                        erroresCta += "79;";
                        ctaVerificada.setBandera(true);
                    }
                } else {
                    erroresCta += "77;";
                    ctaVerificada.setBandera(true);
                }
                
                if (cta.getCodTTIMora().equals("2")) {
                    if (isVacio(cta.getTIMora().trim())) {
                        erroresCta += "82;";
                        ctaVerificada.setBandera(true);
                    } else if (!isNumeric(cta.getTIMora())) {
                        erroresCta += "83;";
                        ctaVerificada.setBandera(true);
                    }
                } else {
                    erroresCta += "81;";
                    ctaVerificada.setBandera(true);
                }
            }
        }

        if (!isVacio(cta.getPersPagMin())) { //alta
            if (!cta.getPersPagMin().toUpperCase().equals("S") && !cta.getPersPagMin().toUpperCase().equals("N")) {
                erroresCta += "89;";
                ctaVerificada.setBandera(true);
            } else if (cta.getPersPagMin().toUpperCase().equals("S")) {
                if (isVacio(cta.getPorcPagMin().trim())) {
                    erroresCta += "90;";
                    ctaVerificada.setBandera(true);
                } else if (!isNumeric(cta.getPorcPagMin())) {
                    erroresCta += "91;";
                    ctaVerificada.setBandera(true);
                }
                if (isVacio(cta.getImpoFijoPagMin().trim())) {
                    erroresCta += "92;";
                    ctaVerificada.setBandera(true);
                } else if (!isNumeric(cta.getImpoFijoPagMin())) {
                    erroresCta += "93;";
                    ctaVerificada.setBandera(true);
                } else if (cta.getImpoFijoPagMin().length() > 13) {
                    erroresCta += "94;";
                    ctaVerificada.setBandera(true);
                }
            }
        }
       
        if (!isVacio(cta.getModPago())) {    //alta
            if (!cta.getModPago().toUpperCase().equals("V") && !cta.getModPago().toUpperCase().equals("D")) {
                erroresCta += "96;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getTipCtaBanc())) {
            if (!cta.getTipCtaBanc().equals("1") && !cta.getTipCtaBanc().equals("2")) {
                erroresCta += "98;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getCtaBanc())) {
            if (cta.getCtaBanc().length() > 15) {
                erroresCta += "100;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getTipPago())) {
            if (!cta.getTipPago().toUpperCase().equals("M") && !cta.getTipPago().toUpperCase().equals("T")) {
                erroresCta += "102;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNroDocCod())) {
            if (cta.getNroDocCod().length() > 15) {
                erroresCta += "104;";
                ctaVerificada.setBandera(true);
            }
            if (isVacio(cta.getTipDocCod())) {
                erroresCta += "105;";
                ctaVerificada.setBandera(true);
            } else if (!validarTipoDoc(cta.getTipDocCod())) {
                erroresCta += "106;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNombre1Cod())) { //alta y modificacion
            if (cta.getNombre1Cod().length() > 15) {
                erroresCta += "108;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNombre2Cod())) { //alta y modificacion
            if (cta.getNombre2Cod().length() > 15) {
                erroresCta += "110;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getApellido1Cod())) { //alta y modificacion
            if (cta.getApellido1Cod().length() > 15) {
                erroresCta += "112;";
                ctaVerificada.setBandera(true);
            }
        }

        if (!isVacio(cta.getApellido2Cod())) { //alta y modificacion
            if (cta.getApellido2Cod().length() > 15) {
                erroresCta += "114;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getSexoCod())) {
            if (!cta.getSexoCod().equals("M") && !cta.getSexoCod().equals("F")) {
                erroresCta += "116;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getFechNacCod())) {
            if (!validarFecha(cta.getFechNacCod())) {
                erroresCta += "118;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getEstCivCod())) {
            if (!cta.getEstCivCod().equals("C") && !cta.getEstCivCod().equals("S") && !cta.getEstCivCod().equals("D") && !cta.getEstCivCod().equals("V")) {
                erroresCta += "120;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getOcupacionCod().trim())) {
            if (!isNumeric(cta.getOcupacionCod())) {
                erroresCta += "122;";
                ctaVerificada.setBandera(true);
            } else if (cta.getOcupacionCod().length() > 4) {
                erroresCta += "123;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getDirEmailCod())) { //alta modificacion
            if (cta.getDirEmailCod().length() > 50) {
                erroresCta += "124;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNombre1Ofi())) { //alta y modificacion
            if (cta.getNombre1Ofi().length() > 15) {
                erroresCta += "126;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getNombre2Ofi())) { //alta y modificacion
            if (cta.getNombre2Ofi().length() > 15) {
                erroresCta += "128;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getApellido1Ofi())) { //alta y modificacion
            if (cta.getApellido1Ofi().length() > 15) {
                erroresCta += "130;";
                ctaVerificada.setBandera(true);
            }
        }

        if (!isVacio(cta.getApellido2Ofi())) { //alta y modificacion
            if (cta.getApellido2Ofi().length() > 15) {
                erroresCta += "132;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getCostNoAplica())) { //alta y modificacion
            if (cta.getCostNoAplica().length() > 30) {
                erroresCta += "135;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getUserActualiza())) {
            if (cta.getUserActualiza().length() > 10) {
                erroresCta += "136;";
                ctaVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(cta.getCargNoAplica())) { //alta y modificacion
            if (cta.getCargNoAplica().length() > 30) {
                erroresCta += "139;";
                ctaVerificada.setBandera(true);
            }
        }
        
//        if (!erroresCta.trim().equals("")) {
//            erroresCta = erroresCta.substring(0, erroresCta.length() -1);
        ctaVerificada.setErrores(erroresCta);
//        }
        return ctaVerificada;
    }
    
    public static ErrorTarjeta validarDatosTarj (Tarjeta tarj) {
        ErrorTarjeta tarjVerificada = new ErrorTarjeta();
        String erroresTarj = "";
        //Inicia validacion de los datos de la tarjeta
//        if (!isVacio(tarj.getNroDoc())) {
//            if (tarj.getNroDoc().length() > 15) {
//                erroresTarj += "141;";
//                tarjVerificada.setBandera(true);
//            }
//        }
        if (!isVacio(tarj.getNroControl().trim())) {
            if (!isNumeric(tarj.getNroControl())) {
                erroresTarj += "141;";
                tarjVerificada.setBandera(true);
            } else if (tarj.getNroControl().length() > 4) {
                erroresTarj += "142;";
                tarjVerificada.setBandera(true);
            }
        }
    
        if (!isVacio(tarj.getIdAfin().trim())) {
            if (!isNumeric(tarj.getIdAfin())) {
                erroresTarj += "36;";
                tarjVerificada.setBandera(true);
            } else if (tarj.getIdAfin().length() > 3) {
                erroresTarj += "37;";
                tarjVerificada.setBandera(true);
            }
        }

        if (!isVacio(tarj.getIdCuenta().trim())) {
            if (!isNumeric(tarj.getIdCuenta())) {
                erroresTarj += "33;";
                tarjVerificada.setBandera(true);
            } else if (tarj.getIdCuenta().length() > 12) {
                erroresTarj += "34;";
                tarjVerificada.setBandera(true);
            }
        }

        if (!isVacio(tarj.getNroTarjeta())) {
            if (tarj.getNroTarjeta().length() > 16) {
                erroresTarj += "143;";
                tarjVerificada.setBandera(true);
            }
        }

        if (!isVacio(tarj.getTipTarjeta())) {
            if (!tarj.getTipTarjeta().toUpperCase().equals("P") && !tarj.getTipTarjeta().toUpperCase().equals("A")) {
                erroresTarj += "145;";
                tarjVerificada.setBandera(true);
            }
        }

        if (!isVacio(tarj.getDuracion().trim())) {
            if (!isNumeric(tarj.getDuracion())) {
                erroresTarj += "147;";
                tarjVerificada.setBandera(true);
            } else if (tarj.getDuracion().length() > 2) {
                erroresTarj += "148;";
                tarjVerificada.setBandera(true);
            }
        }

        if (!isVacio(tarj.getNombPlastico())) {
            if (tarj.getNombPlastico().length() > 20) {
                erroresTarj += "150;";
                tarjVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(tarj.getNroDocPromot())) {
            if (tarj.getNroDocProm().length() > 15) {
                erroresTarj += "153;";
                tarjVerificada.setBandera(true);
            }
            if (isVacio(tarj.getTipDocPromot())) {
                erroresTarj += "154;";
                tarjVerificada.setBandera(true);
            } else if (!validarTipoDoc(tarj.getTipDocPromot())) {
                erroresTarj += "155;";
                tarjVerificada.setBandera(true);
            }
        }

        if (!isVacio(tarj.getSituacion())) {
            if (!tarj.getSituacion().toUpperCase().equals("A") && !tarj.getSituacion().toUpperCase().equals("I") && !tarj.getSituacion().toUpperCase().equals("B")) {
                erroresTarj += "157;";
                tarjVerificada.setBandera(true);
            }
        }
//        
//        if (tarj.getSituacion().isEmpty() || tarj.getSituacion() == null) {
//            erroresCta = "#;";
//            ctaVerificada.setBandera(true);
//        } else if (!tarj.getSituacion().toUpperCase().equals("A") || !tarj.getSituacion().toUpperCase().equals("I") || !tarj.getSituacion().toUpperCase().equals("B")) {
//            erroresCta = "#;";
//            ctaVerificada.setBandera(true);
//        }
        
        if (!isVacio(tarj.getRenovAuto())) {
            if (!tarj.getRenovAuto().toUpperCase().equals("S") && !tarj.getRenovAuto().toUpperCase().equals("N")) {
                erroresTarj += "158;";
                tarjVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(tarj.getEmboza())) {
            if (!tarj.getEmboza().toUpperCase().equals("S") && !tarj.getEmboza().toUpperCase().equals("N")) {
                erroresTarj += "161;";
                tarjVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(tarj.getNroDocPromot())) {
            if (tarj.getNroDocProm().length() > 15) {
                erroresTarj += "153;";
                tarjVerificada.setBandera(true);
            }
            if (isVacio(tarj.getTipDocPromot())) {
                erroresTarj += "154;";
                tarjVerificada.setBandera(true);
            } else if (!validarTipoDoc(tarj.getTipDocPromot())) {
                erroresTarj += "155;";
                tarjVerificada.setBandera(true);
            }
        }
        
        if (!isVacio(tarj.getUserActualiza())) {
            if (tarj.getUserActualiza().length() > 10) {
                erroresTarj += "136;";
                tarjVerificada.setBandera(true);
            }
        }
        
//        if (!erroresTarj.trim().equals("")) {
//            erroresTarj = erroresTarj.substring(0, erroresTarj.length() -1);
        tarjVerificada.setErrores(erroresTarj);
//        }
        return tarjVerificada;
    }
    
    public static boolean isVacio(String cadena) {
        boolean resultado;
        if (cadena.trim().isEmpty() || cadena == null) {
            resultado = true;
        } else {
            resultado = false;
        }
        return resultado;
    } 
       
    public static boolean isNumeric(String cadena) {
        boolean resultado;
        try {
            Long.parseLong(cadena);
            resultado = true;
            //LOGGER.info(resultado);
        } catch (NumberFormatException excepcion) {
            LOGGER.error("Error: " + excepcion);
            resultado = false;
            //LOGGER.info(resultado);
        }

        return resultado;
    }   
    
    public static boolean validarFecha(String fecha) {
        try {
            SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yy");
            formatoFecha.setLenient(false);
            formatoFecha.parse(fecha);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }
     
    public static boolean validarTipoDoc(String cadena) {
        boolean resultado;
        if (cadena.equalsIgnoreCase("CAD") || cadena.equalsIgnoreCase("CIP") || cadena.equalsIgnoreCase("PAS") || cadena.equalsIgnoreCase("CIE") || cadena.equalsIgnoreCase("RUC")) {
            resultado = true;
        } else {
            resultado = false;
        }
        return resultado;
    } 
    
    public static String armaDatosCuenta (Cuenta cta) {
        String datosCuenta = cta.getNroDoc().trim() + ";" 
                + cta.getTipoPers().trim() + ";" + cta.getTipoDoc().trim() + ";" 
                + cta.getNombre1().trim() + ";" + cta.getNombre2().trim() + ";" 
                + cta.getApellido1().trim() + ";" + cta.getApellido2().trim() + ";" 
                + cta.getRazonSoc().trim() + ";" + cta.getDenomComer().trim() + ";" 
                + cta.getPaisDoc().trim() + ";" + cta.getSexo().trim() + ";" 
                + cta.getFechaNac().trim() + ";" + cta.getLugarNac().trim() + ";" 
                + cta.getRucExtr().trim() + ";"+ cta.getEstadoCivil().trim() + ";" 
                + cta.getOcupacion().trim() + ";" + cta.getDirEmailPer().trim() + ";" 
                + cta.getTipDocNew().trim() + ";" + cta.getNroDocNew().trim() + ";"
                + cta.getNroControl().trim() + ";" + cta.getNroCuenta().trim() + ";" 
                + cta.getCodAfin().trim() + ";" + cta.getTipCuenta().trim() + ";" 
                + cta.getDirRecibo().trim() + ";" + cta.getDirExtr1().trim() + ";" 
                + cta.getDirExtr2().trim() + ";" + cta.getDirExtr3().trim() + ";" 
                + cta.getDepart().trim() + ";" + cta.getCiudad().trim() + ";" 
                + cta.getZona().trim() + ";" + cta.getNroTel().trim() + ";" 
                + cta.getNroCel().trim() + ";" + cta.getDirEmailCta().trim() + ";" 
                + cta.getCodSuc().trim() + ";" + cta.getNroDocOfCta().trim() + ";" 
                + cta.getTipDocOfCta().trim() + ";" + cta.getCtaVip().trim() + ";" 
                + cta.getFranqueo().trim() + ";" + cta.getAplSegVida().trim()+ ";" 
                + cta.getCobCosto().trim() + ";" + cta.getTipCosto().trim() + ";" 
                + cta.getRetenExtr().trim() + ";" + cta.getMotReten().trim() + ";" 
                + cta.getTipCierre().trim() + ";" + cta.getEmpAdher().trim() + ";" 
                + cta.getCalifBCP().trim() + ";" + cta.getSituacion().trim() + ";" 
                + cta.getCodCliEnt().trim() + ";" + cta.getNroSocio().trim() + ";" 
                + cta.getBonosCobrand().trim() + ";" + cta.getTipLin1Norm().trim() + ";" 
                + cta.getLinCredNorm().trim() + ";" + cta.getTipLin2Cuota().trim() + ";" 
                + cta.getLinCredCuota().trim() + ";" + cta.getPersTTI().trim() + ";" 
                + cta.getCodTTICorriente().trim() + ";" + cta.getTICorriente().trim() + ";" 
                + cta.getCodTTIMora().trim() + ";" + cta.getTIMora().trim() + ";" 
                + cta.getCodTTIComp().trim() + ";" + cta.gettIcomp().trim() + ";" 
                + cta.getPersPagMin().trim() + ";" + cta.getPorcPagMin().trim() + ";" 
                + cta.getImpoMinPagMin().trim() + ";" + cta.getImpoFijoPagMin().trim() + ";" 
                + cta.getModPago().trim() + ";" + cta.getTipCtaBanc().trim() + ";" 
                + cta.getCtaBanc().trim() + ";" + cta.getTipPago().trim() + ";" 
                + cta.getValCGF().trim() + ";" + cta.getTipTasaFin().trim() + ";" 
                + cta.getFactMultVIP().trim() + ";"+ cta.getCobrandID().trim() + ";" 
                + cta.getNroDocCod().trim() + ";" + cta.getTipPerCod().trim() + ";" 
                + cta.getTipDocCod().trim() + ";" + cta.getNombre1Cod().trim() + ";" 
                + cta.getNombre2Cod().trim() + ";" + cta.getApellido1Cod().trim() + ";" 
                + cta.getApellido2Cod().trim() + ";" + cta.getSexoCod().trim() + ";" 
                + cta.getFechNacCod().trim() + ";" + cta.getLugNacCod().trim() + ";" 
                + cta.getEstCivCod().trim() + ";" + cta.getOcupacionCod().trim() + ";" 
                + cta.getDirEmailCod().trim() + ";" + cta.getNroDocOfi().trim() + ";" 
                + cta.getTipPerOfi().trim() + ";" + cta.getTipDocOfi().trim() + ";" 
                + cta.getNombre1Ofi().trim() + ";" + cta.getNombre2Ofi().trim() + ";" 
                + cta.getApellido1Ofi().trim() + ";" + cta.getApellido2Ofi().trim() + ";" 
                + cta.getSexoOfi().trim() + ";" + cta.getFechNacOfi().trim() + ";" 
                + cta.getLugNacOfi().trim() + ";" + cta.getEstCivOfi().trim() + ";" 
                + cta.getOcupacionOfi().trim() + ";" + cta.getDirEmailofi().trim() + ";" 
                + cta.getCostNoAplica().trim() + ";" + cta.getAplicaImp().trim() + ";" 
                + cta.getUserActualiza().trim() + ";" + cta.getMotNovCta().trim() + ";" 
                + cta.getPartProCupAltera().trim() + ";" + cta.getCargNoAplica() + ";";

        return datosCuenta;
    }
       
    public static String armaDatosTarjeta (Tarjeta tarj) {
        String datoTarjeta = tarj.getNroDoc().trim() + ";" 
                + tarj.getTipoPers().trim() + ";" + tarj.getTipoDoc().trim() + ";" 
                + tarj.getNombre1().trim() + ";" + tarj.getNombre2().trim() + ";" 
                + tarj.getApellido1().trim() + ";" + tarj.getApellido2().trim() + ";" 
                + tarj.getPaisNac().trim() + ";" + tarj.getSexo().trim() + ";" 
                + tarj.getFechaNac().trim() + ";" + tarj.getLugarNac().trim() + ";" 
                + tarj.getEstadoCivil().trim() + ";" + tarj.getOcupacion().trim() + ";" 
                + tarj.getDirEmailPer().trim() + ";" + tarj.getTipDocNew().trim() + ";" 
                + tarj.getNroDocNew().trim() + ";" + tarj.getNroControl().trim() + ";" 
                + tarj.getIdAfin().trim() + ";" + tarj.getIdCuenta().trim() + ";" 
                + tarj.getNroTarjeta().trim() + ";" + tarj.getTipTarjeta().trim() + ";" 
                + tarj.getTipPlastico().trim() + ";" + tarj.getDuracion().trim() + ";" 
                + tarj.getNombPlastico().trim() + ";" + tarj.getAdiPersonali().trim() + ";" 
                + tarj.getNroDocPromot().trim() + ";" + tarj.getTipDocPromot().trim() + ";" 
                + tarj.getSituacion().trim() + ";" + tarj.getRenovAuto().trim() + ";" 
                + tarj.getOrdena1().trim() + ";" + tarj.getOrdena2().trim() + ";" 
                + tarj.getEmboza().trim() + ";" + tarj.getNroDocProm().trim() + ";" 
                + tarj.getTipPersProm().trim() + ";" + tarj.getTipDocProm().trim() + ";" 
                + tarj.getNombre1Prom().trim() + ";" + tarj.getNombre2Prom().trim() + ";" 
                + tarj.getApellido1Prom().trim() + ";" + tarj.getApellido2Prom().trim() + ";" 
                + tarj.getSexoProm().trim() + ";" + tarj.getFechNacProm().trim() + ";" 
                + tarj.getLugNacProm().trim() + ";" + tarj.getEstadCivProm().trim() + ";" 
                + tarj.getOcupaProm().trim() + ";" + tarj.getDirEmailProm().trim() + ";" 
                + tarj.getPlanContad().trim() + ";" + tarj.getOperPlanCont().trim() + ";" 
                + tarj.getImporPersonal().trim() + ";" + tarj.getPorcPersonal().trim() + ";" 
                + tarj.getPlanAdeEfec().trim() + ";" + tarj.getOperPlanAdeEfec().trim() + ";" 
                + tarj.getImporPersonaliza().trim() + ";" + tarj.getPorcPersonaliza().trim() + ";" 
                + tarj.getPermFinanciar().trim() + ";" + tarj.getUserActualiza().trim() + ";" 
                + tarj.getNroSocio().trim() + ";" + tarj.getMotBajaTarj() + "";
        
        return datoTarjeta;
   }
}

