# Coleta Fácil 1.1 — revisão de usabilidade

Os relatos do usuário são observações de uso; não há medição quantitativa de tempo, erros ou satisfação. Esta revisão corrige problemas observados, sem inventar melhoria percentual ou alegar validação com usuários que ainda não aconteceu.

## Fluxo escolhido

Identificar código → vibrar e pausar a câmera → apresentar código, nome e conferência → confirmar → salvar → informar o total confirmado → retomar a câmera.

Nenhum registro é criado pela mera detecção. Nome obrigatório na confirmação; conferência Pendente continua permitida. Cancelar descarta somente o rascunho. Códigos repetidos mostram a quantidade já presente e oferecem abrir o existente ou confirmar explicitamente outra unidade. Cada unidade confirmada ocupa uma linha; a exportação mantém Código, Nome e Confere no Kronos?.

Coletas e itens têm ações Editar e Excluir visíveis por menu. Exclusões pedem confirmação, identificam o alvo e oferecem Desfazer. Restaurar uma coleta recupera também seus itens em uma transação. Editar ou excluir itens de uma coleta finalizada reabre a coleta, sem alterar arquivos já exportados. O banco mantém a versão de esquema anterior, sem migração destrutiva. Um identificador estável por formulário evita que tentativas após recriação da tela gerem unidades extras. O salvamento pertence ao ViewModel e só encerra o rascunho correspondente; a ação de Desfazer fica no ViewModel durante sua disponibilidade, inclusive após rotação.

## Aplicação das dez heurísticas de Nielsen

| Princípio | Decisão no aplicativo |
|---|---|
| 1. Estado visível | Vibração, confirmação, câmera pausada, contadores e mensagem de salvamento. |
| 2. Linguagem familiar | Código, nome, conferência no Kronos, unidade e coleta. |
| 3. Controle | Cancelar, confirmar, editar, excluir, reabrir e desfazer. |
| 4. Consistência | Mesmos menus, rótulos, estados e controles Material nas telas. |
| 5. Prevenção | Confirmação por leitura, duplicidade explícita e exclusão identificada. |
| 6. Reconhecimento | Código e dados existentes aparecem juntos durante a conferência. |
| 7. Eficiência | Abrir item existente, busca, filtros e entrada manual. |
| 8. Simplicidade | Um formulário por item e três colunas de exportação. |
| 9. Recuperação | Erros locais e acionáveis, rascunho mantido e restauração de exclusão. |
| 10. Ajuda | Guia no app, ajuda contextual e explicação da duplicidade. |

Referência primária: https://www.nngroup.com/articles/ten-usability-heuristics/ . Diretrizes complementares UI UX Pro Max: feedback de confirmação, recuperação, anúncio de erros e estado preservado no Compose.

## Verificação

Testar regra de confirmação e duplicidade, CRUD/restauração, formulário sem gravação automática, cancelamento, bloqueio da câmera durante conferência e exportações. Compilar, executar lint e revisar o código antes de gerar o APK. Vibração física, TalkBack e conforto do fluxo precisam de validação no aparelho. A assinatura do APK de teste anterior pode diferir: não orientar desinstalação com dados sem antes exportar as coletas.
