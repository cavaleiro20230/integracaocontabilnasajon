package com.integration.nasajon.service;

import com.integration.nasajon.model.LancamentoContabil;

import java.util.List;

public interface IntegrationService {
    /**
     * Envia um lote de lançamentos contábeis para o sistema Nasajon
     * @param lancamentos Lista de lançamentos contábeis a serem enviados
     * @return true se o envio foi bem-sucedido, false caso contrário
     */
    boolean enviarLote(List<LancamentoContabil> lancamentos);
    
    /**
     * Verifica o status de um lote enviado anteriormente
     * @param loteId Identificador do lote
     * @return Status do lote
     */
    String verificarStatusLote(String loteId);
    
    /**
     * Executa a integração para todos os lançamentos pendentes
     * @return Número de lançamentos processados com sucesso
     */
    int executarIntegracao();
}
