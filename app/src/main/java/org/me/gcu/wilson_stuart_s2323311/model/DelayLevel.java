package org.me.gcu.wilson_stuart_s2323311.model;

// Delay severity taken from <delaysType> inside <impact>.
// Used to colour-code roadworks (short delays vs long delays).
public enum DelayLevel {
    NONE("No delays reported"),
    DELAYS("Delays (short)"),
    LONG("Long delays"),
    VERY_LONG("Very long delays");

    private final String label;

    DelayLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // Converts the raw XML value (e.g. "longDelays") into a DelayLevel
    public static DelayLevel fromDelaysType(String delaysType) {
        if (delaysType == null || delaysType.isEmpty()) {
            return NONE;
        }
        String value = delaysType.toLowerCase();
        if (value.startsWith("verylong")) {
            return VERY_LONG;
        } else if (value.startsWith("long")) {
            return LONG;
        } else if (value.equals("negligible")) {
            return NONE;
        } else {
            return DELAYS; // "delays", "delaysOfUncertainDuration", ...
        }
    }
}
