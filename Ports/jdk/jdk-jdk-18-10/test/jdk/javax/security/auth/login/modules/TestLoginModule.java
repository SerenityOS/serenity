/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package login;

import java.io.IOException;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import com.sun.security.auth.UserPrincipal;

/**
 * Custom JAAS login module which will be loaded through Java LoginContext when
 * it is bundled by Strict/Auto/Unnamed modules.
 */
public class TestLoginModule implements LoginModule {

    private static final String USER_NAME = "testUser";
    private static final String PASSWORD = "testPassword";
    private Subject subject;
    private CallbackHandler callbackHandler;
    private UserPrincipal userPrincipal;
    private String username;
    private String password;
    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {

        this.subject = subject;
        this.callbackHandler = callbackHandler;
        System.out.println(String.format(
                "'%s' login module initialized", this.getClass()));
    }

    /*
     * Authenticate the user by prompting for a username and password.
     */
    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("No CallbackHandler available");
        }

        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(callbacks);
            username = ((NameCallback) callbacks[0]).getName();
            password = new String(((PasswordCallback) callbacks[1])
                    .getPassword());
            System.out.println(String.format("'%s' login module found username"
                    + " as '%s' and password as '%s'", this.getClass(),
                    username, password));
            if (username.equals(USER_NAME)
                    && password.equals(PASSWORD)) {
                System.out.println(String.format("'%s' login module "
                        + "authentication done successfully", this.getClass()));
                succeeded = true;
                return true;
            }
            throw new IllegalArgumentException("Incorrect username/password!");
        } catch (IOException | UnsupportedCallbackException e) {
            throw new LoginException("Login failed: " + e.getMessage());
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (succeeded == false) {
            return false;
        }
        userPrincipal = new UserPrincipal(username);
        if (!subject.getPrincipals().contains(userPrincipal)) {
            subject.getPrincipals().add(userPrincipal);
        }
        System.out.println(String.format("'%s' login module authentication "
                + "committed", this.getClass()));
        password = null;
        commitSucceeded = true;
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (succeeded == false) {
            return false;
        }
        System.out.println(String.format(
                "'%s' login module aborted", this.getClass()));
        clearState();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        clearState();
        System.out.println(String.format(
                "'%s' login module logout completed", this.getClass()));
        return true;
    }

    private void clearState() {
        if (commitSucceeded) {
            subject.getPrincipals().remove(userPrincipal);
        }
        username = null;
        password = null;
        userPrincipal = null;
    }
}
