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
 * @bug 4391445
 * @summary JAAS KeyStoreLoginModule logout doesn't
 *              handle read-only subjects correctly
 *
 * XXX Note this test relies on the KeyStore used by OptionTest
 */

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.security.auth.*;
import javax.security.auth.login.*;
import javax.security.auth.callback.*;
import com.sun.security.auth.module.KeyStoreLoginModule;

public class ReadOnly {

    private static final String TEST = "OptionTest";
    private static int testnum = 1;

    private static final String O_URL = "keyStoreURL";
    private static final String O_ALIAS = "keyStoreAlias";
    private static final String O_SPASS_URL = "keyStorePasswordURL";
    private static final String O_KPASS_URL = "privateKeyPasswordURL";

    private static String URL;
    private static String SPASS_URL;
    private static String KPASS_URL;

    private static final String ALIAS = "alias";
    private static final char[] STORE_PASS = new char[]
        { 's', 't', 'o', 'r', 'e', 'P', 'a', 's', 's' };  // sync with file
    private static final char[] KEY_PASS =
        { 'k', 'e', 'y', 'P', 'a', 's', 's' };            // sync with file

    public static void main(String[] args) throws Exception {
        init();
        testReadOnly();
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

    private static void testReadOnly() throws Exception {

        // setup

        KeyStoreLoginModule m = new KeyStoreLoginModule();
        Subject s = new Subject();
        Map options = new HashMap();
        options.put(O_URL, URL);
        options.put(O_ALIAS, ALIAS);
        options.put(O_SPASS_URL, SPASS_URL);
        options.put(O_KPASS_URL, KPASS_URL);
        m.initialize(s, null, null, options);

        // login first
        m.login();
        m.commit();
        System.out.println("test " + testnum++ + " passed");

        // test regular logout
        m.logout();
        if (s.getPrincipals().size() != 0) {
            throw new SecurityException("expected no principals");
        }
        if (s.getPublicCredentials().size() != 0) {
            throw new SecurityException("expected no public creds");
        }
        if (s.getPrivateCredentials().size() != 0) {
            throw new SecurityException("expected no private creds");
        }
        System.out.println("test " + testnum++ + " passed");

        // login again
        m.login();
        m.commit();
        System.out.println("test " + testnum++ + " passed");

        // set subject to read-only
        s.setReadOnly();

        // try to logout
        try {
            m.logout();
            throw new SecurityException("expected login exception");
        } catch (LoginException le) {
            // good
            System.out.println("test " + testnum++ + " passed");
        }
    }
}
