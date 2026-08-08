# Pendências do Projeto UNO-LP2

## Status revisado — 08/08/2026

Os itens centrais descritos no **Passo a Passo do Projeto de LP2** estão implementados no escopo atual.

### Concluído

- [x] Baralho UNO de 108 cartas e distribuição de 7 cartas.
- [x] Estado do jogador e estado global da partida.
- [x] Validação centralizada das jogadas no servidor.
- [x] `SKIP`, `REVERSE`, `DRAW_TWO`, `WILD` e `WILD_DRAW_FOUR`.
- [x] Regra de legalidade do `WILD_DRAW_FOUR`.
- [x] Reposição do monte de compra.
- [x] Servidor TCP central em loop de `accept()`.
- [x] Thread por conexão/jogador.
- [x] `RoomManager` com até 4 salas simultâneas.
- [x] `Room implements Runnable`, uma thread por sala.
- [x] Handshake para criar/entrar em sala com código.
- [x] Broadcast de lobby e de estado da partida.
- [x] Protocolo textual tokenizado por `;`.
- [x] Timeout de turno de 15 s por padrão.
- [x] Timer global de 15 min por padrão.
- [x] Compra automática de 1 carta em timeout de turno.
- [x] Regra de `UNO` declarada junto da jogada.
- [x] Comando `DORMIU` com punição de +2.
- [x] Fim de jogo com vencedor e estado final das mãos.
- [x] Tratamento de `SAIR` e desconexão durante partida.
- [x] Cliente textual.
- [x] Cliente Swing simples com thread de escuta do servidor.
- [x] Testes unitários/regressão sem framework externo.
- [x] Testes de integração com sockets reais executados.

## Melhorias opcionais — não necessárias para o escopo atual

- [ ] Permitir que o criador inicie a sala antes de atingir o limite. Atualmente a partida inicia ao atingir o limite, opção já prevista no documento.
- [ ] Reconectar um jogador na mesma partida após queda. Atualmente a sala é encerrada de forma segura.
- [ ] Substituir jogador desconectado por bot.
- [ ] Deixar a interface Swing mais visual, com botões/cartas desenhadas.
- [ ] Adicionar regras variantes não exigidas, como empilhamento de `+2/+4` ou desafio formal do `+4`.

Esses itens são evoluções de produto. Não são necessários para demonstrar sockets, threads, sincronização, consistência de estado, salas e gerência de tempo.
