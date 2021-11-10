/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.com.bepsa.ws;

import java.util.Calendar;
import java.util.Date;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import org.apache.log4j.Logger;
import py.com.bepsa.managers.RegrabacionManager;
import py.com.bepsa.pojo.DatosEntrada;
import py.com.bepsa.pojo.Regrabacion;
import py.com.bepsa.utils.DBUtils;
import static py.com.bepsa.utils.DBUtils.validarCorte;
import py.com.bepsa.utils.Utils;

/**
 *
 * @author gmigliore
 */
@WebService(serviceName = "tcservice_apvp737")
public class Tcservice_apvp737 {

    /**
     * This is a sample web service operation
     */
    private static final Logger LOGGER = Logger.getLogger(Tcservice_apvp737.class);

    @WebMethod(operationName = "transferenciaRegrabacion")
    public String transferenciaRegrabacion(@WebParam(name = "usuario") String usuario,
            @WebParam(name = "contrasena") String contrasena,
            @WebParam(name = "opcion") String opcion,
            @WebParam(name = "transReg") DatosEntrada transReg) {

//        PropertyConfigurator.configure("LOGGERRegraba.properties");
        LOGGER.info("-------------------- INICIA REGRABACION:" + new Date() + "--------------------");
        String msg = "";
        while (msg.equalsIgnoreCase("")) {
            if (usuario != null) {
                if (usuario.length() > 10) {
                    msg = "El usuario no puede tener una longitud mayor a 10";
//                    LOGGER.info(msg);
                    break;
                }
            } else {
                msg = "El usuario no puede ser nulo";
//                LOGGER.info(msg);
                break;
            }
            if (contrasena != null) {
                if (contrasena.length() > 15) {
                    msg = "La contraseña no puede tener una longitud mayor a 15";
//                    LOGGER.info(msg);
                    break;
                }
            } else {
                msg = "La contraseña no puede ser nula";
//                LOGGER.info(msg);
                break;
            }
            if (!Utils.validateLogin(usuario, contrasena)) {
                msg = "Usuario y/o password incorrectos";
//                LOGGER.info(msg);
                break;
            }
            if (opcion != null) {
                if (!opcion.equals("P")) {
                    msg = "La opción debe ser P";
//                    LOGGER.info(msg);
                    break;
                }
            } else {
                msg = "La opción es un valor requerido";
//                LOGGER.info(msg);
                break;
            }
            if (transReg != null) {
                if (transReg.getTransReg().size() == 0) {
                    msg = "Archivo de Regrabación vacío";
//                    LOGGER.info(msg);
                    break;
                }
            } else {
                msg = "Archivo de Regrabación vacío";
//                LOGGER.info(msg);
                break;
            }
            Date fechaProceso = new Date();
            if (fechaProceso.getDay() == 0 || fechaProceso.getDay() == 6) {
                msg = "Hoy no es un día hábil";
//                LOGGER.info(msg);
                break;
            } else {
                if (DBUtils.valiDate(Utils.formatearFecha(fechaProceso, "yyyyMMdd"))) {
                    msg = "Hoy no es un día hábil";
//                    LOGGER.info(msg);
                    break;
                }
            }
//            if (DBUtils.validatePendingTransfers()) {
//                msg = "Transferencias pendientes";
//                break;
//            }
            String tarjetas = "(";
            for (Regrabacion rg : transReg.getTransReg()) {
                tarjetas += "'" + rg.getNroTarjeta().trim() + "',";
            }
            tarjetas = tarjetas.substring(0, tarjetas.length() - 1) + ")";
            if (DBUtils.validatePendingCut(tarjetas)) {
                msg = "Corte procesadora sin terminar";
//                LOGGER.info(msg);
                break;
            }
            if (validarCorte()) {
                msg = "Corte procesadora sin terminar";
//                LOGGER.info(msg);
                break;
            }
            Calendar cal = Calendar.getInstance();
            int hora = cal.get(Calendar.HOUR_OF_DAY);
            int minuto = cal.get(Calendar.MINUTE);
            if (Utils.driver == null) {
                Utils.obtenerPropiedades();
            }
            if (!(((hora >= Utils.periodo1HoraInicial && minuto >= Utils.periodo1MinutoInicial) && (hora <= Utils.periodo1HoraFinal && minuto <= Utils.periodo1MinutoFinal)) || ((hora >= Utils.periodo2HoraInicial && minuto >= Utils.periodo2MinutoInicial) && (hora <= Utils.periodo2HoraFinal && minuto <= Utils.periodo2MinutoFinal)))) {
                msg = "Horario no válido p/Transf.";
//                LOGGER.info(msg);
                break;
            }
            if (msg.equalsIgnoreCase("")) {
                msg = "OK";
                break;
            }

        }

        //</editor-fold>
        if (msg.equalsIgnoreCase("OK")) {//SE PROCESA SI PASA LAS VALIDACIONES PREVIAS
            msg = RegrabacionManager.procesar(transReg.getTransReg());
        }
        LOGGER.info("-------------------- FINALIZA REGRABACION --------------------");
        LOGGER.info(msg);
        return msg;
    }
}
