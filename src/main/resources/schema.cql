CREATE KEYSPACE IF NOT EXISTS keyspace_simple
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '3'};
CREATE TABLE IF NOT EXISTS keyspace_simple.pessoa (
    id UUID,
    nome text,
    sobrenome text,
    email text,
    data_de_nascimento date,
    PRIMARY KEY ((nome, sobrenome), id)
);

CREATE KEYSPACE IF NOT EXISTS keyspace_dcs
WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1' : 3, 'DC2' : 3 };
CREATE TABLE IF NOT EXISTS keyspace_dcs.pessoa (
    id UUID,
    nome text,
    sobrenome text,
    email text,
    data_de_nascimento date,
    PRIMARY KEY ((nome, sobrenome), id)
);
