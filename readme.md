# SoundBridge Duplex — plugin JetBrains

Plugin para IDEs JetBrains que envia arquivos e pastas por cabo de áudio usando o SoundBridge. O
plugin não reimplementa o transceptor: executa o CLI do SoundBridge, que já deve estar instalado, e lê
os eventos `--json`.

> **Em desenvolvimento.** Fases 1 a 7: esqueleto, execução do CLI, configuração por projeto, calibração (uni e bi), envio, recebimento e histórico.

## Configuração

*Settings > Tools > SoundBridge*, separada por projeto (fica em `.idea/workspace.xml`, que não vai para o git):

- **Comando**: como executar o SoundBridge nesta máquina (padrão `python -m soundbridge`). **Testar**
  mostra a versão encontrada.
- **Saída e entrada**: as placas usadas no envio. Vazio usa o padrão do SoundBridge na máquina.
- **Pasta de recepção**: padrão, a pasta do projeto; editável.

- **Bidirecional**: liga o modo de dois cabos (dados e confirmação). Desmarcado, a tela mostra só o
  unidirecional e esconde a entrada.
- **Calibração unidirecional**: "Calibrar só a ida" toca a sondagem; o PC que recebe mostra mono, banda, pico e
  MCS, que você digita nos campos do projeto.
- **Calibração bidirecional** (com Bidirecional marcado): mede os dois sentidos com o outro PC recebendo;
  opcionalmente valida com transferências de teste e grava o resultado como perfil do projeto (nunca nos padrões do SoundBridge). Roda em background (barra de status), com cancelamento.

## Envio

Botão direito em arquivos ou pastas no Project → **SoundBridge Enviar**. A Tool Window **SoundBridge** abre com os
itens, o modo e os parâmetros do projeto; o formulário ajusta subpasta no destino, compressão e MCS só para
aquele envio e mostra o comando que será executado. **Enviar** roda em background, com progresso, log e
**Cancelar**; itens em pastas diferentes são enviados um destino por vez, mantendo a estrutura do projeto.
O histórico (por projeto, últimas 200 entradas) permite reenviar com as mesmas opções e ver o log de cada envio.

Na Tool Window **Commit** (e em Local Changes), o **SoundBridge Enviar** do menu das alterações abre uma janela
com a árvore de alterações do projeto (como a do Commit, com checkbox), já marcando o que foi selecionado:
arquivos novos e modificados são enviados; **apagados** viram `--delete` (o RX apaga o arquivo de mesmo caminho,
de vez); **renomeados ou movidos** apagam o caminho antigo e enviam o novo. As exclusões vão primeiro e o
destino é sempre o caminho no projeto (campo de subpasta travado). No formulário e no Histórico os itens
apagados aparecem com ✕, e o Reenviar repete as exclusões. Exige `sb-duplex` 1.4.0b0.

No commit, o plugin avisa o que ainda não foi enviado: cada envio OK grava o conteúdo enviado (SHA-256 por
arquivo, ou "apagado"), e cada arquivo recebido também conta. Se uma alteração do commit difere do registro,
a mesma janela mostra todas as alterações do projeto (com os não versionados) e marca as não enviadas do commit, e oferece **Enviar e comitar**
(envia o que estiver marcado), **Comitar assim mesmo** e **Cancelar**, além de **Não mostrar novamente**. Liga e desliga em *Settings > Tools > SoundBridge* (ligado por padrão;
só avisa com a saída configurada). No unidirecional, OK quer dizer enviado, sem confirmação do RX.

## Recebimento

Na seção **Recebimento** da Tool Window, **Receber** deixa o SoundBridge escutando na pasta de recepção do
projeto (com confirmação no modo bidirecional) até **Parar**. Durante um envio ou uma calibração o recebimento
pausa e retoma sozinho. Cada arquivo recebido aparece no Histórico (ícone de download), na árvore do Project e numa
notificação.

## Compatibilidade

- Todas as IDEs JetBrains (depende só de `com.intellij.modules.platform`).
- A partir da 2023.3 (`since-build` 233), sem limite superior.
- Verificado pelo Plugin Verifier na versão mínima e na mais recente (`gradle.properties`).

## Build

Requer JDK 17 (Gradle 9 e toolchain Java 17).

```bash
./gradlew buildPlugin
```

O `.zip` fica em `build/distributions/` e é instalado em *Settings > Plugins > ⚙ > Install Plugin from Disk*.

| Tarefa | Uso |
|---|---|
| `./gradlew test` | Testes unitários |
| `./gradlew runIde` | Abre uma IDE de teste com o plugin carregado |
| `./gradlew verifyPlugin` | Plugin Verifier nas IDEs de `verifyFloorVersion` e `verifyLatestVersion` |
| `./gradlew verifyPluginProjectConfiguration` | Confere a configuração do projeto |
