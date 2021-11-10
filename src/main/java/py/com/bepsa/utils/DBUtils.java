/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.com.bepsa.utils;

import static java.lang.Math.round;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.apache.log4j.Logger;
import py.com.bepsa.clases.DatoTmoviaf;
import py.com.bepsa.clases.LineasDetalle;
import static py.com.bepsa.managers.RegrabacionManager.proceso;
import py.com.bepsa.pojo.DatoCliente;
import py.com.bepsa.pojo.DatoDireccion;
import py.com.bepsa.pojo.DatoMoneda;
import py.com.bepsa.pojo.DatoTembsaf;
import py.com.bepsa.pojo.DatoTmctaaf;
import py.com.bepsa.pojo.DatoTmtaraf;
import static py.com.bepsa.utils.Utils.aumentarHora;
import static py.com.bepsa.utils.Utils.getFechaAntesDespues;
import static py.com.bepsa.utils.Utils.obtenerFechaHora;

/**
 *
 * @author rarce
 */
public class DBUtils {

    private static final Logger LOGGER = Logger.getLogger(DBUtils.class);

    @SuppressWarnings("finally")
    public static long getSecuencia(String query, Connection conn) {
        long sec = 1;
        try {
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            String ultSec = "";
            while (rs.next()) {
                ultSec = rs.getString(1);
            }
            stmt.close();
            if (ultSec != null) {
                if (!ultSec.equals("")) {
                    sec = Long.parseLong(ultSec);
                    sec++;
                }
            }
        } catch (SQLException ex1) {
            LOGGER.info("Error en " + DBUtils.class + ".valiDate: " + ex1);
        }
        return sec;
    }

    @SuppressWarnings("finally")
    public static Boolean ejecucionSQL(String query, Connection conn) {
        boolean ejecucion = false;
        try {
            Statement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            stmt = conn.createStatement();
            stmt.executeUpdate(query);
            stmt.close();
            ejecucion = true;
        } catch (Exception ex) {
//            try {
            LOGGER.info("Error en " + DBUtils.class + ".ejecucionSQL: " + ex);
            ejecucion = false;
////                conn.rollback();
//
//            } catch (SQLException ex1) {
//                ejecucion = false;
//                LOGGER.info("Error en " + DBUtils.class + ".valiDate: " + ex1);
//            }
        }
        return ejecucion;
    }

    public static Connection connect() throws Exception {
        Connection conn;
        Properties props;

        if (Utils.usrAs == null || Utils.usrAs.equalsIgnoreCase("")) {
            Utils.obtenerPropiedades();
        }

        String DRIVER = Utils.driver;
        String URL = Utils.url + Utils.defaultSchema + ";naming=system;errors=full";
        conn = null;

        //Connect to iSeries 
        Class.forName(DRIVER);
        String decryptedUsr = Utils.usrAs;
        String decryptedPass = Utils.passAs;
        conn = DriverManager.getConnection(URL, decryptedUsr, decryptedPass);
//        LOGGER.info("CONEXION EXITOSA");
        return conn;

    }

    @SuppressWarnings("finally")
    public static Boolean valiDate(String fecha) {
        boolean esFeriado = false;
        Connection conn = null;

        try {
            conn = DBUtils.connect();

            PreparedStatement stmt = null;

            String query = "SELECT COUNT(*) AS ENCONTRADOS FROM GXBDBPS.GFERIAF F "
                    + "WHERE F.FEFERIA = ?  ";
            stmt = /*this.*/ conn.prepareStatement(query);

            stmt.setString(1, fecha);//CI

            ResultSet affectedRows = stmt.executeQuery();
            int affected = 0;
            while (affectedRows.next()) {
                affected = affectedRows.getInt(1);
            }
            stmt.close(); //Ricardo Arce 07052020
            if (affected > 0) {
                esFeriado = true;
            } else {
                esFeriado = false;
            }
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".valiDate: " + ex);
                esFeriado = false;
                conn.rollback();

            } catch (SQLException ex1) {
                esFeriado = false;
                LOGGER.info("Error en " + DBUtils.class + ".valiDate: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada");
                return esFeriado;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".valiDate: " + ex);
            }
        }
        return esFeriado;
    }

    @SuppressWarnings("finally")
    public static Boolean validatePendingTransfers() {
        boolean hayPendientes = false;
        Connection conn = null;

        try {
            conn = DBUtils.connect();

            PreparedStatement stmt = null;

            String query = "SELECT COUNT(*) AS RESULTADOS from LIBDEBITO.EMBNOV0P WHERE CRENTI LIKE '021'";
            stmt = /*this.*/ conn.prepareStatement(query);

            ResultSet affectedRows = stmt.executeQuery();
            int affected = 0;
            while (affectedRows.next()) {
                affected = affectedRows.getInt(1);
            }
            stmt.close(); //Ricardo Arce 07052020
            if (affected > 0) {
                hayPendientes = true;
            } else {
                hayPendientes = false;
            }
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".validatePendingTransfers: " + ex);
                hayPendientes = false;
                conn.rollback();

            } catch (SQLException ex1) {
                hayPendientes = false;
                LOGGER.info("Error en " + DBUtils.class + ".validatePendingTransfers: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
                return hayPendientes;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".validatePendingTransfers: " + ex);
            }
        }
        return hayPendientes;
    }

    //
    @SuppressWarnings("finally")
    public static String getExpirationDate(String nroTarjeta, String emisor) {
        String fechaVencimiento = "";
        Connection conn = null;

        try {
            conn = DBUtils.connect();

            PreparedStatement stmt = null;

            String query = "SELECT MTFEVEN FROM GXBDBPS.TMTARAF WHERE ENEMISO LIKE '" + emisor + "' AND MTNUMTA LIKE '" + nroTarjeta + "'";
            stmt = /*this.*/ conn.prepareStatement(query);

            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                fechaVencimiento = affectedRows.getString(1);
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getExpirationDate: " + ex);
                fechaVencimiento = "";
                conn.rollback();

            } catch (SQLException ex1) {
                fechaVencimiento = "";
                LOGGER.info("Error en " + DBUtils.class + ".getExpirationDate: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
                return fechaVencimiento;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getExpirationDate: " + ex);
            }
        }
        return fechaVencimiento;
    }

    @SuppressWarnings("finally")
    public static String getAfinity(String nroTarjeta, String emisor) {
        String afinidad = "";
        Connection conn = null;

        try {
            conn = DBUtils.connect();

            PreparedStatement stmt = null;

            String query = "SELECT AFAFINI FROM GXBDBPS.TMTARAF WHERE ENEMISO LIKE '" + emisor + "' AND MTNUMTA LIKE '" + nroTarjeta + "'";
            stmt = /*this.*/ conn.prepareStatement(query);

            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                afinidad = affectedRows.getString(1);
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getAfinity: " + ex);
                afinidad = "";
                conn.rollback();

            } catch (SQLException ex1) {
                afinidad = "";
                LOGGER.info("Error en " + DBUtils.class + ".getAfinity: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
                return afinidad;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getAfinity: " + ex);
            }
        }
        return afinidad;
    }

    @SuppressWarnings("finally")
    public static int getDuration(String bin, String emisor, String afinidad) {
        int duracion = 0;
        Connection conn = null;

        try {
            conn = DBUtils.connect();

            PreparedStatement stmt = null;

            String query = "SELECT AFDURAC FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "'";
            stmt = /*this.*/ conn.prepareStatement(query);

            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                duracion = affectedRows.getInt(1);
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getDuration: " + ex);
                duracion = 0;
                conn.rollback();

            } catch (SQLException ex1) {
                duracion = 0;
                LOGGER.info("Error en " + DBUtils.class + ".getDuration: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
                return duracion;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getDuration: " + ex);
            }
        }
        return duracion;
    }

    @SuppressWarnings("finally")
    public static int getCost(String bin, String emisor, String afinidad, String tipoTarjeta) {
        int costo = 0;
        Connection conn = null;

        try {
            conn = DBUtils.connect();

            PreparedStatement stmt = null;
            String campo = "";
            if (tipoTarjeta.equalsIgnoreCase("1")) {
                campo = "AFCORGP";
            } else {
                campo = "AFCORGA";
            }

            String query = "SELECT " + campo + " FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "'";
            stmt = /*this.*/ conn.prepareStatement(query);

            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                costo = affectedRows.getInt(1);
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getCost: " + ex);
                costo = 0;
                conn.rollback();

            } catch (SQLException ex1) {
                costo = 0;
                LOGGER.info("Error en " + DBUtils.class + ".getDuration: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
                return costo;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getDuration: " + ex);
            }
        }
        return costo;
    }

    @SuppressWarnings("finally")
    public static int getCostActivacion(String bin, String emisor, String afinidad, String tipoTarjeta) {
        int costo = 0;
        Connection conn = null;
        String query = "";
        try {
            if (tipoTarjeta.equals("1") || tipoTarjeta.equals("4")) {
                query = "SELECT AFCOEMP FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "'";
            } else {
                query = "SELECT AFCOEMA FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "'";
            }
            conn = DBUtils.connect();
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();
            while (affectedRows.next()) {
                costo = affectedRows.getInt(1);
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getCostActivacion: " + ex);
                costo = 0;
                conn.rollback();

            } catch (SQLException ex1) {
                costo = 0;
                LOGGER.info("Error en " + DBUtils.class + ".getCostActivacion: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getCostActivacion: " + ex);
            }
        }
        return costo;
    }

    @SuppressWarnings("finally")
    public static int getCuota(String bin, String emisor, String afinidad, String tipoTarjeta) {
        int costo = 0;
        Connection conn = null;

        try {
            conn = DBUtils.connect();

            PreparedStatement stmt = null;
            String campo = "";
            if (tipoTarjeta.equalsIgnoreCase("1")) {
                campo = "AFCURGP";
            } else {
                campo = "AFCURGA";
            }

            String query = "SELECT " + campo + " FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "'";
            stmt = /*this.*/ conn.prepareStatement(query);

            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                costo = affectedRows.getInt(1);
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getCost: " + ex);
                costo = 0;
                conn.rollback();

            } catch (SQLException ex1) {
                costo = 0;
                LOGGER.info("Error en " + DBUtils.class + ".getDuration: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
                return costo;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getDuration: " + ex);
            }
        }
        return costo;
    }

    @SuppressWarnings("finally")
    public static int getCuotaActivacion(String bin, String emisor, String afinidad, String tipoTarjeta) {
        int cuota = 0;
        Connection conn = null;
        String query = "";
        try {
            if (tipoTarjeta.equals("1") || tipoTarjeta.equals("4")) {
                query = "SELECT AFCUEMP FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "'";
            } else {
                query = "SELECT AFCUEMA FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "'";
            }
            conn = DBUtils.connect();
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();
            while (affectedRows.next()) {
                cuota = affectedRows.getInt(1);
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getCuotaActivacion: " + ex);
                cuota = 0;
                conn.rollback();

            } catch (SQLException ex1) {
                cuota = 0;
                LOGGER.info("Error en " + DBUtils.class + ".getCuotaActivacion: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getCuotaActivacion: " + ex);
            }
        }
        return cuota;
    }

    @SuppressWarnings("finally")
    public static Boolean validatePendingCut(String in) {
        boolean hayPendientes = false;
        Connection conn = null;

        try {
            conn = DBUtils.connect();

            PreparedStatement stmt = null;

            String query = "SELECT COUNT(*) FROM GXBDBPS.TMTARAF WHERE ENEMISO LIKE '021' AND MTNOVED <> '' AND MTNUMTA IN " + in;
            stmt = /*this.*/ conn.prepareStatement(query);

            ResultSet affectedRows = stmt.executeQuery();
            int affected = 0;
            while (affectedRows.next()) {
                affected = affectedRows.getInt(1);
            }
            stmt.close(); //Ricardo Arce 07052020
            if (affected > 0) {
                hayPendientes = true;
            } else {
                hayPendientes = false;
            }
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".validatePendingCut: " + ex);
                hayPendientes = false;
                conn.rollback();

            } catch (SQLException ex1) {
                hayPendientes = false;
                LOGGER.info("Error en " + DBUtils.class + ".validatePendingCut: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada");
                return hayPendientes;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".validatePendingCut: " + ex);
            }
        }
        return hayPendientes;
    }

    @SuppressWarnings("finally")
    public static DatoTmtaraf getDatosTmtaraf(String nroTarjeta) {
        DatoTmtaraf dato = new DatoTmtaraf();
        Connection conn = null;
        String query = "SELECT * FROM GXBDBPS.TMTARAF WHERE MTNUMTA = '" + nroTarjeta + "'";
//        String query = "SELECT * FROM GXBDBPS.TMTARAF WHERE MTNUMTA = '" + nroTarjeta + "'";
        try {
            conn = DBUtils.connect();
//            LOGGER.info("DatoTmtaraf:" + query);
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                dato.setMTNUMTA(affectedRows.getString(1).trim());
                dato.setMTULTSC(affectedRows.getInt(2));
                dato.setBLCODIG(affectedRows.getString(3).trim());
                dato.setMTSTATS(affectedRows.getString(4).trim());
                dato.setENEMISO(affectedRows.getString(5).trim());
                dato.setSUCODIG(affectedRows.getString(6).trim());
                dato.setCETIPOD(affectedRows.getString(7).trim());
                dato.setCENUMDO(affectedRows.getString(8).trim());
                dato.setBIBINES(affectedRows.getString(9).trim());
                dato.setAFAFINI(affectedRows.getString(10).trim());
                dato.setMCNUMCT(affectedRows.getString(11).trim());
                dato.setMTTIPOT(affectedRows.getString(12).trim());
                dato.setMTCLVIP(affectedRows.getString(13).trim());
                dato.setMTNOPLA(affectedRows.getString(14).trim());
                dato.setMTEMPRE(affectedRows.getString(15).trim());
                dato.setMTFECRE(affectedRows.getString(16).trim());
                dato.setMTFEEMI(affectedRows.getString(17).trim());
                dato.setMTFEBAJ(affectedRows.getString(18).trim());
                dato.setMTFEVEN(affectedRows.getInt(19));
                dato.setMTFEVE2(affectedRows.getInt(20));
                dato.setMTMBOLE(affectedRows.getString(21).trim());
                dato.setMTMRECU(affectedRows.getString(22).trim());
                dato.setMTFEREC(affectedRows.getString(23).trim());
                dato.setMTNOVED(affectedRows.getString(24).trim());
                dato.setMTCODS2(affectedRows.getString(25).trim());
                dato.setMTNUTAA(affectedRows.getString(26).trim());
                dato.setMTRETEN(affectedRows.getString(27).trim());
                dato.setMTRENOV(affectedRows.getString(28).trim());
                dato.setMTCOBRA(affectedRows.getString(29).trim());
                dato.setMTFEPED(affectedRows.getString(30).trim());
                dato.setMTUSPED(affectedRows.getString(31).trim());
                dato.setMTUSEMI(affectedRows.getString(32).trim());
                dato.setMTESTVE(affectedRows.getString(33).trim());
                dato.setMTESTEX(affectedRows.getString(34).trim());
                dato.setMTESTBL(affectedRows.getString(35).trim());
                dato.setMTESTMO(affectedRows.getString(36).trim());
                dato.setMTESTHA(affectedRows.getString(37).trim());
                dato.setMTFEBLQ(affectedRows.getString(38).trim());
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosTmtaraf: " + ex);

                conn.rollback();

            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosTmtaraf: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("CONEXION CERRADA"); //Ricardo Arce 042020
                return dato;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosTmtaraf: " + ex);
            }
        }
        return dato;
    }

    @SuppressWarnings("finally")
    public static DatoDireccion getDireccion(String nroDocumento, String enemiso) {
        DatoDireccion direccion = new DatoDireccion();
        Connection conn = null;
        boolean direN = false;
        boolean telfN = false;
        boolean locaN = false;
        try {
            conn = DBUtils.connect();

            for (int i = 0; i <= 3; i++) {
                String query = "SELECT CEDIREC,CETELEF,CELOCAL FROM GXBDBPS.GDIREAF WHERE CENUMDO LIKE '" + nroDocumento + "' AND ENEMISO = '" + enemiso + "' AND CESECUE =" + i;
                PreparedStatement stmt = null;
                stmt = /*this.*/ conn.prepareStatement(query);
                ResultSet affectedRows = stmt.executeQuery();
                while (affectedRows.next()) {
                    if (!affectedRows.getString(1).trim().equals("") && direN == false) {
                        direccion.setDireccion(affectedRows.getString(1));
                        direN = true;
                    }
                    if (!affectedRows.getString(2).trim().equals("") && telfN == false) {
                        direccion.setNroTelefono(affectedRows.getString(2));
                        telfN = true;
                    }
                    if (!affectedRows.getString(3).trim().equals("") && locaN == false) {
                        direccion.setLocalidad(affectedRows.getString(3));
                        locaN = true;
                    }
                }
                stmt.close();
            }
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getDireccion: " + ex);

                conn.rollback();

            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getDireccion: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
                return direccion;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getDireccion: " + ex);
            }
        }
        return direccion;
    }

    @SuppressWarnings("finally")
    public static DatoTmctaaf getDatosTmctaaf(String nroCuenta) {
        DatoTmctaaf dato = new DatoTmctaaf();
        Connection conn = null;
        String query = "SELECT * FROM GXBDBPS.TMCTAAF WHERE MCNUMCT=" + nroCuenta + "";
//        String query = "SELECT * FROM GXBDBPS.TMCTAAF WHERE MCNUMCT=" + nroCuenta + "";
        try {
            conn = DBUtils.connect();
//            LOGGER.info("Query: " + query);
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                dato.setMCNUMCT(affectedRows.getString(1).trim());
//                dato.setMCSTATS(affectedRows.getString(2).trim());
//                dato.setMCTIPCB(affectedRows.getString(3).trim());
                dato.setMOCODIG(affectedRows.getString(4).trim());
                dato.setMCLIMCO(affectedRows.getLong(5));
                dato.setMCLIMCU(affectedRows.getLong(6));
//                dato.setMCSALUC(affectedRows.getLong(7));
                dato.setMCSAFAC(affectedRows.getLong(8));
                dato.setMCSFNVE(affectedRows.getLong(9));
                dato.setMCSALFI(affectedRows.getLong(10));
                dato.setMCDISCO(affectedRows.getLong(11));
                dato.setMCDISCU(affectedRows.getLong(12));
//                dato.setMCSALPM(affectedRows.getLong(13));
//                dato.setMCSALMO(affectedRows.getLong(14));
//                dato.setMCPAGMI(affectedRows.getLong(15));
//                dato.setMCAUTPE(affectedRows.getLong(16));
//                dato.setMCDIAMO(affectedRows.getInt(17));
//                dato.setMCUPAGO(affectedRows.getLong(18));
//                dato.setMCAPHVT(affectedRows.getLong(19));
//                dato.setMCAPDVT(affectedRows.getLong(20));
//                dato.setMCFORPG(affectedRows.getString(21).trim());
//                dato.setMCTIPPG(affectedRows.getString(22).trim());
                dato.setMCCTABC(affectedRows.getString(23).trim());
//                dato.setMCFEUPG(affectedRows.getString(24).trim());
//                dato.setMCFEUMO(affectedRows.getString(25).trim());
//                dato.setMCFEUCA(affectedRows.getString(26).trim());
//                dato.setMCFEUEX(affectedRows.getString(27).trim());
//                dato.setMCFEALT(affectedRows.getString(28).trim());
//                dato.setMCINTNO(affectedRows.getLong(29));
//                dato.setMCINTMO(affectedRows.getLong(30));
//                dato.setMCINTPU(affectedRows.getLong(31));
//                dato.setMCINTCU(affectedRows.getLong(32));
//                dato.setMCINTCA(affectedRows.getLong(33));
                dato.setMCPORPG(affectedRows.getLong(34));
//                dato.setECCODIG(affectedRows.getString(35).trim());
//                dato.setMCFUCIE(affectedRows.getString(36).trim());
//                dato.setMCFUVTO(affectedRows.getString(37).trim());
//                dato.setGRCODIG(affectedRows.getString(38).trim());
//                dato.setMCHISMO(affectedRows.getInt(39));
                dato.setMCCUOPE(affectedRows.getLong(40));
                dato.setMCREFPE(affectedRows.getLong(41));
                dato.setMCADEPE(affectedRows.getLong(42));
//                dato.setMCADEPE(affectedRows.getLong(43));
//                dato.setMCINTRE(affectedRows.getLong(44));
                dato.setMCFINSU(affectedRows.getLong(44));
                dato.setMCMORSU(affectedRows.getLong(45));
                dato.setMCPUNSU(affectedRows.getLong(46));
//                dato.setMCRECCM(affectedRows.getString(48).trim());
//                dato.setMCRECLM(affectedRows.getString(49).trim());
//                dato.setMCFEPPG(affectedRows.getString(50).trim());
                dato.setMCPMFIJ(affectedRows.getLong(50));
                dato.setMCFEFIJ(affectedRows.getString(51).trim());
//                dato.setMCCNTAD(affectedRows.getInt(53));
//                dato.setMCCNTCO(affectedRows.getInt(54));
//                dato.setMCCNTEX(affectedRows.getInt(55));
//                dato.setMCNUMFI(affectedRows.getLong(56));
//                dato.setMCNUMMO(affectedRows.getLong(57));
//                dato.setMCNUMPU(affectedRows.getLong(58));
//                dato.setMCNUMCO(affectedRows.getLong(59));
//                dato.setMCNUMCA(affectedRows.getLong(60));
//                dato.setMCCOMFI(affectedRows.getLong(61));
//                dato.setMCTARDEB(affectedRows.getString(62).trim());
//                dato.setMCSALIM(affectedRows.getLong(63));
//                dato.setMCCUOPC(affectedRows.getLong(64));
//                dato.setMCCUOPA(affectedRows.getLong(65));
//                dato.setMCAPCOC(affectedRows.getLong(66));
//                dato.setMCAPCOA(affectedRows.getLong(67));
//                dato.setMCAPCUC(affectedRows.getLong(68));
//                dato.setMCAPCUA(affectedRows.getLong(69));
//                dato.setMCSALMI(affectedRows.getLong(70));
//                dato.setMCSUSPE(affectedRows.getString(71).trim());
                dato.setMCEMISO(affectedRows.getString(71).trim());
                dato.setMCAFINI(affectedRows.getString(72).trim());
//                dato.setMCCODSC(affectedRows.getString(74).trim());
//                dato.setMCTIPOC(affectedRows.getString(75).trim());
//                dato.setMCESTEX(affectedRows.getString(76).trim());
//                dato.setMCUTCUO(affectedRows.getLong(77));
//                dato.setMCCUOMA(affectedRows.getLong(78));
//                dato.setMCFESU1(affectedRows.getString(79).trim());
//                dato.setMCFESU2(affectedRows.getString(80).trim());

            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosTmctaaf: " + ex);

                conn.rollback();

            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosTmctaaf: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada"); //Ricardo Arce 042020
                return dato;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosTmctaaf: " + ex);
            }
        }
        return dato;
    }

    @SuppressWarnings("finally")
    public static Boolean ejecucionSQL(String query) {
        boolean ejecucion = false;
        Connection conn = null;
//        LOGGER.info("QUERY:" + query);

        try {
            conn = DBUtils.connect();
            Statement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            stmt = conn.createStatement();
            stmt.executeUpdate(query);
            conn.close();
            stmt.close();
            ejecucion = true;
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".ejecucionSQL: " + ex);
                ejecucion = false;
                proceso = false;
                conn.rollback();

            } catch (SQLException ex1) {
                ejecucion = false;
                proceso = false;
                LOGGER.info("Error en " + DBUtils.class + ".ejecucionSQL: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada");
                return ejecucion;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".ejecucionSQL: " + ex);
            }
        }
        return ejecucion;
    }

    @SuppressWarnings("finally")
    public static Boolean AuditTmtaraf(DatoTmtaraf datoTar, DatoTmctaaf datoCta, String user,
            String fecha, String hora, String campoMod, String valAnt, String valNuevo) {
        boolean Auditoria = false;
        Connection conn = null;

        try {
            conn = DBUtils.connect();
            String querySec = "select max(atnumse) from GXBDBPS.autaraf where atemiso = '021' and atafini = '" + datoTar.getAFAFINI().trim() + "' and atnumta = '" + datoTar.getMTNUMTA().trim() + "'";
//            LOGGER.info("Query secuencia:" + querySec);
            Long Sec = getSecuencia(querySec, conn);

            String autQuery = "INSERT INTO GXBDBPS.AUTARAF VALUES ('" + fecha + "', '" + datoTar.getENEMISO().trim() + "', '" + datoTar.getAFAFINI().trim() + "', "
                    + "'" + datoTar.getMTNUMTA().trim() + "', " + Sec + ", 'M', 1, '" + user + "', '" + fecha + "', "
                    + " " + hora + ", 0, '" + campoMod + "', '" + valAnt + "', '" + valNuevo + "', 0, '" + datoTar.getSUCODIG().trim() + "')";
//            LOGGER.info("Query Auditoria:" + autQuery);

            Statement stmt = null;
            stmt = conn.createStatement();
//            stmt.setString(1, fecha);
//            stmt.setString(2, datoTar.getENEMISO().trim());
//            stmt.setString(3, datoTar.getAFAFINI().trim());
//            stmt.setString(4, datoTar.getMTNUMTA().trim());
//            stmt.setString(5, ""+Sec);
//            stmt.setString(6, "M");
//            stmt.setString(7, "1");
//            stmt.setString(8, regraba.getUsuario());
//            stmt.setString(9, fecha);
//            stmt.setString(10, hora);
//            stmt.setString(11, "0");
//            stmt.setString(12, campoMod);
//            stmt.setString(13, valAnt);
//            stmt.setString(14, valNuevo);
//            stmt.setString(15, "0");
//            stmt.setString(16, datoTar.getSUCODIG().trim());
//            LOGGER.info("Query:"+stmt.toString());
            stmt.executeUpdate(autQuery);
            conn.close();
            stmt.close();
            Auditoria = true;
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".regrabar: " + ex);
                Auditoria = false;
                conn.rollback();

            } catch (SQLException ex1) {
                Auditoria = false;
                LOGGER.info("Error en " + DBUtils.class + ".regrabar: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
                return Auditoria;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".regrabar: " + ex);
            }
        }
        return Auditoria;
    }

//    @SuppressWarnings("finally")
//    public static ResultSet getResultSet(String query) {
//        ResultSet rs = null;
//        Connection conn = null;
//        LOGGER.info("QUERY: " + query);
//
//        try {
//            conn = DBUtils.connect();
//            PreparedStatement stmt = null;
//            stmt = conn.prepareStatement(query);
//            stmt.executeQuery();
//            rs = stmt.getResultSet();
//            conn.close();
//            stmt.close();
//        } catch (Exception ex) {
//            try {
//                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
//                conn.rollback();
//
//            } catch (SQLException ex1) {
//                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex1);
//            }
//        } finally {
//            try {
//                conn.close();
//                LOGGER.info("Conexión Cerrada");
//            } catch (SQLException ex) {
//                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
//            }
//        }
//        return rs;
//    }
    @SuppressWarnings("finally")
    public static String getFechaComercial() {
        //Se obtiene FECHA COMERCIAL
        String obtenerFecom = "select emfep1 from libdebito.empresa0p";
        String fechaCom = "";
        Connection conn = null;
        try {
            conn = DBUtils.connect();
            PreparedStatement stmt4 = null;
            stmt4 = conn.prepareStatement(obtenerFecom);
            stmt4.executeQuery();
            ResultSet rs4 = stmt4.getResultSet();
            Date feCom = new Date();
            while (rs4.next()) {
                feCom = rs4.getDate(1);
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            fechaCom = sdf.format(feCom);
            conn.close();
            stmt4.close();
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
                conn.rollback();

            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
            }
        }
        return fechaCom;
    }

    @SuppressWarnings("finally")
    public static DatoTmoviaf getDatoTmoviaf(String consulta) {
        DatoTmoviaf dato = new DatoTmoviaf();
        Connection conn = null;
        String query = consulta;
        try {
            conn = DBUtils.connect();
//            LOGGER.info("DatoTmoviaf: " + query);
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                dato.setMVSECUE(affectedRows.getString(1).trim());
                dato.setMTNUMTA(affectedRows.getString(2).trim());
                dato.setCMCODIG(affectedRows.getString(3).trim());
                dato.setMVEMISO(affectedRows.getString(4).trim());
                dato.setMVNUMCT(affectedRows.getLong(5));
                dato.setMVCODSC(affectedRows.getString(6).trim());
                dato.setMVCLASE(affectedRows.getString(7).trim());
                dato.setMVAFINI(affectedRows.getString(8).trim());
                dato.setMVCODMN(affectedRows.getString(9).trim());
                dato.setMVTIPOT(affectedRows.getString(10).trim());
                dato.setMVFEVAL(affectedRows.getString(11).trim());
                dato.setMVFEPRO(affectedRows.getString(12).trim());
                dato.setMVFEREA(affectedRows.getString(13).trim());
                dato.setMVHORA(affectedRows.getLong(14));
                dato.setMVIMPO1(affectedRows.getDouble(15));
                dato.setMVIMPO2(affectedRows.getDouble(16));
                dato.setMVDISPO(affectedRows.getString(17).trim());
                dato.setMVNOVED(affectedRows.getString(18).trim());
                dato.setMVCODCO(affectedRows.getString(19).trim());
                dato.setMVNOMCO(affectedRows.getString(20).trim());
                dato.setMVMECAC(affectedRows.getString(21).trim());
                dato.setMVCODAU(affectedRows.getString(22).trim());
                dato.setMVCODRE(affectedRows.getInt(23));
                dato.setMVORIGE(affectedRows.getString(24).trim());
                dato.setMVCUPON(affectedRows.getLong(25));
                dato.setMVINITR(affectedRows.getString(26).trim());
                dato.setMVFINTR(affectedRows.getString(27).trim());
                dato.setMVPOSEM(affectedRows.getString(28).trim());
                dato.setMVIDUSR(affectedRows.getString(29).trim());
                dato.setMVTIPTA(affectedRows.getString(30).trim());
                dato.setMVBINES(affectedRows.getString(31).trim());
                dato.setMVFINAN(affectedRows.getString(32).trim());
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getDatoTmoviaf: " + ex);
                conn.close();
            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getDatoTmoviaf: " + ex1);
            }
        } finally {
            try {
                conn.close();
                LOGGER.info("Conexión Cerrada");
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getDatoTmoviaf: " + ex);
            }
        }
        return dato;
    }

    @SuppressWarnings("finally")
    public static boolean existCliente(String tipoDoc, String nroDoc, Connection conn) {
        boolean esCliente = false;
        String ciCliente = "";
        try {
            //Se obtiene los datos del cliente si existe
            String clienteSQL = "select cenumdo from GXBDBPS.gclieaf where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + nroDoc + "'";
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(clienteSQL);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                ciCliente = rs.getString(1);
            }
            stmt.close();
            if (!ciCliente.equals("") && ciCliente.trim() != null) {
                esCliente = true;
            }
            LOGGER.info("esCliente");
        } catch (SQLException ex) {
            LOGGER.info("Error en " + DBUtils.class + ".existCliente: " + ex);
        }
        return esCliente;
    }

    @SuppressWarnings("finally")
    public static String getFechaExtracto(String enemiso, String afinidad, String bin, Connection conn) {
        String fechaExt = "";
        try {
            Date fech = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
            String fecha = sdf.format(fech);
//            LOGGER.info("fecha " + fecha);
            //Se obtiene la fecha extracto 
            String fechaExtrSQL = "select affecie from GXBDBPS.tmensaf where enemiso = '" + enemiso + "' and bibines = '" + bin + "' and afafini = '" + afinidad + "' and affecie like '" + fecha + "%'";
//            String fechaExtrSQL = "select affecie from GXBDBPS.tmensaf where enemiso = '" + enemiso + "' and bibines = '627431' and afafini = '" + afinidad + "' and affecie like '" + fecha + "%'";
//            LOGGER.info(fechaExtrSQL);
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(fechaExtrSQL);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                fechaExt = rs.getString(1);
            }
//            LOGGER.info("fechaExtracto " + fechaExt);
            stmt.close();
//            if (fechaExt.equals("") || fechaExt.trim() == null) {
//                int mes = Integer.parseInt(fecha.substring(4));
//                if (mes == 1) {
//                    mes = 12;
//                } else {
//                    mes = mes - 1;
//                }
//                fecha = fecha.substring(0, 4) + mes;
//                fechaExtrSQL = "select affecie from GXBDBPS.tmensaf where enemiso = '" + enemiso + "' and bibines = '627431' and afafini = '" + afinidad + "' and affecie like '" + fecha + "%'";
//                LOGGER.info(fechaExtrSQL);
//                PreparedStatement stmt2 = null;
//                stmt2 = conn.prepareStatement(fechaExtrSQL);
//                stmt2.executeQuery();
//                ResultSet rs2 = stmt2.getResultSet();
//                while (rs2.next()) {
//                    fechaExt = rs2.getString(1);
//                }
//                stmt2.close();
//            }
//            LOGGER.info("fechaExtracto " + fechaExt);
        } catch (SQLException ex) {
            LOGGER.info("Error en " + DBUtils.class + ".fechaExtracto: " + ex);
        }
        return fechaExt;
    }

    @SuppressWarnings("finally")
    public static DatoCliente getDatosCliente(String nroDocumento, String entidad, String tipoDoc) {
        DatoCliente dato = new DatoCliente();
        Connection conn = null;
        String query = "SELECT * FROM GXBDBPS.GCLIEAF WHERE ENEMISO = '" + entidad + "' AND CETIPOD = '" + tipoDoc + "' AND CENUMDO = '" + nroDocumento + "'";
//        String query = "SELECT * FROM GXBDBPS.GCLIEAF WHERE ENEMISO = '" + entidad + "' AND CETIPOD = '" + tipoDoc + "' AND CENUMDO = '" + nroDocumento + "'";
        try {
            conn = DBUtils.connect();
//            LOGGER.info("DatoCliente: " + query);
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                dato.setEnemiso(affectedRows.getString(1).trim());
//                LOGGER.info(affectedRows.getString(1).trim());
                dato.setCetipod(affectedRows.getString(2).trim());
//                LOGGER.info(affectedRows.getString(2).trim());
                dato.setCenumdo(affectedRows.getString(3).trim());
//                LOGGER.info(affectedRows.getString(3).trim());
                dato.setCenomb1(affectedRows.getString(4).trim());
//                LOGGER.info(affectedRows.getString(4).trim());
                dato.setCenomb2(affectedRows.getString(5).trim());
//                LOGGER.info(affectedRows.getString(5).trim());
                dato.setCeapel1(affectedRows.getString(6).trim());
//                LOGGER.info(affectedRows.getString(6).trim());
                dato.setCeapel2(affectedRows.getString(7).trim());
//                LOGGER.info(affectedRows.getString(7).trim());
                dato.setCenumru(affectedRows.getString(8).trim());
//                LOGGER.info(affectedRows.getString(8).trim());
                dato.setCesexo(affectedRows.getString(9).trim());
//                LOGGER.info(affectedRows.getString(9).trim());
                dato.setCeecivi(affectedRows.getString(10).trim());
//                LOGGER.info(affectedRows.getString(10).trim());
                dato.setCefenac(affectedRows.getString(11).trim());
//                LOGGER.info(affectedRows.getString(11).trim());
                dato.setMocodig(affectedRows.getString(12).trim());
//                LOGGER.info(affectedRows.getString(12).trim());
                dato.setCetipoc(affectedRows.getString(13).trim());
//                LOGGER.info(affectedRows.getString(13).trim());
                dato.setCenumdc(affectedRows.getString(14).trim());
//                LOGGER.info(affectedRows.getString(14).trim());
//                LOGGER.info("Antes de "+ affectedRows.getString(15).trim());
                dato.setCeempre(affectedRows.getString(15).trim());
//                LOGGER.info(affectedRows.getString(15).trim());
                dato.setPrcodig(affectedRows.getString(16).trim());
//                LOGGER.info(affectedRows.getString(16).trim());
                dato.setCetipov(affectedRows.getString(17).trim());
//                LOGGER.info(affectedRows.getString(17).trim());
                dato.setCesalar(affectedRows.getString(18).trim());
//                LOGGER.info(affectedRows.getString(18).trim());
                dato.setCeultsc(affectedRows.getString(19).trim());
//                LOGGER.info(affectedRows.getString(19).trim());
                dato.setCeapeca(affectedRows.getString(20).trim());
//                LOGGER.info(affectedRows.getString(20).trim());
                dato.setCeapnom(affectedRows.getString(21).trim());
//                LOGGER.info(affectedRows.getString(21).trim());
                dato.setCefeing(affectedRows.getString(22).trim());
//                LOGGER.info(affectedRows.getString(22).trim());
                dato.setCehorin(affectedRows.getString(23).trim());
//                LOGGER.info(affectedRows.getString(23).trim());
                dato.setCeusrin(affectedRows.getString(24).trim());
//                LOGGER.info(affectedRows.getString(24).trim());
                dato.setCenumd2(affectedRows.getString(25).trim());
//                LOGGER.info(affectedRows.getString(25).trim());
                dato.setCesocio(affectedRows.getString(26).trim());
//                LOGGER.info(affectedRows.getString(26).trim());
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosCliente: " + ex);

                conn.rollback();

            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosCliente: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada"); //Ricardo Arce 042020
                return dato;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosTmtaraf: " + ex);
            }
        }
        return dato;
    }

    @SuppressWarnings("finally")
    public static DatoMoneda getDatosMoneda(String codMoneda) {
        DatoMoneda dato = new DatoMoneda();
        Connection conn = null;
        String query = "select * from GXBDBPS.gmoneaf where mocodig = '" + codMoneda + "'";
        try {
            conn = DBUtils.connect();
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                dato.setMocodig(affectedRows.getString(1).trim());
                dato.setModescr(affectedRows.getString(2).trim());
                dato.setMosimbo(affectedRows.getString(3).trim());
                dato.setMopais(affectedRows.getString(4).trim());
                dato.setMocodpa(affectedRows.getString(5).trim());
                dato.setMogenti(affectedRows.getString(6).trim());
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosMoneda: " + ex);

                conn.rollback();

            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosMoneda: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada"); //Ricardo Arce 042020
                return dato;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getDatosMoneda: " + ex);
            }
        }
        return dato;
    }

//    @SuppressWarnings("finally")
//    public static String getDeudaTotal(String safac, String sfrive, String salfi, String cuope, String cadepe, String crefpe) {
//
//        float MCSAFAC = Float.parseFloat(safac);
//        float MCSFRIVE = Float.parseFloat(sfrive);
//        float MCSALFI = Float.parseFloat(salfi);
//        float MCCUOPE = Float.parseFloat(cuope);
//        float MCADETE= Float.parseFloat(cadepe);
//        float MCREFPE= Float.parseFloat(crefpe);
//        
//        String deuda = MCSAFAC + MCSFRIVE + MCSALFI + MCCUOPE + MCADETE + MCREFPE + "";
//        
//        return deuda;
//    } 
    @SuppressWarnings("finally")
    public static List<LineasDetalle> getLineasDetalle(String tarjeta, String afinidad, String emisor, String bin) {
        List<LineasDetalle> lineasDetalle = new ArrayList();
        Connection conn = null;
        String mesHoy = obtenerFechaHora("yyyyMM");
        String mesAnterior = getFechaAntesDespues(0, -1, 0, "yyyyMM");
        String mesSiguiente = getFechaAntesDespues(0, 1, 0, "yyyyMM");
        int fechaProcesoHoy = 0;
        int fechaCierreHoy = 0;
        int fechaCierreAnterior = 0;
        int fechaCierreSiguiente = 0;
        String fechaInicio = "";
        String fechaFin = "";
        try {
            conn = DBUtils.connect();
            //Obtengo fecha proceso de hoy
            String query4 = "SELECT EMFEPRO FROM GXBDBPS.GEMPRAF";

            PreparedStatement stmt4 = null;
            stmt4 = /*this.*/ conn.prepareStatement(query4);
            ResultSet rs4 = stmt4.executeQuery();

            while (rs4.next()) {
                fechaProcesoHoy = Integer.parseInt(rs4.getString(1));
            }
            stmt4.close();
            //Obtenemos las fechas de cierre el mes actual, del mes anterior y del mes siguiente
            String queryFechasCierre = "SELECT AFFECIE FROM GXBDBPS.TCIERAF WHERE ENEMISO = '" + emisor + "' AND "
                    + "AFAFINI = '" + afinidad + "' AND BIBINES = '" + bin + "' AND "
                    + "(AFFECIE like '" + mesHoy + "%' OR AFFECIE like '" + mesAnterior + "%' OR AFFECIE like '" + mesSiguiente + "%') "
                    + "ORDER BY AFFECIE ASC";

            PreparedStatement stmt5 = null;
            stmt5 = /*this.*/ conn.prepareStatement(queryFechasCierre);
            ResultSet rs5 = stmt5.executeQuery();

            while (rs5.next()) {
                if (rs5.getString(1).contains(mesHoy)) {
                    fechaCierreHoy = Integer.parseInt(rs5.getString(1));
                } else if (rs5.getString(1).contains(mesAnterior)) {
                    fechaCierreAnterior = Integer.parseInt(rs5.getString(1));
                } else if (rs5.getString(1).contains(mesSiguiente)) {
                    fechaCierreSiguiente = Integer.parseInt(rs5.getString(1));
                }
            }
            stmt5.close();
            //Obtenemos cual son las fechas de cierre de la consulta de extracto
            if (fechaCierreAnterior < fechaProcesoHoy && fechaCierreHoy >= fechaProcesoHoy) {
                fechaInicio = fechaCierreAnterior + "";
                fechaFin = fechaCierreHoy + "";
            } else if (fechaCierreHoy < fechaProcesoHoy && fechaCierreSiguiente >= fechaProcesoHoy) {
                fechaInicio = fechaCierreHoy + "";
                fechaFin = fechaCierreSiguiente + "";
            }

            LOGGER.info("Mes anterior: " + fechaInicio);
            LOGGER.info("Mes actual: " + fechaFin);
            //Movimientos concretados de Tmoviaf
            String query = "select mtnumta, mvcodre, mvferea, mvfepro, mvcupon, mvnomco, mvimpo2, MVTIPOT "
                    + "from GXBDBPS.tmoviaf where mvemiso = '" + emisor + "' and mvafini = '" + afinidad
                    + "' and mtnumta = '" + tarjeta + "' and mvcodre = 0 and MVFEPRO between '" + fechaInicio + "' "
                    + "and '" + fechaFin + "' AND CMCODIG in (SELECT CMCODIG FROM GXBDBPS.TCMOVAF WHERE CMEXTRA = 'S')";
//            LOGGER.info("Voy a ejecutar SQL:" + query);

            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            int tipoOperacion = 0;
            LineasDetalle linea;
            while (rs.next()) {
                linea = new LineasDetalle();
                linea.setNroTarjeta(rs.getString(1));
                linea.setTipoTransaccion((rs.getString(2).equals("0")) ? "C" : "E");
                linea.setFechaOperacion(rs.getString(3));
                linea.setFechaProceso(rs.getString(4));
                linea.setNroCupon(rs.getString(5));
                linea.setDescripcion(rs.getString(6));
                tipoOperacion = rs.getString(8).equals("D") ? 1 : -1;
                double importeMovimiento = tipoOperacion * (rs.getDouble(7));
                linea.setImporte(importeMovimiento + "");
                linea.setImporte(linea.getImporte().contains(".00")? linea.getImporte().replace(".00", ".0"): linea.getImporte());
                lineasDetalle.add(linea);
            }
            stmt.close();

            //Movimientos en cuotas de Tcuotaf
            String query2 = "SELECT MTNUMTA, CUFEREA, CUFECOM, CUCODAU, CUNOMCO, CUIMPOR, CUCANCU FROM GXBDBPS.TCUOTAF "
                    + "WHERE MTNUMTA = '" + tarjeta + "' AND CUFECOM between '" + fechaInicio + "' "
                    + "and '" + fechaFin + "' AND CUEMISO = '" + emisor
                    + "' AND CUAFINI = '" + afinidad + "' AND CMCODIG in (SELECT CMCODIG FROM GXBDBPS.TCMOVAF WHERE CMEXTRA = 'S')";
//            LOGGER.info("Voy a ejecutar SQL:" + query);
            PreparedStatement stmt2 = null;
            stmt2 = /*this.*/ conn.prepareStatement(query2);
            ResultSet rs2 = stmt2.executeQuery();

            while (rs2.next()) {
                linea = new LineasDetalle();
                linea.setNroTarjeta(rs2.getString(1));
                linea.setTipoTransaccion("C");
                linea.setFechaOperacion(rs2.getString(2));
                linea.setFechaProceso(rs2.getString(3));
                linea.setNroCupon(rs2.getString(4));
                linea.setDescripcion(rs2.getString(5));
                linea.setImporte(rs2.getString(6));
                linea.setCantidadCuotas(rs2.getString(7));
                linea.setImporte(linea.getImporte().contains(".00")? linea.getImporte().replace(".00", ".0"): linea.getImporte());
                lineasDetalle.add(linea);
            }
            stmt2.close();

            //Movimientos pendientes de TAUTPAF
            String query3 = "SELECT MTNUMTA, APSTATS, APFEREA, APFECOM, APCODAU, APNOMCO, APIMPOR, APCANCU FROM GXBDBPS.TAUTPAF "
                    + "WHERE MTNUMTA = '" + tarjeta + "' AND APSTATS = 'P' AND APFECOM between '" + fechaInicio + "' "
                    + "and '" + fechaFin + "' AND APEMISO = '" + emisor
                    + "' AND APAFINI = '" + afinidad + "' AND CMCODIG in (SELECT CMCODIG FROM GXBDBPS.TCMOVAF WHERE CMEXTRA = 'S')";
//            LOGGER.info("Voy a ejecutar SQL:" + query);
            PreparedStatement stmt3 = null;
            stmt3 = /*this.*/ conn.prepareStatement(query3);
            ResultSet rs3 = stmt3.executeQuery();

            while (rs3.next()) {
                linea = new LineasDetalle();
                linea.setNroTarjeta(rs3.getString(1));
                linea.setTipoTransaccion(rs3.getString(2));
                linea.setFechaOperacion(rs3.getString(3));
                linea.setFechaProceso(rs3.getString(4));
                linea.setNroCupon(rs3.getString(5));
                linea.setDescripcion(rs3.getString(6));
                linea.setImporte(rs3.getString(7));
                linea.setCantidadCuotas(rs3.getString(8));
                linea.setImporte(linea.getImporte().contains(".00")? linea.getImporte().replace(".00", ".0"): linea.getImporte());
                lineasDetalle.add(linea);
            }
            stmt3.close();
        } catch (Exception ex) {
            LOGGER.info("Error en " + DBUtils.class + ".getLineasDetalle: " + ex);
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada"); //Ricardo Arce 042020
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getLineasDetalle: " + ex);
            }
        }
        
        return lineasDetalle;
    }

    @SuppressWarnings("finally")
    public static String getFechaVPMPC(String enemiso, String bin, String afinidad) {
        String fechaVtoPagoMin = "";
        String fechaProxCierre = "";
        String msgExtracto = "";
        String fechas = "";
        Connection conn = null;
        try {
            Date fech = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
            //NO olvidar descomentar
            String fecha = sdf.format(fech);
//            String fecha = "201812";
            LOGGER.info("Obtengo el año y mes del dia: " + fecha);
            //Se obtiene la fecha vto pago min y prox cierre
//            String query = "select A.affecie, A.affevto, B.melinea from GXBDBPS.tcieraf A inner join GXBDBPS.tmensaf B on B.affecie = A.affecie "
//                    + "where A.enemiso = '" + enemiso + "' and A.bibines = '627431' and A.afafini = '" + afinidad + "' and A.affecie like '" + fecha + "%'";
            String query = "select b.affecie, b.affevto, a.melinea from GXBDBPS.tmensaf a  inner join GXBDBPS.tcieraf b "
                    + "on a.enemiso=b.enemiso and a.bibines=b.bibines and a.afafini=b.afafini and A.affecie = B.affecie "
                    + "where a.enemiso='" + enemiso + "' and A.affecie like '" + fecha + "%' and a.bibines = '" + bin + "' and a.afafini = '" + afinidad + "' order by mesecue";
//            LOGGER.info("Voy a ejecutar SQL: " + query);

            conn = DBUtils.connect();
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                fechaProxCierre = rs.getString(1);
                fechaVtoPagoMin = rs.getString(2);
                msgExtracto = rs.getString(3);
            }
            stmt.close();
            if (fechaProxCierre.equals("") && fechaVtoPagoMin.equals("")) {
                fechas = "No existe fecha";
            } else {
                LOGGER.info("Fecha VtoPagoMin obtenido:" + fechaVtoPagoMin);
                LOGGER.info("Fecha ProxCierre obtenido:" + fechaProxCierre);
                LOGGER.info("Mensaje extracto obtenido:" + msgExtracto);
                fechas = fechaVtoPagoMin + ";" + fechaProxCierre + ";" + msgExtracto;
            }
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
                conn.rollback();

            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada"); //Ricardo Arce 042020
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
            }
        }
        return fechas;
    }

    @SuppressWarnings("finally")
    public static long getPagoMinPendiente(String enemiso, String afinidad, String tarjeta, DatoTmctaaf datoCuenta) {
        String fechaVtoPagoMin = "";
        int factorSigno = 0;
        long sumaFin = 0;
        long sumaNoFin = 0;
        String cmtipom = "";
        String mvfinan = "";
        long mvimpo2 = 0;

        long sunfi = 0;
        long mcsldac = 0;
        long salnofin = 0;
        long porfi = 0;
        String afmofin = "";
        long afminpm = 0;
        long pagmi = 0;
        long sldtoesp = 0;
        Connection conn = null;
        try {
            conn = DBUtils.connect();
            //Se obtiene cmtipom, mvfinan y mvimpo2
            String query = "select B.cmtipom, A.mvfinan, A.mvimpo2 from GXBDBPS.tmoviaf A "
                    + "inner join GXBDBPS.tcmovaf B on B.cmcodig = A.cmcodig "
                    + "where A.mvemiso = '" + enemiso + "' and A.mvafini = '" + afinidad + "' and A.mtnumta = '" + tarjeta + "'";
//            String query = "select B.cmtipom, A.mvfinan, A.mvimpo2 from GXBDBPS.tmoviaf A "
//                    + "inner join GXBDBPS.tcmovaf B on B.cmcodig = A.cmcodig "
//                    + "where A.mvemiso = '" + enemiso + "' and A.mvafini = '" + afinidad + "' and A.mtnumta = '" + tarjeta + "'";
//            LOGGER.info("Voy a ejecutar SQL:" + query);

            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                cmtipom = rs.getString(1);
                mvfinan = rs.getString(2);
                mvimpo2 = rs.getLong(3);

                // Debito: Aplica positivo
                if (cmtipom.equals("D")) {
                    factorSigno = 1;
                    // Crédito: Aplica negativo
                } else if (cmtipom.equals("C")) {
                    factorSigno = -1;
                    // No aplica
                } else {
                    factorSigno = 0;
                }
                // Movimiento Financiable
                if (mvfinan.equals("S")) {
                    sumaFin += (mvimpo2 * factorSigno);
                    // Movimiento No Financiable
                } else if (mvfinan.equals("N")) {
                    sumaNoFin += (mvimpo2 * factorSigno);
                }
            }
            stmt.close();

            sunfi = sumaNoFin;
            //Ricardo Arce 042020
//            LOGGER.info("SunFi: " + sunfi); 
            //Se halla el saldo actual
//            LOGGER.info("MCSAFAC: " + datoCuenta.getMCSAFAC());
//            LOGGER.info("MCSFNVE: " + datoCuenta.getMCSFNVE());
//            LOGGER.info("MCSALFI: " + datoCuenta.getMCSALFI());
            mcsldac = datoCuenta.getMCSAFAC() + datoCuenta.getMCSFNVE() + datoCuenta.getMCSALFI();
//            LOGGER.info("Mcsldac: " + mcsldac); //Ricardo Arce 042020
            if (sunfi > mcsldac) {
                sunfi = mcsldac;
            } else if (sunfi < 0) {
                sunfi = 0;
            }

            salnofin = mcsldac - sunfi;
//            LOGGER.info("Salnofin: " + salnofin); //Ricardo Arce 042020
            // Pago minimo x saldo descontado (Excl.Mov.no financ.)
            porfi = Math.round((salnofin * datoCuenta.getMCPORPG()) / 100);
//            LOGGER.info("Porfi: " + porfi); //Ricardo Arce 042020

            if (porfi < 0) {
                porfi = 0;
            }
            String query2 = "select afmofin, afminpm from GXBDBPS.tafinaf where enemiso = '" + enemiso + "' and bibines = '" + tarjeta.substring(0, 6) + "' and afafini = '" + afinidad + "'";
//            String query2 = "select afmofin, afminpm from GXBDBPS.tafinaf where enemiso = '" + enemiso + "' and bibines = '627431' and afafini = '" + afinidad + "'";
//            LOGGER.info("Voy a ejecutar SQL2: " + query2);

            PreparedStatement stmt2 = null;
            stmt2 = /*this.*/ conn.prepareStatement(query2);
            ResultSet rs2 = stmt2.executeQuery();
            while (rs2.next()) {
                afmofin = rs2.getString(1);
                afminpm = rs2.getLong(2);
            }
            stmt2.close();
//            LOGGER.info("afmofin: " + afmofin); //Ricardo Arce 042020
//            LOGGER.info("afminpm: " + afminpm); //Ricardo Arce 042020
            // Caso Mora financiable 
            // Asegurar que su pago minimo al menos cubra su mora, de otra forma se 
            // le va a generar intereses moratorios y punitorios sin saldo de pago minimo.
            if (afmofin.equals("S")) {
                // Si el saldo en mora es mayor a (%CompraFin + CrgNoFin)
                if (datoCuenta.getMCSALMO() > porfi + sunfi) {
                    pagmi = datoCuenta.getMCSALMO();        // ..... El pago minimo es el saldo en mora
                } else {
                    pagmi = porfi + sunfi;          // sino: El pago mínimo es la suma de (%CompraFin + CrgNoFin)
                }
                // Caso Mora no financiable
            } else {
                pagmi = porfi + sunfi + datoCuenta.getMCSALMO();  // Pago mínimo es la suma de (%CompraFin + CrgNoFin + SldMora)  
            }

//            LOGGER.info("El pago minimo antes de el nuevo codigo " + pagmi); //Ricardo Arce 042020
               
            //lo que se agrego
            //Ricardo Arce 042020
//            LOGGER.info("MCFINSU: " + datoCuenta.getMCFINSU()); 
//            LOGGER.info("MCMORSU: " + datoCuenta.getMCMORSU());
//            LOGGER.info("MCPUNSU: " + datoCuenta.getMCPUNSU());
            sldtoesp = mcsldac + datoCuenta.getMCFINSU() + datoCuenta.getMCMORSU() + datoCuenta.getMCPUNSU();             // Saldo actual mas cargos en suspenso
            // Se controla que sea al menos el minimo
            if (pagmi < afminpm) { //monto minimo pagominimo (TAFINAF)
                if (afminpm < mcsldac) {      // Siempre que el mínimo no supere al saldo actual
                    pagmi = afminpm;
                } else {
                    pagmi = mcsldac;
                }
            }
            String fepro = getFechaProceso();
//            LOGGER.info("MCFEFIJ " + datoCuenta.getMCFEFIJ()); //Ricardo Arce 042020
            long iMcfefij = Long.parseLong(datoCuenta.getMCFEFIJ());
//            LOGGER.info("fepro " + fepro); //Ricardo Arce 042020
            long iFepro = Long.parseLong(fepro);
            // Se controla el caso de seteo de pago minimo fijo
            if (datoCuenta.getMCPMFIJ() != null && iMcfefij >= iFepro) { //&wvfepro=Fecha de proceso
                if (datoCuenta.getMCPMFIJ() < sldtoesp) {     // Controla respecto al Saldo actual mas cargos en suspenso
                    pagmi = datoCuenta.getMCPMFIJ();
                }
            } else {
                pagmi = sldtoesp;
            }

            // Pago mínimo no debe ser negativo
            if (pagmi < 0) {
                pagmi = 0;    //nullvalue(pagmi);
            }
            // Truncamiento del pago mínimo calculado
            int intPagmi = round(pagmi);
            pagmi = intPagmi;

        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
                conn.rollback();

            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada"); //Ricardo Arce 042020
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
            }
        }
//        LOGGER.info("El monto del pago minimo es de " + pagmi); //Ricardo Arce 042020
        return pagmi;
    }

    @SuppressWarnings("finally")
    public static boolean existTarjeta(String nroTarjeta) {
        boolean esTarjeta = false;
        String nroTar = "";
        String query = "SELECT * FROM GXBDBPS.TMTARAF WHERE MTNUMTA = '" + nroTarjeta + "'";
        //String query = "SELECT * FROM GXBDBPS.TMTARAF WHERE MTNUMTA = '"+nroTarjeta+"'";
        Connection conn = null;
        try {
            conn = DBUtils.connect();
            //Se obtiene el numero de tarjeta si existe
//            LOGGER.info("Voy a ejecutar SQL: " + query);
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(query);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                nroTar = rs.getString(1);
            }
            stmt.close();
            if (!nroTar.equals("") && nroTar.trim() != null) {
                esTarjeta = true;
            }
//            LOGGER.info("esTarjeta");
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
                conn.rollback();
            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("CONEXION CERRADA");
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
            }
        }
        return esTarjeta;
    }

    @SuppressWarnings("finally")
    public static String getFechaProceso() {
        String fechaProceso = "";
        String query = "select emfepro from GXBDBPS.gempraf";
        Connection conn = null;
        try {
            conn = DBUtils.connect();
            //Se obtiene el numero de tarjeta si existe
//            LOGGER.info("Voy a ejecutar SQL: " + query);
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(query);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                fechaProceso = rs.getString(1);
            }
            stmt.close();
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
                conn.rollback();
            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("CONEXION CERRADA");
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
            }
        }
        return fechaProceso;
    }

    @SuppressWarnings("finally")
    public static String[] getMarcaClase(String bin, String tipoTarj) {
        Connection conn = null;
        String[] marcaClase = new String[2];
        marcaClase[0] = "";
        marcaClase[1] = "";
        String query = "Select A.mtid, B.pmtid from GXBDBPS.nut001 A inner join GXBDBPS.nut002 B on A.mtid = B.mtid "
                + "inner join GXBDBPS.nut008 C on A.mtid = C.mtid where C.mtbinid = '" + bin + "' and B.pmttpo = '" + tipoTarj + "'";
        try {
            conn = DBUtils.connect();
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                marcaClase[0] = affectedRows.getString(1).trim();
                marcaClase[1] = affectedRows.getString(2).trim();
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            try {
                LOGGER.info("Error en " + DBUtils.class + ".getMarcaClase: " + ex);

                conn.rollback();

            } catch (SQLException ex1) {
                LOGGER.info("Error en " + DBUtils.class + ".getMarcaClase: " + ex1);
            }
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada"); //Ricardo Arce 042020
                return marcaClase;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getMarcaClase: " + ex);
            }
        }
        return marcaClase;
    }

    public static Boolean validarCorte() {
        Connection conn = null;
        boolean esCorte = false;
        String horario = "";
        String query = "SELECT FICHOPRO FROM GXBDBPS.MTDFIC WHERE FICNOENT like '%Continental%' AND FICNRENT = '021'";
//        Calendar c = Calendar.getInstance();
//        int horaActual = c.get(Calendar.HOUR_OF_DAY);
//        int minutoActual = c.get(Calendar.MINUTE);
        DateFormat dateFormat = new SimpleDateFormat("HHmm");
        Date ahora = new Date();
        String hora = dateFormat.format(ahora);
        LOGGER.info("La Hora actual: " + hora);
        try {
            conn = DBUtils.connect();
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();

            while (affectedRows.next()) {
                horario = affectedRows.getString(1).trim();
                String horaAumentada = aumentarHora(horario);
                Date horaConsulta, horaCorteInicio, horaCorteFinal;
                horaConsulta = dateFormat.parse(hora);
                horaCorteInicio = dateFormat.parse(horario);
//                LOGGER.info("horaCorteInicio: " + horaCorteInicio);
                horaCorteFinal = dateFormat.parse(horaAumentada);
//                LOGGER.info("horaCorteFinal: " + horaCorteFinal);
                if ((horaCorteInicio.compareTo(horaConsulta) <= 0) && (horaCorteFinal.compareTo(horaConsulta) >= 0)) {
                    esCorte = true;
                }
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            LOGGER.info("Error en " + DBUtils.class + ".getMarcaClase: " + ex);
        } finally {
            try {
                conn.close();
//                LOGGER.info("Conexión Cerrada");
                return esCorte;
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".getMarcaClase: " + ex);
            }
        }
        return esCorte;
    }

    @SuppressWarnings("finally")
    public static boolean existNroControl(String nroControl, String emisor, Connection conn) {
        boolean existeNroControl = false;
        String control = "";
        String query = "SELECT * FROM GXBDBPS.CONCTATAR WHERE conemiso = '" + emisor + "' and conumcon = '" + nroControl + "'";
        try {
            //Se obtiene el numero de control si existe
//            LOGGER.info("Voy a ejecutar SQL: " + query);
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(query);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                control = rs.getString(2);
            }
            stmt.close();
            if (!control.equals("") && control.trim() != null) {
                existeNroControl = true;
            }
        } catch (Exception ex) {
            LOGGER.info("Error en " + DBUtils.class + ".getResultSet: " + ex);
        }
        return existeNroControl;
    }

    @SuppressWarnings("finally")
    public static DatoTembsaf getDatosTembsaf(String nroTarjeta, Connection conn) {
        DatoTembsaf dato = new DatoTembsaf();
        String query = "SELECT * FROM GXBDBPS.TEMBSAF WHERE BSNUMTA ='" + nroTarjeta + "'";
//                    LOGGER.info("Query: " + query);
        try {
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();
            while (affectedRows.next()) {
                dato.setBSNUMTA(affectedRows.getString(1).trim());
                dato.setBSFEEMB(affectedRows.getString(2).trim());
                dato.setBSEMISO(affectedRows.getString(3).trim());
                dato.setBSBINES(affectedRows.getString(4).trim());
                dato.setBSAFINI(affectedRows.getString(5).trim());
                dato.setBSCODSC(affectedRows.getString(6).trim());
                dato.setBSNOVED(affectedRows.getString(7).trim());
                dato.setBSFEACT(affectedRows.getString(8).trim());
                dato.setBSCOEMB(affectedRows.getDouble(9));
                dato.setBSCUEMB(affectedRows.getInt(10));
                dato.setBSMCOBR(affectedRows.getString(11).trim());
                dato.setBSGENSF(affectedRows.getString(12).trim());
            }
            stmt.close(); //Ricardo Arce 07052020
            LOGGER.info("SE OBTUBIERON LOS DATOS DE TEMBSAF");
        } catch (Exception ex) {
            LOGGER.error("Error en " + DBUtils.class + ".getDatosTembsaf: " + ex);
        }
        return dato;
    }

    @SuppressWarnings("finally")
    public static int getCuotaActivacion(String bin, String emisor, String afinidad, String tipoTarjeta, String novedad, Connection conn) {
        int cuota = 0;
        String query = "";
        try {
            if (tipoTarjeta.equals("1") || tipoTarjeta.equals("4")) {
                if (novedad.equals("1")) {
                    query = "SELECT afcuemp FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("4") || novedad.equals("6") || novedad.equals("7")) {
                    query = "SELECT afcurep FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("2") || novedad.equals("3")) {
                    query = "SELECT afcurgp FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("5")) {
                    query = "SELECT afcupip FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                }
            } else if (tipoTarjeta.equals("3")) {
                if (novedad.equals("1")) {
                    query = "SELECT afcuema FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("4") || novedad.equals("6") || novedad.equals("7")) {
                    query = "SELECT afcurea FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("2") || novedad.equals("3")) {
                    query = "SELECT afcurga FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("5")) {
                    query = "SELECT afcupia FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                }
            }
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();
            while (affectedRows.next()) {
                cuota = affectedRows.getInt(1);
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            LOGGER.info("Error en " + DBUtils.class + ".getCuotaActivacion: " + ex);
        }
        return cuota;
    }

    @SuppressWarnings("finally")
    public static int getCostoActivacion(String bin, String emisor, String afinidad, String tipoTarjeta, String novedad, Connection conn) {
        int costo = 0;
        String query = "";
        try {
            if (tipoTarjeta.equals("1") || tipoTarjeta.equals("4")) {
                if (novedad.equals("1")) {
                    query = "SELECT afcoemp FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("4") || novedad.equals("6") || novedad.equals("7")) {
                    query = "SELECT afcorep FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("2") || novedad.equals("3")) {
                    query = "SELECT afcorgp FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("5")) {
                    query = "SELECT afcopip FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                }
            } else if (tipoTarjeta.equals("3")) {
                if (novedad.equals("1")) {
                    query = "SELECT afcoema FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("4") || novedad.equals("6") || novedad.equals("7")) {
                    query = "SELECT afcorea FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("2") || novedad.equals("3")) {
                    query = "SELECT afcurga FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                } else if (novedad.equals("5")) {
                    query = "SELECT afcorga FROM GXBDBPS.TAFINAF WHERE BIBINES LIKE '" + bin + "' AND ENEMISO LIKE '" + emisor + "' AND AFAFINI LIKE '" + afinidad + "' and afcobem = '002'";
                }
            }
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();
            while (affectedRows.next()) {
                costo = affectedRows.getInt(1);
            }
            stmt.close(); //Ricardo Arce 07052020
        } catch (Exception ex) {
            LOGGER.info("Error en " + DBUtils.class + ".getCostoActivacion: " + ex);
        }
        return costo;
    }

    @SuppressWarnings("finally")
    public static String getCodAutorizacion(String tarjeta, Connection conn) {
        String codigo = "0";
        String valorGenerado = "";
        boolean existeCod = true;
        //SE GENERA EL CODIGO DE FORMMA RANDOMICA
        Random r = new Random();
        int valorDado = r.nextInt(1000000);
        valorGenerado = String.format("%06d", valorDado);
        System.out.println(valorGenerado);
        try {
            while (existeCod) {
                String query = "SELECT CUCODAU FROM GXBDBPS.TCUOTAF WHERE MTNUMTA = '" + tarjeta + "' AND CUCODCO = '9999999' AND CUCODAU = '" + valorGenerado + "'";
                PreparedStatement stmt = null;
                stmt = /*this.*/ conn.prepareStatement(query);
                ResultSet affectedRows = stmt.executeQuery();
                while (affectedRows.next()) {
                    codigo = affectedRows.getString(1).trim();
                }
                stmt.close(); //Ricardo Arce 07052020
                if (codigo.equalsIgnoreCase("0")) {
                    codigo = valorGenerado;
                    existeCod = false;
                } else {
                    valorDado = r.nextInt(1000000);
                    valorGenerado = String.format("%06d", valorDado);
                }
            }
            LOGGER.info("CODIGO DE AUTORIZACION GENERADO " + codigo);
        } catch (Exception ex) {
            LOGGER.error("Error en " + DBUtils.class + ".generCodAutorizacion: " + ex);
        }
        return codigo;
    }

    @SuppressWarnings("finally")
    public static String getDescrMovimiento(String codMov, Connection conn) {
        String descripcion = "";
        String query = "SELECT CMDESCR FROM GXBDBPS.TCMOVAF WHERE CMCODIG = '" + codMov + "'";
        //            LOGGER.info("Query: " + query);
        try {
            PreparedStatement stmt = null;
            stmt = /*this.*/ conn.prepareStatement(query);
            ResultSet affectedRows = stmt.executeQuery();
            while (affectedRows.next()) {
                descripcion = affectedRows.getString(1).trim();
            }
            stmt.close(); //Ricardo Arce 07052020
//            LOGGER.info("SE OBTUBIERON LOS DATOS DE TEMBSAF");
        } catch (Exception ex) {
            LOGGER.error("Error en " + DBUtils.class + ".getDescrMovimiento: " + ex);
        }
        return descripcion;
    }

    @SuppressWarnings("finally")
    public static boolean existNroBin(String nroTarjeta) {
        boolean esBin = false;
        String binObtenido = "";
        String nroBin = nroTarjeta.substring(0, 6);
        String query = "SELECT BIBINES FROM GXBDBPS.TBINEAF WHERE ENEMISO = '021' AND BIBINES = '" + nroBin + "'";
        Connection conn = null;
        try {
            conn = DBUtils.connect();
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(query);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                binObtenido = rs.getString(1);
            }
            stmt.close();
            if (!binObtenido.equals("") && binObtenido.trim() != null) {
                esBin = true;
            }
        } catch (Exception ex) {
            LOGGER.info("Error en " + DBUtils.class + ".existNroBin: " + ex);
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                LOGGER.info("Error en " + DBUtils.class + ".existNroBin: " + ex);
            }
        }
        return esBin;
    }
}
