/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4377181
 * @summary Provide default configurable CallbackHandlers
 *
 * @build DefaultHandler DefaultHandlerImpl DefaultHandlerModule
 * @run main/othervm -Djava.security.auth.login.config=file:${test.src}/DefaultHandler.config DefaultHandler
 */

import javax.security.auth.*;
import javax.security.auth.login.*;

public class DefaultHandler {

    public static void main(String[] args) {

        // first test if a default is not provided.
        // we should get an exception

        LoginContext lc = null;
        try {
            lc = new LoginContext("SampleLogin");
        } catch (LoginException le) {
            System.out.println
                ("DefaultHandler test failed - login construction failed");
            throw new SecurityException(le.getMessage());
        }

        try {
            lc.login();
            throw new SecurityException
                ("DefaultHandler test failed: got a handler!");
        } catch (LoginException le) {
            // good!
            System.out.println
                ("Good: CallbackHandler implementation not found");
            le.printStackTrace();
        }

        // set the security property for the default handler
        java.security.Security.setProperty("auth.login.defaultCallbackHandler",
                "DefaultHandlerImpl");

        // now test to see if the default handler is picked up.
        // this should succeed.

        LoginContext lc2 = null;
        try {
            lc2 = new LoginContext("SampleLogin");
        } catch (LoginException le) {
            System.out.println
                ("DefaultHandler test failed - constructing LoginContext");
            throw new SecurityException(le.getMessage());
        }

        try {
            lc2.login();
        } catch (LoginException le) {
            System.out.println
                ("DefaultHandler test failed - login method");
            throw new SecurityException(le.getMessage());
        }

        System.out.println("DefaultHandler test succeeded");
    }
}
