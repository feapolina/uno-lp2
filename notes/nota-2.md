# Nota 2 — Estado do jogador e turno

## Implementação atual

`PlayerState.java` guarda identificador, nome e mão do jogador.

- A mão é protegida com `ReentrantLock`.
- `addCards`, `playCard`, `getHandSnapshot`, `cardsInHand` e `removeAllCards` são protegidos pelo lock.
- Um `Semaphore` justo representa a permissão de turno.
- `grantTurn()` libera a vez e `waitForTurn()` bloqueia a thread sem espera ocupada.

## Decisão de simplicidade

Foram removidos `Condition` e semáforos adicionais que não eram necessários. O projeto usa apenas um lock para proteger a mão e um semáforo para bloquear/desbloquear o turno, exatamente os conceitos praticados nos exemplos da disciplina.
