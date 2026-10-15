# Coleta Fácil — sistema visual

Base: UI UX Pro Max, https://github.com/nextlevelbuilder/ui-ux-pro-max-skill.
Arquivos consultados: SKILL.md, references/pro-rules.md, scripts/search.py e bases products/styles/colors/typography/ux-guidelines/stacks/jetpack-compose.

Consultas executadas: `inventory barcode mobile productivity --design-system`; refinamento `inventory management app --design-system`; `lifecycle accessibility state --stack jetpack-compose`.

Resultados adotados: Flat Design, paleta industrial slate + stock green; observação de estado com collectAsStateWithLifecycle e semântica acessível. O padrão de landing page devolvido pelo gerador não se aplica ao app e foi descartado. As sugestões tipográficas inconsistentes entre as consultas também não foram adotadas; a fonte nativa sans-serif é uma decisão de projeto baseada nas diretrizes gerais de legibilidade, sem alegar um resultado específico da base para essa escolha.

## Tokens e componentes
- Fundo claro #F8FAFC, texto #0F172A, secundário #475569; cartões brancos.
- Ação principal #047857 com texto branco, ajuste do verde sugerido para contraste.
- Fundo escuro #0B1220, cartões #111C2E, texto #F1F5F9, ação #6EE7B7 com texto #064E3B.
- Tipografia sans-serif nativa; texto principal 16sp, título 24sp, destaque 32sp; sem fonte remota.
- Espaçamento de 4/8dp; margens 20dp; cartões 20–28dp de raio; ícones Material Outlined consistentes.
- Controles de no mínimo 48dp; ações primárias de 52–54dp; feedback nativo Material.
- Respeitar insets do sistema, teclado, rolagem e orientação; sem animação decorativa.
- Status sempre acompanhado de texto e ícone, nunca apenas cor.
- Início sem dados fictícios; resumo e contadores refletem os registros reais.

## Limites da validação
Contrastes dos pares explícitos podem ser medidos por script. Fontes ampliadas, TalkBack, câmera, rotação e layout real precisam de validação em dispositivo/emulador Android; não foram apresentados como testes executados neste ambiente.
