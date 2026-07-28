package protocol;

import community.SeniorMentor;
import community.ParticipantCredential;

/**
 * JSON body for POST /api/enrolment/senior-mentor. Bundles the mentor
 * record and the credential so registration is a single REST call.
 */
public class SeniorMentorEnrolmentDTO {

    private SeniorMentor          mentor;
    private ParticipantCredential credential;

    public SeniorMentorEnrolmentDTO() {}

    public SeniorMentor          getMentor()                       { return mentor; }
    public ParticipantCredential getCredential()                   { return credential; }
    public void                  setMentor(SeniorMentor m)         { this.mentor = m; }
    public void                  setCredential(ParticipantCredential c) { this.credential = c; }
}
