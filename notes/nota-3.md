# Nota 3 — Implementação do estado global de jogo (GameState)

## O que foi feito

1. Criada a classe `src/model/GameState.java`.
2. Implementado o estado global do jogo com:
   - `drawPile` (baralho de compra)
   - `discardPile` (monte de descarte)
   - lista de `players` (`PlayerState`)
   - `currentPlayerIndex` e `direction`
   - `topCard` e `activeColor`
3. Definido fluxo de início de partida:
   - distribuição de 7 cartas para cada jogador
   - escolha da carta inicial do topo
   - início do primeiro turno
4. Implementado controle de concorrência para o estado do jogo usando `ReentrantLock` e `Condition`.
5. Criados métodos de jogo:
   - `playCard(int playerId, Card card, CardColor declaredColor)`
   - `drawCards(int playerId, int count)`
   - `hasGameOver()` / `getWinner()`
   - `currentPlayer()` / `getTopCard()` / `getActiveColor()`
6. Incluído suporte a efeitos de carta:
   - `SKIP` avança um turno adicional
   - `REVERSE` inverte a direção ou age como skip em 2 jogadores
   - `DRAW_TWO` e `WILD_DRAW_FOUR` fazem o próximo jogador comprar cartas
   - `WILD` e `WILD_DRAW_FOUR` permitem declaração de cor ativa
7. Implementado reposição do baralho de compra a partir do descarte quando necessário.

## Por que foi feito

- `GameState` representa a parte concorrente mais importante do jogo: o estado compartilhado entre jogadores.
- Sem essa classe, não há forma organizada de coordenar turnos, aplicar efeitos e manter o fluxo de jogo.
- O uso de locks garante que apenas uma modificação de estado ocorra por vez, evitando condições de corrida.
- O discarto e reposição garantem que o jogo possa continuar quando o baralho de compra acabar.

## Detalhes importantes

- A carta inicial do topo não pode ser coringa. Se for sorteado um coringa, ele é movido para o descarte e outra carta é retirada.
- A cor ativa é atualizada sempre que uma carta não-coringa é jogada ou quando um coringa é declarado.
- O turno do jogador é concedido quando `currentPlayer().grantTurn()` é chamado. Este modelo funciona bem com threads de cliente/server.
- O método `playCard` valida se a carta é jogável com base na carta do topo e na cor ativa.

## Pontos de atenção

- `replenishDrawPileIfNeeded()` ainda usa `new Random()` para embaralhar o descarte. Poderia ser melhor reutilizar o `Random` do `Deck`.
- A regra de `WILD_DRAW_FOUR` ainda não valida se o jogador não tinha alternativa legal na mão. Isso pode ser aprimorado mais adiante.
- A lógica de fim de partida considera apenas o primeiro jogador que ficar com zero cartas.

## Possibilidade de testar

Já é possível testar:

- Criação de `GameState` com 2 ou mais jogadores
- Distribuição inicial de cartas e baralho restante
- Jogada de cartas válidas e compra de cartas
- Avanço de turnos e aplicação de efeitos simples
- Reposição de baralho de compra quando necessário

Exemplo básico de teste manual:

```java
GameState game = GameState.create(List.of("Alice", "Bob"));
PlayerState current = game.currentPlayer();
Card top = game.getTopCard();
CardColor color = game.getActiveColor();
List<Card> hand = current.getHandSnapshot();
```

E depois, tentar `game.playCard(current.getPlayerId(), hand.get(0), CardColor.RED);` se a carta for jogável.
