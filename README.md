# UNO-LP2 — UNO multiplayer com Threads e Sockets

Projeto acadêmico de Linguagem de Programação II. A implementação foi mantida em **Java puro**, sem bibliotecas externas, e segue a ideia do documento **“Passo a Passo do Projeto de LP2”** com a solução mais simples possível.

## Equipe

- Felipe José de Medeiros Melo
- Felipe Cavalcanti Apolinário
- João Lucas Silva Acioli
- Jose Artur Soares Afreu
- Gabriel Rafa Martins Freire

## O que o projeto demonstra

- servidor TCP central com `ServerSocket`;
- uma `Thread` por conexão/jogador;
- até **4 salas simultâneas**;
- uma `Thread` para cada sala (`Room implements Runnable`);
- estado da partida protegido com `ReentrantLock`;
- controle de turno sem espera ocupada com `Semaphore`;
- protocolo textual tokenizado por `;`;
- timeout de turno de **15 segundos** por padrão;
- timer global da partida de **15 minutos** por padrão;
- tratamento de saída e queda de conexão;
- cliente textual e cliente Swing simples.

Os recursos de concorrência usados são os mesmos tipos de mecanismos presentes nos exemplos fornecidos em aula: `Thread`, `Runnable`, `synchronized`, `Semaphore`, `ReentrantLock`, sockets e `ScheduledExecutorService`.

---

## Arquitetura

```text
                         GameServer
                             |
                        RoomManager
                 _________|___________
                |         |           |
             SALA1      SALA2       ... até 4
            (Thread)    (Thread)
               |            |
        GameState +      GameState +
        ClientHandlers   ClientHandlers
          /      \          /      \
      Socket    Socket   Socket    Socket
      Cliente   Cliente  Cliente   Cliente
```

### Classes principais

```text
src/
├── model/
│   ├── Card.java
│   ├── CardColor.java
│   ├── CardValue.java
│   ├── Deck.java
│   ├── PlayerState.java
│   └── GameState.java
├── server/
│   ├── GameServer.java
│   ├── RoomManager.java
│   ├── Room.java
│   └── ClientHandler.java
├── shared/
│   └── Protocol.java
├── client/
│   ├── GameClient.java       # terminal
│   ├── ConsoleUI.java
│   ├── ClientMain.java       # Swing
│   ├── GameWindow.java
│   └── ServerListener.java
└── tests/
    ├── ProtocolSelfTest.java
    ├── DeckSelfTest.java
    ├── GameStateSelfTest.java
    └── GameStateEdgeSelfTest.java
```

---

## Compilar

**Requisito:** JDK 11 ou superior.

Na raiz do projeto:

```bash
find src -name "*.java" -print > sources.txt
javac -d out @sources.txt
```

O projeto não usa Maven, Gradle, JSON externo ou qualquer dependência adicional.

---

## Executar o servidor

```bash
java -cp out server.GameServer <porta> [timeout-turno-segundos] [duracao-partida-minutos]
```

Exemplo padrão:

```bash
java -cp out server.GameServer 5000
```

Isso usa:

- 15 s por turno;
- 15 min por partida;
- até 4 salas simultâneas.

Para testes, os timers podem ser alterados. `0` desativa o timer correspondente:

```bash
java -cp out server.GameServer 5000 0 0
```

---

## Cliente de terminal

```bash
java -cp out client.GameClient localhost 5000 Ana
```

Ao conectar, escolha uma das opções:

```text
CRIAR 2
```

O servidor mostrará o código, por exemplo `SALA1`. O outro jogador usa:

```text
ENTRAR SALA1
```

A partida inicia automaticamente quando a sala atinge a quantidade definida. Essa é a alternativa mais simples prevista no passo a passo (“criador inicia **ou** sala atinge o limite”).

### Comandos durante a partida

```text
COMPRAR
JOGAR VERMELHO:CINCO
JOGAR PRETO:CORINGA AZUL
JOGAR VERMELHO:CINCO UNO
JOGAR PRETO:CORINGA AZUL UNO
DORMIU
SAIR
```

A declaração de `UNO` é enviada junto da jogada que deixa o jogador com uma carta. Isso evita criar uma segunda janela de tempo e segue a alternativa descrita no projeto de fazer o UNO simultaneamente à jogada.

Se o jogador esquecer, o próximo jogador pode usar `DORMIU` antes de encerrar seu próprio turno; o infrator compra +2.

---

## Cliente Swing

O Swing foi mantido propositalmente simples para não desviar o foco da disciplina.

```bash
java -cp out client.ClientMain localhost 5000 Ana
```

A janela mostra:

- sala;
- jogador da vez;
- topo do descarte;
- cor ativa;
- mão;
- eventos recebidos do servidor;
- campo para envio dos mesmos comandos do cliente textual.

A leitura do servidor ocorre em `ServerListener`, e as atualizações da tela usam `SwingUtilities.invokeLater()`.

---

## Regras implementadas

- baralho UNO padrão de 108 cartas;
- 7 cartas iniciais por jogador;
- jogada por mesma cor, mesmo valor ou coringa;
- `SKIP`;
- `REVERSE` (em 2 jogadores funciona como `SKIP`);
- `DRAW_TWO`;
- `WILD` com escolha de cor;
- `WILD_DRAW_FOUR` com escolha de cor e bloqueio quando existe carta da cor ativa na mão;
- reposição do monte de compra usando o descarte, preservando a carta do topo;
- vitória ao zerar a mão;
- estado final das mãos enviado no encerramento;
- `UNO` e `DORMIU` (+2);
- timeout de turno com compra automática de 1 carta;
- timeout global de partida;
- encerramento da sala se um jogador sair/desconectar durante a partida.

---

## Como a consistência é garantida

O **servidor é a única fonte de verdade**. O cliente apenas solicita ações.

`GameState` valida a jogada e protege alterações do estado compartilhado com `ReentrantLock`. A mão de cada jogador também possui um lock próprio. O `Semaphore` de `PlayerState` bloqueia a thread do jogador até sua vez sem usar busy wait.

O `Deck` não precisa ter um segundo mecanismo de sincronização independente porque todas as compras e reposições que alteram o baralho durante a partida ocorrem dentro do lock de `GameState`. Isso mantém a solução menor sem perder segurança.

---

## Protocolo

As mensagens usam **String Tokenizada**, como proposto no passo a passo:

```text
CRIAR_SALA;Ana;2
ENTRAR_SALA;SALA1;Beto
JOGAR;VERMELHO:CINCO
JOGAR;PRETO:CORINGA;AZUL;UNO
COMPRAR
DORMIU
```

Atualizações do servidor incluem:

```text
MAO;...
TOPO;...
ATIVA;...
ATUAL;...
SUA_VEZ
ATUALIZACAO;...
ERRO;...
TEMPO_ESGOTADO;...
FIM_JOGO;...
```

---

## Testes automáticos

```bash
java -ea -cp out tests.ProtocolSelfTest
java -ea -cp out tests.DeckSelfTest
java -ea -cp out tests.GameStateSelfTest
java -ea -cp out tests.GameStateEdgeSelfTest
```

Os testes cobrem protocolo, composição do baralho, fluxo básico, regras de ação, `WILD_DRAW_FOUR`, validação de coringa, penalidade sem troca de turno e liberação das threads quando há vencedor.

Além desses testes unitários simples, o projeto foi validado com testes de integração usando sockets reais para:

- duas salas simultâneas;
- isolamento entre salas;
- comando inválido seguido de nova tentativa no mesmo turno;
- timeout de turno;
- queda de conexão;
- limite de quatro salas;
- `DORMIU` e penalidade +2;
- partida completa até `FIM_JOGO` em dois e três clientes;
- quatro partidas completas simultâneas (8 clientes);
- timer global real;
- smoke test da interface Swing em ambiente gráfico virtual.

---

## Decisões de simplicidade

Para cumprir o que foi idealizado sem criar uma arquitetura desnecessariamente grande:

- o protocolo é texto com `;`, não JSON;
- a sala começa automaticamente quando fica cheia;
- `RoomManager` usa `HashMap` protegido por `synchronized`, em vez de adicionar estruturas mais sofisticadas sem necessidade;
- o timeout do turno usa `Socket.setSoTimeout`, recurso simples e adequado à leitura do socket;
- `ScheduledExecutorService` é usado no timer global;
- se alguém cair durante a partida, a sala é encerrada de forma limpa em vez de implementar bot/reconexão;
- a interface Swing é funcional, mas deliberadamente básica.

Essas escolhas preservam os conceitos centrais de LP2 e evitam complexidade que não acrescentaria valor ao objetivo da disciplina.
