package com.integration.nasajon.gui;

import com.integration.nasajon.dao.LancamentoContabilDAO;
import com.integration.nasajon.model.LancamentoContabil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LancamentoFormController {
    private static final Logger logger = Logger.getLogger(LancamentoFormController.class.getName());
    
    @FXML private TextField txtConta;
    @FXML private TextField txtHistorico;
    @FXML private TextField txtValor;
    @FXML private DatePicker dpData;
    @FXML private ComboBox<String> cmbNatureza;
    @FXML private Button btnSalvar;
    @FXML private Button btnCancelar;
    
    private LancamentoContabil lancamento;
    private LancamentoContabilDAO lancamentoDAO;
    
    @FXML
    public void initialize() {
        // Inicializar combo de natureza
        cmbNatureza.getItems().addAll("D", "C");
        
        // Validação de campos
        txtValor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                txtValor.setText(oldValue);
            }
        });
    }
    
    public void setLancamento(LancamentoContabil lancamento) {
        this.lancamento = lancamento;
        
        // Preencher campos com dados do lançamento
        if (lancamento.getId() != null) {
            txtConta.setText(lancamento.getConta());
            txtHistorico.setText(lancamento.getHistorico());
            txtValor.setText(String.valueOf(lancamento.getValor()));
            dpData.setValue(lancamento.getData());
            cmbNatureza.setValue(lancamento.getNatureza());
        } else {
            // Valores padrão para novo lançamento
            dpData.setValue(LocalDate.now());
            cmbNatureza.setValue("D");
        }
    }
    
    public void setLancamentoDAO(LancamentoContabilDAO lancamentoDAO) {
        this.lancamentoDAO = lancamentoDAO;
    }
    
    @FXML
    private void onSalvar(ActionEvent event) {
        try {
            // Validar campos obrigatórios
            if (txtConta.getText().isEmpty() || txtHistorico.getText().isEmpty() || 
                txtValor.getText().isEmpty() || dpData.getValue() == null || 
                cmbNatureza.getValue() == null) {
                
                showAlert(Alert.AlertType.WARNING, "Campos obrigatórios", 
                        "Preencha todos os campos obrigatórios.");
                return;
            }
            
            // Atualizar objeto com valores do formulário
            lancamento.setConta(txtConta.getText());
            lancamento.setHistorico(txtHistorico.getText());
            lancamento.setValor(Double.parseDouble(txtValor.getText()));
            lancamento.setData(dpData.getValue());
            lancamento.setNatureza(cmbNatureza.getValue());
            
            // Salvar no banco de dados
            lancamentoDAO.salvar(lancamento);
            
            // Fechar formulário
            Stage stage = (Stage) btnSalvar.getScene().getWindow();
            stage.close();
            
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Valor inválido", 
                    "O valor deve ser um número válido.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao salvar lançamento", e);
            showAlert(Alert.AlertType.ERROR, "Erro ao salvar", e.getMessage());
        }
    }
    
    @FXML
    private void onCancelar(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(type.toString());
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
