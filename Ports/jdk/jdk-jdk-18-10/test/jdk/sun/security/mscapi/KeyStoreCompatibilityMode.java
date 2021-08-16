/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6324294 6931562 8180570
 * @requires os.family == "windows"
 * @run main KeyStoreCompatibilityMode
 * @run main/othervm -Dsun.security.mscapi.keyStoreCompatibilityMode=true KeyStoreCompatibilityMode
 * @run main/othervm -Dsun.security.mscapi.keyStoreCompatibilityMode=false KeyStoreCompatibilityMode -disable
 * @summary Confirm that a null stream or password is not permitted when
 *          compatibility mode is enabled (and vice versa).
*/

import java.io.*;
import java.security.Provider;
import java.security.*;

public class KeyStoreCompatibilityMode {

    private static final String KEYSTORE_COMPATIBILITY_MODE_PROP =
        "sun.security.mscapi.keyStoreCompatibilityMode";

    private static boolean mode;

    public static void main(String[] args) throws Exception {

        if (args.length > 0 && "-disable".equals(args[0])) {
            mode = false;
        } else {
            mode = true;
        }

        Provider p = Security.getProvider("SunMSCAPI");

        System.out.println("SunMSCAPI provider classname is " +
            p.getClass().getName());
        System.out.println(KEYSTORE_COMPATIBILITY_MODE_PROP + " = " +
            System.getProperty(KEYSTORE_COMPATIBILITY_MODE_PROP));

        KeyStore myKeyStore = KeyStore.getInstance("Windows-MY", p);
        KeyStore myKeyStore2 = KeyStore.getInstance("Windows-MY", p);
        KeyStore rootKeyStore = KeyStore.getInstance("Windows-ROOT", p);
        KeyStore rootKeyStore2 = KeyStore.getInstance("Windows-ROOT", p);

        InputStream inStream = new ByteArrayInputStream(new byte[1]);
        OutputStream outStream = new ByteArrayOutputStream();
        char[] password = new char[1];

        // Checking keystore load operations
        testLoadStore(myKeyStore, null, null, true);
        testLoadStore(myKeyStore2, null, password, true);
        testLoadStore(rootKeyStore, inStream, null, true);
        testLoadStore(rootKeyStore2, inStream, password, true);

        // Checking keystore store operations
        testLoadStore(myKeyStore, null, null, false);
        testLoadStore(myKeyStore2, null, password, false);
        testLoadStore(rootKeyStore, outStream, null, false);
        testLoadStore(rootKeyStore2, outStream, password, false);
    }

    private static void testLoadStore(KeyStore keyStore, Object stream,
        char[] password, boolean doLoad) throws Exception {

        String streamValue = stream == null ? "null" : "non-null";
        String passwordValue = password == null ? "null" : "non-null";

        System.out.println("Checking " + (doLoad ? "load" : "store") +
            "(stream=" + streamValue + ", password=" + passwordValue + ")...");

        try {

            if (doLoad) {
                keyStore.load((InputStream) stream, password);
            } else {
                keyStore.store((OutputStream) stream, password);
            }

            if (!mode && keyStore != null && password != null) {
                throw new Exception(
                    "Expected an IOException to be thrown by KeyStore.load");
            }

        } catch (IOException ioe) {
            // When mode=false the exception is expected.
            if (mode) {
                throw ioe;
            } else {
                System.out.println("caught the expected exception: " + ioe);
            }

        } catch (KeyStoreException kse) {
            // store will fail if load has previously failed
            if (doLoad) {
                throw kse;
            } else {
                System.out.println("caught the expected exception: " + kse);
            }
        }
    }
}
