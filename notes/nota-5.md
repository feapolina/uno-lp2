# Nota 5 — Protocolo de rede

## Implementação atual

`Protocol.java` usa mensagens de texto separadas por `;`, sem bibliotecas externas.

Principais comandos do cliente:

- `CRIAR_SALA;<nome>;<quantidade>`
- `ENTRAR_SALA;<codigo>;<nome>`
- `JOGAR;<carta>[;<cor>][;UNO]`
- `COMPRAR`
- `DORMIU`
- `SAIR`

Principais mensagens do servidor incluem `SALA_CRIADA`, `INICIO_PARTIDA`, `MAO`, `TOPO`, `ATIVA`, `ATUAL`, `SUA_VEZ`, `ATUALIZACAO`, `ERRO`, `TEMPO_ESGOTADO` e `FIM_JOGO`.

As cartas são serializadas em português, por exemplo `VERMELHO:CINCO` e `PRETO:CORINGA`.

## Decisão de simplicidade

Foi mantido o modelo de String tokenizada sugerido no roteiro. Não há JSON, serialização de objetos ou dependências externas.
