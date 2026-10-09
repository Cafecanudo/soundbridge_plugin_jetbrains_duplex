# SoundBridge Duplex — plugin JetBrains

Plugin para IDEs JetBrains que envia arquivos e pastas por cabo de áudio usando o SoundBridge. O
plugin não reimplementa o transceptor: executa o CLI do SoundBridge, que já deve estar instalado, e lê
os eventos `--json`.

> **Em desenvolvimento.** Fases 1 a 3: esqueleto, execução do CLI e tela de configuração por projeto.

## Configuração

*Settings > Tools > SoundBridge*, separada por projeto (fica em `.idea/workspace.xml`, que não vai para o git):

- **Comando**: como executar o SoundBridge nesta máquina (padrão `python -m soundbridge`). **Testar**
  mostra a versão encontrada.
- **Saída e entrada**: as placas usadas no envio. Vazio usa o padrão do SoundBridge na máquina.
- **Pasta de recepção**: padrão, a pasta do projeto; editável.

A calibração é a única configuração global: vale para o par de placas, em qualquer projeto.

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
