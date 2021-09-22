package py.com.bepsa.pojo;

import org.simpleframework.xml.Attribute;



public class Regrabacion {
    @Attribute(name = "idMotivo")
    private String idMotivo;
    @Attribute(name = "nroTarjeta")
    private String nroTarjeta;
    @Attribute(name = "nroTarjetaNueva")
    private String nroTarjetaNueva;
    @Attribute(name = "vencimiento")
    private String vencimiento;
    @Attribute(name = "importe")
    private String importe;
    @Attribute(name = "cobrarCosto")
    private String cobrarCosto;
    @Attribute(name = "fecha")
    private String fecha;
    @Attribute(name = "usuario")
    private String usuario;
    
    public static String regrabarExtravio = "157";
    public static String regrabarRotura = "158";
    public static String regrabarFueraFecha = "159";
    public static String regrabarMalaGrabacion = "160";
    public static String regrabarOrdenBanco = "161";

    public String getIdMotivo() {
        return idMotivo;
    }

    public void setIdMotivo(String idMotivo) {
        this.idMotivo = idMotivo;
    }

    public String getNroTarjeta() {
        return nroTarjeta;
    }

    public void setNroTarjeta(String nroTarjeta) {
        this.nroTarjeta = nroTarjeta;
    }

    public String getNroTarjetaNueva() {
        return nroTarjetaNueva;
    }

    public void setNroTarjetaNueva(String nroTarjetaNueva) {
        this.nroTarjetaNueva = nroTarjetaNueva;
    }

    public String getVencimiento() {
        return vencimiento;
    }

    public void setVencimiento(String vencimiento) {
        this.vencimiento = vencimiento;
    }

    public String getImporte() {
        return importe;
    }

    public void setImporte(String importe) {
        this.importe = importe;
    }

    public String getCobrarCosto() {
        return cobrarCosto;
    }

    public void setCobrarCosto(String cobrarCosto) {
        this.cobrarCosto = cobrarCosto;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
    
    

}
