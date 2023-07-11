package com.example;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.github.javafaker.Faker;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Command
@Service
@Slf4j
public class Commands {

    private final CqlSession session;
    private final PrintResultSet printResultSet;
    private String keyspace = "keyspace_dcs";

    @PostConstruct
    public void init() {
        System.out.println("===========================");
        System.out.println("===========================\n");
        System.out.println("Olá. Bem-vindo.\n\nEsse programa abre uma sessão com o Apache Cassandra.");
        System.out.println(
                "Você pode mudar as configurações do driver de conexão com o Cassandra, ao iniciar esse programa, com as seguintes opções:");
        System.out.println("  adicionando um arquivo application.conf junto do jar ou");
        System.out.println("  informando o arquivo de configuração no parâmetro -DcassandraConfigLoadFile=");
        System.out.println("  usando enviando a configuração por parametro usando -Ddatastax-java-driver.????=???");
        System.out.println("\n💡digite help para ver os comandos possíveis.\n");
        profiles();
        System.out.println("");
    }

    public Commands(CqlSession cqlSession, PrintResultSet printResultSet) {
        this.session = cqlSession;
        this.printResultSet = printResultSet;
    }

    @Command(alias = "exit")
    public void quit() {
        log.info("Terminating");
        System.exit(0);
    }

    @Command(description = "Executa um statement CQL")
    public void query(
            @Option(required = true, defaultValue = "DESCRIBE TABLES;", description = "Statement CQL") String query,
            @Option(required = true, defaultValue = "T", description = "tipo de impressão. T=Table F=Form") char format,
            @Option(required = false, description = "Profile de execução") String profile) {

        SimpleStatement simpleStatement = SimpleStatement.builder(query).build();
        if (log.isDebugEnabled()) {
            simpleStatement = simpleStatement.setTracing(true);
        }
        if (null != profile) {
            simpleStatement = simpleStatement.setExecutionProfileName(profile);
        }
        ResultSet rs = session.execute(simpleStatement);
        if (format == 'T') {
            printResultSet.printTableFormat(rs);
        } else {
            printResultSet.printFormFormat(rs);
        }
        if (log.isDebugEnabled()) {
            System.out.printf(
                    "#--^--^--^--%n#Execution Infos.:%n# - Coordinator: %s (%s)%n# - ConsistencyLevel: %s%n%n",
                    rs.getExecutionInfo().getCoordinator().getEndPoint().resolve().toString(),
                    rs.getExecutionInfo().getCoordinator().getDatacenter(),
                    rs.getExecutionInfo().getQueryTrace().getParameters().get("consistency_level"));
        }
    }

    @Command(command = "insert:pessoa", description = "Faz um Insert na tabela Pessoa")
    public void insertPessoa(
            @Option(required = true, defaultValue = "1", description = "Quantidade de pessoas") int qtd,
            @Option(required = true, defaultValue = "500", description = "Tempo entre inserts em milisegundos") long pause,
            @Option(required = false, description = "Profile de execução") String profile)
            throws InterruptedException {
        var statements = new PessoaStatements(session, keyspace);
        LocalDateTime start = LocalDateTime.now();
        int erros = 0;
        try {
            for (int i = 0; i < qtd; i++) {
                var pessoa = Pessoa.fake();
                try {
                    executeBoundStatement(profile,
                            statements.boundStatement(pessoa),
                            statements.stringStatement(pessoa),
                            String.valueOf(i + 1));
                } catch (Exception e) {
                    erros++;
                }
                if (pause > 0 && i < qtd - 1) {
                    Thread.sleep(pause);
                }
            }
        } finally {
            LocalDateTime end = LocalDateTime.now();
            System.out.printf(
                    "#======%n# Qtd.Total: %s%n# Qtd.Inserts: %s%n# Qtd.Erros: %s%n# Início: %s%n# Fim: %s%n# Tempo: %s%n",
                    qtd, qtd - erros, erros, start.toLocalTime(), end.toLocalTime(), Duration.between(start, end));
        }
    }

    private ResultSet executeBoundStatement(@Nullable String profile, BoundStatement boundStatement,
            String printableStatement,
            String executionInformation) {
        if (log.isDebugEnabled()) {
            boundStatement = boundStatement.setTracing(true);
        }
        if (null != profile) {
            boundStatement = boundStatement.setExecutionProfileName(profile);
        }
        System.out.println(printableStatement);
        try {
            ResultSet rs = session.execute(boundStatement);
            if (rs != null && log.isDebugEnabled()) {
                System.out.printf(
                        "#-(%s)-^--^--^--%n#Execution Infos.:%n#  Coordinator: %s (%s)%n#  Consistency Level:%s%n%n",
                        executionInformation,
                        rs.getExecutionInfo().getCoordinator().getEndPoint().resolve().toString(),
                        rs.getExecutionInfo().getCoordinator().getDatacenter(),
                        rs.getExecutionInfo().getQueryTrace().getParameters().get("consistency_level"));
            }
            return rs;
        } catch (Exception e) {
            System.out.printf(
                    "#-(%s)-^--^--^--%n#Exception:%n#  %s%n%n",
                    executionInformation, e.getMessage());
            throw e;
        }
    }

    @Command(description="Lista os profiles configurados no driver.")
    public void profiles() {
        System.out.println("Profiles do drivers Cassandra: " + session.getContext().getConfig().getProfiles().keySet());
    }

    @Command(description="Lista as configurações do driver.")
    public void config(@Option(required = false, description = "filtra as configurações") String filtro) {
        session.getContext().getConfig()
                .getDefaultProfile().entrySet().stream()
                .map(e -> e.getKey() + '=' + e.getValue())
                .filter(e -> filtro == null || e.contains(filtro))
                .toList()
                .forEach(System.out::println);
    }

    @Command(description="Lista as configurações do profile.")
    public void configProfile(@Option(required = true) String profile, @Option(required = false) String filtro) {
        session.getContext().getConfig()
                .getProfile(profile).entrySet().stream()
                .map(e -> e.getKey() + '=' + e.getValue())
                .filter(e -> filtro == null || e.contains(filtro))
                .toList()
                .forEach(System.out::println);
    }

    @Command(description="Informa o status do cluster.")
    public void status() {
        System.out.printf("ClusterName: %s\n", session.getMetadata().getClusterName());
        Map<UUID, Node> nodes = session.getMetadata().getNodes();
        System.out.println("Nodes in the cluster:");
        for (Node node : nodes.values()) {
            System.out.printf(
                    "(DC: %s) %s is %s and %s (%d connections) Broadcast: %s ListenAddress:%s%n",
                    node.getDatacenter(),
                    node.getEndPoint().resolve().toString(),
                    node.getState(),
                    node.getDistance(),
                    node.getOpenConnections(),
                    node.getBroadcastAddress().map(a -> a.getHostName() + ":" + a.getPort()).orElse(""),
                    node.getListenAddress().map(a -> a.getHostName() + ":" + a.getPort()).orElse(""));
        }
    }

}
