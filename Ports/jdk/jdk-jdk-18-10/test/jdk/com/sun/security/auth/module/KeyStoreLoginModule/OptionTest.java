/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4919147
 * @summary Support for token-based KeyStores
 * @run main/othervm -Duser.language=en OptionTest
 */

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.security.auth.*;
import javax.security.auth.login.*;
import javax.security.auth.callback.*;
import com.sun.security.auth.module.KeyStoreLoginModule;

public class OptionTest {

    private static final String TEST = "OptionTest";
    private static int testnum = 1;

    private static final String O_URL = "keyStoreURL";
    private static final String O_ALIAS = "keyStoreAlias";
    private static final String O_TYPE = "keyStoreType";
    private static final String O_SPASS_URL = "keyStorePasswordURL";
    private static final String O_KPASS_URL = "privateKeyPasswordURL";
    private static final String O_PPATH = "protected";
    private static final String NONE = "NONE";

    private static String URL;
    private static String SPASS_URL;
    private static String KPASS_URL;

    private static final String P11KEYSTORE = "PKCS11";
    private static final String ALIAS = "alias";
    private static final char[] STORE_PASS = new char[]
        { 's', 't', 'o', 'r', 'e', 'P', 'a', 's', 's' };  // sync with file
    private static final char[] KEY_PASS =
        { 'k', 'e', 'y', 'P', 'a', 's', 's' };            // sync with file

    public static void main(String[] args) throws Exception {
        init();
        testInvalidOptions();
        testNullCallbackHandler();
        testWithCallbackHandler();
    }

    private static void init() throws Exception {
        File f = new File(System.getProperty("test.src", ".") +
                        File.separatorChar +
                        TEST +
                        ".keystore");
        URL = f.toURI().toURL().toString();

        f = new File(System.getProperty("test.src", ".") +
                        File.separatorChar +
                        TEST +
                        ".storePass");
        SPASS_URL = f.toURI().toURL().toString();

        f = new File(System.getProperty("test.src", ".") +
                        File.separatorChar +
                        TEST +
                        ".keyPass");
        KPASS_URL = f.toURI().toURL().toString();
    }

    private static void testInvalidOptions() throws Exception {

        // if keyStoreType is PKCS11, keyStoreURL must be NONE

        KeyStoreLoginModule m = new KeyStoreLoginModule();
        Subject s = new Subject();
        Map options = new HashMap();
        options.put(O_TYPE, P11KEYSTORE);
        m.initialize(s, null, null, options);
        try {
            m.login();
            throw new SecurityException("expected exception");
        } catch (LoginException le) {
            // good
            //le.printStackTrace();
            System.out.println("test " + testnum++ + " passed");
        }

        // if keyStoreType is PKCS11, keyStoreURL is NONE,
        // then privateKeyPasswordURL must not be specified

        options = new HashMap();
        options.put(O_TYPE, P11KEYSTORE);
        options.put(O_URL, NONE);
        options.put(O_KPASS_URL, KPASS_URL);
        m.initialize(s, null, null, options);
        try {
            m.login();
            throw new SecurityException("expected exception");
        } catch (LoginException le) {
            // good
            //le.printStackTrace();
            System.out.println("test " + testnum++ + " passed");
        }

        // if protected is true, keyStorePasswordURL must not be specified

        options = new HashMap();
        options.put(O_PPATH, "true");
        options.put(O_SPASS_URL, SPASS_URL);
        m.initialize(s, null, null, options);
        try {
            m.login();
            throw new SecurityException("expected exception");
        } catch (LoginException le) {
            // good
            //le.printStackTrace();
            System.out.println("test " + testnum++ + " passed");
        }

        // if protected is true, privateKeyPasswordURL must not be specified

        options = new HashMap();
        options.put(O_PPATH, "true");
        options.put(O_KPASS_URL, KPASS_URL);
        m.initialize(s, null, null, options);
        try {
            m.login();
            throw new SecurityException("expected exception");
        } catch (LoginException le) {
            // good
            //le.printStackTrace();
            System.out.println("test " + testnum++ + " passed");
        }
    }

    private static void testNullCallbackHandler() throws Exception {

        // no options (missing alias)

        KeyStoreLoginModule m = new KeyStoreLoginModule();
        Subject s = new Subject();
        Map options = new HashMap();
        m.initialize(s, null, null, options);
        try {
            m.login();
            throw new SecurityException("expected exception");
        } catch (LoginException le) {
            // good
            //le.printStackTrace();
            System.out.println("test " + testnum++ + " passed");
        }

        // missing passwords

        options.put(O_ALIAS, ALIAS);
        m.initialize(s, null, null, options);
        try {
            m.login();
            throw new SecurityException("expected exception");
        } catch (LoginException le) {
            // good
            //le.printStackTrace();
            System.out.println("test " + testnum++ + " passed");
        }

        // no private key password
        // (private key password is different from store password)

        options.put(O_SPASS_URL, SPASS_URL);
        m.initialize(s, null, null, options);
        try {
            m.login();
            throw new SecurityException("expected exception");
        } catch (LoginException le) {
            // good
            //le.printStackTrace();
            System.out.println("test " + testnum++ + " passed");
        }

        // all necessary options
        // (private key password is different from store password)

        options.put(O_URL, URL);
        options.put(O_KPASS_URL, KPASS_URL);
        m.initialize(s, null, null, options);
        m.login();
        System.out.println("test " + testnum++ + " passed");
    }

    private static void testWithCallbackHandler() throws Exception {

        KeyStoreLoginModule m = new KeyStoreLoginModule();
        Subject s = new Subject();
        Map options = new HashMap();
        options.put(O_URL, URL);

        CallbackHandler goodHandler = new MyCallbackHandler(true);
        m.initialize(s, goodHandler, null, options);
        m.login();
        System.out.println("test " + testnum++ + " passed");

        CallbackHandler badHandler = new MyCallbackHandler(false);
        m.initialize(s, badHandler, null, options);
        try {
            m.login();
            throw new SecurityException("expected LoginException");
        } catch (LoginException le) {
            // good
            System.out.println("test " + testnum++ + " passed");
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
                        nc.setName(OptionTest.ALIAS);
                    } else {
                        nc.setName("FOO");
                    }
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];

                    // this is dependent on KeyStoreLoginModule
                    if (pc.getPrompt().startsWith("Keystore password: ")) {
                        pc.setPassword(OptionTest.STORE_PASS);
                    } else if (pc.getPrompt().startsWith("Private key")) {
                        pc.setPassword(OptionTest.KEY_PASS);
                    }
                } else if (callbacks[i] instanceof ConfirmationCallback) {
                    ConfirmationCallback confirmation =
                        (ConfirmationCallback) callbacks[i];
                    // this is dependent on KeyStoreLoginModule confirmation
                    confirmation.setSelectedIndex(ConfirmationCallback.OK);
                }
            }
        }
    }
}
