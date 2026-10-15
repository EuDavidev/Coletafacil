package br.com.coletafacil.core;

public final class ScanRecord {
    public final String code;
    public final String name;
    public final String status;
    public ScanRecord(String code, String name, String status) {
        this.code = code;
        this.name = name;
        this.status = status;
    }
}
