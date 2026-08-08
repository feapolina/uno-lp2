# Nota 1 — Baralho UNO

## Implementação atual

- `Deck.java` cria o baralho padrão de 108 cartas.
- O embaralhamento usa `Collections.shuffle` e `Random`.
- `draw()` e `draw(int)` fazem as compras.
- `dealHands(...)` distribui as 7 cartas iniciais.
- Quando o monte de compra termina, `GameState` reaproveita o descarte, mantendo a carta do topo.

## Relação com a disciplina

É uma classe simples de modelo. A sincronização do baralho não é duplicada dentro de `Deck`: durante a partida, todas as alterações do baralho passam pelo `ReentrantLock` de `GameState`.
