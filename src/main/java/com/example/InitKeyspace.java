package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.cassandra.core.cql.session.init.ResourceKeyspacePopulator;
import org.springframework.stereotype.Service;
import com.datastax.oss.driver.api.core.CqlSession;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InitKeyspace {

    @Autowired
    private CqlSession session;

    @PostConstruct
    public void init() {
        printInfo();
        populate();
        printTablesInfo();
    }

    private void printInfo() {
        try {
            /**
             * DESCRIBE KEYSPACE:
             * https://docs.datastax.com/en/dse/5.1/cql/cql/cql_reference/cqlsh_commands/cqlshDescribeKeyspace.html
             */
            log.info("Keyspaces in the Cassandra instance: {} ",
                    session.execute("DESC KEYSPACES").map(r -> r.getString(0)).all());

            session.getKeyspace().ifPresent(k -> log.info("Current Keyspace: {} ",
                    session.execute("DESCRIBE KEYSPACE").one().getString(0)));

            /**
             * DataStax Java Driver - Reference configuration:
             * https://docs.datastax.com/en/developer/java-driver/4.9/manual/core/configuration/reference/
             */
            log.info("Cassandra Profiles: {} ", 
                        session.getContext().getConfig()
                                .getProfiles().keySet());
                                
            
            log.info("Cassandra DefaultProfile Configs: {} ", 
                        session.getContext().getConfig()
                                .getDefaultProfile().entrySet()
                                .stream()
                                .map(e->e.getKey()+'='+e.getValue())
                                .toList());
        } catch (Exception e) {
            log.error("An error occurred in Cassandra: {}", e.getMessage());
        }
    }

    @Autowired
    private ResourceLoader resourceLoader;

    private void populate() {
        var resname = "classpath:schema.cql";
        var res = resourceLoader.getResource(resname);
        try {
            if (res.isReadable()) {
                var pop = new ResourceKeyspacePopulator(true, true, null, res);
                pop.populate(session);
            }
        } catch (Exception e) {
            log.error("Error initializing keyspace", e);
        }
    }

    private void printTablesInfo() {
        try {
            session.getKeyspace()
                .ifPresent(k -> 
                    log.info("Tables on Cassandra Keyspace: {} ",
                          session.execute("DESCRIBE TABLES")
                                .map(r -> r.getString(2)).all())
                          );
        } catch (Exception e) {
            log.error("An error occurred in Cassandra: {}", e.getMessage());
        }
    }

}