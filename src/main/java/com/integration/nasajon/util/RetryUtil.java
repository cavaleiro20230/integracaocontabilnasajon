package com.integration.nasajon.util;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetryUtil {
    private static final Logger logger = Logger.getLogger(RetryUtil.class.getName());
    
    /**
     * Executa uma operação com retry em caso de falha
     * @param operation Operação a ser executada
     * @param maxAttempts Número máximo de tentativas
     * @return Resultado da operação
     * @throws Exception Se todas as tentativas falharem
     */
    public static <T> T executeWithRetry(Callable<T> operation, int maxAttempts) throws Exception {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxAttempts) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                if (attempts < maxAttempts) {
                    long waitTime = calculateWaitTime(attempts);
                    logger.log(Level.WARNING, "Tentativa {0} falhou. Aguardando {1}ms antes de tentar novamente.", 
                            new Object[]{attempts, waitTime});
                    
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrompido durante espera para retry", ie);
                    }
                }
            }
        }
        
        logger.severe("Todas as tentativas falharam após " + maxAttempts + " tentativas");
        throw lastException;
    }
    
    /**
     * Calcula o tempo de espera entre tentativas usando backoff exponencial
     * @param attempt Número da tentativa atual
     * @return Tempo de espera em milissegundos
     */
    private static long calculateWaitTime(int attempt) {
        // Backoff exponencial com jitter
        long baseWaitTime = 1000; // 1 segundo
        long maxWaitTime = 30000; // 30 segundos
        
        double exponentialFactor = Math.pow(2, attempt - 1);
        long waitTime = (long) (baseWaitTime * exponentialFactor);
        
        // Adicionar jitter (variação aleatória) para evitar sincronização
        double jitterFactor = 0.2; // 20% de variação
        long jitter = (long) (waitTime * jitterFactor * Math.random());
        waitTime = waitTime + jitter;
        
        return Math.min(waitTime, maxWaitTime);
    }
}
