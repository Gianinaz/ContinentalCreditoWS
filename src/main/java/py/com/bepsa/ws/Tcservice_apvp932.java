/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.com.bepsa.ws;

import java.util.Date;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import py.com.bepsa.clases.RespActivacion;
import py.com.bepsa.managers.ActivacionManager;
import static py.com.bepsa.utils.DBUtils.existNroBin;
import static py.com.bepsa.utils.DBUtils.existTarjeta;
import py.com.bepsa.utils.ErrorUtils;
import py.com.bepsa.utils.Utils;

/**
 *
 * @author rarce
 */
@WebService(serviceName = "tcservice_apvp932")
public class Tcservice_apvp932 {

    private static final Logger LOGGER = LogManager.getLogger(Tcservice_apvp932.class);

    @WebMethod(operationName = "activacionTC")
    public RespActivacion activacionTC(@WebParam(name = "usuario") String usuario,
            @WebParam(name = "contrasena") String contrasena,
            @WebParam(name = "nroCuenta") String nroCuenta,
            @WebParam(name = "nroTarjeta") String nroTarjeta,
            @WebParam(name = "cobrarCosto") String cobrarCosto,
            @WebParam(name = "importe") String importe) {
//        PropertyConfigurator.configure("LOGGERActivacionesTC.properties");
        //<editor-fold desc="VALIDACIONES INICIALES">
        LOGGER.info("--------------- INICIA ACTIVACION TC:" + new Date() + " ---------------");
//        LOGGER.info("usuario " + usuario);
//        LOGGER.info("contrasena " + contrasena);
//        LOGGER.info("nroCuenta " + nroCuenta);
//        LOGGER.info("nroTarjeta " + nroTarjeta);
//        LOGGER.info("cobrarCosto " + cobrarCosto);
//        LOGGER.info("importe " + importe);
//        printStackTrace();
        String cod = "";
        String msg = "";
        RespActivacion respuesta = new RespActivacion();

        while (msg.equalsIgnoreCase("")) {
            if (!usuario.equalsIgnoreCase("") && usuario != null) {
                if (usuario.length() > 10) {
                    cod = "05";
                    msg = "ERROR VALIDACION USUARIO";
                    LOGGER.info("El usuario no puede tener una longitud mayor a 10");
                    break;
                }
            } else {
                cod = "05";
                msg = "ERROR VALIDACION USUARIO";
                LOGGER.info("El usuario no puede ser nulo");
                break;
            }
            if (!contrasena.equalsIgnoreCase("") && contrasena != null) {
                if (contrasena.length() > 15) {
                    LOGGER.info("La contraseña no puede tener una longitud mayor a 15");
                    cod = "05";
                    msg = "ERROR VALIDACION USUARIO";
                    break;
                }
            } else {
                LOGGER.info("La contraseña no puede ser nula");
                cod = "05";
                msg = "ERROR VALIDACION USUARIO";
                break;
            }
            if (!Utils.validateLogin(usuario, contrasena)) {
                LOGGER.info("Usuario y/o password incorrectos");
                cod = "05";
                msg = "ERROR VALIDACION USUARIO";
                break;
            }
            if (!nroCuenta.trim().isEmpty() || !nroTarjeta.trim().isEmpty()) {
                if (nroCuenta.length() > 12) {
                    LOGGER.info("El numero de cuenta no puede tener una longitud mayor a 12");
                    cod = "03";
                    msg = "CUENTA NO EXISTE";
                    break;
                }
                if (nroTarjeta.length() > 16) {
                    LOGGER.info("El numero de tarjeta no puede tener una longitud mayor a 16");
                    cod = "01";
                    msg = "TARJETA NO EXISTE";
                    break;
                }
                if (!nroTarjeta.trim().isEmpty()) {
                    if (!existNroBin(nroTarjeta.trim())) {
                        LOGGER.info("Numero de tarjeta incorrecto, no pertenece a la entidad");
                        cod = "10";
                        msg = "NUMERO TARJETA INCORRECTO";
                        break;
                    }
                    if (!existTarjeta(nroTarjeta)) {
                        LOGGER.info("El numero de tarjeta no existe");
                        cod = "01";
                        msg = "NUMERO DE TARJETA NO EXISTE";
                        break;
                    }
                }

            } else {
                LOGGER.info("Numero de Cuenta y Numero de Tarjeta no pueden ser vacios");
                cod = "09";
                msg = "CUENTA Y TARJETA VACIOS";
                break;
            }
            if (!cobrarCosto.trim().isEmpty() && cobrarCosto != null) {
                if (!cobrarCosto.equalsIgnoreCase("S") && !cobrarCosto.equalsIgnoreCase("N")) {
                    LOGGER.info("Cobrar costo solo puede ser S o N");
                    cod = "07";
                    msg = "COBRAR COSTO INCORRECTO";
                    break;
                }
            }
            if (!importe.equals("") && importe != null) {
                if (!ErrorUtils.isNumeric(importe.trim())) {
                    LOGGER.info("VALOR DE IMPORTE INCORRECTO");
                    cod = "08";
                    msg = "IMPORTE INCORRECTO";
                    break;
                }
            }

            if (msg.equalsIgnoreCase("")) {
                msg = "OK";
                break;
            }
        }

        if (msg.equalsIgnoreCase("OK")) {
            respuesta = ActivacionManager.procesar(nroCuenta, nroTarjeta, cobrarCosto, importe, usuario);
        } else {
            respuesta.setCodResp(cod);
            respuesta.setMsgResp(msg);
        }
        LOGGER.info("---------------------------- FINALIZA ACTIVACION TC -----------------------------");
        LOGGER.info("Codigo Respuesta: " + respuesta.getCodResp());
        LOGGER.info("Mensaje Respuesta: " + respuesta.getMsgResp());
        LOGGER.info("");
        LOGGER.info("");

        return respuesta;
    }
}
