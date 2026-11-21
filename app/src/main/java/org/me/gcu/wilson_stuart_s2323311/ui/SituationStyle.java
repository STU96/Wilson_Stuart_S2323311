package org.me.gcu.wilson_stuart_s2323311.ui;

import android.content.Context;

import androidx.core.content.ContextCompat;

import org.me.gcu.wilson_stuart_s2323311.R;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;
import org.me.gcu.wilson_stuart_s2323311.model.SituationKind;

// Picks the icon and colour for each situation so the lists, details and map all match
public class SituationStyle {

    private SituationStyle() { }

    // Icon for each type of situation (Material Design Icons)
    public static int getIconRes(SituationKind kind) {
        switch (kind) {
            case ROAD_CLOSED:
            case ROADWORKS_CLOSURE:
                return R.drawable.ic_road_closed;
            case LANE_RESTRICTION:
            case ROADWORKS_LANE_CLOSURE:
                return R.drawable.ic_lane_restriction;
            case QUEUING_TRAFFIC:
                return R.drawable.ic_queue;
            case ACCIDENT:
                return R.drawable.ic_accident;
            case OBSTRUCTION:
                return R.drawable.ic_obstruction;
            case WEATHER:
                return R.drawable.ic_weather;
            case ROADWORKS_TRAFFIC_LIGHTS:
                return R.drawable.ic_traffic_lights;
            case ROADWORKS_VERGE:
                return R.drawable.ic_verge;
            case ROADWORKS_SIGNAGE:
                return R.drawable.ic_signage;
            case ROADWORKS_SPEED:
                return R.drawable.ic_speed;
            case ROADWORKS_GENERAL:
                return R.drawable.ic_roadworks;
            default:
                return R.drawable.ic_incident_other;
        }
    }

    // Incidents use one colour; roadworks are colour-coded by delay (short -> long)
    public static int getColourRes(Situation situation) {
        if (!situation.isRoadworks()) {
            return R.color.incident;
        }
        switch (situation.getDelayLevel()) {
            case VERY_LONG:
                return R.color.delay_very_long;
            case LONG:
                return R.color.delay_long;
            case DELAYS:
                return R.color.delay_short;
            default:
                return R.color.delay_none;
        }
    }

    public static int getColour(Context context, Situation situation) {
        return ContextCompat.getColor(context, getColourRes(situation));
    }

    // Text for the small coloured badge
    public static String getBadgeText(Situation situation) {
        return situation.isRoadworks() ? situation.getDelayLevel().getLabel() : "Incident";
    }
}
