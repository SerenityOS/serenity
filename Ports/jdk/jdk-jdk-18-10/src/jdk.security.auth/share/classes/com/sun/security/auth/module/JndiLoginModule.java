/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.security.auth.module;

import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.*;
import javax.naming.*;
import javax.naming.directory.*;

import java.util.Map;
import java.util.LinkedList;

import com.sun.security.auth.UnixPrincipal;
import com.sun.security.auth.UnixNumericUserPrincipal;
import com.sun.security.auth.UnixNumericGroupPrincipal;
import static sun.security.util.ResourcesMgr.getAuthResourceString;


/**
 * The module prompts for a username and password
 * and then verifies the password against the password stored in
 * a directory service configured under JNDI.
 *
 * <p> This {@code LoginModule} interoperates with
 * any conformant JNDI service provider.  To direct this
 * {@code LoginModule} to use a specific JNDI service provider,
 * two options must be specified in the login {@code Configuration}
 * for this {@code LoginModule}.
 * <pre>
 *      user.provider.url=<b>name_service_url</b>
 *      group.provider.url=<b>name_service_url</b>
 * </pre>
 *
 * <b>name_service_url</b> specifies
 * the directory service and path where this {@code LoginModule}
 * can access the relevant user and group information.  Because this
 * {@code LoginModule} only performs one-level searches to
 * find the relevant user information, the {@code URL}
 * must point to a directory one level above where the user and group
 * information is stored in the directory service.
 * For example, to instruct this {@code LoginModule}
 * to contact a NIS server, the following URLs must be specified:
 * <pre>
 *    user.provider.url="nis://<b>NISServerHostName</b>/<b>NISDomain</b>/user"
 *    group.provider.url="nis://<b>NISServerHostName</b>/<b>NISDomain</b>/system/group"
 * </pre>
 *
 * <b>NISServerHostName</b> specifies the server host name of the
 * NIS server (for example, <i>nis.sun.com</i>, and <b>NISDomain</b>
 * specifies the domain for that NIS server (for example, <i>jaas.sun.com</i>.
 * To contact an LDAP server, the following URLs must be specified:
 * <pre>
 *    user.provider.url="ldap://<b>LDAPServerHostName</b>/<b>LDAPName</b>"
 *    group.provider.url="ldap://<b>LDAPServerHostName</b>/<b>LDAPName</b>"
 * </pre>
 *
 * <b>LDAPServerHostName</b> specifies the server host name of the
 * LDAP server, which may include a port number
 * (for example, <i>ldap.sun.com:389</i>),
 * and <b>LDAPName</b> specifies the entry name in the LDAP directory
 * (for example, <i>ou=People,o=Sun,c=US</i> and <i>ou=Groups,o=Sun,c=US</i>
 * for user and group information, respectively).
 *
 * <p> The format in which the user's information must be stored in
 * the directory service is specified in RFC 2307.  Specifically,
 * this {@code LoginModule} will search for the user's entry in the
 * directory service using the user's <i>uid</i> attribute,
 * where <i>uid=<b>username</b></i>.  If the search succeeds,
 * this {@code LoginModule} will then
 * obtain the user's encrypted password from the retrieved entry
 * using the <i>userPassword</i> attribute.
 * This {@code LoginModule} assumes that the password is stored
 * as a byte array, which when converted to a {@code String},
 * has the following format:
 * <pre>
 *      "{crypt}<b>encrypted_password</b>"
 * </pre>
 *
 * The LDAP directory server must be configured
 * to permit read access to the userPassword attribute.
 * If the user entered a valid username and password,
 * this {@code LoginModule} associates a
 * {@code UnixPrincipal}, {@code UnixNumericUserPrincipal},
 * and the relevant UnixNumericGroupPrincipals with the
 * {@code Subject}.
 *
 * <p> This LoginModule also recognizes the following {@code Configuration}
 * options:
 * <pre>
 *    debug          if, true, debug messages are output to System.out.
 *
 *    useFirstPass   if, true, this LoginModule retrieves the
 *                   username and password from the module's shared state,
 *                   using "javax.security.auth.login.name" and
 *                   "javax.security.auth.login.password" as the respective
 *                   keys.  The retrieved values are used for authentication.
 *                   If authentication fails, no attempt for a retry is made,
 *                   and the failure is reported back to the calling
 *                   application.
 *
 *    tryFirstPass   if, true, this LoginModule retrieves the
 *                   the username and password from the module's shared state,
 *                   using "javax.security.auth.login.name" and
 *                   "javax.security.auth.login.password" as the respective
 *                   keys.  The retrieved values are used for authentication.
 *                   If authentication fails, the module uses the
 *                   CallbackHandler to retrieve a new username and password,
 *                   and another attempt to authenticate is made.
 *                   If the authentication fails, the failure is reported
 *                   back to the calling application.
 *
 *    storePass      if, true, this LoginModule stores the username and password
 *                   obtained from the CallbackHandler in the module's
 *                   shared state, using "javax.security.auth.login.name" and
 *                   "javax.security.auth.login.password" as the respective
 *                   keys.  This is not performed if existing values already
 *                   exist for the username and password in the shared state,
 *                   or if authentication fails.
 *
 *    clearPass     if, true, this {@code LoginModule} clears the
 *                  username and password stored in the module's shared state
 *                  after both phases of authentication (login and commit)
 *                  have completed.
 * </pre>
 *
 */
public class JndiLoginModule implements LoginModule {

    /**
     * Directory service/path where this module can access the relevent
     * user information.
     */
    public final String USER_PROVIDER = "user.provider.url";

    /**
     * Directory service/path where this module can access the relevent
     * group information.
     */
    public final String GROUP_PROVIDER = "group.provider.url";

    // configurable options
    private boolean debug = false;
    private boolean strongDebug = false;
    private String userProvider;
    private String groupProvider;
    private boolean useFirstPass = false;
    private boolean tryFirstPass = false;
    private boolean storePass = false;
    private boolean clearPass = false;

    // the authentication status
    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    // username, password, and JNDI context
    private String username;
    private char[] password;
    DirContext ctx;

    // the user (assume it is a UnixPrincipal)
    private UnixPrincipal userPrincipal;
    private UnixNumericUserPrincipal UIDPrincipal;
    private UnixNumericGroupPrincipal GIDPrincipal;
    private LinkedList<UnixNumericGroupPrincipal> supplementaryGroups =
                                new LinkedList<>();

    // initial state
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, Object> sharedState;
    private Map<String, ?> options;

    private static final String CRYPT = "{crypt}";
    private static final String USER_PWD = "userPassword";
    private static final String USER_UID = "uidNumber";
    private static final String USER_GID = "gidNumber";
    private static final String GROUP_ID = "gidNumber";
    private static final String NAME = "javax.security.auth.login.name";
    private static final String PWD = "javax.security.auth.login.password";

    /**
     * Creates a {@code JndiLoginModule}.
     */
    public JndiLoginModule() {}

    /**
     * Initialize this {@code LoginModule}.
     *
     * @param subject the {@code Subject} to be authenticated.
     *
     * @param callbackHandler a {@code CallbackHandler} for communicating
     *                  with the end user (prompting for usernames and
     *                  passwords, for example).
     *
     * @param sharedState shared {@code LoginModule} state.
     *
     * @param options options specified in the login
     *                  {@code Configuration} for this particular
     *                  {@code LoginModule}.
     */
    // Unchecked warning from (Map<String, Object>)sharedState is safe
    // since javax.security.auth.login.LoginContext passes a raw HashMap.
    // Unchecked warnings from options.get(String) are safe since we are
    // passing known keys.
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String,?> sharedState,
                           Map<String,?> options) {

        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = (Map<String, Object>)sharedState;
        this.options = options;

        // initialize any configured options
        debug = "true".equalsIgnoreCase((String)options.get("debug"));
        strongDebug =
                "true".equalsIgnoreCase((String)options.get("strongDebug"));
        userProvider = (String)options.get(USER_PROVIDER);
        groupProvider = (String)options.get(GROUP_PROVIDER);
        tryFirstPass =
                "true".equalsIgnoreCase((String)options.get("tryFirstPass"));
        useFirstPass =
                "true".equalsIgnoreCase((String)options.get("useFirstPass"));
        storePass =
                "true".equalsIgnoreCase((String)options.get("storePass"));
        clearPass =
                "true".equalsIgnoreCase((String)options.get("clearPass"));
    }

    /**
     * Prompt for username and password.
     * Verify the password against the relevant name service.
     *
     * @return true always, since this {@code LoginModule}
     *          should not be ignored.
     *
     * @exception FailedLoginException if the authentication fails.
     *
     * @exception LoginException if this {@code LoginModule}
     *          is unable to perform the authentication.
     */
    public boolean login() throws LoginException {

        if (userProvider == null) {
            throw new LoginException
                ("Error: Unable to locate JNDI user provider");
        }
        if (groupProvider == null) {
            throw new LoginException
                ("Error: Unable to locate JNDI group provider");
        }

        if (debug) {
            System.out.println("\t\t[JndiLoginModule] user provider: " +
                                userProvider);
            System.out.println("\t\t[JndiLoginModule] group provider: " +
                                groupProvider);
        }

        // attempt the authentication
        if (tryFirstPass) {

            try {
                // attempt the authentication by getting the
                // username and password from shared state
                attemptAuthentication(true);

                // authentication succeeded
                succeeded = true;
                if (debug) {
                    System.out.println("\t\t[JndiLoginModule] " +
                                "tryFirstPass succeeded");
                }
                return true;
            } catch (LoginException le) {
                // authentication failed -- try again below by prompting
                cleanState();
                if (debug) {
                    System.out.println("\t\t[JndiLoginModule] " +
                                "tryFirstPass failed with:" +
                                le.toString());
                }
            }

        } else if (useFirstPass) {

            try {
                // attempt the authentication by getting the
                // username and password from shared state
                attemptAuthentication(true);

                // authentication succeeded
                succeeded = true;
                if (debug) {
                    System.out.println("\t\t[JndiLoginModule] " +
                                "useFirstPass succeeded");
                }
                return true;
            } catch (LoginException le) {
                // authentication failed
                cleanState();
                if (debug) {
                    System.out.println("\t\t[JndiLoginModule] " +
                                "useFirstPass failed");
                }
                throw le;
            }
        }

        // attempt the authentication by prompting for the username and pwd
        try {
            attemptAuthentication(false);

            // authentication succeeded
           succeeded = true;
            if (debug) {
                System.out.println("\t\t[JndiLoginModule] " +
                                "regular authentication succeeded");
            }
            return true;
        } catch (LoginException le) {
            cleanState();
            if (debug) {
                System.out.println("\t\t[JndiLoginModule] " +
                                "regular authentication failed");
            }
            throw le;
        }
    }

    /**
     * Abstract method to commit the authentication process (phase 2).
     *
     * <p> This method is called if the LoginContext's
     * overall authentication succeeded
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
     * succeeded).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * {@code login} method), then this method associates a
     * {@code UnixPrincipal}
     * with the {@code Subject} located in the
     * {@code LoginModule}.  If this LoginModule's own
     * authentication attempted failed, then this method removes
     * any state that was originally saved.
     *
     * @exception LoginException if the commit fails
     *
     * @return true if this LoginModule's own login and commit
     *          attempts succeeded, or false otherwise.
     */
    public boolean commit() throws LoginException {

        if (succeeded == false) {
            return false;
        } else {
            if (subject.isReadOnly()) {
                cleanState();
                throw new LoginException ("Subject is Readonly");
            }
            // add Principals to the Subject
            if (!subject.getPrincipals().contains(userPrincipal))
                subject.getPrincipals().add(userPrincipal);
            if (!subject.getPrincipals().contains(UIDPrincipal))
                subject.getPrincipals().add(UIDPrincipal);
            if (!subject.getPrincipals().contains(GIDPrincipal))
                subject.getPrincipals().add(GIDPrincipal);
            for (int i = 0; i < supplementaryGroups.size(); i++) {
                if (!subject.getPrincipals().contains
                        (supplementaryGroups.get(i)))
                    subject.getPrincipals().add(supplementaryGroups.get(i));
            }

            if (debug) {
                System.out.println("\t\t[JndiLoginModule]: " +
                                   "added UnixPrincipal,");
                System.out.println("\t\t\t\tUnixNumericUserPrincipal,");
                System.out.println("\t\t\t\tUnixNumericGroupPrincipal(s),");
                System.out.println("\t\t\t to Subject");
            }
        }
        // in any case, clean out state
        cleanState();
        commitSucceeded = true;
        return true;
    }

    /**
     * This method is called if the LoginContext's
     * overall authentication failed.
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
     * did not succeed).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * {@code login} and {@code commit} methods),
     * then this method cleans up any state that was originally saved.
     *
     * @exception LoginException if the abort fails.
     *
     * @return false if this LoginModule's own login and/or commit attempts
     *          failed, and true otherwise.
     */
    public boolean abort() throws LoginException {
        if (debug)
            System.out.println("\t\t[JndiLoginModule]: " +
                "aborted authentication failed");

        if (succeeded == false) {
            return false;
        } else if (succeeded == true && commitSucceeded == false) {

            // Clean out state
            succeeded = false;
            cleanState();

            userPrincipal = null;
            UIDPrincipal = null;
            GIDPrincipal = null;
            supplementaryGroups = new LinkedList<UnixNumericGroupPrincipal>();
        } else {
            // overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }
        return true;
    }

    /**
     * Logout a user.
     *
     * <p> This method removes the Principals
     * that were added by the {@code commit} method.
     *
     * @exception LoginException if the logout fails.
     *
     * @return true in all cases since this {@code LoginModule}
     *          should not be ignored.
     */
    public boolean logout() throws LoginException {
        if (subject.isReadOnly()) {
            cleanState();
            throw new LoginException ("Subject is Readonly");
        }
        subject.getPrincipals().remove(userPrincipal);
        subject.getPrincipals().remove(UIDPrincipal);
        subject.getPrincipals().remove(GIDPrincipal);
        for (int i = 0; i < supplementaryGroups.size(); i++) {
            subject.getPrincipals().remove(supplementaryGroups.get(i));
        }


        // clean out state
        cleanState();
        succeeded = false;
        commitSucceeded = false;

        userPrincipal = null;
        UIDPrincipal = null;
        GIDPrincipal = null;
        supplementaryGroups = new LinkedList<UnixNumericGroupPrincipal>();

        if (debug) {
            System.out.println("\t\t[JndiLoginModule]: " +
                "logged out Subject");
        }
        return true;
    }

    /**
     * Attempt authentication
     *
     * @param getPasswdFromSharedState boolean that tells this method whether
     *          to retrieve the password from the sharedState.
     */
    private void attemptAuthentication(boolean getPasswdFromSharedState)
    throws LoginException {

        String encryptedPassword = null;

        // first get the username and password
        getUsernamePassword(getPasswdFromSharedState);

        try {

            // get the user's passwd entry from the user provider URL
            InitialContext iCtx = new InitialContext();
            ctx = (DirContext)iCtx.lookup(userProvider);

            /*
            SearchControls controls = new SearchControls
                                        (SearchControls.ONELEVEL_SCOPE,
                                        0,
                                        5000,
                                        new String[] { USER_PWD },
                                        false,
                                        false);
            */

            SearchControls controls = new SearchControls();
            NamingEnumeration<SearchResult> ne = ctx.search("",
                                        "(uid=" + username + ")",
                                        controls);
            if (ne.hasMore()) {
                SearchResult result = ne.next();
                Attributes attributes = result.getAttributes();

                // get the password

                // this module works only if the LDAP directory server
                // is configured to permit read access to the userPassword
                // attribute. The directory administrator need to grant
                // this access.
                //
                // A workaround would be to make the server do authentication
                // by setting the Context.SECURITY_PRINCIPAL
                // and Context.SECURITY_CREDENTIALS property.
                // However, this would make it not work with systems that
                // don't do authentication at the server (like NIS).
                //
                // Setting the SECURITY_* properties and using "simple"
                // authentication for LDAP is recommended only for secure
                // channels. For nonsecure channels, SSL is recommended.

                Attribute pwd = attributes.get(USER_PWD);
                String encryptedPwd = new String((byte[])pwd.get(), "UTF8");
                encryptedPassword = encryptedPwd.substring(CRYPT.length());

                // check the password
                if (verifyPassword
                    (encryptedPassword, new String(password)) == true) {

                    // authentication succeeded
                    if (debug)
                        System.out.println("\t\t[JndiLoginModule] " +
                                "attemptAuthentication() succeeded");

                } else {
                    // authentication failed
                    if (debug)
                        System.out.println("\t\t[JndiLoginModule] " +
                                "attemptAuthentication() failed");
                    throw new FailedLoginException("Login incorrect");
                }

                // save input as shared state only if
                // authentication succeeded
                if (storePass &&
                    !sharedState.containsKey(NAME) &&
                    !sharedState.containsKey(PWD)) {
                    sharedState.put(NAME, username);
                    sharedState.put(PWD, password);
                }

                // create the user principal
                userPrincipal = new UnixPrincipal(username);

                // get the UID
                Attribute uid = attributes.get(USER_UID);
                String uidNumber = (String)uid.get();
                UIDPrincipal = new UnixNumericUserPrincipal(uidNumber);
                if (debug && uidNumber != null) {
                    System.out.println("\t\t[JndiLoginModule] " +
                                "user: '" + username + "' has UID: " +
                                uidNumber);
                }

                // get the GID
                Attribute gid = attributes.get(USER_GID);
                String gidNumber = (String)gid.get();
                GIDPrincipal = new UnixNumericGroupPrincipal
                                (gidNumber, true);
                if (debug && gidNumber != null) {
                    System.out.println("\t\t[JndiLoginModule] " +
                                "user: '" + username + "' has GID: " +
                                gidNumber);
                }

                // get the supplementary groups from the group provider URL
                ctx = (DirContext)iCtx.lookup(groupProvider);
                ne = ctx.search("", new BasicAttributes("memberUid", username));

                while (ne.hasMore()) {
                    result = ne.next();
                    attributes = result.getAttributes();

                    gid = attributes.get(GROUP_ID);
                    String suppGid = (String)gid.get();
                    if (!gidNumber.equals(suppGid)) {
                        UnixNumericGroupPrincipal suppPrincipal =
                            new UnixNumericGroupPrincipal(suppGid, false);
                        supplementaryGroups.add(suppPrincipal);
                        if (debug && suppGid != null) {
                            System.out.println("\t\t[JndiLoginModule] " +
                                "user: '" + username +
                                "' has Supplementary Group: " +
                                suppGid);
                        }
                    }
                }

            } else {
                // bad username
                if (debug) {
                    System.out.println("\t\t[JndiLoginModule]: User not found");
                }
                throw new FailedLoginException("User not found");
            }
        } catch (NamingException ne) {
            // bad username
            if (debug) {
                System.out.println("\t\t[JndiLoginModule]:  User not found");
                ne.printStackTrace();
            }
            throw new FailedLoginException("User not found");
        } catch (java.io.UnsupportedEncodingException uee) {
            // password stored in incorrect format
            if (debug) {
                System.out.println("\t\t[JndiLoginModule]:  " +
                                "password incorrectly encoded");
                uee.printStackTrace();
            }
            throw new LoginException("Login failure due to incorrect " +
                                "password encoding in the password database");
        }

        // authentication succeeded
    }

    /**
     * Get the username and password.
     * This method does not return any value.
     * Instead, it sets global name and password variables.
     *
     * <p> Also note that this method will set the username and password
     * values in the shared state in case subsequent LoginModules
     * want to use them via use/tryFirstPass.
     *
     * @param getPasswdFromSharedState boolean that tells this method whether
     *          to retrieve the password from the sharedState.
     */
    private void getUsernamePassword(boolean getPasswdFromSharedState)
    throws LoginException {

        if (getPasswdFromSharedState) {
            // use the password saved by the first module in the stack
            username = (String)sharedState.get(NAME);
            password = (char[])sharedState.get(PWD);
            return;
        }

        // prompt for a username and password
        if (callbackHandler == null)
            throw new LoginException("Error: no CallbackHandler available " +
                "to garner authentication information from the user");

        String protocol = userProvider.substring(0, userProvider.indexOf(':'));

        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback(protocol + " "
                                            + getAuthResourceString("username."));
        callbacks[1] = new PasswordCallback(protocol + " " +
                                                getAuthResourceString("password."),
                                            false);

        try {
            callbackHandler.handle(callbacks);
            username = ((NameCallback)callbacks[0]).getName();
            char[] tmpPassword = ((PasswordCallback)callbacks[1]).getPassword();
            password = new char[tmpPassword.length];
            System.arraycopy(tmpPassword, 0,
                                password, 0, tmpPassword.length);
            ((PasswordCallback)callbacks[1]).clearPassword();

        } catch (java.io.IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException("Error: " + uce.getCallback().toString() +
                        " not available to garner authentication information " +
                        "from the user");
        }

        // print debugging information
        if (strongDebug) {
            System.out.println("\t\t[JndiLoginModule] " +
                                "user entered username: " +
                                username);
            System.out.print("\t\t[JndiLoginModule] " +
                                "user entered password: ");
            for (int i = 0; i < password.length; i++)
                System.out.print(password[i]);
            System.out.println();
        }
    }

    /**
     * Verify a password against the encrypted passwd from /etc/shadow
     */
    private boolean verifyPassword(String encryptedPassword, String password) {

        if (encryptedPassword == null)
            return false;

        Crypt c = new Crypt();
        try {
            byte[] oldCrypt = encryptedPassword.getBytes("UTF8");
            byte[] newCrypt = c.crypt(password.getBytes("UTF8"),
                                      oldCrypt);
            if (newCrypt.length != oldCrypt.length)
                return false;
            for (int i = 0; i < newCrypt.length; i++) {
                if (oldCrypt[i] != newCrypt[i])
                    return false;
            }
        } catch (java.io.UnsupportedEncodingException uee) {
            // cannot happen, but return false just to be safe
            return false;
        }
        return true;
    }

    /**
     * Clean out state because of a failed authentication attempt
     */
    private void cleanState() {
        username = null;
        if (password != null) {
            for (int i = 0; i < password.length; i++)
                password[i] = ' ';
            password = null;
        }
        ctx = null;

        if (clearPass) {
            sharedState.remove(NAME);
            sharedState.remove(PWD);
        }
    }
}
