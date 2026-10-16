# Changelog

## [Unreleased]

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
