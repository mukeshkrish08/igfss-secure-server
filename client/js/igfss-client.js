/**
 * igfss-client.js - JavaScript REST client for the IGFSS web service.
 *
 * Provides a single object - IgfssClient - with one method per server
 * endpoint. Every method:
 *   - Sends JSON over HTTPS to the Jersey REST API at /api/...
 *   - Awaits the JSON response
 *   - Returns the parsed ApiResponse object
 *
 * Sign-in state is held in sessionStorage under the key
 * IGFSS_AUTHENTICATED_FIDN. Pages that require sign-in call
 * IgfssSession.requireSignIn() at the top of their script - that
 * function redirects unauthenticated visitors back to the gate page.
 */

const IGFSS_API_BASE = "/api";

const IgfssClient = (function () {

    async function postJson(path, body) {
        const response = await fetch(IGFSS_API_BASE + path, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });
        // Server always returns 200 with an ApiResponse body, even on validation failure
        return await response.json();
    }

    async function getJson(path) {
        const response = await fetch(IGFSS_API_BASE + path);
        return await response.json();
    }

    return {
        // Senior Mentor enrolment
        enrolSeniorMentor: function (mentor, credential) {
            return postJson("/enrolment/senior-mentor", {
                mentor: mentor,
                credential: credential
            });
        },
        // Young Household enrolment
        enrolYoungHousehold: function (household, credential) {
            return postJson("/enrolment/young-household", {
                household: household,
                credential: credential
            });
        },
        // Sign in
        signIn: function (loginEmail, passwordPlain) {
            return postJson("/membership/sign-in", {
                loginEmail: loginEmail,
                passwordPlain: passwordPlain
            });
        },
        // Directory
        listAllFidns:    function ()      { return getJson("/directory/fidns"); },
        getParticipant:  function (fidn)  { return getJson("/directory/" + fidn); },
        // Gatherings
        createGathering: function (g)     { return postJson("/gatherings", g); },
        listGatherings:  function ()      { return getJson("/gatherings"); }
    };
})();

/**
 * IgfssSession - browser-side session helper.
 *
 * Stores the authenticated FIDN and login email in sessionStorage so they
 * survive page navigation but are wiped when the browser tab closes.
 *
 * Pages that require sign-in put this at the top of their script:
 *   if (!IgfssSession.requireSignIn()) return;
 */
const IgfssSession = (function () {

    const FIDN_KEY  = "IGFSS_AUTHENTICATED_FIDN";
    const EMAIL_KEY = "IGFSS_AUTHENTICATED_EMAIL";

    return {
        recordSignIn: function (fidn, email) {
            sessionStorage.setItem(FIDN_KEY, String(fidn));
            if (email) sessionStorage.setItem(EMAIL_KEY, email);
        },
        currentFidn: function () {
            const value = sessionStorage.getItem(FIDN_KEY);
            return value ? parseInt(value, 10) : null;
        },
        currentEmail: function () {
            return sessionStorage.getItem(EMAIL_KEY);
        },
        signOut: function () {
            sessionStorage.removeItem(FIDN_KEY);
            sessionStorage.removeItem(EMAIL_KEY);
        },
        /**
         * Redirects to the sign-in page if the user is not authenticated.
         * Returns true when the page should continue rendering.
         */
        requireSignIn: function () {
            if (this.currentFidn() === null) {
                window.location.href = "membership-gate.html";
                return false;
            }
            return true;
        }
    };
})();

/**
 * IgfssValidator - mirrors the server-side CredentialPolicy rules so the
 * frontend can give immediate feedback before submitting the form.
 *
 * The server still re-validates everything - this is purely UX.
 */
const IgfssValidator = {

    PASSWORD_LENGTH: 10,

    /** Returns null when the password is acceptable, or a reason string. */
    describePasswordViolation: function (password) {
        if (!password || password.length !== this.PASSWORD_LENGTH) {
            return "Password must be exactly " + this.PASSWORD_LENGTH + " characters long.";
        }
        let hasLetter = false;
        let hasDigit  = false;
        for (let i = 0; i < password.length; i++) {
            const c = password.charAt(i);
            if (/[A-Za-z]/.test(c))      hasLetter = true;
            else if (/[0-9]/.test(c))    hasDigit  = true;
            else
                return "Password must contain only letters and numbers (no spaces or symbols).";
        }
        if (!hasLetter) return "Password must contain at least one letter.";
        if (!hasDigit)  return "Password must contain at least one number.";
        return null;
    },

    /** Returns null when the email looks valid, or a reason string. */
    describeEmailViolation: function (email) {
        if (!email || !email.trim()) return "Login email cannot be blank.";
        if (!email.includes("@"))    return "Login email must contain an '@' character.";
        return null;
    }
};

/**
 * IgfssUi - small DOM helpers shared by every page.
 */
const IgfssUi = {

    /** Display an accepted/rejected message in a status div. */
    showStatus: function (statusElementId, accepted, message) {
        const el = document.getElementById(statusElementId);
        if (!el) return;
        el.classList.remove("status-accepted", "status-rejected");
        el.classList.add("visible");
        el.classList.add(accepted ? "status-accepted" : "status-rejected");
        el.textContent = (accepted ? "[ACCEPTED] " : "[REJECTED] ") + message;
    },

    /** Read .value from an input by id, trimmed. */
    val: function (id) {
        const el = document.getElementById(id);
        return el ? el.value.trim() : "";
    },

    /** Read .value parsed as integer, or NaN. */
    intVal: function (id) {
        const raw = this.val(id);
        if (raw === "") return NaN;
        const n = parseInt(raw, 10);
        return Number.isNaN(n) ? NaN : n;
    }
};
