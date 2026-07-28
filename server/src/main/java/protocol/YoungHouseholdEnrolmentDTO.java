package protocol;

import community.YoungHousehold;
import community.ParticipantCredential;

/**
 * JSON body for POST /api/enrolment/young-household. Bundles the
 * household record and the credential so registration is one REST call.
 */
public class YoungHouseholdEnrolmentDTO {

    private YoungHousehold        household;
    private ParticipantCredential credential;

    public YoungHouseholdEnrolmentDTO() {}

    public YoungHousehold        getHousehold()                      { return household; }
    public ParticipantCredential getCredential()                      { return credential; }
    public void                  setHousehold(YoungHousehold h)       { this.household = h; }
    public void                  setCredential(ParticipantCredential c) { this.credential = c; }
}
