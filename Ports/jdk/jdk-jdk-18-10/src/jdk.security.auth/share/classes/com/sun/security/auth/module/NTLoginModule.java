/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.io.IOException;
import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.*;
import java.security.Principal;
import com.sun.security.auth.NTUserPrincipal;
import com.sun.security.auth.NTSidUserPrincipal;
import com.sun.security.auth.NTDomainPrincipal;
import com.sun.security.auth.NTSidDomainPrincipal;
import com.sun.security.auth.NTSidPrimaryGroupPrincipal;
import com.sun.security.auth.NTSidGroupPrincipal;
import com.sun.security.auth.NTNumericCredential;

/**
 * This {@code LoginModule}
 * renders a user's NT security information as some number of
 * {@code Principal}s
 * and associates them with a {@code Subject}.
 *
 * <p> This LoginModule recognizes the debug option.
 * If set to true in the login Configuration,
 * debug messages will be output to the output stream, System.out.
 *
 * <p> This LoginModule also recognizes the debugNative option.
 * If set to true in the login Configuration,
 * debug messages from the native component of the module
 * will be output to the output stream, System.out.
 *
 * @see javax.security.auth.spi.LoginModule
 */
public class NTLoginModule implements LoginModule {

    private NTSystem ntSystem;

    // initial state
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    // configurable option
    private boolean debug = false;
    private boolean debugNative = false;

    // the authentication status
    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    private NTUserPrincipal userPrincipal;              // user name
    private NTSidUserPrincipal userSID;                 // user SID
    private NTDomainPrincipal userDomain;               // user domain
    private NTSidDomainPrincipal domainSID;             // domain SID
    private NTSidPrimaryGroupPrincipal primaryGroup;    // primary group
    private NTSidGroupPrincipal[] groups;               // supplementary groups
    private NTNumericCredential iToken;                 // impersonation token

    /**
     * Creates an {@code NTLoginModule}.
     */
    public NTLoginModule() {}

    /**
     * Initialize this {@code LoginModule}.
     *
     * @param subject the {@code Subject} to be authenticated.
     *
     * @param callbackHandler a {@code CallbackHandler} for communicating
     *          with the end user (prompting for usernames and
     *          passwords, for example). This particular LoginModule only
     *          extracts the underlying NT system information, so this
     *          parameter is ignored.
     *
     * @param sharedState shared {@code LoginModule} state.
     *
     * @param options options specified in the login
     *                  {@code Configuration} for this particular
     *                  {@code LoginModule}.
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String,?> sharedState,
                           Map<String,?> options)
    {

        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        // initialize any configured options
        debug = "true".equalsIgnoreCase((String)options.get("debug"));
        debugNative="true".equalsIgnoreCase((String)options.get("debugNative"));

        if (debugNative == true) {
            debug = true;
        }
    }

    /**
     * Import underlying NT system identity information.
     *
     * @return true in all cases since this {@code LoginModule}
     *          should not be ignored.
     *
     * @exception FailedLoginException if the authentication fails.
     *
     * @exception LoginException if this {@code LoginModule}
     *          is unable to perform the authentication.
     */
    public boolean login() throws LoginException {

        succeeded = false; // Indicate not yet successful

        try {
            ntSystem = new NTSystem(debugNative);
        } catch (UnsatisfiedLinkError ule) {
            if (debug) {
                System.out.println("\t\t[NTLoginModule] " +
                                   "Failed in NT login");
            }
            throw new FailedLoginException
                ("Failed in attempt to import the " +
                 "underlying NT system identity information" +
                 " on " + System.getProperty("os.name"));
        }

        if (ntSystem.getName() == null) {
            throw new FailedLoginException
                ("Failed in attempt to import the " +
                 "underlying NT system identity information");
        }
        userPrincipal = new NTUserPrincipal(ntSystem.getName());
        if (debug) {
            System.out.println("\t\t[NTLoginModule] " +
                               "succeeded importing info: ");
            System.out.println("\t\t\tuser name = " +
                userPrincipal.getName());
        }

        if (ntSystem.getUserSID() != null) {
            userSID = new NTSidUserPrincipal(ntSystem.getUserSID());
            if (debug) {
                System.out.println("\t\t\tuser SID = " +
                        userSID.getName());
            }
        }
        if (ntSystem.getDomain() != null) {
            userDomain = new NTDomainPrincipal(ntSystem.getDomain());
            if (debug) {
                System.out.println("\t\t\tuser domain = " +
                        userDomain.getName());
            }
        }
        if (ntSystem.getDomainSID() != null) {
            domainSID =
                new NTSidDomainPrincipal(ntSystem.getDomainSID());
            if (debug) {
                System.out.println("\t\t\tuser domain SID = " +
                        domainSID.getName());
            }
        }
        if (ntSystem.getPrimaryGroupID() != null) {
            primaryGroup =
                new NTSidPrimaryGroupPrincipal(ntSystem.getPrimaryGroupID());
            if (debug) {
                System.out.println("\t\t\tuser primary group = " +
                        primaryGroup.getName());
            }
        }
        if (ntSystem.getGroupIDs() != null &&
            ntSystem.getGroupIDs().length > 0) {

            String[] groupSIDs = ntSystem.getGroupIDs();
            groups = new NTSidGroupPrincipal[groupSIDs.length];
            for (int i = 0; i < groupSIDs.length; i++) {
                groups[i] = new NTSidGroupPrincipal(groupSIDs[i]);
                if (debug) {
                    System.out.println("\t\t\tuser group = " +
                        groups[i].getName());
                }
            }
        }
        if (ntSystem.getImpersonationToken() != 0) {
            iToken = new NTNumericCredential(ntSystem.getImpersonationToken());
            if (debug) {
                System.out.println("\t\t\timpersonation token = " +
                        ntSystem.getImpersonationToken());
            }
        }

        succeeded = true;
        return succeeded;
    }

    /**
     * This method is called if the LoginContext's
     * overall authentication succeeded
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
     * succeeded).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * {@code login} method), then this method associates some
     * number of various {@code Principal}s
     * with the {@code Subject} located in the
     * {@code LoginModuleContext}.  If this LoginModule's own
     * authentication attempted failed, then this method removes
     * any state that was originally saved.
     *
     * @exception LoginException if the commit fails.
     *
     * @return true if this LoginModule's own login and commit
     *          attempts succeeded, or false otherwise.
     */
    public boolean commit() throws LoginException {
        if (succeeded == false) {
            if (debug) {
                System.out.println("\t\t[NTLoginModule]: " +
                    "did not add any Principals to Subject " +
                    "because own authentication failed.");
            }
            return false;
        }
        if (subject.isReadOnly()) {
            throw new LoginException ("Subject is ReadOnly");
        }
        Set<Principal> principals = subject.getPrincipals();

        // we must have a userPrincipal - everything else is optional
        if (!principals.contains(userPrincipal)) {
            principals.add(userPrincipal);
        }
        if (userSID != null && !principals.contains(userSID)) {
            principals.add(userSID);
        }

        if (userDomain != null && !principals.contains(userDomain)) {
            principals.add(userDomain);
        }
        if (domainSID != null && !principals.contains(domainSID)) {
            principals.add(domainSID);
        }

        if (primaryGroup != null && !principals.contains(primaryGroup)) {
            principals.add(primaryGroup);
        }
        for (int i = 0; groups != null && i < groups.length; i++) {
            if (!principals.contains(groups[i])) {
                principals.add(groups[i]);
            }
        }

        Set<Object> pubCreds = subject.getPublicCredentials();
        if (iToken != null && !pubCreds.contains(iToken)) {
            pubCreds.add(iToken);
        }
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
        if (debug) {
            System.out.println("\t\t[NTLoginModule]: " +
                "aborted authentication attempt");
        }

        if (succeeded == false) {
            return false;
        } else if (succeeded == true && commitSucceeded == false) {
            ntSystem = null;
            userPrincipal = null;
            userSID = null;
            userDomain = null;
            domainSID = null;
            primaryGroup = null;
            groups = null;
            iToken = null;
            succeeded = false;
        } else {
            // overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }
        return succeeded;
    }

    /**
     * Logout the user.
     *
     * <p> This method removes the {@code NTUserPrincipal},
     * {@code NTDomainPrincipal}, {@code NTSidUserPrincipal},
     * {@code NTSidDomainPrincipal}, {@code NTSidGroupPrincipal}s,
     * and {@code NTSidPrimaryGroupPrincipal}
     * that may have been added by the {@code commit} method.
     *
     * @exception LoginException if the logout fails.
     *
     * @return true in all cases since this {@code LoginModule}
     *          should not be ignored.
     */
    public boolean logout() throws LoginException {

        if (subject.isReadOnly()) {
            throw new LoginException ("Subject is ReadOnly");
        }
        Set<Principal> principals = subject.getPrincipals();
        if (principals.contains(userPrincipal)) {
            principals.remove(userPrincipal);
        }
        if (principals.contains(userSID)) {
            principals.remove(userSID);
        }
        if (principals.contains(userDomain)) {
            principals.remove(userDomain);
        }
        if (principals.contains(domainSID)) {
            principals.remove(domainSID);
        }
        if (principals.contains(primaryGroup)) {
            principals.remove(primaryGroup);
        }
        for (int i = 0; groups != null && i < groups.length; i++) {
            if (principals.contains(groups[i])) {
                principals.remove(groups[i]);
            }
        }

        Set<Object> pubCreds = subject.getPublicCredentials();
        if (pubCreds.contains(iToken)) {
            pubCreds.remove(iToken);
        }

        succeeded = false;
        commitSucceeded = false;
        userPrincipal = null;
        userDomain = null;
        userSID = null;
        domainSID = null;
        groups = null;
        primaryGroup = null;
        iToken = null;
        ntSystem = null;

        if (debug) {
                System.out.println("\t\t[NTLoginModule] " +
                                "completed logout processing");
        }
        return true;
    }
}
