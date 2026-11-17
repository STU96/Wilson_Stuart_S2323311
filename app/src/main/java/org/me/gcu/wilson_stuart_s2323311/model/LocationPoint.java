package org.me.gcu.wilson_stuart_s2323311.model;

// One end of a situation's location: the <from> or the <to> element.
public class LocationPoint {

    private String pointType = "";      // xsi:type, e.g. TpegJunction or TpegNonJunctionPoint
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private String roadName = "";       // linkName or tpegIlcName1, e.g. "M8"
    private String crossRoadName = "";  // tpegIlcName2, e.g. "B957"
    private String junctionName = "";   // junctionName, e.g. "J19"
    private String placeName = "";      // intersectionName or nonLinkedPointName, e.g. "A90 Brechin"

    public LocationPoint() {
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType == null ? "" : pointType;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public String getCrossRoadName() {
        return crossRoadName;
    }

    public void setCrossRoadName(String crossRoadName) {
        this.crossRoadName = crossRoadName;
    }

    public String getJunctionName() {
        return junctionName;
    }

    public void setJunctionName(String junctionName) {
        this.junctionName = junctionName;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    // A readable name for this point, built from whatever the XML provided
    public String getDisplayName() {
        if (!placeName.isEmpty()) {
            return placeName;
        }
        String name = roadName;
        if (!junctionName.isEmpty()) {
            name = name + " " + junctionName;
        }
        if (!crossRoadName.isEmpty()) {
            name = name + " / " + crossRoadName;
        }
        return name.trim().isEmpty() ? "Unnamed point" : name.trim();
    }

    public String getCoordinatesText() {
        if (!hasCoordinates()) {
            return "No coordinates";
        }
        return String.format(java.util.Locale.UK, "%.5f, %.5f", latitude, longitude);
    }

    @Override
    public String toString() {
        return "LocationPoint{" + getDisplayName()
                + ", road=" + roadName
                + ", junction=" + junctionName
                + ", crossRoad=" + crossRoadName
                + ", type=" + pointType
                + ", lat/long=" + getCoordinatesText() + "}";
    }
}
