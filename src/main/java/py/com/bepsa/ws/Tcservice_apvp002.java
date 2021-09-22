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
import py.com.bepsa.clases.RespExtracto;
import static py.com.bepsa.managers.ExtractoManager.procesar;
import static py.com.bepsa.utils.DBUtils.existNroBin;
import static py.com.bepsa.utils.DBUtils.existTarjeta;
import py.com.bepsa.utils.Utils;

/**
 *
 * @author
 */
@WebService(serviceName = "tcservice_apvp002")
public class Tcservice_apvp002 {

    private static final Logger LOGGER = LogManager.getLogger(Tcservice_apvp002.class);

    @WebMethod(operationName = "extractoTC")
    public RespExtracto extractoTC(@WebParam(name = "usuario") String usuario,
            @WebParam(name = "contrasena") String contrasena,
            @WebParam(name = "nroTarjeta") String nroTarjeta) {

        LOGGER.info("----- INICIA EXTRACTO TC:" + new Date() + " -----");
        String cod = "";
        String msg = "";
        RespExtracto respuesta = new RespExtracto();

        while (msg.equalsIgnoreCase("")) {
            if (usuario != null) {
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
            if (contrasena != null) {
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

            if (nroTarjeta.length() > 16) {
                LOGGER.info("El numero de tarjeta no puede tener una longitud mayor a 16");
                cod = "01";
                msg = "NUMERO DE TARJETA NO EXISTE";
                break;
            }
            if (!existTarjeta(nroTarjeta)) {
                LOGGER.info("El numero de tarjeta no existe");
                cod = "01";
                msg = "NUMERO DE TARJETA NO EXISTE";
                break;
            }
            if (!nroTarjeta.trim().isEmpty()) {
                if (!existNroBin(nroTarjeta.trim())) {
                    LOGGER.info("Numero de tarjeta incorrecto, no pertenece a la entidad");
                    cod = "02";
                    msg = "NUMERO TARJETA INCORRECTO";
                    break;
                }
            }
            String tarjetaOculta = nroTarjeta.substring(0, 5) + "******" + nroTarjeta.substring(11);    //Ricardo Arce 042020
            LOGGER.info("NRO. TARJETA:" + tarjetaOculta);    ////Ricardo Arce 042020
            if (msg.equalsIgnoreCase("")) {
                msg = "OK";
                break;
            }
        }

        if (msg.equalsIgnoreCase("OK")) {
            respuesta = procesar(nroTarjeta);
        } else {
            respuesta.setCodResp(cod);
            respuesta.setMsgResp(msg);
        }
        LOGGER.info("----- FINALIZA EXTRACTO TC -----");
        LOGGER.info("Codigo Respuesta: " + respuesta.getCodResp());
        LOGGER.info("Mensaje Respuesta: " + respuesta.getMsgResp());
        LOGGER.info("");
        LOGGER.info("");

        return respuesta;
    }
}
