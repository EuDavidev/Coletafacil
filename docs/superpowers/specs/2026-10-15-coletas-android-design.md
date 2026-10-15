# Aplicativo Android — Coleta e conferência de códigos

## Objetivo e requisitos confirmados

Ler QR Codes e códigos de barras pela câmera do Android, acumular os registros em uma coleta e exportar os dados em CSV ou XLSX quando o usuário decidir finalizar. O usuário digitará o nome de cada item e informará manualmente se ele confere com o aplicativo Kronos utilizado na empresa.

## Planilha de saída

Exatamente três colunas, nesta ordem:

| Código | Nome | Confere no Kronos? |
|---|---|---|
| Conteúdo integral capturado pela câmera | Texto digitado pelo usuário | Pendente, Sim ou Não |

Código será armazenado como texto, preservando zeros iniciais e valores longos. Não haverá extração automática do nome nem consulta automática ao Kronos. A conferência será uma informação declarada pelo usuário, após consultar o Kronos.

## Proposta de funcionamento

1. Criar uma coleta com nome, como “Conferência do setor”.
2. Iniciar a câmera. Cada código reconhecido e gravado gera um registro, com nome vazio e conferência Pendente.
3. Exibir contador e última leitura confirmada. A câmera continua disponível para ler os próximos itens.
4. Pausar a câmera e abrir a lista a qualquer momento para digitar nomes e selecionar Pendente, Sim ou Não. Os registros podem ser preenchidos individualmente, sem obrigar uma interrupção a cada leitura.
5. Revisar os registros, com filtros para nomes vazios e conferências pendentes. Permitir editar nome, conferência e código digitado, ou excluir um registro com opção de desfazer.
6. Finalizar e escolher CSV ou XLSX. O seletor de arquivos do Android permite escolher o destino. A coleta permanece salva e pode ser exportada novamente.

Registros sem nome ou com conferência Pendente poderão ser exportados: a tela de revisão mostrará essas quantidades, sem converter dados ausentes em confirmações positivas. Depois de finalizada, a coleta poderá ser reaberta para correção.

## Leituras repetidas

Manter o mesmo código diante da câmera gera apenas um registro. Para registrar novamente o mesmo código, será necessário retirá-lo do enquadramento e apresentá-lo outra vez. Ler um código diferente permite registrar o novo item. Registros iguais serão mantidos como linhas separadas, sem somar quantidades ou sobrescrever nomes. Haverá também entrada manual do código caso a câmera não consiga lê-lo.

## Interface e acessibilidade

Interface em português, com três áreas: Coletas, Scanner e Registros. A exportação será acessível a partir dos detalhes da coleta. Tela inicial com ação Nova coleta; scanner com ação Pausar e revisar; lista com ação Finalizar e exportar.

A direção visual seguirá o UI UX Pro Max: hierarquia clara, ícones vetoriais consistentes, alvos de toque de pelo menos 48 dp no Android, espaçamentos em múltiplos de 4/8 dp, contraste de texto de pelo menos 4,5:1 e rótulos acessíveis. Conferência terá texto e ícone, além de cor. Botões respeitarão as barras e áreas de gesto do sistema. Tema claro e escuro, com suporte a fonte ampliada e TalkBack.

Referências consultadas:
- https://github.com/nextlevelbuilder/ui-ux-pro-max-skill/blob/main/.claude/skills/ui-ux-pro-max/SKILL.md
- https://github.com/nextlevelbuilder/ui-ux-pro-max-skill/blob/main/.claude/skills/ui-ux-pro-max/references/pro-rules.md

## Arquitetura recomendada para revisão

Android nativo com Kotlin e Jetpack Compose; CameraX e ML Kit com modelo incorporado para leitura; banco local Room para coletas e registros. O fluxo principal funcionará sem login e sem conexão com a internet.

Separar captura, persistência, edição e exportação. Cada leitura só será apresentada como salva depois de persistida no banco. Edições serão salvas automaticamente; voltar de tela ou fechar o app não descartará alterações confirmadas.

CSV em UTF-8 com BOM, separador ponto e vírgula e escape de aspas e quebras de linha. Como CSV não define tipos de célula, a importação no Excel deverá tratar Código como texto. XLSX usará células textuais para preservar exatamente os códigos. Conteúdos começando por caracteres de fórmula serão neutralizados no CSV; no XLSX serão gravados como texto literal. A interface só anunciará exportação concluída após o arquivo ser escrito com sucesso.

## Falhas e recuperação

Explicar como permitir a câmera e manter disponível a entrada manual. Pausar a captura ao sair da tela ou colocar o app em segundo plano. Falha ao gravar um registro deve mostrar erro e não aumentar o contador. Cancelar ou falhar na exportação mantém a coleta intacta e permite tentar novamente.

## Critérios de validação

- Ler QR Code e código de barras, persistir registros e restaurá-los após reiniciar o app.
- Evitar repetição contínua do mesmo código e permitir repetição intencional após retirá-lo da câmera.
- Editar manualmente nome e conferência, preservando cada registro.
- Exportar exatamente as três colunas na ordem especificada.
- Abrir XLSX em leitor independente e verificar zeros iniciais, códigos longos, acentos e conteúdo literal.
- Verificar CSV com aspas, ponto e vírgula, quebras de linha e caracteres de fórmula.
- Verificar cancelamento de exportação e permissão de câmera negada.
- Conferir interface com fonte ampliada, TalkBack, temas claro/escuro e orientação paisagem.

## Estado

Especificação preparada para revisão. A implementação e a geração do aplicativo ainda não foram iniciadas.
