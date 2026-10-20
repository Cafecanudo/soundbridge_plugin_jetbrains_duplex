# Roadmap — SoundBridge Duplex (plugin JetBrains)

O plugin é uma camada fina sobre o CLI do SoundBridge (`python -m soundbridge`, pacote `sb-duplex`): executa
o CLI com `--json` e lê os eventos (`schema` 1). Nada do áudio é reimplementado.

## Fases

| Fase | Entrega | Estado |
|---|---|---|
| 1 | Esqueleto: Gradle, IntelliJ Platform Gradle Plugin 2.x, Java 17, `since-build` 233, Plugin Verifier | ✅ |
| 2 | Camada `cli`: execução, leitura dos eventos JSON, código de saída, cancelamento | ✅ |
| 3 | *Settings > Tools > SoundBridge* por projeto: comando, saída, entrada, pasta de recepção | ✅ |
| 4 | Calibração no Settings: unidirecional (`--calibrate-uni`, valores digitados) e bidirecional (perfil do projeto) | ✅ |
| 5 | Menu **SoundBridge Enviar** e formulário de envio | ✅ |
| 6 | Envio em background com progresso, log, cancelamento e fila por destino (`--more`) | ✅ |
| 6.1 | Envio automático, envio direto sem janela, Substituir se existir, alertas (5 s, Detalhes, notificação do SO) | ✅ |
| 7 | Histórico por projeto (Reenviar, Ver log, Limpar); Recebimento (`--rx --keep`, pausa durante envio); três Tool Windows | ✅ |
| 7.1 | Menu no Commit/Local Changes: envia alterados e apaga no RX os apagados (`--delete`, sb-duplex 1.4.0b0); falta teste no cabo | ✅ |
| 7.2 | Aviso no commit de alterações não enviadas (registro por SHA-256 do enviado/recebido; Enviar e comitar) | ✅ |
| 7.3 | Verificar (`--verify`): botão, submenu SoundBridge (Enviar/Verificar), lotes, dialog do resultado, Histórico com ?; falta teste no cabo | ✅ |
| 8 | Distribuição do plugin | ⏳ |
| 9 | Skill do Claude para o SoundBridge | ⏳ |

## Pendências antes da fase 8

- Testes no cabo: Recebimento (PC2 → PC1) e Substituir se existir (exige `sb-duplex` 1.2.0b4 no RX do PC2).

## Fase 8 — Distribuição do plugin

A decidir: JetBrains Marketplace (recomendado), repositório próprio (`updatePlugins.xml`) ou `.zip`.

1. Versão `1.0.0-beta.1`, tag por versão e CHANGELOG fechado.
2. Página do plugin: descrição em inglês, notas de mudança a partir do CHANGELOG, `pluginIcon.svg`, vendedor.
3. Assinatura (Marketplace): certificado e chave em variáveis de ambiente, nunca no repositório.
4. `docs/PUBLICACAO.md` com o roteiro, como no `sb-duplex`.

## Fase 9 — Skill do Claude para o SoundBridge

> **A revisar com os perfis de enlace (sb-duplex 1.3.0b0).** Os padrões globais deixam de existir: a skill passa
> a operar só com `--profile <nome>`, no formato de uso "/soundbridge --profile casa --fileDir c:/arquivo.zip",
> enviando um ou vários arquivos e/ou pastas. Os itens abaixo que citam padrões (`--save-defaults`, bloco
> `defaults`) serão reescritos para perfis na discussão da skill.

Skill pessoal em `~/.claude/skills/sb-duplex/` que **só envia e recebe**, sempre com os padrões do
SoundBridge da máquina (`--save-defaults`). Especificação aprovada (antes dos perfis):

1. **Pré-checagem:** `python -m soundbridge --list-devices --json` e leitura de `defaults`.
   - Enviar (uni) exige `out`; enviar bi (só a pedido) exige `out` e `in`; receber exige `in` (`dir` opcional:
     sem ele grava na pasta atual, com aviso).
   - Faltando padrão: não executa nada e responde com o comando para configurar
     (`--save-defaults --out ... --in ... --dir ...`) e a dica de `--list-devices`.
2. **Enviar:** `python -m soundbridge --tx "<itens>" --json` (uni; bi só se o usuário pedir). O perfil de
   calibração salvo na máquina é aplicado pelo próprio SoundBridge.
3. **Receber:** `python -m soundbridge --rx --json` (um arquivo); "fica recebendo" vira `--rx --keep` em
   background até o usuário mandar parar.
4. **Resumo (`sb_run.py`):** roda o CLI e devolve só arquivo, OK/FALHA, tempo, MCS final e avisos; o log
   completo vai para um arquivo temporário.
5. **Autorização:** o pedido do usuário é a autorização para tocar áudio; a skill nunca envia nem recebe por
   iniciativa própria.
6. **Versão mínima:** `sb-duplex` 1.2.0b2 (contrato `--json` com `schema`); avisa se a instalada for anterior.

## Backlog (em discussão)

- **Extensão para o VS Code:** mesma arquitetura (TypeScript, `child_process` + `--json`), um contêiner com
  três views empilhadas (Envio, Recebimento, Histórico), estado em `workspaceState`; distribuição na VS Code
  Marketplace e na Open VSX.
- Ideias de transferência: envio só da diferença (delta por blocos, exige bi), pasta espelhada, trecho do
  editor, "já tenho" por hash, compressão LZMA, mensagens curtas entre IDEs.
