# Nota 4 — Servidor, salas e clientes

## Implementação atual

- `GameServer` mantém o `ServerSocket` em `accept()` contínuo e cria uma thread de lobby por conexão.
- `RoomManager` gerencia até 4 salas simultâneas.
- Cada `Room implements Runnable` aguarda os jogadores e executa uma partida independente.
- Cada jogador em partida possui um `ClientHandler` próprio.
- O servidor valida todas as ações e faz broadcast do estado.
- O timeout de turno usa `Socket.setSoTimeout` e o timer global usa `ScheduledExecutorService`.
- Queda/saída encerra a sala de forma limpa e informa os demais jogadores.
- Existem cliente de terminal e cliente Swing mínimo.

## Lobby

O cliente cria uma sala com quantidade de jogadores ou entra em uma sala existente pelo código. A partida começa automaticamente quando a sala atinge o limite configurado, alternativa prevista no roteiro do projeto.
