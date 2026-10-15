# Validação da versão 1.1.0

Fonte do APK: commit `695352e23d5e0c25e11c1c8b3a5f81126a7469ff`.
Workflow: https://github.com/EuDavidev/Coletafacil/actions/runs/38761099819 .

## Escopo e revisão

Confirmação antes de persistir, nome obrigatório, conferência manual no Kronos, duplicidade com confirmação explícita de outra unidade e contagem por código. Editar/excluir coletas e itens, restaurar exclusões e reabrir coletas modificadas após exportar. CSV/XLSX mantêm as três colunas originais. O banco Room mantém o esquema versão 1, sem migração destrutiva.

Uma revisão independente identificou e levou à correção de três pontos: salvamentos pertencem ao ViewModel e usam identificadores estáveis para evitar inserções repetidas após recriação; abrir um registro existente protege campos ainda não confirmados; Desfazer permanece disponível no ViewModel após rotação. A finalização de um salvamento só encerra o rascunho correspondente, preservando uma leitura posterior.

## Verificações

- Núcleo Java: 28 asserções aprovadas.
- Exportações reais abertas por Python csv e openpyxl, com validação de XML.
- Estrutura XML/YAML, Gradle Wrapper e 16 pares de contraste explícitos >= 4,5:1.
- git diff --check sem erros.
- Compilação `assembleDebug`, compilação dos testes Android, `lintDebug` e `core:check`: aprovados no workflow.
- Emulador Android 11 / API 30 x86_64: **8 testes instrumentados, 0 falhas**. Cobrem Room, formulário, confirmação, duplicidade, cancelamento, proteção ao abrir existente, tentativas idempotentes e restauração da coleta.
- Assinatura verificada por `apksigner verify`: esquema v2 válido.
- ZIP do artefato conferido com o SHA-256 informado pelo GitHub Actions; APK extraído sem modificar seus bytes. Manifesto binário, integridade ZIP e certificado inspecionados.

## Limites

O emulador valida formulários e persistência, mas não substitui leitura de etiquetas pela câmera e vibração no aparelho físico. Conferir câmera, lanterna, permissões, TalkBack, fonte máxima, paisagem, conforto do fluxo e exportação para provedores de documentos reais. Não há estudo quantitativo com usuários nem alegação de melhoria percentual. O mapeamento das dez heurísticas está em [UX-1.1.md](UX-1.1.md).

## Instalação e dados existentes

O APK é de teste. Compare a assinatura ao atualizar: se a chave de debug mudar, o Android rejeita instalar sobre a versão anterior. **Não desinstale a versão anterior sem exportar todas as coletas.** Desinstalar apaga o banco local. As planilhas exportadas permanecem no destino escolhido; esta versão não tem importação automática para restaurar o banco a partir delas. Uma distribuição permanente deverá usar uma chave de assinatura estável e protegida.

## APK verificado

- Pacote: `br.com.coletafacil`; versão `1.1.0` / código `2`; mínimo API 26; target API 35.
- Tamanho: 42028279 bytes.
- SHA-256: `01209f91c0e7ae1d0ee635b2a32cd057bcd497a1f8023bf804659a387e3cbb08`.
- SHA-256 do certificado: `9e3e7132c1111647163fc62e318c91ae87e6a085f79fc7eae6063bd5b1a20f36`.
- O certificado difere do APK 1.0.0 entregue anteriormente; a limitação de atualização descrita acima foi confirmada.
