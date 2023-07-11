package com.example;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;

public class PessoaStatements {

    private final PreparedStatement preparedStatement;
    private final String stringStatement;

    public PessoaStatements(CqlSession session, String keyspace) {
        this.stringStatement = "INSERT INTO " + keyspace
                + ".pessoa (id, nome, sobrenome, email, data_de_nascimento) VALUES (%s, '%s', '%s', '%s', '%s')";
        this.preparedStatement = session.prepare("INSERT INTO " + keyspace
                + ".pessoa (id, nome, sobrenome, email, data_de_nascimento) VALUES (?, ?, ?, ?, ?)");
    }

    public BoundStatement boundStatement(Pessoa pessoa) {
        return preparedStatement.bind(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getSobrenome(),
                pessoa.getEmail(),
                pessoa.getDataNascimento());
    }

    public String stringStatement(Pessoa pessoa) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return String.format(
                stringStatement,
                pessoa.getId().toString(),
                pessoa.getNome(),
                pessoa.getSobrenome(),
                pessoa.getEmail(),
                pessoa.getDataNascimento().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

}
