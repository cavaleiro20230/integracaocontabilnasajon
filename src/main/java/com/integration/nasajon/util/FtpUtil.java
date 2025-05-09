package com.integration.nasajon.util;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FtpUtil {
    private static final Logger logger = Logger.getLogger(FtpUtil.class.getName());
    
    /**
     * Envia um arquivo para um servidor FTP
     * @param host Endereço do servidor FTP
     * @param user Usuário FTP
     * @param password Senha FTP
     * @param localFilePath Caminho local do arquivo a ser enviado
     * @param remoteFileName Nome do arquivo no servidor remoto
     * @return true se o envio foi bem-sucedido, false caso contrário
     */
    public static boolean enviarArquivo(String host, String user, String password, 
                                        String localFilePath, String remoteFileName) {
        FTPClient ftpClient = new FTPClient();
        
        try {
            ftpClient.connect(host);
            int replyCode = ftpClient.getReplyCode();
            
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                logger.warning("Conexão FTP recusada. Código de resposta: " + replyCode);
                return false;
            }
            
            boolean success = ftpClient.login(user, password);
            if (!success) {
                logger.warning("Falha no login FTP");
                return false;
            }
            
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            
            File localFile = new File(localFilePath);
            try (FileInputStream inputStream = new FileInputStream(localFile)) {
                success = ftpClient.storeFile(remoteFileName, inputStream);
                
                if (success) {
                    logger.info("Arquivo enviado com sucesso para o servidor FTP: " + remoteFileName);
                } else {
                    logger.warning("Falha ao enviar arquivo para o servidor FTP");
                }
                
                return success;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro durante operação FTP", e);
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erro ao desconectar do servidor FTP", e);
            }
        }
    }
}
