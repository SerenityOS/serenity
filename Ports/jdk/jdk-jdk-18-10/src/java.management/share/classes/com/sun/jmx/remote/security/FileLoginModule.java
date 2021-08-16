/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.jmx.remote.security;

import com.sun.jmx.mbeanserver.GetPropertyAction;
import com.sun.jmx.mbeanserver.Util;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Map;

import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.*;
import javax.management.remote.JMXPrincipal;

import com.sun.jmx.remote.util.ClassLogger;
import com.sun.jmx.remote.util.EnvHelp;

/**
 * This {@link LoginModule} performs file-based authentication.
 *
 * <p> A supplied username and password is verified against the
 * corresponding user credentials stored in a designated password file.
 * If successful then a new {@link JMXPrincipal} is created with the
 * user's name and it is associated with the current {@link Subject}.
 * Such principals may be identified and granted management privileges in
 * the access control file for JMX remote management or in a Java security
 * policy.
 *
 * By default, the following password file is used:
 * <pre>
 *     ${java.home}/conf/management/jmxremote.password
 * </pre>
 * A different password file can be specified via the <code>passwordFile</code>
 * configuration option.
 *
 * <p> This module recognizes the following <code>Configuration</code> options:
 * <dl>
 * <dt> <code>passwordFile</code> </dt>
 * <dd> the path to an alternative password file. It is used instead of
 *      the default password file.</dd>
 *
 * <dt> <code>useFirstPass</code> </dt>
 * <dd> if <code>true</code>, this module retrieves the username and password
 *      from the module's shared state, using "javax.security.auth.login.name"
 *      and "javax.security.auth.login.password" as the respective keys. The
 *      retrieved values are used for authentication. If authentication fails,
 *      no attempt for a retry is made, and the failure is reported back to
 *      the calling application.</dd>
 *
 * <dt> <code>tryFirstPass</code> </dt>
 * <dd> if <code>true</code>, this module retrieves the username and password
 *      from the module's shared state, using "javax.security.auth.login.name"
 *       and "javax.security.auth.login.password" as the respective keys.  The
 *      retrieved values are used for authentication. If authentication fails,
 *      the module uses the CallbackHandler to retrieve a new username and
 *      password, and another attempt to authenticate is made. If the
 *      authentication fails, the failure is reported back to the calling
 *      application.</dd>
 *
 * <dt> <code>storePass</code> </dt>
 * <dd> if <code>true</code>, this module stores the username and password
 *      obtained from the CallbackHandler in the module's shared state, using
 *      "javax.security.auth.login.name" and
 *      "javax.security.auth.login.password" as the respective keys.  This is
 *      not performed if existing values already exist for the username and
 *      password in the shared state, or if authentication fails.</dd>
 *
 * <dt> <code>clearPass</code> </dt>
 * <dd> if <code>true</code>, this module clears the username and password
 *      stored in the module's shared state after both phases of authentication
 *      (login and commit) have completed.</dd>
 *
 * <dt> <code>hashPasswords</code> </dt>
 * <dd> if <code>true</code>, this module replaces each clear text password
 * with its hash, if present. </dd>
 *
 * </dl>
 */
public class FileLoginModule implements LoginModule {

    private static final String PASSWORD_FILE_NAME = "jmxremote.password";

    // Location of the default password file
    @SuppressWarnings("removal")
    private static final String DEFAULT_PASSWORD_FILE_NAME =
        AccessController.doPrivileged(new GetPropertyAction("java.home")) +
        File.separatorChar + "conf" +
        File.separatorChar + "management" + File.separatorChar +
        PASSWORD_FILE_NAME;

    // Key to retrieve the stored username
    private static final String USERNAME_KEY =
        "javax.security.auth.login.name";

    // Key to retrieve the stored password
    private static final String PASSWORD_KEY =
        "javax.security.auth.login.password";

    // Log messages
    private static final ClassLogger logger =
        new ClassLogger("javax.management.remote.misc", "FileLoginModule");

    // Configurable options
    private boolean useFirstPass = false;
    private boolean tryFirstPass = false;
    private boolean storePass = false;
    private boolean clearPass = false;
    private boolean hashPasswords = false;

    // Authentication status
    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    // Supplied username and password
    private String username;
    private char[] password;
    private JMXPrincipal user;

    // Initial state
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, Object> sharedState;
    private Map<String, ?> options;
    private String passwordFile;
    private String passwordFileDisplayName;
    private boolean userSuppliedPasswordFile;
    private boolean hasJavaHomePermission;
    private HashedPasswordManager hashPwdMgr;

    /**
     * Initialize this <code>LoginModule</code>.
     *
     * @param subject the <code>Subject</code> to be authenticated.
     * @param callbackHandler a <code>CallbackHandler</code> to acquire the
     *                  user's name and password.
     * @param sharedState shared <code>LoginModule</code> state.
     * @param options options specified in the login
     *                  <code>Configuration</code> for this particular
     *                  <code>LoginModule</code>.
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String,?> sharedState,
                           Map<String,?> options)
    {

        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = Util.cast(sharedState);
        this.options = options;

        // initialize any configured options
        tryFirstPass =
                "true".equalsIgnoreCase((String)options.get("tryFirstPass"));
        useFirstPass =
                "true".equalsIgnoreCase((String)options.get("useFirstPass"));
        storePass =
                "true".equalsIgnoreCase((String)options.get("storePass"));
        clearPass =
                "true".equalsIgnoreCase((String)options.get("clearPass"));
        hashPasswords
                = "true".equalsIgnoreCase((String) options.get("hashPasswords"));

        passwordFile = (String)options.get("passwordFile");
        passwordFileDisplayName = passwordFile;
        userSuppliedPasswordFile = true;

        // set the location of the password file
        if (passwordFile == null) {
            passwordFile = DEFAULT_PASSWORD_FILE_NAME;
            userSuppliedPasswordFile = false;
            try {
                System.getProperty("java.home");
                hasJavaHomePermission = true;
                passwordFileDisplayName = passwordFile;
            } catch (SecurityException e) {
                hasJavaHomePermission = false;
                passwordFileDisplayName = PASSWORD_FILE_NAME;
            }
        }
    }

    /**
     * Begin user authentication (Authentication Phase 1).
     *
     * <p> Acquire the user's name and password and verify them against
     * the corresponding credentials from the password file.
     *
     * @return true always, since this <code>LoginModule</code>
     *          should not be ignored.
     * @exception FailedLoginException if the authentication fails.
     * @exception LoginException if this <code>LoginModule</code>
     *          is unable to perform the authentication.
     */
    public boolean login() throws LoginException {

        try {
            synchronized (this) {
                if (hashPwdMgr == null) {
                    hashPwdMgr = new HashedPasswordManager(passwordFile, hashPasswords);
                }
            }
            hashPwdMgr.loadPasswords();
        } catch (IOException ioe) {
            LoginException le = new LoginException(
                    "Error: unable to load the password file: " +
                    passwordFileDisplayName);
            throw EnvHelp.initCause(le, ioe);
        } catch (SecurityException e) {
            if (userSuppliedPasswordFile || hasJavaHomePermission) {
                throw e;
            } else {
                final FilePermission fp
                        = new FilePermission(passwordFileDisplayName, "read");
                @SuppressWarnings("removal")
                AccessControlException ace = new AccessControlException(
                        "access denied " + fp.toString());
                ace.initCause(e);
                throw ace;
            }
        }

        if (logger.debugOn()) {
            logger.debug("login",
                    "Using password file: " + passwordFileDisplayName);
        }

        // attempt the authentication
        if (tryFirstPass) {

            try {
                // attempt the authentication by getting the
                // username and password from shared state
                attemptAuthentication(true);

                // authentication succeeded
                succeeded = true;
                if (logger.debugOn()) {
                    logger.debug("login",
                        "Authentication using cached password has succeeded");
                }
                return true;

            } catch (LoginException le) {
                // authentication failed -- try again below by prompting
                cleanState();
                logger.debug("login",
                    "Authentication using cached password has failed");
            }

        } else if (useFirstPass) {

            try {
                // attempt the authentication by getting the
                // username and password from shared state
                attemptAuthentication(true);

                // authentication succeeded
                succeeded = true;
                if (logger.debugOn()) {
                    logger.debug("login",
                        "Authentication using cached password has succeeded");
                }
                return true;

            } catch (LoginException le) {
                // authentication failed
                cleanState();
                logger.debug("login",
                    "Authentication using cached password has failed");

                throw le;
            }
        }

        if (logger.debugOn()) {
            logger.debug("login", "Acquiring password");
        }

        // attempt the authentication using the supplied username and password
        try {
            attemptAuthentication(false);

            // authentication succeeded
            succeeded = true;
            if (logger.debugOn()) {
                logger.debug("login", "Authentication has succeeded");
            }
            return true;

        } catch (LoginException le) {
            cleanState();
            logger.debug("login", "Authentication has failed");

            throw le;
        }
    }

    /**
     * Complete user authentication (Authentication Phase 2).
     *
     * <p> This method is called if the LoginContext's
     * overall authentication has succeeded
     * (all the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules have succeeded).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * <code>login</code> method), then this method associates a
     * <code>JMXPrincipal</code> with the <code>Subject</code> located in the
     * <code>LoginModule</code>.  If this LoginModule's own
     * authentication attempted failed, then this method removes
     * any state that was originally saved.
     *
     * @exception LoginException if the commit fails
     * @return true if this LoginModule's own login and commit
     *          attempts succeeded, or false otherwise.
     */
    public boolean commit() throws LoginException {

        if (succeeded == false) {
            return false;
        } else {
            if (subject.isReadOnly()) {
                cleanState();
                throw new LoginException("Subject is read-only");
            }
            // add Principals to the Subject
            if (!subject.getPrincipals().contains(user)) {
                subject.getPrincipals().add(user);
            }

            if (logger.debugOn()) {
                logger.debug("commit",
                    "Authentication has completed successfully");
            }
        }
        // in any case, clean out state
        cleanState();
        commitSucceeded = true;
        return true;
    }

    /**
     * Abort user authentication (Authentication Phase 2).
     *
     * <p> This method is called if the LoginContext's overall authentication
     * failed (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules did not succeed).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * <code>login</code> and <code>commit</code> methods),
     * then this method cleans up any state that was originally saved.
     *
     * @exception LoginException if the abort fails.
     * @return false if this LoginModule's own login and/or commit attempts
     *          failed, and true otherwise.
     */
    public boolean abort() throws LoginException {

        if (logger.debugOn()) {
            logger.debug("abort",
                "Authentication has not completed successfully");
        }

        if (succeeded == false) {
            return false;
        } else if (succeeded == true && commitSucceeded == false) {

            // Clean out state
            succeeded = false;
            cleanState();
            user = null;
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
     * that were added by the <code>commit</code> method.
     *
     * @exception LoginException if the logout fails.
     * @return true in all cases since this <code>LoginModule</code>
     *          should not be ignored.
     */
    public boolean logout() throws LoginException {
        if (subject.isReadOnly()) {
            cleanState();
            throw new LoginException ("Subject is read-only");
        }
        subject.getPrincipals().remove(user);

        // clean out state
        cleanState();
        succeeded = false;
        commitSucceeded = false;
        user = null;

        if (logger.debugOn()) {
            logger.debug("logout", "Subject is being logged out");
        }

        return true;
    }

    /**
     * Attempt authentication
     *
     * @param usePasswdFromSharedState a flag to tell this method whether
     *          to retrieve the password from the sharedState.
     */
    @SuppressWarnings("unchecked")  // sharedState used as Map<String,Object>
    private void attemptAuthentication(boolean usePasswdFromSharedState)
        throws LoginException {

        // get the username and password
        getUsernamePassword(usePasswdFromSharedState);

        if (!hashPwdMgr.authenticate(username, password)) {
            // username not found or passwords do not match
            if (logger.debugOn()) {
                logger.debug("login", "Invalid username or password");
            }
            throw new FailedLoginException("Invalid username or password");
        }

        // Save the username and password in the shared state
        // only if authentication succeeded
        if (storePass &&
            !sharedState.containsKey(USERNAME_KEY) &&
            !sharedState.containsKey(PASSWORD_KEY)) {
            sharedState.put(USERNAME_KEY, username);
            sharedState.put(PASSWORD_KEY, password);
        }

        // Create a new user principal
        user = new JMXPrincipal(username);

        if (logger.debugOn()) {
            logger.debug("login",
                "User '" + username + "' successfully validated");
        }
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
     * @param usePasswdFromSharedState boolean that tells this method whether
     *          to retrieve the password from the sharedState.
     */
    private void getUsernamePassword(boolean usePasswdFromSharedState)
        throws LoginException {

        if (usePasswdFromSharedState) {
            // use the password saved by the first module in the stack
            username = (String)sharedState.get(USERNAME_KEY);
            password = (char[])sharedState.get(PASSWORD_KEY);
            return;
        }

        // acquire username and password
        if (callbackHandler == null)
            throw new LoginException("Error: no CallbackHandler available " +
                "to garner authentication information from the user");

        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("username");
        callbacks[1] = new PasswordCallback("password", false);

        try {
            callbackHandler.handle(callbacks);
            username = ((NameCallback)callbacks[0]).getName();
            char[] tmpPassword = ((PasswordCallback)callbacks[1]).getPassword();
            password = new char[tmpPassword.length];
            System.arraycopy(tmpPassword, 0,
                                password, 0, tmpPassword.length);
            ((PasswordCallback)callbacks[1]).clearPassword();

        } catch (IOException ioe) {
            LoginException le = new LoginException(ioe.toString());
            throw EnvHelp.initCause(le, ioe);
        } catch (UnsupportedCallbackException uce) {
            LoginException le = new LoginException(
                                    "Error: " + uce.getCallback().toString() +
                                    " not available to garner authentication " +
                                    "information from the user");
            throw EnvHelp.initCause(le, uce);
        }
    }

    /**
     * Clean out state because of a failed authentication attempt
     */
    private void cleanState() {
        username = null;
        if (password != null) {
            Arrays.fill(password, ' ');
            password = null;
        }

        if (clearPass) {
            sharedState.remove(USERNAME_KEY);
            sharedState.remove(PASSWORD_KEY);
        }
    }
}
