package com.integration.nasajon.dao;

import com.integration.nasajon.config.IntegrationConfig;
import com.integration.nasajon.model.IntegrationLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IntegrationLogDAO {
    private static final Logger logger = Logger.getLogger(IntegrationLogDAO.class.getName());
    
    private final DatabaseManager dbManager;
    private final String schema;
    
    public IntegrationLogDAO(IntegrationConfig config) {
        this.dbManager = new DatabaseManager(config);
        this.schema = config.getSqlServerSchema();
    }
    
    public void salvar(IntegrationLog log) {
        String sql = String.format(
                "INSERT INTO %s.integration_logs (timestamp, tipo, mensagem, detalhes) VALUES (?, ?, ?, ?)",
                schema);
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(log.getTimestamp()));
            stmt.setString(2, log.getTipo());
            stmt.setString(3, log.getMensagem());
            stmt.setString(4, log.getDetalhes());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar log, nenhuma linha afetada.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    log.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Falha ao salvar log, nenhum ID obtido.");
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao salvar log", e);
        }
    }
    
    public List<IntegrationLog> listarTodos(int limit) {
        List<IntegrationLog> logs = new ArrayList<>();
        
        String sql = String.format(
                "SELECT TOP %d id, timestamp, tipo, mensagem, detalhes " +
                "FROM %s.integration_logs ORDER BY timestamp DESC",
                limit, schema);
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao listar logs", e);
        }
        
        return logs;
    }
    
    public List<IntegrationLog> buscarPorTipo(String tipo, int limit) {
        List<IntegrationLog> logs = new ArrayList<>();
        
        String sql = String.format(
                "SELECT TOP %d id, timestamp, tipo, mensagem, detalhes " +
                "FROM %s.integration_logs WHERE tipo = ? ORDER BY timestamp DESC",
                limit, schema);
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, tipo);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToLog(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar logs por tipo", e);
        }
        
        return logs;
    }
    
    private IntegrationLog mapResultSetToLog(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
        String tipo = rs.getString("tipo");
        String mensagem = rs.getString("mensagem");
        String detalhes = rs.getString("detalhes");
        
        return new IntegrationLog(id, timestamp, tipo, mensagem, detalhes);
    }
}
