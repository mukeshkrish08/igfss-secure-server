package protocol;

import java.io.Serializable;

/**
Uniform JSON envelope returned by every REST endpoint:

accepted - true on success, false on validation or lookup failure
replyMessage - human-readable message the UI can show directly
replyData - optional payload (FIDN list, participant record, etc.)

Mirrors the HappyFamiliesReply pattern used elsewhere in the system so the design language stays consistent.
 */
public class ApiResponse implements Serializable {

    private static final long serialVersionUID = 3005L;

    private boolean accepted;
    private String  replyMessage;
    // Transient because the runtime type of replyData might not be Serializable. Jackson handles JSON serialisation regardless.
    private transient Object replyData;

    public ApiResponse() {
        // No-arg constructor for Jackson
    }

    private ApiResponse(boolean accepted, String replyMessage, Object replyData) {
        this.accepted     = accepted;
        this.replyMessage = replyMessage;
        this.replyData    = replyData;
    }

    /** Success with a payload (e.g. participant record, FIDN list). */
    public static ApiResponse accepted(String message, Object data) {
        return new ApiResponse(true, message, data);
    }

    /** Success with no payload (e.g. registration confirmation). */
    public static ApiResponse accepted(String message) {
        return new ApiResponse(true, message, null);
    }

    /** Failure - always carries a clear, user-facing reason. */
    public static ApiResponse rejected(String message) {
        return new ApiResponse(false, message, null);
    }

    public boolean isAccepted()       { return accepted; }
    public String  getReplyMessage()  { return replyMessage; }
    public Object  getReplyData()     { return replyData; }

    public void setAccepted(boolean a)       { this.accepted = a; }
    public void setReplyMessage(String m)    { this.replyMessage = m; }
    public void setReplyData(Object d)       { this.replyData = d; }

    @Override
    public String toString() {
        return (accepted ? "[ACCEPTED] " : "[REJECTED] ") + replyMessage;
    }
}
