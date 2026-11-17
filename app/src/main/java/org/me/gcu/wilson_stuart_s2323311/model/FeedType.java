package org.me.gcu.wilson_stuart_s2323311.model;

// Which Traffic Scotland feed a situation came from.
public enum FeedType {
    INCIDENT("Incident"),
    ROADWORKS("Roadworks");

    private final String label;

    FeedType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
