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

Botão direito em arquivos ou pastas no Project → **SoundBridge → Enviar**. A Tool Window **SoundBridge** abre com os
itens, o modo e os parâmetros do projeto; o formulário ajusta subpasta no destino, compressão e MCS só para
aquele envio e mostra o comando que será executado. **Enviar** roda em background, com progresso, log e
**Cancelar**. Uma pasta selecionada aparece na lista com todos os seus arquivos (recursivo), e **cada arquivo é
enviado num comando próprio** (encadeados com `--more`), mantendo a estrutura do projeto; o Histórico tem uma
linha por arquivo.
O progresso tem duas barras: **Progresso** (arquivo atual, com velocidade e tempo restante) e **Total** (todos os
itens, ponderado pelo tamanho, com *3/10 item(ns)* e as falhas). Na barra de status da IDE, o indicador da tarefa
mostra o total e o texto ao lado traz o item atual (*3/10 · Main.java 45%*). No fim do envio sai uma notificação
só, com os enviados e as falhas.

O histórico (por projeto, últimas 200 entradas) permite reenviar com as mesmas opções e ver o log de cada envio.
Com Ctrl/Shift dá para selecionar várias linhas (envios, recebidos e verificações) e **Reenviar** tudo de uma vez:
os itens são juntados sem repetir, os caminhos das verificações são resolvidos na Pasta de recepção e, com mais de
uma linha, valem as opções atuais do formulário (com uma só, as daquele envio). **Enviar faltantes e diferentes**
(botão direito) manda só os arquivos que as verificações selecionadas marcaram como *não existe* ou *diferente*.

## Verificação

**Verificar** (ao lado de Enviar no formulário, ou **SoundBridge → Verificar** no botão direito do Project)
confere no outro PC se cada arquivo existe e é igual ao daqui (hash), sem gravar nada (`--verify`). Pastas são
percorridas e cada arquivo vai na lista com o caminho relativo à **Pasta de recepção** do Settings (a mesma base
do `--dest` do envio); itens fora dela ficam de fora, com aviso. Usa o modo, os parâmetros e o MCS do formulário.

- **bi:** a linha do Histórico (ícone **?**) começa como *Solicitando* e, quando o resultado volta, mostra
  *existe*, *não existe*, *diferente* ou a contagem de cada um; abre também um dialog com cada arquivo.
- **uni:** a linha fica *Solicitado* e uma notificação lembra de conferir o resultado no destino.

No outro PC, o Recebimento registra a verificação no Histórico (ícone **?**) com o resultado. Duplo clique numa
linha de verificação (ou **Ver resultado** no botão direito) abre o dialog. No dialog de uma verificação feita
daqui, os itens *não existe* e *diferente* têm checkbox (com **Marcar faltantes e diferentes** para todos): com algum
marcado, o OK vira **Reenviar (n)** e leva esses arquivos ao formulário de envio, com as opções dele. Numa
verificação recebida o dialog só mostra o resultado. Listas longas vão em lotes (um
`--verify` por lote) para não passar do limite da linha de comando do Windows; o resultado é juntado numa linha
só. Exige `sb-duplex` 1.4.0b4.

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

## Ignorar

**SoundBridge → Ignorar no envio** (botão direito no Project, ou Alt+Shift+K, I) marca arquivos ou pastas que o
SoundBridge nunca envia nem verifica, mesmo selecionados direto ou dentro de uma pasta; pasta ignorada inclui tudo
dentro. No mesmo menu, **Não ignorar mais** desfaz. No formulário o item aparece com *· ignorado*. O filtro vale na hora do envio: os ignorados viram uma linha **Ignorado** no
Histórico, a notificação do fim conta quantos foram, e exclusões (Commit) de itens ignorados também são puladas.
O aviso de alterações não enviadas no commit não considera os ignorados. A lista fica nas configurações do projeto
na IDE (nenhum arquivo é criado no projeto) e aparece na aba **Ignorados** do Histórico, com **Não ignorar mais** e
**Limpar ignorados**.

## Atalhos

Atalhos em dois toques: **Alt+Shift+K** e depois a letra (no macOS, ⌥⇧K). Trocam em *Settings → Keymap*.

| Atalho | Ação |
|---|---|
| Alt+Shift+K, E | Enviar o que estiver selecionado no Project (ou o arquivo aberto no editor) |
| Alt+Shift+K, V | Verificar o selecionado (ou o arquivo aberto) |
| Alt+Shift+K, R | Receber / Parar, com as opções salvas do Recebimento (modo, pasta, atender pedidos) |
| Alt+Shift+K, H | Abrir o Histórico |
| Alt+Shift+K, I | Ignorar no envio / Não ignorar mais o selecionado |

## Recebimento

Na seção **Recebimento** da Tool Window, **Receber** deixa o SoundBridge escutando na pasta de recepção do
projeto (com confirmação no modo bidirecional) até **Parar**. Durante um envio ou uma calibração o recebimento
pausa e retoma sozinho. Cada arquivo recebido aparece no Histórico (ícone de download) e na árvore do Project.
Numa sequência (pasta ou lista enviada de uma vez), só as falhas notificam na hora; no fim sai um resumo
(*5 recebido(s) · 1 falha(s)*), e a linha **Sequência** do Recebimento conta os recebidos e as falhas.

Receber não exige calibração, nem no uni nem no bi: no bi basta a saída configurada, e sem calibração o ACK usa o
pico padrão (com a calibração bidirecional destas placas, o pico vem do perfil). Mudar a **Pasta de recepção** no
Settings com o Recebimento ligado para o Recebimento e avisa; ligue de novo para escutar na pasta nova.

**Calibrar**, ao lado de Receber, escuta uma calibração iniciada no outro PC e para sozinho no fim. No modo
**uni** mede só a ida e mostra mono, banda, pico e MCS, com **Copiar parâmetros** para digitar no TX do outro
PC; no **bi** responde à calibração completa e mostra chegada, saída e o perfil onde foi gravada (com
`--calibrate-save` no outro PC). O resultado fica em "Última calibração".

**Solicitar reenvio** (botão direito num recebido do Histórico, com OK ou falha) pede o arquivo de volta ao outro
PC, que precisa estar com o Recebimento em **bi** e **Atender pedidos de reenvio** marcado. O outro PC espera
alguns segundos e devolve o arquivo; aqui o Recebimento retoma (ou liga sozinho) e a mesma linha do Histórico é
atualizada com o resultado.

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
