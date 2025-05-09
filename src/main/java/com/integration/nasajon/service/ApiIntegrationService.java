package com.integration.nasajon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integration.nasajon.config.IntegrationConfig;
import com.integration.nasajon.dao.IntegrationLogDAO;
import com.integration.nasajon.dao.LancamentoContabilDAO;
import com.integration.nasajon.model.IntegrationLog;
import com.integration.nasajon.model.LancamentoContabil;
import com.integration.nasajon.util.RetryUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApiIntegrationService implements IntegrationService {
    private static final Logger logger = Logger.getLogger(ApiIntegrationService.class.getName());
    
    private final IntegrationConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final LancamentoContabilDAO lancamentoDAO;
    private final IntegrationLogDAO logDAO;
    
    public ApiIntegrationService(IntegrationConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        this.lancamentoDAO = new LancamentoContabilDAO(config);
        this.logDAO = new IntegrationLogDAO(config);
    }
    
    @Override
    public boolean enviarLote(List<LancamentoContabil> lancamentos) {
        try {
            logger.info("Iniciando envio de lote via API. Total de lançamentos: " + lancamentos.size());
            logDAO.salvar(new IntegrationLog("INFO", "Iniciando envio de lote via API", "Total de lançamentos: " + lancamentos.size()));
            
            // Dividir em lotes menores se necessário
            if (lancamentos.size() > config.getBatchSize()) {
                logger.info("Dividindo em lotes menores de " + config.getBatchSize() + " lançamentos");
                boolean allSuccess = true;
                
                for (int i = 0; i < lancamentos.size(); i += config.getBatchSize()) {
                    int end = Math.min(i + config.getBatchSize(), lancamentos.size());
                    List<LancamentoContabil> batch = lancamentos.subList(i, end);
                    
                    boolean success = enviarLoteIndividual(batch);
                    if (!success) {
                        allSuccess = false;
                    }
                }
                
                return allSuccess;
            } else {
                return enviarLoteIndividual(lancamentos);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao enviar lote via API", e);
            logDAO.salvar(new IntegrationLog("ERROR", "Erro ao enviar lote via API", e.getMessage()));
            return false;
        }
    }
    
    private boolean enviarLoteIndividual(List<LancamentoContabil> lancamentos) {
        try {
            String requestBody = objectMapper.writeValueAsString(lancamentos);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiToken())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            // Usar utilitário de retry para tentar novamente em caso de falha
            HttpResponse<String> response = RetryUtil.executeWithRetry(
                    () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                    config.getRetryAttempts()
            );
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Lote enviado com sucesso! Resposta: " + response.body());
                logDAO.salvar(new IntegrationLog("INFO", "Lote enviado com sucesso", "Resposta: " + response.body()));
                
                // Atualizar status dos lançamentos
                for (LancamentoContabil lancamento : lancamentos) {
                    lancamentoDAO.atualizarStatus(lancamento.getId(), "ENVIADO", null);
                }
                
                return true;
            } else {
                String errorMsg = "Erro ao enviar lote. Código: " + response.statusCode() + ", Mensagem: " + response.body();
                logger.warning(errorMsg);
                logDAO.salvar(new IntegrationLog("WARNING", "Erro ao enviar lote", errorMsg));
                
                // Atualizar status dos lançamentos
                for (LancamentoContabil lancamento : lancamentos) {
                    lancamentoDAO.atualizarStatus(lancamento.getId(), "ERRO", errorMsg);
                }
                
                return false;
            }
            
        } catch (Exception e) {
            String errorMsg = "Erro ao enviar lote individual: " + e.getMessage();
            logger.log(Level.SEVERE, errorMsg, e);
            logDAO.salvar(new IntegrationLog("ERROR", "Erro ao enviar lote individual", e.getMessage()));
            
            // Atualizar status dos lançamentos
            for (LancamentoContabil lancamento : lancamentos) {
                lancamentoDAO.atualizarStatus(lancamento.getId(), "ERRO", errorMsg);
            }
            
            return false;
        }
    }
    
    @Override
    public String verificarStatusLote(String loteId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl() + "/status/" + loteId))
                    .header("Authorization", "Bearer " + config.getApiToken())
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                logger.warning("Erro ao verificar status do lote. Código: " + response.statusCode());
                return "ERRO";
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao verificar status do lote", e);
            return "ERRO";
        }
    }
    
    @Override
    public int executarIntegracao() {
        try {
            // Buscar lançamentos pendentes
            List<LancamentoContabil> lancamentosPendentes = lancamentoDAO.buscarPorStatus("PENDENTE");
            
            if (lancamentosPendentes.isEmpty()) {
                logger.info("Nenhum lançamento pendente para integração");
                return 0;
            }
            
            logger.info("Iniciando integração de " + lancamentosPendentes.size() + " lançamentos pendentes");
            logDAO.salvar(new IntegrationLog("INFO", "Iniciando integração automática", 
                    "Total de lançamentos pendentes: " + lancamentosPendentes.size()));
            
            // Enviar lançamentos
            boolean success = enviarLote(lancamentosPendentes);
            
            if (success) {
                logger.info("Integração concluída com sucesso");
                logDAO.salvar(new IntegrationLog("INFO", "Integração concluída com sucesso", 
                        "Total de lançamentos processados: " + lancamentosPendentes.size()));
                return lancamentosPendentes.size();
            } else {
                logger.warning("Integração concluída com erros");
                logDAO.salvar(new IntegrationLog("WARNING", "Integração concluída com erros", 
                        "Verifique os logs para mais detalhes"));
                return 0;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao executar integração", e);
            logDAO.salvar(new IntegrationLog("ERROR", "Erro ao executar integração", e.getMessage()));
            return 0;
        }
    }
}
