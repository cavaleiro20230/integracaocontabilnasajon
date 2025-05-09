package com.integration.nasajon.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class LancamentoContabil {
    private Long id;
    private String conta;
    private String historico;
    private double valor;
    private LocalDate data;
    private String natureza; // D para débito, C para crédito
    private String status; // PENDENTE, ENVIADO, ERRO
    private String mensagemErro;
    private LocalDate dataEnvio;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public LancamentoContabil() {
        this.status = "PENDENTE";
    }

    public LancamentoContabil(String conta, String historico, double valor, LocalDate data, String natureza) {
        this();
        this.conta = conta;
        this.historico = historico;
        this.valor = valor;
        this.data = data;
        this.natureza = natureza;
    }
    
    public LancamentoContabil(Long id, String conta, String historico, double valor, LocalDate data, 
                             String natureza, String status, String mensagemErro, LocalDate dataEnvio) {
        this.id = id;
        this.conta = conta;
        this.historico = historico;
        this.valor = valor;
        this.data = data;
        this.natureza = natureza;
        this.status = status;
        this.mensagemErro = mensagemErro;
        this.dataEnvio = dataEnvio;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConta() {
        return conta;
    }

    public void setConta(String conta) {
        this.conta = conta;
    }

    public String getHistorico() {
        return historico;
    }

    public void setHistorico(String historico) {
        this.historico = historico;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public LocalDate getData() {
        return data;
    }
    
    public String getDataFormatada() {
        return data != null ? data.format(DATE_FORMATTER) : "";
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getNatureza() {
        return natureza;
    }

    public void setNatureza(String natureza) {
        this.natureza = natureza;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public void setMensagemErro(String mensagemErro) {
        this.mensagemErro = mensagemErro;
    }

    public LocalDate getDataEnvio() {
        return dataEnvio;
    }
    
    public String getDataEnvioFormatada() {
        return dataEnvio != null ? dataEnvio.format(DATE_FORMATTER) : "";
    }

    public void setDataEnvio(LocalDate dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LancamentoContabil that = (LancamentoContabil) o;
        return Double.compare(that.valor, valor) == 0 &&
                Objects.equals(id, that.id) &&
                Objects.equals(conta, that.conta) &&
                Objects.equals(historico, that.historico) &&
                Objects.equals(data, that.data) &&
                Objects.equals(natureza, that.natureza);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, conta, historico, valor, data, natureza);
    }

    @Override
    public String toString() {
        return "LancamentoContabil{" +
                "id=" + id +
                ", conta='" + conta + '\'' +
                ", historico='" + historico + '\'' +
                ", valor=" + valor +
                ", data=" + data +
                ", natureza='" + natureza + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
