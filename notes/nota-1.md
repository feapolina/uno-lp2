# Nota 1 — Implementação do baralho UNO

## O que foi feito

1. Criado `src/model/Deck.java`.
2. Implementado o baralho padrão do UNO com 108 cartas:
   - 1 zero por cor (RED, BLUE, GREEN, YELLOW)
   - 2 cartas de cada número de 1 a 9 por cor
   - 2 cartas de cada ação colorida (`SKIP`, `REVERSE`, `DRAW_TWO`) por cor
   - 4 cartas `WILD` e 4 cartas `WILD_DRAW_FOUR` com cor `BLACK`
3. Adicionado embaralhamento usando `Collections.shuffle` e `Random`.
4. Implementado o método `draw()` para retirar a carta do topo do baralho.
5. Adicionado `draw(int count)` para comprar múltiplas cartas.
6. Implementado `dealHands(int numberOfPlayers, int cardsPerPlayer)` para distribuir cartas iniciais.
7. Adicionado métodos auxiliares: `cardsRemaining()`, `isEmpty()` e `toString()`.

## Por que foi feito

- O baralho é a base do jogo UNO. Sem ele não há como distribuir mãos ou comprar cartas.
- A distribuição em mãos inicial deve ser aleatória e corresponder ao comportamento real do UNO, onde cada jogador recebe sete cartas de forma intercalada.
- A quantidade de cartas restantes no baralho diminui conforme as cartas são distribuídas para os jogadores, o que é crucial para o estado do jogo e para eventual compra posterior.
- Métodos de consulta (`cardsRemaining`, `isEmpty`) ajudam o servidor ou o gerenciador de jogo a saber quando é preciso reembaralhar ou quando o jogo pode acabar.

## Observações

- Atualmente o `Deck` apenas representa o baralho central e a distribuição inicial.
- Mais tarde, podemos implementar um baralho de descarte separado para suportar a regra de reposição quando o baralho acabar.
