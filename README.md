# MeshChat

## Descrição

O **MeshChat** é um chat distribuído desenvolvido em Java utilizando uma arquitetura **Peer-to-Peer (P2P)**.

Cada participante atua simultaneamente como cliente e servidor, podendo aceitar conexões de outros pares e iniciar conexões com participantes conhecidos. Não há um servidor central.

O sistema permite o envio de mensagens públicas e privadas, além de comandos para listar participantes e encerrar a aplicação.

## Configurações

A aplicação pode ser configurada utilizando um arquivo `.properties`.

Exemplo:

```properties id="9rj3kh"
nickname=Joao
host=localhost
port=5001
peers=localhost:5002,localhost:5003
```

| Configuração | Descrição |
|---|---|
| `nickname` | Nome do participante |
| `host` | Endereço do nó |
| `port` | Porta utilizada para receber conexões |
| `peers` | Lista de participantes conhecidos no formato `host:porta` |

## Como executar

O projeto requer **Java 21 ou superior**.

Execute uma instância informando o arquivo de configuração:

```bash id="o0bxz3"
java com.meshchat.Main config-examples/joao.properties
```

Em outro terminal, execute outro participante:

```bash id="o2kk18"
java com.meshchat.Main config-examples/lucas.properties
```

Cada instância será iniciada na porta configurada e tentará se conectar aos participantes informados na propriedade `peers`.

## Protocolo de comunicação

A comunicação entre os participantes é realizada utilizando **sockets TCP**.

As mensagens utilizam um protocolo de framing com um prefixo contendo o tamanho da mensagem:

```text id="cs5o9r"
┌────────────────────┬─────────────────────────┐
│ Tamanho (4 bytes)  │        Payload          │
└────────────────────┴─────────────────────────┘
```

Os principais tipos de mensagens são:

- `JOIN` — entrada de um participante;
- `LEAVE` — saída de um participante;
- `CHAT` — mensagem pública;
- `PRIVATE` — mensagem privada;
- `HEARTBEAT` — manutenção da conexão.

## Comandos

### Mensagem pública

Basta digitar a mensagem:

```text id="3g2zce"
Olá, pessoal!
```

### Mensagem privada

```text id="nycfz1"
/msg <participante> <mensagem>
```

Exemplo:

```text id="pm93dr"
/msg Lucas Olá, Lucas!
```

### Listar participantes

```text id="f2bh7g"
/list
```

### Encerrar a aplicação

```text id="uv9fzq"
/quit
```
