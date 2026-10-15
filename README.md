# Coleta Fácil — Android

Aplicativo para ler QR Codes/códigos de barras, digitar o nome de cada item e registrar manualmente se ele confere com o Kronos da empresa. Exporta CSV e XLSX com exatamente **Código**, **Nome** e **Confere no Kronos?**.

## O que foi entregue

Projeto-fonte Android nativo, Gradle Wrapper, testes do núcleo e testes Android, documentação e workflow de geração de APK. **O APK de teste 1.0.0 foi compilado com sucesso pelo GitHub Actions.** Compilação Android, lint e os testes de exportação passaram; a execução em aparelho/emulador ainda não foi validada.

Build: https://github.com/EuDavidev/Coletafacil/actions/runs/38747876293

Em **Artifacts**, baixe **ColetaFacil-APK-debug** e extraia `app-debug.apk`. O APK exige Android 8.0 ou superior. SHA-256 do arquivo APK: `f87417ca7ac90ba6d2398731e8712b81e1f1204791777405f1ec232281fb0ac0`.

## Gerar e instalar o APK

Você precisa de um computador com Android Studio e JDK 17.

1. Extraia o ZIP e abra a pasta `ColetaFacil` no Android Studio.
2. Aguarde a sincronização. Instale o Android SDK Platform 35 quando solicitado.
3. No terminal da pasta, execute:

```bash
bash gradlew :app:assembleDebug
```

No Windows, use:

```powershell
.\gradlew.bat :app:assembleDebug
```

4. O APK será criado em `app/build/outputs/apk/debug/app-debug.apk`.
5. Transfira o APK para o Android e abra-o para instalar, permitindo a instalação por esse aplicativo de origem quando o aparelho solicitar. Também é possível executar diretamente pelo botão Run do Android Studio com um celular conectado.

Android mínimo: **8.0 (API 26)**. A versão debug é para instalação e teste; uma publicação em loja exige assinatura de release própria.

### Gerar no GitHub Actions

O arquivo `.github/workflows/android.yml` está publicado e foi executado com sucesso neste repositório. A cada push de código, ele compila o APK e disponibiliza o artefato **ColetaFacil-APK-debug**. Ele executa compilação, lint e testes do núcleo; os testes de câmera/Compose exigem dispositivo ou emulador.

## Como usar

1. Toque em **Nova coleta** e dê um nome à lista.
2. Abra **Escanear** e permita a câmera. Aponte para o código; cada leitura gravada aparece no contador.
3. Para ler de novo o mesmo código, retire-o do enquadramento por pelo menos um instante (600 ms) e apresente novamente. Repetições intencionais geram linhas separadas.
4. Em **Registros**, toque em um item para digitar **Nome** e escolher **Pendente**, **Sim** ou **Não** em **Confere no Kronos?**. A conferência é feita por você, consultando o Kronos.
5. Registros existentes salvam após uma breve pausa na digitação; **Concluído** também salva antes de fechar. Espere a indicação **Alterações salvas no celular**. Uma entrada manual nova é criada somente em **Adicionar**.
6. Use os filtros **Sem nome**, **Pendentes** e **Não confere** para revisar.
7. Toque em **Finalizar e exportar**, escolha XLSX ou CSV e selecione o destino. A coleta só é finalizada após o arquivo ser gravado com sucesso. Cancelar o seletor mantém a coleta como estava.
8. Para corrigir uma coleta finalizada, toque em **Reabrir coleta**. A exclusão de um item oferece **Desfazer**.

Sem nome e Pendente são permitidos na exportação e são mostrados na revisão. Exportar a partir de uma lista filtrada sempre inclui **todos os registros da coleta**, não apenas os visíveis.

## CSV ou XLSX?

- **XLSX (recomendado):** Código e os outros campos são células de texto. Zeros iniciais e códigos longos são preservados; fórmulas capturadas são texto literal. Cabeçalho formatado, primeira linha congelada e filtros.
- **CSV:** UTF-8 com BOM, separador `;`, aspas e quebras de linha escapadas. Ao importar no Excel, defina a coluna Código como **Texto** para preservar zeros e códigos longos. Campos que poderiam iniciar uma fórmula recebem apóstrofo protetor no CSV. O conteúdo original permanece no banco e no XLSX.
- O Excel aceita no máximo 32.767 caracteres por célula. Campos incompatíveis com esse formato produzem mensagem de erro; a coleta não é apagada.

## Dados e permissões

As coletas ficam no banco local do app. Não há conta nem integração automática com o Kronos. A câmera usa o modelo ML Kit incorporado. O manifesto remove permissões de rede herdadas; a operação principal não depende de conexão. O usuário escolhe o destino pelo seletor de arquivos do Android, sem permissão ampla de armazenamento.

Arquivos exportados ficam no destino escolhido. Desinstalar o aplicativo ou limpar seus dados remove as coletas locais; mantenha as exportações necessárias.

## Validação

Núcleo puro Java, sem SDK Android:

```bash
bash tools/test-core.sh
python3 -m pip install openpyxl==3.1.5
python3 tools/verify-exports.py
```

O primeiro comando executa a lógica real de repetição e exportação, não uma implementação substituta. O segundo script abre os arquivos produzidos com leitores independentes de CSV, XLSX e XML.

No Android Studio/JDK com SDK e dependências:

```bash
bash gradlew :core:check :app:assembleDebug :app:lintDebug
bash gradlew :app:connectedDebugAndroidTest
```

Os testes instrumentados cobrem os dados e a entrada manual. Ainda é necessário validar câmera real, permissões, reinício do aplicativo, cancelamento de exportação, fonte máxima, TalkBack e orientação paisagem. Não se afirma que esses testes foram executados aqui.

## Estrutura

- `app/`: interface Kotlin/Compose, CameraX/ML Kit, ViewModel e persistência Room.
- `core/`: controle de repetição e serialização CSV/XLSX em Java, testáveis sem Android.
- `tools/`: testes e verificação independente das planilhas.
- `design-system/MASTER.md`: decisões visuais e uso do UI UX Pro Max solicitado.
- `docs/`: especificação, plano, andamento e relatório de validação.

Gradle Wrapper oficial obtido de `gradle/gradle`, tag `v8.11.1`. UI UX Pro Max: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill (diretrizes consultadas; não é dependência do aplicativo).
