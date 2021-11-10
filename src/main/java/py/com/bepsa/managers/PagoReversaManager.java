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
import org.apache.log4j.Logger;
import py.com.bepsa.clases.DatoTmoviaf;
import py.com.bepsa.clases.RespPagoTC;
import py.com.bepsa.utils.DBUtils;
import py.com.bepsa.utils.Utils;

/**
 *
 * @author rarce
 */
public class PagoReversaManager {

    private static final Logger LOGGER = Logger.getLogger(PagoReversaManager.class.getName());

    public static RespPagoTC procesar(String tarjeta, String operacion, String importe, String pedido, String user) {
        RespPagoTC retorno = new RespPagoTC();
        String nroTransaccion = "";
        String fechaAfecDisp = "00000000";
        String fechaExtracto = "00000000";
        String codigo = "";
        String mensaje = "";
        String respuestaAS = "";
        String movimiento = "";
        DatoTmoviaf datosTmoviaf = new DatoTmoviaf();
        DatoTmoviaf datosTmoviaf2 = new DatoTmoviaf();
        String tarjetaIn = tarjeta;
        String importeIn = importe;
        String userIn = user;
        //String nroTransaccion = "";
        String novedad = "*PAGOTC";
        if (operacion.equalsIgnoreCase("P")) {
            movimiento = "510"; //Codigo de pago 
            datosTmoviaf = DBUtils.getDatoTmoviaf("select * from gxbdbps.tmoviaf where mtnumta = '" + tarjeta + "' and mvcodau = '" + pedido + "' and cmcodig = '510'");
            if (datosTmoviaf.getMVCODAU().equals(pedido)) {
                retorno.setCodResp("03");
                retorno.setMsgResp("Pago ya realizado");
                LOGGER.info("PAGO YA REALIZADO");
                return retorno;
            }
        } else if (operacion.equalsIgnoreCase("R")) {
            movimiento = "210"; //Codigo de reversa
//            datosTmoviaf = DBUtils.getDatoTmoviaf("select mvferea, mvnumct, mvemiso, mvcodsc, mvafini, mvfeval, mvcodmn from gxbdbps.tmoviaf where mtnumta = '" + tarjeta + "' and MVCODAU = '" + pedido + "'");
            datosTmoviaf = DBUtils.getDatoTmoviaf("select * from gxbdbps.tmoviaf where mtnumta = '" + tarjeta + "' and mvcodau = '" + pedido + "' and cmcodig = '510'");
            if (!datosTmoviaf.getMVCODAU().equals(pedido)) {
                retorno.setCodResp("05");
                retorno.setMsgResp("No existe transaccion a reversar");
                LOGGER.info("NO EXISTE TRANSACCION A REVERSAR");
                return retorno;
            }
            datosTmoviaf2 = DBUtils.getDatoTmoviaf("select * from gxbdbps.tmoviaf where mtnumta = '" + tarjeta + "' and mvcodau = '" + pedido + "' and cmcodig = '210'");
            if (datosTmoviaf2.getCMCODIG().trim().equals("210")) {
                retorno.setCodResp("04");
                retorno.setMsgResp("Transaccion ya reversada");
                LOGGER.info("TRANSACCION YA REVERSADA");
                return retorno;
            }
        }
        tarjeta = String.format("%1$-16s", tarjeta);
        importe = importe + "00";
        importe = String.format("%1$-15s", importe);
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
            textEnvioByte6 = textEnvio6.toBytes(pedido);
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

                        codigo = "96";
                        mensaje = "ERROR AL PROCESAR LA SOLICITUD";
                    } else {
                        byte[] arrayOfByte11 = arrayOfProgramParameter[9].getOutputData();
                        AS400Text localAS400Text11 = new AS400Text(arrayOfByte11.length, localAS400);
                        respuestaAS = (String) localAS400Text11.toObject(arrayOfByte11);

//                            byte[] arrayOfByte5 = arrayOfProgramParameter[3].getOutputData();
//                            AS400Text localAS400Text5 = new AS400Text(arrayOfByte5.length, localAS400);
//                            codigo = (String) localAS400Text5.toObject(arrayOfByte5);
                        if (respuestaAS.trim().isEmpty()) {
                            codigo = "00";
                            mensaje = ((operacion.equalsIgnoreCase("P")) ? "PAGO" : "REVERSA") + " PROCESADO CORRECTAMENTE";
                        } else {
                            codigo = "96";
                            mensaje = "ERROR AL PROCESAR LA SOLICITUD";
                        }
                    }
                    localAS400.disconnectService(2);
                } catch (ErrorCompletingRequestException | InterruptedException | AS400SecurityException | ObjectDoesNotExistException | NumberFormatException e) {
                    LOGGER.error("Error en procesar(): " + e.getMessage());
//                    e.printStackTrace(); //06/02/2020 Ricardo Arce 
                    localAS400.disconnectService(2);
                    codigo = "96";
                    mensaje = "ERROR AL PROCESAR LA SOLICITUD";
                }
            } else {
                LOGGER.info("Timeout de 20 segundos al intentar conectarse a la AS");
                codigo = "96";
                mensaje = "ERROR AL PROCESAR LA SOLICITUD";
            }
            if (!codigo.equals("96")) {
                //Obtenemos los datos de la tabla Tmoviaf
//                if (datosTmoviaf.getMVSECUE().trim().equalsIgnoreCase("")) { //07022020 Ricardo Arce
                datosTmoviaf = DBUtils.getDatoTmoviaf("select * from gxbdbps.tmoviaf where mtnumta = '" + tarjetaIn + "' and MVCODAU = '" + pedido + "' and CMCODIG = '" + movimiento + "'");
//                }
                //Se obtiene ultima seuencia de la tabla TMOVPAG
                Connection conn = DBUtils.connect();
                String sec = "select max(mprgid) from gxbdbps.tmovpag";
                long secuencia = DBUtils.getSecuencia(sec, conn);
//            conn.close();

                //Insertamos los datos en la tabla TMOVPAG
                String insertPago = "insert into Gxbdbps.tmovpag values(" + secuencia + ", '" + datosTmoviaf.getMVFEPRO() + "','" + datosTmoviaf.getMTNUMTA() + "'," + datosTmoviaf.getMVNUMCT()
                        + "," + datosTmoviaf.getMVEMISO() + "," + datosTmoviaf.getMVCODSC() + ",'',''," + datosTmoviaf.getMVAFINI() + ",'" + datosTmoviaf.getMVFEVAL()
                        + "',''," + (!(datosTmoviaf.getMVCODMN().trim().isEmpty()) ? datosTmoviaf.getMVCODMN() : 0) + "," + datosTmoviaf.getMVIMPO2() + ", 0," + datosTmoviaf.getMVIMPO2()
                        + ", '', 0, '','','',0 ,0 ,0 ,0 ,0 ,0 ,0 ,'' ,0 ,0 ,'' ,'')";
//                LOGGER.info(insertPago); //07022020 Ricardo Arce
                if (DBUtils.ejecucionSQL(insertPago)) {
                    LOGGER.info("TABLA TMOVPAG INSERTADA CORRECTAMENTE");
                } else {
                    LOGGER.info("ERROR AL INSERTAR EN LA TABLA TMOVPAG");
                }
                LOGGER.info(((operacion.equalsIgnoreCase("P")) ? "PAGO" : "REVERSA") + " INSERTADO CORRECTAMENTE");
                nroTransaccion = datosTmoviaf.getMVSECUE();
                LOGGER.info("SE OBTUVO CODIGO DE TRANSACCION"); //07022020 Ricardo Arce
                fechaAfecDisp = datosTmoviaf.getMVFEPRO();
                LOGGER.info("SE OBTUVO FECHA AFECTACION"); //07022020 Ricardo Arce
                fechaExtracto = DBUtils.getFechaExtracto(datosTmoviaf.getMVEMISO(), datosTmoviaf.getMVAFINI(), datosTmoviaf.getMVBINES(), conn);
                LOGGER.info("SE OBTUVO FECHA EXTRACTO"); //07022020 Ricardo Arce
                conn.close();
            }

        } catch (Exception localException) {
            LOGGER.error("Error en procesar(): " + localException.getMessage());
//            localException.printStackTrace(); //06/02/2020 Ricardo Arce 
            codigo = "96";
            mensaje = "ERROR AL PROCESAR LA SOLICITUD";
        }
        retorno.setNroTransaccion(nroTransaccion);
        retorno.setFechaAfecDisp(fechaAfecDisp);
        retorno.setFechaExtracto(fechaExtracto);
        retorno.setCodResp(codigo);
        retorno.setMsgResp(mensaje);
        return retorno;
    }
}
