package br.com.coletafacil.core;

public final class EntryRules {
    public enum Result {
        OK(""), CODE_REQUIRED("Informe o código."), NAME_REQUIRED("Informe o nome do item."),
        STATUS_INVALID("Escolha Pendente, Sim ou Não."),
        DUPLICATE("Este código já existe nesta coleta. Abra o item existente ou confirme outra unidade.");
        public final String message;
        Result(String message) { this.message = message; }
    }
    public static Result validate(String code, String name, String status, int duplicates, boolean allowDuplicate) {
        if (code == null || code.trim().isEmpty()) return Result.CODE_REQUIRED;
        if (name == null || name.trim().isEmpty()) return Result.NAME_REQUIRED;
        if (!"Pendente".equals(status) && !"Sim".equals(status) && !"Não".equals(status)) return Result.STATUS_INVALID;
        if (duplicates > 0 && !allowDuplicate) return Result.DUPLICATE;
        return Result.OK;
    }
}
