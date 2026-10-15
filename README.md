# Coleta Fácil — Android

Aplicativo para ler QR Codes/códigos de barras, digitar o nome de cada item e registrar manualmente se ele confere com o Kronos da empresa. Exporta CSV e XLSX com exatamente **Código**, **Nome** e **Confere no Kronos?**.

## Versão 1.1.0

Cada leitura abre um formulário de conferência; o item só é salvo ao confirmar. Inclui edição e exclusão de coletas e itens, restauração por Desfazer, detecção de repetidos e contagem por código. A revisão das dez heurísticas de Nielsen está em [docs/UX-1.1.md](docs/UX-1.1.md).

O APK exige Android 8.0 ou superior. O workflow gera o artefato **ColetaFacil-APK-debug**, contendo `app-debug.apk`. Compilação, lint, exportações e **8 testes instrumentados (zero falhas)** passaram. Consulte [docs/VALIDACAO-1.1.md](docs/VALIDACAO-1.1.md) para a execução validada e a assinatura do APK.

## Gerar e instalar o APK

Você precisa de um computador com Android Studio e JDK 17.

1. Clone este repositório e abra a pasta do projeto no Android Studio.
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

O arquivo `.github/workflows/android.yml` está publicado e foi executado com sucesso neste repositório. A cada push de código, ele compila o APK e disponibiliza o artefato **ColetaFacil-APK-debug**. Ele executa compilação, lint, testes do núcleo e testes instrumentados de Room/Compose em emulador Android API 30. A câmera e a vibração físicas precisam de validação em aparelho.

## Como usar

1. Toque em **Nova coleta** e dê um nome à lista.
2. Abra **Escanear**, permita a câmera e aponte para o código. A detecção vibra e pausa a leitura; nenhum registro é salvo ainda.
3. Digite o nome obrigatório, escolha **Pendente**, **Sim** ou **Não** para a conferência manual no Kronos e toque em **Confirmar item**. Cancelar não adiciona o item.
4. Se o código já existir nessa coleta, abra o item existente ou marque **Registrar outra unidade deste código** e confirme. Cada unidade confirmada ocupa uma linha. Retire o código do enquadramento por pelo menos 600 ms antes de ler a mesma etiqueta novamente.
5. Em **Registros**, use busca e filtros, incluindo **Repetidos**. Os contadores distinguem registros, códigos únicos e unidades adicionais.
6. Use **Editar** ou **Excluir** nos menus de três pontos das coletas e dos itens. Alterações só são gravadas ao confirmar. Exclusões identificam o alvo, pedem confirmação e oferecem **Desfazer**; uma coleta restaurada recupera também seus itens.
7. Toque em **Finalizar e exportar**, escolha XLSX ou CSV e selecione o destino. A coleta só é finalizada após a gravação do arquivo. Cancelar o seletor mantém a coleta como estava.
8. Editar ou excluir itens de uma coleta finalizada reabre a coleta para revisão. Arquivos exportados anteriormente não mudam; exporte novamente após corrigir.

**Pendente** continua permitido. Registros sem nome existentes na versão anterior podem ser revisados ou exportados; novas confirmações exigem nome. Exportar uma lista filtrada sempre inclui **todos os registros da coleta**. A vibração depende do aparelho e de suas configurações. Há ajuda no próprio aplicativo.

## CSV ou XLSX?

- **XLSX (recomendado):** Código e os outros campos são células de texto. Zeros iniciais e códigos longos são preservados; fórmulas capturadas são texto literal. Cabeçalho formatado, primeira linha congelada e filtros.
- **CSV:** UTF-8 com BOM, separador `;`, aspas e quebras de linha escapadas. Ao importar no Excel, defina a coluna Código como **Texto** para preservar zeros e códigos longos. Campos que poderiam iniciar uma fórmula recebem apóstrofo protetor no CSV. O conteúdo original permanece no banco e no XLSX.
- O Excel aceita no máximo 32.767 caracteres por célula. Campos incompatíveis com esse formato produzem mensagem de erro; a coleta não é apagada.

## Dados e permissões

As coletas ficam no banco local do app. Não há conta nem integração automática com o Kronos. A câmera usa o modelo ML Kit incorporado. O manifesto remove permissões de rede herdadas; a operação principal não depende de conexão. O usuário escolhe o destino pelo seletor de arquivos do Android, sem permissão ampla de armazenamento.

**Atualização do APK de teste:** a assinatura de debug gerada por cada execução pode mudar. Se o Android recusar a atualização por incompatibilidade, exporte todas as coletas da versão instalada antes de considerar desinstalá-la. A nova versão não importa essas planilhas automaticamente.

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

Os testes instrumentados cobrem persistência, edição, exclusão/restauração, confirmação, cancelamento e duplicidade. Resultados por versão estão em docs/VALIDACAO-1.1.md. Câmera/vibração reais, TalkBack, fonte máxima e o uso prolongado precisam de validação em aparelho.

## Estrutura

- `app/`: interface Kotlin/Compose, CameraX/ML Kit, ViewModel e persistência Room.
- `core/`: controle de repetição e serialização CSV/XLSX em Java, testáveis sem Android.
- `tools/`: testes e verificação independente das planilhas.
- `design-system/MASTER.md`: decisões visuais e uso do UI UX Pro Max solicitado.
- `docs/`: especificação, plano, andamento e relatório de validação.

Gradle Wrapper oficial obtido de `gradle/gradle`, tag `v8.11.1`. UI UX Pro Max: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill (diretrizes consultadas; não é dependência do aplicativo).
