# UNO-LP2 - Jogo UNO com servidor multithread

Projeto academico da disciplina de Linguagem de Programacao II.

Arquitetura: servidor central com sockets TCP e uma thread por cliente.

## Equipe

- Felipe Jose de Medeiros Melo
- Felipe Cavalcanti Apolinario
- Joao Lucas Silva Acioli
- Jose Artur Soares Abreu
- Gabriel Rafa Martins Freire

## Objetivo

Implementar um UNO multiplayer em Java, exercitando:

- comunicacao cliente-servidor com sockets;
- sincronizacao de acesso ao estado da partida;
- controle de turno e consistencia de estado entre clientes.

## Estrutura

```text
uno-lp2/
    README.md
    pendencias.md
    src/
        client/
        server/
        shared/
        model/
```

## Como compilar

Na raiz do projeto:

```bash
find src -name "*.java" -print > sources.txt
javac -d out @sources.txt
```

Se houver incompatibilidade de versao entre compilador e runtime Java no ambiente:

```bash
javac --release 25 -d out @sources.txt
```

## Como executar

Servidor:

```bash
java -cp out server.GameServer <porta> <numero-de-jogadores>
```

Exemplo:

```bash
java -cp out server.GameServer 12345 2
```

Cliente:

```bash
java -cp out client.GameClient <host> <porta> <nome>
```

Exemplo:

```bash
java -cp out client.GameClient localhost 12345 Alice
java -cp out client.GameClient localhost 12345 Bob
```

## Comandos do cliente

- `COMPRAR`
- `JOGAR <COR>:<VALOR> [COR_DECLARADA]`
- `SAIR`

Exemplos:

- `JOGAR VERMELHO:CINCO`
- `JOGAR PRETO:CORINGA AZUL`
- `COMPRAR`
- `SAIR`

## Estado atual da implementacao

Ja implementado:

- modelo de cartas e baralho padrao;
- estado da partida com turno, direcao e efeitos principais;
- validacao de `WILD_DRAW_FOUR` (bloqueia quando ha carta da cor ativa);
- notificacao de UNO quando jogador fica com 1 carta;
- tratamento basico de saida/desconexao de jogador;
- validacao de comandos no cliente antes de enviar ao servidor.

Ainda pendente:

- timeout de turno com penalizacao automatica;
- reconexao/reentrada de jogador;
- refinamento do protocolo de mensagens;
- testes automatizados de unidade e integracao.

## Observacao

As pendencias detalhadas e a divisao sugerida para os proximos integrantes estao em `pendencias.md`.
