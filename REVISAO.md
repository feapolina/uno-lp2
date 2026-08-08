# Revisão técnica — UNO-LP2

Data da revisão: 08/08/2026

## Objetivo

Conferir o projeto em relação ao `README.md`, `pendencias.md`, ao documento `Passo a Passo do Projeto de LP2.pdf` e aos exemplos de código fornecidos pelo professor. A prioridade foi cumprir o escopo com Java puro e com a menor complexidade necessária.

## Diagnóstico do projeto recebido

A base já possuía modelo de cartas, baralho, estado da partida, sockets, thread por jogador e mecanismos de sincronização. Porém, ainda havia diferenças importantes para o roteiro idealizado:

- servidor trabalhava essencialmente com uma partida fixa, sem gerência real de múltiplas salas;
- não havia fluxo completo de criar/entrar em sala por código;
- `UNO`/`DORMIU` ainda não estavam fechados como mecânica de rede;
- faltava timer global da partida;
- faltava cliente Swing funcional;
- havia documentação antiga que contradizia o código atual;
- um comando inválido durante o turno podia deixar o jogador bloqueado esperando um novo `Semaphore`;
- ao existir vencedor, outras threads podiam permanecer bloqueadas aguardando turno;
- um coringa malformado podia ser removido da mão antes da validação completa.

## Solução final

A versão revisada mantém uma arquitetura curta:

`GameServer -> RoomManager -> Room -> GameState + ClientHandler`

- `GameServer`: `ServerSocket`, `accept()` contínuo e thread curta de lobby.
- `RoomManager`: `HashMap` + `synchronized`, máximo de 4 salas.
- `Room implements Runnable`: uma thread por sala.
- `ClientHandler implements Runnable`: uma thread por jogador na partida.
- `GameState`: `ReentrantLock` para o estado compartilhado.
- `PlayerState`: `ReentrantLock` para a mão e `Semaphore` para a vez.
- Timeout de turno: `Socket.setSoTimeout`.
- Timer global: `ScheduledExecutorService`.
- Protocolo: Strings separadas por `;`.
- Cliente Swing: somente componentes nativos do JDK.

Essas escolhas são compatíveis com os exemplos fornecidos em aula, especialmente os exemplos de servidor multithread, `ReentrantLock`, `Semaphore` e tarefas agendadas.

## Requisitos do roteiro

| Requisito | Situação |
|---|---|
| Servidor central TCP | OK |
| Múltiplos clientes | OK |
| Até 4 salas | OK |
| Criar/entrar em sala por código | OK |
| Thread por sala | OK |
| Thread por jogador | OK |
| 7 cartas iniciais | OK |
| Servidor como fonte de verdade | OK |
| Jogadas e cartas de ação | OK |
| WILD_DRAW_FOUR validado | OK |
| UNO simultâneo à jogada | OK |
| DORMIU com +2 | OK |
| Timeout de turno | OK |
| Timer global | OK |
| Desconexão tratada | OK |
| Vencedor e mãos finais | OK |
| Protocolo tokenizado | OK |
| Cliente de terminal | OK |
| Cliente Swing simples | OK |

## Testes realizados

### Compilação e compatibilidade

- `javac -Xlint:all`: OK, sem warnings.
- `javac --release 11 -Xlint:all`: OK.

### Self-tests Java

- `ProtocolSelfTest`: OK.
- `DeckSelfTest`: OK.
- `GameStateSelfTest`: OK.
- `GameStateEdgeSelfTest`: OK.

### Integração com sockets reais

- duas salas simultâneas e isolamento entre elas: OK;
- comando inválido e nova tentativa no mesmo turno: OK;
- timeout de turno e passagem para o próximo jogador: OK;
- desconexão abrupta e encerramento limpo: OK;
- limite de quatro salas: OK;
- validações de lobby (quantidade inválida, sala inexistente e nome vazio): OK;
- `DORMIU` e penalidade +2: OK;
- partida completa com 2 jogadores: OK;
- cinco partidas completas adicionais com baralhos aleatórios: OK;
- partida completa com 3 jogadores: OK;
- quatro partidas completas simultâneas (8 clientes): OK;
- timer global real de 1 minuto, com uma única mensagem final para cada cliente: OK;
- abertura, atualização e fechamento da janela Swing em display virtual: OK.

O fluxo visual Swing foi exercitado em um display virtual (`xvfb`), além da compilação.

## Itens propositalmente não adicionados

Reconexão, bot para jogador desconectado, início manual pelo criador antes da sala lotar, interface com cartas desenhadas e regras variantes de UNO não são necessárias para o escopo central. A alternativa escolhida em cada caso reduz complexidade sem retirar os conceitos pedidos no trabalho.
