package community;

import java.util.ArrayList;
import java.util.List;

/**
 * Young Household participant. Can register up to MAX_CHILDREN dependents.
 * Extends CommunityParticipant for the shared registration fields.
 */
public class YoungHousehold extends CommunityParticipant {

    private static final long serialVersionUID = 3004L;

    /** A household registers no more than four children. */
    public static final int MAX_CHILDREN = 4;

    // transient because List itself isn't Serializable and we don't actually
    // use Java serialisation - everything goes over the wire as JSON.
    private transient List<RegisteredChild> householdChildren;

    public YoungHousehold() {
        super();
        this.householdChildren = new ArrayList<>();
    }

    public YoungHousehold(int registrationId,
                          String partnerOneName,    String partnerTwoName,
                          String partnerOneContact, String partnerTwoContact,
                          String partnerOneEmail,   String partnerTwoEmail,
                          String familyHomeAddress, String loginEmail,
                          List<RegisteredChild> householdChildren) {
        super(registrationId, partnerOneName, partnerTwoName,
              partnerOneContact, partnerTwoContact,
              partnerOneEmail, partnerTwoEmail,
              familyHomeAddress, loginEmail);
        this.householdChildren = deepCopy(householdChildren);
    }

    /** Returns a defensive copy so callers can't mutate our internal list. */
    public List<RegisteredChild> getHouseholdChildren() {
        List<RegisteredChild> copy = new ArrayList<>();
        for (RegisteredChild rc : householdChildren) {
            copy.add(new RegisteredChild(rc.getChildAge(), rc.getChildGender()));
        }
        return copy;
    }

    public void setHouseholdChildren(List<RegisteredChild> list) {
        this.householdChildren = deepCopy(list);
    }

    public int getChildCount() { return householdChildren.size(); }

    // Doesn't cap the list size - the resource layer rejects oversized
    // requests with a clear error. Silently truncating here would hide
    // a validation error instead of reporting it.
    private static List<RegisteredChild> deepCopy(List<RegisteredChild> source) {
        List<RegisteredChild> copy = new ArrayList<>();
        if (source == null) return copy;
        for (RegisteredChild rc : source) {
            if (rc != null) copy.add(new RegisteredChild(rc.getChildAge(), rc.getChildGender()));
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(String.format("%nChildren Registered: %d", householdChildren.size()));
        for (int i = 0; i < householdChildren.size(); i++) {
            sb.append(String.format("%n  Child %-2d          : %s",
                      (i + 1), householdChildren.get(i)));
        }
        return sb.toString();
    }
}