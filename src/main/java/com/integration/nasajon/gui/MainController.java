package com.integration.nasajon.gui;

import com.integration.nasajon.config.IntegrationConfig;
import com.integration.nasajon.dao.DatabaseManager;
import com.integration.nasajon.dao.IntegrationLogDAO;
import com.integration.nasajon.dao.LancamentoContabilDAO;
import com.integration.nasajon.model.IntegrationLog;
import com.integration.nasajon.model.LancamentoContabil;
import com.integration.nasajon.scheduler.IntegrationScheduler;
import com.integration.nasajon.service.ApiIntegrationService;
import com.integration.nasajon.service.FileIntegrationService;
import com.integration.nasajon.service.IntegrationService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {
    private static final Logger logger = Logger.getLogger(MainController.class.getName());
    
    // Componentes da interface
    @FXML private TabPane tabPane;
    
    // Tab Lançamentos
    @FXML private TableView<LancamentoContabil> tblLancamentos;
    @FXML private TableColumn<LancamentoContabil, Long> colId;
    @FXML private TableColumn<LancamentoContabil, String> colConta;
    @FXML private TableColumn<LancamentoContabil, String> colHistorico;
    @FXML private TableColumn<LancamentoContabil, Double> colValor;
    @FXML private TableColumn<LancamentoContabil, String> colData;
    @FXML private TableColumn<LancamentoContabil, String> colNatureza;
    @FXML private TableColumn<LancamentoContabil, String> colStatus;
    @FXML private DatePicker dpDataInicio;
    @FXML private DatePicker dpDataFim;
    @FXML private ComboBox<String> cmbFiltroStatus;
    
    // Tab Logs
    @FXML private TableView<IntegrationLog> tblLogs;
    @FXML private TableColumn<IntegrationLog, String> colTimestamp;
    @FXML private TableColumn<IntegrationLog, String> colTipo;
    @FXML private TableColumn<IntegrationLog, String> colMensagem;
    @FXML private TextArea txtDetalhesLog;
    
    // Tab Configurações
    @FXML private TextField txtSqlServerHost;
    @FXML private TextField txtSqlServerPort;
    @FXML private TextField txtSqlServerDatabase;
    @FXML private TextField txtSqlServerUser;
    @FXML private PasswordField txtSqlServerPassword;
    @FXML private TextField txtSqlServerSchema;
    @FXML private ComboBox<String> cmbIntegrationType;
    @FXML private TextField txtApiUrl;
    @FXML private PasswordField txtApiToken;
    @FXML private TextField txtOutputDirectory;
    @FXML private ComboBox<String> cmbFileFormat;
    @FXML private CheckBox chkUseFtp;
    @FXML private TextField txtFtpHost;
    @FXML private TextField txtFtpUser;
    @FXML private PasswordField txtFtpPassword;
    @FXML private TextField txtBatchSize;
    @FXML private TextField txtRetryAttempts;
    @FXML private TextField txtScheduleExpression;
    @FXML private CheckBox chkSchedulingEnabled;
    @FXML private Button btnTestarConexao;
    @FXML private Button btnSalvarConfig;
    
    // Tab Agendamento
    @FXML private Label lblStatusAgendamento;
    @FXML private Label lblProximaExecucao;
    @FXML private Button btnIniciarAgendamento;
    @FXML private Button btnPararAgendamento;
    @FXML private Button btnExecutarAgora;
    
    // Dados
    private IntegrationConfig config;
    private IntegrationScheduler scheduler;
    private LancamentoContabilDAO lancamentoDAO;
    private IntegrationLogDAO logDAO;
    private ObservableList<LancamentoContabil> lancamentos;
    private ObservableList<IntegrationLog> logs;
    
    public void setConfig(IntegrationConfig config) {
        this.config = config;
    }
    
    public void setScheduler(IntegrationScheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    public void initialize() {
        if (config == null) {
            try {
                config = IntegrationConfig.load("config.properties");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Erro ao carregar configurações", e);
                showAlert(Alert.AlertType.ERROR, "Erro", "Erro ao carregar configurações", e.getMessage());
            }
        }
        
        lancamentoDAO = new LancamentoContabilDAO(config);
        logDAO = new IntegrationLogDAO(config);
        
        // Inicializar componentes
        inicializarTabelaLancamentos();
        inicializarTabelaLogs();
        inicializarFiltros();
        inicializarConfiguracao();
        
        // Carregar dados iniciais
        carregarLancamentos();
        carregarLogs();
        
        // Atualizar status do agendamento
        atualizarStatusAgendamento();
    }
    
    private void inicializarTabelaLancamentos() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colConta.setCellValueFactory(new PropertyValueFactory<>("conta"));
        colHistorico.setCellValueFactory(new PropertyValueFactory<>("historico"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colData.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDataFormatada()));
        colNatureza.setCellValueFactory(new PropertyValueFactory<>("natureza"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Formatação de células
        colValor.setCellFactory(column -> new TableCell<LancamentoContabil, Double>() {
            @Override
            protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                if (empty || valor == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", valor));
                }
            }
        });
        
        colStatus.setCellFactory(column -> new TableCell<LancamentoContabil, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "PENDENTE":
                            setStyle("-fx-text-fill: blue;");
                            break;
                        case "ENVIADO":
                            setStyle("-fx-text-fill: green;");
                            break;
                        case "ERRO":
                            setStyle("-fx-text-fill: red;");
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });
        
        // Menu de contexto
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem menuItemEditar = new MenuItem("Editar");
        menuItemEditar.setOnAction(e -> editarLancamentoSelecionado());
        
        MenuItem menuItemExcluir = new MenuItem("Excluir");
        menuItemExcluir.setOnAction(e -> excluirLancamentoSelecionado());
        
        MenuItem menuItemReenviar = new MenuItem("Reenviar");
        menuItemReenviar.setOnAction(e -> reenviarLancamentoSelecionado());
        
        contextMenu.getItems().addAll(menuItemEditar, menuItemExcluir, menuItemReenviar);
        tblLancamentos.setContextMenu(contextMenu);
        
        // Duplo clique para editar
        tblLancamentos.setRowFactory(tv -> {
            TableRow<LancamentoContabil> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editarLancamentoSelecionado();
                }
            });
            return row;
        });
    }
    
    private void inicializarTabelaLogs() {
        colTimestamp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimestampFormatado()));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colMensagem.setCellValueFactory(new PropertyValueFactory<>("mensagem"));
        
        // Formatação de células
        colTipo.setCellFactory(column -> new TableCell<IntegrationLog, String>() {
            @Override
            protected void updateItem(String tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(tipo);
                    switch (tipo) {
                        case "INFO":
                            setStyle("-fx-text-fill: green;");
                            break;
                        case "WARNING":
                            setStyle("-fx-text-fill: orange;");
                            break;
                        case "ERROR":
                            setStyle("-fx-text-fill: red;");
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });
        
        // Seleção de log para exibir detalhes
        tblLogs.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtDetalhesLog.setText(newSelection.getDetalhes());
            } else {
                txtDetalhesLog.clear();
            }
        });
    }
    
    private void inicializarFiltros() {
        // Filtro de status
        cmbFiltroStatus.setItems(FXCollections.observableArrayList("Todos", "PENDENTE", "ENVIADO", "ERRO"));
        cmbFiltroStatus.getSelectionModel().select(0);
        
        // Datas padrão (último mês)
        dpDataInicio.setValue(LocalDate.now().minusMonths(1));
        dpDataFim.setValue(LocalDate.now());
        
        // Listeners para filtros
        cmbFiltroStatus.setOnAction(e -> aplicarFiltros());
        dpDataInicio.setOnAction(e -> aplicarFiltros());
        dpDataFim.setOnAction(e -> aplicarFiltros());
    }
    
    private void inicializarConfiguracao() {
        // Tipos de integração
        cmbIntegrationType.setItems(FXCollections.observableArrayList("api", "file"));
        
        // Formatos de arquivo
        cmbFileFormat.setItems(FXCollections.observableArrayList("csv", "xml", "json"));
        
        // Carregar valores da configuração
        txtSqlServerHost.setText(config.getSqlServerHost());
        txtSqlServerPort.setText(String.valueOf(config.getSqlServerPort()));
        txtSqlServerDatabase.setText(config.getSqlServerDatabase());
        txtSqlServerUser.setText(config.getSqlServerUser());
        txtSqlServerPassword.setText(config.getSqlServerPassword());
        txtSqlServerSchema.setText(config.getSqlServerSchema());
        
        cmbIntegrationType.getSelectionModel().select(config.getIntegrationType());
        txtApiUrl.setText(config.getApiUrl());
        txtApiToken.setText(config.getApiToken());
        txtOutputDirectory.setText(config.getOutputDirectory());
        cmbFileFormat.getSelectionModel().select(config.getFileFormat());
        
        chkUseFtp.setSelected(config.isUseFtp());
        txtFtpHost.setText(config.getFtpHost());
        txtFtpUser.setText(config.getFtpUser());
        txtFtpPassword.setText(config.getFtpPassword());
        
        txtBatchSize.setText(String.valueOf(config.getBatchSize()));
        txtRetryAttempts.setText(String.valueOf(config.getRetryAttempts()));
        
        txtScheduleExpression.setText(config.getScheduleExpression());
        chkSchedulingEnabled.setSelected(config.isSchedulingEnabled());
        
        // Habilitar/desabilitar campos FTP
        atualizarCamposFtp();
        chkUseFtp.setOnAction(e -> atualizarCamposFtp());
        
        // Habilitar/desabilitar campos de acordo com o tipo de integração
        atualizarCamposIntegracao();
        cmbIntegrationType.setOnAction(e -> atualizarCamposIntegracao());
    }
    
    private void atualizarCamposFtp() {
        boolean useFtp = chkUseFtp.isSelected();
        txtFtpHost.setDisable(!useFtp);
        txtFtpUser.setDisable(!useFtp);
        txtFtpPassword.setDisable(!useFtp);
    }
    
    private void atualizarCamposIntegracao() {
        String integrationType = cmbIntegrationType.getValue();
        boolean isApi = "api".equalsIgnoreCase(integrationType);
        
        txtApiUrl.setDisable(!isApi);
        txtApiToken.setDisable(!isApi);
        
        txtOutputDirectory.setDisable(isApi);
        cmbFileFormat.setDisable(isApi);
        chkUseFtp.setDisable(isApi);
        txtFtpHost.setDisable(isApi || !chkUseFtp.isSelected());
        txtFtpUser.setDisable(isApi || !chkUseFtp.isSelected());
        txtFtpPassword.setDisable(isApi || !chkUseFtp.isSelected());
    }
    
    private void carregarLancamentos() {
        try {
            List<LancamentoContabil> lista;
            
            // Aplicar filtros
            String filtroStatus = cmbFiltroStatus.getValue();
            LocalDate dataInicio = dpDataInicio.getValue();
            LocalDate dataFim = dpDataFim.getValue();
            
            if ("Todos".equals(filtroStatus)) {
                if (dataInicio != null && dataFim != null) {
                    lista = lancamentoDAO.buscarPorPeriodo(dataInicio, dataFim);
                } else {
                    lista = lancamentoDAO.listarTodos();
                }
            } else {
                lista = lancamentoDAO.buscarPorStatus(filtroStatus);
            }
            
            lancamentos = FXCollections.observableArrayList(lista);
            tblLancamentos.setItems(lancamentos);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao carregar lançamentos", e);
            showAlert(Alert.AlertType.ERROR, "Erro", "Erro ao carregar lançamentos", e.getMessage());
        }
    }
    
    private void carregarLogs() {
        try {
            List<IntegrationLog> lista = logDAO.listarTodos(100);
            logs = FXCollections.observableArrayList(lista);
            tblLogs.setItems(logs);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao carregar logs", e);
            showAlert(Alert.AlertType.ERROR, "Erro", "Erro ao carregar logs", e.getMessage());
        }
    }
    
    private void aplicarFiltros() {
        carregarLancamentos();
    }
    
    @FXML
    private void onNovoLancamento(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LancamentoForm.fxml"));
            Parent root = loader.load();
            
            LancamentoFormController controller = loader.getController();
            controller.setLancamento(new LancamentoContabil());
            controller.setLancamentoDAO(lancamentoDAO);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Novo Lançamento Contábil");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Recarregar lançamentos após fechar o formulário
            carregarLancamentos();
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao abrir formulário de lançamento", e);
            showAlert(Alert.AlertType.ERROR, "Erro", "Erro ao abrir formulário", e.getMessage());
        }
    }
    
    private void editarLancamentoSelecionado() {
        LancamentoContabil lancamento = tblLancamentos.getSelectionModel().getSelectedItem();
        if (lancamento == null) {
            showAlert(Alert.AlertType.WARNING, "Aviso", "Nenhum lançamento selecionado", 
                    "Selecione um lançamento para editar.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LancamentoForm.fxml"));
            Parent root = loader.load();
            
            LancamentoFormController controller = loader.getController();
            controller.setLancamento(lancamento);
            controller.setLancamentoDAO(lancamentoDAO);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Editar Lançamento Contábil");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Recarregar lançamentos após fechar o formulário
            carregarLancamentos();
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao abrir formulário de lançamento", e);
            showAlert(Alert.AlertType.ERROR, "Erro", "Erro ao abrir formulário", e.getMessage());
        }
    }
    
    private void excluirLancamentoSelecionado() {
        LancamentoContabil lancamento = tblLancamentos.getSelectionModel().getSelectedItem();
        if (lancamento == null) {
            showAlert(Alert.AlertType.WARNING, "Aviso", "Nenhum lançamento selecionado", 
                    "Selecione um lançamento para excluir.");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmação");
        alert.setHeaderText("Excluir Lançamento");
        alert.setContentText("Tem certeza que deseja excluir o lançamento selecionado?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            lancamentoDAO.excluir(lancamento.getId());
            carregarLancamentos();
        }
    }
    
    private void reenviarLancamentoSelecionado() {
        LancamentoContabil lancamento = tblLancamentos.getSelectionModel().getSelectedItem();
        if (lancamento == null) {
            showAlert(Alert.AlertType.WARNING, "Aviso", "Nenhum lançamento selecionado", 
                    "Selecione um lançamento para reenviar.");
            return;
        }
        
        // Atualizar status para PENDENTE
        lancamentoDAO.atualizarStatus(lancamento.getId(), "PENDENTE", null);
        carregarLancamentos();
        
        showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Lançamento marcado para reenvio", 
                "O lançamento foi marcado como PENDENTE e será reenviado na próxima integração.");
    }
    
    @FXML
    private void onEnviarLancamentos(ActionEvent event) {
        List<LancamentoContabil> lancamentosSelecionados = tblLancamentos.getSelectionModel().getSelectedItems();
        if (lancamentosSelecionados.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Aviso", "Nenhum lançamento selecionado", 
                    "Selecione pelo menos um lançamento para enviar.");
            return;
        }
        
        try {
            // Criar serviço de integração
            IntegrationService service;
            if ("api".equalsIgnoreCase(config.getIntegrationType())) {
                service = new ApiIntegrationService(config);
            } else {
                service = new FileIntegrationService(config);
            }
            
            // Enviar lançamentos
            boolean success = service.enviarLote(lancamentosSelecionados);
            
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Lançamentos enviados", 
                        "Os lançamentos foram enviados com sucesso.");
            } else {
                showAlert(Alert.AlertType.WARNING, "Aviso", "Erro ao enviar lançamentos", 
                        "Ocorreram erros ao enviar os lançamentos. Verifique os logs para mais detalhes.");
            }
            
            // Recarregar lançamentos e logs
            carregarLancamentos();
            carregarLogs();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao enviar lançamentos", e);
            showAlert(Alert.AlertType.ERROR, "Erro", "Erro ao enviar lançamentos", e.getMessage());
        }
    }
    
    @FXML
    private void onAtualizarLancamentos(ActionEvent event) {
        carregarLancamentos();
    }
    
    @FXML
    private void onAtualizarLogs(ActionEvent event) {
        carregarLogs();
    }
    
    @FXML
    private void onTestarConexao(ActionEvent event) {
        try {
            // Criar configuração temporária com os valores do formulário
            IntegrationConfig tempConfig = new IntegrationConfig();
            tempConfig.setSqlServerHost(txtSqlServerHost.getText());
            tempConfig.setSqlServerPort(Integer.parseInt(txtSqlServerPort.getText()));
            tempConfig.setSqlServerDatabase(txtSqlServerDatabase.getText());
            tempConfig.setSqlServerUser(txtSqlServerUser.getText());
            tempConfig.setSqlServerPassword(txtSqlServerPassword.getText());
            tempConfig.setSqlServerSchema(txtSqlServerSchema.getText());
            
            // Testar conexão
            DatabaseManager dbManager = new DatabaseManager(tempConfig);
            dbManager.testConnection();
            
            showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Conexão estabelecida", 
                    "A conexão com o SQL Server foi estabelecida com sucesso.");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao testar conexão", e);
            showAlert(Alert.AlertType.ERROR, "Erro", "Falha na conexão", 
                    "Não foi possível conectar ao SQL Server: " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erro", "Valor inválido", 
                    "A porta deve ser um número inteiro válido.");
        }
    }
    
    @FXML
    private void onSalvarConfig(ActionEvent event) {
        try {
            // Atualizar configuração com os valores do formulário
            config.setSqlServerHost(txtSqlServerHost.getText());
            config.setSqlServerPort(Integer.parseInt(txtSqlServerPort.getText()));
            config.setSqlServerDatabase(txtSqlServerDatabase.getText());
            config.setSqlServerUser(txtSqlServerUser.getText());
            config.setSqlServerPassword(txtSqlServerPassword.getText());
            config.setSqlServerSchema(txtSqlServerSchema.getText());
            
            config.setIntegrationType(cmbIntegrationType.getValue());
            config.setApiUrl(txtApiUrl.getText());
            config.setApiToken(txtApiToken.getText());
            config.setOutputDirectory(txtOutputDirectory.getText());
            config.setFileFormat(cmbFileFormat.getValue());
            
            config.setUseFtp(chkUseFtp.isSelected());
            config.setFtpHost(txtFtpHost.getText());
            config.setFtpUser(txtFtpUser.getText());
            config.setFtpPassword(txtFtpPassword.getText());
            
            config.setBatchSize(Integer.parseInt(txtBatchSize.getText()));
            config.setRetryAttempts(Integer.parseInt(txtRetryAttempts.getText()));
            
            config.setScheduleExpression(txtScheduleExpression.getText());
            config.setSchedulingEnabled(chkSchedulingEnabled.isSelected());
            
            // Salvar configuração
            config.save("config.properties");
            
            // Atualizar agendamento
            if (scheduler != null) {
                scheduler.updateSchedule(config.getScheduleExpression());
                if (config.isSchedulingEnabled()) {
                    scheduler.start();
                } else {
                    scheduler.stop();
                }
            }
            
            showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Configurações salvas", 
                    "As configurações foram salvas com sucesso.");
            
            // Atualizar status do agendamento
            atualizarStatusAgendamento();
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao salvar configurações", e);
            showAlert(Alert.AlertType.ERROR, "Erro", "Erro ao salvar configurações", e.getMessage());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erro", "Valor inválido", 
                    "Verifique os campos numéricos (porta, tamanho do lote, tentativas).");
        }
    }
    
    @FXML
    private void onIniciarAgendamento(ActionEvent event) {
        if (scheduler != null) {
            scheduler.start();
            atualizarStatusAgendamento();
        }
    }
    
    @FXML
    private void onPararAgendamento(ActionEvent event) {
        if (scheduler != null) {
            scheduler.stop();
            atualizarStatusAgendamento();
        }
    }
    
    @FXML
    private void onExecutarAgora(ActionEvent event) {
        if (scheduler != null) {
            scheduler.executeNow();
            showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Integração iniciada", 
                    "A integração foi iniciada manualmente.");
            
            // Agendar atualização dos dados após alguns segundos
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> {
                        carregarLancamentos();
                        carregarLogs();
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    private void atualizarStatusAgendamento() {
        if (scheduler != null) {
            boolean ativo = config.isSchedulingEnabled();
            lblStatusAgendamento.setText(ativo ? "Ativo" : "Inativo");
            lblStatusAgendamento.setStyle(ativo ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
            
            lblProximaExecucao.setText(ativo ? "Próxima execução conforme expressão: " + config.getScheduleExpression() : "Agendamento desativado");
            
            btnIniciarAgendamento.setDisable(ativo);
            btnPararAgendamento.setDisable(!ativo);
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
