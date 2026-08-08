# Nota 5 — Refinamento do Protocolo de Rede (Fase 4)

## O que foi feito

1. Atualização completa da classe `src/shared/Protocol.java` para estruturar a comunicação TCP.
2. Definição do separador de protocolo (`;`) para implementação do modelo de String Tokenizada.
3. Adição de constantes padronizadas para as mensagens trocadas:
   - **Servidor -> Cliente:** `MAO`, `TOPO`, `ATIVA`, `ATUAL`, `SALA_JOIN_SUCCESS`, `FIM_JOGO`, etc.
   - **Cliente -> Servidor:** `ENTRAR_SALA`, `JOGAR`, `COMPRAR`, `UNO`, `SAIR`.
4. Criação de métodos utilitários robustos:
   - `buildMessage(String command, String... arguments)`: junta comandos e argumentos em uma única string com o separador.
   - `parseMessage(String rawMessage)`: quebra a string recebida pelo Socket em um array de fácil leitura.
5. Tradução nativa no mapeamento de cartas (bloco `static`): serialização de cores e valores convertida do inglês (ex: `RED:DRAW_TWO`) para o português (`VERMELHO:COMPRA_DOIS`).

## Por que foi feito

- O projeto exige mensagens claras trafegando no Socket para facilitar a depuração. A abordagem de "String Tokenizada" foi escolhida para estruturar os dados mantendo o projeto restrito ao Java puro (sem dependências externas como JSON).
- Evitar *Magic Strings* (textos jogados diretamente no código) no `ClientHandler` e no `GameClient`. O uso de constantes evita bugs silenciosos por erros de digitação.
- A tradução das cartas no nível do protocolo melhora drasticamente a legibilidade dos logs do servidor e a interface textual (UX) apresentada ao usuário no terminal.
- Concluir todas as exigências listadas no `pendencias.md` referentes à **Fase 4**.

## Como usar na Integração (Próximos Passos)

A partir de agora, todas as mensagens trafegam na rede separadas por ponto e vírgula (`;`). As classes `ClientHandler.java` e `GameClient.java` (ou a nova `ConsoleUI.java`) devem adotar o `Protocol.java` para montar e desmontar essas mensagens.

**1. Exemplo de Envio (Cliente -> Servidor):**
O objetivo é que a string trafegue no socket TCP exatamente assim: `JOGAR;VERMELHO:CINCO`.

```java
// Você não precisa concatenar string com ";" na mão. Use o utilitário:
String mensagem = Protocol.buildMessage(Protocol.CMD_JOGAR, "VERMELHO:CINCO");

out.println(mensagem); // Envia a string "JOGAR;VERMELHO:CINCO" pela rede
```

**2. Exemplo de Leitura (Servidor recebendo a mensagem):**
```java
String inputLine = in.readLine(); // Recebeu a string "JOGAR;VERMELHO:CINCO"

// Desmonta a string usando o separador ";"
String[] partes = Protocol.parseMessage(inputLine); // Retorna o array: ["JOGAR", "VERMELHO:CINCO"]

// Analisa a primeira parte (o comando) e a segunda (o argumento)
if (partes[0].equals(Protocol.CMD_JOGAR)) {
    Card cartaJogada = Protocol.parseCard(partes[1]);
    
    // Passa a carta tratada para o GameState aplicar a regra
}
```