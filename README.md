# insert-cassandra

Projeto para realizar carga no Cassandra realizando insert de dados.

## Requisitos

* Java 17
* Maven (Para build)

## Executar

Execute o projeto:

```shell
mvn spring-boot:run
```

ou

```shell
mvn package
java -jar ./target/insert-cassandra-0.0.1-SNAPSHOT.jar
```

Ao iniciar ele vai abrir um JShell onde poderá ser executado o comando `help` que vai mostrar os próximos comandos.

## Configurar

Ao iniciar o programa tenta conectar em um Apache Cassandra no endereço `127.0.0.1:9042`.
A configuração inicial é 

```conf
datastax-java-driver {
  basic.contact-points = [ "127.0.0.1:9042" ]
  basic.request.timeout=10s
  basic.load-balancing-policy {
    local-datacenter = datacenter1
  }
  advanced {
    connection.connect-timeout=10s
    connection.init-query-timeout=10s
  }
}
```

Você pode mudar a configuração, criando um arquivo `application.conf` na pasta local. Informações de como realizar essa configuração voce obtem em :

- DataStax Java Driver - Configuration: https://docs.datastax.com/en/developer/java-driver/4.9/manual/core/configuration/
- DataStax Java Driver - Reference configuration: https://docs.datastax.com/en/developer/java-driver/4.9/manual/core/configuration/reference/

Outra formas de mudar a configuração:

### System Properties:

```bash
java -jar -Ddatastax-java-driver.basic.request.timeout=12s ./target/insert-cassandra-0.0.1-SNAPSHOT.jar`
```

### Informando outro arquivo conf:

```bash
java -jar -DcassandraConfigLoadFile='./application.conf.example' ./target/insert-cassandra-0.0.1-SNAPSHOT.jar`
```
