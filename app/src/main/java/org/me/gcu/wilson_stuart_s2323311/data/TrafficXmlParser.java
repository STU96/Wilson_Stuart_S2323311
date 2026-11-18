package org.me.gcu.wilson_stuart_s2323311.data;

import android.util.Log;

import org.me.gcu.wilson_stuart_s2323311.model.FeedType;
import org.me.gcu.wilson_stuart_s2323311.model.LocationPoint;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;
import org.me.gcu.wilson_stuart_s2323311.model.TextHelper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;

// Parses a DATEX II feed (already downloaded into a String) with XmlPullParser - Lab 3 approach.
public class TrafficXmlParser {

    private static final String TAG = "Parsing";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    private Date publicationTime;   // <publicationTime> of the last feed parsed

    public Date getPublicationTime() {
        return publicationTime;
    }

    public ArrayList<Situation> parse(String xml, FeedType feedType) {
        ArrayList<Situation> situations = new ArrayList<>();

        Situation situation = null;      // the <situation> currently being built
        LocationPoint point = null;      // the <from> or <to> currently being built
        boolean insideComment = false;   // true while inside <generalPublicComment>
        int recordDepth = -1;            // depth of <situationRecord>, used to spot its direct children
        int recordCount = 0;             // number of <situationRecord>s seen in this situation
        String lastValue = "";           // last <value> text read inside <from>/<to>
        publicationTime = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xml));

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG) {
                    String tag = xpp.getName();

                    if (tag.equalsIgnoreCase("publicationTime")) {
                        publicationTime = TextHelper.parseXmlDate(xpp.nextText());

                    } else if (tag.equalsIgnoreCase("situation")) {
                        // A new situation starts: create a new object (like a new Thing in Lab 3)
                        situation = new Situation(feedType);
                        situation.setSituationId(xpp.getAttributeValue(null, "id"));
                        recordCount = 0;

                    } else if (situation != null) {
                        // Everything below only matters while we are inside a <situation>

                        if (tag.equalsIgnoreCase("situationRecord")) {
                            recordDepth = xpp.getDepth();
                            recordCount++;
                            if (recordCount == 1) {
                                // Attributes: xsi:type="..." id="..." version="..."
                                // (the spec's xpp.getAttributeValue(0) also returns the xsi:type here)
                                situation.setRecordType(xpp.getAttributeValue(XSI_NAMESPACE, "type"));
                                situation.setRecordId(xpp.getAttributeValue(null, "id"));
                                situation.setRecordVersion(
                                        TextHelper.parseIntSafe(xpp.getAttributeValue(null, "version")));
                            }

                        } else if (tag.equalsIgnoreCase("situationRecordCreationTime")) {
                            situation.setCreationTime(TextHelper.parseXmlDate(xpp.nextText()));
                        } else if (tag.equalsIgnoreCase("situationRecordVersionTime")) {
                            situation.setVersionTime(TextHelper.parseXmlDate(xpp.nextText()));
                        } else if (tag.equalsIgnoreCase("probabilityOfOccurrence")) {
                            situation.setProbability(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("sourceIdentification")) {
                            situation.setSource(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("validityStatus")) {
                            situation.setValidityStatus(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("overallStartTime")) {
                            situation.setStartTime(TextHelper.parseXmlDate(xpp.nextText()));
                        } else if (tag.equalsIgnoreCase("overallEndTime")) {
                            situation.setEndTime(TextHelper.parseXmlDate(xpp.nextText()));

                        // ----- <impact> -----
                        } else if (tag.equalsIgnoreCase("numberOfOperationalLanes")) {
                            situation.getImpact().setNumberOfOperationalLanes(
                                    TextHelper.parseIntSafe(xpp.nextText()));
                        } else if (tag.equalsIgnoreCase("numberOfLanesRestricted")) {
                            situation.getImpact().setNumberOfLanesRestricted(
                                    TextHelper.parseIntSafe(xpp.nextText()));
                        } else if (tag.equalsIgnoreCase("delaysType")) {
                            situation.getImpact().setDelaysType(xpp.nextText().trim());

                        } else if (tag.equalsIgnoreCase("generalPublicComment")) {
                            insideComment = true;

                        // ----- <groupOfLocations> -----
                        } else if (tag.equalsIgnoreCase("tpegDirection")) {
                            situation.getLocation().setDirection(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("tpegLinearLocationType")) {
                            situation.getLocation().setLocationType(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("from")) {
                            point = new LocationPoint();
                            point.setPointType(xpp.getAttributeValue(XSI_NAMESPACE, "type"));
                            situation.getLocation().setFrom(point);
                        } else if (tag.equalsIgnoreCase("to")) {
                            point = new LocationPoint();
                            point.setPointType(xpp.getAttributeValue(XSI_NAMESPACE, "type"));
                            situation.getLocation().setTo(point);
                        } else if (tag.equalsIgnoreCase("latitude")) {
                            double lat = TextHelper.parseDoubleSafe(xpp.nextText());
                            if (point != null) point.setLatitude(lat);
                        } else if (tag.equalsIgnoreCase("longitude")) {
                            double lon = TextHelper.parseDoubleSafe(xpp.nextText());
                            if (point != null) point.setLongitude(lon);

                        } else if (tag.equalsIgnoreCase("value")) {
                            // <value lang="en"> is used in several places; the flags tell us which one
                            String text = xpp.nextText().trim();
                            if (insideComment) {
                                if (situation.getDescription().isEmpty()) {
                                    situation.setDescription(text);
                                } else if (!situation.getDescription().contains(text)) {
                                    situation.setDescription(situation.getDescription() + " | " + text);
                                }
                            } else if (point != null) {
                                // The meaning of this value is given by the descriptor type that follows it
                                lastValue = text;
                            }
                            // values inside <sourceName> are ignored

                        } else if (tag.equalsIgnoreCase("tpegOtherPointDescriptorType")) {
                            String type = xpp.nextText().trim();
                            if (point != null) {
                                if (type.equalsIgnoreCase("linkName")) {
                                    point.setRoadName(lastValue);          // e.g. "M8"
                                } else if (point.getPlaceName().isEmpty()) {
                                    point.setPlaceName(lastValue);         // intersectionName / nonLinkedPointName
                                }
                            }
                        } else if (tag.equalsIgnoreCase("tpegIlcPointDescriptorType")) {
                            String type = xpp.nextText().trim();
                            if (point != null) {
                                if (type.equalsIgnoreCase("tpegIlcName1")) {
                                    point.setRoadName(lastValue);          // e.g. "A90"
                                } else if (type.equalsIgnoreCase("tpegIlcName2")) {
                                    point.setCrossRoadName(lastValue);     // e.g. "B957"
                                }
                            }
                        } else if (tag.equalsIgnoreCase("tpegJunctionPointDescriptorType")) {
                            String type = xpp.nextText().trim();
                            if (point != null && type.equalsIgnoreCase("junctionName")) {
                                point.setJunctionName(lastValue);          // e.g. "J19"
                            }

                        // ----- remaining direct children of <situationRecord> -----
                        } else if (tag.equalsIgnoreCase("complianceOption")) {
                            situation.setComplianceOption(xpp.nextText().trim());
                        } else if (tag.equalsIgnoreCase("urgentRoadworks")) {
                            situation.setUrgentRoadworks(Boolean.parseBoolean(xpp.nextText().trim()));
                        } else if (recordDepth > 0 && xpp.getDepth() == recordDepth + 1 && tag.endsWith("Type")) {
                            // roadOrCarriagewayOrLaneManagementType, abnormalTrafficType, roadMaintenanceType,
                            // and any new ...Type that appears in future (the app must not crash on new data)
                            String text = xpp.nextText().trim();
                            if (situation.getDetailType().isEmpty()
                                    || tag.equalsIgnoreCase(situation.getRecordType() + "Type")) {
                                situation.setDetailType(text);
                            }
                        }
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    String tag = xpp.getName();
                    if (tag.equalsIgnoreCase("situation") && situation != null) {
                        // Situation finished: add it to the collection
                        situations.add(situation);
                        Log.d(TAG, situation.toString());
                        situation = null;
                        recordDepth = -1;
                    } else if (tag.equalsIgnoreCase("generalPublicComment")) {
                        insideComment = false;
                    } else if (tag.equalsIgnoreCase("from") || tag.equalsIgnoreCase("to")) {
                        point = null;
                        lastValue = "";
                    }
                }

                eventType = xpp.next();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XML parsing problem: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IO problem while parsing: " + e.getMessage());
        }

        Log.d(TAG, feedType.getLabel() + ": " + situations.size() + " situations parsed");
        return situations;
    }
}
