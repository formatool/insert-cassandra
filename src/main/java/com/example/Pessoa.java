package com.example;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.github.javafaker.Faker;

/**
 * Uma entidade de negócio para exemplos
 */
public class Pessoa {
    private UUID id;
    private String nome;
    private String sobrenome;
    private String email;
    private LocalDate dataNascimento;

    public static Pessoa fake() {
        Faker faker = new Faker();
        var obj = new Pessoa();
        obj.id = Uuids.random();
        obj.nome = faker.name().firstName();
        obj.sobrenome = faker.name().lastName();
        obj.email = faker.internet().emailAddress();
        var data = faker.date().birthday();
        obj.dataNascimento = data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return obj;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

}
