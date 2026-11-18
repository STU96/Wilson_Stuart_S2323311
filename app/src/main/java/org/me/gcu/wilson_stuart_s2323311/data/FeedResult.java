package org.me.gcu.wilson_stuart_s2323311.data;

import org.me.gcu.wilson_stuart_s2323311.model.FeedType;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;

import java.util.ArrayList;
import java.util.Date;

// What the repository hands back for one feed: the parsed list plus where it came from.
public class FeedResult {

    public enum Source { LIVE, CACHE, SAMPLE, NONE }

    private final FeedType feedType;
    private final ArrayList<Situation> situations;
    private final Source source;
    private final long dataTimeMillis;      // when this data was downloaded
    private final Date publicationTime;     // <publicationTime> from the feed
    private final String errorMessage;      // why the live download failed ("" if it worked)

    public FeedResult(FeedType feedType, ArrayList<Situation> situations, Source source,
                      long dataTimeMillis, Date publicationTime, String errorMessage) {
        this.feedType = feedType;
        this.situations = situations;
        this.source = source;
        this.dataTimeMillis = dataTimeMillis;
        this.publicationTime = publicationTime;
        this.errorMessage = errorMessage;
    }

    public FeedType getFeedType() { return feedType; }
    public ArrayList<Situation> getSituations() { return situations; }
    public Source getSource() { return source; }
    public long getDataTimeMillis() { return dataTimeMillis; }
    public Date getPublicationTime() { return publicationTime; }
    public String getErrorMessage() { return errorMessage; }

    public boolean hasData() {
        return source != Source.NONE;
    }
}
