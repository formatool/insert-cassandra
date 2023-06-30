CREATE KEYSPACE IF NOT EXISTS keyspace01
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};
USE keyspace01;

CREATE TABLE IF NOT EXISTS pessoa (
    id UUID,
    nome text,
    sobrenome text,
    email text,
    data_de_nascimento date,
    PRIMARY KEY ((nome, sobrenome), id)
);