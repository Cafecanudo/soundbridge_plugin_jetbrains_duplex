# Changelog

## [Unreleased]

### Corrigido
- **Receber** logo depois de **Parar** era ignorado sem aviso enquanto o processo anterior ainda terminava (e as
  opções novas, como **Copiar para a memória**, se perdiam); agora espera o anterior sair e liga com as novas.
- **Copiar para a memória** pode ser marcado com o Recebimento ligado: ele reinicia na hora com a opção nova (durante
  um envio, a opção vale na retomada).

## [1.0.19-SNAPSHOT] — 2026-10-21

### Adicionado
- **Usar memória** abaixo de Itens: detecta arquivos, print ou texto na área de transferência (também copiados fora da
  IDE) e os coloca nos itens; print e texto vão como `sb-memoria.png` e `sb-memoria.txt`.
- Texto selecionado no editor é enviado como `sb-memoria.txt` (SoundBridge → Enviar no botão direito do editor, ou
  Alt+Shift+K, E). O menu SoundBridge passa a aparecer também no editor.
- **Copiar para a memória** no Recebimento (`--copy-memory`, sb-duplex 1.4.0b5): o recebido vai para a área de
  transferência; log e notificação dizem se foi texto, imagem ou arquivo.

### Alterado
- Lista de Itens com uma linha só quando vazia, com a dica de onde escolher os itens.

## [1.0.18-SNAPSHOT] — 2026-10-21

### Adicionado
- Indicador do Recebimento na barra de status da IDE: ponto verde (escutando), azul (recebendo), amarelo (pausado
  durante um envio), roxo (calibrando) e cinza (parado); tooltip com modo e pasta; clique abre o Recebimento.

## [1.0.17-SNAPSHOT] — 2026-10-21

### Alterado
- Mudar a **Pasta de recepção** no Settings com o Recebimento ligado para o Recebimento (antes ele seguia na pasta
  antiga, inclusive ao retomar depois de um envio) e avisa; o campo Pasta do Recebimento passa a mostrar a nova.
- Texto dos parâmetros do Recebimento bi sem calibração deixa claro que a calibração é opcional.

## [1.0.16-SNAPSHOT] — 2026-10-20

### Removido
- Marcação dos itens ignorados no Project (texto em cinza e rótulo *SoundBridge: ignorado*); a lista segue na aba
  Ignorados do Histórico e no formulário de envio.

## [1.0.15-SNAPSHOT] — 2026-10-20

### Adicionado
- **Ignorar no envio** no menu SoundBridge (e Alt+Shift+K, I): arquivos e pastas marcados nunca são enviados nem
  verificados, mesmo selecionados direto ou por pasta; **Não ignorar mais** desfaz. Filtro na hora do envio, com linha
  **Ignorado** no Histórico e contagem na notificação; exclusões de ignorados também são puladas e o aviso de não
  enviados do commit os desconsidera. Itens ignorados aparecem em cinza no Project e marcados no formulário.
  Nova aba **Ignorados** no Histórico. A lista fica nas configurações do projeto (sem arquivo no projeto).
- Barra **Total** no envio (todos os itens, ponderada pelo tamanho, com contagem e falhas), além da barra do arquivo
  atual. O indicador da barra de status da IDE passa a mostrar o total, com o item atual no texto (*3/10 · Main.java 45%*).
- Linha **Sequência** no Recebimento com os recebidos e as falhas da sequência atual (ou da última).

### Alterado
- Recebimento sem notificação por arquivo: numa sequência (`--more`), só as falhas notificam na hora e o fim traz um
  resumo com recebidos e falhas. Arquivo avulso continua com a notificação de antes.

## [1.0.14-SNAPSHOT] — 2026-10-20

### Adicionado
- Atalhos em dois toques com **Alt+Shift+K** (⌥⇧K no macOS): **E** Enviar, **V** Verificar, **R** Receber / Parar
  (com as opções salvas do Recebimento) e **H** Histórico. Enviar e Verificar valem para a seleção do Project ou para
  o arquivo aberto no editor.

### Adicionado
- Dialog do resultado da verificação com checkbox nos itens *não existe* e *diferente* e **Marcar faltantes e
  diferentes**; com algum marcado, o OK vira **Reenviar (n)** e leva os arquivos ao formulário de envio. Numa
  verificação recebida (?↓) o dialog continua só de leitura.

## [1.0.13-SNAPSHOT] — 2026-10-20

### Adicionado
- Histórico com seleção múltipla: **Reenviar** junta os itens de várias linhas (envios, recebidos e verificações),
  sem repetir; com mais de uma linha valem as opções atuais do formulário. **Enviar faltantes e diferentes** (botão
  direito) manda só o que as verificações selecionadas marcaram como *não existe* ou *diferente*.

## [1.0.12-SNAPSHOT] — 2026-10-20

### Adicionado
- **Verificar** (`--verify`, sb-duplex 1.4.0b4): botão ao lado de Enviar e item **SoundBridge → Verificar** no
  botão direito do Project. Pastas são percorridas e os caminhos vão relativos à Pasta de recepção, em lotes de até
  24 mil caracteres. No bi, a linha do Histórico (ícone ?) vai de *Solicitando* para o resultado e abre um dialog com
  cada arquivo; no uni fica *Solicitado* e uma notificação manda conferir no destino. O Recebimento registra a
  verificação recebida no Histórico com o resultado; duplo clique ou **Ver resultado** abre o dialog.

### Alterado
- O menu do Project vira o submenu **SoundBridge** com **Enviar** e **Verificar**.

### Corrigido
- Um Recebimento que recebia uma verificação criava uma linha vazia de arquivo recebido no Histórico.

## [1.0.11-SNAPSHOT] — 2026-10-16

### Alterado
- O destino (`--dest` e campo Destino) passa a ser calculado a partir da **Pasta de recepção** do Settings, e não
  mais da raiz do projeto da IDE: com a pasta `c:/projetos/frontend`, o arquivo
  `c:/projetos/frontend/src/nome/Main.java` vai com `--dest src/nome`. Sem pasta configurada, vale a do projeto;
  arquivo fora da pasta vai sem `--dest` (raiz do RX).

## [1.0.10-SNAPSHOT] — 2026-10-16

### Alterado
- Formulário de envio mostra para onde cada arquivo vai: com destino por item (pastas diferentes ou pasta com
  subpastas), cada linha da lista de itens traz o destino (→ src/app, → (raiz)); o campo Destino vazio mostra a
  dica "raiz da pasta de recepção (sem --dest)".

## [1.0.9-SNAPSHOT] — 2026-10-16

### Corrigido
- O log do Envio é limpo no início de qualquer envio (botão Enviar, menu sem janela, Reenviar, Commit), não só
  pelo botão do formulário.
- O log do Recebimento é limpo ao clicar em Receber ou Calibrar (a retomada automática depois de um envio
  continua sem limpar).

## [1.0.8-SNAPSHOT] — 2026-10-16

### Alterado
- Coluna **Arq** removida do Histórico (com uma linha por arquivo ela ficou redundante).

## [1.0.7-SNAPSHOT] — 2026-10-16

### Adicionado
- Coluna **#** no início do Histórico com o número da linha (1 = mais recente).

## [1.0.6-SNAPSHOT] — 2026-10-16

### Adicionado
- **Enviar os arquivos de uma pasta um a um** no Settings (ligado por padrão): ligado, a pasta é listada e cada
  arquivo vai num comando; desligado, a pasta vai num único `--tx` e vira uma linha só no Histórico (com os
  arquivos somados).
- **Intervalo entre comandos (ms)** no Settings (padrão 1000, até 60000): espera entre um `--tx` e o próximo da
  mesma fila; o Cancelar interrompe a espera.
- **Notificações do sistema operacional** em *Settings > Tools > SoundBridge* (por projeto, ligada por padrão):
  desmarcada, os avisos do SoundBridge ficam só dentro da IDE, mesmo com ela fora de foco.

## [1.0.5-SNAPSHOT] — 2026-10-16

### Alterado
- **Um comando por arquivo:** cada arquivo (e cada exclusão) vai num `--tx` próprio, encadeado com `--more`;
  nenhum comando leva vários arquivos. Sai a divisão de listas longas, que deixou de ser necessária.
- Pasta selecionada aparece na lista do formulário já expandida em todos os seus arquivos (recursivo). O campo de
  subpasta continua editável quando todos estão na mesma pasta.

### Corrigido
- Modo de envio: além do clique em Uni/Bi, o modo usado em cada envio fica gravado no projeto.

## [1.0.4-SNAPSHOT] — 2026-10-16

### Alterado
- Histórico com **uma linha por arquivo** enviado, mesmo enviando vários ou uma pasta: o plugin expande as pastas
  em arquivos antes de montar os comandos e casa cada resultado do CLI com o seu arquivo (status, tamanho, tempo,
  MCS e log só daquele arquivo; destino do grupo gravado na linha). Cada exclusão também vira uma linha. Arquivos
  não alcançados ficam FALHA (ou CANCELADO). O Reenviar de uma linha reenvia só aquele arquivo, no mesmo destino.

### Corrigido
- Nomes com `[`, `*` ou `?` (ex.: `[id].ts`) eram tratados como curinga pelo CLI e falhavam; o plugin agora os
  escapa nos envios.
- Listas longas (pasta com muitos arquivos) são divididas em vários comandos (até 8 mil caracteres por `--tx`),
  dentro do limite de linha de comando do Windows.

## [1.0.3-SNAPSHOT] — 2026-10-16

### Corrigido
- Modo de envio: a escolha do usuário (Uni/Bi) é gravada no projeto e prevalece; o formulário não volta mais
  sozinho para o bidirecional ao abrir a Tool Window ou trocar os itens. O envio direto (sem janela) usa a mesma
  escolha.
- Reenviar do Histórico: o modo e o MCS do envio original não são mais sobrescritos ao abrir a Tool Window; com
  Envio automático ligado, o reenvio começa sozinho.
- Falha no Solicitar reenvio mostra o motivo (última linha do SoundBridge) na notificação, em vez de só pedir
  para conferir comando e cabos.

## [1.0.2-SNAPSHOT] — 2026-10-16

### Adicionado
- **Solicitar reenvio** no menu de botão direito do Histórico, para qualquer arquivo recebido (com OK ou falha):
  o plugin manda `--tx NOME --dest SUB --request`, retoma (ou liga) o Recebimento e, quando o arquivo volta,
  **atualiza a mesma linha** do Histórico; sem resposta em 2 min, avisa. O menu também tem Reenviar para os
  enviados. No outro PC, **Atender pedidos de reenvio** no Recebimento (bi, ligado por padrão) passa `--serve`.
  O Histórico guarda o caminho do manifesto (`remote_name`); entradas antigas usam o caminho relativo à pasta de
  recepção. Exige `sb-duplex` 1.4.0b1 (`--request`/`--serve`).

## [1.0.1-SNAPSHOT] — 2026-10-16

### Adicionado
- **Versões** no topo de *Settings > Tools > SoundBridge*: versão do plugin (gravada no build pelo Gradle) e do
  SoundBridge (`--version`, conferida ao abrir e no Testar; "não encontrado" se o comando falhar).
- **Calibrar** no Recebimento, ao lado de Receber: escuta uma calibração iniciada no outro PC e para sozinho no
  fim. Uni mede só a ida (mono, banda, pico, MCS) com **Copiar parâmetros** para o TX do outro PC; bi responde à
  calibração completa e mostra chegada, saída (com os testes) e o perfil onde foi gravada. A escuta normal
  (Receber) também mostra a última calibração recebida. No bidirecional o RX passa `--profile` (o
  `--calibrate-save` do outro PC grava neste perfil). Exige `sb-duplex` 1.4.0b1 para os detalhes do bidirecional (`choice` em JSON).
- **Não mostrar novamente** na janela do aviso no commit (vale ao comitar ou enviar; Cancelar ignora); para voltar
  a mostrar, *Settings > Tools > SoundBridge > Avisar no commit alterações não enviadas*.
- Aviso no commit de alterações não enviadas (`CheckinHandlerFactory`, na dependência opcional do VCS): cada
  comando de envio OK grava o SHA-256 de cada arquivo enviado (pastas expandidas) ou "apagado"; arquivos
  recebidos também entram. No commit, uma janela com a árvore de alterações (`SimpleAsyncChangesBrowser`, com
  checkbox) mostra todas as alterações do projeto, com os não versionados, e marca as não enviadas do commit: Enviar e comitar, Comitar assim mesmo e Cancelar. O
  menu do Commit abre a mesma janela com todas as alterações do projeto e a seleção já marcada. Opção em *Settings > Tools > SoundBridge*, ligada por padrão e ativa só com a
  saída configurada.
- **SoundBridge Enviar** no menu das alterações da Tool Window Commit e do Local Changes (dependência opcional
  `com.intellij.modules.vcs`): novos e modificados são enviados; apagados viram `--delete` no RX; renomeados ou
  movidos apagam o caminho antigo e enviam o novo. Exclusões primeiro (renomeação só de maiúsculas no
  Windows), agrupadas por destino, com destino sempre por item. Formulário e Histórico mostram os apagados com
  ✕; o Reenviar repete as exclusões. Exige `sb-duplex` 1.4.0b0.
- Perfil do SoundBridge no Settings (lista **Perfil** acima de **Entrada** e **Saída**): o plugin não cria perfis,
  só usa os existentes (`--save-profile` no SoundBridge). Escolher um perfil preenche entrada e saída; o envio
  passa `--profile NOME` (o RX escolhe a pasta pelo perfil). O leitor do `--list-devices` aceita o `schema` 2
  (`profiles`, sem `defaults`) e continua aceitando o 1. Exige `sb-duplex` 1.3.0b0 para listar e usar perfis.
- Recebimento com opções na própria janela: modo (uni/bi), placas do Settings com Configurar…, pasta
  editável (grava a pasta de recepção do projeto), **Mostrar comando** e **Mostrar log**. No bidirecional, passa
  `--ack-peak` da calibração bidirecional das mesmas placas.
- **Recebimento**: Receber/Parar roda `--rx <pasta do projeto> --keep` (com `--out` no
  modo bidirecional), com estado, progresso e log (Mostrar log). O RX pausa durante um envio ou uma
  calibração e retoma no fim. Cada arquivo recebido entra no Histórico (coluna de direção: ↑ enviado,
  ↓ recebido), atualiza a árvore do Project e gera a notificação "Recebido: arquivo · tempo".
- Três Tool Windows visíveis ao mesmo tempo: **SoundBridge** (envio, à direita em cima), **SoundBridge
  Recebimento** (à direita embaixo, `secondary`) e **SoundBridge Histórico** (painel inferior). Reenviar abre o
  SoundBridge com o formulário preenchido; o Detalhes de um recebido abre o Recebimento. A caixa Mostrar
  histórico deixa de existir.
- Fase 7: histórico de envios na Tool Window (por projeto, últimas 200 entradas, ocultável com **Mostrar
  histórico**). Uma linha por envio: data, itens, modo, arquivos OK/total, tamanho, MCS final, duração e
  status (OK, Falha, Cancelado). **Reenviar** carrega itens, modo, destino, compressão, MCS e Substituir no
  formulário (itens apagados ficam de fora, com aviso); **Ver log** mostra as últimas 200 linhas daquele
  envio; **Limpar** apaga o histórico, com confirmação.
- Alertas de envio com o link **Detalhes**, que abre a Tool Window.
- Balão de alerta (envio e calibração) some após 5 s; o alerta fica no painel Notifications da IDE.
- Caixa **Mostrar esta janela ao enviar pelo menu** (por projeto, marcada por padrão). Desmarcada, o menu
  envia direto, sem abrir a Tool Window, com o modo calibrado, o destino pela pasta do item, o MCS do
  Settings, a última compressão (agora gravada por projeto) e o Substituir se existir.
- Comando e log da Tool Window ocultos por padrão, com as caixas **Mostrar comando** e **Mostrar log** (por
  projeto).
- Caixa **Substituir se existir** na Tool Window (por projeto), acima dos itens: passa `--force-repl` e o RX
  substitui o arquivo de mesmo nome em vez de gravar `nome (1).ext` (exige sb-duplex 1.2.0b4 no RX).
- Fase 6.1: caixa **Enviar automaticamente ao selecionar** na Tool Window (por projeto). Marcada, o SoundBridge
  Enviar do menu de contexto já inicia o envio; sem modo calibrado ou com envio/calibração em andamento,
  não envia e avisa por notificação.
- Fase 6.1, ajustes da Tool Window: título **SoundBridge**, aberta por padrão do lado direito, sem o
  botão Limpar; barra de progresso verde, 100% ao concluir cada arquivo e vermelha em falha ou
  cancelamento; tempos em h:mm:ss (alerta, log e tempo restante); alertas de envio e calibração também
  como notificação do sistema operacional quando a IDE não está em foco.
- Fase 6: envio pela Tool Window. **Enviar** roda o SoundBridge em background (barra de status), com
  **Cancelar**, barra de progresso (fração, KB/s, tempo restante, arquivo atual e contador), log e
  notificação no fim. Itens em pastas diferentes viram um comando por destino, em sequência, todos menos o
  último com `--more` (a estrutura do projeto chega igual do outro lado). Envio e calibração não rodam ao
  mesmo tempo (mesma placa).
- Fase 5: ação **SoundBridge Enviar** no menu de contexto do Project (arquivos, pastas, seleção múltipla) e
  Tool Window **SoundBridge** (lado direito) com o formulário de envio: itens; modo unidirecional ou
  bidirecional, habilitado só quando calibrado; parâmetros do modo (perfil uni digitado ou calibração bi
  das mesmas placas); destino (pasta do item a partir da raiz do projeto, editável); compressão; MCS
  (o do Settings, automático ou fixo); e o comando que será executado, com `--tx` e `--dest` entre aspas.
  O envio em si entra na fase 6.
- Opção "Bidirecional" na calibração (por projeto). Desmarcada: só a seção Unidirecional (e a entrada some da
  tela). Marcada: Unidirecional e Bidirecional. Unidirecional: botão "Calibrar só a ida" (`--calibrate-uni`,
  sondagem pela saída) e campos mono, banda, pico e MCS para digitar os valores mostrados no PC que recebe.
- Calibração na tela *Settings > Tools > SoundBridge* (fase 4): tamanhos de teste (2k a 10mb), validar com
  transferências e gravar como perfil; roda em background na barra de status, com cancelamento, log ao vivo
  na tela e notificação com o resumo no fim. Opções e perfil (sentido este PC → outro) gravados por projeto;
  o perfil indica quando foi feito com outras placas. O plugin nunca usa `--calibrate-save`: o perfil fica
  só no projeto, sem mexer nos padrões do SoundBridge em nenhum dos PCs.
- Esqueleto do plugin: Gradle 9.7.1, IntelliJ Platform Gradle Plugin 2.19.0, Java 17.
- `plugin.xml` com dependência só de `com.intellij.modules.platform` (todas as IDEs JetBrains).
- Compatível a partir da 2023.3 (`since-build` 233), sem `until-build`.
- Plugin Verifier configurado para a versão mínima e a mais recente.
- Camada `cli`: executa o CLI do SoundBridge (comando configurável, padrão `python -m soundbridge`) com
  `--json` e UTF-8 forçado, remonta as linhas de stdout/stderr, converte as linhas JSON em `CliEvent`
  (schema 1), entrega texto livre à parte, mapeia o código de saída e cancela o processo.
- Testes JUnit 5 da camada `cli`, incluindo um processo real simulado (`FakeCli.java`).
- *Settings > Tools > SoundBridge*, **por projeto** (gravado em `.idea/workspace.xml`, fora do git): comando do
  SoundBridge com botão Testar; saída e entrada (listas lidas do SoundBridge, editáveis; vazio usa o padrão
  do SoundBridge na máquina); pasta de recepção (padrão: a pasta do projeto, editável, com atalho para voltar
  a ela). Chamadas ao CLI fora da thread de interface; erros exibidos na própria tela.
- Só a calibração continua global: o perfil é do par de placas, não do projeto.
- Textos da interface em `messages/SoundBridgeBundle.properties`, sem nomes de parâmetros da linha de comando.

### Alterado
- Histórico: a Tool Window aparece por padrão no painel inferior; direção com ícone (upload/download) numa coluna
  estreita; coluna Data removida; **Itens** lista todos os arquivos e ocupa o espaço livre; colunas renomeadas e
  estreitas (Mod, Arq, Tam, MCS, Duração, Status); todas redimensionáveis pelo usuário.
- Barra de progresso sem `ColorProgressBar` (obsoleta na 2026.2): cores do tema (`ProgressBar.passedColor` e
  `ProgressBar.failedColor`), com o verde e o vermelho anteriores como reserva.

### Corrigido
- Compilação Java em UTF-8 (o javac usava windows-1252 e corrompia textos acentuados).
