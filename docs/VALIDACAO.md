# Validação da entrega

## Resultado disponível

Projeto-fonte Android implementado e revisado. **APK de teste 1.0.0 gerado com sucesso no GitHub Actions**, execução https://github.com/EuDavidev/Coletafacil/actions/runs/38747876293, commit `7b2ecf0bc79f5d8b701535a626f660d269753c59`. Compilação Android e lint passaram. Teste de câmera real, emulador e renderização de Compose ainda não foram executados.

APK: pacote `br.com.coletafacil`, versão `1.0.0`, Android mínimo API 26, target API 35, 41.995.511 bytes. ZIP interno, manifesto binário e presença do bloco de assinatura APK conferidos. SHA-256: `f87417ca7ac90ba6d2398731e8712b81e1f1204791777405f1ec232281fb0ac0`.

## Verificações executadas

| Verificação | Resultado |
|---|---|
| Compilação do núcleo Java real com Java 17, destino Java 8 | Aprovada |
| Compilação Android assembleDebug e lintDebug no GitHub Actions | Aprovadas |
| Testes do núcleo | 21 asserções aprovadas |
| CSV aberto pelo leitor independente Python csv | Aprovado |
| XLSX aberto pelo openpyxl | Aprovado |
| Estrutura XML de todas as partes XLSX | Aprovada |
| Zeros iniciais, códigos longos e fórmulas como texto no XLSX | Aprovados |
| CSV com acentos, aspas, ponto e vírgula, quebra de linha e proteção de fórmulas | Aprovado |
| Coleta vazia no serializador XLSX | Aprovada |
| Controle de repetição, remoção por 600 ms, falha de salvamento e estado por coleta | Aprovado |
| XML do manifesto/recursos e estrutura YAML do workflow | Aprovados |
| 16 pares de contraste explícitos dos temas claro e escuro | Pelo menos 4,5:1 |
| Integridade ZIP do Gradle Wrapper oficial | Aprovada |
| git diff --check | Aprovado |

## Revisão independente e correções

Uma revisão independente examinou o projeto completo, incluindo fluxo de edição, exportação, cancelamento, callbacks da câmera e integração das APIs. Não identificou erro de compilação por inspeção; isso não substitui a compilação Android.

- Correção importante: o estado que impede duplicações pertencia à prévia da câmera e era perdido ao pausar. Agora pertence à coleta no ViewModel. O teste de regressão falhou antes da correção e passou depois. Coleções diferentes mantêm estados independentes.
- A disposição da prévia limpa reservas de leitura interrompidas. Uma leitura já reconhecida termina sua gravação e registra a confirmação junto ao controle de repetição, mesmo que a prévia seja fechada durante a gravação.
- Ajuste de recursos: o compressor ZIP usado na exportação XLSX é fechado de forma determinística, mantendo a propriedade do stream de destino com o chamador. Os arquivos resultantes foram revalidados após o ajuste.

## Limite local e validação na nuvem

O SDK Android não estava instalado. A tentativa de `bash gradlew :app:assembleDebug` não conseguiu baixar o Gradle 8.11.1: `java.net.SocketException: Network is unreachable`. Pedidos aos servidores de dependências pelo proxy configurado também expiraram. Não houve tentativa de contornar a restrição de rede.

Depois de publicar o projeto, a primeira execução falhou ao tentar instalar o pacote Android legado `tools`. O workflow foi corrigido para instalar `platform-tools`. A execução seguinte concluiu `assembleDebug`, `lintDebug`, `core:check` e a verificação independente das planilhas com sucesso.

Ficam pendentes:

- Execução dos testes instrumentados Room e Compose incluídos.
- Teste real de QR Code, código de barras, lanterna, permissões e câmera.
- Persistência após encerrar e reiniciar o processo do app no aparelho.
- Cancelamento/erro de exportação com provedores de documentos reais.
- Layout em celular pequeno, tablet, paisagem, fonte máxima e TalkBack.

O workflow foi publicado e executado neste repositório. O APK foi obtido do artefato oficial dessa execução; não houve execução em celular ou emulador durante esta entrega.
