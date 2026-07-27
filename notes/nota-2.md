# Nota 2 — Implementação do estado do jogador com controle de concorrência

## O que foi feito

1. Criada a classe `src/model/PlayerState.java`.
2. Incluídos os dados básicos do jogador:
   - `playerId`
   - `playerName`
   - `hand` (mão de cartas)
3. Adicionada proteção contra acesso simultâneo à mão de cartas usando `ReentrantLock`.
4. Implementadas operações de mão thread-safe:
   - `addCards(List<Card>)`
   - `drawCard(Card)`
   - `playCard(Card)`
   - `getHandSnapshot()`
   - `cardsInHand()`
   - `removeAllCards()`
5. Adicionado mecanismo de espera por carta com `Condition`:
   - `waitForCard()`
6. Adicionados semáforos para controle de turno e de ação:
   - `grantTurn()` / `waitForTurn()` / `clearTurnPermission()`
   - bloqueio de ação `actionPermission` para proteger `playCard`
7. Adicionada verificação de carta jogável com `hasPlayableCard(Card, CardColor)`.

## Por que foi feito

- Como o projeto é de Programação Concorrente e Distribuída, o estado do jogador precisa ser seguro para acesso por múltiplas threads.
- Cada jogador terá uma thread associada (por exemplo, no servidor) e essas threads podem ler e escrever no estado do jogador ao mesmo tempo.
- O uso de `ReentrantLock` garante exclusão mútua durante leitura/escrita da mão do jogador.
- O `Condition` permite que o servidor espere pelo jogador ter cartas antes de prosseguir, evitando verificação ativa (busy wait).
- O `Semaphore` garante que somente o jogador cujo turno foi concedido possa agir naquele momento.

## Como funciona

- `grantTurn()` libera o próximo turno para o jogador.
- `waitForTurn()` bloqueia até que o servidor dê o turno.
- `playCard(Card)` bloqueia com semáforo de ação antes de alterar a mão, garantindo que apenas uma jogada por vez seja processada.
- `hasPlayableCard(...)` permite verificar se o jogador possui cartas válidas para a jogada atual.

## Observações

- Ainda não há uma implementação completa do fluxo de turno do jogo ou de como o servidor coordena os jogadores.
- O próximo passo natural é criar uma classe de estado de jogo (`GameState`) que use `PlayerState` e controle os turnos entre múltiplos jogadores.
