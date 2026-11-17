package org.me.gcu.wilson_stuart_s2323311.model;

import java.util.Date;
import java.util.Locale;

// One <situation> from either feed (Lab 3 "Thing" class, extended).
public class Situation {

    private final FeedType feedType;
    private String situationId = "";        // <situation id="...">
    private String recordId = "";           // <situationRecord id="...">
    private int recordVersion = -1;         // <situationRecord version="...">
    private String recordType = "";         // xsi:type, e.g. RoadOrCarriagewayOrLaneManagement
    private String detailType = "";         // e.g. roadClosed, queuingTraffic, roadworks
    private String description = "";        // <generalPublicComment> text
    private String probability = "";        // <probabilityOfOccurrence>
    private String source = "";             // <sourceIdentification>
    private String validityStatus = "";     // <validityStatus>
    private String complianceOption = "";   // <complianceOption>
    private boolean urgentRoadworks;        // <urgentRoadworks>
    private Date creationTime;
    private Date versionTime;
    private Date startTime;                 // <overallStartTime>
    private Date endTime;                   // <overallEndTime> (roadworks only)
    private Impact impact = new Impact();
    private TrafficLocation location = new TrafficLocation();

    // Worked out from the description text (roadworks): "A9 Lothmore SB - TTL"
    private String locationText = "";       // "A9 Lothmore SB"
    private String worksType = "";          // "TTL"

    // Set by a distance search so the list can show "3.2 km away"; -1 = not set
    private double distanceKm = -1;

    public Situation(FeedType feedType) {
        this.feedType = feedType;
    }

    // ---------- getters and setters (generated with Alt+Insert, as in Lab 3) ----------

    public FeedType getFeedType() { return feedType; }

    public String getSituationId() { return situationId; }
    public void setSituationId(String situationId) { this.situationId = situationId == null ? "" : situationId; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId == null ? "" : recordId; }

    public int getRecordVersion() { return recordVersion; }
    public void setRecordVersion(int recordVersion) { this.recordVersion = recordVersion; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType == null ? "" : recordType; }

    public String getDetailType() { return detailType; }
    public void setDetailType(String detailType) { this.detailType = detailType; }

    public String getDescription() { return description; }

    // Storing the description also breaks it down into location text + works type
    public void setDescription(String description) {
        this.description = description;
        int lastDash = description.lastIndexOf(" - ");
        if (feedType == FeedType.ROADWORKS && lastDash > 0) {
            locationText = description.substring(0, lastDash).trim();
            worksType = description.substring(lastDash + 3).trim();
        } else {
            locationText = description;
            worksType = "";
        }
    }

    public String getProbability() { return probability; }
    public void setProbability(String probability) { this.probability = probability; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getValidityStatus() { return validityStatus; }
    public void setValidityStatus(String validityStatus) { this.validityStatus = validityStatus; }

    public String getComplianceOption() { return complianceOption; }
    public void setComplianceOption(String complianceOption) { this.complianceOption = complianceOption; }

    public boolean isUrgentRoadworks() { return urgentRoadworks; }
    public void setUrgentRoadworks(boolean urgentRoadworks) { this.urgentRoadworks = urgentRoadworks; }

    public Date getCreationTime() { return creationTime; }
    public void setCreationTime(Date creationTime) { this.creationTime = creationTime; }

    public Date getVersionTime() { return versionTime; }
    public void setVersionTime(Date versionTime) { this.versionTime = versionTime; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Impact getImpact() { return impact; }
    public void setImpact(Impact impact) { this.impact = impact; }

    public TrafficLocation getLocation() { return location; }
    public void setLocation(TrafficLocation location) { this.location = location; }

    public String getLocationText() { return locationText; }
    public String getWorksType() { return worksType; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    // ---------- derived information ----------

    public boolean isRoadworks() {
        return feedType == FeedType.ROADWORKS;
    }

    public DelayLevel getDelayLevel() {
        return impact.getDelayLevel();
    }

    // "TTL" -> "Temporary traffic lights"
    public String getWorksTypeText() {
        return TextHelper.expandWorksAbbreviations(worksType);
    }

    // Road name from the location points, or failing that from the description text
    public String getRoadName() {
        String road = location.getRoadName();
        if (road.isEmpty()) {
            road = TextHelper.findRoadCode(description);
        }
        return road;
    }

    // Classifies the situation so that each kind can have its own icon
    public SituationKind getKind() {
        if (isRoadworks() || recordType.contains("Works")) {
            // Use the works type after the last " - "; if there is none, use the whole description
            String works = (worksType.isEmpty() ? description : worksType).toLowerCase(Locale.UK);
            if (works.contains("ttl") || works.contains("temp light") || works.contains("traffic light")
                    || works.contains("stop and go") || works.contains("stop/go") || works.contains("convoy")) {
                return SituationKind.ROADWORKS_TRAFFIC_LIGHTS;
            } else if (works.contains("lane") || works.contains("mlc") || works.contains("hard shoulder")
                    || works.contains("contraflow")) {
                return SituationKind.ROADWORKS_LANE_CLOSURE;
            } else if (works.contains("closure") || works.contains("closed")) {
                return SituationKind.ROADWORKS_CLOSURE;
            } else if (works.contains("verge")) {
                return SituationKind.ROADWORKS_VERGE;
            } else if (works.contains("sign") || works.contains("vms") || works.contains("diversion")) {
                return SituationKind.ROADWORKS_SIGNAGE;
            } else if (works.contains("speed")) {
                return SituationKind.ROADWORKS_SPEED;
            }
            return SituationKind.ROADWORKS_GENERAL;
        }
        if (recordType.equals("Accident")) {
            return SituationKind.ACCIDENT;
        } else if (recordType.equals("AbnormalTraffic")) {
            return SituationKind.QUEUING_TRAFFIC;
        } else if (recordType.equals("RoadOrCarriagewayOrLaneManagement")) {
            if (detailType.toLowerCase(Locale.UK).contains("closed") || impact.isRoadClosed()) {
                return SituationKind.ROAD_CLOSED;
            }
            return SituationKind.LANE_RESTRICTION;
        } else if (recordType.contains("Obstruction")) {
            return SituationKind.OBSTRUCTION;
        } else if (recordType.contains("Weather") || recordType.contains("Environment")
                || recordType.contains("RoadConditions")) {
            return SituationKind.WEATHER;
        }
        return SituationKind.OTHER_INCIDENT;
    }

    // The "type" shown in lists, e.g. "Road closed" or "Roadworks – temporary traffic lights"
    public String getTypeLabel() {
        return getKind().getLabel();
    }

    // "RoadOrCarriagewayOrLaneManagement" -> "Road or carriageway or lane management"
    public String getRecordTypeText() {
        return TextHelper.camelCaseToWords(recordType);
    }

    public String getDetailTypeText() {
        return detailType.isEmpty() ? "Not given" : TextHelper.camelCaseToWords(detailType);
    }

    // Number of whole days between start and end (roadworks), or -1 if unknown
    public long getDurationDays() {
        if (startTime == null || endTime == null) {
            return -1;
        }
        long millis = endTime.getTime() - startTime.getTime();
        return Math.max(0, millis / (24L * 60 * 60 * 1000));
    }

    // Distance in km from a position to the nearest end of this situation (Haversine formula)
    public double distanceFrom(double latitude, double longitude) {
        double best = Double.MAX_VALUE;
        LocationPoint[] points = { location.getFrom(), location.getTo() };
        for (LocationPoint p : points) {
            if (p != null && p.hasCoordinates()) {
                best = Math.min(best, haversineKm(latitude, longitude, p.getLatitude(), p.getLongitude()));
            }
        }
        return best;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // Road search: road codes must match exactly (so "A9" does not match "A90");
    // any other text is matched anywhere in the description or place names.
    public boolean matchesRoad(String query) {
        String road = TextHelper.normaliseRoad(query);
        if (road.isEmpty()) {
            return false;
        }
        if (TextHelper.isRoadCode(road)) {
            if (road.equals(TextHelper.normaliseRoad(getRoadName()))) {
                return true;
            }
            LocationPoint[] points = { location.getFrom(), location.getTo() };
            for (LocationPoint p : points) {
                if (p != null && (road.equals(TextHelper.normaliseRoad(p.getRoadName()))
                        || road.equals(TextHelper.normaliseRoad(p.getCrossRoadName())))) {
                    return true;
                }
            }
            for (String word : description.split("[^A-Za-z0-9()]+")) {
                if (road.equals(TextHelper.normaliseRoad(word))) {
                    return true;
                }
            }
            return false;
        }
        String text = query.trim().toLowerCase(Locale.UK);
        if (description.toLowerCase(Locale.UK).contains(text)) {
            return true;
        }
        LocationPoint[] points = { location.getFrom(), location.getTo() };
        for (LocationPoint p : points) {
            if (p != null && p.getDisplayName().toLowerCase(Locale.UK).contains(text)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Situation{" + feedType.getLabel()
                + ", id=" + situationId
                + ", recordId=" + recordId + " v" + recordVersion
                + ", type=" + recordType + "/" + detailType
                + ", kind=" + getKind()
                + ", road=" + getRoadName()
                + ", description=" + description
                + ", works=" + worksType
                + ", start=" + TextHelper.formatDate(startTime)
                + ", end=" + TextHelper.formatDate(endTime)
                + ", " + impact
                + ", " + location + "}";
    }
}
