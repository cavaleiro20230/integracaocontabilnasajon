package com.integration.nasajon.dao;

import com.integration.nasajon.config.IntegrationConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    
    private final IntegrationConfig config;
    
    public DatabaseManager(IntegrationConfig config) {
        this.config = config;
    }
    
    public Connection getConnection() throws SQLException {
        try {
            // Carregar o driver JDBC do SQL Server
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            
            // Estabelecer conexão
            return DriverManager.getConnection(
                    config.getJdbcUrl(),
                    config.getSqlServerUser(),
                    config.getSqlServerPassword());
            
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Driver JDBC do SQL Server não encontrado", e);
            throw new SQLException("Driver JDBC não encontrado", e);
        }
    }
    
    public void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Criar tabela de lançamentos contábeis se não existir
            String createLancamentosTable = String.format(
                    "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'lancamentos_contabeis' AND schema_id = SCHEMA_ID('%s')) " +
                    "BEGIN " +
                    "    CREATE TABLE %s.lancamentos_contabeis ( " +
                    "        id BIGINT IDENTITY(1,1) PRIMARY KEY, " +
                    "        conta VARCHAR(50) NOT NULL, " +
                    "        historico VARCHAR(255) NOT NULL, " +
                    "        valor DECIMAL(18,2) NOT NULL, " +
                    "        data DATE NOT NULL, " +
                    "        natureza CHAR(1) NOT NULL, " +
                    "        status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', " +
                    "        mensagem_erro VARCHAR(MAX), " +
                    "        data_envio DATE " +
                    "    ) " +
                    "END",
                    config.getSqlServerSchema(), config.getSqlServerSchema());
            
            stmt.execute(createLancamentosTable);
            
            // Criar tabela de logs se não existir
            String createLogsTable = String.format(
                    "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'integration_logs' AND schema_id = SCHEMA_ID('%s')) " +
                    "BEGIN " +
                    "    CREATE TABLE %s.integration_logs ( " +
                    "        id BIGINT IDENTITY(1,1) PRIMARY KEY, " +
                    "        timestamp DATETIME NOT NULL DEFAULT GETDATE(), " +
                    "        tipo VARCHAR(20) NOT NULL, " +
                    "        mensagem VARCHAR(255) NOT NULL, " +
                    "        detalhes VARCHAR(MAX) " +
                    "    ) " +
                    "END",
                    config.getSqlServerSchema(), config.getSqlServerSchema());
            
            stmt.execute(createLogsTable);
            
            logger.info("Banco de dados inicializado com sucesso");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao inicializar o banco de dados", e);
        }
    }
    
    public void testConnection() throws SQLException {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                logger.info("Conexão com o SQL Server estabelecida com sucesso");
            }
        }
    }
}
