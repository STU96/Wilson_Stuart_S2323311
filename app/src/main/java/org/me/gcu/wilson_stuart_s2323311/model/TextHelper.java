package org.me.gcu.wilson_stuart_s2323311.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Small String / date helpers used while breaking the XML text down (Lab 2 style String methods).
public class TextHelper {

    // XML dates look like 2026-08-10T11:37:42+01:00
    private static final String XML_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String DISPLAY_DATE_PATTERN = "EEE d MMM yyyy, HH:mm";

    private TextHelper() { }

    public static Date parseXmlDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat(XML_DATE_PATTERN, Locale.UK);
            return format.parse(text.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return "Not given";
        }
        SimpleDateFormat format = new SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.UK);
        return format.format(date);
    }

    // "roadOrCarriagewayOrLaneManagement" -> "Road or carriageway or lane management"
    // "southBound" -> "Southbound", "queuingTraffic" -> "Queuing traffic"
    public static String camelCaseToWords(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                // keep "southBound" as one word: "Southbound"
                if (text.substring(i).equals("Bound")) {
                    result.append("bound");
                    break;
                }
                result.append(' ').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        // Capitalise the first letter
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    // Parses an int, returning -1 if the text is not a number (Lab 2 Task 3: catch NumberFormatException)
    public static int parseIntSafe(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }

    public static double parseDoubleSafe(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return Double.NaN;
        }
    }

    // Upper-cases and removes spaces so "a 90", "A90 " and "a90" all become "A90"
    public static String normaliseRoad(String road) {
        if (road == null) {
            return "";
        }
        return road.toUpperCase(Locale.UK).replace(" ", "").trim();
    }

    // True for road codes such as M8, A90, B957, A74(M)
    public static boolean isRoadCode(String text) {
        return normaliseRoad(text).matches("[AMB][0-9]+(\\(M\\))?");
    }

    // Finds the first road code mentioned in a description, e.g. "The A78 is closed..." -> "A78"
    public static String findRoadCode(String text) {
        if (text == null) {
            return "";
        }
        String[] words = text.split("[^A-Za-z0-9()]+");
        for (String word : words) {
            if (isRoadCode(word)) {
                return normaliseRoad(word);
            }
        }
        return "";
    }

    // Expands common Traffic Scotland roadworks abbreviations
    public static String expandWorksAbbreviations(String text) {
        if (text == null) {
            return "";
        }
        String result = text.trim();
        String upper = result.toUpperCase(Locale.UK);
        if (upper.equals("TTL") || upper.equals("TTLS") || upper.equals("TTL'S")) {
            return "Temporary traffic lights";
        } else if (upper.equals("EMERGENCY TTL")) {
            return "Emergency temporary traffic lights";
        } else if (upper.equals("MVMS")) {
            return "Mobile variable message sign";
        } else if (upper.equals("MLC")) {
            return "Mobile lane closure";
        }
        return result;
    }
}
