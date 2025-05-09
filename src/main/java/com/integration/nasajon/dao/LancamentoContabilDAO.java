package com.integration.nasajon.dao;

import com.integration.nasajon.config.IntegrationConfig;
import com.integration.nasajon.model.LancamentoContabil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LancamentoContabilDAO {
    private static final Logger logger = Logger.getLogger(LancamentoContabilDAO.class.getName());
    
    private final DatabaseManager dbManager;
    private final String schema;
    
    public LancamentoContabilDAO(IntegrationConfig config) {
        this.dbManager = new DatabaseManager(config);
        this.schema = config.getSqlServerSchema();
    }
    
    public void salvar(LancamentoContabil lancamento) {
        String sql;
        
        if (lancamento.getId() == null) {
            // Insert
            sql = String.format(
                    "INSERT INTO %s.lancamentos_contabeis (conta, historico, valor, data, natureza, status, mensagem_erro, data_envio) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    schema);
        } else {
            // Update
            sql = String.format(
                    "UPDATE %s.lancamentos_contabeis SET " +
                    "conta = ?, historico = ?, valor = ?, data = ?, natureza = ?, status = ?, mensagem_erro = ?, data_envio = ? " +
                    "WHERE id = ?",
                    schema);
        }
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, lancamento.getConta());
            stmt.setString(2, lancamento.getHistorico());
            stmt.setDouble(3, lancamento.getValor());
            stmt.setDate(4, lancamento.getData() != null ? Date.valueOf(lancamento.getData()) : null);
            stmt.setString(5, lancamento.getNatureza());
            stmt.setString(6, lancamento.getStatus());
            stmt.setString(7, lancamento.getMensagemErro());
            stmt.setDate(8, lancamento.getDataEnvio() != null ? Date.valueOf(lancamento.getDataEnvio()) : null);
            
            if (lancamento.getId() != null) {
                stmt.setLong(9, lancamento.getId());
            }
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar lançamento contábil, nenhuma linha afetada.");
            }
            
            if (lancamento.getId() == null) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        lancamento.setId(generatedKeys.getLong(1));
                    } else {
                        throw new SQLException("Falha ao salvar lançamento contábil, nenhum ID obtido.");
                    }
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao salvar lançamento contábil", e);
        }
    }
    
    public void excluir(Long id) {
        String sql = String.format("DELETE FROM %s.lancamentos_contabeis WHERE id = ?", schema);
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao excluir lançamento contábil", e);
        }
    }
    
    public LancamentoContabil buscarPorId(Long id) {
        String sql = String.format(
                "SELECT id, conta, historico, valor, data, natureza, status, mensagem_erro, data_envio " +
                "FROM %s.lancamentos_contabeis WHERE id = ?",
                schema);
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLancamento(rs);
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar lançamento contábil por ID", e);
        }
        
        return null;
    }
    
    public List<LancamentoContabil> listarTodos() {
        List<LancamentoContabil> lancamentos = new ArrayList<>();
        
        String sql = String.format(
                "SELECT id, conta, historico, valor, data, natureza, status, mensagem_erro, data_envio " +
                "FROM %s.lancamentos_contabeis ORDER BY data DESC, id DESC",
                schema);
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                lancamentos.add(mapResultSetToLancamento(rs));
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao listar lançamentos contábeis", e);
        }
        
        return lancamentos;
    }
    
    public List<LancamentoContabil> buscarPorStatus(String status) {
        List<LancamentoContabil> lancamentos = new ArrayList<>();
        
        String sql = String.format(
                "SELECT id, conta, historico, valor, data, natureza, status, mensagem_erro, data_envio " +
                "FROM %s.lancamentos_contabeis WHERE status = ? ORDER BY data DESC, id DESC",
                schema);
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lancamentos.add(mapResultSetToLancamento(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar lançamentos contábeis por status", e);
        }
        
        return lancamentos;
    }
    
    public List<LancamentoContabil> buscarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        List<LancamentoContabil> lancamentos = new ArrayList<>();
        
        String sql = String.format(
                "SELECT id, conta, historico, valor, data, natureza, status, mensagem_erro, data_envio " +
                "FROM %s.lancamentos_contabeis WHERE data BETWEEN ? AND ? ORDER BY data DESC, id DESC",
                schema);
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(dataInicio));
            stmt.setDate(2, Date.valueOf(dataFim));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lancamentos.add(mapResultSetToLancamento(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar lançamentos contábeis por período", e);
        }
        
        return lancamentos;
    }
    
    public void atualizarStatus(Long id, String status, String mensagemErro) {
        String sql = String.format(
                "UPDATE %s.lancamentos_contabeis SET status = ?, mensagem_erro = ?, data_envio = ? WHERE id = ?",
                schema);
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setString(2, mensagemErro);
            stmt.setDate(3, status.equals("ENVIADO") ? Date.valueOf(LocalDate.now()) : null);
            stmt.setLong(4, id);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao atualizar status do lançamento contábil", e);
        }
    }
    
    private LancamentoContabil mapResultSetToLancamento(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String conta = rs.getString("conta");
        String historico = rs.getString("historico");
        double valor = rs.getDouble("valor");
        LocalDate data = rs.getDate("data") != null ? rs.getDate("data").toLocalDate() : null;
        String natureza = rs.getString("natureza");
        String status = rs.getString("status");
        String mensagemErro = rs.getString("mensagem_erro");
        LocalDate dataEnvio = rs.getDate("data_envio") != null ? rs.getDate("data_envio").toLocalDate() : null;
        
        return new LancamentoContabil(id, conta, historico, valor, data, natureza, status, mensagemErro, dataEnvio);
    }
}
