/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.com.bepsa.managers;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import py.com.bepsa.pojo.DatoDireccion;
import py.com.bepsa.pojo.DatoTmctaaf;
import py.com.bepsa.pojo.DatoTmtaraf;
import py.com.bepsa.pojo.Regrabacion;
import py.com.bepsa.utils.DBUtils;
import static py.com.bepsa.utils.DBUtils.existNroBin;
import static py.com.bepsa.utils.DBUtils.getCuota;
import static py.com.bepsa.utils.DBUtils.getDatosTmtaraf;
import static py.com.bepsa.utils.DBUtils.getDireccion;
import py.com.bepsa.utils.Utils;

/**
 *
 * @author gmigliore
 */
public class RegrabacionManager {

    private static final Logger LOGGER = LogManager.getLogger(RegrabacionManager.class);
    public static boolean proceso;
    public static String MensajeError = "";

    public static String procesar(List<Regrabacion> aProcesar) {
        String nroTarjeta = "";
        //String vencimiento = "";
        int duracion = 0;
        String costoPersonalizado = "";
        String cuotas = "";
        boolean tarjetaN = false;
        boolean venceN = false;
        boolean costoN = false;
        String resp = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyMM");
        SimpleDateFormat sdf3 = new SimpleDateFormat("HHmmss");
        SimpleDateFormat sdf4 = new SimpleDateFormat("HH:mm:ss");
        Date dateHoy = new Date();
        Calendar fechaHoy = Calendar.getInstance();
        String fecha = sdf.format(dateHoy);
        String hora = sdf3.format(dateHoy);
        String hora2 = sdf4.format(dateHoy);
        LOGGER.info("INICIO DE RECORRIDO");
        proceso = true;
        boolean procesoReg;
        String errores = "";
        int contProc = 0;
        int contError = 0;
        int linea = 0;

        DatoTmctaaf datoCuenta = null;
        String tarjOfus = "";
        String tarjOfusN = "";
        String getSecuencia = "select max(motasec) from Gxbdbps.tmmota";
        long secTmmota = 0;
        try {
            Connection con = DBUtils.connect();
            secTmmota = DBUtils.getSecuencia(getSecuencia, con);
            String insertTransferencia = "INSERT INTO Gxbdbps.TMMOTA VALUES ('021' , " + secTmmota + ", '" + fecha + "', '" + hora2 + "', 3, 'S', 0, 0, 0, " + aProcesar.size() + ", 1)";
            if (!DBUtils.ejecucionSQL(insertTransferencia)) {
                LOGGER.error("ERROR AL INSERTAR TMMOTA");
            }
            con.close();
        } catch (Exception e) {
            LOGGER.error("ERROR AL OBTENER SECUENCIA" + e);

        }
        for (Regrabacion regrabacion : aProcesar) {//SE ITERA EL PEDIDO DE REGRABACIONES A PROCESAR
            errores = "";
            linea += 1;
            procesoReg = true;
            MensajeError ="";
            tarjOfus = regrabacion.getNroTarjeta().substring(0, 6) + "******" + regrabacion.getNroTarjeta().substring(12);
            if (regrabacion.getNroTarjetaNueva() != null && !regrabacion.getNroTarjetaNueva().equals("")) {
                tarjOfusN = regrabacion.getNroTarjetaNueva().substring(0, 6) + "******" + regrabacion.getNroTarjetaNueva().substring(12);
            }
            LOGGER.info("COBRAR COSTO:" + regrabacion.getCobrarCosto());
            LOGGER.info("FECHA:" + regrabacion.getFecha());
            LOGGER.info("ID MOTIVO:" + regrabacion.getIdMotivo());
            LOGGER.info("IMPORTE:" + regrabacion.getImporte());
            LOGGER.info("NRO TARJETA:" + tarjOfus);
            LOGGER.info("NRO TARJETA NUEVA:" + tarjOfusN);
            LOGGER.info("USUARIO:" + regrabacion.getUsuario());
            LOGGER.info("VENCIMIENTO NUEVO:" + regrabacion.getVencimiento());
            LOGGER.info("OBTENGO DATOS DE TMTARAF");
            DatoTmtaraf dato = getDatosTmtaraf(regrabacion.getNroTarjeta());
            //Se guarda un string con los datos de entrada
            String datoEntrada = regrabacion.getIdMotivo() + " ;" + regrabacion.getNroTarjeta() + " ;" + regrabacion.getNroTarjetaNueva() + " ;" + regrabacion.getVencimiento() +
                    " ;" + regrabacion.getImporte() + " ;" + regrabacion.getCobrarCosto() + " ;" + regrabacion.getFecha() + " ;" + regrabacion.getUsuario();
            LOGGER.info("SE VALIDA LOS DATOS DE ENTRADA");
            boolean validacion = validarDatos(regrabacion, MensajeError);
            if (validacion) { //Ingresa si los datos de entrada son correctos
                if (!dato.getMTNUMTA().equalsIgnoreCase("")) {
                    LOGGER.info("VOY A OBTENER DATOS DE TMCTAAF");
                    datoCuenta = DBUtils.getDatosTmctaaf(dato.getMCNUMCT().trim());
//                    LOGGER.info("VEO QUE HACER");
                } else {
                    if (MensajeError.equalsIgnoreCase("")) {
                        MensajeError += "170";
                    } else {
                        MensajeError += ";170";
                    }
                    resp = "NUMERO DE TARJETA NO EXISTE";
                    LOGGER.info(resp);
                    proceso = false;
                    contError += 1;
                    String insertTdmota = "INSERT INTO Gxbdbps.TDMOTA VALUES( " + secTmmota + ", " + linea + ", 0, "
                            + "" + (!dato.getMCNUMCT().equalsIgnoreCase("") ? dato.getMCNUMCT() : 0) + ", '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '', '1',"
                            + "" + (!MensajeError.equalsIgnoreCase("") ? 3 : 1) + " , '" + MensajeError + "', '" + datoEntrada + "', '021')";
                    if (!DBUtils.ejecucionSQL(insertTdmota)) {
                        LOGGER.error("ERROR AL ACTUALIZAR EL DETALLE TDMOTA");
                    }
                    continue;
                }
                if (regrabacion.getNroTarjetaNueva() != null && !regrabacion.getNroTarjetaNueva().equalsIgnoreCase("")) {
                    LOGGER.info("OBTENGO DATOS DE TMTARAF CON EL NRO DE TARJETA NUEVA");
                    DatoTmtaraf datoTarNueva = getDatosTmtaraf(regrabacion.getNroTarjetaNueva());
                    if (datoTarNueva.getMTNUMTA() != null && !datoTarNueva.getMTNUMTA().equalsIgnoreCase("")) {
                        resp = "NUMERO DE TARJETA NUEVA YA EXISTE";
                        LOGGER.info(resp);
                        proceso = false;
                        contError += 1;
                        if (MensajeError.equalsIgnoreCase("")) {
                            MensajeError += "171";
                        } else {
                            MensajeError += ";171";
                        }
                        String insertTdmota = "INSERT INTO Gxbdbps.TDMOTA VALUES( " + secTmmota + ", " + linea + ", 0, "
                                + "" + (!dato.getMCNUMCT().equalsIgnoreCase("") ? dato.getMCNUMCT() : 0) + ", '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '', '1',"
                                + "" + (!MensajeError.equalsIgnoreCase("") ? 3 : 1) + " , '" + MensajeError + "', '" + datoEntrada + "', '021')";
                        if (!DBUtils.ejecucionSQL(insertTdmota)) {
                            LOGGER.error("ERROR AL ACTUALIZAR EL DETALLE TDMOTA");
                        }
                        continue;
                    }
                }
                if (!existNroBin(regrabacion.getNroTarjeta())) {
                    resp = "BIN O ENTIDAD DE LA TARJETA INCORRECTA";
                    LOGGER.info(resp);
                    proceso = false;
                    contError += 1;
                    if (MensajeError.equalsIgnoreCase("")) {
                        MensajeError += "174";
                    } else {
                        MensajeError += ";174";
                    }
                    String insertTdmota = "INSERT INTO Gxbdbps.TDMOTA VALUES( " + secTmmota + ", " + linea + ", 0, "
                            + "" + (!dato.getMCNUMCT().equalsIgnoreCase("") ? dato.getMCNUMCT() : 0) + ", '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '', '1', "
                            + "" + (!MensajeError.equalsIgnoreCase("") ? 3 : 1) + " ,'" + MensajeError + "', '" + datoEntrada + "', '021')";
                    if (!DBUtils.ejecucionSQL(insertTdmota)) {
                        LOGGER.error("ERROR AL ACTUALIZAR EL DETALLE TDMOTA");
                    }
                    continue;
                }
                 if (dato.getMTNUMTA() == null || dato.getMTNUMTA().equalsIgnoreCase("")) {
                    resp = "NO SE ENCUENTRA LA TARJETA A REGRABAR";
                    LOGGER.info(resp);
                    proceso = false;
                    contError += 1;
                    if (MensajeError.equalsIgnoreCase("")) {
                        MensajeError += "170";
                    } else {
                        MensajeError += ";170";
                    }
                    String insertTdmota = "INSERT INTO Gxbdbps.TDMOTA VALUES(" + secTmmota + ", " + linea + ", 0, "
                            + "" + (!dato.getMCNUMCT().equalsIgnoreCase("") ? dato.getMCNUMCT() : 0) + ", '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '', '1', "
                            + "" + (!MensajeError.equalsIgnoreCase("") ? 3 : 1) + " , '" + MensajeError + "', '" + datoEntrada + "', '021')";
                    if (!DBUtils.ejecucionSQL(insertTdmota)) {
                        LOGGER.error("ERROR AL ACTUALIZAR EL DETALLE TDMOTA");
                    }
                    continue;
                }
                if (datoCuenta.getMCNUMCT() == null) {
                    resp = "NO SE ENCUENTRA LA CUENTA A REGRABAR";
                    LOGGER.info(resp);
                    proceso = false;
                    contError += 1;
                    if (MensajeError.equalsIgnoreCase("")) {
                        MensajeError += "164";
                    } else {
                        MensajeError += ";164";
                    }
                    String insertTdmota = "INSERT INTO Gxbdbps.TDMOTA VALUES(" + secTmmota + ", " + linea + ", 0, "
                            + "" + (!dato.getMCNUMCT().equalsIgnoreCase("") ? dato.getMCNUMCT() : 0) + ", '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '', '1', "
                            + "" + (!MensajeError.equalsIgnoreCase("") ? 3 : 1) + " , '" + MensajeError + "', '" + datoEntrada + "', '021')";
                    if (!DBUtils.ejecucionSQL(insertTdmota)) {
                        LOGGER.error("ERROR AL ACTUALIZAR EL DETALLE TDMOTA");
                    }
                    continue;
                }

                if (regrabacion.getIdMotivo().equalsIgnoreCase(Regrabacion.regrabarExtravio)) {

                    if (regrabacion.getNroTarjetaNueva() != null && !regrabacion.getNroTarjetaNueva().equalsIgnoreCase("")) { //SI TIENE NUEVO NUMERO DE TARJETA SE SETEA EL MISMO
                        nroTarjeta = regrabacion.getNroTarjetaNueva();
                        tarjetaN = true;
                    } else {//SINO SE UTILIZA EL QUE YA TIENE
                        nroTarjeta = regrabacion.getNroTarjeta();
                    }

                    if (regrabacion.getVencimiento() != null && !regrabacion.getVencimiento().equalsIgnoreCase("")) {//SI VIENE UNA NUEVA FECHA DE VENCIMIENTO, SE UTILIZA LA QUE VIENE Y SE ANALIZA SI CORRESPONDE RECALCULAR LA DURACION
                        String mes = regrabacion.getVencimiento().substring(4);
                        String anho = regrabacion.getVencimiento().substring(2, 4);
                        Calendar cal = Calendar.getInstance();
                        int mesActual = cal.get(Calendar.MONTH);
                        String mesActualS = (mesActual + "").length() == 2 ? (mesActual + "") : "0" + mesActual;
                        int anhoActual = cal.get(Calendar.YEAR);
                        String anhoActualS = (anhoActual + "").substring(2);
                        anhoActual = Integer.parseInt(anhoActualS);
                        int difAnho = anhoActual - Integer.parseInt(anho);
                        int difMes = mesActual - Integer.parseInt(mes);
                        if (difAnho < 1) {//SI FALTA MENOS DE UN AÑO PARA EL VENCIMIENTO
                            if (difMes > 0 && difMes <= 3) {//Y TRES MESES O MENOS
                                //SE RECALCULA LA DURACION
                                String afinidad = DBUtils.getAfinity(regrabacion.getNroTarjeta(), "021");
                                duracion = DBUtils.getDuration(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad);
                            } else {//SINO SE CALCULA LA DIFERENCIA DE MESES ENTRE EL VENCIMIENTO Y LA FECHA ACTUAL
                                duracion = Utils.getMonthDiference("01/" + mes + "/" + anho, "01/" + mesActualS + "/" + anhoActualS);
                            }
                        } else {//SINO SE CALCULA LA DIFERENCIA DE MESES ENTRE EL VENCIMIENTO Y LA FECHA ACTUAL
                            duracion = Utils.getMonthDiference("01/" + mes + "/" + anho, "01/" + mesActualS + "/" + anhoActualS);
                        }
                        venceN = true;
                    } else {//SE UTILIZA LA FECHA DE VENCIMIENTO QUE YA TENIA LA TARJETA Y SE CALCULA SI ES NECESARIO RECALCULAR LA DURACION
                        String fechaVencimiento = DBUtils.getExpirationDate(regrabacion.getNroTarjeta(), "021");
                        String mes = fechaVencimiento.substring(2);
                        String anho = fechaVencimiento.substring(0, 2);
                        Calendar cal = Calendar.getInstance();
                        int mesActual = cal.get(Calendar.MONTH);
                        String mesActualS = (mesActual + "").length() == 2 ? (mesActual + "") : "0" + mesActual;
                        int anhoActual = cal.get(Calendar.YEAR);
                        String anhoActualS = (anhoActual + "").substring(2);
                        anhoActual = Integer.parseInt(anhoActualS);
                        int difAnho = anhoActual - Integer.parseInt(anho);
                        int difMes = mesActual - Integer.parseInt(mes);
                        if (difAnho < 1) {//SI FALTA MENOS DE UN AÑO PARA EL VENCIMIENTO
                            if (difMes > 0 && difMes <= 3) {//Y TRES MESES O MENOS
                                String afinidad = DBUtils.getAfinity(regrabacion.getNroTarjeta(), "021");
                                duracion = DBUtils.getDuration(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad);
                                LOGGER.info("DURACION:" + duracion);
                                venceN = true;
                            } else {//SINO SE CALCULA LA DIFERENCIA DE MESES ENTRE EL VENCIMIENTO Y LA FECHA ACTUAL
                                duracion = Utils.getMonthDiference("01/" + mes + "/" + anho, "01/" + mesActualS + "/" + anhoActualS);
                                LOGGER.info("DURACION:" + duracion);
                            }
                        } else {//SINO SE CALCULA LA DIFERENCIA DE MESES ENTRE EL VENCIMIENTO Y LA FECHA ACTUAL
                            duracion = Utils.getMonthDiference("01/" + mes + "/" + anho, "01/" + mesActualS + "/" + anhoActualS);
                            LOGGER.info("DURACION:" + duracion);
                        }
                    }
                    fechaHoy.add(Calendar.MONTH, duracion);
                    String vencimiento = sdf2.format(fechaHoy.getTime());
                    LOGGER.info("VENCIMIENTO FINAL:" + vencimiento);
                    LOGGER.info("SE VERIFICA SI SE COBRA COSTO");
                    if (regrabacion.getCobrarCosto().equalsIgnoreCase("N")) {
                        costoPersonalizado = "0";
                        cuotas = "0";
                    } else {
                        String afinidad = DBUtils.getAfinity(regrabacion.getNroTarjeta(), "021");
                        cuotas = "" + getCuota(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad, dato.getMTTIPOT().trim());
                        if (regrabacion.getImporte() != null && !regrabacion.getImporte().equalsIgnoreCase("")) { //SE VERIFICA SI LLEGA UN IMPORTE PERSONALIZADO
                            costoPersonalizado = regrabacion.getImporte().trim();
                            costoN = true;
                        } else {
                            costoPersonalizado = DBUtils.getCost(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad, dato.getMTTIPOT().trim()) + "";
                            costoPersonalizado = costoPersonalizado.trim();
                        }
                    }
                    LOGGER.info("COSTO PERSONALIZADO:" + costoPersonalizado);
                    LOGGER.info("CUOTAS:" + cuotas);
                    LOGGER.info("REALICE TODOAS LAS VERIFICACIONES");
                    if (tarjetaN && venceN) {
                        LOGGER.info("TARJETA NUEVA Y NUEVO VENCIMIENTO");
                        LOGGER.info("SE OBTIENE DIRECCION Y TELEFONO DEL CLIENTE");
                        DatoDireccion direccion = getDireccion(dato.getCENUMDO(), dato.getENEMISO());
                        //BLOQUEO DE TARJETA ANTERIOR
                        String query = "UPDATE GXBDBPS.TMTARAF SET BLCODIG = '05', MTTIPOT= '3', MTUSPED = '" + regrabacion.getUsuario() + "', "
                                + "MTFEPED='" + fecha + "', MTESTBL = 'B' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                        LOGGER.info("BLOQUEO DE TARJETA ANTERIOR");
                        if (!DBUtils.ejecucionSQL(query)) {
                            procesoReg = false;
                            resp = "ERROR AL PROCESAR LA SOLICITUD";
                            LOGGER.error("ERROR AL REALIZAR EL BLOQUEO DE LA TARJETA ANTERIOR");
                        }

                        //SE GRABA LA AUDITORIA codigo bloqueo
                        LOGGER.info("SE GRABA LA AUDITORIA DE TMTARAF codigo bloqueo");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Codigo de bloqueo", dato.getBLCODIG().trim(), "05")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA codigo bloqueo TMTARAF");
                        }

                        //SE GRABA LA AUDITORIA Usuario pedido
                        LOGGER.info("SE GRABA LA AUDITORIA Usuario pedido");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Usuario que pidio", dato.getMTUSPED(), regrabacion.getUsuario())) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA Usuario pedido TMTARAF");
                        }

                        //SE GRABA LA AUDITORIA Tipo de bloqueo
                        LOGGER.info("SE GRABA LA AUDITORIA Tipo de bloqueo");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Tipo de bloqueo", dato.getMTESTBL(), "B")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA Tipo de bloqueo TMTARAF");
                        }
                        
                        //SE GRABA LA AUDITORIA Tipo de tarjeta
                        LOGGER.info("SE GRABA LA AUDITORIA Tipo de tarjeta");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Tipo de tarjeta", dato.getMTTIPOT(), "3")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA Tipo de tarjeta TMTARAF");
                        }

                        //NUEVO REGISTRO DE LA NUEVA TARJETA
                        query = "INSERT INTO GXBDBPS.TMTARAF VALUES ('" + regrabacion.getNroTarjetaNueva() + "', " + dato.getMTULTSC() + ", "
                                + "'" + dato.getBLCODIG() + "', '" + dato.getMTSTATS() + "', '" + dato.getENEMISO() + "', '" + dato.getSUCODIG() + "', "
                                + "'" + dato.getCETIPOD() + "', '" + dato.getCENUMDO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                + "" + dato.getMCNUMCT() + ", '" + (dato.getMTTIPOT().equalsIgnoreCase("1") ? "4" : dato.getMTTIPOT()) + "', '" + dato.getMTCLVIP() + "', '" + dato.getMTNOPLA() + "', "
                                + "'" + dato.getMTEMPRE() + "', '" + dato.getMTFECRE() + "', '" + dato.getMTFEEMI() + "', '" + dato.getMTFEBAJ() + "', "
                                + "" + vencimiento + ", " + dato.getMTFEVE2() + ", '" + dato.getMTMBOLE() + "', '" + dato.getMTMRECU() + "', "
                                + "'" + dato.getMTFEREC() + "', '', '" + dato.getMTCODS2() + "', '" + dato.getMTNUMTA() + "', "
                                + "'" + dato.getMTRETEN() + "', '" + dato.getMTRENOV() + "', '" + dato.getMTCOBRA() + "', '" + dato.getMTFEPED() + "', "
                                + "'" + dato.getMTUSPED() + "', '" + dato.getMTUSEMI() + "', '" + dato.getMTESTVE() + "', '" + dato.getMTESTEX() + "', "
                                + "'" + dato.getMTESTBL() + "', '" + dato.getMTESTMO() + "', 'E', '" + dato.getMTFEBLQ() + "'"
                                + ")";
                        LOGGER.info("NUEVO REGISTRO DE LA NUEVA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR REGISTRO DE LA NUEVA TARJETA");
                        }

                        //SE AGREGA EL DATO DE COBRO DE LA TARJETA
                        query = "INSERT INTO GXBDBPS.TEMBSAF VALUES ('" + regrabacion.getNroTarjetaNueva() + "', "
                                + "'" + fecha + "', '" + dato.getENEMISO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                + "'001', '1', '00000000', " + costoPersonalizado + ", " + (cuotas.equalsIgnoreCase("0") ? "1" : cuotas) + ", 'N', 'N'"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DE COBRO DE LA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATOS DEL COBRO");
                        }
                        //SE AGREGA EL DATO DEL EMBOZADO
                        query = "INSERT INTO LIBDEBITO.EMBNOV0P VALUES ('" + dato.getENEMISO() + "', '001', '" + dato.getAFAFINI() + "',"
                                + "'" + dato.getBIBINES() + "', '" + regrabacion.getNroTarjetaNueva() + "', ''," + dato.getMCNUMCT() + ",  "
                                + "'" + datoCuenta.getMCCTABC() + "', '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '" + vencimiento + "',"
                                + "'" + dato.getMTEMPRE() + "', '" + direccion.getDireccion() + "', '" + direccion.getLocalidad() + "', '" + direccion.getNroTelefono() + "', "
                                + "" + datoCuenta.getMCLIMCO() + ", '1', '" + regrabacion.getUsuario() + "', "
                                + "" + hora + ", " + fecha + ", 0,  '', 0, 0"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DEL EMBOZADO");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATO DEL EMBOZADO");
                        }
                        //SE ASOCIA LA NUEVA TARJETA A LA CUENTA SI ES PRINCIPAL
                        if (dato.getMTTIPOT().equals("1")) {
                            query = "UPDATE Gxbdbps.TMCTAAF SET MCNUMTA = '" + regrabacion.getNroTarjetaNueva() + "' WHERE MCNUMCT = " + dato.getMCNUMCT();
                            LOGGER.info("SE MODIFICA LA NUEVA TARJETA PRINCIPAL EN LA CUENTA");
                            if (!DBUtils.ejecucionSQL(query)) {
                                procesoReg = false;
                                resp = "ERROR AL PROCESAR LA SOLICITUD";
                                LOGGER.error("ERROR AL MODIFICAR LA NUEVA TARJETA PRINCIPAL EN LA CUENTA");
                            }
                        }
                        if (procesoReg) {
                            contProc += 1;
                        } else {
                            contError += 1;
                        }
                    } else if (tarjetaN && !venceN) {

                        //BLOQUEO DE TARJETA ANTERIOR
                        String query = "UPDATE GXBDBPS.TMTARAF SET BLCODIG = '05', MTTIPOT= '3', MTUSPED = '" + regrabacion.getUsuario() + "', "
                                + "MTFEPED='" + fecha + "', MTESTBL = 'B' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                        LOGGER.info("BLOQUEO DE TARJETA ANTERIOR");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                        }

                        //SE GRABA LA AUDITORIA codigo bloqueo
                        LOGGER.info("SE GRABA LA AUDITORIA codigo bloqueo");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Codigo de bloqueo", dato.getBLCODIG().trim(), "05")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA codigo bloqueo TMTARAF");
                        }

                        //SE GRABA LA AUDITORIA Usuario pedido
                        LOGGER.info("SE GRABA LA AUDITORIA Usuario pedido");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Usuario que pidio", dato.getMTUSPED(), regrabacion.getUsuario())) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA Usuario pedido TMTARAF");
                        }

                        //SE GRABA LA AUDITORIA Tipo de bloqueo
                        LOGGER.info("SE GRABA LA AUDITORIA Tipo de bloqueo");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Tipo de bloqueo", dato.getMTESTBL(), "B")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA codigo bloqueo TMTARAF");
                        }
                        
                        //SE GRABA LA AUDITORIA Tipo de tarjeta
                        LOGGER.info("SE GRABA LA AUDITORIA Tipo de tarjeta");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Tipo de tarjeta", dato.getMTTIPOT(), "3")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA tipo tarjeta TMTARAF");
                        }
                        //NUEVO REGISTRO DE LA NUEVA TARJETA
                        query = "INSERT INTO GXBDBPS.TMTARAF VALUES ('" + regrabacion.getNroTarjetaNueva() + "', " + dato.getMTULTSC() + ", "
                                + "'" + dato.getBLCODIG() + "', '" + dato.getMTSTATS() + "', '" + dato.getENEMISO() + "', '" + dato.getSUCODIG() + "', "
                                + "'" + dato.getCETIPOD() + "', '" + dato.getCENUMDO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                + "" + dato.getMCNUMCT() + ", '" + (dato.getMTTIPOT().equalsIgnoreCase("1") ? "4" : dato.getMTTIPOT()) + "', '" + dato.getMTCLVIP() + "', '" + dato.getMTNOPLA() + "', "
                                + "'" + dato.getMTEMPRE() + "', '" + dato.getMTFECRE() + "', '" + dato.getMTFEEMI() + "', '" + dato.getMTFEBAJ() + "', "
                                + "" + dato.getMTFEVEN() + ", " + dato.getMTFEVE2() + ", '" + dato.getMTMBOLE() + "', '" + dato.getMTMRECU() + "', "
                                + "'" + dato.getMTFEREC() + "', '', '" + dato.getMTCODS2() + "', '" + dato.getMTNUMTA() + "', "
                                + "'" + dato.getMTRETEN() + "', '" + dato.getMTRENOV() + "', '" + dato.getMTCOBRA() + "', '" + dato.getMTFEPED() + "', "
                                + "'" + dato.getMTUSPED() + "', '" + dato.getMTUSEMI() + "', '" + dato.getMTESTVE() + "', '" + dato.getMTESTEX() + "', "
                                + "'" + dato.getMTESTBL() + "', '" + dato.getMTESTMO() + "', 'E', '" + dato.getMTFEBLQ() + "'"
                                + ")";
                        LOGGER.info("NUEVO REGISTRO DE LA NUEVA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR REGISTRO DE LA NUEVA TARJETA");
                        }
                    //query = "UPDATE GXBDBPS.TMCTAAF SET MCTIPOC = '3' WHERE ";

                        //SE AGREGA EL DATO DE COBRO DE LA TARJETA
                        query = "INSERT INTO GXBDBPS.TEMBSAF VALUES ('" + regrabacion.getNroTarjetaNueva() + "', "
                                + "'" + fecha + "', '" + dato.getENEMISO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                + "'001', '1', '00000000', " + costoPersonalizado + ", " + (cuotas.equalsIgnoreCase("0") ? "1" : cuotas) + ", 'N', 'N'"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DE COBRO DE LA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATOS DEL COBRO");
                        }
                        //SE AGREGA EL DATO DEL EMBOZADO
                        DatoDireccion direccion = getDireccion(dato.getCENUMDO(), dato.getENEMISO());
                        query = "INSERT INTO LIBDEBITO.EMBNOV0P VALUES ('" + dato.getENEMISO() + "', '001', '" + dato.getAFAFINI() + "',"
                                + "'" + dato.getBIBINES() + "', '" + regrabacion.getNroTarjetaNueva() + "', ''," + dato.getMCNUMCT() + ",  "
                                + "'" + datoCuenta.getMCCTABC() + "', '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '" + dato.getMTFEVEN() + "',"
                                + "'" + dato.getMTEMPRE() + "', '" + direccion.getDireccion() + "', '" + direccion.getLocalidad() + "', '" + direccion.getNroTelefono() + "', "
                                + "" + datoCuenta.getMCLIMCO() + ", '3', '" + regrabacion.getUsuario() + "', "
                                + "" + hora + ", " + fecha + ", 0,  '', 0, 0"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DEL EMBOZADO");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATO DEL EMBOZADO");
                        }
                        //SE ASOCIA LA NUEVA TARJETA A LA CUENTA SI ES PRINCIPAL
                        if (dato.getMTTIPOT().equals("1")) {
                            query = "UPDATE Gxbdbps.TMCTAAF SET MCNUMTA = '" + regrabacion.getNroTarjetaNueva() + "' WHERE MCNUMCT = " + dato.getMCNUMCT();
                            LOGGER.info("SE MODIFICA LA NUEVA TARJETA PRINCIPAL EN LA CUENTA");
                            if (!DBUtils.ejecucionSQL(query)) {
                                procesoReg = false;
                                resp = "ERROR AL PROCESAR LA SOLICITUD";
                                LOGGER.error("ERROR AL MODIFICAR LA NUEVA TARJETA PRINCIPAL EN LA CUENTA");
                            }
                        }
                        if (procesoReg) {
                            contProc += 1;
                        } else {
                            contError += 1;
                        }

                    } else if (!tarjetaN && venceN) {
                        
                        String query = "UPDATE GXBDBPS.TMTARAF SET MTFEVE2 = '" + vencimiento + "' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                        LOGGER.info("SE ACTUALIZA VENCIMIENTO DE LA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                        }

                        //SE GRABA LA AUDITORIA vencimiento nuevo
                        LOGGER.info("SE GRABA LA AUDITORIA vencimiento nuevo");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Vencimiento de los renovados", dato.getMTFEVE2() + "", vencimiento)) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA Vencimiento nuevo TMTARAF");
                        }

                        //SE AGREGA EL DATO DE COBRO DE LA TARJETA
                        query = "INSERT INTO GXBDBPS.TEMBSAF VALUES ('" + regrabacion.getNroTarjeta() + "', "
                                + "'" + fecha + "', '" + dato.getENEMISO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                + "'001', '7', '00000000', " + costoPersonalizado + ", " + ((cuotas.equalsIgnoreCase("0") && !costoPersonalizado.equalsIgnoreCase("0")) ? "1" : cuotas) + ", 'N', 'N'"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DE COBRO DE LA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            LOGGER.error("ERROR AL INGRESAR DATOS DEL COBRO");
                            procesoReg = false;
                        }
                        //SE AGREGA EL DATO DEL EMBOZADO
                        DatoDireccion direccion = getDireccion(dato.getCENUMDO(), dato.getENEMISO());
                        query = "INSERT INTO LIBDEBITO.EMBNOV0P VALUES ('" + dato.getENEMISO() + "', '001', '" + dato.getAFAFINI() + "',"
                                + "'" + dato.getBIBINES() + "', '" + regrabacion.getNroTarjeta() + "', ''," + dato.getMCNUMCT() + ",  "
                                + "'" + datoCuenta.getMCCTABC() + "', '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '" + vencimiento + "',"
                                + "'" + dato.getMTEMPRE() + "', '" + direccion.getDireccion() + "', '" + direccion.getLocalidad() + "', '" + direccion.getNroTelefono() + "', "
                                + "" + datoCuenta.getMCLIMCO() + ", '7', '" + regrabacion.getUsuario() + "', "
                                + "" + hora + ", " + fecha + ", 0,  '', 0, 0"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DEL EMBOZADO");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATO DEL EMBOZADO");
                        }
                        if (procesoReg) {
                            contProc += 1;
                        } else {
                            contError += 1;
                        }
                    } else if (!tarjetaN && !venceN && costoN) {
                        //CAMBIO DE ESTADO DE FALTA DE EMISION Y BLOQUEO POR FALTA DE ENTREGA
                        String query = "UPDATE GXBDBPS.TMTARAF SET MTESTHA = 'E', BLCODIG = '01' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                        LOGGER.info("CAMBIO DE ESTADO DE FALTA DE EMISION Y BLOQUEO POR FALTA DE ENTREGA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                        }

                        //SE GRABA LA AUDITORIA codigo bloqueo
                        LOGGER.info("SE GRABA LA AUDITORIA codigo bloqueo");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Codigo de bloqueo", dato.getBLCODIG().trim(), "01")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA codigo bloqueo TMTARAF");
                        }

                        //SE GRABA LA AUDITORIA Estado inhabilitada
                        LOGGER.info("SE GRABA LA AUDITORIA Estado inhabilitada");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Estado inhabilitada", dato.getMTESTHA(), "E")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA Estado inhabilitada TMTARAF");
                        }

                        //SE AGREGA EL DATO DE COBRO DE LA TARJETA
                        query = "INSERT INTO GXBDBPS.TEMBSAF VALUES ('" + regrabacion.getNroTarjeta() + "', "
                                + "'" + fecha + "', '" + dato.getENEMISO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                + "'001', '3', '00000000', " + costoPersonalizado + ", " + ((cuotas.equalsIgnoreCase("0") && !costoPersonalizado.trim().equalsIgnoreCase("0")) ? "1" : cuotas) + ", 'N', 'N'"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DE COBRO DE LA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATOS DEL COBRO");
                        }
                        DatoDireccion direccion = getDireccion(dato.getCENUMDO(), dato.getENEMISO());
                        query = "INSERT INTO LIBDEBITO.EMBNOV0P VALUES ('" + dato.getENEMISO() + "', '001', '" + dato.getAFAFINI() + "',"
                                + "'" + dato.getBIBINES() + "', '" + regrabacion.getNroTarjeta() + "', ''," + dato.getMCNUMCT() + ",  "
                                + "'" + datoCuenta.getMCCTABC() + "', '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '" + dato.getMTFEVEN() + "',"
                                + "'" + dato.getMTEMPRE() + "', '" + direccion.getDireccion() + "', '" + direccion.getLocalidad() + "', '" + direccion.getNroTelefono() + "', "
                                + "" + datoCuenta.getMCLIMCO() + ", '1', '" + regrabacion.getUsuario() + "', "
                                + "" + hora + ", " + fecha + ", 0,  '', 0, 0"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DEL EMBOZADO");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATO DEL EMBOZADO");
                        }
                        if (procesoReg) {
                            contProc += 1;
                        } else {
                            contError += 1;
                        }
                    } else if (!tarjetaN && !venceN && !costoN && regrabacion.getCobrarCosto().equalsIgnoreCase("N")) {
                        //CAMBIO DE ESTADO DE FALTA DE EMISION Y BLOQUEO POR FALTA DE ENTREGA
                        String query = "UPDATE GXBDBPS.TMTARAF SET MTESTHA = 'E', BLCODIG = '01' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                        LOGGER.info("CAMBIO DE ESTADO DE FALTA DE EMISION Y BLOQUEO POR FALTA DE ENTREGA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            procesoReg = false;
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                        }

                        //SE GRABA LA AUDITORIA codigo bloqueo
                        LOGGER.info("SE GRABA LA AUDITORIA codigo bloqueo");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Codigo de bloqueo", dato.getBLCODIG().trim(), "01")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA codigo bloqueo TMTARAF");
                        }

                        //SE GRABA LA AUDITORIA Estado inhabilitada
                        LOGGER.info("SE GRABA LA AUDITORIA Estado inhabilitada");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Estado inhabilitada", dato.getMTESTHA(), "E")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA ESTADO INHABILITADA TMTARAF");

                        }

                        //SE AGREGA EL DATO DE COBRO DE LA TARJETA
                        query = "INSERT INTO GXBDBPS.TEMBSAF VALUES ('" + regrabacion.getNroTarjeta() + "', "
                                + "'" + fecha + "', '" + dato.getENEMISO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                + "'001', '3', '00000000', " + costoPersonalizado + ", " + (cuotas.equalsIgnoreCase("0") ? "1" : cuotas) + ", 'N', 'N'"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DE COBRO DE LA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            LOGGER.error("ERROR AL INGRESAR DATOS DEL COBRO");
                            procesoReg = false;
                        }
                        DatoDireccion direccion = getDireccion(dato.getCENUMDO(), dato.getENEMISO());
                        query = "INSERT INTO LIBDEBITO.EMBNOV0P VALUES ('" + dato.getENEMISO() + "', '001', '" + dato.getAFAFINI() + "',"
                                + "'" + dato.getBIBINES() + "', '" + regrabacion.getNroTarjeta() + "', ''," + dato.getMCNUMCT() + ",  "
                                + "'" + datoCuenta.getMCCTABC() + "', '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '" + dato.getMTFEVEN() + "',"
                                + "'" + dato.getMTEMPRE() + "', '" + direccion.getDireccion() + "', '" + direccion.getLocalidad() + "', '" + direccion.getNroTelefono() + "', "
                                + "" + datoCuenta.getMCLIMCO() + ", '1', '" + regrabacion.getUsuario() + "', "
                                + "" + hora + ", " + fecha + ", 0,  '', 0, 0"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DE EMBOZADO");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATO DEL EMBOZADO");
                        }
                        if (procesoReg) {
                            contProc += 1;
                        } else {
                            contError += 1;
                        }
                    } else if (!tarjetaN && !venceN && !costoN && regrabacion.getCobrarCosto().equalsIgnoreCase("S")) {
                        String query = "UPDATE GXBDBPS.TMTARAF SET MTNOVED = '3', MTUSPED = '" + regrabacion.getUsuario() + "', "
                                + "MTFEPED='" + fecha + "' WHERE MTNUMTA = '" + nroTarjeta + "'";
                        LOGGER.info("SE ACTUALIZA LOS DATOS DE LA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATO DEL EMBOZADO");
                        }

                        //SE GRABA LA AUDITORIA Novedad
                        LOGGER.info("SE GRABA LA AUDITORIA Novedad");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Marca de novedades", dato.getMTNOVED(), "3")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA NOVEDAD DE TMTARAF");
                        }
                        if (procesoReg) {
                            contProc += 1;
                        } else {
                            contError += 1;
                        }
                    }
                } else if (regrabacion.getIdMotivo().equalsIgnoreCase(Regrabacion.regrabarFueraFecha)) {
                    if (regrabacion.getVencimiento() != null && !regrabacion.getVencimiento().equalsIgnoreCase("")) {//SI VIENE UNA NUEVA FECHA DE VENCIMIENTO, SE UTILIZA LA QUE VIENE Y SE ANALIZA SI CORRESPONDE RECALCULAR LA DURACION
                        String mes = regrabacion.getVencimiento().substring(4);
                        String anho = regrabacion.getVencimiento().substring(2, 4);
                        Calendar cal = Calendar.getInstance();
                        int mesActual = cal.get(Calendar.MONTH);
                        String mesActualS = (mesActual + "").length() == 2 ? (mesActual + "") : "0" + mesActual;
                        int anhoActual = cal.get(Calendar.YEAR);
                        String anhoActualS = (anhoActual + "").substring(2);
                        anhoActual = Integer.parseInt(anhoActualS);
                        int difAnho = anhoActual - Integer.parseInt(anho);
                        int difMes = mesActual - Integer.parseInt(mes);
                        if (difAnho < 1) {//SI FALTA MENOS DE UN AÑO PARA EL VENCIMIENTO
                            if (difMes > 0 && difMes <= 3) {//Y TRES MESES O MENOS
                                //SE RECALCULA LA DURACION
                                String afinidad = DBUtils.getAfinity(regrabacion.getNroTarjeta(), "021");
                                duracion = DBUtils.getDuration(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad);
                            } else {//SINO SE CALCULA LA DIFERENCIA DE MESES ENTRE EL VENCIMIENTO Y LA FECHA ACTUAL
                                duracion = Utils.getMonthDiference("01/" + mes + "/" + anho, "01/" + mesActualS + "/" + anhoActualS);
                            }
                        } else {//SINO SE CALCULA LA DIFERENCIA DE MESES ENTRE EL VENCIMIENTO Y LA FECHA ACTUAL
                            duracion = Utils.getMonthDiference("01/" + mes + "/" + anho, "01/" + mesActualS + "/" + anhoActualS);
                        }
                    } else {//SE UTILIZA LA FECHA DE VENCIMIENTO QUE YA TENIA LA TARJETA Y SE CALCULA SI ES NECESARIO RECALCULAR LA DURACION
                        LOGGER.info("VOY A VERIFICAR VENCIMIENTO");
                        String fechaVencimiento = DBUtils.getExpirationDate(regrabacion.getNroTarjeta(), "021");
                        LOGGER.info("VENCIMIENTO inicial:" + fechaVencimiento);
                        String mes = fechaVencimiento.substring(2);
                        LOGGER.info("MES:" + mes);
                        String anho = fechaVencimiento.substring(0, 2);
                        LOGGER.info("ANHO:" + anho);
                        Calendar cal = Calendar.getInstance();
                        int mesActual = cal.get(Calendar.MONTH);
                        LOGGER.info("MES ACUTAL:" + mesActual);
                        String mesActualS = (mesActual + "").length() == 2 ? (mesActual + "") : "0" + mesActual;
                        LOGGER.info("MES ACUTAL String:" + mesActualS);
                        int anhoActual = cal.get(Calendar.YEAR);
                        LOGGER.info("AÑo ACUTAL:" + anhoActual);
                        String anhoActualS = (anhoActual + "").substring(2);
                        LOGGER.info("Año ACUTAL string :" + anhoActualS);
                        anhoActual = Integer.parseInt(anhoActualS);
                        int difAnho = Integer.parseInt(anho) - anhoActual;
                        int difMes = Integer.parseInt(mes) - mesActual;
                        LOGGER.info("difAnho:" + difAnho);
                        LOGGER.info("difMes:" + difMes);
                        LOGGER.info("VOY A OBETNER LA DURACION");
                        if (difAnho < 1) {//SI FALTA MENOS DE UN AÑO PARA EL VENCIMIENTO
                            if (difMes > 0 && difMes <= 3) {//Y TRES MESES O MENOS
                                String afinidad = DBUtils.getAfinity(regrabacion.getNroTarjeta(), "021");
                                duracion = DBUtils.getDuration(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad);
                                LOGGER.info("DURACION:" + duracion);
                            } else {//SINO SE CALCULA LA DIFERENCIA DE MESES ENTRE EL VENCIMIENTO Y LA FECHA ACTUAL
                                duracion = Utils.getMonthDiference("01/" + mes + "/" + anho, "01/" + mesActualS + "/" + anhoActualS);
                                LOGGER.info("DURACION:" + duracion);
                            }
                        } else {//SINO SE CALCULA LA DIFERENCIA DE MESES ENTRE EL VENCIMIENTO Y LA FECHA ACTUAL
                            duracion = Utils.getMonthDiference("01/" + mes + "/" + anho, "01/" + mesActualS + "/" + anhoActualS);
                            LOGGER.info("DURACION:" + duracion);
                        }
                    }
                    fechaHoy.add(Calendar.MONTH, duracion);
                    String vencimiento = sdf2.format(fechaHoy.getTime());
                    LOGGER.info("VENCIMIENTO FINAL:" + vencimiento);

                    if (regrabacion.getCobrarCosto().equalsIgnoreCase("N")) { //NO SE COBRA COSTO DE REIMPRESION
                        costoPersonalizado = "0";
                        cuotas = "0";
                    } else { //SE COBRA COSTO DE REIMPRESION
                        String afinidad = DBUtils.getAfinity(regrabacion.getNroTarjeta(), "021");
                        cuotas = "" + getCuota(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad, dato.getMTTIPOT().trim());
                        if (regrabacion.getImporte() != null && !regrabacion.getImporte().equalsIgnoreCase("")) { //SE VERIFICA SI LLEGA UN IMPORTE PERSONALIZADO
                            costoPersonalizado = regrabacion.getImporte();
                        } else { //SE COBRA COSTO DE REIMPRESION DEL PARAMETRO DE LA AFINIDAD
                            costoPersonalizado = DBUtils.getCost(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad, dato.getMTTIPOT().trim()) + "";
                        }
                    }

                    if (regrabacion.getCobrarCosto().equalsIgnoreCase("S")) {
                        if ((regrabacion.getImporte() == null || regrabacion.getImporte().equalsIgnoreCase(""))) {
                            if ((regrabacion.getVencimiento() == null || regrabacion.getVencimiento().equalsIgnoreCase(""))) {  //SIN CAMBIO DE VENCIMIENTO CON COSTO NORMAL

                                //CAMBIO DE NOVEDAD SE TOMA COMO EL PROCESO NORMAL
                                String query = "UPDATE GXBDBPS.TMTARAF SET MTNOVED = '3' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                                LOGGER.info("CAMBIO DE NOVEDAD SE TOMA COMO EL PROCESO NORMAL");
                                if (!DBUtils.ejecucionSQL(query)) {
                                    resp = "ERORR AL PROCESAR LA SOLICITUD";
                                    procesoReg = false;
                                    LOGGER.error("ERROR AL GENERAR LA NOVEDAD");
                                }
                                //SE GRABA LA AUDITORIA Novedad
                                LOGGER.info("SE GRABA LA AUDITORIA Novedad");
                                if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Marca de novedades", dato.getMTNOVED(), "3")) {
                                    LOGGER.error("ERROR AL GRABAR LA AUDITORIA NOVEDAD DE TMTARAF");
                                }
                                if (procesoReg) {
                                    contProc += 1;
                                } else {
                                    contError += 1;
                                }
                            } else { //CON CAMBIO DE VENCIMIENTO CON COSTO NORMAL DEL LA ENTIDAD
                                String query = "";
                                if (regrabacion.getVencimiento() != null && !regrabacion.getVencimiento().equalsIgnoreCase("")) {
                                    query = "UPDATE GXBDBPS.TMTARAF SET MTFEVE2 = '" + vencimiento + "' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                                    LOGGER.info("SE ACTUALIZA LOS DATOS DE LA TARJETA");
                                    if (!DBUtils.ejecucionSQL(query)) {
                                        resp = "ERORR AL PROCESAR LA SOLICITUD";
                                        procesoReg = false;
                                    }

                                    //SE GRABA LA AUDITORIA vencimiento nuevo
                                    LOGGER.info("SE GRABA LA AUDITORIA vencimiento nuevo");
                                    if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Vencimiento de los renovados", dato.getMTFEVE2() + "", vencimiento)) {
                                        LOGGER.error("ERROR AL GRABAR LA AUDITORIA Vencimiento nuevo TMTARAF");
                                    }

                                }

                                //SE AGREGA EL DATO DE COBRO DE LA TARJETA
                                query = "INSERT INTO GXBDBPS.TEMBSAF VALUES ('" + regrabacion.getNroTarjeta() + "', "
                                        + "'" + fecha + "', '" + dato.getENEMISO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                        + "'001', '7', '00000000', " + costoPersonalizado + ", "
                                        + ((cuotas.trim().equalsIgnoreCase("0")) ? "1" : cuotas) + ", 'N', 'N'"
                                        + ")";
                                LOGGER.info("SE AGREGA EL DATO DE COBRO DE LA TARJETA");
                                if (!DBUtils.ejecucionSQL(query)) {
                                    resp = "ERORR AL PROCESAR LA SOLICITUD";
                                    procesoReg = false;
                                    LOGGER.error("ERROR AL INGRESAR DATOS DEL COBRO");
                                }

                                //SE AGREGA EL DATO DEL EMBOZADO
                                DatoDireccion direccion = getDireccion(dato.getCENUMDO(), dato.getENEMISO());
                                query = "INSERT INTO LIBDEBITO.EMBNOV0P VALUES ('" + dato.getENEMISO() + "', '001', '" + dato.getAFAFINI() + "',"
                                        + "'" + dato.getBIBINES() + "', '" + regrabacion.getNroTarjeta() + "', ''," + dato.getMCNUMCT() + ",  "
                                        + "'" + datoCuenta.getMCCTABC() + "', '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '" + vencimiento + "',"
                                        + "'" + dato.getMTEMPRE() + "', '" + direccion.getDireccion() + "', '" + direccion.getLocalidad() + "', '" + direccion.getNroTelefono() + "', "
                                        + "" + datoCuenta.getMCLIMCO() + ", '7', '" + regrabacion.getUsuario() + "', "
                                        + "" + hora + ", " + fecha + ", 0,  '', 0, 0"
                                        + ")";
                                LOGGER.info("SE AGREGA EL DATO DEL EMBOZADO");
                                if (!DBUtils.ejecucionSQL(query)) {
                                    resp = "ERORR AL PROCESAR LA SOLICITUD";
                                    procesoReg = false;
                                    LOGGER.error("ERROR AL INGRESAR DATO DEL EMBOZADO");
                                }
                                if (procesoReg) {
                                    contProc += 1;
                                } else {
                                    contError += 1;
                                }
                            }
                        } else { //CON CAMBIO DE VENCIMIENTO CON COSTO NORMAL DEL LA ENTIDAD O LO QUE LLEGA Y SE CALCULA
                            LOGGER.info("ingresee vence?:" + regrabacion.getVencimiento());
                            String query;
                            String novedad = "3";
                            if (regrabacion.getVencimiento() != null && !regrabacion.getVencimiento().equalsIgnoreCase("")) {
                                query = "UPDATE GXBDBPS.TMTARAF SET MTFEVE2 = '" + vencimiento + "' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                                LOGGER.info("SE ACTUALIZA LOS DATOS DE LA TARJETA");
                                if (!DBUtils.ejecucionSQL(query)) {
                                    resp = "ERORR AL PROCESAR LA SOLICITUD";
                                    procesoReg = false;
                                }
                                novedad = "7";

                                //SE GRABA LA AUDITORIA vencimiento nuevo
                                LOGGER.info("SE GRABA LA AUDITORIA vencimiento nuevo");
                                if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Vencimiento de los renovados", dato.getMTFEVE2() + "", vencimiento)) {
                                    LOGGER.error("ERROR AL GRABAR LA AUDITORIA Vencimiento nuevo TMTARAF");
                                }

                            }

                            //SE AGREGA EL DATO DE COBRO DE LA TARJETA
                            query = "INSERT INTO GXBDBPS.TEMBSAF VALUES ('" + regrabacion.getNroTarjeta() + "', "
                                    + "'" + fecha + "', '" + dato.getENEMISO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                    + "'001', '" + novedad + "', '00000000', " + costoPersonalizado + ", " + (cuotas.trim().equalsIgnoreCase("0") ? "1" : cuotas) + ", 'N', 'N'"
                                    + ")";
                            LOGGER.info("SE AGREGA EL DATO DE COBRO DE LA TARJETA");
                            if (!DBUtils.ejecucionSQL(query)) {
                                resp = "ERORR AL PROCESAR LA SOLICITUD";
                                procesoReg = false;
                                LOGGER.error("ERROR AL INGRESAR DATOS DEL COBRO");
                            }
                            //SE AGREGA EL DATO DEL EMBOZADO

                            DatoDireccion direccion = getDireccion(dato.getCENUMDO(), dato.getENEMISO());
                            query = "INSERT INTO LIBDEBITO.EMBNOV0P VALUES ('" + dato.getENEMISO() + "', '001', '" + dato.getAFAFINI() + "',"
                                    + "'" + dato.getBIBINES() + "', '" + regrabacion.getNroTarjeta() + "', ''," + dato.getMCNUMCT() + ",  "
                                    + "'" + datoCuenta.getMCCTABC() + "', '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '" + vencimiento + "',"
                                    + "'" + dato.getMTEMPRE() + "', '" + direccion.getDireccion() + "', '" + direccion.getLocalidad() + "', '" + direccion.getNroTelefono() + "', "
                                    + "" + datoCuenta.getMCLIMCO() + ", '" + novedad + "', '" + regrabacion.getUsuario() + "', "
                                    + "" + hora + ", " + fecha + ", 0,  '', 0, 0"
                                    + ")";
                            LOGGER.info("SE AGREGA EL DATO DEL EMBOZADO");
                            if (!DBUtils.ejecucionSQL(query)) {
                                resp = "ERROR AL PROCESAR LA SOLICITUD";
                                procesoReg = false;
                                LOGGER.error("ERROR AL INGRESAR DATO DEL EMBOZADO");
                            }
                            if (procesoReg) {
                                contProc += 1;
                            } else {
                                contError += 1;
                            }
                        }
                    } else { //NO SE REALIZA EL COBRO (0)
                        String query = "";
                        String novedad = "";
                        if ((regrabacion.getVencimiento() != null && !regrabacion.getVencimiento().equalsIgnoreCase(""))) { //CAMBIO DE VENCIMIENTO SIN COSTO
                            LOGGER.info("cambio de vence?:" + regrabacion.getVencimiento());
                            //SE CAMBIA EL DATO DEL VENCIMIENTO
                            query = "UPDATE GXBDBPS.TMTARAF SET MTFEVE2 = '" + vencimiento + "' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                            LOGGER.info("SE CAMBIA EL DATO DEL VENCIMIENTO");
                            if (!DBUtils.ejecucionSQL(query)) {
                                procesoReg = false;
                                resp = "ERORR AL PROCESAR LA SOLICITUD";
                            }
                            novedad = "7";

                            //SE GRABA LA AUDITORIA vencimiento nuevo
                            LOGGER.info("SE GRABA LA AUDITORIA vencimiento nuevo");
                            if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Vencimiento de los renovados", dato.getMTFEVE2() + "", vencimiento)) {
                                LOGGER.error("ERROR AL GRABAR LA AUDITORIA Vencimiento nuevo TMTARAF");
                            }

                        } else { //SIN CAMBIO DE VENCIMIENTO Y COSTO 0
                            //CAMBIO DE ESTADO DE FALTA DE EMISION Y BLOQUEO POR FALTA DE ENTREGA
                            query = "UPDATE GXBDBPS.TMTARAF SET MTESTHA = 'E', BLCODIG = '01' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                            LOGGER.info("CAMBIO DE ESTADO DE FALTA DE EMISION Y BLOQUEO POR FALTA DE ENTREGA");
                            if (!DBUtils.ejecucionSQL(query)) {
                                resp = "ERORR AL PROCESAR LA SOLICITUD";
                                procesoReg = false;
                            }
                            novedad = "3";

                            //SE GRABA LA AUDITORIA codigo bloqueo
                            LOGGER.info("SE GRABA LA AUDITORIA codigo bloqueo");
                            if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Codigo de bloqueo", dato.getBLCODIG().trim(), "01")) {
                                LOGGER.error("ERROR AL GRABAR LA AUDITORIA codigo bloqueo TMTARAF");
                            }

                            //SE GRABA LA AUDITORIA Estado inhabilitada
                            LOGGER.info("SE GRABA LA AUDITORIA Estado inhabilitada");
                            if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Estado inhabilitada", dato.getMTESTHA(), "E")) {
                                LOGGER.error("ERROR AL GRABAR LA AUDITORIA ESTADO INHABILITADA TMTARAF");

                            }

                        }

                        //SE AGREGA EL DATO DE COBRO DE LA TARJETA
                        query = "INSERT INTO GXBDBPS.TEMBSAF VALUES ('" + regrabacion.getNroTarjeta() + "', "
                                + "'" + fecha + "', '" + dato.getENEMISO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                + "'001', '" + novedad + "', '00000000', 0, 0, 'N', 'N'"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DE COBRO DE LA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATOS DEL COBRO");
                        }
                        //SE AGREGA EL DATO DEL EMBOZADO
                        DatoDireccion direccion = getDireccion(dato.getCENUMDO(), dato.getENEMISO());
                        query = "INSERT INTO LIBDEBITO.EMBNOV0P VALUES ('" + dato.getENEMISO() + "', '001', '" + dato.getAFAFINI() + "',"
                                + "'" + dato.getBIBINES() + "', '" + regrabacion.getNroTarjeta() + "', ''," + dato.getMCNUMCT() + ",  "
                                + "'" + datoCuenta.getMCCTABC() + "', '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '" + vencimiento + "',"
                                + "'" + dato.getMTEMPRE() + "', '" + direccion.getDireccion() + "', '" + direccion.getLocalidad() + "', '" + direccion.getNroTelefono() + "', "
                                + "" + datoCuenta.getMCLIMCO() + ", '" + novedad + "', '" + regrabacion.getUsuario() + "', "
                                + "" + hora + ", " + fecha + ", 0,  '', 0, 0"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DEL EMBOZADO");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATO DEL EMBOZADO");
                        }
                        if (procesoReg) {
                            contProc += 1;
                        } else {
                            contError += 1;
                        }
                    }

                } else if (regrabacion.getIdMotivo().equalsIgnoreCase(Regrabacion.regrabarOrdenBanco) || regrabacion.getIdMotivo().equalsIgnoreCase(Regrabacion.regrabarMalaGrabacion) || regrabacion.getIdMotivo().equalsIgnoreCase(Regrabacion.regrabarRotura)) {
                    if (regrabacion.getCobrarCosto().equalsIgnoreCase("N")) {
                        costoPersonalizado = "0";
                        cuotas = "1";
                    } else {
                        String afinidad = DBUtils.getAfinity(regrabacion.getNroTarjeta(), "021");
                        if (regrabacion.getImporte() != null && !regrabacion.getImporte().equalsIgnoreCase("")) { //SE VERIFICA SI LLEGA UN IMPORTE PERSONALIZADO
                            costoPersonalizado = regrabacion.getImporte();
                            cuotas = "" + getCuota(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad, dato.getMTTIPOT().trim());
                        } else {
                            costoPersonalizado = DBUtils.getCost(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad, dato.getMTTIPOT().trim()) + "";
                            cuotas = "" + getCuota(regrabacion.getNroTarjeta().substring(0, 6), "021", afinidad, dato.getMTTIPOT().trim());
                        }
                    }
                    if (regrabacion.getCobrarCosto().equalsIgnoreCase("S") && !costoPersonalizado.equalsIgnoreCase(regrabacion.getImporte())) {
                        //CAMBIO DE NOVEDAD SE TOMA COMO EL PROCESO NORMAL
                        String query = "UPDATE GXBDBPS.TMTARAF SET MTNOVED = '3' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                        LOGGER.info("CAMBIO DE NOVEDAD SE TOMA COMO EL PROCESO NORMAL");
                        if (!DBUtils.ejecucionSQL(query)) {
                            procesoReg = false;
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            LOGGER.error("ERROR AL GENERAR LA NOVEDAD");
                        }
                        //SE GRABA LA AUDITORIA Novedad
                        LOGGER.info("SE GRABA LA AUDITORIA Novedad");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Marca de novedades", dato.getMTNOVED(), "3")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA NOVEDAD DE TMTARAF");
                        }
                        if (procesoReg) {
                            contProc += 1;
                        } else {
                            contError += 1;
                        }
                    } else {

                        //CAMBIO DE ESTADO DE FALTA DE EMISION Y BLOQUEO POR FALTA DE ENTREGA
                        String query = "UPDATE GXBDBPS.TMTARAF SET MTESTHA = 'E', BLCODIG = '01' WHERE MTNUMTA = '" + regrabacion.getNroTarjeta() + "'";
                        LOGGER.info("CAMBIO DE ESTADO DE FALTA DE EMISION Y BLOQUEO POR FALTA DE ENTREGA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            procesoReg = false;
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA codigo bloqueo TMTARAF");
                        }

                        //SE GRABA LA AUDITORIA codigo bloqueo
                        LOGGER.info("SE GRABA LA AUDITORIA codigo bloqueo");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Codigo de bloqueo", dato.getBLCODIG().trim(), "01")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA codigo bloqueo TMTARAF");
                        }

                        //SE GRABA LA AUDITORIA Estado inhabilitada
                        LOGGER.info("SE GRABA LA AUDITORIA Estado inhabilitada");
                        if (!DBUtils.AuditTmtaraf(dato, datoCuenta, regrabacion.getUsuario(), fecha, hora, "Estado inhabilitada", dato.getMTESTHA(), "E")) {
                            LOGGER.error("ERROR AL GRABAR LA AUDITORIA ESTADO INHABILITADA TMTARAF");

                        }

                        //SE AGREGA EL DATO DE COBRO DE LA TARJETA
                        query = "INSERT INTO GXBDBPS.TEMBSAF VALUES ( '" + regrabacion.getNroTarjeta() + "', "
                                + "'" + fecha + "', '" + dato.getENEMISO() + "', '" + dato.getBIBINES() + "', '" + dato.getAFAFINI() + "',"
                                + "'001', '3', '00000000', " + costoPersonalizado + ", "
                                + ((cuotas.equalsIgnoreCase("0") && !costoPersonalizado.equalsIgnoreCase("0")) ? "1" : cuotas) + ", 'N', 'N'"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DE COBRO DE LA TARJETA");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERORR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATOS DEL COBRO");
                        }
                        //SE AGREGA EL DATO DEL EMBOZADO
                        String vencimiento = sdf2.format(fechaHoy.getTime());
                        DatoDireccion direccion = getDireccion(dato.getCENUMDO(), dato.getENEMISO());
                        query = "INSERT INTO LIBDEBITO.EMBNOV0P VALUES ('" + dato.getENEMISO() + "', '001', '" + dato.getAFAFINI() + "',"
                                + "'" + dato.getBIBINES() + "', '" + regrabacion.getNroTarjeta() + "', ''," + dato.getMCNUMCT() + ",  "
                                + "'" + datoCuenta.getMCCTABC() + "', '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '" + vencimiento + "',"
                                + "'" + dato.getMTEMPRE() + "', '" + direccion.getDireccion() + "', '" + direccion.getLocalidad() + "', '" + direccion.getNroTelefono() + "', "
                                + "" + datoCuenta.getMCLIMCO() + ", '1', '" + regrabacion.getUsuario() + "', "
                                + "" + hora + ", " + fecha + ", 0,  '', 0, 0"
                                + ")";
                        LOGGER.info("SE AGREGA EL DATO DEL EMBOZADO");
                        if (!DBUtils.ejecucionSQL(query)) {
                            resp = "ERROR AL PROCESAR LA SOLICITUD";
                            procesoReg = false;
                            LOGGER.error("ERROR AL INGRESAR DATO DEL EMBOZADO");
                        }
                        if (procesoReg) {
                            contProc += 1;
                        } else {
                            contError += 1;
                        }
                    }
                }
            } else {
                contError += 1;               
            }

            //datosRegrabacion();
            String insertTdmota = "INSERT INTO Gxbdbps.TDMOTA VALUES( " + secTmmota + ", " + linea + ", 0, "
                    + "" + dato.getMCNUMCT() + ", '" + dato.getMTNOPLA() + "', '" + dato.getCENUMDO() + "', '', '1', "
                    + ""+(!MensajeError.equalsIgnoreCase("")? 3 : 1 ) + " , '" + errores + "', '"+datoEntrada+"', '021')";
            LOGGER.info("SE ACTUALIZA LA TABLA TDMOTA");
            if (!DBUtils.ejecucionSQL(insertTdmota)) {
                LOGGER.error("ERROR AL ACTUALIZAR EL DETALLE TDMOTA");
            }
        }
        int cantProcesados = contError + contProc;
        String updateTmmota = "update Gxbdbps.tmmota set MOTACERR = " + contError + ", MOTASERR = " + contProc + ", MOTAPRO = " + cantProcesados + " where MOTASEC = " + secTmmota + " and MOTAENT = '021'";
        LOGGER.info("SE ACTUALIZA LA TABLA TMMOTA");
        if (!DBUtils.ejecucionSQL(updateTmmota)) {
            LOGGER.error("ERROR AL ACTUALIZAR CABECERA TMMOTA");
        }
//        LOGGER.info("ESTADO DE PROCESO:" + proceso);
        if (proceso) {
            resp = "Proceso submitido";
        } else {
            resp = "Proceso submitido con error";
        }
        return resp;
    }

    private static boolean validarDatos(Regrabacion regrabacion, String errores) {
        boolean validacion = true;
        if (regrabacion.getNroTarjeta().length() < 11 && regrabacion.getNroTarjeta().length() > 19) {
            if (errores.equals("")) {
                errores += "143";
            } else {
                errores += ";143";
            }
            validacion = false;
        }
        if (regrabacion.getUsuario() == null || regrabacion.getUsuario().equalsIgnoreCase("")) {
            if (errores.equals("")) {
                errores += "136";
            } else {
                errores += ";136";
            }
            validacion = false;
        } else if (regrabacion.getUsuario().length() > 10) {
            if (errores.equals("")) {
                errores += "136";
            } else {
                errores += ";136";
            }
            validacion = false;
        }
        if (regrabacion.getNroTarjetaNueva() != null && !regrabacion.getNroTarjetaNueva().equalsIgnoreCase("")) {
            if (regrabacion.getNroTarjetaNueva().length() < 11 && regrabacion.getNroTarjetaNueva().length() > 19) {
                if (errores.equals("")) {
                    errores += "143";
                } else {
                    errores += ";143";
                }
                validacion = false;
            }
        }
        if (regrabacion.getVencimiento() != null && !regrabacion.getVencimiento().equalsIgnoreCase("")) {
            if (regrabacion.getVencimiento().length() != 6) {
                if (errores.equals("")) {
                    errores += "179";
                } else {
                    errores += ";179";
                }
                validacion = false;
            }
        }
        MensajeError = errores;
        return validacion;
    }
}
