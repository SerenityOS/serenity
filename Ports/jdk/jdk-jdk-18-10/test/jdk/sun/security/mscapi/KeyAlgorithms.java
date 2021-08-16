/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8213009 8237804
 * @summary Make sure SunMSCAPI keys have correct algorithm names
 * @requires os.family == "windows"
 * @library /test/lib
 * @modules jdk.crypto.mscapi
 */

import java.security.*;

import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;

public class KeyAlgorithms {

    private static final String ALIAS = "8213009";
    private static final String ALG = "RSA";

    public static void main(String[] arg) throws Exception {

        cleanup();
        SecurityTools.keytool("-genkeypair",
                "-storetype", "Windows-My",
                "-keyalg", ALG,
                "-alias", ALIAS,
                "-dname", "cn=" + ALIAS,
                "-noprompt").shouldHaveExitValue(0);

        try {
            test(loadKeysFromKeyStore());
        } finally {
            cleanup();
        }

        test(generateKeys());
    }

    private static void cleanup() {
        try {
            KeyStore ks = KeyStore.getInstance("Windows-MY");
            ks.load(null, null);
            ks.deleteEntry(ALIAS);
            ks.store(null, null);
        } catch (Exception e) {
            System.out.println("No such entry.");
        }
    }

    static KeyPair loadKeysFromKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);
        return new KeyPair(ks.getCertificate(ALIAS).getPublicKey(),
                (PrivateKey) ks.getKey(ALIAS, null));
    }

    static KeyPair generateKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALG, "SunMSCAPI");
        return kpg.generateKeyPair();
    }

    static void test(KeyPair kp) {
        Asserts.assertEQ(kp.getPrivate().getAlgorithm(), ALG);
        Asserts.assertEQ(kp.getPublic().getAlgorithm(), ALG);
    }
}
