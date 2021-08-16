/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * @test
 * @bug 8050427 4703361
 * @summary Test case for RFE: 4703361. Tests the Dynamic Configuration of
 * Authentication Modules with different methods
 * @compile SmartLoginModule.java DummyLoginModule.java MyConfiguration.java
 * @run main/othervm DynamicConfigurationTest
 */
public class DynamicConfigurationTest {

    public static void main(String... args) {
        String rightConfigName = "PT";
        String wrongConfigName = "NT";
        char[] rightPwd = new char[]{'t', 'e', 's', 't', 'P', 'a', 's', 's',
            'w', 'o', 'r', 'd', '1'};
        char[] wrongPwd = new char[]{'w', 'r', 'o', 'n', 'g', 'P', 'a', 's',
            's','w', 'o', 'r', 'd'};

        // Test with wrong configuration name
        // Expect LoginException when initiate a new LoginContext object
        testConfigName(wrongConfigName, true);
        System.out.println("Wrong Config Name Test passed ");

        // Spedify two loginModules: SmartLoginModule and DummyLoginModule
        // Flags: required-required
        // Test with right password for SmartLoginModule
        // No exception is expected
        Configuration cf = new MyConfiguration();
        testLogin(rightConfigName, rightPwd, cf, false);
        System.out.println("Positive test passed");

        // Spedify two loginModules: SmartLoginModule and DummyLoginModule
        // Flags: required-required
        // Test with wrong password for SmartLoginModule
        // Expect LoginException by calling LoginContext.login() method
        testLogin(rightConfigName, wrongPwd, cf, true);
        System.out.println("Should fail test passed");

        // Spedify two loginModules: SmartLoginModule and DummyLoginModule
        // Change the flags from required-required to optional-sufficient
        // Test with wrong password for SmartLoginModule, while DummyLoginModule
        // always passes
        // No Exception is expected
        cf = new MyConfiguration(true);
        testLogin(rightConfigName, wrongPwd, cf, false);
        System.out.println("One module fails where are other module succeeeds "
                + "Test passed with optional-sufficient flags");
    }

    public static void testConfigName(String confName,
            boolean expectException) {
        String expectedMsg = "No LoginModules configured for " + confName;
        try {
            LoginContext lc = new LoginContext(confName, new Subject(),
                    new MyCallbackHandler(), new MyConfiguration());

            if (expectException) {
                throw new RuntimeException("Wrong Config Name Test failed: "
                        + "expected LoginException not thrown.");
            }
        } catch (LoginException le) {
            if (!expectException || !le.getMessage().equals(expectedMsg)) {
                System.out.println("Wrong Config Name Test failed: "
                        + "received Unexpected exception.");
                throw new RuntimeException(le);
            }
        }
    }

    public static void testLogin(String confName, char[] passwd,
            Configuration cf, boolean expectException) {
        try {
            CallbackHandler ch = new MyCallbackHandler("testUser", passwd);
            LoginContext lc = new LoginContext(confName, new Subject(),
                    ch, cf);
            lc.login();
            if (expectException) {
                throw new RuntimeException("Login Test failed: "
                        + "expected LoginException not thrown");
            }
        } catch (LoginException le) {
            if (!expectException) {
                System.out.println("Login Test failed: "
                        + "received Unexpected exception.");
                throw new RuntimeException(le);
            }
        }
    }
}

/**
 * The application simulates the CallbackHandler. It simulates! which means all
 * process to get username and password is ignored. We have to take this
 * approach for automation purpose. So, this is not a real world example at all.
 */
class MyCallbackHandler implements CallbackHandler {

    String userName;
    char[] password;

    /**
     * This is simply a workaround approach for IO approach to get username and
     * password. For automation purpose only.
     */
    public MyCallbackHandler() {
        super();
    }

    public MyCallbackHandler(String username, char[] password) {
        super();
        userName = username;
        this.password = password;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                NameCallback nc = (NameCallback) callback;
                nc.setName(userName);
            } else if (callback instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) callback;
                pc.setPassword(password);
            } else {
                throw new UnsupportedCallbackException(callback,
                        "Unrecognized Callback");
            }
        }
    }
}
