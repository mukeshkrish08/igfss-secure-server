package community;

/**
 * Senior Mentor participant. Adds yearsWedded (must be at least 20) to the shared CommunityParticipant fields.
 */
public class SeniorMentor extends CommunityParticipant {

    private static final long serialVersionUID = 3002L;

    private int yearsWedded;

    public SeniorMentor() {
        super();
    }

    public SeniorMentor(int registrationId,
                        String partnerOneName,    String partnerTwoName,
                        String partnerOneContact, String partnerTwoContact,
                        String partnerOneEmail,   String partnerTwoEmail,
                        String familyHomeAddress, String loginEmail,
                        int yearsWedded) {
        super(registrationId, partnerOneName, partnerTwoName,
              partnerOneContact, partnerTwoContact,
              partnerOneEmail, partnerTwoEmail,
              familyHomeAddress, loginEmail);
        this.yearsWedded = yearsWedded;
    }

    public int getYearsWedded()             { return yearsWedded; }
    public void setYearsWedded(int years)   { this.yearsWedded = years; }

    @Override
    public String toString() {
        return super.toString()
             + String.format("%nYears Married    : %d", yearsWedded);
    }
}
