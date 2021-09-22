/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.com.bepsa.managers;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import py.com.bepsa.clases.LineasDetalle;
import py.com.bepsa.clases.RespExtracto;
import py.com.bepsa.clases.TarConMovimientos;
import py.com.bepsa.pojo.DatoCliente;
import py.com.bepsa.pojo.DatoMoneda;
import py.com.bepsa.pojo.DatoTmctaaf;
import py.com.bepsa.pojo.DatoTmtaraf;
import py.com.bepsa.utils.DBUtils;

/**
 *
 * @author gmigliore
 */
public class ExtractoManager {

    private static final Logger LOGGER = LogManager.getLogger(ExtractoManager.class);
    public static boolean proceso;
    public static String MensajeError = "";

    public static RespExtracto procesar(String nroTarjeta) {
        RespExtracto resp = new RespExtracto();
        TarConMovimientos tarjConMov = new TarConMovimientos();
        List<TarConMovimientos> listaTarConMov = new ArrayList();
        List<LineasDetalle> lineasDetalle = new ArrayList();
        try {
            LOGGER.info("SE OBTIENE LOS DATOS DE LA TARJETA");
            DatoTmtaraf datoTarjeta = DBUtils.getDatosTmtaraf(nroTarjeta);
            LOGGER.info("SE OBTIENE LOS DATOS DE LA CUENTA");
            DatoTmctaaf datoCuenta = DBUtils.getDatosTmctaaf(datoTarjeta.getMCNUMCT());
            //Verificamos si la cuenta existe
            if (!datoCuenta.getMCNUMCT().equals("")) {
                LOGGER.info("SE OBTIENE LOS DATOS DEL CLIENTE");
                DatoCliente datoCliente = DBUtils.getDatosCliente(datoTarjeta.getCENUMDO(), datoTarjeta.getENEMISO(), datoTarjeta.getCETIPOD());
                LOGGER.info("SE OBTIENE LOS DATOS DE LA MONEDA");
                DatoMoneda datoMoneda = DBUtils.getDatosMoneda(datoCuenta.getMOCODIG());

                LOGGER.info("SE CALCULA LA DEUDA TOTAL DE LA TARJETA");
                //Formula para calcular la deuda total de la tarjeta
                String deudaTotal = (datoCuenta.getMCSAFAC() + datoCuenta.getMCSFNVE() + datoCuenta.getMCSALFI() + datoCuenta.getMCCUOPE() + datoCuenta.getMCADEPE() + datoCuenta.getMCREFPE() + "");
//                LOGGER.info("DEUDA TOTAL: " + deudaTotal);
                //Se obtiene fecha vto pago min y Proximo Cierre
                String[] fechaConsulta = new String[3];
                fechaConsulta = DBUtils.getFechaVPMPC(datoTarjeta.getENEMISO(), datoTarjeta.getBIBINES(), datoTarjeta.getAFAFINI()).split(";");
                if (fechaConsulta[0].contains("No existe fecha")) { //Ricardo Arce 042020
                    LOGGER.error("Error, no se obtuvieron los datos de fecha y mensaje");
                    resp = new RespExtracto();
                    resp.setCodResp("96");
                    resp.setMsgResp("ERROR AL PROCESAR LA SOLICITUD");
                    return resp;
                }
                
                //Tarjeta con movimientos
                tarjConMov.setNroTarjeta(datoTarjeta.getMTNUMTA());
                String tipoTarj = (datoTarjeta.getMTTIPOT().equals("1")) ? "P" : "A";
                tarjConMov.setTipoTarjeta(tipoTarj);
                tarjConMov.setNombreCliente(datoCliente.getCeapnom());
                LOGGER.info("SE OBTIENE LOS MOVIMIENTOS DE LA TARJETA");
                listaTarConMov.add(tarjConMov);

                //Se traen los detalles de cada linea
                LOGGER.info("SE OBTIENE LAS LINEAS DE DETALLE");
                lineasDetalle = DBUtils.getLineasDetalle(nroTarjeta, datoTarjeta.getAFAFINI(), datoTarjeta.getENEMISO(), datoTarjeta.getBIBINES());

                resp.setNombreCliente(datoCliente.getCeapnom());
                String[] consMarcaClase = new String[2];
                consMarcaClase = DBUtils.getMarcaClase(nroTarjeta.substring(0,6), "C");
                resp.setMarca(consMarcaClase[0]);
                resp.setClase(consMarcaClase[1]);
                resp.setAfinidad(datoTarjeta.getAFAFINI());
                resp.setMoneda(datoMoneda.getModescr());
                //Obtenemos limite de credito de la tarjeta
                long limiteCredito = datoCuenta.getMCLIMCO() + datoCuenta.getMCLIMCU();
                resp.setLineaCredito(limiteCredito + "");
                resp.setDispCompraNormal(datoCuenta.getMCDISCO() + "");
                resp.setDispCompraCuotas(datoCuenta.getMCDISCU() + "");
                resp.setDispAvanceEfectivo(datoCuenta.getMCDISCO() + "");
                resp.setDeudaCompraNormal(datoCuenta.getMCSAFAC() + "");
                resp.setDeudaCompraCuotas(datoCuenta.getMCCUOPE() + "");
                resp.setDeudaTotal(deudaTotal);
                resp.setDeudaEnMora(datoCuenta.getMCSALMO() + "");
                //este espera
                LOGGER.info("SE OBTIENE MONTO DEL PAGO MINIMO PENDIENTE");
                resp.setPagoMinPendiente(DBUtils.getPagoMinPendiente(datoTarjeta.getENEMISO(), datoTarjeta.getAFAFINI(), nroTarjeta, datoCuenta) + "");

                LOGGER.info("SE SETEA FechaVtoPagoMin");
//        LOGGER.info("Consulta 0 " + fechaConsulta[0]);
                resp.setFechaVtoPagoMin(fechaConsulta[0]);
                LOGGER.info("SE SETEA ProxCierre");
//        LOGGER.info("Consulta 1 " + fechaConsulta[1]);
                resp.setFechaProxCierre(fechaConsulta[1]);
                LOGGER.info("SE SETEA TarConMovimientos");
                resp.setTarConMovimientos(listaTarConMov);
                LOGGER.info("SE SETEA LineasDetalle");
                resp.setLineasDetalle(lineasDetalle);
                LOGGER.info("SE SETEA MensajeExtracto");

                resp.setMensajeExtracto(fechaConsulta[2]);
                LOGGER.info("SE SETEA nro de trasaccion");
                resp.setNroTransaccion("1");
//        LOGGER.info("SE SETEA cod de rpta"); 
                resp.setCodResp("00");
//        LOGGER.info("SE SETEA Mensaje rpta");
                resp.setMsgResp("PROCESADO CORRECTAMENTE");
            } else {
                LOGGER.error("Error, no se obtuvo los datos de la cuenta");
                resp.setCodResp("96");
                resp.setMsgResp("ERROR AL PROCESAR LA SOLICITUD");
            }
        } catch (Exception ex) {
            LOGGER.error("Error en procesar(): " + ex.getMessage());
            resp = new RespExtracto();
            resp.setCodResp("96");
            resp.setMsgResp("ERROR AL PROCESAR LA SOLICITUD");
        }
        return resp;
    }

}
