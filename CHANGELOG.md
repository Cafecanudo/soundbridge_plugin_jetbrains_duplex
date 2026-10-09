# Changelog

## [Unreleased]

### Adicionado
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
