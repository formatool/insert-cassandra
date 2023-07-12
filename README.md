# insert-cassandra

Projeto para realizar testes de INSERT no Cassandra.

Demonstra como:
  - Executar um cluster Cassandra com docker
  - Como mudar e conferir configurações do driver de conexão Cassandra
  - Inicializar um keyspaces Cassandra ao inicia aplicação
  - Como obter informações de execução
  - Uso de profiles no driver
  - Retry com Downgrade de Consistency Level com a abordagem padrão e customizada

## Requisitos

* Java 17
* Docker (para executar um cluster de testes)

## Devtools: Subir um cluster Cassandra

Estão disponíveis 2 docker-compose para realizar testes. Um com apenas um nó e outro MultiDC. Para executar faça:

```bash
cd ./devtools/docker/compose-cluster1/
docker compose up 
```

ou

```bash
cd ./devtools/docker/compose-multidc/
docker compose up 
```

para remover o cassandra completamente, execute `docker compose down -v` na pasta correpondente.

## Executar

Execute o projeto:

```shell
./mvnw spring-boot:run
```

ou

```shell
./mvnw package
java -jar ./target/insert-cassandra-0.0.1-SNAPSHOT.jar
```

Ao iniciar ele vai abrir um JShell onde poderá ser executado o comando `help` que vai mostrar os próximos comandos.

## Configurar

O programa utiliza por padrão a configuração do arquivo [src/main/resources/application.conf](./src/main/resources/application.conf). Essa configuração pode ser extendida criando outro aqui application.conf na pasta raiz. Informações de como realizar essa configuração voce obtem em :

- DataStax Java Driver - Configuration: https://docs.datastax.com/en/developer/java-driver/4.9/manual/core/configuration/
- DataStax Java Driver - Reference configuration: https://docs.datastax.com/en/developer/java-driver/4.9/manual/core/configuration/reference/

Outra formas de mudar a configuração:

### System Properties:

```bash
./mvnw package
java -jar -Ddatastax-java-driver.basic.request.timeout=12s ./target/insert-cassandra-0.0.1-SNAPSHOT.jar`
```

ou

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Ddatastax-java-driver.basic.request.timeout=12s"`
```

### Informando outro arquivo conf:

```bash
./mvnw package
java -jar -DcassandraConfigLoadFile='./application.conf.example' ./target/insert-cassandra-0.0.1-SNAPSHOT.jar`
```
ou

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-DcassandraConfigLoadFile='./application.conf.example'"`
```

## Comandos JQuery

Ao iniciar o programa, será apresentado o prompt `shell:>` onde o usuário poderá interagir. Digite `help` para ver as opções.
