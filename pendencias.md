# Pendências do Projeto UNO-LP2

## Status atual (27/07/2026)

### Implementado recentemente

- Regra de `WILD_DRAW_FOUR` no `GameState`:
  - bloqueia jogada quando o jogador ainda possui carta da cor ativa.
- Notificação de `UNO` no servidor:
  - ao ficar com 1 carta, é enviada atualização para todos os clientes.
- Suporte a comando `SAIR`:
  - cliente encerra de forma ordenada;
  - servidor trata saída e notifica os demais jogadores.
- Tratamento básico de desconexão:
  - quando restam menos de 2 jogadores conectados, a partida é encerrada.
- Timeout de turno no servidor:
  - se o jogador não agir dentro do tempo limite, compra 1 carta automaticamente;
  - a vez avança e o estado é sincronizado com todos os clientes.
- Sincronização explícita de estado entre clientes:
  - após jogadas válidas, erros e timeout, servidor reenvia `TOPO`, `ATIVA`, `ATUAL` e `MAO`.
- Validação de comandos no cliente antes de enviar ao servidor:
  - aceita `COMPRAR`, `SAIR` e `JOGAR <COR>:<VALOR> [COR_DECLARADA]`.
- Ajuste no handshake do cliente:
  - nome enviado imediatamente ao conectar.

### Base já existente no projeto

- Modelo de cartas completo (`Card`, `CardColor`, `CardValue`).
- Baralho UNO padrão (`Deck`) com compra e distribuição de mão inicial.
- Estado de jogador concorrente (`PlayerState`) com proteção da mão.
- Estado global da partida (`GameState`) com turno, direção e efeitos principais.
- Comunicação cliente/servidor com protocolo textual (`GameServer`, `ClientHandler`, `GameClient`, `Protocol`).

## Pendências que ainda faltam

### Fase 1 — Modelo

- Completar regras avançadas do UNO (ex.: desafios e variações de efeitos encadeados).
- Revisar cenários extremos de compra/monte para partidas longas.

### Fase 2 — Servidor

- Concluída no escopo atual:
  - `GameServer` + `ClientHandler` com threads por cliente e estado compartilhado;
  - timeout de turno com penalização automática;
  - tratamento de desconexão e encerramento ordenado da partida;
  - sincronização de estado para todos os clientes após eventos de jogo.
- Evoluções futuras (opcionais):
  - reconexão/reentrada real de jogador;
  - logs estruturados e telemetria de partidas.

### Fase 3 — Cliente

- Melhorar UI textual (blocos de status mais claros e melhor organização da mão).
- Implementar ajuda persistente e mensagens guiadas.
- Suporte a reconexão no cliente.

### Fase 4 — Protocolo

- Padronizar melhor as mensagens (`TOPO`, `ATIVA`, `ATUAL`, `MAO`, `ATUALIZACAO`).
- Evoluir protocolo para mensagens mais estruturadas (com menos ambiguidade).

### Testes e qualidade

- Criar testes automatizados (unidade e integração).
- Cobrir cenários de erro e concorrência:
  - jogada inválida fora de turno;
  - baralho esgotando repetidamente;
  - desconexão durante jogada.

## Sugestão de divisão para os próximos integrantes

1. Integrante A: refinamento de protocolo e padronização de mensagens.
2. Integrante B: reconexão de jogador e melhorias de UX no cliente.
