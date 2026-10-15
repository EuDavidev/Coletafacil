package br.com.coletafacil.core;
import java.util.HashMap;
import java.util.Map;

/** Collection-owned scan state survives camera pause and preview recreation. */
public final class ScanSessions {
    private final Map<String, ScanGate> sessions = new HashMap<>();
    public synchronized ScanGate forCollection(String id) {
        ScanGate gate = sessions.get(id);
        if (gate == null) { gate = new ScanGate(); sessions.put(id, gate); }
        return gate;
    }
}
