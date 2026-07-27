# Pendências do Projeto UNO-LP2

## O que já foi feito

- Modelo de cartas completo:
  - `src/model/Card.java`
  - `src/model/CardColor.java`
  - `src/model/CardValue.java`
- Implementação do baralho UNO padrão:
  - `src/model/Deck.java`
  - distribuição de mãos iniciais aleatória
  - compra de cartas
  - contagem de cartas restantes
- Estado do jogador com concorrência:
  - `src/model/PlayerState.java`
  - exclusão mútua na mão de cartas
  - controle básico de turno com `Semaphore`
- Estado global da partida:
  - `src/model/GameState.java`
  - baralho de compra e monte de descarte
  - lógica de turno, direção e efeitos de cartas
  - reposição do baralho de compra a partir do descarte
- Integração servidor/cliente:
  - `src/server/GameServer.java`
  - `src/server/ClientHandler.java`
  - `src/client/GameClient.java`
  - `src/shared/Protocol.java`
- Interface de cliente com cores ANSI e formato em português.
- Notas de implementação em `notes/nota-1.md` a `notes/nota-4.md`.

## O que ainda está pendente

### Fase 1 — Modelo

- `GameState` precisa completar regras do UNO:
  - validação de `WILD_DRAW_FOUR` apenas quando não há alternativa legal
  - regras de compra/skip em cadeias de efeitos quando múltiplas cartas são jogadas
  - lógica de `UNO` (notificação de quando o jogador fica com uma carta)
- suporte de descarte e compra para construção de partidas reais com mais de 2 jogadores.

### Fase 2 — Servidor

- robustez de conexão:
  - tratamento de cliente desconectado durante a partida
  - reentrada de jogador ou encerramento ordenado da partida
- controle de timeout de turno e penalização automática.
- sincronização do estado entre clientes quando um jogador joga fora de turno ou falha.
- logs do servidor em português e com contexto de jogo.

### Fase 3 — Cliente

- interface mais amigável:
  - exibir mãos com separação clara
  - mostrar status do jogo em blocos e com legendas
  - menu de ajuda persistente e comandos claros
- suportar saída ordenada (`SAIR`) e reconexão.
- tratamento de entrada inválida no próprio cliente antes de enviar ao servidor.

### Fase 4 — Protocolo

- protocolo ainda é texto simples e precisa de refinamento:
  - mensagens de atualização mais estruturadas
  - consistência entre `TOPO`, `ATIVA`, `ATUAL` e `MAO`
  - remoção de tokens internos em inglês no log do servidor
- serialização de cartas e cores já está em português, mas ainda pode ser melhorada para UX.

### Testes e qualidade

- falta de testes automatizados de unidade e integração.
- nenhuma validação de cenário extremo:
  - baralho acabando repetidamente
  - tentativa de jogar carta inválida
  - jogador comprando carta sem desistir
- documentação de uso e instruções não atualizada no `README.md`.

## Pendências específicas do passo a passo

- Fase 1 — Modelo: ainda não concluída completamente. O baralho e o estado básico existem, mas faltam regras completas do UNO e tratamento de mão/monte avançado.
- Fase 2 — Servidor: está implementado um servidor inicial, mas ainda falta o controle de jogo mais robusto e a gestão de threads para tempo limite.
- Fase 3 — Cliente: existe cliente funcional, mas a UI ainda não está completa e precisa de conversão de estado para apresentação mais moderna.
- Fase 4 — Protocolo: já há um protocolo básico, mas o projeto precisa de mensagens melhor definidas e tratamento de erros mais consistente.

## Recomendações para próximos passos

1. Priorizar a validação das regras do UNO no `GameState`.
2. Implementar timeouts e tratamento de desconexão no servidor.
3. Melhorar a apresentação do cliente com blocos de estado e cores consistentes.
4. Adicionar testes unitários para `Deck`, `PlayerState`, `GameState` e protocolo.
5. Atualizar `README.md` com instruções de uso em português e exemplos de comando.
