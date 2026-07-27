# Nota 4 — Integração servidor/cliente

## O que foi feito

1. Criados os pacotes `src/server`, `src/client` e `src/shared` com implementação real de integração.
2. `src/shared/Protocol.java` define a convenção de serialização de cartas e mãos entre servidor e cliente.
3. `src/server/GameServer.java` implementa:
   - servidor TCP que aguarda um número fixo de jogadores
   - registro de nomes ao conectar
   - inicialização da partida usando `GameState`
   - broadcast de mensagens de atualização para todos os clientes
4. `src/server/ClientHandler.java` implementa:
   - loop de comunicação individual com cada cliente
   - envio de estado atual do jogo
   - recepção de comandos do cliente: `DRAW` e `PLAY`
   - uso de semáforo em `PlayerState` para bloquear até que seja a vez do jogador
5. `src/client/GameClient.java` implementa cliente textual que:
   - se conecta ao servidor
   - envia nome do jogador
   - recebe e imprime mensagens do servidor
   - permite entrada do usuário para enviar comandos

## Por que foi feito

- O projeto exige comunicação distribuída, então é necessário um servidor central e clientes que se conectem via socket.
- O servidor coordena a partida e mantém o estado compartilhado (`GameState`), enquanto cada cliente representa a interface do jogador.
- O protocolo textual permite depuração simples e entendimento das mensagens trocadas.
- A integração com `GameState` garante que a lógica de jogo centralizada seja executada apenas no servidor.

## Como testar

1. Compile o projeto:
   ```bash
   find src -name "*.java" -print > sources.txt
   javac -d out @sources.txt
   ```
2. Inicie o servidor para 2 jogadores:
   ```bash
   java -cp out server.GameServer 12345 2
   ```
3. Em outra janela, inicie dois clientes:
   ```bash
   java -cp out client.GameClient localhost 12345 Alice
   java -cp out client.GameClient localhost 12345 Bob
   ```
4. Use comandos no cliente:
   - `DRAW` — compra uma carta e passa a vez
   - `PLAY RED:FIVE` — joga a carta se ela for válida
   - `PLAY BLACK:WILD BLUE` — joga coringa e declara a nova cor

> As mensagens do servidor exibem o estado atual e facilitam a depuração.
