package com.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.DisposableBean;

import org.springframework.stereotype.Service;

import lombok.Synchronized;

/**
 * Classe utilitária para printar os resultados dos comandos em arquivos
 */
@Service
public class PrintResult implements DisposableBean{

    final String outputFileName = "spring-result.log";
    final PrintWriter writer;

    public PrintResult() throws IOException {        
        this.writer = new PrintWriter(new FileWriter(outputFileName));
    }
    
    @Synchronized
    public void printf(String message, Object... args) {                
        this.writer.printf(message, args);
    }

    public void clear() {
        // Limpa o conteúdo do arquivo
        try {
            FileWriter fileWriter = new FileWriter(outputFileName);
            fileWriter.write("");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String toString() {        
        this.writer.flush();
        try {
            byte[] encodedBytes = Files.readAllBytes(Paths.get(outputFileName));
            return new String(encodedBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public void destroy() throws Exception {
       this.writer.close();
    }
}
