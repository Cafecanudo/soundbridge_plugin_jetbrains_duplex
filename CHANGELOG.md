# Changelog

## [Unreleased]

### Adicionado
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

### Corrigido
- Compilação Java em UTF-8 (o javac usava windows-1252 e corrompia textos acentuados).
