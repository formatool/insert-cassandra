package com.example;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CassandraSession implements DisposableBean {

    private CqlSession cqlSession;

    @Bean
    CqlSession cqlSession() throws IOException {
        log.info("Manually building connection with Cassandra.");

        // Constrói a sessão do CqlSession com as configurações mescladas
        this.cqlSession = CqlSession.builder()
                .withConfigLoader(this.configLoader())
                .build();

        return this.cqlSession;
    }

    private DriverConfigLoader configLoader() throws IOException {
        Config internalConfig = ConfigFactory.load("application.conf");
        log.warn("Internal configuration:\n{}", internalConfig.root().render(ConfigRenderOptions.concise()));
        Resource[] externalResources = getExternalResources();
        log.warn("External configurations application.conf found: {}.", externalResources.length);
        Config externalConfig = ConfigFactory.empty();
        for (Resource resource : externalResources) {
            log.warn("External configuration file: {}", resource.getFile().getAbsolutePath());
            externalConfig = externalConfig.withFallback(ConfigFactory.parseURL(resource.getURL()));            
        }        
        Config mergedConfig = externalConfig.withFallback(internalConfig);
        log.warn("Merged configuration:\n{}", externalConfig.root().render(ConfigRenderOptions.concise()));
        return DriverConfigLoader.fromString(mergedConfig.root().render(ConfigRenderOptions.concise()));
    }

    private Resource[] getExternalResources() {
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        String cassandraConfigLoadFile = System.getProperty("cassandraConfigLoadFile");
        try {
            Resource[] resources = {};
            if (cassandraConfigLoadFile != null) {
                resources = resourceResolver.getResources("file:" + cassandraConfigLoadFile);
            }
            Resource[] fileResources = resourceResolver.getResources("file:./application.conf");
            Resource[] fileJsonResources = resourceResolver.getResources("file:./application.json");
            Resource[] filePropertiesResources = resourceResolver.getResources("file:./application.properties");
            Resource[] fileReferenceResources = resourceResolver.getResources("file:./reference.conf");

            return Stream.of(resources, fileResources, fileJsonResources, filePropertiesResources, fileReferenceResources)
                    .flatMap(Arrays::stream)
                    .filter(Resource::exists)
                    .toArray(Resource[]::new);
        } catch (IOException e) {
            log.error("Erro ao buscar configurações do cassandra.", e);
            return new Resource[] {};
        }
    }

    @Override
    public void destroy() {
        log.info("Closing CqlSession");
        if (cqlSession != null && !cqlSession.isClosed()) {
            log.info("Closing CqlSession");
            cqlSession.close();
        }
    }

}
