package community;

import java.io.Serializable;

/**
Shared base for the two participant types - SeniorMentor and YoungHousehold. Holds the registration fields common to both: FIDN, partner details, address, and the login email used to sign in.

Subclasses add their own type-specific fields (years_wedded for mentors, the children list for households).
 */
public abstract class CommunityParticipant implements Serializable {

    private static final long serialVersionUID = 3001L;

    // Stored as registrationId internally - exposed publicly as the FIDN.
    private int registrationId;

    private String partnerOneName;
    private String partnerTwoName;
    private String partnerOneContact;
    private String partnerTwoContact;
    private String partnerOneEmail;
    private String partnerTwoEmail;
    private String familyHomeAddress;

    // Username for sign-in. Independent from the per-partner contact emails
    // so couples can change them later without losing access.
    private String loginEmail;

    public CommunityParticipant() {
    }

    public CommunityParticipant(int registrationId,
                                String partnerOneName,    String partnerTwoName,
                                String partnerOneContact, String partnerTwoContact,
                                String partnerOneEmail,   String partnerTwoEmail,
                                String familyHomeAddress, String loginEmail) {
        this.registrationId    = registrationId;
        this.partnerOneName    = partnerOneName;
        this.partnerTwoName    = partnerTwoName;
        this.partnerOneContact = partnerOneContact;
        this.partnerTwoContact = partnerTwoContact;
        this.partnerOneEmail   = partnerOneEmail;
        this.partnerTwoEmail   = partnerTwoEmail;
        this.familyHomeAddress = familyHomeAddress;
        this.loginEmail        = loginEmail;
    }

    /** The Family Identification Number assigned by the server on registration. */
    public int getFidn() { return registrationId; }

    public String getPartnerOneName()    { return partnerOneName; }
    public String getPartnerTwoName()    { return partnerTwoName; }
    public String getPartnerOneContact() { return partnerOneContact; }
    public String getPartnerTwoContact() { return partnerTwoContact; }
    public String getPartnerOneEmail()   { return partnerOneEmail; }
    public String getPartnerTwoEmail()   { return partnerTwoEmail; }
    public String getFamilyHomeAddress() { return familyHomeAddress; }
    public String getLoginEmail()        { return loginEmail; }

    public void setFidn(int id)                  { this.registrationId    = id; }
    public void setPartnerOneName(String n)      { this.partnerOneName    = n; }
    public void setPartnerTwoName(String n)      { this.partnerTwoName    = n; }
    public void setPartnerOneContact(String c)   { this.partnerOneContact = c; }
    public void setPartnerTwoContact(String c)   { this.partnerTwoContact = c; }
    public void setPartnerOneEmail(String e)     { this.partnerOneEmail   = e; }
    public void setPartnerTwoEmail(String e)     { this.partnerTwoEmail   = e; }
    public void setFamilyHomeAddress(String a)   { this.familyHomeAddress = a; }
    public void setLoginEmail(String e)          { this.loginEmail        = e; }

    @Override
    public String toString() {
        return String.format(
            "FIDN             : %d%n" +
            "Login Email      : %s%n" +
            "Partner 1 Name   : %s%n" +
            "Partner 1 Phone  : %s%n" +
            "Partner 1 Email  : %s%n" +
            "Partner 2 Name   : %s%n" +
            "Partner 2 Phone  : %s%n" +
            "Partner 2 Email  : %s%n" +
            "Home Address     : %s",
            registrationId, loginEmail,
            partnerOneName, partnerOneContact, partnerOneEmail,
            partnerTwoName, partnerTwoContact, partnerTwoEmail,
            familyHomeAddress);
    }
}
