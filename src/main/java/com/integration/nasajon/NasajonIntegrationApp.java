package com.integration.nasajon;

import com.integration.nasajon.config.IntegrationConfig;
import com.integration.nasajon.gui.MainApplication;
import com.integration.nasajon.scheduler.IntegrationScheduler;
import javafx.application.Application;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NasajonIntegrationApp {
    private static final Logger logger = Logger.getLogger(NasajonIntegrationApp.class.getName());

    public static void main(String[] args) {
        try {
            // Carregar configurações
            IntegrationConfig config = IntegrationConfig.load("config.properties");
            
            // Iniciar o agendador de tarefas
            IntegrationScheduler scheduler = new IntegrationScheduler(config);
            scheduler.initialize();
            
            // Iniciar a interface gráfica
            Application.launch(MainApplication.class, args);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao iniciar a aplicação", e);
            System.exit(1);
        }
    }
}
