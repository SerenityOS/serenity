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
package client;

import java.io.IOException;
import java.security.Principal;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.LoginContext;
import com.sun.security.auth.UserPrincipal;

/**
 * JAAS client which will try to authenticate a user through a custom JAAS LOGIN
 * Module.
 */
public class JaasClient {

    private static final String USER_NAME = "testUser";
    private static final String PASSWORD = "testPassword";
    private static final String LOGIN_CONTEXT = "ModularLoginConf";

    public static void main(String[] args) {
        try {
            LoginContext lc = new LoginContext(LOGIN_CONTEXT,
                    new MyCallbackHandler());
            lc.login();
            checkPrincipal(lc, true);
            lc.logout();
            checkPrincipal(lc, false);
        } catch (LoginException le) {
            throw new RuntimeException(le);
        }
        System.out.println("Test passed.");

    }

    /*
     * Check context for principal of the test user.
     */
    private static void checkPrincipal(LoginContext loginContext,
            boolean principalShouldExist) {
        if (!principalShouldExist) {
            if (loginContext.getSubject().getPrincipals().size() != 0) {
                throw new RuntimeException("Test failed. Principal was not "
                        + "cleared.");
            }
            return;
        }
        for (Principal p : loginContext.getSubject().getPrincipals()) {
            if (p instanceof UserPrincipal
                    && USER_NAME.equals(p.getName())) {
                //Proper principal was found, return.
                return;
            }
        }
        throw new RuntimeException("Test failed. UserPrincipal "
                + USER_NAME + " expected.");

    }

    private static class MyCallbackHandler implements CallbackHandler {

        @Override
        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(USER_NAME);
                } else if (callback instanceof PasswordCallback) {
                    ((PasswordCallback) callback).setPassword(
                            PASSWORD.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        }
    }

}
