package protocol;

/**
JSON body for POST /api/membership/sign-in.

Plain-text password is acceptable here because the TLS layer encrypts the entire request body before it leaves the browser
 */
public class SignInRequestDTO {

    private String loginEmail;
    private String passwordPlain;

    public SignInRequestDTO() {}

    public String getLoginEmail()              { return loginEmail; }
    public String getPasswordPlain()           { return passwordPlain; }
    public void   setLoginEmail(String e)      { this.loginEmail = e; }
    public void   setPasswordPlain(String p)   { this.passwordPlain = p; }
}
