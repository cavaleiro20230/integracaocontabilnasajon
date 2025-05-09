package com.integration.nasajon.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IntegrationConfig {
    private static final Logger logger = Logger.getLogger(IntegrationConfig.class.getName());
    
    // Configurações de Integração
    private String integrationType;
    private String apiUrl;
    private String apiToken;
    private String outputDirectory;
    private String fileFormat;
    private boolean useFtp;
    private String ftpHost;
    private String ftpUser;
    private String ftpPassword;
    private int batchSize;
    private int retryAttempts;
    
    // Configurações do SQL Server
    private String sqlServerHost;
    private int sqlServerPort;
    private String sqlServerDatabase;
    private String sqlServerUser;
    private String sqlServerPassword;
    private String sqlServerSchema;
    
    // Configurações de Agendamento
    private String scheduleExpression;
    private boolean schedulingEnabled;
    
    public static IntegrationConfig load(String configFile) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Arquivo de configuração não encontrado. Usando valores padrão.", e);
            // Criar arquivo de configuração padrão se não existir
            return createDefaultConfig(configFile);
        }
        
        IntegrationConfig config = new IntegrationConfig();
        
        // Configurações de Integração
        config.setIntegrationType(props.getProperty("integration.type", "api"));
        config.setApiUrl(props.getProperty("api.url", "https://api.nasajon.com.br/contabil/lancamentos"));
        config.setApiToken(props.getProperty("api.token", ""));
        config.setOutputDirectory(props.getProperty("file.output.directory", "./output"));
        config.setFileFormat(props.getProperty("file.format", "csv"));
        config.setUseFtp(Boolean.parseBoolean(props.getProperty("ftp.use", "false")));
        config.setFtpHost(props.getProperty("ftp.host", ""));
        config.setFtpUser(props.getProperty("ftp.user", ""));
        config.setFtpPassword(props.getProperty("ftp.password", ""));
        config.setBatchSize(Integer.parseInt(props.getProperty("batch.size", "100")));
        config.setRetryAttempts(Integer.parseInt(props.getProperty("retry.attempts", "3")));
        
        // Configurações do SQL Server
        config.setSqlServerHost(props.getProperty("sqlserver.host", "localhost"));
        config.setSqlServerPort(Integer.parseInt(props.getProperty("sqlserver.port", "1433")));
        config.setSqlServerDatabase(props.getProperty("sqlserver.database", "contabilidade"));
        config.setSqlServerUser(props.getProperty("sqlserver.user", "sa"));
        config.setSqlServerPassword(props.getProperty("sqlserver.password", ""));
        config.setSqlServerSchema(props.getProperty("sqlserver.schema", "dbo"));
        
        // Configurações de Agendamento
        config.setScheduleExpression(props.getProperty("schedule.expression", "0 0 22 * * ?"));  // Padrão: todos os dias às 22h
        config.setSchedulingEnabled(Boolean.parseBoolean(props.getProperty("schedule.enabled", "false")));
        
        logger.log(Level.INFO, "Configuração carregada: tipo={0}", config.getIntegrationType());
        return config;
    }
    
    public void save(String configFile) throws IOException {
        Properties props = new Properties();
        
        // Configurações de Integração
        props.setProperty("integration.type", integrationType);
        props.setProperty("api.url", apiUrl);
        props.setProperty("api.token", apiToken);
        props.setProperty("file.output.directory", outputDirectory);
        props.setProperty("file.format", fileFormat);
        props.setProperty("ftp.use", String.valueOf(useFtp));
        props.setProperty("ftp.host", ftpHost);
        props.setProperty("ftp.user", ftpUser);
        props.setProperty("ftp.password", ftpPassword);
        props.setProperty("batch.size", String.valueOf(batchSize));
        props.setProperty("retry.attempts", String.valueOf(retryAttempts));
        
        // Configurações do SQL Server
        props.setProperty("sqlserver.host", sqlServerHost);
        props.setProperty("sqlserver.port", String.valueOf(sqlServerPort));
        props.setProperty("sqlserver.database", sqlServerDatabase);
        props.setProperty("sqlserver.user", sqlServerUser);
        props.setProperty("sqlserver.password", sqlServerPassword);
        props.setProperty("sqlserver.schema", sqlServerSchema);
        
        // Configurações de Agendamento
        props.setProperty("schedule.expression", scheduleExpression);
        props.setProperty("schedule.enabled", String.valueOf(schedulingEnabled));
        
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Configurações da Integração Nasajon");
        }
        
        logger.info("Configurações salvas com sucesso em " + configFile);
    }
    
    private static IntegrationConfig createDefaultConfig(String configFile) throws IOException {
        IntegrationConfig config = new IntegrationConfig();
        
        // Configurações padrão
        config.setIntegrationType("api");
        config.setApiUrl("https://api.nasajon.com.br/contabil/lancamentos");
        config.setApiToken("");
        config.setOutputDirectory("./output");
        config.setFileFormat("csv");
        config.setUseFtp(false);
        config.setFtpHost("");
        config.setFtpUser("");
        config.setFtpPassword("");
        config.setBatchSize(100);
        config.setRetryAttempts(3);
        
        config.setSqlServerHost("localhost");
        config.setSqlServerPort(1433);
        config.setSqlServerDatabase("contabilidade");
        config.setSqlServerUser("sa");
        config.setSqlServerPassword("");
        config.setSqlServerSchema("dbo");
        
        config.setScheduleExpression("0 0 22 * * ?");
        config.setSchedulingEnabled(false);
        
        // Salvar configurações padrão
        config.save(configFile);
        
        return config;
    }

    // Getters e Setters
    public String getIntegrationType() {
        return integrationType;
    }

    public void setIntegrationType(String integrationType) {
        this.integrationType = integrationType;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public boolean isUseFtp() {
        return useFtp;
    }

    public void setUseFtp(boolean useFtp) {
        this.useFtp = useFtp;
    }

    public String getFtpHost() {
        return ftpHost;
    }

    public void setFtpHost(String ftpHost) {
        this.ftpHost = ftpHost;
    }

    public String getFtpUser() {
        return ftpUser;
    }

    public void setFtpUser(String ftpUser) {
        this.ftpUser = ftpUser;
    }

    public String getFtpPassword() {
        return ftpPassword;
    }

    public void setFtpPassword(String ftpPassword) {
        this.ftpPassword = ftpPassword;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public String getSqlServerHost() {
        return sqlServerHost;
    }

    public void setSqlServerHost(String sqlServerHost) {
        this.sqlServerHost = sqlServerHost;
    }

    public int getSqlServerPort() {
        return sqlServerPort;
    }

    public void setSqlServerPort(int sqlServerPort) {
        this.sqlServerPort = sqlServerPort;
    }

    public String getSqlServerDatabase() {
        return sqlServerDatabase;
    }

    public void setSqlServerDatabase(String sqlServerDatabase) {
        this.sqlServerDatabase = sqlServerDatabase;
    }

    public String getSqlServerUser() {
        return sqlServerUser;
    }

    public void setSqlServerUser(String sqlServerUser) {
        this.sqlServerUser = sqlServerUser;
    }

    public String getSqlServerPassword() {
        return sqlServerPassword;
    }

    public void setSqlServerPassword(String sqlServerPassword) {
        this.sqlServerPassword = sqlServerPassword;
    }

    public String getSqlServerSchema() {
        return sqlServerSchema;
    }

    public void setSqlServerSchema(String sqlServerSchema) {
        this.sqlServerSchema = sqlServerSchema;
    }

    public String getScheduleExpression() {
        return scheduleExpression;
    }

    public void setScheduleExpression(String scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
    }

    public boolean isSchedulingEnabled() {
        return schedulingEnabled;
    }

    public void setSchedulingEnabled(boolean schedulingEnabled) {
        this.schedulingEnabled = schedulingEnabled;
    }
    
    public String getJdbcUrl() {
        return String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=true", 
                sqlServerHost, sqlServerPort, sqlServerDatabase);
    }
}
