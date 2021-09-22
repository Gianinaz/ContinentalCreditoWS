/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package py.com.bepsa.utils;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import py.com.bepsa.BepCypher;

/**
 *
 * @author rarce
 */
public class Utils {

    public static String formatearFecha(Date fecha, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(fecha);
    }

    public static String formatearMonto(Double monto) {
        DecimalFormat df = new DecimalFormat("###");
        return df.format(monto);
    }

    public static String usrConti;
    public static String passConti;
    public static String usrAs;
    public static String passAs;
    public static String driver;
    public static String url;
    public static String defaultSchema;
    public static String server;
    public static String libAS400;
    public static String progAS400;
    public static long timeOutAS;

    public static int periodo1HoraInicial;
    public static int periodo1HoraFinal;
    public static int periodo1MinutoInicial;
    public static int periodo1MinutoFinal;
    public static int periodo2HoraInicial;
    public static int periodo2HoraFinal;
    public static int periodo2MinutoInicial;
    public static int periodo2MinutoFinal;
    public static int horasCorte;
    public static int minutosCorte;
    //public static int corteHoraFinal;
//    public static int corteMinutoFinal;

    public static String default_schemma;
    public static String usrAS400;
    public static String passAS400;
    private static final Logger LOGGER = LogManager.getLogger(Utils.class);

    public static void obtenerPropiedades() {
        try {
            Properties props = new Properties();
            InputStream inputStream = Utils.class.getResourceAsStream("/generic.properties");
            props.load(inputStream);
            LOGGER.info("OBTENIENDO PARAMETROS");
            url = props.getProperty("url").trim();
            usrConti = BepCypher.decrypt(props.getProperty("usrConti"));
            passConti = BepCypher.decrypt(props.getProperty("passConti"));
            usrAS400 = BepCypher.decrypt(props.getProperty("usrAS400"));
            passAS400 = BepCypher.decrypt(props.getProperty("passAS400"));
            default_schemma = props.getProperty("default_schemma");
            driver = props.getProperty("driver");
            server = props.getProperty("server");
            libAS400 = props.getProperty("libAS400");
            progAS400 = props.getProperty("progAS400");
            timeOutAS = Long.parseLong(props.getProperty("timeOutAS"));

            defaultSchema = props.getProperty("default_schema");
            periodo1HoraInicial = Integer.parseInt(props.getProperty("periodo1HoraInicial"));
            periodo1HoraFinal = Integer.parseInt(props.getProperty("periodo1HoraFinal"));
            periodo1MinutoInicial = Integer.parseInt(props.getProperty("periodo1MinutoInicial"));
            periodo1MinutoFinal = Integer.parseInt(props.getProperty("periodo1MinutoFinal"));
            periodo2HoraInicial = Integer.parseInt(props.getProperty("periodo2HoraInicial"));
            periodo2HoraFinal = Integer.parseInt(props.getProperty("periodo2HoraFinal"));
            periodo2MinutoInicial = Integer.parseInt(props.getProperty("periodo2MinutoInicial"));
            periodo2MinutoFinal = Integer.parseInt(props.getProperty("periodo2MinutoFinal"));
            horasCorte = Integer.parseInt(props.getProperty("horasCorte"));
            minutosCorte = Integer.parseInt(props.getProperty("minutosCorte"));
            //corteHoraFinal = Integer.parseInt(props.getProperty("corteHoraFinal"));
//            corteMinutoFinal = Integer.parseInt(props.getProperty("corteMinutoFinal"));
            usrAs = BepCypher.decrypt(props.getProperty("usrAS400").trim());
            passAs = BepCypher.decrypt(props.getProperty("passAS400").trim());

        } catch (Exception ex) {
            LOGGER.error("ERROR: " + ex);
        }
    }

    public static Boolean verificarDia() {
//        Calendar c = Calendar.getInstance();
//        int dia = c.get(Calendar.DAY_OF_WEEK);
        boolean respuesta = true;
//        if (dia == 2 || dia == 3 || dia == 4 || dia == 5 || dia == 6) {
//            respuesta = true;
//        }
        Date fechaProceso = new Date();
        if (fechaProceso.getDay() == 0 || fechaProceso.getDay() == 6) {
            respuesta = false;
        } else {
            if (DBUtils.valiDate(Utils.formatearFecha(fechaProceso, "yyyyMMdd"))) {
                respuesta = false;
            }
        }
        return respuesta;
    }

    public static Boolean verificarHorario() {
        obtenerPropiedades();
        Calendar c = Calendar.getInstance();
        int hora = c.get(Calendar.HOUR_OF_DAY);
        int minuto = c.get(Calendar.MINUTE);
        //int minutos = c.get(Calendar.MINUTE);
        boolean respuesta = true;
        if (!(((hora >= periodo1HoraInicial && minuto >= periodo1MinutoInicial) && (hora <= periodo1HoraFinal && minuto <= periodo1MinutoFinal)) || ((hora >= periodo2HoraInicial && minuto >= periodo2MinutoInicial) && (hora <= periodo2HoraFinal && minuto <= periodo2MinutoFinal)))) {
            respuesta = false;
        }
        return respuesta;
    }
//    public static Boolean validarCorte() {
//        obtenerPropiedades();
//        Calendar c = Calendar.getInstance();
//        int hora = c.get(Calendar.HOUR_OF_DAY);
//        LOGGER.info(hora);
//        int minuto = c.get(Calendar.MINUTE);
//        //int minutos = c.get(Calendar.MINUTE);
//        boolean respuesta = true;
//        if (hora == corteHoraInicial && (minuto >= corteMinutoInicial && minuto <= corteMinutoFinal)) {
//            respuesta = false;
//        }
//        return respuesta;
//    }

    public static String aumentarHora(String hora) {
        obtenerPropiedades();
        String horaSalida = "";
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HHmm");
            Date time = dateFormat.parse(hora);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(time); //tuFechaBase es un Date;
            //lo que mas quieras sumar
            calendar.add(Calendar.HOUR, horasCorte); //horasASumar es int.
            calendar.add(Calendar.MINUTE, minutosCorte); //minutosASumar es int.

            Date fechaSalida = calendar.getTime(); //Y ya tienes la fecha sumada.
            SimpleDateFormat dateFormat2 = new SimpleDateFormat("HHmm");
            horaSalida = dateFormat2.format(fechaSalida);

        } catch (ParseException ex) {
            LOGGER.info("Error en " + Utils.class + ".aumentarHora: " + ex);
        }
        return horaSalida;
    }

    public static String getFechaAntesDespues(int cantDias, int cantMeses, int cantAños, String formatoFecha) {
        String mesesAtras = "";
        Date time = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        //los meses que quieras restar
        calendar.add(Calendar.DAY_OF_YEAR, cantDias);
        calendar.add(Calendar.MONTH, cantMeses); //meses a restar en int.
        calendar.add(Calendar.YEAR, cantAños);
        Date fechaSalida = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat(formatoFecha);
        mesesAtras = dateFormat.format(fechaSalida);
        return mesesAtras;
    }

    public static String obtenerFechaHora(String formato) {
        Date fech = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(formato);
        String fecha = sdf.format(fech);
//        fecha = fecha.replace(":", "");
        return fecha;
    }

    public static String modificarFormatoFecha(String fech) throws ParseException {
        String fecha = "";
        if (!fech.equals("") && fech != null) {
            Date date;
            SimpleDateFormat formateador = new SimpleDateFormat("dd/MM/yy");
            date = formateador.parse(fech);
            SimpleDateFormat formateador2 = new SimpleDateFormat("yyyyMMdd");
            fecha = formateador2.format(date);
        }
        return fecha;
    }

    public static String obtenerUltimoDiaMes() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date date = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fechaCalif = sdf.format(date);
        return fechaCalif;

    }

    public static Boolean validateLogin(String user, String password) {
        Utils.obtenerPropiedades();
        Boolean retorno = true;
//        LOGGER.info("User igual a userConti" + user + "=" + Utils.usrConti);
//        LOGGER.info("Pass igual a passConti" + password + "=" + Utils.passConti);
        if (user.equalsIgnoreCase(Utils.usrConti)) {
            if (!password.equals(Utils.passConti)) {
                retorno = false;
            }
        } else {
            retorno = false;
        }

        return retorno;
    }

    public static int getMonthDiference(String fecha1, String fecha2) {
        int difA = 0;
        int difM = 0;
        try {
            LOGGER.info("Fecha1:" + fecha1);
            LOGGER.info("Fecha2:" + fecha2);
            Calendar inicio = new GregorianCalendar();
            Calendar fin = new GregorianCalendar();
            inicio.setTime(new SimpleDateFormat("dd/MM/yy").parse(fecha2));
            fin.setTime(new SimpleDateFormat("dd/MM/yy").parse(fecha1));

            difA = fin.get(Calendar.YEAR) - inicio.get(Calendar.YEAR);
            difM = difA * 12 + fin.get(Calendar.MONTH) - inicio.get(Calendar.MONTH);
            difM = difM - 1;
            System.out.println(difM);
        } catch (ParseException ex) {
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return difM;
    }

    public static String mapearOcupacion(String codigo) {
        String ocupacion;
        int codOcupacion;
        codOcupacion = (!codigo.trim().isEmpty()) ? Integer.parseInt(codigo) : 0;
        switch (codOcupacion) {
            case 2001:
                ocupacion = "009";
                break;
            case 5001:
                ocupacion = "029";
                break;
            case 2002:
                ocupacion = "030";
                break;
            case 3001:
                ocupacion = "031";
                break;
            case 5002:
                ocupacion = "032";
                break;
            case 3002:
                ocupacion = "033";
                break;
            case 1701:
                ocupacion = "034";
                break;
            case 2003:
                ocupacion = "035";
                break;
            case 2004:
                ocupacion = "036";
                break;
            case 3003:
                ocupacion = "037";
                break;
            case 1601:
                ocupacion = "038";
                break;
            case 1702:
                ocupacion = "039";
                break;
            case 3004:
                ocupacion = "040";
                break;
            case 6001:
                ocupacion = "041";
                break;
            case 3005:
                ocupacion = "015";
                break;
            case 2005:
                ocupacion = "042";
                break;
            case 1703:
                ocupacion = "043";
                break;
            case 3006:
                ocupacion = "044";
                break;
            case 2006:
                ocupacion = "010";
                break;
            case 1704:
                ocupacion = "045";
                break;
            case 1000:
                ocupacion = "046";
                break;
            case 9000:
                ocupacion = "047";
                break;
            case 9001:
                ocupacion = "048";
                break;
            case 9002:
                ocupacion = "049";
                break;
            case 9003:
                ocupacion = "050";
                break;
            case 9004:
                ocupacion = "051";
                break;
            case 9005:
                ocupacion = "052";
                break;
            case 9006:
                ocupacion = "053";
                break;
            case 9009:
                ocupacion = "054";
                break;
            case 9007:
                ocupacion = "055";
                break;
            case 9008:
                ocupacion = "056";
                break;
            case 6002:
                ocupacion = "057";
                break;
            case 2007:
                ocupacion = "058";
                break;
            case 5003:
                ocupacion = "059";
                break;
            case 1602:
                ocupacion = "060";
                break;
            case 5004:
                ocupacion = "061";
                break;
            case 1705:
                ocupacion = "062";
                break;
            case 5005:
                ocupacion = "063";
                break;
            case 1603:
                ocupacion = "064";
                break;
            case 5006:
                ocupacion = "065";
                break;
            case 2008:
                ocupacion = "066";
                break;
            case 2009:
                ocupacion = "067";
                break;
            case 2010:
                ocupacion = "016";
                break;
            case 1604:
                ocupacion = "068";
                break;
            case 1706:
                ocupacion = "069";
                break;
            case 3007:
                ocupacion = "070";
                break;
            case 5007:
                ocupacion = "071";
                break;
            case 1707:
                ocupacion = "072";
                break;
            case 5008:
                ocupacion = "073";
                break;
            case 5009:
                ocupacion = "074";
                break;
            case 1501:
                ocupacion = "075";
                break;
            case 5010:
                ocupacion = "076";
                break;
            case 5011:
                ocupacion = "077";
                break;
            case 3008:
                ocupacion = "078";
                break;
            case 4000:
                ocupacion = "013";
                break;
            case 2011:
                ocupacion = "079";
                break;
            case 5012:
                ocupacion = "080";
                break;
            case 5013:
                ocupacion = "081";
                break;
            case 5014:
                ocupacion = "082";
                break;
            case 3009:
                ocupacion = "083";
                break;
            case 2012:
                ocupacion = "007";
                break;
            case 5015:
                ocupacion = "084";
                break;
            case 5016:
                ocupacion = "085";
                break;
            case 3010:
                ocupacion = "086";
                break;
            case 1600:
                ocupacion = "087";
                break;
            case 3013:
                ocupacion = "088";
                break;
            case 3011:
                ocupacion = "089";
                break;
            case 3012:
                ocupacion = "090";
                break;
            case 5017:
                ocupacion = "091";
                break;
            case 2013:
                ocupacion = "092";
                break;
            case 2014:
                ocupacion = "026";
                break;
            case 1502:
                ocupacion = "093";
                break;
            case 2015:
                ocupacion = "008";
                break;
            case 2016:
                ocupacion = "094";
                break;
            case 15:
                ocupacion = "095";
                break;
            case 5018:
                ocupacion = "096";
                break;
            case 5019:
                ocupacion = "097";
                break;
            case 1001:
                ocupacion = "018";
                break;
            case 1004:
                ocupacion = "018";
                break;
            case 1002:
                ocupacion = "018";
                break;
            case 1005:
                ocupacion = "018";
                break;
            case 1003:
                ocupacion = "018";
                break;
            case 6000:
                ocupacion = "098";
                break;
            case 3031:
                ocupacion = "099";
                break;
            case 3014:
                ocupacion = "100";
                break;
            case 2017:
                ocupacion = "101";
                break;
            case 2018:
                ocupacion = "102";
                break;
            case 6003:
                ocupacion = "103";
                break;
            case 2019:
                ocupacion = "104";
                break;
            case 98:
                ocupacion = "006";
                break;
            case 17:
                ocupacion = "105";
                break;
            case 5020:
                ocupacion = "106";
                break;
            case 2020:
                ocupacion = "107";
                break;
            case 5021:
                ocupacion = "108";
                break;
            case 2021:
                ocupacion = "109";
                break;
            case 2022:
                ocupacion = "110";
                break;
            case 1708:
                ocupacion = "111";
                break;
            case 3015:
                ocupacion = "112";
                break;
            case 3016:
                ocupacion = "113";
                break;
            case 5022:
                ocupacion = "114";
                break;
            case 3017:
                ocupacion = "115";
                break;
            case 5023:
                ocupacion = "116";
                break;
            case 5024:
                ocupacion = "117";
                break;
            case 1709:
                ocupacion = "118";
                break;
            case 18:
                ocupacion = "119";
                break;
            case 1605:
                ocupacion = "120";
                break;
            case 1710:
                ocupacion = "121";
                break;
            case 2023:
                ocupacion = "122";
                break;
            case 2024:
                ocupacion = "123";
                break;
            case 1606:
                ocupacion = "124";
                break;
            case 1503:
                ocupacion = "125";
                break;
            case 5025:
                ocupacion = "126";
                break;
            case 1504:
                ocupacion = "127";
                break;
            case 2025:
                ocupacion = "128";
                break;
            case 3018:
                ocupacion = "129";
                break;
            case 2026:
                ocupacion = "130";
                break;
            case 1711:
                ocupacion = "131";
                break;
            case 5026:
                ocupacion = "132";
                break;
            case 5027:
                ocupacion = "133";
                break;
            case 7005:
                ocupacion = "134";
                break;
            case 7001:
                ocupacion = "135";
                break;
            case 7002:
                ocupacion = "136";
                break;
            case 7004:
                ocupacion = "137";
                break;
            case 7003:
                ocupacion = "138";
                break;
            case 7000:
                ocupacion = "139";
                break;
            case 5000:
                ocupacion = "140";
                break;
            case 2027:
                ocupacion = "141";
                break;
            case 2028:
                ocupacion = "004";
                break;
            case 5028:
                ocupacion = "142";
                break;
            case 5029:
                ocupacion = "143";
                break;
            case 3019:
                ocupacion = "144";
                break;
            case 4002:
                ocupacion = "145";
                break;
            case 1607:
                ocupacion = "146";
                break;
            case 5030:
                ocupacion = "147";
                break;
            case 2029:
                ocupacion = "148";
                break;
            case 3020:
                ocupacion = "149";
                break;
            case 5031:
                ocupacion = "150";
                break;
            case 2030:
                ocupacion = "151";
                break;
            case 2031:
                ocupacion = "152";
                break;
            case 2053:
                ocupacion = "153";
                break;
            case 2032:
                ocupacion = "154";
                break;
            case 2033:
                ocupacion = "155";
                break;
            case 1505:
                ocupacion = "156";
                break;
            case 5032:
                ocupacion = "157";
                break;
            case 6004:
                ocupacion = "158";
                break;
            case 5033:
                ocupacion = "159";
                break;
            case 3021:
                ocupacion = "160";
                break;
            case 3022:
                ocupacion = "161";
                break;
            case 2034:
                ocupacion = "162";
                break;
            case 4003:
                ocupacion = "163";
                break;
            case 2035:
                ocupacion = "012";
                break;
            case 2037:
                ocupacion = "164";
                break;
            case 5034:
                ocupacion = "165";
                break;
            case 2036:
                ocupacion = "166";
                break;
            case 100:
                ocupacion = "167";
                break;
            case 1506:
                ocupacion = "168";
                break;
            case 4001:
                ocupacion = "169";
                break;
            case 5035:
                ocupacion = "170";
                break;
            case 1507:
                ocupacion = "171";
                break;
            case 5036:
                ocupacion = "172";
                break;
            case 5037:
                ocupacion = "173";
                break;
            case 2038:
                ocupacion = "174";
                break;
            case 2039:
                ocupacion = "175";
                break;
            case 3023:
                ocupacion = "176";
                break;
            case 2040:
                ocupacion = "177";
                break;
            case 2041:
                ocupacion = "178";
                break;
            case 5038:
                ocupacion = "179";
                break;
            case 3024:
                ocupacion = "180";
                break;
            case 1508:
                ocupacion = "181";
                break;
            case 8004:
                ocupacion = "182";
                break;
            case 8005:
                ocupacion = "183";
                break;
            case 8002:
                ocupacion = "184";
                break;
            case 8003:
                ocupacion = "185";
                break;
            case 8001:
                ocupacion = "186";
                break;
            case 8000:
                ocupacion = "187";
                break;
            case 5039:
                ocupacion = "188";
                break;
            case 2042:
                ocupacion = "189";
                break;
            case 2043:
                ocupacion = "190";
                break;
            case 6005:
                ocupacion = "191";
                break;
            case 5040:
                ocupacion = "192";
                break;
            case 5041:
                ocupacion = "193";
                break;
            case 3025:
                ocupacion = "194";
                break;
            case 1801:
                ocupacion = "195";
                break;
            case 5042:
                ocupacion = "196";
                break;
            case 3026:
                ocupacion = "197";
                break;
            case 2044:
                ocupacion = "198";
                break;
            case 6006:
                ocupacion = "199";
                break;
            case 2045:
                ocupacion = "200";
                break;
            case 1700:
                ocupacion = "201";
                break;
            case 2000:
                ocupacion = "202";
                break;
            case 2046:
                ocupacion = "203";
                break;
            case 3027:
                ocupacion = "025";
                break;
            case 2047:
                ocupacion = "204";
                break;
            case 5043:
                ocupacion = "205";
                break;
            case 2048:
                ocupacion = "206";
                break;
            case 3028:
                ocupacion = "207";
                break;
            case 1509:
                ocupacion = "208";
                break;
            case 1510:
                ocupacion = "209";
                break;
            case 6007:
                ocupacion = "210";
                break;
            case 3029:
                ocupacion = "211";
                break;
            case 6012:
                ocupacion = "212";
                break;
            case 6008:
                ocupacion = "213";
                break;
            case 6009:
                ocupacion = "214";
                break;
            case 6010:
                ocupacion = "215";
                break;
            case 6011:
                ocupacion = "216";
                break;
            case 6013:
                ocupacion = "217";
                break;
            case 1802:
                ocupacion = "218";
                break;
            case 2049:
                ocupacion = "219";
                break;
            case 5044:
                ocupacion = "220";
                break;
            case 1803:
                ocupacion = "221";
                break;
            case 1511:
                ocupacion = "222";
                break;
            case 1804:
                ocupacion = "223";
                break;
            case 1512:
                ocupacion = "224";
                break;
            case 3030:
                ocupacion = "225";
                break;
            case 3000:
                ocupacion = "017";
                break;
            case 1608:
                ocupacion = "226";
                break;
            case 2050:
                ocupacion = "227";
                break;
            case 2051:
                ocupacion = "228";
                break;
            case 5045:
                ocupacion = "229";
                break;
            case 3032:
                ocupacion = "230";
                break;
            case 6014:
                ocupacion = "231";
                break;
            case 6015:
                ocupacion = "232";
                break;
            case 6016:
                ocupacion = "233";
                break;
            case 6017:
                ocupacion = "234";
                break;
            case 6018:
                ocupacion = "235";
                break;
            case 6019:
                ocupacion = "236";
                break;
            case 3033:
                ocupacion = "237";
                break;
            case 6020:
                ocupacion = "238";
                break;
            case 33:
                ocupacion = "239";
                break;
            case 3034:
                ocupacion = "240";
                break;
            case 5046:
                ocupacion = "241";
                break;
            case 5047:
                ocupacion = "242";
                break;
            case 1805:
                ocupacion = "243";
                break;
            case 1806:
                ocupacion = "244";
                break;
            default:
                ocupacion = "999";
                break;
        }
        return ocupacion;
    }
}
