/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.SecurityTools.keytool;

import java.io.File;
import java.security.KeyStore;

/*
 * @test
 * @bug 8215776
 * @library /test/lib
 * @summary Keytool importkeystore may mix up certificate chain entries when DNs conflict
 */
public class SameDN {

    private static final String COMMON = "-keystore ks -storepass changeit ";

    public static final void main(String[] args) throws Exception {
        genkeypair("ca1", "CN=CA");
        genkeypair("ca2", "CN=CA");
        genkeypair("user1", "CN=user");
        genkeypair("user2", "CN=user");
        gencert("ca1", "user1");
        gencert("ca2", "user2");

        KeyStore ks = KeyStore.getInstance(
                new File("ks"), "changeit".toCharArray());
        if (!ks.getCertificate("ca1").equals(ks.getCertificateChain("user1")[1])) {
            throw new Exception("user1 not signed by ca1");
        }
        if (!ks.getCertificate("ca2").equals(ks.getCertificateChain("user2")[1])) {
            throw new Exception("user2 not signed by ca2");
        }
    }

    static void genkeypair(String alias, String dn) throws Exception {
        keytool(COMMON + "-genkeypair -keyalg DSA -alias " + alias + " -dname " + dn)
                .shouldHaveExitValue(0);
    }

    static void gencert(String issuer, String subject) throws Exception {
        keytool(COMMON + "-certreq -alias " + subject + " -file req")
                .shouldHaveExitValue(0);
        keytool(COMMON + "-gencert -alias " + issuer + " -infile req -outfile cert")
                .shouldHaveExitValue(0);
        keytool(COMMON + "-importcert -alias " + subject + " -file cert")
                .shouldHaveExitValue(0);
    }
}
