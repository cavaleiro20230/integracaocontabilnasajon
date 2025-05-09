package com.integration.nasajon.service;

import com.integration.nasajon.config.IntegrationConfig;
import com.integration.nasajon.dao.IntegrationLogDAO;
import com.integration.nasajon.dao.LancamentoContabilDAO;
import com.integration.nasajon.model.IntegrationLog;
import com.integration.nasajon.model.LancamentoContabil;
import com.integration.nasajon.util.FtpUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileIntegrationService implements IntegrationService {
    private static final Logger logger = Logger.getLogger(FileIntegrationService.class.getName());
    
    private final IntegrationConfig config;
    private final LancamentoContabilDAO lancamentoDAO;
    private final IntegrationLogDAO logDAO;
    
    public FileIntegrationService(IntegrationConfig config) {
        this.config = config;
        this.lancamentoDAO = new LancamentoContabilDAO(config);
        this.logDAO = new IntegrationLogDAO(config);
        
        // Garantir que o diretório de saída existe
        try {
            Files.createDirectories(Paths.get(config.getOutputDirectory()));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao criar diretório de saída", e);
        }
    }
    
    @Override
    public boolean enviarLote(List<LancamentoContabil> lancamentos) {
        try {
            logger.info("Iniciando geração de arquivo para lote. Total de lançamentos: " + lancamentos.size());
            logDAO.salvar(new IntegrationLog("INFO", "Iniciando geração de arquivo para lote", 
                    "Total de lançamentos: " + lancamentos.size()));
            
            String fileName = gerarNomeArquivo();
            String filePath = config.getOutputDirectory() + File.separator + fileName;
            
            // Gerar arquivo de acordo com o formato configurado
            switch (config.getFileFormat().toLowerCase()) {
                case "csv":
                    gerarArquivoCsv(lancamentos, filePath);
                    break;
                case "xml":
                    gerarArquivoXml(lancamentos, filePath);
                    break;
                case "json":
                    gerarArquivoJson(lancamentos, filePath);
                    break;
                default:
                    String errorMsg = "Formato de arquivo não suportado: " + config.getFileFormat();
                    logger.warning(errorMsg);
                    logDAO.salvar(new IntegrationLog("WARNING", errorMsg));
                    return false;
            }
            
            // Enviar arquivo via FTP se configurado
            boolean success = true;
            if (config.isUseFtp()) {
                success = FtpUtil.enviarArquivo(
                        config.getFtpHost(),
                        config.getFtpUser(),
                        config.getFtpPassword(),
                        filePath,
                        fileName
                );
            }
            
            if (success) {
                logger.info("Arquivo gerado com sucesso: " + filePath);
                logDAO.salvar(new IntegrationLog("INFO", "Arquivo gerado com sucesso", 
                        "Caminho: " + filePath + (config.isUseFtp() ? " e enviado via FTP" : "")));
                
                // Atualizar status dos lançamentos
                for (LancamentoContabil lancamento : lancamentos) {
                    lancamentoDAO.atualizarStatus(lancamento.getId(), "ENVIADO", null);
                }
            } else {
                String errorMsg = "Erro ao enviar arquivo via FTP";
                logger.warning(errorMsg);
                logDAO.salvar(new IntegrationLog("WARNING", errorMsg));
                
                // Atualizar status dos lançamentos
                for (LancamentoContabil lancamento : lancamentos) {
                    lancamentoDAO.atualizarStatus(lancamento.getId(), "ERRO", "Erro ao enviar arquivo via FTP");
                }
            }
            
            return success;
            
        } catch (Exception e) {
            String errorMsg = "Erro ao gerar arquivo para lote: " + e.getMessage();
            logger.log(Level.SEVERE, errorMsg, e);
            logDAO.salvar(new IntegrationLog("ERROR", "Erro ao gerar arquivo para lote", e.getMessage()));
            
            // Atualizar status dos lançamentos
            for (LancamentoContabil lancamento : lancamentos) {
                lancamentoDAO.atualizarStatus(lancamento.getId(), "ERRO", errorMsg);
            }
            
            return false;
        }
    }
    
    private String gerarNomeArquivo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "lancamentos_contabeis_" + timestamp + "." + config.getFileFormat().toLowerCase();
    }
    
    private void gerarArquivoCsv(List<LancamentoContabil> lancamentos, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Cabeçalho
            writer.append("Conta;Historico;Valor;Data;Natureza\n");
            
            // Dados
            for (LancamentoContabil lancamento : lancamentos) {
                writer.append(lancamento.getConta()).append(";")
                      .append(lancamento.getHistorico()).append(";")
                      .append(String.valueOf(lancamento.getValor())).append(";")
                      .append(lancamento.getDataFormatada()).append(";")
                      .append(lancamento.getNatureza()).append("\n");
            }
        }
    }
    
    private void gerarArquivoXml(List<LancamentoContabil> lancamentos, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.append("<lancamentos>\n");
            
            for (LancamentoContabil lancamento : lancamentos) {
                writer.append("  <lancamento>\n");
                writer.append("    <conta>").append(lancamento.getConta()).append("</conta>\n");
                writer.append("    <historico>").append(lancamento.getHistorico()).append("</historico>\n");
                writer.append("    <valor>").append(String.valueOf(lancamento.getValor())).append("</valor>\n");
                writer.append("    <data>").append(lancamento.getDataFormatada()).append("</data>\n");
                writer.append("    <natureza>").append(lancamento.getNatureza()).append("</natureza>\n");
                writer.append("  </lancamento>\n");
            }
            
            writer.append("</lancamentos>");
        }
    }
    
    private void gerarArquivoJson(List<LancamentoContabil> lancamentos, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("{\n");
            writer.append("  \"lancamentos\": [\n");
            
            for (int i = 0; i < lancamentos.size(); i++) {
                LancamentoContabil lancamento = lancamentos.get(i);
                writer.append("    {\n");
                writer.append("      \"conta\": \"").append(lancamento.getConta()).append("\",\n");
                writer.append("      \"historico\": \"").append(lancamento.getHistorico()).append("\",\n");
                writer.append("      \"valor\": ").append(String.valueOf(lancamento.getValor())).append(",\n");
                writer.append("      \"data\": \"").append(lancamento.getDataFormatada()).append("\",\n");
                writer.append("      \"natureza\": \"").append(lancamento.getNatureza()).append("\"\n");
                writer.append("    }").append(i < lancamentos.size() - 1 ? "," : "").append("\n");
            }
            
            writer.append("  ]\n");
            writer.append("}");
        }
    }
    
    @Override
    public String verificarStatusLote(String loteId) {
        // Para integração baseada em arquivos, o status pode ser verificado
        // através de um arquivo de resposta ou outro mecanismo
        logger.info("Verificação de status não implementada para integração baseada em arquivos");
        return "DESCONHECIDO";
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
