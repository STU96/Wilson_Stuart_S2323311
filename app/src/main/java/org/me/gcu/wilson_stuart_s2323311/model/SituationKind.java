package org.me.gcu.wilson_stuart_s2323311.model;

// The finer-grained "type" of a situation. Each kind has its own icon (see ui/SituationStyle).
public enum SituationKind {
    // Unplanned events (incidents)
    ROAD_CLOSED("Road closed"),
    LANE_RESTRICTION("Lane restriction"),
    QUEUING_TRAFFIC("Queuing traffic"),
    ACCIDENT("Accident"),
    OBSTRUCTION("Obstruction"),
    WEATHER("Weather / road conditions"),
    OTHER_INCIDENT("Incident"),
    // Roadworks
    ROADWORKS_GENERAL("Roadworks"),
    ROADWORKS_TRAFFIC_LIGHTS("Roadworks – temporary traffic lights"),
    ROADWORKS_CLOSURE("Roadworks – closure"),
    ROADWORKS_LANE_CLOSURE("Roadworks – lane closure"),
    ROADWORKS_VERGE("Roadworks – verge works"),
    ROADWORKS_SIGNAGE("Roadworks – signs / diversion"),
    ROADWORKS_SPEED("Roadworks – speed restriction");

    private final String label;

    SituationKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
