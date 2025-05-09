package com.integration.nasajon.scheduler;

import com.integration.nasajon.config.IntegrationConfig;
import com.integration.nasajon.dao.IntegrationLogDAO;
import com.integration.nasajon.model.IntegrationLog;
import com.integration.nasajon.service.ApiIntegrationService;
import com.integration.nasajon.service.FileIntegrationService;
import com.integration.nasajon.service.IntegrationService;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public class IntegrationScheduler {
    private static final Logger logger = Logger.getLogger(IntegrationScheduler.class.getName());
    
    private final IntegrationConfig config;
    private Scheduler scheduler;
    
    public IntegrationScheduler(IntegrationConfig config) {
        this.config = config;
    }
    
    public void initialize() {
        try {
            // Criar scheduler
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
            
            // Definir job
            JobDetail job = JobBuilder.newJob(IntegrationJob.class)
                    .withIdentity("integrationJob", "nasajonGroup")
                    .build();
            
            // Passar configuração para o job
            job.getJobDataMap().put("config", config);
            
            // Definir trigger com expressão cron
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("integrationTrigger", "nasajonGroup")
                    .withSchedule(CronScheduleBuilder.cronSchedule(config.getScheduleExpression()))
                    .build();
            
            // Agendar job
            scheduler.scheduleJob(job, trigger);
            
            // Iniciar scheduler se habilitado
            if (config.isSchedulingEnabled()) {
                scheduler.start();
                logger.info("Agendador de integração iniciado com expressão: " + config.getScheduleExpression());
                
                IntegrationLogDAO logDAO = new IntegrationLogDAO(config);
                logDAO.salvar(new IntegrationLog("INFO", "Agendador de integração iniciado", 
                        "Expressão cron: " + config.getScheduleExpression()));
            } else {
                logger.info("Agendador de integração está desabilitado nas configurações");
            }
            
        } catch (SchedulerException e) {
            logger.log(Level.SEVERE, "Erro ao inicializar o agendador de integração", e);
        }
    }
    
    public void start() {
        try {
            if (scheduler != null && !scheduler.isStarted()) {
                scheduler.start();
                logger.info("Agendador de integração iniciado");
                
                // Atualizar configuração
                config.setSchedulingEnabled(true);
                
                IntegrationLogDAO logDAO = new IntegrationLogDAO(config);
                logDAO.salvar(new IntegrationLog("INFO", "Agendador de integração iniciado", 
                        "Expressão cron: " + config.getScheduleExpression()));
            }
        } catch (SchedulerException e) {
            logger.log(Level.SEVERE, "Erro ao iniciar o agendador de integração", e);
        }
    }
    
    public void stop() {
        try {
            if (scheduler != null && scheduler.isStarted()) {
                scheduler.standby();
                logger.info("Agendador de integração pausado");
                
                // Atualizar configuração
                config.setSchedulingEnabled(false);
                
                IntegrationLogDAO logDAO = new IntegrationLogDAO(config);
                logDAO.salvar(new IntegrationLog("INFO", "Agendador de integração pausado"));
            }
        } catch (SchedulerException e) {
            logger.log(Level.SEVERE, "Erro ao pausar o agendador de integração", e);
        }
    }
    
    public void updateSchedule(String cronExpression) {
        try {
            // Verificar se a expressão cron é válida
            CronScheduleBuilder.cronSchedule(cronExpression);
            
            // Atualizar trigger
            TriggerKey triggerKey = TriggerKey.triggerKey("integrationTrigger", "nasajonGroup");
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .build();
            
            scheduler.rescheduleJob(triggerKey, trigger);
            
            // Atualizar configuração
            config.setScheduleExpression(cronExpression);
            
            logger.info("Agendamento atualizado para: " + cronExpression);
            
            IntegrationLogDAO logDAO = new IntegrationLogDAO(config);
            logDAO.salvar(new IntegrationLog("INFO", "Agendamento atualizado", 
                    "Nova expressão cron: " + cronExpression));
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao atualizar o agendamento", e);
        }
    }
    
    public void executeNow() {
        try {
            JobKey jobKey = JobKey.jobKey("integrationJob", "nasajonGroup");
            scheduler.triggerJob(jobKey);
            
            logger.info("Execução manual da integração iniciada");
            
            IntegrationLogDAO logDAO = new IntegrationLogDAO(config);
            logDAO.salvar(new IntegrationLog("INFO", "Execução manual da integração iniciada"));
            
        } catch (SchedulerException e) {
            logger.log(Level.SEVERE, "Erro ao executar integração manualmente", e);
        }
    }
    
    public static class IntegrationJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                // Obter configuração
                IntegrationConfig config = (IntegrationConfig) context.getJobDetail().getJobDataMap().get("config");
                
                // Criar serviço de integração
                IntegrationService service;
                if ("api".equalsIgnoreCase(config.getIntegrationType())) {
                    service = new ApiIntegrationService(config);
                } else {
                    service = new FileIntegrationService(config);
                }
                
                // Executar integração
                int processados = service.executarIntegracao();
                
                logger.info("Integração agendada executada. Lançamentos processados: " + processados);
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Erro ao executar job de integração", e);
                throw new JobExecutionException(e);
            }
        }
    }
}
