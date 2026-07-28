package community;

import java.io.Serializable;
import java.math.BigDecimal;

/**
A community event organised by a member - workshop, social gathering, or community activity - Holds the six core fields (category, date, time, duration, venue, cost) plus the server-issued gathering ID and the organiser's FIDN.

Date and time travel as ISO strings (YYYY-MM-DD and HH:MM) so the frontend can render and validate them without timezone surprises.

BigDecimal is used for cost to avoid floating-point rounding errors on money values.
 */
public class CommunityGathering implements Serializable {

    private static final long serialVersionUID = 3008L;

    private int gatheringId;
    private int organiserFidn;
    private GatheringCategory gatheringCategory;
    private String gatheringDate;       // YYYY-MM-DD
    private String gatheringTime;       // HH:MM (24-hour)
    private int durationMinutes;
    private String venue;
    private BigDecimal estimatedCost;

    public CommunityGathering() {
    }

    public CommunityGathering(int gatheringId, int organiserFidn,
                              GatheringCategory gatheringCategory,
                              String gatheringDate, String gatheringTime,
                              int durationMinutes, String venue,
                              BigDecimal estimatedCost) {
        this.gatheringId       = gatheringId;
        this.organiserFidn     = organiserFidn;
        this.gatheringCategory = gatheringCategory;
        this.gatheringDate     = gatheringDate;
        this.gatheringTime     = gatheringTime;
        this.durationMinutes   = durationMinutes;
        this.venue             = venue;
        this.estimatedCost     = estimatedCost;
    }

    public int getGatheringId()                    { return gatheringId; }
    public int getOrganiserFidn()                  { return organiserFidn; }
    public GatheringCategory getGatheringCategory() { return gatheringCategory; }
    public String getGatheringDate()               { return gatheringDate; }
    public String getGatheringTime()               { return gatheringTime; }
    public int getDurationMinutes()                { return durationMinutes; }
    public String getVenue()                       { return venue; }
    public BigDecimal getEstimatedCost()           { return estimatedCost; }

    public void setGatheringId(int id)                       { this.gatheringId       = id; }
    public void setOrganiserFidn(int fidn)                   { this.organiserFidn     = fidn; }
    public void setGatheringCategory(GatheringCategory c)    { this.gatheringCategory = c; }
    public void setGatheringDate(String d)                   { this.gatheringDate     = d; }
    public void setGatheringTime(String t)                   { this.gatheringTime     = t; }
    public void setDurationMinutes(int m)                    { this.durationMinutes   = m; }
    public void setVenue(String v)                           { this.venue             = v; }
    public void setEstimatedCost(BigDecimal c)               { this.estimatedCost     = c; }

    @Override
    public String toString() {
        return String.format(
            "Gathering #%d%n" +
            "  Category   : %s%n" +
            "  Date       : %s%n" +
            "  Time       : %s%n" +
            "  Duration   : %d minutes%n" +
            "  Venue      : %s%n" +
            "  Est. Cost  : $%s%n" +
            "  Organiser  : FIDN %d",
            gatheringId, gatheringCategory, gatheringDate, gatheringTime,
            durationMinutes, venue, estimatedCost, organiserFidn);
    }
}
