package org.me.gcu.wilson_stuart_s2323311.model;

// The <impact> element: lanes affected and/or delays.
public class Impact {

    private int numberOfLanesRestricted = -1;   // -1 = not given in the XML
    private int numberOfOperationalLanes = -1;  // -1 = not given in the XML
    private String delaysType = "";             // delays, longDelays, veryLongDelays or ""

    public Impact() {
    }

    public int getNumberOfLanesRestricted() {
        return numberOfLanesRestricted;
    }

    public void setNumberOfLanesRestricted(int numberOfLanesRestricted) {
        this.numberOfLanesRestricted = numberOfLanesRestricted;
    }

    public int getNumberOfOperationalLanes() {
        return numberOfOperationalLanes;
    }

    public void setNumberOfOperationalLanes(int numberOfOperationalLanes) {
        this.numberOfOperationalLanes = numberOfOperationalLanes;
    }

    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String delaysType) {
        this.delaysType = delaysType;
    }

    public DelayLevel getDelayLevel() {
        return DelayLevel.fromDelaysType(delaysType);
    }

    public boolean isRoadClosed() {
        return numberOfOperationalLanes == 0;
    }

    // One readable line describing the impact, for lists and the detail screen
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        if (numberOfOperationalLanes == 0) {
            summary.append("No lanes open (closed)");
        } else if (numberOfOperationalLanes > 0) {
            summary.append(numberOfOperationalLanes).append(" lane(s) open");
        }
        if (numberOfLanesRestricted > 0) {
            if (summary.length() > 0) summary.append(" · ");
            summary.append(numberOfLanesRestricted).append(" lane(s) restricted");
        }
        if (!delaysType.isEmpty()) {
            if (summary.length() > 0) summary.append(" · ");
            summary.append(getDelayLevel().getLabel());
        }
        return summary.length() == 0 ? "No impact details given" : summary.toString();
    }

    @Override
    public String toString() {
        return "Impact{lanesRestricted=" + numberOfLanesRestricted
                + ", operationalLanes=" + numberOfOperationalLanes
                + ", delaysType=" + delaysType + "}";
    }
}
