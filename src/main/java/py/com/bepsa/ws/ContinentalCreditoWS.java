/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.com.bepsa.ws;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.xml.bind.annotation.XmlElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import py.com.bepsa.clases.ArchivoCuenta;
import py.com.bepsa.clases.ArchivoTarjeta;
import py.com.bepsa.clases.Cuenta;
import py.com.bepsa.clases.Tarjeta;
import py.com.bepsa.pojo.BloqueoWS;
import py.com.bepsa.pojo.DatoBloqueo;
import py.com.bepsa.pojo.DatoCliente;
import py.com.bepsa.utils.DBUtils;
import static py.com.bepsa.utils.DBUtils.existCliente;
import static py.com.bepsa.utils.DBUtils.getDatosCliente;
import static py.com.bepsa.utils.DBUtils.getFechaComercial;
import static py.com.bepsa.utils.DBUtils.validarCorte;
import py.com.bepsa.utils.ErrorCuenta;
import py.com.bepsa.utils.ErrorTarjeta;
import py.com.bepsa.utils.ErrorUtils;
import py.com.bepsa.utils.Utils;
import static py.com.bepsa.utils.Utils.modificarFormatoFecha;
import static py.com.bepsa.utils.Utils.verificarDia;
import static py.com.bepsa.utils.Utils.verificarHorario;

/**
 *
 * @author rarce
 */
@WebService(serviceName = "ContinentalCreditoWS")
@SOAPBinding(style = Style.RPC)
public class ContinentalCreditoWS {

    public ContinentalCreditoWS() {
        Utils.obtenerPropiedades();
    }

    private static final Logger LOGGER = LogManager.getLogger(ContinentalCreditoWS.class);

    @WebMethod(operationName = "abmCuentaTarjeta")
    public String abmCuentaTarjeta(@WebParam(name = "usuario") @XmlElement(required = true) String usuario,
            @WebParam(name = "clave") @XmlElement(required = true) String clave,
            @WebParam(name = "archCuenta") @XmlElement(required = true) ArchivoCuenta archCuenta,
            @WebParam(name = "archTarjeta") @XmlElement(required = true) ArchivoTarjeta archTarjeta,
            @WebParam(name = "opcion") @XmlElement(required = true) String opcion) {

//        PropertyConfigurator.configure("LOGGER.properties");
        String retorno = "";
        LOGGER.info("------------------------------------------------");
        LOGGER.info("INICIA ABM CONTINENTAL - Credito - " + new Date());

        if (usuario.equals(Utils.usrConti) && clave.equals(Utils.passConti)) {
            LOGGER.info("USUARIO VALIDADO");
            if (verificarDia()) {
                LOGGER.info("DIA VALIDADO");
                if (verificarHorario()) {
                    LOGGER.info("HORARIO VALIDADO");
                    if (!validarCorte()) {
                        if (opcion.equals("C") && !archCuenta.getCuentas().isEmpty()) {
                            retorno = procesarCta(archCuenta, usuario);
                        } else if (opcion.equals("T") && !archTarjeta.getTarjetas().isEmpty()) {
                            retorno = procesarTarj(archTarjeta, usuario);
                        } else if (opcion.equals("A") && !archCuenta.getCuentas().isEmpty() && !archTarjeta.getTarjetas().isEmpty()) {
                            retorno = procesarCta(archCuenta, usuario);
                            retorno = procesarTarj(archTarjeta, usuario);
                        } else {
                            retorno = "Opción debe ser C, T, A";
                            LOGGER.info(retorno);
                        }
                    } else {
                        retorno = "Corte procesadora sin terminar";
                        LOGGER.info(retorno);
                    }
                } else {
                    retorno = "Horario no válido p/Transf.";
                    LOGGER.info(retorno);
                }
            } else {
                retorno = "Hoy no es un día hábil";
                LOGGER.info(retorno);
            }
        } else {
            retorno = "Usuario no autorizado para WS";
            LOGGER.info(retorno);
        }
        LOGGER.info("FINALIZA ABM CONTINENTAL");
        LOGGER.info("------------------------------------------------");
        LOGGER.info("Respuesta: " + retorno);
        LOGGER.info("");
        return retorno;
    }

    private String procesarCta(ArchivoCuenta cuentas, String usuario) {
        String retorno = "";
        Connection conn = null;
        try {
            Class.forName(Utils.driver);
            conn = DriverManager.getConnection(Utils.url, Utils.usrAS400, Utils.passAS400);
            conn.setAutoCommit(false);
            LOGGER.info("SE INICIA LA CONEXION A LA BD");
            long secTrxC = setTrx(cuentas.getCuentas().size() + "", "2");
            for (Cuenta cuenta : cuentas.getCuentas()) {
                //logEntradaCta(cuenta);
                retorno = "";
                ErrorCuenta ctaError = new ErrorCuenta();
                ctaError = ErrorUtils.validarDatosCta(cuenta);
                String datosEntrada = ErrorUtils.armaDatosCuenta(cuenta);
                //LOGGER.info(ctaError.getBandera());
                if (!ctaError.getBandera()) {
                    //Se verifica el usuario
                    if (cuenta.getUserActualiza().equals("") || cuenta.getUserActualiza() == null) {
                        cuenta.setUserActualiza(usuario.toUpperCase());
                    }
                    //Generacion de cuenta 
                    if (cuenta.getNroCuenta().trim().isEmpty() || cuenta.getNroCuenta() == null) {
                        if (verificarAfinidad(cuenta.getCodAfin(), conn)) {
                            retorno = altaCuenta(cuenta, conn, secTrxC, datosEntrada);
                        } else {
                            retorno += "36;";
                            LOGGER.info("CODIGO DE AFINIDAD INVALIDO");
                            setDetalleTrxC(cuenta, retorno, secTrxC, datosEntrada, conn);
                            retorno = "Proceso submitido";
                        }
                    } else {
                        try {
                            //Se extrae la cuenta si es que existe         
                            String consulCta = "";
                            String consulStat = "";
                            String consulDoc = "";
                            String consulCdSuc = "";
                            String str = "select mcnumct, mcstats, mcnumdo, mccodsc from Gxbdbps.tmctaaf where mcnumct = " + cuenta.getNroCuenta();
                            PreparedStatement stmt = null;
                            stmt = conn.prepareStatement(str);
                            stmt.executeQuery();
                            ResultSet rs = stmt.getResultSet();
                            while (rs.next()) {
                                consulCta = rs.getString(1).trim();
                                consulStat = rs.getString(2).trim();
                                consulDoc = rs.getString(3).trim();
                                consulCdSuc = rs.getString(4).trim();
                            }
                            stmt.close();
                            //Alta de cuenta 
                            if (consulCta.isEmpty()) {
                                if (verificarAfinidad(cuenta.getCodAfin(), conn)) {
                                    retorno = altaCuenta(cuenta, conn, secTrxC, datosEntrada);
                                } else {
                                    retorno += "36;";
                                    LOGGER.info("CODIGO DE AFINIDAD INVALIDO");
                                    setDetalleTrxC(cuenta, retorno, secTrxC, datosEntrada, conn);
                                    retorno = "Proceso submitido";
                                }
                                //Modificacion de cuenta 
                            } else if ((cuenta.getSituacion().isEmpty() || cuenta.getSituacion() == null) && !consulCta.isEmpty() && !cuenta.getCostNoAplica().toUpperCase().contains("BOR")
                                    && !cuenta.getCargNoAplica().toUpperCase().contains("BOR") && !cuenta.getDirExtr2().toUpperCase().equals("BORRAR")
                                    && !cuenta.getDirExtr3().toUpperCase().equals("BORRAR")) {
                                retorno = modificarCuenta(cuenta, conn, secTrxC, datosEntrada);
                                //Cambio de estado
                            } else if (!cuenta.getSituacion().isEmpty() && !consulCta.isEmpty() && !cuenta.getDirExtr2().toUpperCase().equals("BORRAR")
                                    && !cuenta.getDirExtr3().toUpperCase().equals("BORRAR") && !cuenta.getCostNoAplica().toUpperCase().contains("BOR") && !cuenta.getCargNoAplica().toUpperCase().contains("BOR")) {
                                retorno = cambiarEstadoCta(cuenta, conn, secTrxC, datosEntrada);
                                //Borrado Dirección de la Cuenta
                            } else if (!consulCta.isEmpty() && (cuenta.getDirExtr2().toUpperCase().equals("BORRAR") || cuenta.getDirExtr3().toUpperCase().equals("BORRAR"))) {
                                retorno = borrarDirCta(cuenta, conn, secTrxC, datosEntrada);
                                //Borrado Excepción de Costo
                            } else if (!consulCta.isEmpty() && cuenta.getCostNoAplica().toUpperCase().contains("BOR")) {
                                retorno = borrarCostoCta(cuenta, conn, secTrxC, datosEntrada);
                                //Borrado Excepción de Cargos
                            } else if (!consulCta.isEmpty() && cuenta.getCargNoAplica().toUpperCase().contains("BOR")) {
                                retorno = borrarCargoCta(cuenta, conn, secTrxC, datosEntrada);
                            }

                        } catch (Exception ex) {
                            LOGGER.error("ERROR: " + ex);
                            retorno = "ERROR AL PROCESAR LA SOLICITUD";
                            LOGGER.info(retorno);
                        }
                    }
                } else {
                    LOGGER.info("ERRORES DE ENTRADA DE DATOS CUENTA: " + ctaError.getErrores());
                    setDetalleTrxC(cuenta, ctaError.getErrores(), secTrxC, datosEntrada, conn);
                    retorno = "Proceso submitido";
                }
            }
            //LOGGER.info("Retorno: " + retorno);
            conn.commit();
            conn.close();
            LOGGER.error("CONEXION CERRADA");
            //retorno = "Proceso submitido";
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            retorno = "ERROR AL PROCESAR LA SOLICITUD";
            LOGGER.info(retorno);
            try {
                conn.rollback();
                conn.close();
                LOGGER.error("CONEXION CERRADA");
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        return retorno;
    }

    private String procesarTarj(ArchivoTarjeta tarjetas, String usuario) {
        String retorno = "";
        Connection conn = null;
        try {
            Class.forName(Utils.driver);
            conn = DriverManager.getConnection(Utils.url, Utils.usrAS400, Utils.passAS400);
            conn.setAutoCommit(false);
            LOGGER.info("SE INICIA LA CONEXION A LA BD");
            long secTrxT = setTrx(tarjetas.getTarjetas().size() + "", "1");
            for (Tarjeta tarjeta : tarjetas.getTarjetas()) {
                //logEntradaTarj(tarjeta);
                retorno = "";
                ErrorTarjeta tarjError = new ErrorTarjeta();
                tarjError = ErrorUtils.validarDatosTarj(tarjeta);
                String datosEntrada = ErrorUtils.armaDatosTarjeta(tarjeta);
                if (!tarjError.getBandera()) {
                    //Se verifica el usuario
                    if (tarjeta.getUserActualiza().trim().equals("") || tarjeta.getUserActualiza() == null) {
                        tarjeta.setUserActualiza(usuario.toUpperCase());
                    }
                    //Generacion de tarjetas
                    if (tarjeta.getNroTarjeta().trim().isEmpty() || tarjeta.getNroTarjeta() == null) {
                        if (verificarAfinidad(tarjeta.getIdAfin(), conn)) {
                            if (!tarjeta.getNroControl().isEmpty() && tarjeta.getNroTarjeta().isEmpty()) {
                                retorno = altaTarjeta(tarjeta, conn, secTrxT, datosEntrada);
                            } else if (verificarBin(tarjeta.getNroTarjeta())) {
                                retorno = altaTarjeta(tarjeta, conn, secTrxT, datosEntrada);
                            } else {
                                retorno += "174;";
                                LOGGER.info("ENTIDAD O NUMERO DE BIN INCORRECTO");
                                setDetalleTrxT(tarjeta, retorno, secTrxT, datosEntrada, conn);
                                retorno = "Proceso submitido";
                            }
                        } else {
                            retorno += "36;";
                            LOGGER.info("CODIGO AFINIDAD INVALIDO");
                            setDetalleTrxT(tarjeta, retorno, secTrxT, datosEntrada, conn);
                            retorno = "Proceso submitido";
                        }
                    } else {
                        try {
                            //Se extrae la tarjeta si existe
                            String consulTarj = "";
                            String str = "select mtnumta from Gxbdbps.tmtaraf where mtnumta = " + tarjeta.getNroTarjeta();
                            PreparedStatement stmt = null;
                            stmt = conn.prepareStatement(str);
                            stmt.executeQuery();
                            ResultSet rs = stmt.getResultSet();
                            while (rs.next()) {
                                consulTarj = rs.getString(1);
                            }
                            stmt.close();
                            //Alta de tarjetas
                            if (consulTarj.trim().isEmpty() || consulTarj.trim().equals("")) {
                                if (verificarAfinidad(tarjeta.getIdAfin(), conn)) {
                                    if (verificarBin(tarjeta.getNroTarjeta())) {
                                        retorno = altaTarjeta(tarjeta, conn, secTrxT, datosEntrada);
                                    } else {
                                        retorno += "174;";
                                        LOGGER.info("ENTIDAD O NUMERO DE BIN INCORRECTO");
                                        setDetalleTrxT(tarjeta, retorno, secTrxT, datosEntrada, conn);
                                        retorno = "Proceso submitido";
                                    }
                                } else {
                                    retorno += "36;";
                                    LOGGER.info("CODIGO AFINIDAD INVALIDO");
                                    setDetalleTrxT(tarjeta, retorno, secTrxT, datosEntrada, conn);
                                    retorno = "Proceso submitido";
                                }
                                //Modificacion de tarjetas
                            } else if (tarjeta.getSituacion().isEmpty()) {
                                retorno = modificarTarjeta(tarjeta, conn, secTrxT, datosEntrada);
                                //Cambio estado de tarjetas
                            } else if (!tarjeta.getSituacion().isEmpty()) {
                                retorno = cambiarEstadoTarjeta(tarjeta, conn, secTrxT, datosEntrada);
                            }
                        } catch (Exception ex) {
                            LOGGER.error("ERROR: " + ex);
                            retorno = "ERROR AL PROCESAR LA SOLICITUD";
                            LOGGER.info(retorno);
                        }
                    }
                } else {
                    LOGGER.info("ERRORES DE ENTRADA DE DATOS TARJETA: " + tarjError.getErrores());
                    setDetalleTrxT(tarjeta, tarjError.getErrores(), secTrxT, datosEntrada, conn);
                    retorno = "Proceso submitido";
                }
            }
            //LOGGER.info("Retorno: " + retorno);
            conn.commit();
            conn.close();
            LOGGER.error("CONEXION CERRADA");
            //retorno = "Proceso submitido";
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            retorno = "ERROR AL PROCESAR LA SOLICITUD";
            LOGGER.info(retorno);
            try {
                conn.rollback();
                conn.close();
                LOGGER.error("CONEXION CERRADA");
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        return retorno;
    }

    private String altaCuenta(Cuenta cta, Connection conn, long secTrx, String datosEntrada) {

        String fecha = Utils.obtenerFechaHora("yyyyMMdd");
        String hora = Utils.obtenerFechaHora("HHmm");
        Date fechDate = new Date();
        Timestamp fechaHora = new Timestamp(fechDate.getTime());
        String tipoDoc = mapearTipoDoc(cta.getTipoDoc());
        String tipoDocCod = mapearTipoDoc(cta.getTipDocCod());
        String nombresCli = cta.getNombre1() + " " + cta.getNombre2();
        String apellidosCli = cta.getApellido1() + " " + cta.getApellido2();
        String nombreApellido = (cta.getNombre1() + " " + cta.getNombre2()).trim() + ", " + cta.getApellido1() + " " + cta.getApellido2();
        String nombreApellidoCod = (cta.getNombre1Cod() + " " + cta.getNombre2Cod()).trim() + ", " + cta.getApellido1Cod() + " " + cta.getApellido2Cod();
        String retener = mapearReten(cta.getRetenExtr());
        String retorno = "";
        String[] cuentaTarjeta = new String[2];
        LOGGER.info("---------- INICIA ALTA DE CUENTA ----------");
        LOGGER.info("SECUENCIA TRANSACCION: " + secTrx);
        try {
            String fechaNac = modificarFormatoFecha(cta.getFechaNac());
            String fechaNacCod = modificarFormatoFecha(cta.getFechNacCod());
            //Se obtiene los datos del cliente si existe
            String clienteSQL = "select cenumdo from Gxbdbps.gclieaf where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "'";
            //LOGGER.info("clienteSQL");
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(clienteSQL);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            String ciCliente = "";
            while (rs.next()) {
                ciCliente = rs.getString(1);
            }
            stmt.close();
            //Se verifica si se obtuvo el cliente 
            //LOGGER.info("CI Cliente: " + ciCliente);
            if (ciCliente.isEmpty() || ciCliente.trim().equals("")) {
                //Verificar que traiga el primer nombre y primer apellido
                if (cta.getNombre1().trim().isEmpty() || cta.getApellido1().trim().isEmpty()) {
                    retorno += "5;9;";
                    LOGGER.info("Nombre 1 y Apellido 1 del titular de la cuenta vacios");
                    setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                    retorno = "Proceso submitido";
                    return retorno;
                }
                int cantDire = 0;
                if (!cta.getDirRecibo().isEmpty()) {
                    cantDire++;
                }
                if (!cta.getDirExtr1().isEmpty()) {
                    cantDire++;
                }
                if (!cta.getDirExtr2().isEmpty()) {
                    cantDire++;
                }
                if (!cta.getDirExtr3().isEmpty()) {
                    cantDire++;
                }
                if (!cta.getDirEmailPer().isEmpty() || !cta.getNroCel().isEmpty()) {
                    cantDire++;
                }
                //para verificar cod profesion
                String profesion = Utils.mapearOcupacion(cta.getOcupacion());
                //Si cliente no existe se insertan los datos
                String insertCliSQL = "insert into Gxbdbps.gclieaf values('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', '" + cta.getNombre1() + "', '" + cta.getNombre2()
                        + "', '" + cta.getApellido1() + "', '" + cta.getApellido2() + "', '" + cta.getRucExtr() + "', '" + cta.getSexo() + "', '" + cta.getEstadoCivil()
                        + "', '" + fechaNac + "', '" + cta.getPaisDoc() + "', '', '', '', '" + profesion + "', 'P', 0.00, " + cantDire + ", '', '" + nombreApellido
                        + "', '" + fecha + "', '" + hora + "', '" + cta.getUserActualiza() + "', '', '" + cta.getNroSocio() + "')";
                if (DBUtils.ejecucionSQL(insertCliSQL, conn)) {
                    LOGGER.info("CLIENTE INSERTADO CORRECTAMENTE");
                }
                //Se obtiene la fecha comercial
                String fechaComercial = getFechaComercial();
                //Se inserta el nuevo cliente auditoria
                String insertAudCliSQL = "insert into Gxbdbps.aucliaf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 1,'A', 1, '" + cta.getUserActualiza() + "', "
                        + "0, 'Alta Cliente', '', '', 0, '" + fecha + "', " + hora + ")";
                if (DBUtils.ejecucionSQL(insertAudCliSQL, conn)) {
                    LOGGER.info("AUDITORIA CLIENTE INSERTADO CORRECTAMENTE");
                }

                //Se obtiene el nombre del departamento si se envia
                String depar = "";
                if (!cta.getDepart().isEmpty() || !cta.getDepart().equals("")) {
                    String dp = String.format("%03d", Integer.parseInt(cta.getDepart()));
                    String consDeparSQL = "select codtodesc from Gxbdbps.codtoaf where coddto = '" + dp + "'";
                    PreparedStatement stmt1 = null;
                    stmt1 = conn.prepareStatement(consDeparSQL);
                    stmt1.executeQuery();
                    ResultSet rs1 = stmt1.getResultSet();
                    while (rs1.next()) {
                        depar = rs1.getString(1);
                    }
                }
                String depCdad = depar.trim() + "/Cod. Ciudad " + cta.getCiudad().trim();
                String zona = "Cod. Zona " + cta.getZona();

                //Se insertan las direcciones del cliente
                String insertDirSQL = "insert into gxbdbps.gdireaf values";
                if (!cta.getDirRecibo().isEmpty() || !cta.getNroTel().isEmpty()) {
                    insertDirSQL += "('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, '" + cta.getDirRecibo() + "', 'S', 'O', '" + cta.getNroTel()
                            + "', '" + depCdad + "', '" + zona + "'),";
                }

                if (!cta.getDirExtr1().isEmpty()) {
                    insertDirSQL += "('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 1, '" + cta.getDirExtr1() + "', 'S', 'O', '', '" + depCdad + "', '" + zona + "'),";
                }

                if (!cta.getDirExtr2().isEmpty()) {
                    insertDirSQL += "('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 2, '" + cta.getDirExtr2() + "', 'S', 'O', '', '"
                            + depCdad + "', '" + zona + "'),";
                }

                if (!cta.getDirExtr3().isEmpty()) {
                    insertDirSQL += "('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 3, '" + cta.getDirExtr3() + "', 'S', 'O', '', '" + depCdad
                            + "', '" + zona + "'),";
                }
                if (!cta.getDirEmailPer().isEmpty() || !cta.getNroCel().isEmpty()) {
                    insertDirSQL += "('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 4, '" + cta.getDirEmailPer() + "', 'S', 'E', '" + cta.getNroCel() + "', '', ''),";
                }
//                LOGGER.info("Inserta de direcciones: " + insertDirSQL);
                if (insertDirSQL.length() > 35) {
                    insertDirSQL = insertDirSQL.substring(0, insertDirSQL.length() - 1);
                    if (DBUtils.ejecucionSQL(insertDirSQL, conn)) {
                        LOGGER.info("DIRECCIONES DEl CLIENTE INSERTADAS CORRECTAMENTE");
                    }
                } else {
                    retorno += "162;";
                    LOGGER.info("Direccion/Nro telefono/Nro Celular para la cuenta vacio");
                    try {
                        conn.rollback();
                        setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                    } catch (Exception sqlex) {
                        LOGGER.error("ERROR: " + sqlex);
                    }
                    retorno = "Proceso submitido";
                    return retorno;

                }

                String insAudCliDirSQLAudi = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, 0, 'A', 1,"
                        + " '" + cta.getUserActualiza() + "', 0, 'Alta Direcciones', '', '', 0,'" + fecha + "', " + hora + ")";
                if (DBUtils.ejecucionSQL(insAudCliDirSQLAudi, conn)) {
                    LOGGER.info("AUDITORIA DE DIRECCIONES DEl CLIENTE INSERTADAS CORRECTAMENTE");
                }
            }
            //Se verifica si ya existe oficial de cuenta
            String nroOfiCta = "000";
            if (!cta.getNroDocOfi().isEmpty() && !cta.getNroDocOfi().equals("")) {
                String ofiCtaSQL = "select ecidusr, eccodig from Gxbdbps.tectaaf where enemiso = '021' and ecidusr = '" + cta.getNroDocOfi() + "'";
                PreparedStatement stmt22 = null;
                stmt22 = conn.prepareStatement(ofiCtaSQL);
                stmt22.executeQuery();
                ResultSet rs22 = stmt22.getResultSet();
                String consOfiCta = "";
//                String nroOfiCta = "";
                while (rs22.next()) {
                    consOfiCta = rs22.getString(1);
                    nroOfiCta = rs22.getString(2);
                }
                stmt22.close();
                if (consOfiCta.isEmpty()) {
                    String maxOfiCtaSQL = "select max(eccodig) from Gxbdbps.tectaaf where enemiso = '021'";
                    long secOfiCta = DBUtils.getSecuencia(maxOfiCtaSQL, conn);
                    nroOfiCta = String.format("%03d", secOfiCta);

                    String nombreOfi = cta.getNombre1Ofi() + " " + cta.getApellido1Ofi();
                    //Se guarda al final con el nro de documento del oficial de cuenta
                    String insOfiCtaSQL = "insert into Gxbdbps.tectaaf values('021', '" + nroOfiCta + "', '" + nombreOfi + "', '" + fecha + "', '00000000', '" + cta.getNroDocOfi() + "')";
                    if (DBUtils.ejecucionSQL(insOfiCtaSQL, conn)) {
                        LOGGER.info("OFICIAL DE CUENTA INSERTADO CORRECTAMENTE");
                    }

                    //Se inserta nuevo oficial en la tabla de oficiales y promotores
                    String insOfiSQL = "insert into Gxbdbps.tmanencta values('021', '" + nroOfiCta + "', '" + cta.getNroDocOfi() + "', '" + fecha + "', '" + cta.getUserActualiza()
                            + "', '" + fecha + "', '', '" + fechaHora + "', 'S', 'N')";
                    boolean ofi = DBUtils.ejecucionSQL(insOfiSQL, conn);
                }
            }

            //Se verifica si se envio un nro de cuenta
            if (cta.getNroCuenta().isEmpty() || cta.getNroCuenta() == null) {
                //Se genere un nro de cuenta
                cuentaTarjeta = generarCtaTarj("021", "627431", cta.getCodAfin(), "S", "N", conn).split(",");
                cta.setNroCuenta(cuentaTarjeta[0]);
                //tarjeta.setNroTarjeta(cuentaTarjeta[1]);
                //LOGGER.info("Nro de cuenta creado: " + cta.getNroCuenta());
                //LOGGER.info("Nro de tarjeta creado: " + cuentaTarjeta[1]);
            } else if (verificarCta(cta.getNroCuenta(), conn)) {
                retorno += "163;";
                LOGGER.info("NUMERO DE CUENTA DUPLICADO");
                try {
                    conn.rollback();
                    setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
                retorno = "Proceso submitido";
                return retorno;
            }

            //Se verifica si se personaliza la tasa de interes
            String intNormal = "0";
            String intMoratorio = "0";
            String intPunitorio = "0";
            if (cta.getPersTTI().equals("N")) {
                //Se obtiene las tasas de interes
                String interesSQL = "select kftasa from Gxbdbps.tcconaf where enemiso = '021' and bibines = '627431' and afafini = '" + cta.getCodAfin() + "' and kftasa > 0 and cmcodig in (150, 152, 153)";
                PreparedStatement stmt8 = null;
                stmt8 = conn.prepareStatement(interesSQL);
                stmt8.executeQuery();
                ResultSet rs8 = stmt8.getResultSet();
                String[] arrayInt = new String[3];
                int i = 0;
                while (rs8.next()) {
                    arrayInt[i] = rs8.getString(1);
                    i++;
                }
                intNormal = arrayInt[0];
                intMoratorio = arrayInt[1];
                intPunitorio = arrayInt[2];
                stmt8.close();
                String insCorrMorCom = "";
                insCorrMorCom += "insert into Gxbdbps.tccctaf values(" + cta.getNroCuenta() + ", '150', " + intNormal + ")";
                insCorrMorCom += ",(" + cta.getNroCuenta() + ", '152', " + intMoratorio + ")";
                if (DBUtils.ejecucionSQL(insCorrMorCom, conn)) {
                    LOGGER.info("TASAS DE INTERESES CREADAS CORRECTAMENTE");
                }

            } else if (cta.getPersTTI().equals("S")) {
                //Se inserta los intereses personalizados
                String insCorrMorCom = "";
                insCorrMorCom += "insert into Gxbdbps.tccctaf values(" + cta.getNroCuenta() + ", '" + cta.getCodTTICorriente() + "', " + cta.getTICorriente() + ")";
                insCorrMorCom += ",(" + cta.getNroCuenta() + ", '" + cta.getCodTTIMora() + "', " + cta.getTIMora() + ")";
                if (DBUtils.ejecucionSQL(insCorrMorCom, conn)) {
                    LOGGER.info("TASAS DE INTERESES CREADAS CORRECTAMENTE");
                }
                intNormal = cta.getCodTTICorriente();
                intMoratorio = cta.getCodTTIMora();
                intPunitorio = "0";
            } else {
                retorno += "74;";
                LOGGER.info("PERSONALIZA TIPO TASA INTERÉS S/N VACIO");
                try {
                    conn.rollback();
                    setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
                retorno = "Proceso submitido";
                return retorno;
            }

            //Se verifica si se personaliza pago minimo
            String porcenPagoMin = "0";
            String montPagoMinFij = "0";
            String interes2SQL = "select afintcu, afintca, afporpg from Gxbdbps.tafinaf where enemiso = '021' and bibines = '627431' and afafini = '" + cta.getCodAfin().trim() + "'";
            //LOGGER.error(interes2SQL);
            PreparedStatement stmt10 = null;
            stmt10 = conn.prepareStatement(interes2SQL);
            stmt10.executeQuery();
            ResultSet rs10 = stmt10.getResultSet();
            String intCuota = "";
            String intCuotATM = "";
            while (rs10.next()) {
                intCuota = rs10.getString(1);
                intCuotATM = rs10.getString(2);
                porcenPagoMin = rs10.getString(3);
            }
            stmt10.close();
            if (cta.getPersPagMin().toUpperCase().equals("S")) {
                porcenPagoMin = cta.getPorcPagMin();
                montPagoMinFij = cta.getImpoFijoPagMin();

                if (montPagoMinFij.trim().isEmpty() || montPagoMinFij.equals("")) {
                    montPagoMinFij = "0";
                }
            }
            String sucursal = obtenerSucursal(cta.getCodSuc(), conn, fecha, cta.getUserActualiza(), cta.getCodAfin());
            String tipoCtaB = (!cta.getTipCtaBanc().isEmpty()) ? mapearCtaB(Integer.parseInt(cta.getTipCtaBanc())) : "";
            String formPago = mapearFormP(cta.getModPago());
            String cobraCosto = mapearCobroCosto(cta.getCobCosto());
            String tipoDocOfi = mapearTipoDoc(cta.getTipDocOfCta());
            //ARREGLAR
            if (!cta.getTipLin1Norm().equals("1")) {
                cta.setLinCredNorm("0");
            }
            if (!cta.getTipLin2Cuota().equals("2")) {
                cta.setLinCredCuota("0");
            }
            //Se inserta un nuevo registro de cuenta
            //LOGGER.error("llego 300");
            String insertCtaSQL = "insert into Gxbdbps.tmctaaf values(" + cta.getNroCuenta() + ", '1', '" + tipoCtaB + "', '" + cta.getPaisDoc() + "', " + cta.getLinCredNorm()
                    + ", " + cta.getLinCredCuota() + ", " + 0 + ", " + 0 + ", " + 0 + "," + 0 + ", " + cta.getLinCredNorm() + ", " + cta.getLinCredCuota() + ", 0, 0, 0, 0, 0, 0, 0, 0"
                    + ", '" + formPago + "', '" + cta.getTipPago() + "', '" + cta.getCtaBanc() + "', '00000000', '00000000', '00000000', '00000000', '" + fecha + "', " + intNormal
                    + ", " + intMoratorio + ", " + intPunitorio + ", " + intCuota + ", " + intCuotATM + ", " + porcenPagoMin + ", '" + nroOfiCta + "', '00000000', '00000000', 'CON', 0"
                    + ", 0, 0, 0, 0, 0, 0, 0, '', '', '00000000', " + montPagoMinFij + ", '00000000', 0, 0, 0, 0, 0, 0, 0, 0, 0, '', 0, 0, 0, 0, 0, 0, 0, 0, '" + cobraCosto
                    + "', '021', '" + cta.getCodAfin() + "', '" + sucursal + "', '', '', 0, 0, '00000000', '00000000', 0, '00000000', '', '', '', '" + tipoDoc + "', '" + cta.getNroDoc()
                    + "', 0, 0, 0, 0, 0, '627431', 0)";
            //LOGGER.info(insertCtaSQL);
            if (DBUtils.ejecucionSQL(insertCtaSQL, conn)) {
                LOGGER.info("CUENTA INSERTADA CORRECTAMENTE");
            }

            //Se obtiene la fecha comercial
            String fechaComercial = getFechaComercial();
            //Se inserta auditoria de la cuenta
            String insertAudCtaSQL = "insert into Gxbdbps.auctaaf values('" + fechaComercial + "', '021', '" + cta.getCodAfin() + "', " + cta.getNroCuenta() + ", 1, 'A', 1, '"
                    + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Alta Cuenta', '', '', 0, '" + sucursal + "')";
            if (DBUtils.ejecucionSQL(insertAudCtaSQL, conn)) {
                LOGGER.info("AUDITORIA CUENTA INSERTADA CORRECTAMENTE");
            }

            //Se inserta calificacion del cliente
            if (!cta.getCalifBCP().isEmpty()) {
                String fechCalif = Utils.obtenerUltimoDiaMes();
                String inserCalBNF = "insert into gxbdbps.tgtclf  values('" + cta.getNroDoc() + "', '" + fechCalif + "', '" + nombresCli + "', '" + apellidosCli + "', '" + cta.getCalifBCP() + "', " + fecha
                        + ", '" + cta.getUserActualiza() + "')";
                if (DBUtils.ejecucionSQL(inserCalBNF, conn)) {
                    LOGGER.info("CALIFICACION INSERTADA CORRECTAMENTE");
                }

            }
            //Se obtiene los datos del cliente codeudor si existe
            if (!tipoDocCod.isEmpty() && !cta.getNroDocCod().isEmpty()) {
                String codeudorSQL = "select cenumdo from Gxbdbps.gclieaf where enemiso = '021' and cetipod = '" + tipoDocCod + "' and cenumdo = '" + cta.getNroDocCod() + "'";
                PreparedStatement stmt14 = null;
                stmt14 = conn.prepareStatement(codeudorSQL);
                stmt14.executeQuery();
                ResultSet rs14 = stmt14.getResultSet();
                String codeuCliente = "";
                while (rs14.next()) {
                    codeuCliente = rs14.getString(1);
                }
                stmt14.close();
                //Se verifica si se obtuvo el cliente codeudor
                int cantMailCod = 0;
                //LOGGER.info("Codeudor cliente: " + codeuCliente);
                if (codeuCliente.isEmpty()) {
                    if (!cta.getDirEmailCod().isEmpty()) {
                        cantMailCod++;
                    }
                    String profesionCod = Utils.mapearOcupacion(cta.getOcupacionCod());
                    //Si cliente codeudor no existe se insertan los datos
                    String insCliCodSQL = "insert into Gxbdbps.gclieaf values('021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', '" + cta.getNombre1Cod() + "', '" + cta.getNombre2Cod()
                            + "', '" + cta.getApellido1Cod() + "', '" + cta.getApellido2Cod() + "', '', '" + cta.getSexoCod() + "', '" + cta.getEstCivCod()
                            + "', '" + fechaNacCod + "', '', '', '', '', '" + profesionCod + "', 'P', 0.00, " + cantMailCod + ", '', '" + nombreApellidoCod
                            + "', '" + fecha + "', '" + hora + "', '" + cta.getUserActualiza() + "', '', '')";
                    if (DBUtils.ejecucionSQL(insCliCodSQL, conn)) {
                        LOGGER.info("CLIENTE CODEUDOR INSERTADO CORRECTAMENTE");
                    }

                    //Se inserta el nuevo cliente auditoria
                    String insertAudCliSQL = "insert into Gxbdbps.aucliaf values('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', 1,'A', 1, '" + cta.getUserActualiza() + "', "
                            + "0, 'Alta Cliente', '', '', 0, '" + fecha + "', " + hora + ")";
                    if (DBUtils.ejecucionSQL(insertAudCliSQL, conn)) {
                        LOGGER.info("AUDITORIA CLIENTE CODEUDOR INSERTADO CORRECTAMENTE");
                    }

                    //Se insertan las direcciones del cliente codeudor
                    if (!cta.getDirEmailPer().isEmpty() || !cta.getNroCel().isEmpty()) {
                        String insertDirCodSQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', 4, '" + cta.getDirEmailCod()
                                + "', 'S', 'E', '', '', '')";
                        if (DBUtils.ejecucionSQL(insertDirCodSQL, conn)) {
                            LOGGER.info("DIRECCIONES CLIENTE CODEUDOR INSERTADO CORRECTAMENTE");
                        }

                        String insAudCliDirSQLAudi = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', 4, 1, 'A', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Alta Direcciones', '', '', 0,'" + fecha + "', " + hora + ")";
                        if (DBUtils.ejecucionSQL(insAudCliDirSQLAudi, conn)) {
                            LOGGER.info("DIRECCIONES CLIENTE CODEUDOR INSERTADO CORRECTAMENTE");
                        }
                    }
                }
            }

            //Se cargan las excepciones de costo si vienen
            String costoNoAplica = cta.getCostNoAplica().trim();
            if (!costoNoAplica.isEmpty() && costoNoAplica.length() != 0) {
                for (int i = 0; i < costoNoAplica.length(); i += 3) {
                    String idCosto = costoNoAplica.substring(i, i + 3);
                    String insCosNoApl = "insert into Gxbdbps.tcuenocob values(" + cta.getNroCuenta() + ", " + idCosto + ", '" + cta.getUserActualiza() + "', '" + fecha + "', '" + cta.getAplicaImp()
                            + "', '', '')";
                    //LOGGER.info(insCosNoApl);
                    Statement stmt25 = null;
                    stmt25 = conn.createStatement();
                    stmt25.executeUpdate(insCosNoApl);
                    stmt25.close();
                }
                LOGGER.info("EXCEPCIONES COSTOS POR CUENTA INSERTADOS CORRECTAMENTE");
            }

            //Se cargan las excepciones de cargo del cierre si vienen
            String cargoNoAplica = cta.getCargNoAplica().trim();
            if (!cargoNoAplica.isEmpty() && cargoNoAplica.length() != 0) {
                for (int i = 0; i < cargoNoAplica.length(); i += 3) {
                    String idCargo = cargoNoAplica.substring(i, i + 3);
                    String insCarNoApl = "insert into Gxbdbps.tctaexcar values(" + cta.getNroCuenta() + ", '" + idCargo + "', '" + cta.getUserActualiza() + "', '" + fecha + "', '', '', '')";
                    //LOGGER.info(insCarNoApl);
                    Statement stmt26 = null;
                    stmt26 = conn.createStatement();
                    stmt26.executeUpdate(insCarNoApl);
                    stmt26.close();
                }
                LOGGER.info("EXCEPCIONES CARGOS DEL CIERRE POR CUENTA INSERTADOS CORRECTAMENTE");
            }
            //Se inserta nro de control de la cuenta, la retencion y nro sucursal
            String insConCtaSQL = "insert into Gxbdbps.conctatar values('" + cta.getNroCuenta() + "', '" + cta.getNroControl() + "', '', '" + cta.getNroDocCod() + "', '" + retener + "', '" + sucursal + "')";
            //LOGGER.info(insConCtaSQL);
            Statement stmt24 = null;
            stmt24 = conn.createStatement();
            stmt24.executeUpdate(insConCtaSQL);
            stmt24.close();

            setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
            retorno = "Proceso submitido";
//            retorno = altaTarjeta(tarjeta, conn, cta);
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            retorno = "ERROR AL PROCESAR LA SOLICITUD";
            try {
                conn.rollback();
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        LOGGER.info("---------- FINALIZA ALTA DE CUENTA ----------");
        //LOGGER.info(retorno);
        LOGGER.info("");
        return retorno;
    }

    private String modificarCuenta(Cuenta cta, Connection conn, long secTrx, String datosEntrada) {
        //String errorMod = "";
        String fecha = Utils.obtenerFechaHora("yyyyMMdd");
        String hora = Utils.obtenerFechaHora("HHmm");
        String tipoDoc = mapearTipoDoc(cta.getTipoDoc());
        String tipoDocCod = mapearTipoDoc(cta.getTipDocCod());
//        String nombres = cta.getNombre1() + " " + cta.getNombre2();
//        String apellidos = cta.getApellido1() + " " + cta.getApellido2();
        String retorno = "";
        LOGGER.info("---------- INICIA MODIFICACION DE CUENTA ----------");
        LOGGER.info("SECUENCIA TRANSACCION: " + secTrx);
        try {
            //Se obtiene la fecha comercial
            String fechaComercial = getFechaComercial();
            String afinidad = consultarAfinidad(cta.getNroCuenta(), conn).trim();

            if (verificarCta(cta.getNroCuenta(), conn)) {
                LOGGER.info("Verificacion de cuenta aprobada");
                //Se carga los datos datos del cliente si no viene
                if (cta.getNroDoc().trim().isEmpty() || cta.getNroDoc() != null) {
                    String consCtaSQL = "select mctipod, mcnumdo from Gxbdbps.tmctaaf where mcnumct = '" + cta.getNroCuenta() + "'";
                    PreparedStatement stmt = null;
                    stmt = conn.prepareStatement(consCtaSQL);
                    stmt.executeQuery();
                    ResultSet rs = stmt.getResultSet();
                    String numDoc = "";
                    while (rs.next()) {
                        tipoDoc = rs.getString(1);
                        numDoc = rs.getString(2);
                    }
                    cta.setNroDoc(numDoc.trim());
                    cta.setTipoDoc(tipoDoc.trim());
                    //LOGGER.info("Numero de documento consultado " + cta.getNroDoc());
                    //LOGGER.info("Tipo de documento consultado " + cta.getTipoDoc());
                } else if (!cta.getTipoDoc().trim().isEmpty() && cta.getTipoDoc() != null) {
                    String tipDocQL = "select cetipod from Gxbdbps.gclieaf where cenumdo = " + cta.getNroDoc() + " and enemiso = '021'";
                    PreparedStatement stmt50 = null;
                    stmt50 = conn.prepareStatement(tipDocQL);
                    stmt50.executeQuery();
                    ResultSet rs50 = stmt50.getResultSet();
                    while (rs50.next()) {
                        tipoDoc = rs50.getString(1);
                    }
                }

                //Se verifica si se envia o no un nuevo nro de documento para su cambio
                if ((!cta.getTipDocNew().trim().isEmpty() && cta.getTipDocNew() != null) && (cta.getNroDocNew() != null && !cta.getNroDocNew().trim().isEmpty())) {
                    String consCliSQL = "select cenumdo, cenomb1, cenomb2, ceapel1, ceapel2, cenumru, cesexo, ceecivi, cefenac, mocodig, cetipoc, cenumdc, ceempre, prcodig, "
                            + "cetipov, cesalar, ceultsc, ceapeca, ceapnom, cefeing, cehorin, ceusrin, cenumd2, cesocio from Gxbdbps.gclieaf where enemiso = '021' "
                            + "and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "'";
                    //LOGGER.info(consCliSQL);
                    PreparedStatement stmt = null;
                    stmt = conn.prepareStatement(consCliSQL);
                    stmt.executeQuery();
                    ResultSet rs = stmt.getResultSet();
                    String cenumdo = "";
                    String cenomb1 = "";
                    String cenomb2 = "";
                    String ceapel1 = "";
                    String ceapel2 = "";
                    String cenumru = "";
                    String cesexo = "";
                    String ceecivi = "";
                    String cefenac = "";
                    String mocodig = "";
                    String cetipoc = "";
                    String cenumdc = "";
                    String ceempre = "";
                    String prcodig = "";
                    String cetipov = "";
                    String cesalar = "";
                    String ceultsc = "";
                    String ceapeca = "";
                    String ceapnom = "";
                    String cefeing = "";
                    String cehorin = "";
                    String ceusrin = "";
                    String cenumd2 = "";
                    String cesocio = "";
                    while (rs.next()) {
                        cenumdo = rs.getString(1);
                        cenomb1 = rs.getString(2);
                        cenomb2 = rs.getString(3);
                        ceapel1 = rs.getString(4);
                        ceapel2 = rs.getString(5);
                        cenumru = rs.getString(6);
                        cesexo = rs.getString(7);
                        ceecivi = rs.getString(8);
                        cefenac = rs.getString(9);
                        mocodig = rs.getString(10);
                        cetipoc = rs.getString(11);
                        cenumdc = rs.getString(12);
                        ceempre = rs.getString(13);
                        prcodig = rs.getString(14);
                        cetipov = rs.getString(15);
                        cesalar = rs.getString(16);
                        ceultsc = rs.getString(17);
                        ceapeca = rs.getString(18);
                        ceapnom = rs.getString(19);
                        cefeing = rs.getString(20);
                        cehorin = rs.getString(21);
                        ceusrin = rs.getString(22);
                        cenumd2 = rs.getString(23);
                        cesocio = rs.getString(24);
                    }
                    stmt.close();
                    //Si inserta los datos del cliente con el nuevo numero de documento
                    String tipoDocNew = mapearTipoDoc(cta.getTipDocNew());
                    String insertCliSQL = "insert into Gxbdbps.gclieaf values('021', '" + tipoDocNew + "', '" + cta.getNroDocNew() + "', '" + cenomb1 + "', '" + cenomb2
                            + "', '" + ceapel1 + "', '" + ceapel2 + "', '" + cenumru + "', '" + cesexo + "', '" + ceecivi + "', '" + cefenac + "', '" + mocodig + "', '"
                            + cetipoc + "', '" + cenumdc + "', '" + ceempre + "', '" + prcodig + "', '" + cetipov + "', " + cesalar + ", " + ceultsc + ", '" + ceapeca
                            + "', '" + ceapnom + "', '" + cefeing + "', '" + cehorin + "', '" + cta.getUserActualiza() + "', '" + cenumdo + "', '" + cesocio + "')";
                    //LOGGER.info(insertCliSQL);
                    Statement stmt2 = null;
                    stmt2 = conn.createStatement();
                    stmt2.executeUpdate(insertCliSQL);
                    stmt2.close();

                    //Se obtiene el ultimo valor de la secuencia clientes
                    String ultSecSQL = "select max(alnumse) from Gxbdbps.aucliaf where alnumdo = '" + cta.getNroDoc() + "' and alemiso = '021' and altipod = '" + tipoDoc + "'";
                    long nroAudCli = DBUtils.getSecuencia(ultSecSQL, conn);
                    //Se inserta el cliente con su nuevo nro de documento
                    String insertSQLAudi = "insert into Gxbdbps.aucliaf values ('" + fechaComercial + "', '021', '" + tipoDocNew + "', '" + cta.getNroDocNew() + "', " + nroAudCli + ", 'M', 1,"
                            + " '" + cta.getUserActualiza() + "', 0, 'Numero de Documento', '" + cenumdo + "', '" + cta.getNroDocNew() + "', 0,'" + fecha + "', " + hora + ")";
                    //LOGGER.info(insertSQLAudi);
                    Statement stmt4 = null;
                    stmt4 = conn.createStatement();
                    stmt4.executeUpdate(insertSQLAudi);
                    stmt4.close();

                    //Se actualiza nro de documento del cliente en sus direcciones
                    String consDirCliSQL = "select enemiso, cetipod, cenumdo, cesecue, cedirec, ceenvio, cediret, cetelef, celocal, cezona from Gxbdbps.gdireaf where enemiso = '021' "
                            + "and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "'";
                    //LOGGER.info(consDirCliSQL);
                    PreparedStatement stmt13 = null;
                    stmt13 = conn.prepareStatement(consDirCliSQL);
                    stmt13.executeQuery();
                    ResultSet rs13 = stmt13.getResultSet();
                    String enemiso = "";
                    String cetipod = "";
                    String cnumdo = "";
                    String cesecue = "";
                    String cedirec = "";
                    String ceenvio = "";
                    String cediret = "";
                    String cetelef = "";
                    String celocal = "";
                    String cezona = "";
                    while (rs13.next()) {
                        enemiso = rs13.getString(1);
                        cetipod = rs13.getString(2);
                        cnumdo = rs13.getString(3);
                        cesecue = rs13.getString(4);
                        cedirec = rs13.getString(5);
                        ceenvio = rs13.getString(6);
                        cediret = rs13.getString(7);
                        cetelef = rs13.getString(8);
                        celocal = rs13.getString(9);
                        cezona = rs13.getString(10);

                        String insDirCliSQL = "insert into Gxbdbps.Gdireaf values('" + enemiso + "', '" + tipoDocNew + "', '" + cta.getNroDocNew() + "', " + cesecue + ", '"
                                + cedirec + "', '" + ceenvio + "', '" + cediret + "', '" + cetelef + "', '" + celocal + "', '" + cezona + "')";
                        Statement stmt14 = null;
                        stmt14 = conn.createStatement();
                        stmt14.executeUpdate(insDirCliSQL);
                        stmt14.close();

                        String secMailSQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '" + enemiso + "' and aetipod = '" + tipoDoc + "' and aenumdo = '" + cta.getNroDoc() + "' and aesecue = '" + cesecue + "'";
                        long nroAudDir = DBUtils.getSecuencia(secMailSQL, conn);

                        String insCliDirSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '" + enemiso + "', '" + tipoDocNew + "', '" + cta.getNroDocNew() + "', " + cesecue + ", "
                                + nroAudDir + ", 'M', 1," + " '" + cta.getUserActualiza() + "', 0, 'Numero Documento', '" + cnumdo + "', '" + cta.getNroDocNew() + "', 0,'" + fecha + "', " + hora + ")";
                        Statement stmt16 = null;
                        stmt16 = conn.createStatement();
                        stmt16.executeUpdate(insCliDirSQL);
                        stmt16.close();

                        if (!cetipod.trim().equals(tipoDocNew.trim())) {
                            String insCliDir2SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '" + enemiso + "', '" + tipoDocNew + "', '" + cta.getNroDocNew() + "', " + cesecue + ", "
                                    + nroAudDir + ", 'M', 1," + " '" + cta.getUserActualiza() + "', 0, 'Tipo Documento', '" + cetipod + "', '" + cta.getTipDocNew() + "', 0,'" + fecha + "', " + hora + ")";
                            Statement stmt17 = null;
                            stmt17 = conn.createStatement();
                            stmt17.executeUpdate(insCliDir2SQL);
                            stmt17.close();
                        }
                    }

                    //Se actualiza nro de documento del cliente en sus cuentas
                    String consCtaSQL = "select mcnumct, mcafini, mccodsc from Gxbdbps.tmctaaf where mcnumdo = '" + cenumdo + "' and mcemiso = '021'";
                    //LOGGER.info(consCtaSQL);
                    PreparedStatement stmt5 = null;
                    stmt5 = conn.prepareStatement(consCtaSQL);
                    stmt5.executeQuery();
                    ResultSet rs5 = stmt5.getResultSet();
                    String nroCuenta = "";
                    String afiCuenta = "";
                    String sucursal = "";
                    while (rs5.next()) {
                        nroCuenta = rs5.getString(1);
                        afiCuenta = rs5.getString(2);
                        sucursal = rs5.getString(3);

                        String updateSQL = "update Gxbdbps.tmctaaf set mcnumdo = " + cta.getNroDocNew() + " where mcnumct = '" + nroCuenta + "' and mcemiso = '021'";
                        //LOGGER.info(updateSQL);
                        Statement stmt6 = null;
                        stmt6 = conn.createStatement();
                        stmt6.executeUpdate(updateSQL);
                        stmt6.close();
                        //Se obtiene el ultimo valor de la secuencia
                        String ultSecCtaSQL = "select max(acnumse) from Gxbdbps.auctaaf where acemiso = '021' and acafini = '" + afiCuenta + "' and acnumct = " + nroCuenta;
                        PreparedStatement stmt7 = null;
                        stmt7 = conn.prepareStatement(ultSecCtaSQL);
                        stmt7.executeQuery();
                        ResultSet rs7 = stmt7.getResultSet();
                        String idSecCta = "";
                        while (rs7.next()) {
                            idSecCta = rs7.getString(1);
                        }
                        stmt7.close();
                        //LOGGER.info("Se obtuvo ultima secuencia de auditoria cuenta: " + idSecCta);
                        long secAudCta = 1;
                        if (idSecCta != null) {
                            if (!idSecCta.equals("")) {
                                secAudCta = Long.parseLong(idSecCta);
                                secAudCta++;
                            }
                        }
                        //Se inserta un nuevo registro en el historico de Cuentas
                        String insertSQL = "insert into Gxbdbps.auctaaf values('" + fechaComercial + "', '021', '" + afiCuenta + "', '" + nroCuenta + "', '" + secAudCta + "', 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Numero Documento', '" + cenumdo + "', '" + cta.getNroDocNew() + "', 0, '" + sucursal + "')";
                        Statement stmt8 = null;
                        stmt8 = conn.createStatement();
                        stmt8.executeUpdate(insertSQL);
                        stmt8.close();
                    }
                    stmt5.close();
                    //Se actualiza nro de documento del cliente en sus tarjetas
                    String consTarSQL = "select mtnumta, afafini from Gxbdbps.tmtaraf where cenumdo = '" + cenumdo + "' and bibines = '627431' and enemiso = '021'";
                    PreparedStatement stmt9 = null;
                    stmt9 = conn.prepareStatement(consTarSQL);
                    stmt9.executeQuery();
                    ResultSet rs9 = stmt9.getResultSet();
                    String nroTarjeta = "";
                    String afiTarjeta = "";
                    while (rs9.next()) {
                        nroTarjeta = rs9.getString(1);
                        afiTarjeta = rs9.getString(2);

                        String updateSQL = "update Gxbdbps.tmtaraf set cenumdo = '" + cta.getNroDocNew() + "' where mcnumct = '" + nroCuenta + "' and enemiso = '021' and bibines = '627431'";
                        Statement stmt10 = null;
                        stmt10 = conn.createStatement();
                        stmt10.executeUpdate(updateSQL);
                        stmt10.close();
                        //Se obtiene el ultimo valor de la secuencia
                        String ultSecCtaSQL = "select max(atnumse) from Gxbdbps.autaraf where atemiso = '021' and atafini = '" + afiTarjeta + "' and atnumta = " + nroTarjeta;
                        PreparedStatement stmt11 = null;
                        stmt11 = conn.prepareStatement(ultSecCtaSQL);
                        stmt11.executeQuery();
                        ResultSet rs11 = stmt11.getResultSet();
                        String idSecTar = "";
                        while (rs11.next()) {
                            idSecTar = rs11.getString(1);
                        }
                        stmt11.close();
                        //LOGGER.info("Se obtuvo ultima secuencia de auditoria tarjeta: " + idSecTar);
                        long secAudTar = 1;
                        if (idSecTar != null) {
                            if (!idSecTar.equals("")) {
                                secAudTar = Long.parseLong(idSecTar);
                                secAudTar++;
                            }
                        }
                        //Se inserta un nuevo registro en el historico de tarjetas
                        String insertSQL = "insert into Gxbdbps.autaraf values('" + fechaComercial + "', '021', '" + afiTarjeta + "', '" + nroTarjeta + "', '" + secAudTar + "', 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Numero Documento', '" + cenumdo + "', '" + cta.getNroDocNew() + "', 0, '" + sucursal + "')";
                        Statement stmt12 = null;
                        stmt12 = conn.createStatement();
                        stmt12.executeUpdate(insertSQL);
                        stmt12.close();
                    }
//                    cta.setNroDoc(cta.getNroDocNew());
//                    cta.setTipoDoc(cta.getTipDocNew());
//                    tipoDoc = mapearTipoDoc(cta.getTipoDoc());
                }
                //Inicia modificacion de datos de la cuenta
                if (!cta.getNroDoc().isEmpty() && cta.getNroDoc() != null) {
                    //Se inicia la modificacion del cliente
                    String consCliSQL = "select cenomb1, cenomb2, ceapel1, ceapel2, mocodig, cefenac, cesexo, ceecivi, prcodig from Gxbdbps.gclieaf where enemiso = '021' "
                            + "and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "'";
                    //LOGGER.info(consCliSQL);
                    PreparedStatement stmt = null;
                    stmt = conn.prepareStatement(consCliSQL);
                    stmt.executeQuery();
                    ResultSet rs = stmt.getResultSet();
                    String nombre1 = "";
                    String nombre2 = "";
                    String apellido1 = "";
                    String apellido2 = "";
                    String nacion = "";
                    String feNac = "";
                    String sexo = "";
                    String estCivil = "";
                    String prof = "";
                    while (rs.next()) {
                        nombre1 = rs.getString(1);
                        nombre2 = rs.getString(2);
                        apellido1 = rs.getString(3);
                        apellido2 = rs.getString(4);
                        nacion = rs.getString(5);
                        feNac = rs.getString(6);
                        sexo = rs.getString(7);
                        estCivil = rs.getString(8);
                        prof = rs.getString(9);
                    }
                    stmt.close();
                    //Se obtiene el ultimo valor de la secuencia clientes
                    String ultSecSQL = "select max(alnumse) from Gxbdbps.aucliaf where alnumdo = " + cta.getNroDoc() + " and alemiso = '021' and altipod = '" + tipoDoc + "'";
                    //LOGGER.info(ultSecSQL);
                    PreparedStatement stmt2 = null;
                    stmt2 = conn.prepareStatement(ultSecSQL);
                    stmt2.executeQuery();
                    ResultSet rs2 = stmt2.getResultSet();
                    String secAudCli = "";
                    while (rs2.next()) {
                        secAudCli = rs2.getString(1);
                    }
                    stmt2.close();
                    //LOGGER.info("Se obtuvo ultima secuencia de auditoria clientes: " + secAudCli);
                    long nroAudCli = 0;
                    if (secAudCli != null) {
                        if (!secAudCli.equals("")) {
                            nroAudCli = Long.parseLong(secAudCli);
                        }
                    }

                    String updSQL1 = "update Gxbdbps.gclieaf set ";
                    String updSQL2 = " where enemiso = '021' " + "and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "'";
                    String insertSQLAudi = "insert into Gxbdbps.aucliaf values";
                    int bandera = 0;
                    String[] nomApe = {nombre1, nombre2, apellido1, apellido2};
                    int bndNomApe = 0;
                    if (!cta.getNombre1().isEmpty() && cta.getNombre1() != null) {
                        updSQL1 += "cenomb1 = '" + cta.getNombre1() + "',";
                        nroAudCli++;
                        insertSQLAudi += "('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', " + nroAudCli + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Primer Nombre', '" + nombre1 + "', '" + cta.getNombre1() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera++;
                        nomApe[0] = cta.getNombre1();
                        bndNomApe++;
                    }
                    if (!cta.getNombre2().isEmpty() && cta.getNombre2() != null) {
                        updSQL1 += "cenomb2 = '" + cta.getNombre2() + "',";
                        nroAudCli++;
                        insertSQLAudi += "('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', " + nroAudCli + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Segundo Nombre', '" + nombre2 + "', '" + cta.getNombre2() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera++;
                        nomApe[1] = cta.getNombre2();
                        bndNomApe++;
                    }
                    if (!cta.getApellido1().isEmpty() && cta.getApellido1() != null) {
                        updSQL1 += "ceapel1 = '" + cta.getApellido1() + "',";
                        nroAudCli++;
                        insertSQLAudi += "('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', " + nroAudCli + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Primer Apellido', '" + apellido1 + "', '" + cta.getApellido1() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera++;
                        nomApe[2] = cta.getApellido1();
                        bndNomApe++;
                    }
                    if (!cta.getApellido2().isEmpty() && cta.getApellido2() != null) {
                        updSQL1 += "ceapel2 = '" + cta.getApellido2() + "',";
                        nroAudCli++;
                        insertSQLAudi += "('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', " + nroAudCli + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Segundo Apellido', '" + apellido2 + "', '" + cta.getApellido2() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera++;
                        nomApe[3] = cta.getApellido2();
                        bndNomApe++;
                    }
                    if (bndNomApe != 0) {
                        String newNombApe = nomApe[0].trim() + " " + nomApe[1].trim() + ", " + nomApe[2].trim() + " " + nomApe[3].trim();
                        updSQL1 += "ceapnom = '" + newNombApe + "',";
                    }
                    if (!cta.getPaisDoc().isEmpty() && cta.getPaisDoc() != null) {
                        updSQL1 += "mocodig = '" + cta.getPaisDoc() + "',";
                        nroAudCli++;
                        insertSQLAudi += "('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', " + nroAudCli + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Cod. de Nacionalidad', '" + nacion + "', '" + cta.getPaisDoc() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera++;
                    }
                    if (!cta.getFechaNac().isEmpty() && cta.getFechaNac() != null) {
                        String fechaNac = modificarFormatoFecha(cta.getFechaNac().trim());
                        updSQL1 += "cefenac = '" + fechaNac + "',";
                        nroAudCli++;
                        insertSQLAudi += "('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', " + nroAudCli + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Fecha Nacimiento', '" + feNac + "', '" + fechaNac + "', 0,'" + fecha + "', " + hora + "),";
                        bandera++;
                    }
                    if (!cta.getSexo().isEmpty() && cta.getSexo() != null) {
                        updSQL1 += "cesexo = '" + cta.getSexo() + "',";
                        nroAudCli++;
                        insertSQLAudi += "('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', " + nroAudCli + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Sexo', '" + sexo + "', '" + cta.getSexo() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera++;
                    }
                    if (!cta.getEstadoCivil().isEmpty() && cta.getEstadoCivil() != null) {
                        updSQL1 += "ceecivi = '" + cta.getEstadoCivil() + "',";
                        nroAudCli++;
                        insertSQLAudi += "('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', " + nroAudCli + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Estado Civil', '" + estCivil + "', '" + cta.getEstadoCivil() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera++;
                    }
                    if (!cta.getOcupacion().isEmpty() && cta.getOcupacion() != null) {
                        String profesion = Utils.mapearOcupacion(cta.getOcupacion());
                        updSQL1 += "prcodig = '" + profesion + "',";
                        nroAudCli++;
                        insertSQLAudi += "('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', " + nroAudCli + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Profesion', '" + prof + "', '" + profesion + "', 0,'" + fecha + "', " + hora + "),";
                        bandera++;
                    }
                    if (bandera != 0) {
                        //Se extrae la ultima coma del string
                        updSQL1 = updSQL1.trim().substring(0, updSQL1.length() - 1);
                        String updSQL = updSQL1 + updSQL2;
                        //LOGGER.info(updSQL);
                        Statement stmt3 = null;
                        stmt3 = conn.createStatement();
                        stmt3.executeUpdate(updSQL);

                        insertSQLAudi = insertSQLAudi.substring(0, insertSQLAudi.length() - 1);
                        Statement stmt4 = null;
                        stmt4 = conn.createStatement();
                        stmt4.executeUpdate(insertSQLAudi);
                        LOGGER.info("CLIENTE MODIFICADO CORRECTAMENTE");
                        stmt3.close();
                        stmt4.close();
                    }
                    //Se inicia la modificacion de direcciones del cliente
                    String consCantDirSQL = "select ceultsc from Gxbdbps.gclieaf where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "'";
                    PreparedStatement stmt5 = null;
                    stmt5 = conn.prepareStatement(consCantDirSQL);
                    stmt5.executeQuery();
                    ResultSet rs5 = stmt5.getResultSet();
                    int ultSecDir = 0;
                    int cantNewDir = 0;
                    while (rs5.next()) {
                        ultSecDir = rs5.getInt(1);
                    }
                    stmt5.close();
                    //Inicia modificacion de direccion de recibo y nro de telefono
                    if ((!cta.getDirRecibo().isEmpty() && cta.getDirRecibo() != null) || (!cta.getNroTel().isEmpty() && cta.getNroTel() != null)) {
                        String consDirRecSQL = "select cedirec, cetelef from Gxbdbps.Gdireaf where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 0 and cediret = 'O'";
                        PreparedStatement stmt18 = null;
                        stmt18 = conn.prepareStatement(consDirRecSQL);
                        stmt18.executeQuery();
                        ResultSet rs18 = stmt18.getResultSet();
                        String dirRecAct = "";
                        String telAct = "";
                        while (rs18.next()) {
                            dirRecAct = rs18.getString(1);
                            telAct = rs18.getString(2);
                        }
                        stmt18.close();
                        //Se obtiene el ultimo valor de la secuencia de direccion
                        String secDirRSQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '" + cta.getNroDoc() + "' and aesecue = 0";
                        long nroAudDirR = DBUtils.getSecuencia(secDirRSQL, conn);

                        if (dirRecAct.isEmpty() && telAct.isEmpty()) {
                            if (!cta.getDirRecibo().isEmpty() && !cta.getNroTel().isEmpty()) {
                                String insDirRSQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, '" + cta.getDirRecibo() + "', 'S', 'O', '" + cta.getNroTel() + "', '', '')";
                                boolean insDirR = DBUtils.ejecucionSQL(insDirRSQL, conn);
                                cantNewDir++;

                                String insAudDirRSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, " + nroAudDirR + ", 'M', 1,"
                                        + " '" + cta.getUserActualiza() + "', 0, 'Direccion Recibo', '', '" + cta.getDirRecibo() + ", 0,'" + fecha + "', " + hora + "), ('"
                                        + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, " + (nroAudDirR + 1) + ", 'M', 1," + " '" + cta.getUserActualiza()
                                        + "', 0, 'Nro. Teléfono', '', '" + cta.getNroTel() + "', 0,'" + fecha + "', " + hora + ")";
                                boolean insAudDirR = DBUtils.ejecucionSQL(insAudDirRSQL, conn);
                            } else if (!cta.getDirRecibo().isEmpty() && cta.getDirRecibo() != null) {
                                String insDirRSQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, '" + cta.getDirRecibo() + "', 'S', 'O', '', '', '')";
                                boolean insDirR = DBUtils.ejecucionSQL(insDirRSQL, conn);
                                cantNewDir++;

                                String insAudDirRSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, " + nroAudDirR + ", 'M', 1,"
                                        + " '" + cta.getUserActualiza() + "', 0, 'Direccion Recibo', '', '" + cta.getDirRecibo() + "', 0,'" + fecha + "', " + hora + ")";
                                boolean insAudDirR = DBUtils.ejecucionSQL(insAudDirRSQL, conn);
                            } else if (!cta.getNroTel().isEmpty() && cta.getNroTel() != null) {
                                String insTelSQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, '', 'S', 'O', '" + cta.getNroTel() + "', '', '')";
                                boolean insTel = DBUtils.ejecucionSQL(insTelSQL, conn);
                                cantNewDir++;

                                String insAudTelSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, " + nroAudDirR + ", 'M', 1,"
                                        + " '" + cta.getUserActualiza() + "', 0, 'Nro. Telefono', '', '" + cta.getNroTel() + "', 0,'" + fecha + "', " + hora + ")";
                                boolean insAudTel = DBUtils.ejecucionSQL(insAudTelSQL, conn);
                            }
                        } else {
                            if (!cta.getDirRecibo().isEmpty() && cta.getDirRecibo() != null) {
                                String updDirRSQL = "update gxbdbps.gdireaf set cedirec = '" + cta.getDirRecibo() + "' where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 0";
                                boolean updDirR = DBUtils.ejecucionSQL(updDirRSQL, conn);

                                String insAudDirRSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, " + nroAudDirR + ", 'M', 1,"
                                        + " '" + cta.getUserActualiza() + "', 0, 'Direccion Recibo', '" + dirRecAct + "', '" + cta.getDirRecibo() + "', 0,'" + fecha + "', " + hora + ")";
                                boolean insAudDirR = DBUtils.ejecucionSQL(insAudDirRSQL, conn);
                            }
                            if (!cta.getNroTel().isEmpty() && cta.getNroTel() != null) {
                                String updTelSQL = "update gxbdbps.gdireaf set cetelef = '" + cta.getNroTel() + "' where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 0";
                                boolean updTel = DBUtils.ejecucionSQL(updTelSQL, conn);

                                String insAudTelSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, " + nroAudDirR + ", 'M', 1,"
                                        + " '" + cta.getUserActualiza() + "', 0, 'Nro. Teléfono', '" + telAct + "', '" + cta.getNroTel() + "', 0,'" + fecha + "', " + hora + ")";
                                boolean insAudTel = DBUtils.ejecucionSQL(insAudTelSQL, conn);
                            }
                        }
                        LOGGER.info("DIRECCION RECIBO Y NRO. DE TELEFONO MODIFICADO CORRECTAMENTE");
                    }
                    //Inica modificacion de direccion recibo y nro. de nro telefono
                    if ((!cta.getDirEmailPer().isEmpty() && cta.getDirEmailPer() != null) || (!cta.getNroCel().isEmpty() && cta.getNroCel() != null)) {
                        String consDirMailSQL = "select cedirec, cetelef from Gxbdbps.Gdireaf where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 4 and cediret = 'E'";
                        PreparedStatement stmt6 = null;
                        stmt6 = conn.prepareStatement(consDirMailSQL);
                        stmt6.executeQuery();
                        ResultSet rs6 = stmt6.getResultSet();
                        String mailAct = "";
                        String celAct = "";
                        while (rs6.next()) {
                            mailAct = rs6.getString(1);
                            celAct = rs6.getString(2);
                        }
                        stmt6.close();
                        //Se obtiene el ultimo valor de la secuencia de direccion
                        String secMailSQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '" + cta.getNroDoc() + "' and aesecue = 4";
                        long nroAudMail = DBUtils.getSecuencia(secMailSQL, conn);

                        if ((mailAct.isEmpty() && celAct.isEmpty()) || (mailAct == null && celAct == null)) {
                            if (!cta.getDirEmailPer().isEmpty() && !cta.getNroCel().isEmpty()) {
                                String insMailSQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 4, '" + cta.getDirEmailPer() + "', 'S', 'E', '" + cta.getNroCel() + "', '', '')";
                                boolean insMail = DBUtils.ejecucionSQL(insMailSQL, conn);
                                cantNewDir++;
                                String insAudMailSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 4, " + nroAudMail + ", 'M', 1,"
                                        + " '" + cta.getUserActualiza() + "', 0, 'Direccion de Email', '', '" + cta.getDirEmailPer() + ", 0,'" + fecha + "', " + hora + "), ('"
                                        + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 4, " + (nroAudMail + 1) + ", 'M', 1," + " '" + cta.getUserActualiza()
                                        + "', 0, 'Nro. Celular', '', '" + cta.getNroCel() + "', 0,'" + fecha + "', " + hora + ")";
                                boolean insAudMail = DBUtils.ejecucionSQL(insAudMailSQL, conn);
                            } else if (!cta.getDirEmailPer().isEmpty()) {
                                String insMailSQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 4, '" + cta.getDirEmailPer() + "', 'S', 'E', '', '', '')";
                                boolean insMail = DBUtils.ejecucionSQL(insMailSQL, conn);
                                cantNewDir++;
                                String insAudMailSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 4, " + nroAudMail + ", 'M', 1,"
                                        + " '" + cta.getUserActualiza() + "', 0, 'Direccion de Email', '', '" + cta.getDirEmailPer() + "', 0,'" + fecha + "', " + hora + ")";
                                boolean insAudMail = DBUtils.ejecucionSQL(insAudMailSQL, conn);
                            } else if (!cta.getNroCel().isEmpty()) {
                                String insMailSQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 4, '', 'S', 'E', '" + cta.getNroCel() + "', '', '')";
                                boolean insMail = DBUtils.ejecucionSQL(insMailSQL, conn);
                                cantNewDir++;
                                String insAudMailSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 4, " + nroAudMail + ", 'M', 1,"
                                        + " '" + cta.getUserActualiza() + "', 0, 'Nro. Celular', '', '" + cta.getNroCel() + "', 0,'" + fecha + "', " + hora + ")";
                                boolean insAudMail = DBUtils.ejecucionSQL(insAudMailSQL, conn);
                            }
                        } else {
                            if (!cta.getDirEmailPer().isEmpty()) {
                                String updMailSQL = "update gxbdbps.gdireaf set cedirec = '" + cta.getDirEmailPer() + "' where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 4";
                                boolean updMail = DBUtils.ejecucionSQL(updMailSQL, conn);
                                String insAudMailSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 4, " + nroAudMail + ", 'M', 1,"
                                        + " '" + cta.getUserActualiza() + "', 0, 'Direccion de Email', '" + mailAct + "', '" + cta.getDirEmailPer() + "', 0,'" + fecha + "', " + hora + ")";
                                boolean insAudMail = DBUtils.ejecucionSQL(insAudMailSQL, conn);
                            }
                            if (!cta.getNroCel().isEmpty()) {
                                String updMailSQL = "update gxbdbps.gdireaf set cetelef = '" + cta.getNroCel() + "' where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 4";
                                boolean updMail = DBUtils.ejecucionSQL(updMailSQL, conn);
                                String insAudMailSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 4, " + nroAudMail + ", 'M', 1,"
                                        + " '" + cta.getUserActualiza() + "', 0, 'Nro. Celular', '" + celAct + "', '" + cta.getNroCel() + "', 0,'" + fecha + "', " + hora + ")";
                                boolean insAudMail = DBUtils.ejecucionSQL(insAudMailSQL, conn);
                            }
                        }
                        LOGGER.info("DIRECCION DE MAIL Y NRO. DE CELULAR MODIFICADO CORRECTAMENTE");
                    }
                    //Inicia modificacion de direccion extracto 1 y nro de telefono
                    if (!cta.getDirExtr1().isEmpty() && cta.getDirExtr1() != null) {
                        String consDir1SQL = "select cedirec from Gxbdbps.Gdireaf where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 1 and cediret = 'O'";
                        PreparedStatement stmt30 = null;
                        stmt30 = conn.prepareStatement(consDir1SQL);
                        stmt30.executeQuery();
                        ResultSet rs30 = stmt30.getResultSet();
                        String dir1Act = "";
                        while (rs30.next()) {
                            dir1Act = rs30.getString(1);
                        }
                        stmt30.close();
                        String secDir1SQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '" + cta.getNroDoc() + "' and aesecue = 1";
                        long nroAudDir1 = DBUtils.getSecuencia(secDir1SQL, conn);

                        if (dir1Act.isEmpty() || dir1Act == null) {
                            String insDir1SQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 1, '" + cta.getDirExtr1() + "', 'S', 'O', '', '', '')";
                            boolean insDir1 = DBUtils.ejecucionSQL(insDir1SQL, conn);
                            cantNewDir++;
                            String insAudDir1SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 1, " + nroAudDir1 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Direccion Extracto 1', '', '" + cta.getDirExtr1() + "', 0,'" + fecha + "', " + hora + ")";
                            boolean insDir1Aud = DBUtils.ejecucionSQL(insAudDir1SQL, conn);
                        } else {
                            String updDir1SQL = "update gxbdbps.gdireaf set cedirec = '" + cta.getDirExtr1() + "' where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 1";
                            boolean updDir1 = DBUtils.ejecucionSQL(updDir1SQL, conn);
                            String insDir1AudSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 1, " + nroAudDir1 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Direccion Extracto 1', '" + dir1Act + "', '" + cta.getDirExtr1() + "', 0,'" + fecha + "', " + hora + ")";
                            boolean insDir1Aud = DBUtils.ejecucionSQL(insDir1AudSQL, conn);
                        }
                        LOGGER.info("DIRECCION EXTRACTO 1 MODIFICADO CORRECTAMENTE");
                    }
                    if (!cta.getDirExtr2().isEmpty() && cta.getDirExtr2() != null) {
                        String consDir2SQL = "select cedirec from Gxbdbps.Gdireaf where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 2 and cediret = 'O'";
                        PreparedStatement stmt30 = null;
                        stmt30 = conn.prepareStatement(consDir2SQL);
                        stmt30.executeQuery();
                        ResultSet rs30 = stmt30.getResultSet();
                        String dir2Act = "";
                        while (rs30.next()) {
                            dir2Act = rs30.getString(1);
                        }
                        stmt30.close();
                        String secDir2SQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '" + cta.getNroDoc() + "' and aesecue = 2";
                        long nroAudDir2 = DBUtils.getSecuencia(secDir2SQL, conn);

                        if (dir2Act.isEmpty()) {
                            String insDir2SQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 2, '" + cta.getDirExtr2() + "', 'S', 'O', '', '', '')";
                            boolean insDir2 = DBUtils.ejecucionSQL(insDir2SQL, conn);
                            cantNewDir++;
                            String insAudDir2SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 2, " + nroAudDir2 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Direccion Extracto 2', '', '" + cta.getDirExtr2() + "', 0,'" + fecha + "', " + hora + ")";
                            boolean insAudDir2 = DBUtils.ejecucionSQL(insAudDir2SQL, conn);
                        } else {
                            String updDir2SQL = "update gxbdbps.gdireaf set cedirec = '" + cta.getDirExtr2() + "' where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 2";
                            boolean udpDir2 = DBUtils.ejecucionSQL(updDir2SQL, conn);
                            String insAudDir2SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 2, " + nroAudDir2 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Direccion Extracto 2', '" + dir2Act + "', '" + cta.getDirExtr2() + "', 0,'" + fecha + "', " + hora + ")";
                            boolean insAudDir2 = DBUtils.ejecucionSQL(insAudDir2SQL, conn);
                        }
                        LOGGER.info("DIRECCION EXTRACTO 2 MODIFICADO CORRECTAMENTE");
                    }
                    if (!cta.getDirExtr3().isEmpty() && cta.getDirExtr3() != null) {
                        String consDir3SQL = "select cedirec from Gxbdbps.Gdireaf where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 3 and cediret = 'O'";
                        PreparedStatement stmt36 = null;
                        stmt36 = conn.prepareStatement(consDir3SQL);
                        stmt36.executeQuery();
                        ResultSet rs36 = stmt36.getResultSet();
                        String dir3Act = "";
                        while (rs36.next()) {
                            dir3Act = rs36.getString(1);
                        }
                        stmt36.close();
                        String secDir3SQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '" + cta.getNroDoc() + "' and aesecue = 3";
                        long nroAudDir3 = DBUtils.getSecuencia(secDir3SQL, conn);

                        if (dir3Act.isEmpty()) {
                            String insDir3SQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 3, '" + cta.getDirExtr3() + "', 'S', 'O', '', '', '')";
                            boolean insDir3 = DBUtils.ejecucionSQL(insDir3SQL, conn);
                            cantNewDir++;
                            String insAudDir3SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 3, " + nroAudDir3 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Direccion Extracto 3', '', '" + cta.getDirExtr3() + "', 0,'" + fecha + "', " + hora + ")";
                            boolean insAudDir3 = DBUtils.ejecucionSQL(insAudDir3SQL, conn);
                        } else {
                            String updDir3SQL = "update gxbdbps.gdireaf set cedirec = '" + cta.getDirExtr3() + "' where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 3";
                            boolean updDir3 = DBUtils.ejecucionSQL(updDir3SQL, conn);
                            String insAudDir3SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 3, " + nroAudDir3 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Nro. Direccion Extracto 3', '" + dir3Act + "', '" + cta.getDirExtr3() + "', 0,'" + fecha + "', " + hora + ")";
                            boolean insAudDir3 = DBUtils.ejecucionSQL(insAudDir3SQL, conn);
                        }
                        LOGGER.info("DIRECCION EXTRACTO 3 MODIFICADO CORRECTAMENTE");
                    }
                    if (cantNewDir > 0) {
                        ultSecDir = ultSecDir + cantNewDir;
                        String updUltDir = "update Gxbdbps.gclieaf set ceultsc = " + ultSecDir + " where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "'";
                        Statement stmt60 = null;
                        stmt60 = conn.createStatement();
                        stmt60.executeUpdate(updUltDir);
                    }
                    //Inicia modificacion de departamento y ciudad si existe
                    String deparCiudad = "";
                    String secDirRSQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '" + cta.getNroDoc() + "' and aesecue = 0";
                    String secDir1SQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '" + cta.getNroDoc() + "' and aesecue = 1";
                    String secDir2SQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '" + cta.getNroDoc() + "' and aesecue = 2";
                    String secDir3SQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '" + cta.getNroDoc() + "' and aesecue = 3";
                    if (!cta.getDepart().isEmpty() && !cta.getDepart().equals("") || !cta.getCiudad().isEmpty() && !cta.getCiudad().equals("")) {
                        String consDepCdadSQL = "select celocal, cesecue from Gxbdbps.Gdireaf where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue in (0, 1, 2, 3)";
                        PreparedStatement stmt1 = null;
                        stmt1 = conn.prepareStatement(consDepCdadSQL);
                        stmt1.executeQuery();
                        ResultSet rs1 = stmt1.getResultSet();
                        String[] nroSec = new String[4];
                        int i = 0;
                        while (rs1.next()) {
                            if (!rs1.getString(1).trim().isEmpty() && rs1.getString(1) != null) {
                                deparCiudad = rs1.getString(1);
                            }
                            nroSec[i] = rs1.getString(2);
                            i++;
                        }
                        String[] depCdad = deparCiudad.split("/");

                        if (!cta.getDepart().isEmpty() && !cta.getDepart().equals("")) {
                            String dp = String.format("%03d", Integer.parseInt(cta.getDepart()));
                            String consDeparSQL = "select codtodesc from Gxbdbps.codtoaf where coddto = '" + dp + "'";
                            //LOGGER.info(consDeparSQL);
                            PreparedStatement stmt3 = null;
                            stmt3 = conn.prepareStatement(consDeparSQL);
                            stmt3.executeQuery();
                            String depar = "";
                            ResultSet rs3 = stmt3.getResultSet();
                            while (rs3.next()) {
                                depar = rs3.getString(1);
                            }
                            //LOGGER.info(depar);
                            String newDeparCdad = depar.trim() + "/" + depCdad[1].trim();
                            //LOGGER.info(newDeparCdad);
                            //depCdad[0] = depar.trim();
                            long ultSecR = DBUtils.getSecuencia(secDirRSQL, conn);
                            long ultSec1 = DBUtils.getSecuencia(secDir1SQL, conn);
                            long ultSec2 = DBUtils.getSecuencia(secDir2SQL, conn);
                            long ultSec3 = DBUtils.getSecuencia(secDir3SQL, conn);
                            String updDir1SQL = "update gxbdbps.gdireaf set celocal = '" + newDeparCdad + "' where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue in (0, 1, 2, 3)";
                            boolean resp = DBUtils.ejecucionSQL(updDir1SQL, conn);
                            String insAudRSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, " + ultSecR + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Departamento', '" + depCdad[0].trim() + "', '" + depar + "', 0,'" + fecha + "', " + hora + ")";
                            String insAud1SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 1, " + ultSec1 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Departamento', '" + depCdad[0].trim() + "', '" + depar + "', 0,'" + fecha + "', " + hora + ")";
                            String insAud2SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 2, " + ultSec2 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Departamento', '" + depCdad[0].trim() + "', '" + depar + "', 0,'" + fecha + "', " + hora + ")";
                            String insAud3SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 3, " + ultSec3 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Departamento', '" + depCdad[0].trim() + "', '" + depar + "', 0,'" + fecha + "', " + hora + ")";
                            if (nroSec[0].equals("0") || nroSec[1].equals("0") || nroSec[2].equals("0") || nroSec[3].equals("0")) {
                                boolean respR = DBUtils.ejecucionSQL(insAudRSQL, conn);
                            }
                            if (nroSec[0].equals("1") || nroSec[1].equals("1") || nroSec[2].equals("1") || nroSec[3].equals("1")) {
                                boolean resp1 = DBUtils.ejecucionSQL(insAud1SQL, conn);
                            }
                            if (nroSec[0].equals("2") || nroSec[1].equals("2") || nroSec[2].equals("2") || nroSec[3].equals("2")) {
                                boolean resp2 = DBUtils.ejecucionSQL(insAud2SQL, conn);
                            }
                            if (nroSec[0].equals("3") || nroSec[1].equals("3") || nroSec[2].equals("3") || nroSec[3].equals("3")) {
                                boolean resp3 = DBUtils.ejecucionSQL(insAud3SQL, conn);
                            }
                            LOGGER.info("DEPARTAMENTO MODIFICADO CORRECTAMENTE");
                        }
                        if (!cta.getCiudad().isEmpty() && !cta.getCiudad().equals("")) {
                            String newDeparCdad = depCdad[0].trim() + "/Cod. Ciudad " + cta.getCiudad().trim();
                            long ultSecR = DBUtils.getSecuencia(secDirRSQL, conn);
                            long ultSec1 = DBUtils.getSecuencia(secDir1SQL, conn);
                            long ultSec2 = DBUtils.getSecuencia(secDir2SQL, conn);
                            long ultSec3 = DBUtils.getSecuencia(secDir3SQL, conn);
                            String updDir1SQL = "update gxbdbps.gdireaf set celocal = '" + newDeparCdad + "' where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue in (0, 1, 2, 3)";
                            boolean resp = DBUtils.ejecucionSQL(updDir1SQL, conn);
                            String insAudRSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, " + ultSecR + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Ciudad', '" + depCdad[1].trim() + "', 'Cod. Ciudad " + cta.getCiudad() + "', 0,'" + fecha + "', " + hora + ")";
                            String insAud1SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 1, " + ultSec1 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Ciudad', '" + depCdad[1].trim() + "', 'Cod. Ciudad " + cta.getCiudad() + "', 0,'" + fecha + "', " + hora + ")";
                            String insAud2SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 2, " + ultSec2 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Ciudad', '" + depCdad[1].trim() + "', 'Cod. Ciudad " + cta.getCiudad() + "', 0,'" + fecha + "', " + hora + ")";
                            String insAud3SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 3, " + ultSec3 + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Ciudad', '" + depCdad[1].trim() + "', 'Cod. Ciudad " + cta.getCiudad() + "', 0,'" + fecha + "', " + hora + ")";
                            if (nroSec[0].equals("0") || nroSec[1].equals("0") || nroSec[2].equals("0") || nroSec[3].equals("0")) {
                                boolean respR = DBUtils.ejecucionSQL(insAudRSQL, conn);
                            }
                            if (nroSec[0].equals("1") || nroSec[1].equals("1") || nroSec[2].equals("1") || nroSec[3].equals("1")) {
                                boolean resp1 = DBUtils.ejecucionSQL(insAud1SQL, conn);
                            }
                            if (nroSec[0].equals("2") || nroSec[1].equals("2") || nroSec[2].equals("2") || nroSec[3].equals("2")) {
                                boolean resp2 = DBUtils.ejecucionSQL(insAud2SQL, conn);
                            }
                            if (nroSec[0].equals("3") || nroSec[1].equals("3") || nroSec[2].equals("3") || nroSec[3].equals("3")) {
                                boolean resp3 = DBUtils.ejecucionSQL(insAud3SQL, conn);
                            }
                            LOGGER.info("CIUDAD MODIFICADA CORRECTAMENTE");
                        }
                    }
                    if (!cta.getZona().isEmpty() && !cta.getZona().equals("")) {
                        String consZonaSQL = "select cezona, cesecue from Gxbdbps.Gdireaf where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue in (0, 1, 2, 3)";
                        PreparedStatement stmt1 = null;
                        stmt1 = conn.prepareStatement(consZonaSQL);
                        stmt1.executeQuery();
                        ResultSet rs1 = stmt1.getResultSet();
                        String zona = "";
                        String[] nroSec = new String[4];
                        int i = 0;
                        while (rs1.next()) {
                            if (!rs1.getString(1).trim().isEmpty() && rs1.getString(1) != null) {
                                zona = rs1.getString(1);
                            }
                            nroSec[i] = rs1.getString(2);
                            i++;
                        }
                        long ultSecR = DBUtils.getSecuencia(secDirRSQL, conn);
                        long ultSec1 = DBUtils.getSecuencia(secDir1SQL, conn);
                        long ultSec2 = DBUtils.getSecuencia(secDir2SQL, conn);
                        long ultSec3 = DBUtils.getSecuencia(secDir3SQL, conn);
                        String updDir1SQL = "update gxbdbps.gdireaf set cezona = 'Cod. Zona " + cta.getZona() + "' where enemiso = '021' and cetipod = '" + tipoDoc + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue in (0, 1, 2, 3)";
                        boolean resp = DBUtils.ejecucionSQL(updDir1SQL, conn);
                        String insAudRSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 0, " + ultSecR + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Zona', '" + zona + "', 'Cod. Zona " + cta.getZona() + "', 0,'" + fecha + "', " + hora + ")";
                        String insAud1SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 1, " + ultSec1 + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Zona', '" + zona + "', 'Cod. Zona " + cta.getZona() + "', 0,'" + fecha + "', " + hora + ")";
                        String insAud2SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 2, " + ultSec2 + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Zona', '" + zona + "', 'Cod. Zona " + cta.getZona() + "', 0,'" + fecha + "', " + hora + ")";
                        String insAud3SQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 3, " + ultSec3 + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Zona', '" + zona + "', 'Cod. Zona " + cta.getZona() + "', 0,'" + fecha + "', " + hora + ")";
                        if (nroSec[0].equals("0") || nroSec[1].equals("0") || nroSec[2].equals("0") || nroSec[3].equals("0")) {
                            boolean respR = DBUtils.ejecucionSQL(insAudRSQL, conn);
                        }
                        if (nroSec[0].equals("1") || nroSec[1].equals("1") || nroSec[2].equals("1") || nroSec[3].equals("0")) {
                            boolean resp1 = DBUtils.ejecucionSQL(insAud1SQL, conn);
                        }
                        if (nroSec[0].equals("2") || nroSec[1].equals("2") || nroSec[2].equals("2") || nroSec[3].equals("0")) {
                            boolean resp2 = DBUtils.ejecucionSQL(insAud2SQL, conn);
                        }
                        if (nroSec[0].equals("3") || nroSec[1].equals("3") || nroSec[2].equals("3") || nroSec[3].equals("0")) {
                            boolean resp3 = DBUtils.ejecucionSQL(insAud3SQL, conn);
                        }
                        LOGGER.info("ZONA MODIFICADA CORRECTAMENTE");
                    }
                }
                //Se inicia la modificacion de la cuenta
                String consCtaSQL = "select mctipcb, mcforpg, mctippg, mcctabc, mcpmfij, mcfefij, mccodsc, mclimco, mclimcu, mcdisco, mcdiscu from Gxbdbps.tmctaaf where mcnumct = " + cta.getNroCuenta() + "";
                PreparedStatement stmt42 = null;
                stmt42 = conn.prepareStatement(consCtaSQL);
                stmt42.executeQuery();
                ResultSet rs42 = stmt42.getResultSet();
                String tipCtaB = "";
                String formaPago = "";
                String tipoPago = "";
                String ctaBanco = "";
                String montoPgMinFij = "";
                String fecHasPgMinFij = "";
                String sucursal = "";
                String limNormal = "";
                String limCuota = "";
                String dispNormal = "";
                String dispCuota = "";
                while (rs42.next()) {
                    tipCtaB = rs42.getString(1);
                    formaPago = rs42.getString(2);
                    tipoPago = rs42.getString(3);
                    ctaBanco = rs42.getString(4);
                    montoPgMinFij = rs42.getString(5);
                    fecHasPgMinFij = rs42.getString(6);
                    sucursal = rs42.getString(7);
                    limNormal = rs42.getString(8);
                    limCuota = rs42.getString(9);
                    dispNormal = rs42.getString(10);
                    dispCuota = rs42.getString(11);
                }
                stmt42.close();
                //Se obtiene el ultimo valor de la secuencia de cuenta
                String ultSecSQL4 = "select max(acnumse) from Gxbdbps.auctaaf where acnumct = " + cta.getNroCuenta() + " and acemiso = '021'"; //and acafini = '" + cta.getCodAfin() + "'";
                PreparedStatement stmt43 = null;
                stmt43 = conn.prepareStatement(ultSecSQL4);
                stmt43.executeQuery();
                ResultSet rs43 = stmt43.getResultSet();
                String secAudCta = "";
                while (rs43.next()) {
                    secAudCta = rs43.getString(1);
                }
                stmt43.close();
                //LOGGER.info("Se obtuvo ultima secuencia de auditoria cuentas: " + secAudCta);
                long nroAudCta = 0;
                if (secAudCta != null) {
                    if (!secAudCta.equals("")) {
                        nroAudCta = Long.parseLong(secAudCta);
                    }
                }
                String updCtaSQL = "update Gxbdbps.tmctaaf set ";
                String updCtaSQL2 = " where mcnumct = " + cta.getNroCuenta();
                String insertCtaSQLAud = "insert into Gxbdbps.auctaaf values";
                int bandera2 = 0;
//            if (!cta.getNombre1().isEmpty()) {
//                updCtaSQL += "cenomb1 = " + cta.getNombre1() + ",";
//                idnum2++;
//                insertSQLAudi2 += "insert into Gxbdbps.auctaaf values('" + fechaComercial + "', '021', '" + cta.getCodAfin() + "', '" + cta.getNroCuenta() + "', '" + idnum + "', 'M', 1,"
//                    + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Estado', '" + situaActual + ", '" + situaFuturo + "', 0, '" + cta.getCodSuc()+ "');";
//                bandera2 ++;
//            } 
                if (!cta.getModPago().isEmpty() && cta.getModPago() != null) {
                    String modoPago = mapearFormP(cta.getModPago());
                    updCtaSQL += "mcforpg = '" + modoPago + "',";
                    nroAudCta++;
                    insertCtaSQLAud += "('" + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + nroAudCta + "', 'M', 1,"
                            + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Forma de Pago', '" + formaPago + "', '" + modoPago + "', 0, '" + sucursal + "'),";
                    bandera2++;
                }
                if (!cta.getTipPago().isEmpty() && cta.getTipPago() != null) {
                    updCtaSQL += "mctippg = '" + cta.getTipPago() + "',";
                    nroAudCta++;
                    insertCtaSQLAud += "('" + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + nroAudCta + "', 'M', 1,"
                            + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Tipo de Pago', '" + tipoPago + "', '" + cta.getTipPago() + "', 0, '" + sucursal + "'),";
                    bandera2++;
                }
                if (cta.getTipLin1Norm().equals("1") && !cta.getLinCredNorm().isEmpty() && cta.getLinCredNorm() != null) {
                    float saldoAnt = Float.parseFloat(limNormal) - Float.parseFloat(dispNormal);
                    float nuevoDisponible = Float.parseFloat(cta.getLinCredNorm()) - saldoAnt;
                    String disponible = nuevoDisponible + "";
                    updCtaSQL += "mclimco = " + cta.getLinCredNorm() + ", mcdisco = " + disponible + ",";
                    nroAudCta++;
                    insertCtaSQLAud += "('" + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + nroAudCta + "', 'M', 1,"
                            + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Linea Credito para Compras', '" + limNormal + "', '" + cta.getLinCredNorm() + "', 0, '" + sucursal + "'),('"
                            + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + ++nroAudCta + "', 'M', 1,"
                            + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Disponible para compras', '" + dispNormal + "', '" + disponible + "', 0, '" + sucursal + "'),";
                    bandera2++;
                }
                if (cta.getTipLin2Cuota().equals("2") && !cta.getLinCredCuota().isEmpty() && cta.getLinCredCuota() != null) {
                    float saldoAnt = Float.parseFloat(limCuota) - Float.parseFloat(dispCuota);
                    float nuevoDisponible = Float.parseFloat(cta.getLinCredCuota()) - saldoAnt;
                    String disponible = nuevoDisponible + "";
                    updCtaSQL += "mclimcu = " + cta.getLinCredCuota() + ", mcdiscu = " + disponible + ",";
                    nroAudCta++;
                    insertCtaSQLAud += "('" + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + nroAudCta + "', 'M', 1,"
                            + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Límite Compra Cuotas', '" + limCuota + "', '" + cta.getLinCredCuota() + "', 0, '" + sucursal + "'),('"
                            + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + ++nroAudCta + "', 'M', 1,"
                            + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Disponible Compra Cuotas', '" + dispCuota + "', '" + disponible + "', 0, '" + sucursal + "'),";
                    bandera2++;
                }
                if (!cta.getTipCtaBanc().isEmpty() && cta.getTipCtaBanc() != null) {
                    String newTipoCtaB = mapearCtaB(Integer.parseInt(cta.getTipCtaBanc()));
                    updCtaSQL += "mctipcb = '" + newTipoCtaB + "',";
                    nroAudCta++;
                    insertCtaSQLAud += "('" + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + nroAudCta + "', 'M', 1,"
                            + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Tipo Cuenta Banco', '" + tipCtaB + "', '" + newTipoCtaB + "', 0, '" + sucursal + "'),";
                    bandera2++;
                }
                if (!cta.getCtaBanc().isEmpty() && cta.getCtaBanc() != null) {
                    updCtaSQL += "mcctabc = '" + cta.getCtaBanc() + "',";
                    nroAudCta++;
                    insertCtaSQLAud += "('" + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + nroAudCta + "', 'M', 1,"
                            + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Cuenta Banco', '" + ctaBanco + "', '" + cta.getCtaBanc() + "', 0, '" + sucursal + "'),";
                    bandera2++;
                }
                if (!cta.getImpoFijoPagMin().isEmpty() && cta.getImpoFijoPagMin() != null) {
                    updCtaSQL += "mcpmfij = " + cta.getImpoFijoPagMin() + ",";
                    nroAudCta++;
                    insertCtaSQLAud += "('" + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + nroAudCta + "', 'M', 1,"
                            + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Monto pgo. min. fijo', '" + montoPgMinFij + "', '" + cta.getImpoFijoPagMin() + "', 0, '" + sucursal + "'),";
                    bandera2++;
                }
//            if (!cta.getFechaNac().isEmpty()) {
//                updCtaSQL += "cefenac = " + cta.getFechaNac() + ",";
//                idnum++;
//                insertCtaSQLAud += "insert into Gxbdbps.auctaaf values('" + fechaComercial + "', '021', '" + cta.getCodAfin() + "', '" + cta.getNroCuenta() + "', '" + idnum2 + "', 'M', 1,"
//                    + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Estado', '" + situaActual + ", '" + situaFuturo + "', 0, '" + cta.getCodSuc()+ "');";
//                bandera2 ++;
//            }
                if (bandera2 != 0) {
                    //Se extrae la ultima coma del string
                    updCtaSQL = updCtaSQL.trim().substring(0, updCtaSQL.length() - 1);
                    String updCtaSQL3 = updCtaSQL + updCtaSQL2;
                    //LOGGER.info(updCtaSQL3);
                    Statement stmt44 = null;
                    stmt44 = conn.createStatement();
                    stmt44.executeUpdate(updCtaSQL3);

                    insertCtaSQLAud = insertCtaSQLAud.trim().substring(0, insertCtaSQLAud.length() - 1);
                    //LOGGER.info(insertCtaSQLAud);
                    Statement stmt45 = null;
                    stmt45 = conn.createStatement();
                    stmt45.executeUpdate(insertCtaSQLAud);
                    LOGGER.info("CUENTA MODIFICADA CORRECTAMENTE");

                    stmt44.close();
                    stmt45.close();
                }

                //Se inicia modificacion de calificacion
                if (!cta.getCalifBCP().trim().isEmpty() && !cta.getCalifBCP().equals("")) {
                    String fechCalif = Utils.obtenerUltimoDiaMes();
                    String conCalifSQL = "select clffch from Gxbdbps.tgtclf where clfcci = '" + cta.getNroDoc() + "' and clffch = '" + fechCalif + "'";
                    PreparedStatement stmt46 = null;
                    stmt46 = conn.prepareStatement(conCalifSQL);
                    stmt46.executeQuery();
                    ResultSet rs46 = stmt46.getResultSet();
                    String fechaCalifica = "";
                    while (rs46.next()) {
                        fechaCalifica = rs46.getString(1);
                    }
                    stmt46.close();
                    if (!fechaCalifica.isEmpty()) {
                        String updCalifSQL = "update Gxbdbps.tgtclf set clfcal = '" + cta.getCalifBCP() + "' where clfcci = '" + cta.getNroDoc() + "' and clffch = '" + fechCalif + "'";
                        Statement stmt47 = null;
                        stmt47 = conn.createStatement();
                        stmt47.executeUpdate(updCalifSQL);
                        stmt47.close();
                        LOGGER.info("CALIFICACION BNF MODIFICADA CORRECTAMENTE");
                    } else {
                        DatoCliente cliente = getDatosCliente(cta.getNroDoc(), "021", cta.getTipoDoc());
                        String nombres = cliente.getCenomb1().trim() + " " + cliente.getCenomb2().trim();
                        String apellidos = cliente.getCeapel1().trim() + " " + cliente.getCeapel2().trim();
                        String inserCalBNF = "insert into gxbdbps.tgtclf  values('" + cta.getNroDoc() + "', '" + fechCalif + "', '" + nombres + "', '" + apellidos + "', '" + cta.getCalifBCP() + "', " + fecha
                                + ", '" + cta.getUserActualiza() + "')";
                        Statement stmt48 = null;
                        stmt48 = conn.createStatement();
                        stmt48.executeUpdate(inserCalBNF);
                        stmt48.close();
                        LOGGER.info("CALIFICACION BNF INSERTADA CORRECTAMENTE");
                    }
                }
                //LOGGER.info("LLEGO 20");

                //Se inicia la modificacion del cliente codeudor
                if (!cta.getNroDocCod().trim().isEmpty()) {
                    if (tipoDocCod.isEmpty()) {
                        String tipDocCodSQL = "select cetipod from Gxbdbps.gclieaf where cenumdo = " + cta.getNroDocCod() + " and enemiso = '021'";
                        PreparedStatement stmt50 = null;
                        stmt50 = conn.prepareStatement(tipDocCodSQL);
                        stmt50.executeQuery();
                        ResultSet rs50 = stmt50.getResultSet();
                        while (rs50.next()) {
                            tipoDocCod = rs50.getString(1);
                        }
                    }

                    String consCliCodSQL = "select cenomb1, cenomb2, ceapel1, ceapel2, cefenac, cesexo, ceecivi, prcodig from Gxbdbps.gclieaf where enemiso = '021' "
                            + "and cetipod = '" + tipoDocCod + "' and cenumdo = '" + cta.getNroDocCod() + "'";
                    PreparedStatement stmt49 = null;
                    stmt49 = conn.prepareStatement(consCliCodSQL);
                    stmt49.executeQuery();
                    ResultSet rs49 = stmt49.getResultSet();
                    String nombre1Cod = "";
                    String nombre2Cod = "";
                    String apellido1Cod = "";
                    String apellido2Cod = "";
                    String feNacCod = "";
                    String sexoCod = "";
                    String estCivilCod = "";
                    String profCod = "";
                    while (rs49.next()) {
                        nombre1Cod = rs49.getString(1);
                        nombre2Cod = rs49.getString(2);
                        apellido1Cod = rs49.getString(3);
                        apellido2Cod = rs49.getString(4);
                        feNacCod = rs49.getString(5);
                        sexoCod = rs49.getString(6);
                        estCivilCod = rs49.getString(7);
                        profCod = rs49.getString(8);
                    }
                    stmt49.close();
                    //Se obtiene el ultimo valor de la secuencia clientes
                    String secCliCodSQL = "select max(alnumse) from Gxbdbps.aucliaf where alnumdo = " + cta.getNroDocCod() + " and alemiso = '021' and altipod = '" + tipoDocCod + "'";
                    PreparedStatement stmt50 = null;
                    stmt50 = conn.prepareStatement(secCliCodSQL);
                    stmt50.executeQuery();
                    ResultSet rs50 = stmt50.getResultSet();
                    String secAudCliCod = "";
                    while (rs50.next()) {
                        secAudCliCod = rs50.getString(1);
                    }
                    stmt50.close();
                    //LOGGER.info("Se obtuvo ultima secuencia de auditoria clientes codeudores: " + secAudCliCod);
                    long nroAudCliCod = 0;
                    if (secAudCliCod != null) {
                        if (!secAudCliCod.equals("")) {
                            nroAudCliCod = Long.parseLong(secAudCliCod);
                        }
                    }

                    String updCliCodSQL1 = "update Gxbdbps.gclieaf set ";
                    String updCliCodSQL2 = " where enemiso = '021' " + "and cetipod = '" + tipoDocCod + "' and cenumdo = '" + cta.getNroDocCod() + "'";
                    String insCliCodSQL = "insert into Gxbdbps.aucliaf values";
                    int bandera3 = 0;
                    if (!cta.getNombre1Cod().isEmpty() && cta.getNombre1Cod() != null) {
                        updCliCodSQL1 += "cenomb1 = '" + cta.getNombre1Cod() + "',";
                        nroAudCliCod++;
                        insCliCodSQL += "('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', " + nroAudCliCod + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Primer Nombre', '" + nombre1Cod + "', '" + cta.getNombre1Cod() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera3++;
                    }
                    if (!cta.getNombre2Cod().isEmpty() && cta.getNombre2Cod() != null) {
                        updCliCodSQL1 += "cenomb2 = '" + cta.getNombre2Cod() + "',";
                        nroAudCliCod++;
                        insCliCodSQL += "('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', " + nroAudCliCod + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Segundo Nombre', '" + nombre2Cod + "', '" + cta.getNombre2Cod() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera3++;
                    }
                    if (!cta.getApellido1Cod().isEmpty() && cta.getApellido1Cod() != null) {
                        updCliCodSQL1 += "ceapel1 = '" + cta.getApellido1Cod() + "',";
                        nroAudCliCod++;
                        insCliCodSQL += "('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', " + nroAudCliCod + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Primer Apellido', '" + apellido1Cod + "', '" + cta.getApellido1Cod() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera3++;
                    }
                    if (!cta.getApellido2Cod().isEmpty() && cta.getApellido2Cod() != null) {
                        updCliCodSQL1 += "ceapel2 = '" + cta.getApellido2Cod() + "',";
                        nroAudCliCod++;
                        insCliCodSQL += "('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', " + nroAudCliCod + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Segundo Apellido', '" + apellido2Cod + "', '" + cta.getApellido2Cod() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera3++;
                    }
                    if (!cta.getFechNacCod().isEmpty() && cta.getFechNacCod() != null) {
                        updCliCodSQL1 += "cefenac = '" + cta.getFechNacCod() + "',";
                        nroAudCliCod++;
                        insCliCodSQL += "('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', " + nroAudCliCod + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Fecha Nacimiento', '" + feNacCod + "', '" + cta.getFechNacCod() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera3++;
                    }
                    if (!cta.getSexoCod().isEmpty() && cta.getSexoCod() != null) {
                        updCliCodSQL1 += "cesexo = '" + cta.getSexoCod() + "',";
                        nroAudCliCod++;
                        insCliCodSQL += "('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', " + nroAudCliCod + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Sexo', '" + sexoCod + "', '" + cta.getSexoCod() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera3++;
                    }
                    if (!cta.getEstCivCod().isEmpty() && cta.getEstCivCod() != null) {
                        updCliCodSQL1 += "ceecivi = '" + cta.getEstCivCod() + "',";
                        nroAudCliCod++;
                        insCliCodSQL += "('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', " + nroAudCliCod + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Estado Civil', '" + estCivilCod + "', '" + cta.getEstCivCod() + "', 0,'" + fecha + "', " + hora + "),";
                        bandera3++;
                    }
                    if (!cta.getOcupacionCod().isEmpty() && cta.getOcupacionCod() != null) {
                        String profesion = Utils.mapearOcupacion(cta.getOcupacionCod());
                        updCliCodSQL1 += "prcodig = '" + profesion + "',";
                        nroAudCliCod++;
                        insCliCodSQL += "('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', " + nroAudCliCod + ", 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', 0, 'Profesion', '" + profCod + "', '" + profesion + "', 0,'" + fecha + "', " + hora + "),";
                        bandera3++;
                    }
                    if (bandera3 != 0) {
                        //Se extrae la ultima coma del string
                        updCliCodSQL1 = updCliCodSQL1.trim().substring(0, updCliCodSQL1.length() - 1);
                        String updCliCodSQL = updCliCodSQL1 + updCliCodSQL2;
                        //LOGGER.info(updCliCodSQL);
                        Statement stmt51 = null;
                        stmt51 = conn.createStatement();
                        stmt51.executeUpdate(updCliCodSQL);

                        insCliCodSQL = insCliCodSQL.substring(0, insCliCodSQL.length() - 1);
                        //LOGGER.info(insCliCodSQL);
                        Statement stmt52 = null;
                        stmt52 = conn.createStatement();
                        stmt52.executeUpdate(insCliCodSQL);
                        LOGGER.info("CLIENTE CODEUDOR MODIFICADO CORRECTAMENTE");

                        stmt51.close();
                        stmt52.close();

                    }
                    //Se inicia la modificacion de direcciones del cliente codeudor
                    if (!cta.getDirEmailCod().isEmpty() && cta.getDirEmailCod() != null) {
                        //Se consulta si existe una direccion de correo del cliente codeudor 
                        String mailCodSQL = "select cedirec from Gxbdbps.Gdireaf where enemiso = '021' and cetipod = '" + tipoDocCod + "' and cenumdo = '" + cta.getNroDocCod() + "' and cediret = 'E'";
                        PreparedStatement stmt53 = null;
                        stmt53 = conn.prepareStatement(mailCodSQL);
                        stmt53.executeQuery();
                        ResultSet rs53 = stmt53.getResultSet();
                        String emailCod = "";
                        while (rs53.next()) {
                            emailCod = rs53.getString(1);
                        }
                        stmt53.close();
                        if (!emailCod.isEmpty() && emailCod != null) {
                            //Se obtiene el ultimo valor de la secuencia de direccion del cliente codeudor
                            String secDirCodSQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDocCod + "' and aenumdo = '" + cta.getNroDocCod() + "' and aesecue = 4";
                            PreparedStatement stmt54 = null;
                            stmt54 = conn.prepareStatement(secDirCodSQL);
                            stmt54.executeQuery();
                            ResultSet rs54 = stmt54.getResultSet();
                            String secDirCod = "";
                            while (rs54.next()) {
                                secDirCod = rs54.getString(1);
                            }
                            long nroDirCod = 0;
                            if (secDirCod != null) {
                                if (!secDirCod.equals("")) {
                                    nroDirCod = Long.parseLong(secDirCod);
                                    nroDirCod++;
                                }
                            }
                            String updDirCodSQL = "update Gxbdbps.gdireaf set cedirec = " + cta.getDirEmailCod() + " where enemiso = '021' and cetipod = '" + tipoDocCod + "' and cenumdo = '" + cta.getNroDocCod() + "' and cesecue = 4";
                            Statement stmt55 = null;
                            stmt55 = conn.createStatement();
                            stmt55.executeUpdate(updDirCodSQL);
                            String insAudDirCodSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', 4, " + nroDirCod + ", 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Direccion de Email', '" + emailCod + "', '" + cta.getDirEmailCod() + "', 0,'" + fecha + "', " + hora + ")";
                            //LOGGER.info(insAudDirCodSQL);
                            Statement stmt56 = null;
                            stmt56 = conn.createStatement();
                            stmt56.executeUpdate(insAudDirCodSQL);

                            stmt54.close();
                            stmt55.close();
                            stmt56.close();
                        } else {
                            String insDirCodSQL = "insert into gxbdbps.gdireaf values('021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', 4, '" + cta.getDirEmailCod() + "', 'S', 'E', '', '', '')";
                            Statement stmt57 = null;
                            stmt57 = conn.createStatement();
                            stmt57.executeUpdate(insDirCodSQL);
                            String insAudDirCodSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDocCod + "', '" + cta.getNroDocCod() + "', 4, 1, 'M', 1,"
                                    + " '" + cta.getUserActualiza() + "', 0, 'Direccion de Email', '', '" + cta.getDirEmailCod() + "', 0,'" + fecha + "', " + hora + ")";
                            Statement stmt58 = null;
                            stmt58 = conn.createStatement();
                            stmt58.executeUpdate(insAudDirCodSQL);
                            String updDirCodSQL = "update Gxbdbps.gclieaf set ceultsc = 1 where enemiso = '021' and cetipod = '" + tipoDocCod + "' and cenumdo = '" + cta.getNroDocCod() + "'";
                            Statement stmt59 = null;
                            stmt59 = conn.createStatement();
                            stmt59.executeUpdate(updDirCodSQL);

                            stmt57.close();
                            stmt58.close();
                            stmt59.close();
                        }
                        LOGGER.info("DIRECCION CLIENTE CODEUDOR MODIFICADO CORRECTAMENTE");
                    }
                }
                //LOGGER.info("LLEGO 30");
                //SE MODIFICA EXCEPCION DE COSTO POR CUENTA SI EXISTE
                String costoNoAplica = cta.getCostNoAplica().trim();
                if (!costoNoAplica.isEmpty() && costoNoAplica.length() != 0) {
                    for (int i = 0; i < costoNoAplica.length(); i += 3) {
                        String idCosto = costoNoAplica.substring(i, i + 3);
                        String consCostoSQL = "select cnctcar, cncaimp from Gxbdbps.tcuenocob where cnccue = " + cta.getNroCuenta() + " and cnctcar = " + idCosto;
                        PreparedStatement stmt60 = null;
                        stmt60 = conn.prepareStatement(consCostoSQL);
                        stmt60.executeQuery();
                        ResultSet rs60 = stmt60.getResultSet();
                        String consIdCosto = "";
                        String consImpuesto = "";
                        while (rs60.next()) {
                            consIdCosto = rs60.getString(1);
                            consImpuesto = rs60.getString(2);
                        }
                        if (!consIdCosto.isEmpty() && !consIdCosto.equals("") && !consImpuesto.equals(cta.getAplicaImp())) {
                            String updCostNoSQL = "update Gxbdbps.tcuenocob set cncaimp = '" + cta.getAplicaImp() + "', cncumo = '" + cta.getUserActualiza() + "', cncfmo = '" + fecha + "' where cnccue = " + cta.getNroCuenta()
                                    + " and cnctcar = " + idCosto;
                            Statement stmt61 = null;
                            stmt61 = conn.createStatement();
                            stmt61.executeUpdate(updCostNoSQL);
                            stmt61.close();
                        } else if (consIdCosto.isEmpty() || consIdCosto.equals("")) {
                            String insCosNoApl = "insert into Gxbdbps.tcuenocob values(" + cta.getNroCuenta() + ", " + idCosto + ", '" + cta.getUserActualiza() + "', '" + fecha + "', '" + cta.getAplicaImp()
                                    + "', '', '')";
                            //LOGGER.info(insCosNoApl);
                            Statement stmt62 = null;
                            stmt62 = conn.createStatement();
                            stmt62.executeUpdate(insCosNoApl);
                            stmt62.close();
                        }
                    }
                    LOGGER.info("EXCEPCIONES COSTOS POR CUENTA MODIFICADOS CORRECTAMENTE");
                }
                //LOGGER.info("LLEGO 40");
                //Se cargan las excepciones de cargo del cierre si vienen
                String cargoNoAplica = cta.getCargNoAplica().trim();
                if (!cargoNoAplica.isEmpty() && cargoNoAplica.length() != 0) {
                    for (int i = 0; i < cargoNoAplica.length(); i += 3) {
                        String idCargo = cargoNoAplica.substring(i, i + 3);
                        String consCargoSQL = "select cectcar from Gxbdbps.tctaexcar where ceccta = " + cta.getNroCuenta() + " and cectcar = " + idCargo;
                        PreparedStatement stmt63 = null;
                        stmt63 = conn.prepareStatement(consCargoSQL);
                        stmt63.executeQuery();
                        ResultSet rs63 = stmt63.getResultSet();
                        String consIdCargo = "";
                        while (rs63.next()) {
                            consIdCargo = rs63.getString(1);
                        }
                        if (consIdCargo.isEmpty() || consIdCargo.equals("")) {
                            String insCarNoApl = "insert into Gxbdbps.tctaexcar values(" + cta.getNroCuenta() + ", '" + idCargo + "', '" + cta.getUserActualiza() + "', '" + fecha + "', '', '', '')";
                            //LOGGER.info(insCarNoApl);
                            Statement stmt26 = null;
                            stmt26 = conn.createStatement();
                            stmt26.executeUpdate(insCarNoApl);
                            stmt26.close();
                        }
                    }
                    LOGGER.info("EXCEPCIONES CARGOS DEL CIERRE POR CUENTA MODIFICADOS CORRECTAMENTE");
                }
                //LOGGER.info("LLEGO 50");
                //Se verifica si el codigo sucursal es diferente al que se dio de alta
                if (!cta.getCodSuc().isEmpty() && cta.getCodSuc() != null) {
                    String sucursalNew = obtenerSucursal(cta.getCodSuc(), conn, fecha, cta.getUserActualiza(), afinidad);
                    String conSuc = "select mccodsc from Gxbdbps.tmctaaf where mcnumct = " + cta.getNroCuenta();
                    PreparedStatement stmt = null;
                    stmt = conn.prepareStatement(conSuc);
                    stmt.executeQuery();
                    ResultSet rs = stmt.getResultSet();
                    String suc = "";
                    while (rs.next()) {
                        suc = rs.getString(1);
                    }
                    if (!suc.trim().equals(sucursalNew)) {
                        //Se actualiza la sucursal de la cuenta
                        String updSucSQL = "update Gxbdbps.tmctaaf set mccodsc = '" + sucursalNew + "' where mcnumct = " + cta.getNroCuenta();
                        boolean sucAct = DBUtils.ejecucionSQL(updSucSQL, conn);
                        //Se obtiene el ultimo valor de la secuencia
                        String conSecSucSQL = "select max(acnumse) from Gxbdbps.auctaaf where acemiso = '021' and acafini = '" + afinidad + "' and acnumct = " + cta.getNroCuenta();
                        long secSuc = DBUtils.getSecuencia(conSecSucSQL, conn);
                        //LOGGER.info("Se obtuvo ultima secuencia de auditoria cuenta: " + secSuc);
                        //Se inserta un nuevo registro en el historico de Cuentas
                        String insAudCtaSQL = "insert into Gxbdbps.auctaaf values('" + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + secSuc + "', 'M', 1,"
                                + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Sucursal Cuenta', '" + suc + "', '" + sucursalNew + "', 0, '" + sucursalNew + "')";
                        boolean sucAud = DBUtils.ejecucionSQL(insAudCtaSQL, conn);
                        LOGGER.info("SUCURSAL CUENTA MODIFICADO CORRECTAMENTE");
                    }
                }
                setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                retorno = "Proceso submitido";
            } else {
                retorno += "164;";
                LOGGER.error("NUMERO DE CUENTA NO EXISTE");
                try {
                    conn.rollback();
                    setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
//                retorno = "ERROR AL PROCESAR LA SOLICITUD";
//                retorno = retorno + "," + errorMod;
                retorno = "Proceso submitido";
                return retorno;
            }

        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            //retorno = "ERROR AL PROCESAR LA SOLICITUD";
            try {
                conn.rollback();
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        LOGGER.info("---------- FINALIZA MODIFICACION DE CUENTA ----------");
        //LOGGER.info(retorno);
        LOGGER.info("");
        return retorno;
    }

    private String cambiarEstadoCta(Cuenta cta, Connection conn, long secTrx, String datosEntrada) {
        //String errorSit = "";
        String fecha = Utils.obtenerFechaHora("yyyyMMdd");
        String hora = Utils.obtenerFechaHora("HHmm");
        String retorno = "";
        String situFuturo = mapearSituacionCtaTarj(cta.getSituacion());
        LOGGER.info("---------- INICIA CAMBIO ESTADO DE CUENTA ----------");
        LOGGER.info("SECUENCIA TRANSACCION: " + secTrx);
        try {
            if (verificarCta(cta.getNroCuenta(), conn)) {
                LOGGER.info("verificacion de cuenta aprobada");
                String afinidad = consultarAfinidad(cta.getNroCuenta(), conn).trim();
                //String sucursal = obtenerSucursal(cta.getCodSuc(), conn, fecha, cta.getUserActualiza(), afinidad);
                //Se obtiene la situacion actual de la cuenta
                String situacionSQL = "select mcstats, mccodsc from Gxbdbps.tmctaaf where mcnumct = " + cta.getNroCuenta();
                PreparedStatement stmt = null;
                stmt = conn.prepareStatement(situacionSQL);
                stmt.executeQuery();
                ResultSet rs = stmt.getResultSet();
                String situaActual = "";
                String sucursal = "";
                while (rs.next()) {
                    situaActual = rs.getString(1);
                    sucursal = rs.getString(2);
                }
                stmt.close();
                if (situaActual.trim().equals(situFuturo)) {
                    String resp = situaActual.equals("1") ? "Ativa" : "Inactiva";
                    retorno += situaActual.equals("1") ? "175;" : "176;";
                    LOGGER.info("Numero de Cuenta ya esta " + resp);
                    setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                    retorno = "Proceso submitido";
                    return retorno;
                }
                //Se actualiza la situacion de la cuenta
                String updateSQL = "update Gxbdbps.tmctaaf set mcstats = " + situFuturo + " where mcnumct = " + cta.getNroCuenta();
                Statement stmt2 = null;
                stmt2 = conn.createStatement();
                stmt2.executeUpdate(updateSQL);
                stmt2.close();

                //Se obtiene la Fecha Comercial 
                String fechaComercial = getFechaComercial();
                //Se obtiene el ultimo valor de la secuencia
                String ultSecSQL = "select max(acnumse) from Gxbdbps.auctaaf where acemiso = '021' and acafini = '" + afinidad + "' and acnumct = " + cta.getNroCuenta();
                long secAudCta = DBUtils.getSecuencia(ultSecSQL, conn);

                //Se inserta un nuevo registro en el historico de Cuentas
                String insertSQL = "insert into Gxbdbps.auctaaf values('" + fechaComercial + "', '021', '" + afinidad + "', '" + cta.getNroCuenta() + "', '" + secAudCta + "', 'M', 1,"
                        + " '" + cta.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Estado Cuenta', '" + situaActual + "', '" + situFuturo + "', 0, '" + sucursal + "')";
                Statement stmt4 = null;
                stmt4 = conn.createStatement();
                stmt4.executeUpdate(insertSQL);
                stmt4.close();

                LOGGER.info("ESTADO CUENTA MODIFICADO CORRECTAMENTE");
                setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                retorno = "Proceso submitido";
            } else {
                retorno += "164;";
                LOGGER.error("NUMERO DE CUENTA NO EXISTE");
                try {
                    conn.rollback();
                    setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
                retorno = "Proceso submitido";
                return retorno;
//                retorno = "ERROR AL PROCESAR LA SOLICITUD";
//                retorno = retorno + "," + errorSit;
            }
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            //retorno = "ERROR AL PROCESAR LA SOLICITUD";
            try {
                conn.rollback();
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        LOGGER.info("---------- FINALIZA CAMBIO ESTADO DE CUENTA ----------");
        //LOGGER.info(retorno);
        LOGGER.info("");
        return retorno;
    }

    private String borrarDirCta(Cuenta cta, Connection conn, long secTrx, String datosEntrada) {
        //String errorDir = "";
        String fecha = Utils.obtenerFechaHora("yyyyMMdd");
        String hora = Utils.obtenerFechaHora("HHmm");
        String tipoDoc = mapearTipoDoc(cta.getTipoDoc());
        String retorno = "";
        LOGGER.info("---------- INICIA BORRADO DE DIRECCION 2 Y 3 DE CUENTA ----------");
        LOGGER.info("SECUENCIA TRANSACCION: " + secTrx);
        try {
            if (verificarCta(cta.getNroCuenta(), conn)) {
                LOGGER.info("Verificacion de cuenta aprobada");
                if (cta.getNroDoc().trim().isEmpty() || cta.getNroDoc() == null) {
                    String consCtaSQL = "select mctipod, mcnumdo from Gxbdbps.tmctaaf where mcnumct = '" + cta.getNroCuenta() + "'";
                    PreparedStatement stmt = null;
                    stmt = conn.prepareStatement(consCtaSQL);
                    stmt.executeQuery();
                    ResultSet rs = stmt.getResultSet();
                    String numDoc = "";
                    while (rs.next()) {
                        tipoDoc = rs.getString(1);
                        numDoc = rs.getString(2);
                    }
                    cta.setNroDoc(numDoc.trim());
                    cta.setTipoDoc(tipoDoc.trim());
                    //LOGGER.info("Numero de documento consultado " + cta.getNroDoc());
                    //LOGGER.info("Tipo de documento consultado " + cta.getTipoDoc());
                } else if (!cta.getTipoDoc().trim().isEmpty() && cta.getTipoDoc() != null) {
                    String tipDocQL = "select cetipod from Gxbdbps.gclieaf where cenumdo = " + cta.getNroDoc() + " and enemiso = '021'";
                    PreparedStatement stmt50 = null;
                    stmt50 = conn.prepareStatement(tipDocQL);
                    stmt50.executeQuery();
                    ResultSet rs50 = stmt50.getResultSet();
                    while (rs50.next()) {
                        tipoDoc = rs50.getString(1);
                    }
                }
                //Se obtiene la ultima secuencia de direcciones de la cuenta
                String ultSecDirCliSQL = "select ceultsc from gxbdbps.gclieaf where enemiso = '021' and cetipod = '" + tipoDoc
                        + "' and cenumdo = '" + cta.getNroDoc() + "'";
                PreparedStatement stmt = null;
                stmt = conn.prepareStatement(ultSecDirCliSQL);
                stmt.executeQuery();
                ResultSet rs = stmt.getResultSet();
                String ultSec = "";
                while (rs.next()) {
                    ultSec = rs.getString(1);
                }
                int ultSecCli = Integer.parseInt(ultSec);
                stmt.close();
                if (cta.getDirExtr2().toUpperCase().equals("BORRAR")) {
                    //Se obtiene la direccion 2 de la cuenta
                    String direccion2SQL = "select cedirec from gxbdbps.gdireaf where enemiso = '021' and cetipod = '" + tipoDoc
                            + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 2 and cediret = 'O'";
                    PreparedStatement stmt2 = null;
                    stmt2 = conn.prepareStatement(direccion2SQL);
                    stmt2.executeQuery();
                    ResultSet rs2 = stmt2.getResultSet();
                    String dirAnt = "";
                    while (rs2.next()) {
                        dirAnt = rs2.getString(1);
                    }
                    //Se elimina el registro de Direccion Extracto2
                    String deleteSQL = "delete from Gxbdbps.gdireaf where enemiso = '021' and cetipod = '" + tipoDoc
                            + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 2 and cediret = 'O'";
                    Statement stmt3 = null;
                    stmt3 = conn.createStatement();
                    stmt3.executeUpdate(deleteSQL);

                    //Se obtiene la Fecha Comercial 
                    String fechaComercial = getFechaComercial();
                    //Se obtiene el ultimo valor de la secuencia de direcciones
                    String ultSecDirSQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '"
                            + cta.getNroDoc() + "' and aesecue = 2";
                    PreparedStatement stmt4 = null;
                    stmt4 = conn.prepareStatement(ultSecDirSQL);
                    stmt4.executeQuery();
                    ResultSet rs4 = stmt4.getResultSet();
                    String idSec = "";
                    while (rs4.next()) {
                        idSec = rs4.getString(1);
                    }
                    //LOGGER.info("Se obtuvo ultima secuencia de auditoria de direcciones : " + idSec);
                    long idnum = 1;
                    if (idSec != null) {
                        if (!idSec.equals("")) {
                            idnum = Long.parseLong(idSec);
                            idnum++;
                        }
                    }
                    //Se inserta un nuevo registro en el historico de Direcciones
                    String insertSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 2, '" + idnum + "', 'B', 1,"
                            + " '" + cta.getUserActualiza() + "', 0, 'Direccion Extracto 2', '" + dirAnt + "', '', 0,'" + fecha + "', " + hora + ")";
                    Statement stmt5 = null;
                    stmt5 = conn.createStatement();
                    stmt5.executeUpdate(insertSQL);
                    //Se inserta la nueva ultima secuencia de direcciones del cliente
                    ultSecCli--;
                    String ultDirCliSQL = "update gxbdbps.gclieaf set ceultsc = " + ultSecCli + " where enemiso = '021' and cetipod = '" + tipoDoc
                            + "' and cenumdo = '" + cta.getNroDoc() + "'";
                    Statement stmt6 = null;
                    stmt6 = conn.createStatement();
                    stmt6.executeUpdate(ultDirCliSQL);

                    stmt2.close();
                    stmt3.close();
                    stmt4.close();
                    stmt5.close();
                    stmt6.close();
                    LOGGER.info("DIRECCION 2 DE LA CUENTA BORRADO CORRECTAMENTE");
                }
                if (cta.getDirExtr3().toUpperCase().equals("BORRAR")) {
                    //Se obtiene la direccion 3 de la cuenta
                    String direccion2SQL = "select cedirec from gxbdbps.gdireaf where enemiso = '021' and cetipod = '" + tipoDoc
                            + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 3 and cediret = 'O'";
                    PreparedStatement stmt7 = null;
                    stmt7 = conn.prepareStatement(direccion2SQL);
                    stmt7.executeQuery();
                    ResultSet rs7 = stmt7.getResultSet();
                    String dirAnt = "";
                    while (rs7.next()) {
                        dirAnt = rs7.getString(1);
                    }
                    //Se elimina el registro de Direccion Extracto3
                    String deleteSQL = "delete from Gxbdbps.gdireaf where enemiso = '021' and cetipod = '" + tipoDoc
                            + "' and cenumdo = '" + cta.getNroDoc() + "' and cesecue = 3 and cediret = 'O'";
                    Statement stmt8 = null;
                    stmt8 = conn.createStatement();
                    stmt8.executeUpdate(deleteSQL);

                    //Se obtiene la Fecha Comercial 
                    String fechaComercial = getFechaComercial();
                    //Se obtiene el ultimo valor de la secuencia
                    String ultSecSQL = "select max(aenumse) from Gxbdbps.audiraf where aeemiso = '021' and aetipod = '" + tipoDoc + "' and aenumdo = '"
                            + cta.getNroDoc() + "' and aesecue = 3";
                    PreparedStatement stmt9 = null;
                    stmt9 = conn.prepareStatement(ultSecSQL);
                    stmt9.executeQuery();
                    ResultSet rs9 = stmt9.getResultSet();
                    String idSec = "";
                    while (rs9.next()) {
                        idSec = rs9.getString(1);
                    }
                    //LOGGER.info("Se obtuvo ultima secuencia de auditoria de direcciones: " + idSec);
                    long idnum = 1;
                    if (idSec != null) {
                        if (!idSec.equals("")) {
                            idnum = Long.parseLong(idSec);
                            idnum += 1;
                        }
                    }
                    //Se inserta un nuevo registro en el historico de Direcciones
                    String insertSQL = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + cta.getNroDoc() + "', 3, '" + idnum + "', 'B', 1,"
                            + " '" + cta.getUserActualiza() + "', 0, 'Direccion Extracto 3', '" + dirAnt + "', '', 0,'" + fecha + "', " + hora + ")";
                    Statement stmt10 = null;
                    stmt10 = conn.createStatement();
                    stmt10.executeUpdate(insertSQL);
                    //Se inserta la nueva ultima secuencia de direcciones del cliente
                    ultSecCli--;
                    String ultDirCliSQL = "update gxbdbps.gclieaf set ceultsc = " + ultSecCli + " where enemiso = '021' and cetipod = '" + tipoDoc
                            + "' and cenumdo = '" + cta.getNroDoc() + "'";
                    Statement stmt11 = null;
                    stmt11 = conn.createStatement();
                    stmt11.executeUpdate(ultDirCliSQL);

                    stmt7.close();
                    stmt8.close();
                    stmt9.close();
                    stmt10.close();
                    stmt11.close();
                    LOGGER.info("DIRECCION 3 DE LA CUENTA BORRADO CORRECTAMENTE");
                }
                setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                retorno = "Proceso submitido";
            } else {
                retorno += "164;";
                LOGGER.error("NO EXISTE NUMERO DE CUENTA");
                try {
                    conn.rollback();
                    setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
                retorno = "Proceso submitido";
                return retorno;
//                retorno = "ERROR AL PROCESAR LA SOLICITUD";
//                retorno = retorno + "," + errorDir;
            }
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            //retorno = "ERROR AL PROCESAR LA SOLICITUD";
            try {
                conn.rollback();
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        LOGGER.info("---------- FINALIZA BORRADO DE DIRECCION 2 Y 3 DE CUENTA ----------");
        //LOGGER.info(retorno);
        LOGGER.info("");
        return retorno;
    }

    private String borrarCostoCta(Cuenta cta, Connection conn, long secTrx, String datosEntrada) {
        //String errorCosto = "";
        String retorno = "";
        LOGGER.info("---------- INICIA BORRADO DE EXCEPCION DE COSTOS DE CUENTA ----------");
        LOGGER.info("SECUENCIA TRANSACCION: " + secTrx);
        try {
            if (verificarCta(cta.getNroCuenta(), conn)) {
                String costoNoAplica = cta.getCostNoAplica().trim();
                //LOGGER.info(costoNoAplica);
                costoNoAplica = costoNoAplica.toUpperCase().replace("BOR", "").trim();
                if (!costoNoAplica.isEmpty() && costoNoAplica.length() != 0) {
                    for (int i = 0; i < costoNoAplica.length(); i += 3) {
                        String idCosto = costoNoAplica.substring(i, i + 3);
                        //LOGGER.info(idCosto);
                        String delCosNoApl = "delete from Gxbdbps.tcuenocob where cnccue = " + cta.getNroCuenta() + " and cnctcar = " + idCosto;
                        //LOGGER.info(delCosNoApl);
                        Statement stmt1 = null;
                        stmt1 = conn.createStatement();
                        stmt1.executeUpdate(delCosNoApl);
                        stmt1.close();
                    }
                }
                setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                retorno = "Proceso submitido";
            } else {
                retorno += "164;";
                LOGGER.error("NO EXISTE NUMERO DE CUENTA");
                try {
                    conn.rollback();
                    setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
                retorno = "Proceso submitido";
                return retorno;
//                retorno = "ERROR AL PROCESAR LA SOLICITUD";
//                retorno = retorno + "," + errorCosto;
            }
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            //retorno = "ERROR AL PROCESAR LA SOLICITUD";
            try {
                conn.rollback();
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        LOGGER.info("---------- FINALIZA BORRADO DE EXCEPCION DE COSTOS DE CUENTA ----------");
        //LOGGER.info(retorno);
        LOGGER.info("");
        return retorno;
    }

    private String borrarCargoCta(Cuenta cta, Connection conn, long secTrx, String datosEntrada) {
        //String errorCargo = "";
        String retorno = "";
        LOGGER.info("---------- INICIA BORRADO DE EXCEPCION DE CARGOS DE CUENTA ----------");
        LOGGER.info("SECUENCIA TRANSACCION: " + secTrx);
        try {
            if (verificarCta(cta.getNroCuenta(), conn)) {
                String cargoNoAplica = cta.getCargNoAplica().trim();
                cargoNoAplica = cargoNoAplica.toUpperCase().replace("BOR", "").trim();
                if (!cargoNoAplica.isEmpty() && cargoNoAplica.length() != 0) {
                    for (int i = 0; i < cargoNoAplica.length(); i += 3) {
                        String idCargo = cargoNoAplica.substring(i, i + 3);
                        String delCarNoApl = "delete from Gxbdbps.tctaexcar where ceccta = " + cta.getNroCuenta() + " and cectcar = " + idCargo;
                        //LOGGER.info(delCarNoApl);
                        Statement stmt1 = null;
                        stmt1 = conn.createStatement();
                        stmt1.executeUpdate(delCarNoApl);
                        stmt1.close();
                    }
                }
                setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                retorno = "Proceso submitido";
            } else {
                retorno += "164;";
                LOGGER.error("NO EXISTE NUMERO DE CUENTA");
                try {
                    conn.rollback();
                    setDetalleTrxC(cta, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
                retorno = "Proceso submitido";
                return retorno;
//                retorno = "ERROR AL PROCESAR LA SOLICITUD";
//                retorno = retorno + "," + errorCargo;
            }
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            //retorno = "ERROR AL PROCESAR LA SOLICITUD";
            try {
                conn.rollback();
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        LOGGER.info("---------- FINALIZA BORRADO DE EXCEPCION DE CARGOS DE CUENTA ----------");
        //LOGGER.info(retorno);
        LOGGER.info("");
        return retorno;
    }

    private String altaTarjeta(Tarjeta tarj, Connection conn, long secTrx, String datosEntrada) {
        //String errorTarj = "";
        String fecha = Utils.obtenerFechaHora("yyyyMMdd");
        String hora = Utils.obtenerFechaHora("HHmm");
        Date fechDate = new Date();
        Timestamp fechaHora = new Timestamp(fechDate.getTime());
        String tipoDoc = mapearTipoDoc(tarj.getTipoDoc());
        String tipoTarj = "";
        String venc = "";
        String emboza = "";
        String[] tarjetaCuenta = new String[2];
        String retorno = "";
        boolean okTarjeta = false;
        LOGGER.info("---------- INICIA ALTA DE TARJETA ----------");
        LOGGER.info("SECUENCIA TRANSACCION: " + secTrx);
        LOGGER.info("TIPO DE TARJETA: " + tarj.getTipTarjeta());
//        LOGGER.info("NOMBRE DE PLASTICO: " + tarj.getNombPlastico());
//        LOGGER.info("DURACION: " + tarj.getDuracion());
//        LOGGER.info("EMBOZA: " + tarj.getEmboza());
        try {

            String retener = "";
            String sucursal = "";
            if (tarj.getTipTarjeta().trim().isEmpty() || tarj.getTipTarjeta() == null) {
                retorno += "144;";
                LOGGER.error("Tipo Tarjeta P/A vacio");
                setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                retorno = "Proceso submitido";
                return retorno;
            }
            if (tarj.getDuracion().trim().isEmpty() || tarj.getDuracion() == null) {
                retorno += "146;";
                LOGGER.error("Duración (en meses) vacia");
                setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                retorno = "Proceso submitido";
                return retorno;
            }
            if (tarj.getNombPlastico().trim().isEmpty() || tarj.getNombPlastico() == null) {
                retorno += "149;";
                LOGGER.error("Nombre Plástico vacio");
                setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                retorno = "Proceso submitido";
                return retorno;
            }
            if (tarj.getEmboza().trim().isEmpty() || tarj.getEmboza() == null) {
                tarj.setEmboza("S");
//                retorno += "160;";
//                LOGGER.error("Emboza S/N vacio");
//                setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
//                retorno = "Proceso submitido";
//                return retorno;
            }
            //Mapeamos el tipo de tarjeta, el vencimineto y el embozado
            tipoTarj = mapearTipoTarj(tarj.getTipTarjeta());
            venc = calcularVenc(Integer.parseInt(tarj.getDuracion()));
            emboza = mapearEmbozado(tarj.getEmboza());

            if (tarj.getIdCuenta().trim().isEmpty() && tarj.getNroTarjeta().trim().isEmpty() && !tarj.getNroControl().trim().isEmpty()) {
                String consCtaTarSQL = "select conumcta, conreten, cocodsuc from Gxbdbps.conctatar where conumcon = '" + tarj.getNroControl() + "'";
                //LOGGER.info(consCtaTarSQL);
                PreparedStatement stmt1 = null;
                stmt1 = conn.prepareStatement(consCtaTarSQL);
                stmt1.executeQuery();
                ResultSet rs1 = stmt1.getResultSet();
                String nroCuenta = "";
                while (rs1.next()) {
                    nroCuenta = rs1.getString(1);
                    retener = rs1.getString(2);
                    sucursal = rs1.getString(3);
                }
                stmt1.close();
                //Se verifica que la afinidad de la cuenta sea igual a al afinidad de la tarjeta
                String consAfinSQL = "select mcafini from Gxbdbps.tmctaaf WHERE mcemiso = '021' and mcnumct = " + nroCuenta.trim();
                PreparedStatement stmt = null;
                stmt = conn.prepareStatement(consAfinSQL);
                stmt.executeQuery();
                ResultSet rs = stmt.getResultSet();
                String afinRS = "";
                while (rs.next()) {
                    afinRS = rs.getString(1);
                }
                //Se verifica que los codigos de afinidad coincidan
                if (!afinRS.trim().equals(tarj.getIdAfin().trim())) {
                    retorno += "172;";
                    LOGGER.error("CODIGO DE AFINIDAD DE LA CUENTA Y TARJETA NO COINCIDEN");
                    setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                    retorno = "Proceso submitido";
                    return retorno;
                }
                //Se verifica si ya existe tarjeta principál
                if (tipoTarj.equals("1")) {
                    if (!verificarTarjP(nroCuenta, conn)) {
                        retorno += "173;";
                        LOGGER.error("LA CUENTA YA POSEE UNA TARJETA PRINCIPAL");
                        setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                        retorno = "Proceso submitido";
                        return retorno;
                    }
                }
                //Se crea el nro de tarjeta
                tarjetaCuenta = generarCtaTarj("021", "627431", tarj.getIdAfin(), "N", "S", conn).split(",");
                //cta.setNroCuenta(cuentaTarjeta[0]);
                tarj.setIdCuenta(nroCuenta);
                tarj.setNroTarjeta(tarjetaCuenta[1]);
                //LOGGER.info("Nro de cuenta creado: " + cuentaTarjeta[0]);
                //LOGGER.info("Nro de tarjeta creado: " + tarjetaCuenta[1]);
                okTarjeta = false;
            } else if (!tarj.getIdCuenta().trim().isEmpty() && tarj.getIdCuenta() != null) {
                if (verificarCta(tarj.getIdCuenta(), conn)) {
                    String consCtaTarSQL = "select conreten, cocodsuc from Gxbdbps.conctatar where conumcta = '" + tarj.getIdCuenta() + "'";
                    //LOGGER.info(consCtaTarSQL);
                    PreparedStatement stmt2 = null;
                    stmt2 = conn.prepareStatement(consCtaTarSQL);
                    stmt2.executeQuery();
                    ResultSet rs2 = stmt2.getResultSet();
                    while (rs2.next()) {
                        retener = rs2.getString(1);
                        sucursal = rs2.getString(2);
                    }
                    stmt2.close();
                    //Se verifica que la afinidad de la cuenta sea igual a al afinidad de la tarjeta
                    String consAfinSQL = "select mcafini from Gxbdbps.tmctaaf WHERE mcemiso = '021' and mcnumct = " + tarj.getIdCuenta();
                    PreparedStatement stmt = null;
                    stmt = conn.prepareStatement(consAfinSQL);
                    stmt.executeQuery();
                    ResultSet rs = stmt.getResultSet();
                    String afinRS = "";
                    while (rs.next()) {
                        afinRS = rs.getString(1);
                    }
                    if (!afinRS.trim().equals(tarj.getIdAfin().trim())) {
                        retorno += "172;";
                        LOGGER.error("CODIGO DE AFINIDAD DE LA CUENTA Y TARJETA NO COINCIDEN");
                        setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                        retorno = "Proceso submitido";
                        return retorno;
                    }
                    //Se verifica si ya existe tarjeta principál
                    if (tipoTarj.equals("1")) {
                        if (!verificarTarjP(tarj.getIdCuenta(), conn)) {
                            retorno += "173;";
                            LOGGER.error("LA CUENTA YA POSEE UNA TARJETA PRINCIPAL");
                            setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                            retorno = "Proceso submitido";
                            return retorno;
                        }
                    }
                    okTarjeta = verificarTarj(tarj.getNroTarjeta(), conn);
                } else {
                    retorno += "164;";
                    LOGGER.error("NO EXISTE NUMERO DE CUENTA");
                    try {
                        conn.rollback();
                        setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                    } catch (Exception sqlex) {
                        LOGGER.error("ERROR: " + sqlex);
                    }
                    retorno = "Proceso submitido";
                    return retorno;
                }
            } else {
                retorno += "165;";
                LOGGER.error("NUMERO DE CUENTA VACIO");
                try {
                    conn.rollback();
                    setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
                retorno = "Proceso submitido";
                return retorno;
            }

            //Solo se da de alta la tarjeta si no existe
            if (!okTarjeta) {
                if (tarj.getNroDoc().trim().isEmpty() || tarj.getNroDoc() == null) {
                    String consCliSQL = "select mcnumdo, mctipod from Gxbdbps.tmctaaf where mcnumct = " + tarj.getIdCuenta();
                    PreparedStatement stmt9 = null;
                    stmt9 = conn.prepareStatement(consCliSQL);
                    stmt9.executeQuery();
                    ResultSet rs9 = stmt9.getResultSet();
                    String mcnumdo = "";
                    while (rs9.next()) {
                        mcnumdo = rs9.getString(1);
                        tipoDoc = rs9.getString(2);
                    }
                    tarj.setNroDoc(mcnumdo.trim());
                } else {
                    //Verificar que el cliente exista, si no existe, cargar los datos
                    if (!existCliente(mapearTipoDoc(tarj.getTipoDoc()), tarj.getNroDoc(), conn)) {
                        if (tarj.getNombre1().trim().isEmpty() || tarj.getApellido1().trim().isEmpty()) {
                            retorno += "5;9;";
                            LOGGER.info("Nombre 1 y Apellido 1 del titular de la cuenta vacios");
                            setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                            retorno = "Proceso submitido";
                            return retorno;
                        }
                        int cantDire = 0;
                        if (!tarj.getDirEmailPer().isEmpty()) {
                            cantDire++;
                        }
                        //para verificar cod profesion
                        String profesion = Utils.mapearOcupacion(tarj.getOcupacion());
                        String fechaNac = modificarFormatoFecha(tarj.getFechaNac());
                        String nombreApellido = (tarj.getNombre1() + " " + tarj.getNombre2()).trim() + ", " + tarj.getApellido1() + " " + tarj.getApellido2();
                        //Si cliente no existe se insertan los datos
                        String insertCliSQL = "insert into Gxbdbps.gclieaf values('021', '" + tipoDoc + "', '" + tarj.getNroDoc() + "', '" + tarj.getNombre1() + "', '" + tarj.getNombre2()
                                + "', '" + tarj.getApellido1() + "', '" + tarj.getApellido2() + "', '', '" + tarj.getSexo() + "', '" + tarj.getEstadoCivil()
                                + "', '" + fechaNac + "', '" + tarj.getPaisNac() + "', '', '', '', '" + profesion + "', 'P', 0.00, " + cantDire + ", '', '" + nombreApellido
                                + "', '" + fecha + "', '" + hora + "', '" + tarj.getUserActualiza() + "', '', '" + tarj.getNroSocio() + "')";
                        if (DBUtils.ejecucionSQL(insertCliSQL, conn)) {
                            LOGGER.info("CLIENTE INSERTADO CORRECTAMENTE");
                        }
                        //Se obtiene la fecha comercial
                        String fechaComercial = getFechaComercial();
                        //Se inserta el nuevo cliente auditoria
                        String insertAudCliSQL = "insert into Gxbdbps.aucliaf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + tarj.getNroDoc() + "', 1,'A', 1, '" + tarj.getUserActualiza() + "', "
                                + "0, 'Alta Cliente', '', '', 0, '" + fecha + "', " + hora + ")";
                        if (DBUtils.ejecucionSQL(insertAudCliSQL, conn)) {
                            LOGGER.info("AUDITORIA CLIENTE INSERTADO CORRECTAMENTE");
                        }
                        //Se insertan las direcciones del cliente
                        String insertDirSQL = "insert into gxbdbps.gdireaf values";
                        if (!tarj.getDirEmailPer().isEmpty()) {
                            insertDirSQL += "('021', '" + tipoDoc + "', '" + tarj.getNroDoc() + "', 4, '" + tarj.getDirEmailPer() + "', 'S', 'E', '', '', '')";
                        }
                        if (insertDirSQL.length() > 35) {
                            //insertDirSQL = insertDirSQL.substring(0, insertDirSQL.length() - 1);
                            if (DBUtils.ejecucionSQL(insertDirSQL, conn)) {
                                LOGGER.info("DIRECCIONES DEl CLIENTE INSERTADAS CORRECTAMENTE");
                            }
                        } //else {
                        //retorno += "162;";
                        //LOGGER.info("Direccion/Nro telefono/Nro Celular para la cuenta vacio");
                        //try {
                        //    conn.rollback();
                        //    setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                        //} catch (Exception sqlex) {
                        //    LOGGER.error("ERROR: " + sqlex);
                        //}
                        //retorno = "Proceso submitido";
                        //return retorno;

                        //}
                        String insAudCliDirSQLAudi = "insert into Gxbdbps.audiraf values('" + fechaComercial + "', '021', '" + tipoDoc + "', '" + tarj.getNroDoc() + "', 0, 0, 'A', 1,"
                                + " '" + tarj.getUserActualiza() + "', 0, 'Alta Direcciones', '', '', 0,'" + fecha + "', " + hora + ")";
                        if (DBUtils.ejecucionSQL(insAudCliDirSQLAudi, conn)) {
                            LOGGER.info("AUDITORIA DE DIRECCIONES DEl CLIENTE INSERTADAS CORRECTAMENTE");
                        }
                    }
                }
                if (tarj.getTipTarjeta().equalsIgnoreCase("P")) {
                    String consCliSQL = "select mcnumdo, mctipod from Gxbdbps.tmctaaf where mcnumct = " + tarj.getIdCuenta();
                    PreparedStatement stmt9 = null;
                    stmt9 = conn.prepareStatement(consCliSQL);
                    stmt9.executeQuery();
                    ResultSet rs9 = stmt9.getResultSet();
                    String mcnumdo = "";
                    String mctipod = "";
                    while (rs9.next()) {
                        mcnumdo = rs9.getString(1);
                        mctipod = rs9.getString(2);
                    }
                    stmt9.close();
                    if (!mcnumdo.trim().equals(tarj.getNroDoc().trim())) {
                        retorno += "166;";
                        LOGGER.info("NUMERO DE DOCUMENTO DE LA CUENTA Y TARJETA PRINCIPAL NO COINCIDEN");
                        try {
                            conn.rollback();
                            setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                        } catch (Exception sqlex) {
                            LOGGER.error("ERROR: " + sqlex);
                        }
                        retorno = "Proceso submitido";
                        return retorno;
                    }
                }

                String maxSecTarSQL = "select max(mtultsc) from Gxbdbps.tmtaraf where enemiso = '021' and afafini = '" + tarj.getIdAfin() + "' and mcnumct = " + tarj.getIdCuenta();
                long secUltTar = DBUtils.getSecuencia(maxSecTarSQL, conn);

                //Se inserta la nueva tarjeta  
                String insertTarjSQL = "insert into Gxbdbps.tmtaraf values('" + tarj.getNroTarjeta() + "', " + secUltTar + ", '00', '1', '021', '" + sucursal//cta.getCodSuc()
                        + "', '" + tipoDoc + "', '" + tarj.getNroDoc() + "', '627431', '" + tarj.getIdAfin() + "', " + tarj.getIdCuenta() + ", '" + tipoTarj
                        + "', '', '" + tarj.getNombPlastico() + "', '', '00000000', '00000000', '00000000', " + venc + ", 0, '', '', '00000000', '" + emboza + "', '000',"
                        + "'', '" + retener + "', '" + tarj.getRenovAuto() + "', 'S', '" + fecha + "', '" + tarj.getUserActualiza() + "', '', '', '', '', '', 'E', '00000000')";
                //LOGGER.info(insertTarjSQL);
                Statement stmt4 = null;
                stmt4 = conn.createStatement();
                stmt4.executeUpdate(insertTarjSQL);
                stmt4.close();
                //Se inserta en la cuenta el nro de tarjeta principal
                if (tipoTarj.equals("1")) {
                    String udpCtaSQL = "update Gxbdbps.tmctaaf set mcnumta = " + tarj.getNroTarjeta() + " where mcnumct = " + tarj.getIdCuenta() + " and mcemiso = '021'";
                    // LOGGER.info(udpCtaSQL);
                    Statement stmt5 = null;
                    stmt5 = conn.createStatement();
                    stmt5.executeUpdate(udpCtaSQL);
                    stmt5.close();
                }

                //Se obtiene la Fecha Comercial 
                String fechaComercial = getFechaComercial();

                //Se inserta un nuevo registro en el historico de Tarjetas
                String insertSQL = "insert into Gxbdbps.autaraf values('" + fechaComercial + "', '021', '" + tarj.getIdAfin() + "', '" + tarj.getNroTarjeta() + "', 1, 'A', 1,"
                        + " '" + tarj.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Alta Tarjeta', '', '', 0, '" + sucursal + "')";//+ tarj.getSuc()+ "')";
                //LOGGER.info(insertSQL);
                Statement stmt6 = null;
                stmt6 = conn.createStatement();
                stmt6.executeUpdate(insertSQL);
                stmt6.close();
                //Se registra el numero de tarjeta principal
                if (tipoTarj.equals("1")) {
                    String updConTarjSQL = "update Gxbdbps.conctatar set conumtar = '" + tarj.getNroTarjeta() + "' where conumcta = '" + tarj.getIdCuenta() + "'";
                    //LOGGER.info(updConTarjSQL);
                    Statement stmt7 = null;
                    stmt7 = conn.createStatement();
                    stmt7.executeUpdate(updConTarjSQL);
                    stmt7.close();
                }
                //Se inserta promotor de tarjeta si existe
                if (!tarj.getNroDocProm().trim().isEmpty()) {
                    String consPromTarjSQL = "select ecidusr, eccodig from Gxbdbps.tectaaf where enemiso = '021' and ecidusr = '" + tarj.getNroDocProm() + "'";
                    //LOGGER.info(consPromTarjSQL);
                    PreparedStatement stmt8 = null;
                    stmt8 = conn.prepareStatement(consPromTarjSQL);
                    stmt8.executeQuery();
                    ResultSet rs8 = stmt8.getResultSet();
                    String promoTarj = "";
                    String codProTarj = "";
                    while (rs8.next()) {
                        promoTarj = rs8.getString(1);
                        codProTarj = rs8.getString(2);
                    }
                    stmt8.close();
                    if (promoTarj.trim().isEmpty()) {
                        String maxProTarSQL = "select max(eccodig) from Gxbdbps.tectaaf where enemiso = '021'";
                        long secProTar = DBUtils.getSecuencia(maxProTarSQL, conn);

                        codProTarj = String.format("%03d", secProTar);

                        if (!tarj.getNombre1Prom().trim().isEmpty() && !tarj.getApellido1Prom().trim().isEmpty()) {
                            String nombrePro = tarj.getNombre1Prom() + " " + tarj.getApellido1Prom();
                            //Se guarda al final con el nro de documento del oficial de cuenta
                            String insProTarSQL = "insert into Gxbdbps.tectaaf values('021', '" + codProTarj + "', '" + nombrePro + "', '" + fecha + "', '00000000', '" + tarj.getNroDocProm() + "')";
                            //LOGGER.info(insProTarSQL);
                            Statement stmt10 = null;
                            stmt10 = conn.createStatement();
                            stmt10.executeUpdate(insProTarSQL);
                            stmt10.close();

                            //Se inserta nuevo oficial en la tabla de oficiales y promotores
                            String insOfiSQL = "insert into Gxbdbps.tmanencta values('021', '" + codProTarj + "', '" + tarj.getNroDocProm() + "', '" + fecha + "', '" + tarj.getUserActualiza()
                                    + "', " + fecha + ", '', '" + fechaHora + "', 'N', 'S')";
                            //LOGGER.info(insOfiSQL);
                            Statement stmt25 = null;
                            stmt25 = conn.createStatement();
                            stmt25.executeUpdate(insOfiSQL);
                            stmt25.close();
                            LOGGER.info("PROMOTOR DE TARJETA INSERTADO CORRECTAMENTE");
                        } else {
                            retorno += "167;168;";
                            LOGGER.info("NOMBRE 1 Y APELLIDO 1 DEL PROMOTOR VACIOS");
                            try {
                                conn.rollback();
                                setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                            } catch (Exception sqlex) {
                                LOGGER.error("ERROR: " + sqlex);
                            }
                            retorno = "Proceso submitido";
                            return retorno;
//                            retorno = "ERROR AL PROCESAR LA SOLICITUD";
//                            retorno = retorno + "," + errorTarj;
                        }

                    }
                }
                //Se obtiene datos del codeudor si existe
                String tipDocCod = "";
                String nroDocCod = "";
                String codTarSQL = "";
                if (!tarj.getNroControl().trim().isEmpty()) {
                    codTarSQL = "select cetipod, cenumdo from Gxbdbps.gclieaf where enemiso = '021' and cenumdo = (select conumdco from Gxbdbps.conctatar where conumcon = '" + tarj.getNroControl() + "')";
                } else if (!tarj.getIdCuenta().trim().isEmpty()) {
                    codTarSQL = "select cetipod, cenumdo from Gxbdbps.gclieaf where enemiso = '021' and cenumdo = (select conumdco from Gxbdbps.conctatar where conumcta = '" + tarj.getIdCuenta() + "')";
                }
                //LOGGER.info(codTarSQL);
                PreparedStatement stmt11 = null;
                stmt11 = conn.prepareStatement(codTarSQL);
                stmt11.executeQuery();
                ResultSet rs11 = stmt11.getResultSet();
                while (rs11.next()) {
                    tipDocCod = rs11.getString(1);
                    nroDocCod = rs11.getString(2);
                }
                stmt11.close();

                //Se inserta codeudores si existe
                if (!nroDocCod.trim().isEmpty()) {
                    String insertCodeSQL = "insert into Gxbdbps.tcodeaf values('" + tarj.getNroTarjeta() + "', 1, '" + tipDocCod + "', '" + nroDocCod + "', '')";
                    //LOGGER.info(insertCodeSQL);
                    Statement stmt12 = null;
                    stmt12 = conn.createStatement();
                    stmt12.executeUpdate(insertCodeSQL);
                    stmt12.close();
                }

                setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                LOGGER.info("TARJETA INSERTADA CORRECTAMENTE");
                retorno = "Proceso submitido";
            } else {
                retorno += "170;";
                LOGGER.info("YA EXISTE NUMERO DE TARJETA");
                try {
                    conn.rollback();
                    setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
                retorno = "Proceso submitido";
                return retorno;
            }
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            //retorno = "ERROR AL PROCESAR LA SOLICITUD";
            try {
                conn.rollback();
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        LOGGER.info("---------- FINALIZA ALTA DE TARJETA ----------");
        //LOGGER.info(retorno);
        LOGGER.info("");
        return retorno;
    }

    private String modificarTarjeta(Tarjeta tarj, Connection conn, long secTrx, String datosEntrada) {
        //String errorTarj = "";
        String fecha = Utils.obtenerFechaHora("yyyyMMdd");
        String hora = Utils.obtenerFechaHora("HHmm");
        String retorno = "";
        LOGGER.info("---------- INICIA MODIFICACION DE TARJETA ----------");
        LOGGER.info("SECUENCIA TRANSACCION: " + secTrx);
        try {
            if (verificarTarj(tarj.getNroTarjeta(), conn)) {
                LOGGER.info("NUMERO DE TARJETA VERIFICADO CORRECTAMENTE");
                if (tarj.getIdCuenta().trim().isEmpty() || tarj.getIdCuenta() == null) {
                    String consTarjSQL = "select mcnumct, afafini from Gxbdbps.tmtaraf where enemiso = '021' and mtnumta = '" + tarj.getNroTarjeta().trim() + "'";

                    PreparedStatement stmt9 = null;
                    stmt9 = conn.prepareStatement(consTarjSQL);
                    stmt9.executeQuery();
                    ResultSet rs9 = stmt9.getResultSet();
                    String mcnumct = "";
                    String afafini = "";
                    while (rs9.next()) {
                        mcnumct = rs9.getString(1);
                        afafini = rs9.getString(2);
                    }
                    stmt9.close();
                    tarj.setIdCuenta(mcnumct.trim());
                    tarj.setIdAfin(afafini.trim());
                }
                //Se obtiene los datos actuales de la tarjeta
                String consTarjSQL = "select mtnopla, mtfeven, mtnoved, sucodig from Gxbdbps.tmtaraf where mtnumta = " + tarj.getNroTarjeta() + " and mcnumct = " + tarj.getIdCuenta();
                PreparedStatement stmt = null;
                stmt = conn.prepareStatement(consTarjSQL);
                stmt.executeQuery();
                ResultSet rs = stmt.getResultSet();
                String nomPlasticoAnt = "";
                String venceAnt = "";
                String marcaNovedadAnt = "";
                String sucursal = "";
                while (rs.next()) {
                    nomPlasticoAnt = rs.getString(1);
                    venceAnt = rs.getString(2);
                    marcaNovedadAnt = rs.getString(3);
                    sucursal = rs.getString(4);
                }
                stmt.close();
                //Se obtiene el ultimo valor de la secuencia
                String ultSecSQL = "select max(atnumse) from Gxbdbps.autaraf where atemiso = '021' and atafini = '" + tarj.getIdAfin() + "' and atnumta = '" + tarj.getNroTarjeta() + "'";
                PreparedStatement stmt2 = null;
                stmt2 = conn.prepareStatement(ultSecSQL);
                stmt2.executeQuery();
                ResultSet rs2 = stmt2.getResultSet();
                String secTarj = "";
                while (rs2.next()) {
                    secTarj = rs2.getString(1);
                }
                stmt2.close();

                long nroSecTar = 0;
                if (secTarj != null) {
                    if (!secTarj.equals("")) {
                        nroSecTar = Long.parseLong(secTarj);
                    }
                }
                //Se obtiene la Fecha Comercial 
                String fechaComercial = getFechaComercial();

                String updateTarjSQL1 = "update Gxbdbps.tmtaraf set ";
                String updateTarjSQL2 = " where mtnumta = '" + tarj.getNroTarjeta() + "'";
                String insAudTarjSQL = "insert into Gxbdbps.autaraf values";
                int bandera = 0;
                if (!tarj.getNombPlastico().isEmpty() && tarj.getNombPlastico() != null) {
                    updateTarjSQL1 += "mtnopla = '" + tarj.getNombPlastico() + "',";
                    nroSecTar++;
                    insAudTarjSQL += "('" + fechaComercial + "', '021', '" + tarj.getIdAfin() + "', '" + tarj.getNroTarjeta() + "', " + nroSecTar + ", 'M', 1,"
                            + " '" + tarj.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Nombre Plastico', '" + nomPlasticoAnt + "', '" + tarj.getNombPlastico() + "', 0, '" + sucursal + "'),"; //falta sucursal
                    bandera++;
                }
                if (!tarj.getDuracion().isEmpty() && tarj.getDuracion() != null) {
                    String venceNew = calcularVenc(Integer.parseInt(tarj.getDuracion()));
                    updateTarjSQL1 += "mtfeven = " + venceNew + ",";
                    nroSecTar++;
                    insAudTarjSQL += "('" + fechaComercial + "', '021', '" + tarj.getIdAfin() + "', '" + tarj.getNroTarjeta() + "', " + nroSecTar + ", 'M', 1,"
                            + " '" + tarj.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Vencimiento', '" + venceAnt + "', '" + venceNew + "', 0, '" + sucursal + "'),"; //falta sucursal
                    bandera++;
                }
                if (bandera != 0) {
                    //Se extrae la ultima coma del string
                    updateTarjSQL1 = updateTarjSQL1.trim().substring(0, updateTarjSQL1.length() - 1);
                    String updTarjSQL = updateTarjSQL1 + updateTarjSQL2;
                    //LOGGER.info(updTarjSQL);
                    Statement stmt3 = null;
                    stmt3 = conn.createStatement();
                    stmt3.executeUpdate(updTarjSQL);
                    stmt3.close();
                    insAudTarjSQL = insAudTarjSQL.substring(0, insAudTarjSQL.length() - 1);
                    //LOGGER.info(insAudTarjSQL);
                    Statement stmt4 = null;
                    stmt4 = conn.createStatement();
                    stmt4.executeUpdate(insAudTarjSQL);
                    //setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                    LOGGER.info("TARJETA MODIFICADA CORRECTAEMENTE");
                    stmt4.close();
                }
                setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                retorno = "Proceso submitido";
            } else {
                retorno += "170;";
                LOGGER.info("NO EXISTE NUMERO DE TARJETA");
                try {
                    conn.rollback();
                    setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
                retorno = "Proceso submitido";
                return retorno;
            }
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            //retorno = "ERROR AL PROCESAR LA SOLICITUD";
            try {
                conn.rollback();
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        LOGGER.info("---------- FINALIZA MODIFICACION DE TARJETA ----------");
        //LOGGER.info(retorno);
        LOGGER.info("");
        return retorno;
    }

    private String cambiarEstadoTarjeta(Tarjeta tarj, Connection conn, long secTrx, String datosEntrada) {
        //String errorTarj = "";
        String fecha = Utils.obtenerFechaHora("yyyyMMdd");
        String hora = Utils.obtenerFechaHora("HHmm");
        String retorno = "";
        String situaFuturo = mapearSituacionCtaTarj(tarj.getSituacion());
        LOGGER.info("---------- INICIA CAMBIO ESTADO DE TARJETA ----------");
        LOGGER.info("SECUENCIA TRANSACCION: " + secTrx);
        try {
            if (verificarTarj(tarj.getNroTarjeta(), conn)) {
                //Se obtiene la situacion actual de la tarjeta
                String situacionSQL = "select mtstats, sucodig, mcnumct from Gxbdbps.tmtaraf where mtnumta = " + tarj.getNroTarjeta();
                PreparedStatement stmt = null;
                stmt = conn.prepareStatement(situacionSQL);
                stmt.executeQuery();
                ResultSet rs = stmt.getResultSet();
                String situaActual = "";
                String sucursal = "";
                String cuenta = "";
                while (rs.next()) {
                    situaActual = rs.getString(1);
                    sucursal = rs.getString(2);
                    cuenta = rs.getString(3);
                }
                stmt.close();
                if (tarj.getIdCuenta().trim().isEmpty()) {
                    tarj.setIdCuenta(cuenta);
                }
                if (situaActual.trim().equals(situaFuturo)) {
                    String resp = situaActual.equals("1") ? "Ativa" : "Inactiva";
                    retorno += situaActual.equals("1") ? "177;" : "178;";
                    LOGGER.info("Numero de Tarjeta ya esta " + resp);
                    setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                    retorno = "Proceso submitido";
                    return retorno;
                }
                //Se actualiza la situacion de la tarjeta
                String updSitTarjSQL = "update Gxbdbps.tmtaraf set mtstats = " + situaFuturo + " where mtnumta = " + tarj.getNroTarjeta();
                Statement stmt2 = null;
                stmt2 = conn.createStatement();
                stmt2.executeUpdate(updSitTarjSQL);
                stmt2.close();
                //Se obtiene la Fecha Comercial 
                String fechaComercial = getFechaComercial();
                //Se obtiene el ultimo valor de la secuencia
                String ultSecSQL = "select max(atnumse) from Gxbdbps.autaraf where atemiso = '021' and atafini = '" + tarj.getIdAfin() + "' and atnumta = '" + tarj.getNroTarjeta() + "'";
                long secAudTar = DBUtils.getSecuencia(ultSecSQL, conn);
                //Se inserta un nuevo registro en el historico de Tarjetas
                String insertSQL = "insert into Gxbdbps.autaraf values('" + fechaComercial + "', '021', '" + tarj.getIdAfin() + "', '" + tarj.getNroTarjeta() + "', '" + secAudTar + "', 'M', 1,"
                        + " '" + tarj.getUserActualiza() + "', '" + fecha + "', " + hora + ", 0, 'Estado Tarjeta', '" + situaActual + "', '" + situaFuturo + "', 0, '" + sucursal + "')";//+ tarj.getSuc()+ "')";
                Statement stmt4 = null;
                stmt4 = conn.createStatement();
                stmt4.executeUpdate(insertSQL);
                stmt4.close();
                setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                LOGGER.info("ESTADO TARJETA MODIFICADO CORRECTAMENTE");
                retorno = "Proceso submitido";
            } else {
                retorno += "170;";
                LOGGER.info("NO EXISTE NUMERO DE TARJETA");
                try {
                    conn.rollback();
                    setDetalleTrxT(tarj, retorno, secTrx, datosEntrada, conn);
                } catch (Exception sqlex) {
                    LOGGER.error("ERROR: " + sqlex);
                }
                retorno = "Proceso submitido";
                return retorno;
            }
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            retorno = "ERROR AL PROCESAR LA SOLICITUD";
            try {
                conn.rollback();
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        LOGGER.info("---------- FINALIZA CAMBIO ESTADO DE TARJETA ----------");
        //LOGGER.info(retorno);
        LOGGER.info("");
        return retorno;
    }

    private String mapearSituacionCtaTarj(String sit) {
        String situacion = "";
        if (!sit.equals("") || sit != null) {
            if (sit.equals("A")) {
                situacion = "1";
            } else if (sit.equals("B") || sit.equals("I")) {
                situacion = "3";
            }
        }
        return situacion;
    }

    private String mapearTipoDoc(String doc) {
        String tipoDoc = "";
        if (!doc.equals("") || doc != null) {
            if (doc.equals("CIP")) {
                tipoDoc = "C";
            } else if (doc.equals("RUC")) {
                tipoDoc = "R";
            } else if (doc.equals("CAD") || doc.equals("DNI")) {
                tipoDoc = "E";
            } else {
                tipoDoc = "N";
            }
        }
        return tipoDoc;
    }

    private String mapearCtaB(int tipoBanco) {
        String tipoCB = "";
        if (tipoBanco == 1 || tipoBanco == 2) {
            if (tipoBanco == 1) {
                tipoCB = "A";
            } else if (tipoBanco == 2) {
                tipoCB = "C";
            }
        }
        return tipoCB;
    }

    private String mapearFormP(String formPag) {
        String forPago = "";
        if (!formPag.equals("") && formPag != null) {
            if (formPag.equals("V")) {
                forPago = "C";
            } else if (formPag.equals("D")) {
                forPago = "D";
            }
        }
        return forPago;
    }

    private String mapearCobroCosto(String cobCos) {
        String cobCosto = "";
        if (!cobCos.equals("") && !cobCos.isEmpty()) {
            if (cobCos.equals("S")) {
                cobCosto = "";
            } else if (cobCos.equals("N")) {
                cobCosto = "S";
            }
        }
        return cobCosto;
    }

    private String mapearTipoTarj(String tipT) {
        String tipTar = "";
        if (!tipT.equals("") && !tipT.isEmpty()) {
            if (tipT.equals("P")) {
                tipTar = "1";
            } else if (tipT.equals("A")) {
                tipTar = "3";
            }
        }
        //LOGGER.info("Tipo de tarjeta " + tipTar);
        return tipTar;
    }

    private String calcularVenc(int cantMeses) {
        String fecVenc = "";
        if (cantMeses > 0) {
            Calendar hoy = Calendar.getInstance();
            hoy.add(Calendar.MONTH, cantMeses);
            Date date = hoy.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyMM");
            fecVenc = sdf.format(date);
        }
        return fecVenc;
    }

    private String mapearEmbozado(String emb) {
        String embozar = "";
        if (!emb.equals("") || emb != null) {
            if (emb.equals("S")) {
                embozar = "1";
            } else if (emb.equals("N")) {
                embozar = "";
            }
        }
        return embozar;
    }

    private String mapearReten(String ret) {
        String retener = "";
        if (!ret.equals("") || ret != null) {
            if (ret.equals("S")) {
                retener = "R";
            } else if (ret.equals("N")) {
                retener = "E";
            }
        }
        return retener;
    }

    private boolean verificarCta(String cuenta, Connection conn) throws SQLException {
        boolean procesar = false;
        String consCtaSQL = "select mcnumct from Gxbdbps.tmctaaf where mcnumct = '" + cuenta + "'";
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement(consCtaSQL);
        stmt.executeQuery();
        ResultSet rs = stmt.getResultSet();
        String cta = "";
        while (rs.next()) {
            cta = rs.getString(1);
        }
        if (!cta.trim().isEmpty() && !cta.equals("") && cta != null) {
            procesar = true;
        }
        return procesar;
    }

    private boolean verificarTarj(String tarjeta, Connection conn) throws SQLException {
        boolean procesar = false;
        String consTarjSQL = "select mtnumta from Gxbdbps.tmtaraf where enemiso = '021' and bibines = '627431' and mtnumta = '" + tarjeta + "'";
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement(consTarjSQL);
        stmt.executeQuery();
        ResultSet rs = stmt.getResultSet();
        String tarj = "";
        while (rs.next()) {
            tarj = rs.getString(1);
        }
        if (!tarj.trim().isEmpty() && !tarj.equals("") && tarj != null) {
            procesar = true;
        }
        return procesar;
    }

    private boolean verificarTarjP(String cuenta, Connection conn) throws SQLException {
        boolean procesar = true;
        String consTarjSQL = "select mttipot from Gxbdbps.tmtaraf where enemiso = '021' and bibines = '627431' and mttipot = '1' and mcnumct = " + cuenta + "";
        //LOGGER.info(consTarjSQL);
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement(consTarjSQL);
        stmt.executeQuery();
        ResultSet rs = stmt.getResultSet();
        String tarjTipo = "";
        while (rs.next()) {
            tarjTipo = rs.getString(1);
        }

        if (tarjTipo.trim().equals("1")) {
            procesar = false;
        }
        return procesar;
    }

    //Se inserta la sucursal si no existe
    private String obtenerSucursal(String cod, Connection conn, String fecha, String userUdp, String afinidad) throws SQLException {

        String codSucursal = (!cod.trim().isEmpty()) ? String.format("%03d", Integer.parseInt(cod)) : "001";
        String conSucSQL = "select sucodig from Gxbdbps.gsucuaf where enemiso = '021' and sucodig = '" + codSucursal + "'";
        //LOGGER.info(conSucSQL);
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement(conSucSQL);
        stmt.executeQuery();
        ResultSet rs = stmt.getResultSet();
        String codSuc = "";
        while (rs.next()) {
            codSuc = rs.getString(1);
        }
        if (codSuc.isEmpty()) {
            String insSucSQL = "insert into Gxbdbps.gsucuaf values('021', '" + codSucursal + "', 'Suc. Continental','" + fecha + "', '00000000', '" + userUdp + "')";
            boolean inSuc1 = DBUtils.ejecucionSQL(insSucSQL, conn);

            String insSuc2SQL = "insert into Gxbdbps.gsaldaf values('021', '" + codSucursal + "', '627431', '" + afinidad + "', 0, 0, 0, 0, 0, 0)";
            boolean inSuc2 = DBUtils.ejecucionSQL(insSuc2SQL, conn);
            LOGGER.info("SUCURSAL INSERTADA CORRECTAMENTE");
        }
        return codSucursal;
    }

    private boolean verificarAfinidad(String codAfini, Connection conn) throws SQLException {
        boolean respuesta = false;
        String consAfinSQL = "select afafini from Gxbdbps.tafinaf where enemiso = '021' and bibines = '627431' and afafini = '" + codAfini + "'";
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement(consAfinSQL);
        stmt.executeQuery();
        ResultSet rs = stmt.getResultSet();
        String afinidad = "";
        while (rs.next()) {
            afinidad = rs.getString(1);
        }
        if (!afinidad.trim().isEmpty() && !afinidad.trim().equals("")) {
            respuesta = true;
        }
        return respuesta;
    }

    private boolean verificarBin(String nroTarj) {
        boolean rpta = false;
        if (nroTarj.contains("627431021")) {
            rpta = true;
        }
        return rpta;
    }

    private String consultarAfinidad(String cuenta, Connection conn) throws SQLException {
        String consAfinidad = "select mcafini from Gxbdbps.tmctaaf WHERE mcemiso = '021' and mcnumct =" + cuenta;
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement(consAfinidad);
        stmt.executeQuery();
        ResultSet rs = stmt.getResultSet();
        String afinidad = "";
        while (rs.next()) {
            afinidad = rs.getString(1);
        }
        return afinidad;
    }

    public static long setTrx(String tamanhoLote, String tipoLote) {
        //tipo lote 2 es cuenta y 1 es tarjeta
        Connection conn = null;
        long secuencia = 0;
        try {
            conn = DBUtils.connect();
            String fecha = Utils.obtenerFechaHora("yyyyMMdd");
            String hora = Utils.obtenerFechaHora("HH:mm:ss");
            String consSec = "select max(motasec) from Gxbdbps.tmmota";
            secuencia = DBUtils.getSecuencia(consSec, conn);
            String insTrx = "insert into Gxbdbps.tmmota values(" + secuencia + ", '" + fecha + "', '" + hora + "', " + tipoLote + ", 'S', 0, 0, " + tamanhoLote + ", " + tamanhoLote + ", 1)";
            //LOGGER.info(insTrx);
            boolean trxCta = DBUtils.ejecucionSQL(insTrx, conn);
        } catch (Exception ex) {
            LOGGER.error("Error en " + ex);
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                LOGGER.error("Error en " + ex);
            }
        }

        return secuencia;
    }

    public static void setDetalleTrxC(Cuenta cuenta, String errorCta, long secTrx, String datosCta, Connection conn) throws SQLException {
        String nroDoc = "";
        String nroRuc = "";
        String nombre = "";
        String estadoCta = "";
        String consSecLinea = "select max(demolin) from Gxbdbps.tdmota where motasec = " + secTrx;
        //LOGGER.info(consSecLinea);
        long secLinea = DBUtils.getSecuencia(consSecLinea, conn);
        errorCta = (errorCta.trim().isEmpty()) ? "" : errorCta.substring(0, errorCta.length() - 1);
        String estadoError = (errorCta.trim().isEmpty()) ? "1" : "3";
        if (!cuenta.getNroCuenta().trim().isEmpty() && ErrorUtils.isNumeric(cuenta.getNroCuenta())) {
            String consPersona = "select A.cenumdo, A.cenumru, A.ceapnom, B.mcstats from Gxbdbps.tmctaaf B INNER JOIN Gxbdbps.gclieaf A on B.mcemiso = A.enemiso and A.cetipod = B.mctipod and B.mcnumdo = A.cenumdo where B.mcnumct = " + cuenta.getNroCuenta().trim();
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(consPersona);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                nroDoc = rs.getString(1);
                nroRuc = rs.getString(2);
                nombre = rs.getString(3);
                estadoCta = rs.getString(4);
            }
        } else {
            nroDoc = cuenta.getNroDoc();
            nroRuc = cuenta.getRucExtr();
            nombre = (cuenta.getNombre1() + " " + cuenta.getNombre2()).trim() + ", " + cuenta.getApellido1() + " " + cuenta.getApellido2();
        }
        String nroCta = (!cuenta.getNroCuenta().trim().isEmpty()) ? cuenta.getNroCuenta() : "0";
        String insTrxCta = "insert into Gxbdbps.tdmota values(" + secTrx + ", '" + secLinea + "', 0, " + nroCta + ", '" + nombre + "', '" + nroDoc + "', '" + nroRuc + "', '" + estadoCta + "', " + estadoError + ", '" + errorCta + "', '" + datosCta + "')";
        //LOGGER.info(insTrxCta);
        boolean detalleTrx = DBUtils.ejecucionSQL(insTrxCta, conn);
        if (!errorCta.trim().isEmpty()) {
            String consError = "select motacerr from Gxbdbps.tmmota where motasec = " + secTrx;
            long sumador = DBUtils.getSecuencia(consError, conn);
            String udpTrxCta = "update Gxbdbps.tmmota set motacerr = " + sumador + " where motasec = " + secTrx;
            //LOGGER.info(udpTrxCta);
            boolean trxCta = DBUtils.ejecucionSQL(udpTrxCta, conn);
        } else {
            String consError = "select motaserr from Gxbdbps.tmmota where motasec = " + secTrx;
            long sumador = DBUtils.getSecuencia(consError, conn);
            String udpTrxCta = "update Gxbdbps.tmmota set motaserr = " + sumador++ + " where motasec = " + secTrx;
            //LOGGER.info(udpTrxCta);
            boolean trxCta = DBUtils.ejecucionSQL(udpTrxCta, conn);
        }
    }

    public static void setDetalleTrxT(Tarjeta tarjeta, String errorTarj, long secTrx, String datosTarj, Connection conn) throws SQLException {
        String nroDoc = "";
        String nroRuc = "";
        String nombre = "";
        String estadoCta = "";
        String nroCta = "0";
        String consSecLinea = "select max(demolin) from Gxbdbps.tdmota where motasec = " + secTrx;
        //LOGGER.info(consSecLinea);
        long secLinea = DBUtils.getSecuencia(consSecLinea, conn);
        errorTarj = (errorTarj.trim().isEmpty()) ? "" : errorTarj.substring(0, errorTarj.length() - 1);
        String estadoError = (errorTarj.trim().isEmpty()) ? "1" : "3";
        if (!tarjeta.getIdCuenta().trim().isEmpty() && ErrorUtils.isNumeric(tarjeta.getIdCuenta())) {
            String consPersona = "select A.cenumdo, A.cenumru, A.ceapnom, B.mcstats, B.mcnumct from Gxbdbps.tmctaaf B INNER JOIN Gxbdbps.gclieaf A on B.mcemiso = A.enemiso and A.cetipod = B.mctipod and B.mcnumdo = A.cenumdo where B.mcnumct = " + tarjeta.getIdCuenta().trim();
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(consPersona);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                nroDoc = rs.getString(1);
                nroRuc = rs.getString(2);
                nombre = rs.getString(3);
                estadoCta = rs.getString(4);
                nroCta = rs.getString(5);
            }
            stmt.close();
        } else if (!tarjeta.getNroControl().trim().isEmpty()) {
            String consPersona = "select A.cenumdo, A.cenumru, A.ceapnom, B.mcstats, B.mcnumct from Gxbdbps.tmctaaf B INNER JOIN Gxbdbps.gclieaf A on B.mcemiso = A.enemiso and A.cetipod = B.mctipod and B.mcnumdo = A.cenumdo where B.mcnumct = (select conumcta from GXBDBPS.CONCTATAR where conumcon = '" + tarjeta.getNroControl().trim() + "') ";
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(consPersona);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                nroDoc = rs.getString(1);
                nroRuc = rs.getString(2);
                nombre = rs.getString(3);
                estadoCta = rs.getString(4);
                nroCta = rs.getString(5);
            }
            stmt.close();
        } else {
            String consPersona = "select A.cenumdo, A.cenumru, A.ceapnom, B.mcstats, B.mcnumct from Gxbdbps.tmctaaf B INNER JOIN Gxbdbps.gclieaf A on B.mcemiso = A.enemiso and A.cetipod = B.mctipod and B.mcnumdo = A.cenumdo where B.mcnumct = (select mcnumct from Gxbdbps.tmtaraf where mtnumta = '" + tarjeta.getNroTarjeta().trim() + "') ";
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(consPersona);
            stmt.executeQuery();
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                nroDoc = rs.getString(1);
                nroRuc = rs.getString(2);
                nombre = rs.getString(3);
                estadoCta = rs.getString(4);
                nroCta = rs.getString(5);
            }
        }

        String insTrxCta = "insert into Gxbdbps.tdmota values(" + secTrx + ", '" + secLinea + "', 0, " + nroCta + ", '" + nombre + "', '" + nroDoc + "', '" + nroRuc + "', '" + estadoCta + "', " + estadoError + ", '" + errorTarj + "', '" + datosTarj + "')";
        //LOGGER.info(insTrxCta);
        boolean detalleTrx = DBUtils.ejecucionSQL(insTrxCta, conn);
        if (!errorTarj.trim().isEmpty()) {
            String consError = "select motacerr from Gxbdbps.tmmota where motasec = " + secTrx;
            long sumador = DBUtils.getSecuencia(consError, conn);
            String udpTrxCta = "update Gxbdbps.tmmota set motacerr = " + sumador + " where motasec = " + secTrx;
            //LOGGER.info(udpTrxCta);
            boolean trxCta = DBUtils.ejecucionSQL(udpTrxCta, conn);
        } else {
            String consError = "select motaserr from Gxbdbps.tmmota where motasec = " + secTrx;
            long sumador = DBUtils.getSecuencia(consError, conn);
            String udpTrxCta = "update Gxbdbps.tmmota set motaserr = " + sumador++ + " where motasec = " + secTrx;
            //LOGGER.info(udpTrxCta);
            boolean trxCta = DBUtils.ejecucionSQL(udpTrxCta, conn);
        }
    }

//    private void logEntradaCta(Cuenta cta) {
//        LOGGER.info("Datos enviados por Continental");
//        LOGGER.info("NroDoc " + cta.getNroDoc());
//        LOGGER.info("TipoPers " + cta.getTipoPers());
//        LOGGER.info("TipoDoc " + cta.getTipoDoc());
//        LOGGER.info("Nombre1 " + cta.getNombre1());
//        LOGGER.info("Nombre2 " + cta.getNombre2());
//        LOGGER.info("Apellido1 " + cta.getApellido1());
//        LOGGER.info("Apellido2 " + cta.getApellido2());
//        LOGGER.info("RazonSoc " + cta.getRazonSoc());
//        LOGGER.info("DenomComer " + cta.getDenomComer());
//        LOGGER.info("PaisDoc " + cta.getPaisDoc());
//        LOGGER.info("Sexo " + cta.getSexo());
//        LOGGER.info("FechaNac " + cta.getFechaNac());
//        LOGGER.info("LugarNac " + cta.getLugarNac());
//        LOGGER.info("RucExtr " + cta.getRucExtr());
//        LOGGER.info("EstadoCivil " + cta.getEstadoCivil());
//        LOGGER.info("Ocupacion " + cta.getOcupacion());
//        LOGGER.info("DirEmailPer " + cta.getDirEmailPer());
//        LOGGER.info("TipDocNew " + cta.getTipDocNew());
//        LOGGER.info("NroDocNew " + cta.getNroDocNew());
//        //Datosdelacuenta
//        LOGGER.info("NroControl " + cta.getNroControl());
//        LOGGER.info("NroCuenta " + cta.getNroCuenta());
//        LOGGER.info("CodAfin " + cta.getCodAfin());
//        LOGGER.info("TipCuenta " + cta.getTipCuenta());
//        LOGGER.info("DirRecibo " + cta.getDirRecibo());
//        LOGGER.info("DirExtr1 " + cta.getDirExtr1());
//        LOGGER.info("DirExtr2 " + cta.getDirExtr2());
//        LOGGER.info("DirExtr3 " + cta.getDirExtr3());
//        LOGGER.info("Depart " + cta.getDepart());
//        LOGGER.info("Ciudad " + cta.getCiudad());
//        LOGGER.info("Zona " + cta.getZona());
//        LOGGER.info("NroTel " + cta.getNroTel());
//        LOGGER.info("NroCel " + cta.getNroCel());
//        LOGGER.info("DirEmailCta " + cta.getDirEmailCta());
//        LOGGER.info("CodSuc " + cta.getCodSuc());
//        LOGGER.info("NroDocOfCta " + cta.getNroDocOfCta());
//        LOGGER.info("TipDocOfCta " + cta.getTipDocOfCta());
//        LOGGER.info("CtaVip " + cta.getCtaVip());
//        LOGGER.info("Franqueo " + cta.getFranqueo());
//        LOGGER.info("AplSegVida " + cta.getAplSegVida());
//        LOGGER.info("CobCosto " + cta.getCobCosto());
//        LOGGER.info("TipCosto " + cta.getTipCosto());
//        LOGGER.info("RetenExtr " + cta.getRetenExtr());
//        LOGGER.info("MotReten " + cta.getMotReten());
//        LOGGER.info("TipCierre " + cta.getTipCierre());
//        LOGGER.info("EmpAdher " + cta.getEmpAdher());
//        LOGGER.info("CalifBCP " + cta.getCalifBCP());
//        LOGGER.info("Situacion " + cta.getSituacion());
//        LOGGER.info("CodCliEnt " + cta.getCodCliEnt());
//        LOGGER.info("NroSocio " + cta.getNroSocio());
//        LOGGER.info("BonosCobrand " + cta.getBonosCobrand());
//        //Datosfinancierosdelacuenta
//        LOGGER.info("TipLin1Norm " + cta.getTipLin1Norm());
//        LOGGER.info("LinCredNorm " + cta.getLinCredNorm());
//        LOGGER.info("TipLin2Cuota " + cta.getTipLin2Cuota());
//        LOGGER.info("LinCredCuota " + cta.getLinCredCuota());
//        LOGGER.info("PersTTI " + cta.getPersTTI());
//        LOGGER.info("CodTTICorriente " + cta.getCodTTICorriente());
//        LOGGER.info("TICorriente " + cta.getTICorriente());
//        LOGGER.info("CodTTIMora " + cta.getCodTTIMora());
//        LOGGER.info("TIMora " + cta.getTIMora());
//        LOGGER.info("CodTTIComp " + cta.getCodTTIComp());
//        LOGGER.info("Icomp " + cta.gettIcomp());
//        LOGGER.info("PersPagMin " + cta.getPersPagMin());
//        LOGGER.info("PorcPagMin " + cta.getPorcPagMin());
//        LOGGER.info("ImpoMinPagMin " + cta.getImpoMinPagMin());
//        LOGGER.info("ImpoFijoPagMin " + cta.getImpoFijoPagMin());
//        LOGGER.info("ModPago " + cta.getModPago());
//        LOGGER.info("TipCtaBanc " + cta.getTipCtaBanc());
//        LOGGER.info("CtaBanc " + cta.getCtaBanc());
//        LOGGER.info("TipPago " + cta.getTipPago());
//        LOGGER.info("ValCGF " + cta.getValCGF());
//        LOGGER.info("TipTasaFin " + cta.getTipTasaFin());
//        LOGGER.info("FactMultVIP " + cta.getFactMultVIP());
//        LOGGER.info("CobrandID " + cta.getCobrandID());
//        //DatosdelCodeudordelaCuenta
//        LOGGER.info("NroDocCod " + cta.getNroDocCod());
//        LOGGER.info("TipPerCod " + cta.getTipPerCod());
//        LOGGER.info("TipDocCod " + cta.getTipDocCod());
//        LOGGER.info("Nombre1Cod " + cta.getNombre1Cod());
//        LOGGER.info("Nombre2Cod " + cta.getNombre2Cod());
//        LOGGER.info("Apellido1Cod " + cta.getApellido1Cod());
//        LOGGER.info("Apellido2Cod " + cta.getApellido2Cod());
//        LOGGER.info("SexoCod " + cta.getSexoCod());
//        LOGGER.info("FechNacCod " + cta.getFechNacCod());
//        LOGGER.info("LugNacCod " + cta.getLugNacCod());
//        LOGGER.info("EstCivCod " + cta.getEstCivCod());
//        LOGGER.info("OcupacionCod " + cta.getOcupacionCod());
//        LOGGER.info("DirEmailCod " + cta.getDirEmailCod());
//        //Datosdeloficialdelacuenta
//        LOGGER.info("NroDocOfi " + cta.getNroDocOfi());
//        LOGGER.info("TipPerOfi " + cta.getTipPerOfi());
//        LOGGER.info("TipDocOfi " + cta.getTipDocOfi());
//        LOGGER.info("Nombre1Ofi " + cta.getNombre1Ofi());
//        LOGGER.info("Nombre2Ofi " + cta.getNombre2Ofi());
//        LOGGER.info("Apellido1Ofi " + cta.getApellido1Ofi());
//        LOGGER.info("tApellido2Ofi " + cta.getApellido2Ofi());
//        LOGGER.info("SexoOfi " + cta.getSexoOfi());
//        LOGGER.info("FechNacOfi " + cta.getFechNacOfi());
//        LOGGER.info("LugNacOfi " + cta.getLugNacOfi());
//        LOGGER.info("EstCivOfi " + cta.getEstCivOfi());
//        LOGGER.info("OcupacionOfi " + cta.getOcupacionOfi());
//        LOGGER.info("DirEmailofi " + cta.getDirEmailofi());
//        //ExcepcionesCostosporCuenta
//        LOGGER.info("CostNoAplica " + cta.getCostNoAplica());
//        LOGGER.info("AplicaImp " + cta.getAplicaImp());
//        //Datosinternos
//        LOGGER.info("UserActualiza " + cta.getUserActualiza());
//        LOGGER.info("MotNovCta " + cta.getMotNovCta());
//        //Datospromocion
//        LOGGER.info("PartProCupAltera " + cta.getPartProCupAltera());
//        //ExcepcionesCargosdelCierreporCuenta
//        LOGGER.info("CargNoAplica" + cta.getCargNoAplica());
//    }
//    
//    private void logEntradaTarj(Tarjeta tarj) {
//        LOGGER.info("Datos enviados por Continental");
//        //Datos del Titular de la Tarjeta – Personas (Física) 
//        LOGGER.info("NroDoc " + tarj.getNroDoc());
//        LOGGER.info("TipoPers " + tarj.getTipoPers());
//        LOGGER.info("TipoDoc " + tarj.getTipoDoc());
//        LOGGER.info("Nombre1 " + tarj.getNombre1());
//        LOGGER.info("Nombre2 " + tarj.getNombre2());
//        LOGGER.info("Apellido1 " + tarj.getApellido1());
//        LOGGER.info("Apellido2 " + tarj.getApellido2());
//        LOGGER.info("PaisNac " + tarj.getPaisNac());
//        LOGGER.info("Sexo " + tarj.getSexo());
//        LOGGER.info("FechaNac " + tarj.getFechaNac());
//        LOGGER.info("LugarNac " + tarj.getLugarNac());
//        LOGGER.info("EstadoCivil " + tarj.getEstadoCivil());
//        LOGGER.info("Ocupacion " + tarj.getOcupacion());
//        LOGGER.info("DirEmailPer " + tarj.getDirEmailPer());
//        LOGGER.info("TipDocNew " + tarj.getTipDocNew());
//        LOGGER.info("NroDocNew " + tarj.getNroDocNew());
//        //Datos de la Tarjeta
//        LOGGER.info("NroControl " + tarj.getNroControl());
//        LOGGER.info("IdAfin " + tarj.getIdAfin());
//        LOGGER.info("IdCuenta " + tarj.getIdCuenta());
//        LOGGER.info("NroTarjeta " + tarj.getNroTarjeta());
//        LOGGER.info("TipTarjeta " + tarj.getTipTarjeta());
//        LOGGER.info("TipPlastico " + tarj.getTipPlastico());
//        LOGGER.info("Duracion " + tarj.getDuracion());
//        LOGGER.info("NombPlastico " + tarj.getNombPlastico());
//        LOGGER.info("AdiPersonali " + tarj.getAdiPersonali());
//        LOGGER.info("NroDocPromot " + tarj.getNroDocPromot());
//        LOGGER.info("TipDocPromot " + tarj.getTipDocPromot());
//        LOGGER.info("Situacion " + tarj.getSituacion());
//        LOGGER.info("RenovAuto " + tarj.getRenovAuto());
//        LOGGER.info("Ordena1 " + tarj.getOrdena1());
//        LOGGER.info("Ordena2 " + tarj.getOrdena2());
//        LOGGER.info("Emboza " + tarj.getEmboza());
//        //Datos del Promotor de la Tarjeta
//        LOGGER.info("NroDocProm " + tarj.getNroDocProm());
//        LOGGER.info("TipPersProm " + tarj.getTipPersProm());
//        LOGGER.info("TipDocProm " + tarj.getTipDocProm());
//        LOGGER.info("Nombre1Prom " + tarj.getNombre1Prom());
//        LOGGER.info("Nombre2Prom " + tarj.getNombre2Prom());
//        LOGGER.info("Apellido1Prom " + tarj.getApellido1Prom());
//        LOGGER.info("Apellido2Prom " + tarj.getApellido2Prom());
//        LOGGER.info("SexoProm " + tarj.getSexoProm());
//        LOGGER.info("FechNacProm " + tarj.getFechNacProm());
//        LOGGER.info("LugNacProm " + tarj.getLugNacProm());
//        LOGGER.info("EstadCivProm " + tarj.getEstadCivProm());
//        LOGGER.info("OcupaProm " + tarj.getOcupaProm());
//        LOGGER.info("DirEmailProm " + tarj.getDirEmailProm());
//        //Parámetros Tarjeta Adicional Personalizada 
//        LOGGER.info("PlanContad " + tarj.getPlanContad());
//        LOGGER.info("OperPlanCont " + tarj.getOperPlanCont());
//        LOGGER.info("ImporPersonal" + tarj.getImporPersonal());
//        LOGGER.info("PorcPersonal " + tarj.getPorcPersonal());
//        LOGGER.info("PlanAdeEfec" + tarj.getPlanAdeEfec());
//        LOGGER.info("OperPlanAdeEfec " + tarj.getOperPlanAdeEfec());
//        LOGGER.info("ImporPersonaliza " + tarj.getImporPersonaliza());
//        LOGGER.info("PorcPersonaliza " + tarj.getPorcPersonaliza());
//        LOGGER.info("PermFinanciar " + tarj.getPermFinanciar());
//        //Datos internos
//        LOGGER.info("UserActualiza " + tarj.getUserActualiza());
//        LOGGER.info("NroSocio " + tarj.getNroSocio());
//        LOGGER.info("MotBajaTarj " + tarj.getMotBajaTarj());
//    }
    public static String generarCtaTarj(String emisor, String bin, String afinidad, String ctaSN, String tarjSN, Connection conn) {
        //Se obtiene el ultimo valor de la secuencia
        String cuentaNew = "";
        String tarjetaNew = "";
        String respCtaTarj = "";

        LOGGER.info("----- INICIA GENERACION DE NRO DE CUENTA Y TARJETA -----");
        try {
            conn.setAutoCommit(false);
            //Se obtiene el ultimo valor de la secuencia cuenta
            if (ctaSN.equals("S")) {
                String ultSecSQL = "select max(biuncta) from Gxbdbps.tbineaf where enemiso = '" + emisor + "' and bibines = '" + bin + "'";
                PreparedStatement stmt = null;
                stmt = conn.prepareStatement(ultSecSQL);

                stmt.executeQuery();
                ResultSet rs = stmt.getResultSet();
                String secCta = "";
                while (rs.next()) {
                    secCta = rs.getString(1);
                }

                String ultSecSQL2 = "select clclase from Gxbdbps.tbineaf where enemiso = '" + emisor + "' and bibines = '" + bin + "'";
                PreparedStatement stmt2 = null;
                stmt2 = conn.prepareStatement(ultSecSQL2);
                stmt2.executeQuery();
                ResultSet rs2 = stmt2.getResultSet();
                String clase = "";
                while (rs2.next()) {
                    clase = rs2.getString(1);
                }
//                LOGGER.info("Se obtuvo el ultimo id de SEC Cta:" + secCta);
                long idnum = 0;
                if (secCta != null) {
                    if (!secCta.equals("")) {
                        idnum = Long.parseLong(secCta);
                        idnum += 1;
                    } else {
                        idnum = 1;
                    }
                } else {
                    idnum = 1;
                }
                //Se inserta el ultimo valor 
                String updateSQL = "update Gxbdbps.tbineaf set biuncta = " + idnum + " where enemiso = '" + emisor + "' and bibines = '" + bin + "'";
                Statement stmt3 = null;
                stmt3 = conn.createStatement();
                stmt3.executeUpdate(updateSQL);

                String idNroCta = "";
                idNroCta = String.format("%6s", idnum).replace(' ', '0');
                //Se crea un nuevo nro de cuenta
                String cta1 = emisor + clase + idNroCta;
                String cta2 = "21212121212";

                int dig1 = 0;
                int dig2 = 0;
                int[] valor = new int[12];
                for (int i = 0; i < 11; i++) {
                    dig1 = Integer.parseInt(cta1.substring(i, i + 1));
                    dig2 = Integer.parseInt(cta2.substring(i, i + 1));
                    valor[i] = dig1 * dig2;
                }

                int j = 0;
                int[] valor2 = new int[22];
                String cadena = "";
                String caracter = "";
                String caract = "";
                for (int i = 0; i < 11; i++) {
                    cadena = String.format("%2s", valor[i]);
                    caracter = cadena.substring(0, 1);
                    if (caracter.equals(" ")) {
                        caract = "0";
                    } else {
                        caract = caracter;
                    }
                    valor2[j] = Integer.parseInt(caract);
                    j += 1;
                    caracter = cadena.substring(1, 2);
                    if (caracter.equals(" ")) {
                        caract = "0";
                    } else {
                        caract = caracter;
                    }
                    valor2[j] = Integer.parseInt(caract);
                    j += 1;
                }

                int nroCta1 = 0;
                for (int i = 0; i < valor2.length; i++) {
                    nroCta1 += valor2[i];
                }

                String cadCta = String.format("%3s", nroCta1);
                String nroCta2 = cadCta.substring(2, 3);
                if (nroCta2.equals("0")) {
                    caract = "0";
                } else {
                    dig1 = 10 - Integer.parseInt(nroCta2);
                    caract = String.format("%1s", dig1);;
                }
                cuentaNew = cta1 + caract;
                stmt.close();
                stmt2.close();
                stmt3.close();

                LOGGER.info("NRO DE CUENTA GENERADO CORRECTAMENTE");
            }
            if (tarjSN.equals("S")) {
                //Se obtiene el ultimo valor de la secuencia tarjeta
                String ultSecSQL3 = "select max(afuntrj) from Gxbdbps.tafinaf where enemiso = '" + emisor + "' and bibines = '" + bin + "'"; //and afafini = '" + afinidad + "'";
                //LOGGER.info(ultSecSQL3);
                PreparedStatement stmt4 = null;
                stmt4 = conn.prepareStatement(ultSecSQL3);
                stmt4.executeQuery();
                ResultSet rs4 = stmt4.getResultSet();
                String secTarj = "";
                while (rs4.next()) {
                    secTarj = rs4.getString(1);
                }
//                LOGGER.info("Se obtuvo el ultimo id de SEC:" + secTarj);
                long idnum2 = 0;
                if (secTarj != null) {
                    if (!secTarj.equals("")) {
                        idnum2 = Long.parseLong(secTarj);
                        idnum2 += 1;
                    } else {
                        idnum2 = 1;
                    }
                } else {
                    idnum2 = 1;
                }
                //Se inserta el ultimo valor 
                String updateSQL2 = "update Gxbdbps.tafinaf set afuntrj = " + idnum2 + " where enemiso = '" + emisor + "' and bibines = '" + bin + "' and afafini = '" + afinidad + "'";
                //LOGGER.info(updateSQL2);
                Statement stmt5 = null;
                stmt5 = conn.createStatement();
                stmt5.executeUpdate(updateSQL2);
                String idNroTarj = "";
                idNroTarj = String.format("%7s", idnum2).replace(' ', '0');
                //Se crea un nuevo nro de tarjeta
                String tarj1 = "";
                if (bin.equals("627431")) {
                    tarj1 = bin + emisor + idNroTarj.substring(1, 7);
                } else {
                    tarj1 = bin + afinidad.substring(1, 3) + idNroTarj;
                }

                String tarj2 = "212121212121212";

                int dig1 = 0;
                int dig2 = 0;
                int[] valor = new int[15];
                for (int i = 0; i < 15; i++) {
                    dig1 = Integer.parseInt(tarj1.substring(i, i + 1));
                    dig2 = Integer.parseInt(tarj2.substring(i, i + 1));
                    valor[i] = dig1 * dig2;
                }
                int j = 0;
                int[] valor2 = new int[30];
                String cadena = "";
                String caracter = "";
                String caract = "";
                for (int i = 0; i < valor.length; i++) {
                    cadena = String.format("%2s", valor[i]);
                    caracter = cadena.substring(0, 1);
                    if (caracter.equals(" ")) {
                        caract = "0";
                    } else {
                        caract = caracter;
                    }

                    valor2[j] = Integer.parseInt(caract);
                    j += 1;
                    caracter = cadena.substring(1, 2);
                    if (caracter.equals(" ")) {
                        caract = "0";
                    } else {
                        caract = caracter;
                    }

                    valor2[j] = Integer.parseInt(caract);
                    j += 1;
                }

                int nroTarj1 = 0;
                for (int i = 0; i < valor2.length; i++) {
                    nroTarj1 += valor2[i];
                }

                String cadTarj = String.format("%3s", nroTarj1);
                String nroTarj2 = cadTarj.substring(2, 3);
                if (nroTarj2.equals("0")) {
                    caract = "0";
                } else {
                    dig1 = 10 - Integer.parseInt(nroTarj2);
                    caract = String.format("%1s", dig1);;
                }
                tarjetaNew = tarj1 + caract;
                stmt4.close();
                stmt5.close();

                LOGGER.info("NRO DE TARJETA GENERADO CORRECTAMENTE");
            }
        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
            respCtaTarj = "ERROR AL PROCESAR LA SOLICITUD";
            try {
                conn.rollback();
            } catch (Exception sqlex) {
                LOGGER.error("ERROR: " + sqlex);
            }
        }
        LOGGER.info("-----FINALIZA GENERACION DE NRO DE CUENTA Y TARJETA -----");
        respCtaTarj = cuentaNew + "," + tarjetaNew;
        return respCtaTarj;
    }

    @WebMethod(operationName = "bloqueoTC")
    public DatoBloqueo bloqueoTC(@WebParam(name = "usuario") @XmlElement(required = true) String usuario,
            @WebParam(name = "clave") @XmlElement(required = true) String clave,
            @WebParam(name = "accion") @XmlElement(required = true) String accion,
            @WebParam(name = "BloqueoWS") @XmlElement(required = true) BloqueoWS bloqueoWS) {

        String retorno = "";
        String codRetorno = "";
        String fecha = Utils.obtenerFechaHora("yyyyMMdd");
        String hora = Utils.obtenerFechaHora("HHmmss");
        DatoBloqueo resp = new DatoBloqueo();
        LOGGER.info("INICIA " + accion + " TC CONTINENTAL - bloqueoTC - " + new Date());
//        LOGGER.info("user" + usuario);
//        LOGGER.info("clave" + clave);
        LOGGER.info("-----------------");
        LOGGER.info("TARJETA:" + bloqueoWS.getTrjNro().substring(0, 6) + "XXXXXX" + bloqueoWS.getTrjNro().substring(12));
        LOGGER.info("ACCION:" + accion);
        LOGGER.info("CODIGO ACCION:" + bloqueoWS.getAccCod());
        LOGGER.info("CODIGO " + accion + " ENTRANTE:" + bloqueoWS.getTBId());
//        LOGGER.info("USUARIO SOLICITUD " + accion + ":" + bloqueoWS.getUsuUpd());
//        LOGGER.info("user props" + Utils.usrConti);
//        LOGGER.info("pass props" + Utils.passConti);
        if (usuario.equals(Utils.usrConti) && clave.equals(Utils.passConti)) {
            LOGGER.info("USUARIO VALIDADO");
            if (!bloqueoWS.getAmbBlo().equals("L")) {
                bloqueoWS.setResCod("06");
                bloqueoWS.setResExt("TARJETA SOLO ADMITE LOCAL");
                resp.setBloqueoWS(bloqueoWS);
                resp.setMensaje("TARJETA SOLO ADMITE LOCAL");
                LOGGER.info("06-TARJETA SOLO ADMITE LOCAL");
                LOGGER.info("---------FINALIZA " + accion + " TC CONTINENTAL--------------");
                return resp;
            }
            if (accion.equals("BLOQUEO")) {
                if (bloqueoWS.getUsuUpd().equals("") || bloqueoWS.getTrjNro().equals("") || bloqueoWS.getMBId().equals("") || bloqueoWS.getUsuUpd().length() > 10) { //Ricardo Arce 27032020
                    bloqueoWS.setResCod("05");
                    bloqueoWS.setResExt("ERROR EN PARAMETROS RECIBIDOS");
                    resp.setBloqueoWS(bloqueoWS);
                    resp.setMensaje("ERROR EN PARAMETROS RECIBIDOS");
                    LOGGER.info("05-ERROR EN PARAMETROS RECIBIDOS");
                    LOGGER.info("---------FINALIZA " + accion + " TC CONTINENTAL--------------");
                    return resp;
                }
                try {
                    Class.forName(Utils.driver);
                    Connection conn = null;

                    String str = "select mtnumta,blcodig,mtstats,bibines,mtfeven,afafini,enemiso, sucodig, MTESTBL "
                            + "from gxbdbps.tmtaraf where enemiso = '021' and mtnumta = '" + bloqueoWS.getTrjNro() + "'";
                    PreparedStatement stmt = null;
                    conn = DriverManager.getConnection(Utils.url, Utils.usrAS400, Utils.passAS400);
                    stmt = conn.prepareStatement(str);
                    stmt.executeQuery();
                    ResultSet rs = stmt.getResultSet();
                    String numtar = "";
                    String blocodig = "";
                    String estado = "";
                    String bines = "";
                    String vencimiento = "";
                    String afinidad = "";
                    String entidad = "";
                    String sucursal = "";
                    String tipBloqueo = ""; //Ricardo Arce 13032020
                    while (rs.next()) {
                        numtar = rs.getString(1);
                        blocodig = rs.getString(2);
                        estado = rs.getString(3);
                        bines = rs.getString(4);
                        vencimiento = rs.getString(5);
                        afinidad = rs.getString(6);
                        entidad = rs.getString(7);
                        sucursal = rs.getString(8);
                        tipBloqueo = rs.getString(9); //Ricardo Arce 13032020
                        LOGGER.info("ResultSet Obtenido");
                    }
                    if (blocodig.equals("")) {
                        retorno = "TARJETA NO ECONTRADA";
                        bloqueoWS.setResCod("01");
                        bloqueoWS.setResExt(retorno);
                        resp.setBloqueoWS(bloqueoWS);
                        resp.setMensaje(retorno);
                        LOGGER.info("RETORNO:01-" + retorno);
                    } else {
                        if (blocodig.equals("00")) {
                            String bloqueo = obtenerTipoBloqueo(bloqueoWS.getMBId());
                            if (bloqueo.equals("NO")) { //Ricardo Arce 26032020
                                bloqueoWS.setResCod("05");
                                bloqueoWS.setResExt("ERROR EN PARAMETROS RECIBIDOS");
                                resp.setBloqueoWS(bloqueoWS);
                                resp.setMensaje("ERROR EN PARAMETROS RECIBIDOS");
                                LOGGER.info("05-ERROR EN PARAMETROS RECIBIDOS");
                                LOGGER.info("---------FINALIZA " + accion + " TC CONTINENTAL--------------");
                                return resp;
                            }
                            //Actualizacion estado tarjeta
                            String updateSQL = "update gxbdbps.tmtaraf set blcodig = '" + bloqueo + "', MTESTBL = 'B' where enemiso = '021' and mtnumta = '" + bloqueoWS.getTrjNro() + "'";  //Ricardo Arce 13032020
                            Statement stmt2 = null;
                            Class.forName(Utils.driver);
                            stmt2 = conn.createStatement();
                            stmt2.executeUpdate(updateSQL);
                            LOGGER.info("TARJETA BLOQUEADA");
                            //Se obtiene utimo numero de la auditoria
                            String obtenerID = "select max(atnumse) from gxbdbps.autaraf where atemiso = '" + entidad + "' "
                                    + " and atafini = '" + afinidad + "' and atnumta = '" + bloqueoWS.getTrjNro() + "'";
                            PreparedStatement stmt3 = null;
                            stmt3 = conn.prepareStatement(obtenerID);
                            stmt3.executeQuery();
                            ResultSet rs3 = stmt3.getResultSet();
                            String idLog = "";
                            while (rs3.next()) {
                                idLog = rs3.getString(1);
                            }
                            LOGGER.info("Se obtubo el ultimo id de LOG:" + idLog);
                            long idnum;
                            if (idLog != null) { //Ricardo Arce 13032020
                                idnum = Long.parseLong(idLog);
                                idnum += 1;
                            } else {
                                idnum = 1;
                            }
                            //Se obtiene FECHA COMERCIAL
                            String fechaCom = getFechaComercial();

                            //Se inserta auditoria
                            String insertSQL = "INSERT INTO GXBDBPS.autaraf VALUES('" + fechaCom + "','" + entidad + "', '" + afinidad + "' , '" + numtar + "' , "
                                    + idnum + " , 'M', 1 , '" + bloqueoWS.getUsuUpd() + "', '" + fecha + "', " + hora + ", 0 , 'Codigo de Bloqueo', '" + blocodig + "' , '" + bloqueo + "' , 0 , '" + sucursal + "'),"
                                    + "('" + fechaCom + "','" + entidad + "', '" + afinidad + "' , '" + numtar + "' , " //Ricardo Arce 13032020
                                    + ++idnum + " , 'M', 1 , '" + bloqueoWS.getUsuUpd() + "', '" + fecha + "', " + hora + ", 0 , 'Tipo de Bloqueo', '" + tipBloqueo + "' , 'B' , 0 , '" + sucursal + "')"; //Ricardo Arce 13032020
                            Statement stmt5 = null;
                            stmt5 = conn.createStatement();
                            stmt5.executeUpdate(insertSQL);

                            retorno = "TARJETA BLOQUEADA";
                            bloqueoWS.setResCod("00");
                            bloqueoWS.setResExt(retorno);
                            resp.setBloqueoWS(bloqueoWS);
                            resp.setMensaje(retorno);

                            LOGGER.info("RETORNO:00-" + retorno);

                            stmt2.close();
                            stmt3.close();
                            stmt5.close();

                        } else {
                            bloqueoWS.setResCod("02");
                            retorno = "LA TARJETA YA POSEE UN BLOQUEO";
                            bloqueoWS.setResExt(retorno);
                            resp.setBloqueoWS(bloqueoWS);
                            resp.setMensaje(retorno);
                            LOGGER.info("RETORNO:02-" + retorno);
                        }
                    }
                    stmt.close();
                    conn.close();

                } catch (Exception ex) {
                    retorno = "Error al procesar la solicitud";
                    bloqueoWS.setResCod("96");
                    bloqueoWS.setResExt(retorno);
                    resp.setBloqueoWS(bloqueoWS);
                    resp.setMensaje(retorno);
                    LOGGER.info("Error:" + ex);
                    LOGGER.info("RETORNO:96-" + retorno);
                    LOGGER.info("---------FINALIZA " + accion + " TC CONTINENTAL--------------");
                    return resp;
                }
                return resp;
            } else if (accion.equals("DESBLOQUEO")) { //Ricardo Arce 26032020
                if (bloqueoWS.getUsuUpd().equals("") || bloqueoWS.getTrjNro().equals("") || bloqueoWS.getUsuUpd().length() > 10) { //Ricardo Arce 27032020
                    bloqueoWS.setResCod("05");
                    bloqueoWS.setResExt("ERROR EN PARAMETROS RECIBIDOS");
                    resp.setBloqueoWS(bloqueoWS);
                    resp.setMensaje("ERROR EN PARAMETROS RECIBIDOS");
                    LOGGER.info("05-ERROR EN PARAMETROS RECIBIDOS");
                    LOGGER.info("---------FINALIZA " + accion + " TC CONTINENTAL--------------");
                    return resp;
                }

                try {
                    Class.forName(Utils.driver);
                    Connection conn = null;
                    String str = "select mtnumta,blcodig,mtstats,bibines,mtfeven,afafini,enemiso, sucodig, MTESTBL "
                            + "from gxbdbps.tmtaraf where enemiso = '021' and mtnumta = '" + bloqueoWS.getTrjNro() + "'";
                    PreparedStatement stmt = null;
                    conn = DriverManager.getConnection(Utils.url, Utils.usrAS400, Utils.passAS400);
                    stmt = conn.prepareStatement(str);
                    stmt.executeQuery();
                    ResultSet rs = stmt.getResultSet();
                    String numtar = "";
                    String blocodig = "";
                    String estado = "";
                    String bines = "";
                    String vencimiento = "";
                    String afinidad = "";
                    String entidad = "";
                    String sucursal = "";
                    String tipoBloqueo = ""; //Ricardo Arce 13032020
                    while (rs.next()) {
                        numtar = rs.getString(1);
                        blocodig = rs.getString(2);
                        estado = rs.getString(3);
                        bines = rs.getString(4);
                        vencimiento = rs.getString(5);
                        afinidad = rs.getString(6);
                        entidad = rs.getString(7);
                        sucursal = rs.getString(8);
                        tipoBloqueo = rs.getString(9); //Ricardo Arce 13032020
                        LOGGER.info("ResultSet Obtenido");
                    }
                    if (blocodig.equals("")) {
                        retorno = "TARJETA NO ECONTRADA";
                        bloqueoWS.setResCod("01");
                        bloqueoWS.setResExt(retorno);
                        resp.setBloqueoWS(bloqueoWS);
                        resp.setMensaje(retorno);
                        LOGGER.info("RETORNO:01-" + retorno);
                    } else {
                        if (!blocodig.equals("00")) {
                            //Actualizacion estado tarjeta
                            String updateSQL = "update gxbdbps.tmtaraf set blcodig = '00', MTESTBL = '' where enemiso = '021' and mtnumta = '" + bloqueoWS.getTrjNro() + "'"; //Ricardo Arce 13032020
                            Statement stmt2 = null;
                            Class.forName(Utils.driver);
                            stmt2 = conn.createStatement();
                            stmt2.executeUpdate(updateSQL);
                            LOGGER.info("TARJETA DESBLOQUEADA");
                            //Se obtiene utimo numero de la auditoria
                            String obtenerID = "select max(atnumse) from gxbdbps.autaraf where atemiso = '" + entidad + "' "
                                    + " and atafini = '" + afinidad + "' and atnumta = '" + bloqueoWS.getTrjNro() + "'";
                            PreparedStatement stmt3 = null;
                            stmt3 = conn.prepareStatement(obtenerID);
                            stmt3.executeQuery();
                            ResultSet rs3 = stmt3.getResultSet();
                            String idLog = "";
                            while (rs3.next()) {
                                idLog = rs3.getString(1);
                            }
                            LOGGER.info("Se obtubo el ultimo id de LOG:" + idLog);
                            long idnum;
                            if (idLog != null) { //Ricardo Arce 13032020
                                idnum = Long.parseLong(idLog);
                                idnum += 1;
                            } else {
                                idnum = 1;
                            }
                            //Se obtiene FECHA COMERCIAL
                            String fechaCom = getFechaComercial();

                            //Se inserta auditoria
                            String insertSQL = "INSERT INTO GXBDBPS.autaraf VALUES('" + fechaCom + "','" + entidad + "', '" + afinidad + "' , '" + bloqueoWS.getTrjNro() + "' , "
                                    + idnum + " , 'M', 1 , '" + bloqueoWS.getUsuUpd() + "', '" + fecha + "', " + hora + ", 0 , 'Codigo de Bloqueo', '" + blocodig + "' , '00' , 0 , '" + sucursal + "'),"
                                    + "('" + fechaCom + "','" + entidad + "', '" + afinidad + "' , '" + bloqueoWS.getTrjNro() + "' , " //Ricardo Arce 13032020
                                    + ++idnum + " , 'M', 1 , '" + bloqueoWS.getUsuUpd() + "', '" + fecha + "', " + hora + ", 0 , 'Tipo de Bloqueo', '" + tipoBloqueo + "' , '' , 0 , '" + sucursal + "')";//Ricardo Arce 13032020
                            Statement stmt5 = null;
                            stmt5 = conn.createStatement();
                            stmt5.executeUpdate(insertSQL);
                            retorno = "TARJETA DESBLOQUEADA";
                            bloqueoWS.setResCod("00");
                            bloqueoWS.setResExt(retorno);
                            resp.setBloqueoWS(bloqueoWS);
                            resp.setMensaje(retorno);
                            LOGGER.info("RETORNO:00-" + retorno);
                            stmt2.close();
                            stmt3.close();
                            stmt5.close();
                        } else {
                            retorno = "LA TARJETA NO POSEE BLOQUEOS";
                            bloqueoWS.setResCod("02");
                            bloqueoWS.setResExt(retorno);
                            resp.setBloqueoWS(bloqueoWS);
                            resp.setMensaje(retorno);
                            LOGGER.info("RETORNO:02-" + retorno);
                        }
                    }
                    stmt.close();
                    conn.close();

                } catch (Exception ex) {
                    retorno = "Error al procesar la solicitud";
                    LOGGER.info("Error:" + ex);
                    bloqueoWS.setResCod("96");
                    bloqueoWS.setResExt(retorno);
                    resp.setBloqueoWS(bloqueoWS);
                    resp.setMensaje(retorno);
                    LOGGER.info("RETORNO:96-" + retorno);
                    LOGGER.info("---------FINALIZA " + accion + " TC CONTINENTAL--------------");
                    return resp;
                }
                LOGGER.info("---------FINALIZA " + accion + " TC CONTINENTAL--------------");
                return resp;
            } else { //Ricardo Arce 26032020
                bloqueoWS.setResCod("05");
                bloqueoWS.setResExt("ERROR EN PARAMETROS RECIBIDOS");
                resp.setBloqueoWS(bloqueoWS);
                resp.setMensaje("ERROR EN PARAMETROS RECIBIDOS");
                LOGGER.info("05-ERROR EN PARAMETROS RECIBIDOS");
                LOGGER.info("---------FINALIZA " + accion + " TC CONTINENTAL--------------");
                return resp;
            }

        } else {
            retorno = "ERROR DE VALIDACION DE USUARIO";
            bloqueoWS.setResCod("05");
            bloqueoWS.setResExt(retorno);
            resp.setBloqueoWS(bloqueoWS);
            resp.setMensaje(retorno);
            LOGGER.info("RETORNO:05-" + retorno);
        }
        LOGGER.info("---------FINALIZA " + accion + " TC CONTINENTAL--------------");
        return resp;
    }

    private String obtenerTipoBloqueo(String tipo) {
        String bloqueo;
        int codBloq;
        codBloq = Integer.parseInt(tipo);
        switch (codBloq) {
            case 5:
                bloqueo = "04";
                break;
            case 3:
                bloqueo = "07";
                break;
            case 4:
                bloqueo = "06";
                break;
            case 9:
                bloqueo = "01";
                break;
            case 8:
                bloqueo = "09";
                break;
            case 11: //Ricardo Arce 26032020
                bloqueo = "04";//consultar
                break;
            default:
                bloqueo = "NO"; //Ricardo Arce 26032020
                break;
        }
        return bloqueo;
    }

}
