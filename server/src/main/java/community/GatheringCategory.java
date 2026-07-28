package community;

/**
The three permitted gathering types  social event, workshop, or activity.

Using an enum keeps invalid categories out at compile time and as a MySQL ENUM at the database tier.
 */
public enum GatheringCategory {

    /** Informal social events - dinners, picnics, meet-and-greets. */
    SOCIAL_GATHERING,

    /** Learning sessions - parenting workshops, financial literacy talks. */
    WORKSHOP,

    /** Active community pursuits - park clean-ups, walking groups. */
    COMMUNITY_ACTIVITY
}
