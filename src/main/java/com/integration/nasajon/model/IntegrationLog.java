package com.integration.nasajon.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IntegrationLog {
    private Long id;
    private LocalDateTime timestamp;
    private String tipo;  // INFO, WARNING, ERROR
    private String mensagem;
    private String detalhes;
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public IntegrationLog() {
        this.timestamp = LocalDateTime.now();
    }

    public IntegrationLog(String tipo, String mensagem) {
        this();
        this.tipo = tipo;
        this.mensagem = mensagem;
    }

    public IntegrationLog(String tipo, String mensagem, String detalhes) {
        this(tipo, mensagem);
        this.detalhes = detalhes;
    }
    
    public IntegrationLog(Long id, LocalDateTime timestamp, String tipo, String mensagem, String detalhes) {
        this.id = id;
        this.timestamp = timestamp;
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.detalhes = detalhes;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getTimestampFormatado() {
        return timestamp != null ? timestamp.format(DATETIME_FORMATTER) : "";
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }

    @Override
    public String toString() {
        return "[" + getTimestampFormatado() + "] " + tipo + ": " + mensagem;
    }
}
