# Validação da entrega

## Resultado disponível

Projeto-fonte Android implementado e revisado. **APK não gerado neste ambiente.** Não foi executada uma compilação Android, teste de câmera real, emulador ou renderização de Compose.

## Verificações executadas

| Verificação | Resultado |
|---|---|
| Compilação do núcleo Java real com Java 17, destino Java 8 | Aprovada |
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

## Ferramentas que não puderam executar

O SDK Android não estava instalado. A tentativa de `bash gradlew :app:assembleDebug` não conseguiu baixar o Gradle 8.11.1: `java.net.SocketException: Network is unreachable`. Pedidos aos servidores de dependências pelo proxy configurado também expiraram. Não houve tentativa de contornar a restrição de rede.

Por isso, ficam pendentes:

- Compilação Android e lint.
- Execução dos testes instrumentados Room e Compose incluídos.
- Teste real de QR Code, código de barras, lanterna, permissões e câmera.
- Persistência após encerrar e reiniciar o processo do app no aparelho.
- Cancelamento/erro de exportação com provedores de documentos reais.
- Layout em celular pequeno, tablet, paisagem, fonte máxima e TalkBack.

O workflow fornecido pode executar a compilação, lint e testes do núcleo em um repositório com Actions habilitado. Não foi publicado nem executado durante esta entrega.
