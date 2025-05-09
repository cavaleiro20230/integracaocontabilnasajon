package com.integration.nasajon.gui;

import com.integration.nasajon.config.IntegrationConfig;
import com.integration.nasajon.dao.DatabaseManager;
import com.integration.nasajon.scheduler.IntegrationScheduler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApplication extends Application {
    private static final Logger logger = Logger.getLogger(MainApplication.class.getName());
    
    private static IntegrationConfig config;
    private static IntegrationScheduler scheduler;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Carregar configuração
            if (config == null) {
                config = IntegrationConfig.load("config.properties");
            }
            
            // Inicializar banco de dados
            DatabaseManager dbManager = new DatabaseManager(config);
            dbManager.initializeDatabase();
            
            // Inicializar agendador
            if (scheduler == null) {
                scheduler = new IntegrationScheduler(config);
                scheduler.initialize();
            }
            
            // Carregar interface gráfica
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            
            // Configurar controller
            MainController controller = loader.getController();
            controller.setConfig(config);
            controller.setScheduler(scheduler);
            controller.initialize();
            
            // Configurar janela principal
            Scene scene = new Scene(root, 1024, 768);
            primaryStage.setTitle("Integração Contábil Nasajon");
            primaryStage.setScene(scene);
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
            primaryStage.setOnCloseRequest(e -> {
                try {
                    config.save("config.properties");
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Erro ao salvar configurações", ex);
                }
            });
            
            primaryStage.show();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao iniciar aplicação", e);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
    public static void setConfig(IntegrationConfig config) {
        MainApplication.config = config;
    }
    
    public static void setScheduler(IntegrationScheduler scheduler) {
        MainApplication.scheduler = scheduler;
    }
}
