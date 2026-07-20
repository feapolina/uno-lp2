# 🃏 UNO-LP2 — Clone do Jogo UNO com Servidor Multithreaded

> Projeto acadêmico desenvolvido para a disciplina de **Linguagem de Programação II**.  
> Arquitetura: **Servidor Multithreaded Centralizado** com comunicação via Sockets TCP.

---

## 👥 Equipe:

| Felipe José de Medeiros Melo 
| Felipe Cavalcanti Apolinário |
| João Lucas Silva Acioli 
| Jose Artur Soares Afreu 
| Gabriel Rafa Martins Freire 

---

## 🎯 Objetivos do Projeto

Implementar uma versão multijogador do clássico jogo de cartas **UNO**, explorando na prática os seguintes conceitos da disciplina:

- **Comunicação via Sockets** — troca de mensagens entre cliente e servidor via TCP/IP
- **Sincronização de Threads** — controle de acesso concorrente ao estado do jogo (`synchronized`, `wait/notify`)
- **Consistência de Estado** — garantia de que todos os clientes enxergam o mesmo estado da partida
- **Gerência de Tempo** — timeouts de jogada e controle de turno

---

## 🏗️ Arquitetura

```
Cliente  ──── TCP Socket ────▶  Servidor Central
Cliente  ──── TCP Socket ────▶  (uma Thread por cliente)
Cliente  ──── TCP Socket ────▶  (estado da partida compartilhado)
```

O servidor atua como árbitro central: valida todas as jogadas, mantém o estado da partida e retransmite atualizações para todos os clientes conectados.

---

## 📁 Estrutura do Projeto

```
uno-lp2/
├── README.md
├── .gitignore
└── src/
    ├── client/          # Código exclusivo do cliente (UI, envio de comandos)
    ├── server/          # Código exclusivo do servidor (motor do jogo, threads)
    ├── shared/          # Protocolo de comunicação (mensagens trafegadas via socket)
    └── model/           # Modelo de domínio puro (cartas, baralho, estado do jogador)
```

---

## 🚀 Como Compilar e Executar

> **Requisito:** JDK 11+ instalado. Nenhum gerenciador de dependências necessário (Java puro).

### Compilar tudo de uma vez

```bash
# Na raiz do projeto
find src -name "*.java" -print > sources.txt
javac -d out @sources.txt
```

### Executar o Servidor

```bash
java -cp out server.GameServer <porta>
```

### Executar o Cliente

```bash
java -cp out client.GameClient <endereço-do-servidor> <porta>
```

---

## 🗺️ Roadmap de Fases

| Fase | Descrição | Status |
|------|-----------|--------|
| **Fase 1 — Modelo** | Cartas, Baralho e Estado do Jogador | 🔄 Em andamento |
| **Fase 2 — Servidor** | GameServer, ClientHandler (threads), estado compartilhado | ⏳ Pendente |
| **Fase 3 — Cliente** | Conexão ao servidor, loop de jogada, UI textual | ⏳ Pendente |
| **Fase 4 — Protocolo** | Mensagens estruturadas, validações e tratamento de erros | ⏳ Pendente |

---

## 📐 Conceitos Aplicados

### Sincronização de Threads
O estado da partida (`GameState`) é acessado por múltiplas threads de clientes simultaneamente. Usamos blocos `synchronized` e `ReentrantLock` para garantir atomicidade nas operações críticas (jogar carta, comprar carta, passar a vez).

### Comunicação via Sockets
Cada cliente abre uma conexão TCP com o servidor. O protocolo é baseado em texto simples (uma mensagem por linha) para facilitar a depuração.

### Gerência de Tempo
Cada jogador tem um tempo limite por turno. Um `ScheduledExecutorService` no servidor gerencia o timeout e aplica a penalidade automaticamente (comprar cartas) se o jogador não agir a tempo.
