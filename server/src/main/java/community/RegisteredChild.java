package community;

import java.io.Serializable;

/**
 * A single child belonging to a YoungHousehold. Persisted as one row in the registered_children table, linked to the parent household by FIDN.
 */
public class RegisteredChild implements Serializable {

    private static final long serialVersionUID = 3003L;

    public static final String MALE = "Male";
    public static final String FEMALE = "Female";

    private int childAge;
    private String childGender;

    public RegisteredChild() {
        // No-arg ctor for Jackson JSON deserialisation.
    }

    public RegisteredChild(int childAge, String childGender) {
        this.childAge = childAge;
        this.childGender = childGender;
    }

    public int getChildAge()            { return childAge; }
    public String getChildGender()      { return childGender; }
    public void setChildAge(int age)    { this.childAge = age; }
    public void setChildGender(String g) { this.childGender = g; }

    @Override
    public String toString() {
        return String.format("Age %d, %s", childAge, childGender);
    }
}
