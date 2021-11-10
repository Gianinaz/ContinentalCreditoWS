/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.com.bepsa.managers;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JPing;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import com.ibm.as400.access.QSYSObjectPathName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;
import py.com.bepsa.clases.RespActivacion;
import py.com.bepsa.pojo.DatoTembsaf;
import py.com.bepsa.pojo.DatoTmctaaf;
import py.com.bepsa.pojo.DatoTmtaraf;
import py.com.bepsa.utils.DBUtils;
import static py.com.bepsa.utils.DBUtils.getCodAutorizacion;
import static py.com.bepsa.utils.DBUtils.getCostoActivacion;
import static py.com.bepsa.utils.DBUtils.getCuotaActivacion;
import static py.com.bepsa.utils.DBUtils.getDatosTembsaf;
import static py.com.bepsa.utils.DBUtils.getDatosTmctaaf;
import static py.com.bepsa.utils.DBUtils.getDatosTmtaraf;
import static py.com.bepsa.utils.DBUtils.getDescrMovimiento;
import static py.com.bepsa.utils.DBUtils.valiDate;
import py.com.bepsa.utils.Utils;

/**
 *
 * @author rarce
 */
public class ActivacionManager {

    private static final Logger LOGGER = Logger.getLogger(ActivacionManager.class.getName());

    public static RespActivacion procesar(String cuenta, String tarjeta, String cobraCosto, String importe, String user) {
        String fecha = Utils.obtenerFechaHora("yyyyMMdd");
        String hora = Utils.obtenerFechaHora("HHmmss");
        String cod = "";
        String msg = "";
        String costoPersonalizado = "";
        int cuotas = 0;
        Connection conn = null;
        RespActivacion respuesta = new RespActivacion();
        //VERIFICA EXISTENCIA DE LA CUENTA
        if (!cuenta.trim().isEmpty() && cuenta != null) {
            DatoTmctaaf datoCta = getDatosTmctaaf(cuenta);
            if (!datoCta.getMCNUMCT().trim().isEmpty() && datoCta.getMCNUMCT() != null) {
                LOGGER.info("INICIO DE ACTIVACION DE CUENTA Y TARJETAS");
                try {
                    String consultaTarjeta = "select mtnumta from GXBDBPS.tmtaraf where enemiso = '021' "
                            + "and mcnumct = " + datoCta.getMCNUMCT().trim() + " and afafini = '" + datoCta.getMCAFINI() + "' and mtestbl <> 'B'";
//                    LOGGER.info("Query " + consultaTarjeta);
                    conn = DBUtils.connect();
                    conn.setAutoCommit(false);
                    PreparedStatement stmt = null;
                    stmt = conn.prepareStatement(consultaTarjeta);
                    stmt.executeQuery();
                    ResultSet rs = stmt.getResultSet();
                    String tarj = "";
                    while (rs.next()) {
                        tarj = rs.getString(1).trim();
                        //OBTENGO TODOS LOS DATOS DE LA TARJETA
                        DatoTmtaraf datoTarj = getDatosTmtaraf(tarj);
                        //OFUSCAMOS LA TARJETA 
                        String tarjOfuscada = tarj.substring(0, 5) + "******" + tarj.substring(11);
                        LOGGER.info("--------------------------------------------");
                        LOGGER.info("TARJETA A ACTIVAR: " + tarjOfuscada);
                        //OBTENGO LOS DATOS DE TEMBSAF
                        DatoTembsaf datoTembsaf = getDatosTembsaf(datoTarj.getMTNUMTA(), conn);
                        //VALIDO QUE bsmcobr SEA N Y bsfeact SEA VACIO
                        if (datoTembsaf.getBSMCOBR() != null && datoTembsaf.getBSFEACT() != null) {
                            if (datoTembsaf.getBSMCOBR().equalsIgnoreCase("N") && datoTembsaf.getBSFEACT().equalsIgnoreCase("00000000")) {
                                if (datoTarj.getMTESTBL().equalsIgnoreCase("B")) {
                                    cod = "04";
                                    msg = "TARJETA BLOQUEADA";
                                    LOGGER.info(msg + " " + tarjOfuscada);
                                } else {
                                    if (datoTarj.getMTESTHA().equalsIgnoreCase("E") || datoTarj.getMTFEVE2() != 0) {
                                        //SE SETEA EL COBRO Y LAS CUOTAS
                                        if (cobraCosto.equalsIgnoreCase("N")) {
                                            costoPersonalizado = "0";
                                            cuotas = 0;
                                        } else {
                                            cuotas = getCuotaActivacion(datoTarj.getBIBINES(), datoTarj.getENEMISO(), datoTarj.getAFAFINI(), datoTarj.getMTTIPOT(), datoTembsaf.getBSNOVED(), conn);
                                            LOGGER.info("CUOTAS: " + cuotas);
                                            if (importe != null && !importe.trim().equalsIgnoreCase("")) { //SE VERIFICA SI LLEGA UN IMPORTE PERSONALIZADO
                                                costoPersonalizado = importe.trim();
                                                LOGGER.info("COSTO: " + costoPersonalizado);
                                                if (cuotas == 0) {
                                                    cuotas = 1;
                                                }
                                            } else {
                                                costoPersonalizado = "" + getCostoActivacion(datoTarj.getBIBINES(), datoTarj.getENEMISO(), datoTarj.getAFAFINI(), datoTarj.getMTTIPOT(), datoTembsaf.getBSNOVED(), conn);
                                                LOGGER.info("COSTO: " + costoPersonalizado);
                                            }
                                        }
                                        String updateSQL = "";
                                        String insertSQL = "";
                                        //Se obtiene utimo numero de la auditoria
                                        String consultaSec = "select max(atnumse) from GXBDBPS.autaraf where atemiso = '021' and atafini = '" + datoTarj.getAFAFINI() + "' and atnumta = '" + datoTarj.getMTNUMTA() + "'";
                                        long sec = DBUtils.getSecuencia(consultaSec, conn);
                                        //Se obtiene FECHA COMERCIAL
                                        String fechaCom = DBUtils.getFechaComercial();
                                        //Se actualiza el estado de tarjeta
                                        if (datoTarj.getMTTIPOT().equals("4")) {
                                            if (datoTembsaf.getBSNOVED().equals("4") || datoTembsaf.getBSNOVED().equals("7")) {
                                                updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mttipot = '1', mtfeemi = '" + fecha + "', MTFEVEN=" + datoTarj.getMTFEVE2() + ", MTFEVE2=0 where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                                insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Tipo de Tarjeta', '" + datoTarj.getMTTIPOT() + "' , '1' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Vencimiento AAMM', '" + datoTarj.getMTFEVEN() + "' , '" + datoTarj.getMTFEVE2() + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Vencimiento de los renovados', '" + datoTarj.getMTFEVE2() + "' , '0' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                            } else if (datoTembsaf.getBSNOVED().equals("2") || datoTembsaf.getBSNOVED().equals("6")) {
                                                updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mttipot = '1', mtfeemi = '" + fecha + "', BLCODIG ='00' where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                                insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Tipo de Tarjeta', '" + datoTarj.getMTTIPOT() + "' , '1' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Código de bloqueo', '" + datoTarj.getBLCODIG() + "' , '00' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                            } else {
                                                updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mttipot = '1', mtfeemi = '" + fecha + "' where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                                insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Tipo de Tarjeta', '" + datoTarj.getMTTIPOT() + "' , '1' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                            }
                                        } else {
                                            if (datoTembsaf.getBSNOVED().equals("4") || datoTembsaf.getBSNOVED().equals("7")) {
                                                updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mtfeemi = '" + fecha + "', MTFEVEN=" + datoTarj.getMTFEVE2() + ", MTFEVE2=0 where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                                insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Vencimiento AAMM', '" + datoTarj.getMTFEVEN() + "' , '" + datoTarj.getMTFEVE2() + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Vencimiento de los renovados', '" + datoTarj.getMTFEVE2() + "' , '0' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                            } else if (datoTembsaf.getBSNOVED().equals("2") || datoTembsaf.getBSNOVED().equals("3") || datoTembsaf.getBSNOVED().equals("6")) {
                                                updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mtfeemi = '" + fecha + "', BLCODIG ='00' where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                                insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Código de bloqueo', '" + datoTarj.getBLCODIG() + "' , '00' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                            } else {
                                                updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mtfeemi = '" + fecha + "' where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                                insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                        + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                        + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                            }
                                        }
                                        if (DBUtils.ejecucionSQL(updateSQL, conn)) {
                                            msg = "ACTIVADA";
                                            cod = "00";
                                            LOGGER.info("TARJETA " + msg);
                                            //Se inserta auditoria
                                            if (DBUtils.ejecucionSQL(insertSQL, conn)) {
                                                LOGGER.info("AUDITORIA TARJETA INSERTADA CORRECTAMENTE");
                                            }
                                            if ((datoTarj.getMTTIPOT().equals("1") || datoTarj.getMTTIPOT().equals("4")) && (datoTembsaf.getBSNOVED().equals("2") || datoTembsaf.getBSNOVED().equals("6"))) {
                                                String updTarjCtaSQL = "update Gxbdbps.tmctaaf set MCNUMTA = '" + datoTarj.getMTNUMTA() + "' where mcnumct = " + datoTarj.getMCNUMCT();
                                                //Se actualiza el numero de tarjeta principal en la cuenta
                                                if (DBUtils.ejecucionSQL(updTarjCtaSQL, conn)) {
                                                    LOGGER.info("CUENTA ACTUALIZADA CORRECTAMENTE");
                                                    //Se obtiene el ultimo valor de la secuencia
                                                    String conSecCtaSQL = "select max(acnumse) from Gxbdbps.auctaaf where acemiso = '" + datoTarj.getENEMISO() + "' and acafini = '" + datoTarj.getAFAFINI() + "' and acnumct = " + datoTarj.getMCNUMCT();
                                                    long secSuc = DBUtils.getSecuencia(conSecCtaSQL, conn);
                                                    //Se inserta un nuevo registro en el historico de Cuentas
                                                    String insAudCtaSQL = "insert into Gxbdbps.auctaaf values('" + fechaCom + "', '" + datoTarj.getENEMISO() + "', '" + datoTarj.getAFAFINI() + "', '" + datoTarj.getMCNUMCT() + "', '" + secSuc + "', 'M', 1,"
                                                            + " 'USERWS', '" + fecha + "', " + hora + ", 0, 'Tarjeta principal', '" + datoTarj.getMTNUTAA() + "', '" + datoTarj.getMTNUMTA() + "', 0, '" + datoTarj.getSUCODIG() + "')";
                                                    if (DBUtils.ejecucionSQL(insAudCtaSQL, conn)) {
                                                        LOGGER.info("AUDITORIA CUENTA INSERTADA CORRECTAMENTE");
                                                    }
                                                }
                                            }
                                        }

                                        if (cuotas > 1) {
                                            //SE MAPEA EL CODIGO DE NOVEDAD A CODIGO DE MOVIMIENTO
                                            String codMov = mapearCodMov(datoTembsaf.getBSNOVED());
                                            //SE OBTIENE LA DESCRIPCION DEL CODIGO DE MOVIMIENTO
                                            String descrMov = getDescrMovimiento(codMov, conn);
                                            //SE GENERA EL CODIDO DE AUTORIZACION
                                            String codAutor = getCodAutorizacion(datoTarj.getMTNUMTA(), conn);
                                            //QUERY PARA INSERTAR LA CABECERA DE LAS CUOTAS
                                            String insertCabecera = "INSERT INTO GXBDBPS.TCUOTAF VALUES('" + datoTarj.getMTNUMTA() + "','9999999','" + codAutor + "','" + descrMov + "', '" + fechaCom + "','" + fecha + "', "
                                                    + hora + ",'" + codMov + "'," + costoPersonalizado + ",0," + cuotas + "," + datoTarj.getMCNUMCT() + ",'USERWS','10.1.25415','',0,'021','" + datoTarj.getSUCODIG() + "',"
                                                    + "'" + datoTarj.getBIBINES() + "','" + datoTarj.getAFAFINI() + "','00000000',0.00,'P','')";
                                            int contadorCuota = 1;
                                            double divCosto = Double.parseDouble(costoPersonalizado) / cuotas; //SE DIVIDE EL COSTO TOTAL ENTRE LA CANTIDAD DE CUOTAS
                                            //QUERY PARA INSERTAR DETALLE DE LAS CUOTAS
                                            String insertDetalle = "INSERT INTO GXBDBPS.TCUO1AF VALUES";
                                            String fechaVenc = fechaCom;
                                            while (contadorCuota <= cuotas) {
                                                insertDetalle += "('" + datoTarj.getMTNUMTA() + "','9999999','" + codAutor + "'," + contadorCuota + "," + divCosto + ", 0,'" + fechaVenc + "','00000000'),";
                                                contadorCuota++;
                                                fechaVenc = getFechaVenc(fechaVenc);
                                            }
                                            insertDetalle = insertDetalle.substring(0, insertDetalle.length() - 1);
                                            //SE EJECUTA QUERY DE CABECERA DE CUOTAS
                                            if (DBUtils.ejecucionSQL(insertCabecera, conn)) {
                                                LOGGER.info("ARCHIVO DE CUOTAS INSERTADO CORRECTAMENTE");
                                                //SE EJECUTA QUERY DE DETALLE DE CUOTAS
                                                if (DBUtils.ejecucionSQL(insertDetalle, conn)) {
                                                    LOGGER.info("DETALLE DE CUOTAS INSERTADO CORRECTAMENTE");
                                                }
                                            }
                                        } else if (cuotas == 1) {
                                            //SE MAPEA EL CODIGO DE NOVEDAD A CODIGO DE MOVIMIENTO
                                            String codMov = mapearCodMov(datoTembsaf.getBSNOVED());
//                                        //SE OBTIENE LA DESCRIPCION DEL CODIGO DE MOVIMIENTO
//                                        String descrMov = getDescrMovimiento(codMov, conn);
//                                        //SE GENERA EL CODIDO DE AUTORIZACION
//                                        String codAutor = getCodAutorizacion(datoTarj.getMTNUMTA(), conn);
//                                        String insertTmoviaf = "INSERT INTO GXBDBPS.TMOVIAF VALUES('040211477371','"+datoTarj.getMTNUMTA()+"','"+codMov+"','"+emisor+"',"+datoTarj.getMCNUMCT()+",'"+datoTarj.getSUCODIG()+"','','"+datoTarj.getAFAFINI()+"','600','D', '20040211', "
//                                                + "'20040211','"+fecha+"',"+hora+",0.00,100000.00,'T012620511564300', '*ADEL/CON ','9999999','"+descrMov+"','','"+codAutor+"',14,'A', 6205,"
//                                                + "'*BEPSA ','*SWT   ','    ','QCOMUNICA ',' ','"+datoTarj.getBIBINES()+"','S');";
                                            if (setMovTmoviaf(datoTarj.getMTNUMTA(), codMov, costoPersonalizado, "USERWS")) {
                                                LOGGER.info("ARCHIVO CONSOLIDADO DE MOV. INSERTADO CORRECTAMENTE");
                                            }
                                        }
                                        //SE ACTUALIZA LA TABLA DE EMBOZADO
                                        String updateTembsaf = "update GXBDBPS.tembsaf set bsmcobr = 'S', bsfeact = '" + fecha + "' where bsnumta = '" + datoTarj.getMTNUMTA() + "' and bsmcobr = 'N' and bsfeact = '00000000'";
                                        if (DBUtils.ejecucionSQL(updateTembsaf, conn)) {
                                            LOGGER.info("TABLA TEMBSAF ACTUALIZADA CORRECTAMENTE");
                                        }
                                    } else {
                                        cod = "02";
                                        msg = "TARJETA YA ACTIVADA";
                                        LOGGER.info(msg);
                                    }
                                }
                            } else {
                                cod = "06";
                                msg = "TARJETA YA EMBOZADA";
                                LOGGER.info(msg);
                            }
                        } else {
                            cod = "05";
                            msg = "TARJETA NO EMBOZADA";
                            LOGGER.info(msg);
                        }
                        LOGGER.info("--------------------------------------------");
                    }
                    if (tarj.equalsIgnoreCase("") || tarj == null) {
                        msg = "LA CUENTA NO POSEE TARJETAS";
                        cod = "07";
                        LOGGER.info(msg);
                    }
                    conn.commit();
                } catch (Exception ex) {
                    msg = "ERROR AL PROCESAR LA SOLICITUD";
                    cod = "08";
                    LOGGER.info(msg);
                    try {
                        LOGGER.info("Error en " + ActivacionManager.class + ".getResultSet: " + ex);
                        conn.rollback();
                    } catch (SQLException ex1) {
                        LOGGER.info("Error en " + ActivacionManager.class + ".getResultSet: " + ex1);
                    }
                } finally {
                    try {
                        conn.commit();
                        conn.close();
                        LOGGER.info("Conexión Cerrada");
                    } catch (SQLException ex) {
                        LOGGER.info("Error en " + ActivacionManager.class + ".getResultSet: " + ex);
                    }
                }
            } else {
                cod = "03";
                msg = "CUENTA NO EXISTE";
                LOGGER.info(msg);
            }
            //VERIFICA EXISTENCIA DE LA TARJETA
        } else if (!tarjeta.trim()
                .isEmpty() && tarjeta != null) {
            //OBTENGO TODOS LOS DATOS DE LA TARJETA
            DatoTmtaraf datoTarj = getDatosTmtaraf(tarjeta);
//            DatoTmctaaf datoCta = getDatosTmctaaf(datoTarj.getMCNUMCT());
            if (!datoTarj.getMTNUMTA().trim().isEmpty() && datoTarj.getMTNUMTA() != null) {
                LOGGER.info("INICIO DE ACTIVACION DE TARJETAS");
                try {
                    conn = DBUtils.connect();
                    conn.setAutoCommit(false);
                    //OFUSCAMOS LA TARJETA 
                    String tarjOfuscada = tarjeta.substring(0, 5) + "******" + tarjeta.substring(11);
                    LOGGER.info("TARJETA A ACTIVAR: " + tarjOfuscada);
                    //OBTENGO LOS DATOS DE TEMBSAF
                    DatoTembsaf datoTembsaf = getDatosTembsaf(datoTarj.getMTNUMTA(), conn);
                    //VALIDO QUE bsmcobr SEA N Y bsfeact SEA VACIO
                    if (datoTembsaf.getBSMCOBR() != null && datoTembsaf.getBSFEACT() != null) {
                        if (datoTembsaf.getBSMCOBR().equalsIgnoreCase("N") && datoTembsaf.getBSFEACT().equalsIgnoreCase("00000000")) {
                            if (datoTarj.getMTESTBL().equalsIgnoreCase("B")) {
                                cod = "04";
                                msg = "TARJETA BLOQUEADA";
                                LOGGER.info(msg + " " + tarjOfuscada);
                            } else {
                                if (datoTarj.getMTESTHA().equalsIgnoreCase("E") || datoTarj.getMTFEVE2() != 0) {
                                    //SE SETEA EL COBRO Y LAS CUOTAS
                                    if (cobraCosto.equalsIgnoreCase("N")) {
                                        costoPersonalizado = "0";
                                        cuotas = 0;
                                    } else {
                                        cuotas = getCuotaActivacion(datoTarj.getBIBINES(), datoTarj.getENEMISO(), datoTarj.getAFAFINI(), datoTarj.getMTTIPOT(), datoTembsaf.getBSNOVED(), conn);
                                        LOGGER.info("CUOTAS: " + cuotas);
                                        if (importe != null && !importe.trim().equalsIgnoreCase("")) { //SE VERIFICA SI LLEGA UN IMPORTE PERSONALIZADO
                                            costoPersonalizado = importe.trim();
                                            LOGGER.info("COSTO: " + costoPersonalizado);
                                            if (cuotas == 0) {
                                                cuotas = 1;
                                            }
                                        } else {
                                            costoPersonalizado = "" + getCostoActivacion(datoTarj.getBIBINES(), datoTarj.getENEMISO(), datoTarj.getAFAFINI(), datoTarj.getMTTIPOT(), datoTembsaf.getBSNOVED(), conn);
                                            LOGGER.info("COSTO: " + costoPersonalizado);
                                        }
                                    }
                                    String updateSQL = "";
                                    String insertSQL = "";
                                    //Se obtiene utimo numero de la auditoria
                                    String consultaSec = "select max(atnumse) from GXBDBPS.autaraf where atemiso = '021' and atafini = '" + datoTarj.getAFAFINI() + "' and atnumta = '" + datoTarj.getMTNUMTA() + "'";
                                    long sec = DBUtils.getSecuencia(consultaSec, conn);
                                    //Se obtiene FECHA COMERCIAL
                                    String fechaCom = DBUtils.getFechaComercial();
                                    //Se actualiza el estado de tarjeta
                                    if (datoTarj.getMTTIPOT().equals("4")) {
                                        if (datoTembsaf.getBSNOVED().equals("4") || datoTembsaf.getBSNOVED().equals("7")) {
                                            updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mttipot = '1', mtfeemi = '" + fecha + "', MTFEVEN=" + datoTarj.getMTFEVE2() + ", MTFEVE2=0 where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                            insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Tipo de Tarjeta', '" + datoTarj.getMTTIPOT() + "' , '1' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Vencimiento AAMM', '" + datoTarj.getMTFEVEN() + "' , '" + datoTarj.getMTFEVE2() + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Vencimiento de los renovados', '" + datoTarj.getMTFEVE2() + "' , '0' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                        } else if (datoTembsaf.getBSNOVED().equals("2") || datoTembsaf.getBSNOVED().equals("6")) {
                                            updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mttipot = '1', mtfeemi = '" + fecha + "', BLCODIG ='00' where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                            insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Tipo de Tarjeta', '" + datoTarj.getMTTIPOT() + "' , '1' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Código de bloqueo', '" + datoTarj.getBLCODIG() + "' , '00' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                        } else {
                                            updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mttipot = '1', mtfeemi = '" + fecha + "' where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                            insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Tipo de Tarjeta', '" + datoTarj.getMTTIPOT() + "' , '1' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                        }
                                    } else {
                                        if (datoTembsaf.getBSNOVED().equals("4") || datoTembsaf.getBSNOVED().equals("7")) {
                                            updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mtfeemi = '" + fecha + "', MTFEVEN=" + datoTarj.getMTFEVE2() + ", MTFEVE2=0 where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                            insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Vencimiento AAMM', '" + datoTarj.getMTFEVEN() + "' , '" + datoTarj.getMTFEVE2() + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Vencimiento de los renovados', '" + datoTarj.getMTFEVE2() + "' , '0' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                        } else if (datoTembsaf.getBSNOVED().equals("2") || datoTembsaf.getBSNOVED().equals("3") || datoTembsaf.getBSNOVED().equals("6")) {
                                            updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mtfeemi = '" + fecha + "', BLCODIG ='00' where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                            insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Código de bloqueo', '" + datoTarj.getBLCODIG() + "' , '00' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                        } else {
                                            updateSQL = "update GXBDBPS.tmtaraf set mtestha = '', mtfeemi = '" + fecha + "' where enemiso = '021' and mtnumta = '" + datoTarj.getMTNUMTA() + "'";
                                            insertSQL = "insert into GXBDBPS.autaraf values('" + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Estado Inhabilitado', '" + datoTarj.getMTESTHA() + "' , '' , 0 , '" + datoTarj.getSUCODIG() + "'),('"
                                                    + fechaCom + "','021', '" + datoTarj.getAFAFINI() + "' , '" + datoTarj.getMTNUMTA() + "' , " + ++sec + " , 'M', 1 , '"
                                                    + user + "', '" + fecha + "', " + hora + ", 0 , 'Fecha de Entrega', '" + datoTarj.getMTFEEMI() + "' , '" + fecha + "' , 0 , '" + datoTarj.getSUCODIG() + "')";
                                        }
                                    }
                                    if (DBUtils.ejecucionSQL(updateSQL, conn)) {
                                        msg = "ACTIVADA";
                                        cod = "00";
                                        LOGGER.info("TARJETA " + msg);
                                        //Se inserta auditoria
                                        if (DBUtils.ejecucionSQL(insertSQL, conn)) {
                                            LOGGER.info("AUDITORIA TARJETA INSERTADA CORRECTAMENTE");
                                        }
                                        if ((datoTarj.getMTTIPOT().equals("1") || datoTarj.getMTTIPOT().equals("4")) && (datoTembsaf.getBSNOVED().equals("2") || datoTembsaf.getBSNOVED().equals("6"))) {
                                            String updTarjCtaSQL = "update Gxbdbps.tmctaaf set MCNUMTA = '" + datoTarj.getMTNUMTA() + "' where mcnumct = " + datoTarj.getMCNUMCT();
                                            //Se actualiza el numero de tarjeta principal en la cuenta
                                            if (DBUtils.ejecucionSQL(updTarjCtaSQL, conn)) {
                                                LOGGER.info("CUENTA ACTUALIZADA CORRECTAMENTE");
                                                //Se obtiene el ultimo valor de la secuencia
                                                String conSecCtaSQL = "select max(acnumse) from Gxbdbps.auctaaf where acemiso = '" + datoTarj.getENEMISO() + "' and acafini = '" + datoTarj.getAFAFINI() + "' and acnumct = " + datoTarj.getMCNUMCT();
                                                long secSuc = DBUtils.getSecuencia(conSecCtaSQL, conn);
                                                //Se inserta un nuevo registro en el historico de Cuentas
                                                String insAudCtaSQL = "insert into Gxbdbps.auctaaf values('" + fechaCom + "', '" + datoTarj.getENEMISO() + "', '" + datoTarj.getAFAFINI() + "', '" + datoTarj.getMCNUMCT() + "', '" + secSuc + "', 'M', 1,"
                                                        + " 'USERWS', '" + fecha + "', " + hora + ", 0, 'Tarjeta principal', '" + datoTarj.getMTNUTAA() + "', '" + datoTarj.getMTNUMTA() + "', 0, '" + datoTarj.getSUCODIG() + "')";
                                                if (DBUtils.ejecucionSQL(insAudCtaSQL, conn)) {
                                                    LOGGER.info("AUDITORIA CUENTA INSERTADA CORRECTAMENTE");
                                                }
                                            }
                                        }
                                    }

                                    if (cuotas > 1) {
                                        //SE MAPEA EL CODIGO DE NOVEDAD A CODIGO DE MOVIMIENTO
                                        String codMov = mapearCodMov(datoTembsaf.getBSNOVED());
                                        //SE OBTIENE LA DESCRIPCION DEL CODIGO DE MOVIMIENTO
                                        String descrMov = getDescrMovimiento(codMov, conn);
                                        //SE GENERA EL CODIDO DE AUTORIZACION
                                        String codAutor = getCodAutorizacion(datoTarj.getMTNUMTA(), conn);
                                        //QUERY PARA INSERTAR LA CABECERA DE LAS CUOTAS
                                        String insertCabecera = "INSERT INTO GXBDBPS.TCUOTAF VALUES('" + datoTarj.getMTNUMTA() + "','9999999','" + codAutor + "','" + descrMov + "', '" + fechaCom + "','" + fecha + "', "
                                                + hora + ",'" + codMov + "'," + costoPersonalizado + ",0," + cuotas + "," + datoTarj.getMCNUMCT() + ",'USERWS','10.1.25415','',0,'021','" + datoTarj.getSUCODIG() + "',"
                                                + "'" + datoTarj.getBIBINES() + "','" + datoTarj.getAFAFINI() + "','00000000',0.00,'P','')";
                                        int contadorCuota = 1;
                                        double divCosto = Double.parseDouble(costoPersonalizado) / cuotas; //SE DIVIDE EL COSTO TOTAL ENTRE LA CANTIDAD DE CUOTAS
                                        //QUERY PARA INSERTAR DETALLE DE LAS CUOTAS
                                        String insertDetalle = "INSERT INTO GXBDBPS.TCUO1AF VALUES";
                                        String fechaVenc = fechaCom;
                                        while (contadorCuota <= cuotas) {
                                            insertDetalle += "('" + datoTarj.getMTNUMTA() + "','9999999','" + codAutor + "'," + contadorCuota + "," + divCosto + ", 0,'" + fechaVenc + "','00000000'),";
                                            contadorCuota++;
                                            fechaVenc = getFechaVenc(fechaVenc);
                                        }
                                        insertDetalle = insertDetalle.substring(0, insertDetalle.length() - 1);
                                        //SE EJECUTA QUERY DE CABECERA DE CUOTAS
                                        if (DBUtils.ejecucionSQL(insertCabecera, conn)) {
                                            LOGGER.info("ARCHIVO DE CUOTAS INSERTADO CORRECTAMENTE");
                                            //SE EJECUTA QUERY DE DETALLE DE CUOTAS
                                            if (DBUtils.ejecucionSQL(insertDetalle, conn)) {
                                                LOGGER.info("DETALLE DE CUOTAS INSERTADO CORRECTAMENTE");
                                            }
                                        }
                                    } else if (cuotas == 1) {
                                        //SE MAPEA EL CODIGO DE NOVEDAD A CODIGO DE MOVIMIENTO
                                        String codMov = mapearCodMov(datoTembsaf.getBSNOVED());
//                                        //SE OBTIENE LA DESCRIPCION DEL CODIGO DE MOVIMIENTO
//                                        String descrMov = getDescrMovimiento(codMov, conn);
//                                        //SE GENERA EL CODIDO DE AUTORIZACION
//                                        String codAutor = getCodAutorizacion(datoTarj.getMTNUMTA(), conn);
//                                        String insertTmoviaf = "INSERT INTO GXBDBPS.TMOVIAF VALUES('040211477371','"+datoTarj.getMTNUMTA()+"','"+codMov+"','"+emisor+"',"+datoTarj.getMCNUMCT()+",'"+datoTarj.getSUCODIG()+"','','"+datoTarj.getAFAFINI()+"','600','D', '20040211', "
//                                                + "'20040211','"+fecha+"',"+hora+",0.00,100000.00,'T012620511564300', '*ADEL/CON ','9999999','"+descrMov+"','','"+codAutor+"',14,'A', 6205,"
//                                                + "'*BEPSA ','*SWT   ','    ','QCOMUNICA ',' ','"+datoTarj.getBIBINES()+"','S');";
                                        if (setMovTmoviaf(datoTarj.getMTNUMTA(), codMov, costoPersonalizado, "USERWS")) {
                                            LOGGER.info("ARCHIVO CONSOLIDADO DE MOV. INSERTADO CORRECTAMENTE");
                                        }
                                    }
                                    //SE ACTUALIZA LA TABLA DE EMBOZADO
                                    String updateTembsaf = "update GXBDBPS.tembsaf set bsmcobr = 'S', bsfeact = '" + fecha + "' where bsnumta = '" + datoTarj.getMTNUMTA() + "' and bsmcobr = 'N' and bsfeact = '00000000'";
                                    if (DBUtils.ejecucionSQL(updateTembsaf, conn)) {
                                        LOGGER.info("TABLA TEMBSAF ACTUALIZADA CORRECTAMENTE");
                                    }
                                } else {
                                    cod = "02";
                                    msg = "TARJETA YA ACTIVADA";
                                    LOGGER.info(msg);
                                }
                            }
                        } else {
                            cod = "06";
                            msg = "TARJETA YA EMBOZADA";
                            LOGGER.info(msg);
                        }
                    } else {
                        cod = "05";
                        msg = "TARJETA NO EMBOZADA";
                        LOGGER.info(msg);
                    }
                    conn.commit();
                } catch (Exception ex) {
                    msg = "ERROR AL PROCESAR LA SOLICITUD";
                    cod = "08";
                    LOGGER.info(msg);
                    try {
                        LOGGER.info("Error en " + ActivacionManager.class + ".getResultSet: " + ex);
                        conn.rollback();
                    } catch (SQLException ex1) {
                        LOGGER.info("Error en " + ActivacionManager.class + ".getResultSet: " + ex1);
                    }
                } finally {
                    try {
                        conn.commit();
                        conn.close();
                        LOGGER.info("Conexión Cerrada");
                    } catch (SQLException ex) {
                        LOGGER.info("Error en " + ActivacionManager.class + ".getResultSet: " + ex);
                    }
                }
            } else {
                msg = "TARJETA NO EXISTE";
                cod = "01";
                LOGGER.info(msg);
            }
        }

        respuesta.setCodResp(cod);

        respuesta.setMsgResp(msg);
        return respuesta;
    }

    private static String mapearCodMov(String valor) {
        String codigo = "";
        if (valor.equals("1")) {
            codigo = "050";
        } else if (valor.equals("4") || valor.equals("6") || valor.equals("7")) {
            codigo = "070";
        } else if (valor.equals("2") || valor.equals("3")) {
            codigo = "060";
        } else if (valor.equals("5")) {
            codigo = "080";
        }
        return codigo;
    }

    private static String getFechaVenc(String fecha) {
        String newFecha = "";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date time;
        try {
            time = dateFormat.parse(fecha);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(time); //tuFechaBase es un Date;
            calendar.add(Calendar.MONTH, 1); //LE SUMAMOS UN MES A LA FECHA RECIBIDA
            Date fechaSalida = calendar.getTime();
            newFecha = dateFormat.format(fechaSalida);
            System.out.println("Fecha mas un mes: " + newFecha);
            if (valiDate(newFecha)) {
                calendar.add(Calendar.DATE, 1); //LE SUMAMOS UN DIA AL MES GENERADO
                fechaSalida = calendar.getTime();
                newFecha = dateFormat.format(fechaSalida);
                System.out.println("Fecha mas un dia: " + newFecha);
                if (valiDate(newFecha)) {
                    calendar.add(Calendar.DATE, 1); //LE SUMAMOS UN DIA AL MES GENERADO
                    fechaSalida = calendar.getTime();
                    newFecha = dateFormat.format(fechaSalida);
                    System.out.println("Fecha mas un dia: " + newFecha);

                }
            }
        } catch (ParseException ex) {
            LOGGER.info("Error en " + ActivacionManager.class
                    + ".getFechaVenc: " + ex);
        }
        return newFecha;
    }

    private static boolean setMovTmoviaf(String tarjeta, String movimiento, String importe, String user) {
        boolean procesado = false;
        String respuestaAS = "";
        String novedad = "*DEC/CTA";
        tarjeta = String.format("%1$-16s", tarjeta);
        if (importe.contains(".") || importe.contains(",")) {
            importe = importe.replace(".", "");
            importe = String.format("%1$-15s", importe);
        } else {
            importe = importe + "00";
            importe = String.format("%1$-15s", importe);
        }
        novedad = String.format("%1$-10s", novedad);
        //String cupon = transaccion.substring(7);
        user = String.format("%1$-10s", user).toUpperCase();
        try {
            //Llamamos al programa RPG que inserta el pago/reversa en tmoviaf
            AS400 localAS400 = new AS400(Utils.server, Utils.usrAS400, Utils.passAS400);
            QSYSObjectPathName localQSYSObjectPathName = new QSYSObjectPathName(Utils.libAS400, Utils.progAS400, "PGM");
            ProgramCall localProgramCall = new ProgramCall(localAS400);
            ProgramParameter[] arrayOfProgramParameter = new ProgramParameter[10];
            //LOGGER.info("Antes de correr");
            AS400Text textEnvio = null;
            AS400Text textEnvio2 = null;
            AS400Text textEnvio3 = null;
            AS400Text textEnvio4 = null;
            AS400Text textEnvio5 = null;
            AS400Text textEnvio6 = null;
            AS400Text textEnvio7 = null;
            AS400Text textEnvio8 = null;
            AS400Text textEnvio9 = null;
            AS400Text textEnvio10 = null;

            byte[] textEnvioByte = null;
            byte[] textEnvioByte2 = null;
            byte[] textEnvioByte3 = null;
            byte[] textEnvioByte4 = null;
            byte[] textEnvioByte5 = null;
            byte[] textEnvioByte6 = null;
            byte[] textEnvioByte7 = null;
            byte[] textEnvioByte8 = null;
            byte[] textEnvioByte9 = null;
            byte[] textEnvioByte10 = null;

            textEnvio = new AS400Text(128, localAS400);
            textEnvio2 = new AS400Text(128, localAS400);
            textEnvio3 = new AS400Text(128, localAS400);
            textEnvio4 = new AS400Text(128, localAS400);
            textEnvio5 = new AS400Text(128, localAS400);
            textEnvio6 = new AS400Text(128, localAS400);
            textEnvio7 = new AS400Text(128, localAS400);
            textEnvio8 = new AS400Text(128, localAS400);
            textEnvio9 = new AS400Text(128, localAS400);
            textEnvio10 = new AS400Text(128, localAS400);

//                textEnvioByte = textEnvio.toBytes(pan);
            textEnvioByte = textEnvio.toBytes(tarjeta);
            arrayOfProgramParameter[0] = new ProgramParameter(textEnvioByte, 16);
            textEnvioByte2 = textEnvio2.toBytes(movimiento);
            arrayOfProgramParameter[1] = new ProgramParameter(textEnvioByte2, 3);
            textEnvioByte3 = textEnvio3.toBytes(importe);
            arrayOfProgramParameter[2] = new ProgramParameter(textEnvioByte3, 15);
            textEnvioByte4 = textEnvio4.toBytes(" ");
            arrayOfProgramParameter[3] = new ProgramParameter(textEnvioByte4, 2);
            textEnvioByte5 = textEnvio5.toBytes(" ");
            arrayOfProgramParameter[4] = new ProgramParameter(textEnvioByte5, 2);
            textEnvioByte6 = textEnvio6.toBytes(" "); //pedido
            arrayOfProgramParameter[5] = new ProgramParameter(textEnvioByte6, 6);
            textEnvioByte7 = textEnvio7.toBytes(novedad);
            arrayOfProgramParameter[6] = new ProgramParameter(textEnvioByte7, 10);
            textEnvioByte8 = textEnvio8.toBytes(""); //cupon);
            arrayOfProgramParameter[7] = new ProgramParameter(textEnvioByte8, 8);
            textEnvioByte9 = textEnvio9.toBytes(user);
            arrayOfProgramParameter[8] = new ProgramParameter(textEnvioByte9, 10);
            textEnvioByte10 = textEnvio10.toBytes(" ");
            arrayOfProgramParameter[9] = new ProgramParameter(textEnvioByte10, 50);

            //LOGGER.info("Despues de seteo");
            localProgramCall.setProgram(localQSYSObjectPathName.getPath(), arrayOfProgramParameter);
            AS400JPing localAS400JPing = new AS400JPing(Utils.server, 2, false);
            localAS400JPing.setTimeout(Utils.timeOutAS);
            boolean ping = localAS400JPing.ping();
            LOGGER.info("Ping a AS400:" + ping);
            if (ping) {
                int timeOutPgm = Integer.parseInt("30");
                //localProgramCall.setTimeOut(timeOutPgm);
                AS400Message[] localObject;
                LOGGER.info("Ejecutando pedido");
                try {
                    if (localProgramCall.run() != true) {
                        localObject = localProgramCall.getMessageList();
                        LOGGER.info("Mensajes de la AS:");
                        for (int i = 0; i < localObject.length; i++) {
                            LOGGER.info(localObject[i].getText());
                        }
                        procesado = false;
                    } else {
                        byte[] arrayOfByte11 = arrayOfProgramParameter[9].getOutputData();
                        AS400Text localAS400Text11 = new AS400Text(arrayOfByte11.length, localAS400);
                        respuestaAS = (String) localAS400Text11.toObject(arrayOfByte11);

//                            byte[] arrayOfByte5 = arrayOfProgramParameter[3].getOutputData();
//                            AS400Text localAS400Text5 = new AS400Text(arrayOfByte5.length, localAS400);
//                            codigo = (String) localAS400Text5.toObject(arrayOfByte5);
                        if (respuestaAS.trim().isEmpty()) {
                            procesado = true;
                        } else {
                            procesado = false;
                        }

                    }
                    localAS400.disconnectService(2);
                } catch (ErrorCompletingRequestException | InterruptedException | AS400SecurityException | ObjectDoesNotExistException | NumberFormatException e) {
                    LOGGER.error("Error en procesar(): " + e.getMessage());
                    e.printStackTrace();
                    localAS400.disconnectService(2);
                    procesado = false;
                }
            } else {
                LOGGER.info("Timeout de 20 segundos al intentar conectarse a la AS");
                procesado = false;
            }
        } catch (Exception localException) {
            LOGGER.error("Error en procesar(): " + localException.getMessage());
            localException.printStackTrace();
            procesado = false;
        }
        return procesado;
    }
}
