# Nota 3 — Estado global da partida

## Implementação atual

`GameState.java` centraliza as regras e é a única fonte de verdade da partida.

- distribui 7 cartas por jogador;
- controla topo, cor ativa, direção e jogador atual;
- usa um `ReentrantLock` justo para alterações do estado compartilhado;
- valida jogadas antes de alterar a mão;
- implementa `SKIP`, `REVERSE`, `DRAW_TWO`, `WILD` e `WILD_DRAW_FOUR`;
- valida a restrição do `WILD_DRAW_FOUR`;
- repõe o monte de compra com o descarte;
- detecta vencedor e libera threads que estavam aguardando turno;
- permite penalidade de +2 do `DORMIU` sem trocar o turno;
- permite encerrar a partida de modo controlado em timeout global ou desconexão.

Os testes em `src/tests` verificam o fluxo básico e os casos de borda mais importantes.
