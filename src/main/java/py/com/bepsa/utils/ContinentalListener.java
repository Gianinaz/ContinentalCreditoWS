///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package py.com.bepsa.utils;
//
//import javax.servlet.ServletContextAttributeEvent;
//import javax.servlet.ServletContextAttributeListener;
//import javax.servlet.ServletContextEvent;
//import javax.servlet.ServletContextListener;
//import javax.servlet.annotation.WebListener;
//import org.apache.log4j.Logger;
//
///**
// *
// * @author ggarcia
// */
//@WebListener
//public class ContinentalListener implements ServletContextAttributeListener, ServletContextListener {
//
//    private final WSServletContextListener listener;
//    
//    public ContinentalListener() {
//        this.listener = new WSServletContextListener();
//    }
//    
//     final static Logger log4j = Logger.getLogger(ContinentalListener.class.getName());
//
//    @Override
//    public void contextInitialized(ServletContextEvent sce) {
//        log4j.info("----------------------contextInitialized--------------------------");
//    }
//
//    @Override
//    public void contextDestroyed(ServletContextEvent sce) {
//        log4j.info("-------------------------contextDestroyed-----------------------");
//    }
//
//    @Override
//    public void attributeAdded(ServletContextAttributeEvent scae) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public void attributeRemoved(ServletContextAttributeEvent scae) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public void attributeReplaced(ServletContextAttributeEvent scae) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//    
//}
