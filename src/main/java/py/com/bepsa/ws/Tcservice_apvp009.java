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
import py.com.bepsa.clases.RespPagoTC;
import py.com.bepsa.managers.PagoReversaManager;
import static py.com.bepsa.utils.DBUtils.existNroBin;
import static py.com.bepsa.utils.DBUtils.existTarjeta;
import py.com.bepsa.utils.ErrorUtils;
import py.com.bepsa.utils.Utils;

/**
 *
 * @author rarce
 */
@WebService(serviceName = "tcservice_apvp009")
public class Tcservice_apvp009 {

    private static final Logger LOGGER = LogManager.getLogger(Tcservice_apvp009.class);

    @WebMethod(operationName = "pagoTC")
    public RespPagoTC pagoTC(@WebParam(name = "usuario") String usuario,
            @WebParam(name = "contrasena") String contrasena,
            @WebParam(name = "tipoOperacion") String tipoOperacion,
            @WebParam(name = "nroTarjeta") String nroTarjeta,
            @WebParam(name = "montoPago") String montoPago,
            @WebParam(name = "origen") String origen,
            @WebParam(name = "formaPago") String formaPago,
            @WebParam(name = "nroCheque") String nroCheque,
            @WebParam(name = "ctaDebito") String ctaDebito,
            @WebParam(name = "nroPedido") String nroPedido) //            @WebParam(name = "nroTransaccion") String nroTransaccion,
    //            @WebParam(name = "fechaAfecDisp") String fechaAfecDisp,
    //            @WebParam(name = "fechaExtracto") String fechaExtracto)
    {
        //PropertyConfigurator.configure("LOGGERActivacionesTC.properties");
        //<editor-fold desc="VALIDACIONES INICIALES">
        LOGGER.info("---------- INICIA " + ((tipoOperacion.equalsIgnoreCase("P")) ? "PAGO" : "REVERSA") + " TC:" + new Date() + " ----------");
        String tarjetaOculta = nroTarjeta.substring(0, 5) + "******" + nroTarjeta.substring(11);
        LOGGER.info("NRO. TARJETA:" + tarjetaOculta);
        LOGGER.info("MONTO PAGO: " + montoPago);
        LOGGER.info("FORMA DE PAGO: " + formaPago);
        LOGGER.info("NUMERO PEDIDO: " + nroPedido);
//        printStackTrace();
        String cod = "";
        String msg = "";
        RespPagoTC respuesta = new RespPagoTC();

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
            if (!tipoOperacion.equalsIgnoreCase("P") && !tipoOperacion.equalsIgnoreCase("R")) {
                LOGGER.info(msg);
                cod = "06";
                msg = "TIPO OPERACION INCORRECTO";
                break;
            }
            if (!nroTarjeta.trim().isEmpty()) {
                if (nroTarjeta.length() > 16) {
                    LOGGER.info("El numero de tarjeta no puede tener una longitud mayor a 16");
                    cod = "07";
                    msg = "NUMERO DE TARJETA INCORRECTO";
                    break;
                }
                if (!existNroBin(nroTarjeta.trim())) {
                    LOGGER.info("El numero de tarjeta no pertenece a la entidad");
                    cod = "07";
                    msg = "NUMERO DE TARJETA INCORRECTO";
                    break;
                }
                if (!existTarjeta(nroTarjeta)) {
                    LOGGER.info("El numero de tarjeta no existe");
                    cod = "01";
                    msg = "NUMERO DE TARJETA NO EXISTE";
                    break;
                }
            } else {
                LOGGER.info("Numero de Tarjeta no pueden ser vacio");
                cod = "07";
                msg = "NUMERO DE TARJETA INCORRECTO";
                break;
            }
            if (!ErrorUtils.isNumeric(montoPago)) {
                cod = "08";
                msg = "MONTO INCORRECTO";
                LOGGER.info(msg);
                break;
            }
            if (formaPago.equalsIgnoreCase("E") || formaPago.equalsIgnoreCase("C") || formaPago.equalsIgnoreCase("D")) {
                if (formaPago.equalsIgnoreCase("C")) {
                    if (nroCheque.length() > 15) {
                        cod = "09";
                        msg = "FORMA DE PAGO INCORRECTA";
                        LOGGER.info(msg);
                        break;
                    }
                }
                if (formaPago.equalsIgnoreCase("D")) {
                    if (ctaDebito.length() > 12) {
                        cod = "09";
                        msg = "FORMA DE PAGO INCORRECTA";
                        LOGGER.info(msg);
                        break;
                    }
                }
            } else {
                LOGGER.info(msg);
                cod = "09";
                msg = "FORMA DE PAGO INCORRECTA";
                break;
            }
            if (!ErrorUtils.isNumeric(nroPedido) || nroPedido.length() > 6) {
                cod = "10";
                msg = "NUMERO DE PEDIDO INCORRECTO";
                LOGGER.info(msg);
                break;
            }
//            if (fechaAfecDisp.length() > 8) {
//                cod = "";
//                msg = "FECHA AFECTACION DISPONIBLE INCORRECTO";
//                LOGGER.info(msg);
//                break;
//            }
//            if (fechaExtracto.length() > 8) {
//                cod = "";
//                msg = "FECHA EXTRACTO INCORRECTO";
//                LOGGER.info(msg);
//                break;
//            }
            if (msg.equalsIgnoreCase("")) {
                msg = "OK";
                break;
            }
        }

        if (msg.equalsIgnoreCase("OK")) {
//            respuesta = PagoReversaManager.procesar(nroTarjeta, nroTransaccion, tipoOperacion, montoPago, nroPedido, usuario);
            respuesta = PagoReversaManager.procesar(nroTarjeta, tipoOperacion, montoPago, nroPedido, usuario);
        } else {
            respuesta.setCodResp(cod);
            respuesta.setMsgResp(msg);
        }
        LOGGER.info("---------- FINALIZA " + ((tipoOperacion.equalsIgnoreCase("P")) ? "PAGO" : "REVERSA") + " TC ------");
        LOGGER.info("Numero Transaccion: " + respuesta.getNroTransaccion());
        LOGGER.info("Fecha Afectacion Disponible: " + respuesta.getFechaAfecDisp());
        LOGGER.info("Fecha Extracto: " + respuesta.getFechaExtracto());
        LOGGER.info("Codigo Respuesta: " + respuesta.getCodResp());
        LOGGER.info("Mensaje Respuesta: " + respuesta.getMsgResp());
        LOGGER.info("");
        LOGGER.info("");
        return respuesta;
    }
}
