package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.mustache.MustacheProperties.Reactive;
import org.springframework.data.cassandra.core.WriteResult;
import org.springframework.data.cassandra.core.mapping.Tuple;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Service;

import com.datastax.dse.driver.api.core.DseSession;
import com.datastax.dse.driver.api.core.cql.reactive.ReactiveResultSet;
import com.datastax.dse.driver.internal.core.cql.reactive.SimpleUnicastProcessor;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.EndPoint;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.github.javafaker.Faker;
import com.typesafe.config.ConfigFactory;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

/**
 * Define todos os comandos JShell da aplicação
 */
@Command
@Service
@Slf4j
public class Commands {

    private final CqlSession session;
    private final PrintResultSet printResultSet;
    private final PrintResult printResult;
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

    public Commands(CqlSession cqlSession, PrintResultSet printResultSet, PrintResult printResult) {
        this.session = cqlSession;
        this.printResultSet = printResultSet;
        this.printResult = printResult;
    }

    @Command(alias = "exit")
    public void quit() {
        log.info("Terminating");
        System.exit(0);
    }

    @Command(description = "Executa e imprime o statement CQL")
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

    @Command(command = "inserePessoa:sync", description = "Faz um Insert na tabela Pessoa utilizando CqlSession.execute")
    public void insertPessoa(
            @Option(required = true, defaultValue = "1", description = "Quantidade de pessoas") int qtd,
            @Option(required = true, defaultValue = "0", description = "Tempo entre inserts em milisegundos") long pause,
            @Option(required = false, description = "Profile de execução") String profile)
            throws InterruptedException {
        printResult.clear();
        printResult.printf("%n#---%n# Iniciando inserePessoa:sync Qtd: %s%n", qtd);
        var statements = new PessoaStatements(session, keyspace);
        LocalDateTime start = LocalDateTime.now();
        int erros = 0;
        try {
            for (int i = 0; i < qtd; i++) {
                var pessoa = Pessoa.fake();
                try {
                    executeBoundStatement(
                            configBoundStatement(statements.boundStatement(pessoa), profile),
                            statements.stringStatement(pessoa),
                            String.valueOf(i + 1),
                            (pause > 0 || qtd <= 10));
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

    @Command(command = "inserePessoa:async", description = "Faz um insert de várias Pessoa utilizando CqlSession.executeAsync")
    public void insertPessoaAsync(
            @Option(required = true, defaultValue = "1000", description = "Quantidade de pessoas") final int qtd,
            @Option(required = false, description = "Profile de execução") final String profile)
            throws InterruptedException, ExecutionException {
        printResult.clear();
        printf(true, "%n#---%n# Iniciando inserePessoa:async. Qtd: %s%n", qtd);
        var statements = new PessoaStatements(session, keyspace);
        LocalDateTime start = LocalDateTime.now();
        List<CompletableFuture<?>> pending = new ArrayList<>();
        AtomicInteger erroCount = new AtomicInteger(0);
        for (int i = 0; i < qtd; i++) {
            String executionInformation = String.valueOf(i);
            var pessoa = Pessoa.fake();
            var future = executeAsyncBoundStatement(
                    configBoundStatement(statements.boundStatement(pessoa), profile),
                    statements.stringStatement(pessoa),
                    executionInformation);
            future.exceptionally(e -> {
                erroCount.incrementAndGet();
                return null;
            });
            pending.add(future);
        }

        CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).get();

        LocalDateTime end = LocalDateTime.now();
        System.out.printf(
                "#======%n# Qtd.Total: %s%n# Qtd.Inserts: %s%n# Qtd.Erros: %s%n# Início: %s%n# Fim: %s%n# Tempo: %s%n# ThrottlerMaxConcurrency Default: %s%n",
                pending.size(), pending.size() - erroCount.get(), erroCount.get(), start.toLocalTime(),
                end.toLocalTime(), Duration.between(start, end),
                session.getContext().getConfig().getDefaultProfile()
                        .getInt(DefaultDriverOption.REQUEST_THROTTLER_MAX_CONCURRENT_REQUESTS));
    }

    @Command(command = "inserePessoa:reactive", description = "Faz um insert de várias Pessoa utilizando CqlSession.executeAsync")
    public void insertPessoaReactice(
            @Option(required = true, defaultValue = "1000", description = "Quantidade de pessoas") final int qtd,
            @Option(required = false, description = "Profile de execução") final String profile)
            throws InterruptedException, ExecutionException {
        printResult.clear();
        printf(true, "%n#---%n# Iniciando inserePessoa:reactive. Qtd: %s%n", qtd);
        var statements = new PessoaStatements(session, keyspace);
        LocalDateTime start = LocalDateTime.now();
        // https://docs.datastax.com/en/developer/java-driver/4.8/manual/core/reactive/
        long erros = Flux.range(1, qtd)
                .map((index) -> Tuples.of(String.valueOf(index), Pessoa.fake()))
                .map((t2) -> Tuples.of(t2.getT1(),
                        statements.boundStatement(t2.getT2()),
                        statements.stringStatement(t2.getT2())))
                .doOnNext((t3) -> printf(false, "%s%n", t3.getT3()))
                .flatMap(t -> Flux.from(session.executeReactive(t.getT2()))
                        // dummy cast, since result sets are always empty for write queries
                        .cast(Long.class)
                        // flow will always be empty, so '1' will be emitted for each query
                        .defaultIfEmpty(0L)
                        .doOnError(e -> System.err.printf("Statement failed: %s %s%n", t.getT3(), e.getMessage()))
                        .onErrorReturn(1L))
                .reduce(0L, Long::sum)
                .block();

        LocalDateTime end = LocalDateTime.now();
        System.out.printf(
                "#======%n# Qtd.Total: %s%n# Qtd.Inserts: %s%n# Qtd.Erros: %s%n# Início: %s%n# Fim: %s%n# Tempo: %s%n# ThrottlerMaxConcurrency Default: %s%n",
                qtd, qtd - erros, erros, start.toLocalTime(), end.toLocalTime(),
                Duration.between(start, end),
                session.getContext().getConfig().getDefaultProfile()
                        .getInt(DefaultDriverOption.REQUEST_THROTTLER_MAX_CONCURRENT_REQUESTS));
    }

    private BoundStatement configBoundStatement(BoundStatement boundStatement, @Nullable final String profile) {
        if (log.isDebugEnabled()) {
            boundStatement = boundStatement.setTracing(true);
        }
        if (null != profile) {
            boundStatement = boundStatement.setExecutionProfileName(profile);
        }
        return boundStatement;
    }

    private ResultSet executeBoundStatement(BoundStatement boundStatement,
            String printableStatement,
            String executionInformation,
            boolean showProgress) {
        try {
            ResultSet rs = session.execute(boundStatement);
            if (rs != null && log.isDebugEnabled()) {
                printf(showProgress,
                        "%s%n#-(%s)-^--^--^--%n#Execution Infos.:%n#  Coordinator: %s (%s)%n#  Consistency Level:%s%n%n",
                        printableStatement,
                        executionInformation,
                        rs.getExecutionInfo().getCoordinator().getEndPoint().resolve().toString(),
                        rs.getExecutionInfo().getCoordinator().getDatacenter(),
                        rs.getExecutionInfo().getQueryTrace().getParameters().get("consistency_level"));
            }
            return rs;
        } catch (Exception e) {
            printf(showProgress,
                    "%s%n#-(%s)-^--^--^--%n#Exception:%n#  %s%n%n",
                    printableStatement,
                    executionInformation, e.getClass().getSimpleName() + " - " + e.getMessage());
            throw e;
        }
    }

    private void printf(boolean showProgress, String message, Object... args) {
        if (showProgress) {
            System.out.printf(message, args);
        }
        if (showProgress || log.isDebugEnabled()) {
            printResult.printf(message, args);
        }
    }

    private CompletableFuture<AsyncResultSet> executeAsyncBoundStatement(BoundStatement boundStatement,
            String printableStatement,
            String executionInformation) {
        var completionStage = session.executeAsync(boundStatement)
                .thenApplyAsync(rs -> {
                    ExecutionInfo executionInfo = rs.getExecutionInfo();
                    printResult.printf(
                            "%s%n#-(%s)-^--^--^--%n#Execution Infos.:%n#  Coordinator: %s (%s)%n#  Consistency Level:%s%n%n",
                            printableStatement,
                            executionInformation,
                            executionInfo.getCoordinator().getEndPoint().resolve().toString(),
                            executionInfo.getCoordinator().getDatacenter(),
                            executionInfo.getQueryTrace().getParameters().get("consistency_level"));
                    return rs;
                })
                .exceptionally(e -> {
                    printResult.printf(
                            "%s%n#-(%s)-^--^--^--%n#Exception:%n#  %s%n%n%n",
                            printableStatement,
                            executionInformation, e.getClass().getSimpleName() + " - " + e.getMessage());
                    return null;
                });
        return completionStage.toCompletableFuture();
    }

    @Command(description = "Lista os profiles configurados no driver.")
    public void profiles() {
        System.out.println("Profiles do drivers Cassandra: " + session.getContext().getConfig().getProfiles().keySet());
    }

    @Command(description = "Lista as configurações do driver.")
    public void config(@Option(required = false, description = "filtra as configurações") String filtro) {
        session.getContext().getConfig()
                .getDefaultProfile().entrySet().stream()
                .map(e -> e.getKey() + '=' + e.getValue())
                .filter(e -> filtro == null || e.contains(filtro))
                .toList()
                .forEach(System.out::println);
    }

    @Command(description = "Lista as configurações do profile.")
    public void configProfile(@Option(required = true) String profile, @Option(required = false) String filtro) {
        session.getContext().getConfig()
                .getProfile(profile).entrySet().stream()
                .map(e -> e.getKey() + '=' + e.getValue())
                .filter(e -> filtro == null || e.contains(filtro))
                .toList()
                .forEach(System.out::println);
    }

    @Command(description = "Informa o status do cluster.")
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

    @Command(description = "Imprime o resultado dos comandos executados anteriormente.")
    public void print() {
        System.out.println(printResult.toString());
    }

}
