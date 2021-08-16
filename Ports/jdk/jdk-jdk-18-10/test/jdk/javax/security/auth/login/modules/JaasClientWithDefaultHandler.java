/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Principal;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import com.sun.security.auth.UserPrincipal;

public class JaasClientWithDefaultHandler {

    private static final String USER_NAME = "testUser";
    private static final String LOGIN_CONTEXT = "ModularLoginConf";
    private static final String CBH_PROP = "auth.login.defaultCallbackHandler";

    public static void main(String[] args) {
        try {
            java.security.Security.setProperty(CBH_PROP, args[0]);
            LoginContext lc = new LoginContext(LOGIN_CONTEXT);
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
     * Verify principal for the test user.
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

}
