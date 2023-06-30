package com.example;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
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
            @Option(required = true, defaultValue = "T", description = "tipo de impressão. T=Table F=Form") char format) {
        ResultSet resultSet = session.execute(query);
        if (format == 'T') {
            printResultSet.printTableFormat(resultSet);
        } else {
            printResultSet.printFormFormat(resultSet);
        }
    }

    @Command(command = "insert:pessoa", description = "Faz um Insert na tabela Pessoa")
    public void insertPessoa(
            @Option(required = true, defaultValue = "1", description = "Quantidade de pessoas") int qtd,
            @Option(required = true, defaultValue = "500", description = "Tempo entre inserts em milisegundos") long pause) throws InterruptedException {
        Faker faker = new Faker();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        PreparedStatement preparedStatement = session
                    .prepare("INSERT INTO pessoa (id, nome, sobrenome, email, data_de_nascimento) " +
                            "VALUES (?, ?, ?, ?, ?)");
        for (int i = 0; i < qtd; i++) {            
            UUID id = Uuids.random();
            String nome = faker.name().firstName();
            String sobrenome = faker.name().lastName();
            String email = faker.internet().emailAddress();
            Date dataNascimento = faker.date().birthday();
            LocalDate localDate = dataNascimento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            BoundStatement boundStatement = preparedStatement.bind(id, nome, sobrenome, email, localDate);
            String insertQuery = String.format(
                    "INSERT INTO pessoa (id, nome, sobrenome, email, data_de_nascimento) " +
                            "VALUES (%s, '%s', '%s', '%s', '%s');",
                    id.toString(), nome, sobrenome, email, dateFormat.format(dataNascimento));
            System.out.println(insertQuery);
            session.execute(boundStatement);
            if (pause > 0 && i < qtd - 1) {
                Thread.sleep(pause);
            }
        }
    }

    @Command
    public void profiles() {
        System.out.println("Profiles do drivers Cassandra: " + session.getContext().getConfig().getProfiles().keySet());
    }

    @Command
    public void config(@Option(required = false) String filtro) {
        session.getContext().getConfig()
                .getDefaultProfile().entrySet().stream()
                .map(e -> e.getKey() + '=' + e.getValue())
                .filter(e -> filtro == null || e.contains(filtro))
                .toList()
                .forEach(System.out::println);
    }

}
