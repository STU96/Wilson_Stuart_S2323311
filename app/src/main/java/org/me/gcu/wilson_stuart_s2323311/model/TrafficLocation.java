package org.me.gcu.wilson_stuart_s2323311.model;

// The <groupOfLocations> / <tpegLinearLocation> element: a direction and two points (from -> to).
public class TrafficLocation {

    private String direction = "";      // e.g. southBound, bothWays
    private String locationType = "";   // e.g. segment
    private LocationPoint from;
    private LocationPoint to;

    public TrafficLocation() {
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    // "southBound" -> "Southbound"
    public String getDirectionText() {
        return direction.isEmpty() ? "Not given" : TextHelper.camelCaseToWords(direction);
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public LocationPoint getFrom() {
        return from;
    }

    public void setFrom(LocationPoint from) {
        this.from = from;
    }

    public LocationPoint getTo() {
        return to;
    }

    public void setTo(LocationPoint to) {
        this.to = to;
    }

    // Road name taken from the location points (first one found)
    public String getRoadName() {
        if (from != null && !from.getRoadName().isEmpty()) {
            return from.getRoadName();
        }
        if (to != null && !to.getRoadName().isEmpty()) {
            return to.getRoadName();
        }
        return "";
    }

    // True when the two ends are different points, i.e. the situation is a line rather than a single point
    public boolean isLine() {
        return from != null && to != null && from.hasCoordinates() && to.hasCoordinates()
                && (from.getLatitude() != to.getLatitude() || from.getLongitude() != to.getLongitude());
    }

    @Override
    public String toString() {
        return "TrafficLocation{direction=" + direction
                + ", type=" + locationType
                + ", from=" + from
                + ", to=" + to + "}";
    }
}
