/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @author Vincent Ryan
 * @bug 4814522
 * @summary Check that a LdapLoginModule can be initialized using various
 *          options.
 *          (LdapLoginModule replaces the JndiLoginModule for LDAP access)
 */

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import javax.security.auth.*;
import javax.security.auth.login.*;
import javax.security.auth.callback.*;
import com.sun.security.auth.module.LdapLoginModule;

public class CheckOptions {

    private static final String USER_PROVIDER_OPTION = "UsErPrOvIdeR";

    public static void main(String[] args) throws Exception {
        init();
        testInvalidOptions();
        testNullCallbackHandler();
        testWithCallbackHandler();
    }

    private static void init() throws Exception {
    }

    private static void testInvalidOptions() throws Exception {

        // empty set of options

        LdapLoginModule ldap = new LdapLoginModule();
        Subject subject = new Subject();
        ldap.initialize(subject, null, null, Collections.EMPTY_MAP);

        try {
            ldap.login();
            throw new SecurityException("expected a LoginException");

        } catch (LoginException le) {
            // expected behaviour
            System.out.println("Caught a LoginException, as expected");
        }

        // bad value for userProvider option

        Map<String, String> options = new HashMap<>();
        options.put(USER_PROVIDER_OPTION, "ldap://localhost:23456");
        ldap.initialize(subject, null, null, options);

        try {
            ldap.login();
            throw new SecurityException("expected a LoginException");

        } catch (LoginException le) {
            // expected behaviour
            System.out.println("Caught a LoginException, as expected");
        }
    }

    private static void testNullCallbackHandler() throws Exception {

        // empty set of options

        LdapLoginModule ldap = new LdapLoginModule();
        Subject subject = new Subject();
        Map<String, String> options = new HashMap<>();
        ldap.initialize(subject, null, null, options);

        try {
            ldap.login();
            throw new SecurityException("expected LoginException");

        } catch (LoginException le) {
            // expected behaviour
            System.out.println("Caught a LoginException, as expected");
        }
    }

    private static void testWithCallbackHandler() throws Exception {

        LdapLoginModule ldap = new LdapLoginModule();
        Subject subject = new Subject();
        Map<String, String> options = new HashMap<>();

        CallbackHandler goodHandler = new MyCallbackHandler(true);
        ldap.initialize(subject, goodHandler, null, options);

        try {
            ldap.login();
            throw new SecurityException("expected LoginException");

        } catch (LoginException le) {
            // expected behaviour
            System.out.println("Caught a LoginException, as expected");
        }

        CallbackHandler badHandler = new MyCallbackHandler(false);
        ldap.initialize(subject, badHandler, null, options);

        try {
            ldap.login();
            throw new SecurityException("expected LoginException");

        } catch (LoginException le) {
            // expected behaviour
            System.out.println("Caught a LoginException, as expected");
        }
    }

    private static class MyCallbackHandler implements CallbackHandler {

        private final boolean good;

        public MyCallbackHandler(boolean good) {
            this.good = good;
        }

        public void handle(Callback[] callbacks)
                throws IOException, UnsupportedCallbackException {

            for (int i = 0; i < callbacks.length; i++) {

                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];

                    if (good) {
                        nc.setName("foo");
                    } else {
                        // do nothing
                    }

                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];

                    if (good) {
                        pc.setPassword("foo".toCharArray());
                    } else {
                        // do nothing
                    }
                }
            }
        }
    }
}
