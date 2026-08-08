# 🃏 UNO-LP2 — Clone do Jogo UNO com Servidor Multithreaded

> Projeto acadêmico desenvolvido para a disciplina de **Linguagem de Programação II**.
> Arquitetura: **Servidor Multithreaded Centralizado** com comunicação via Sockets TCP.

---

## 👥 Equipe:

| Integrante |
|------------|
| Felipe José de Medeiros Melo |
| Felipe Cavalcanti Apolinário |
| João Lucas Silva Acioli |
| Jose Artur Soares Afreu |
| Gabriel Rafa Martins Freire |

---

## 🎯 Objetivos do Projeto

Implementar uma versão multijogador do clássico jogo de cartas **UNO**, explorando na prática os seguintes conceitos da disciplina:

- **Comunicação via Sockets** - troca de mensagens entre cliente e servidor via TCP/IP
- **Sincronização de Threads** - controle de acesso concorrente ao estado do jogo (`synchronized`, `wait/notify`, `ReentrantLock`)
- **Consistência de Estado** - garantia de que todos os clientes enxergam o mesmo estado da partida
- **Gerência de Tempo** - timeouts de jogada e controle de turno

---

## 🏗️ Arquitetura

```text
Cliente  ──── TCP Socket ────▶  Servidor Central
Cliente  ──── TCP Socket ────▶  (uma Thread por cliente)
Cliente  ──── TCP Socket ────▶  (estado da partida compartilhado)
```

O servidor atua como árbitro central: valida todas as jogadas, mantém o estado da partida e retransmite atualizações para todos os clientes conectados.

---

## 📁 Estrutura do Projeto

```text
uno-lp2/
├── README.md
├── .gitignore
└── src/
    ├── client/          # Código do cliente (UI, envio de comandos)
    ├── server/          # Código do servidor (motor do jogo, threads)
    ├── shared/          # Protocolo de comunicação (mensagens via socket)
    └── model/           # Modelo de domínio (cartas, baralho, estado do jogador)
```

---

## 🚀 Como Compilar e Executar

> **Requisito:** JDK 11+ instalado. Nenhum gerenciador de dependências é necessário.

### Compilar tudo de uma vez

```bash
# Na raiz do projeto
find src -name "*.java" -print > sources.txt
javac -d out @sources.txt
```

> Em alguns ambientes pode ser necessário compilar com release explícito:

```bash
javac --release 25 -d out @sources.txt
```

### Executar o Servidor

```bash
java -cp out server.GameServer <porta> <numero-de-jogadores> [timeout-segundos]
```

Exemplos:

```bash
# Com timeout padrão (10s)
java -cp out server.GameServer 5000 2

# Sem timeout de turno (evita avanço automático)
java -cp out server.GameServer 5000 2 0
```

### Executar o Cliente

```bash
java -cp out client.GameClient <endereço-do-servidor> <porta> <nome>
```

### Comandos do Cliente

```text
COMPRAR
JOGAR <COR>:<VALOR> [COR_DECLARADA]
SAIR
```

---

## 🗺️ Roadmap de Fases

| Fase | Descrição | Status |
|------|-----------|--------|
| **Fase 1 — Modelo** | Cartas, Baralho e Estado do Jogador | ✅ Base concluída / ajustes avançados pendentes |
| **Fase 2 — Servidor** | GameServer, ClientHandler (threads), estado compartilhado | ✅ Concluída no escopo atual |
| **Fase 3 — Cliente** | Conexão ao servidor, loop de jogada, UI textual | ✅ Concluída no escopo atual |
| **Fase 4 — Protocolo** | Mensagens estruturadas, validações e tratamento de erros | ✅ Concluída no escopo atual |

---

## 📐 Conceitos Aplicados

### Sincronização de Threads
O estado da partida (`GameState`) é acessado por múltiplas threads de clientes simultaneamente. Usamos blocos `synchronized`, `ReentrantLock` e `Semaphore` para garantir atomicidade nas operações críticas (jogar carta, comprar carta, passar a vez).

### Comunicação via Sockets
Cada cliente abre uma conexão TCP com o servidor. O protocolo é baseado em texto simples (uma mensagem por linha) com separação por `;` (classe `Protocol`) para facilitar a depuração e manter consistência.

### Gerência de Tempo
Cada jogador tem um tempo limite por turno. O servidor aplica timeout por turno no `ClientHandler` e penaliza automaticamente com compra de carta quando não há ação no tempo esperado.

---

## ✅ Atualizações Recentes

- Validação de `WILD_DRAW_FOUR` no modelo (impede jogada ilegal quando há carta da cor ativa).
- Notificação de `UNO` quando o jogador fica com 1 carta.
- Comando `SAIR` com encerramento ordenado do cliente.
- Tratamento básico de desconexão no servidor.
- Validação de entrada no cliente antes do envio ao servidor.
- Timeout de turno com penalização automática (compra de 1 carta).
- Reenvio de estado (`TOPO`, `ATIVA`, `ATUAL`, `MAO`) após eventos relevantes para manter clientes sincronizados.

---

## 🧪 Testes Automatizados (sem framework externo)

Os testes ficam em `src/tests` e podem ser executados direto com `java`.

### Executar todos os testes

```bash
# Na raiz do projeto
find src -name "*.java" -print > sources.txt
javac -d out @sources.txt

java -cp out tests.ProtocolSelfTest
java -cp out tests.DeckSelfTest
java -cp out tests.GameStateSelfTest
```

### O que esses testes cobrem

- `ProtocolSelfTest`:
    - serialização e parse de mensagens do protocolo;
    - compatibilidade com comando legado separado por espaço;
    - round-trip de carta e mão.
- `DeckSelfTest`:
    - tamanho e composição do baralho UNO padrão (108 cartas);
    - regras básicas de jogabilidade de cartas.
- `GameStateSelfTest`:
    - estado inicial da partida;
    - avanço de turno após compra;
    - fluxo simples de jogar carta (quando possível) ou comprar.
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
