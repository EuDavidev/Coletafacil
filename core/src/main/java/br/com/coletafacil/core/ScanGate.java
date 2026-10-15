package br.com.coletafacil.core;
import java.util.*;
public final class ScanGate {
    private static final long REARM_AFTER_MS = 600;
    private final Set<String> blocked = new HashSet<>();
    private final Map<String, Long> absentSince = new HashMap<>();
    private String pending;

    public synchronized String candidate(Set<String> visible, long nowMs) {
        for (Iterator<String> it = blocked.iterator(); it.hasNext();) {
            String code = it.next();
            if (visible.contains(code)) {
                absentSince.remove(code);
            } else {
                Long since = absentSince.get(code);
                if (since == null) absentSince.put(code, nowMs);
                else if (nowMs - since >= REARM_AFTER_MS) {
                    it.remove();
                    absentSince.remove(code);
                }
            }
        }
        if (pending != null) return null;
        for (String code : visible) {
            if (code != null && !code.trim().isEmpty() && !blocked.contains(code)) {
                pending = code;
                return code;
            }
        }
        return null;
    }

    public synchronized void accepted(String code) {
        blocked.add(code);
        if (code.equals(pending)) pending = null;
    }

    public synchronized void failed(String code) {
        if (code.equals(pending)) pending = null;
    }
}
