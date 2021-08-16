/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8266400
 * @summary Test importkeystore to a password less PKCS12 keystore
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

public class ImportToPwordlessPK12 {

    static OutputAnalyzer kt(String cmd, String ks) throws Exception {
        return SecurityTools.keytool("-storepass changeit " + cmd +
                " -keystore " + ks);
    }

    public static void main(String[] args) throws Exception {

        kt("-genkeypair -keyalg EC -alias testcert -dname CN=EE " +
                "-storetype jks -keypass pass123 ", "ks.jks");

        /*
         * Test by setting the responses for source keystore password and
         * key password for alias
         */
        SecurityTools.setResponse("changeit", "pass123");
        SecurityTools.keytool("-importkeystore -srckeystore ks.jks " +
                "-destkeystore ks.p12 " +
                "-J-Dkeystore.pkcs12.macAlgorithm=NONE " +
                "-J-Dkeystore.pkcs12.certProtectionAlgorithm=NONE")
                .shouldHaveExitValue(0);

        SecurityTools.keytool("-list -keystore ks.p12 -debug")
                .shouldContain("Keystore type: PKCS12")
                .shouldContain("keystore contains 1 entry")
                .shouldNotContain("Enter keystore password:")
                .shouldHaveExitValue(0);

        kt("-genkeypair -keyalg EC -alias testcert -dname CN=EE " +
                "-storetype jks -keypass pass123 ", "ks1.jks");

        // Test with all of options specified on command line
        SecurityTools.keytool("-importkeystore -srckeystore ks1.jks " +
                "-destkeystore ks1.p12 " +
                "-srcstorepass changeit " +
                "-srcalias testcert " +
                "-srckeypass pass123 " +
                "-J-Dkeystore.pkcs12.macAlgorithm=NONE " +
                "-J-Dkeystore.pkcs12.certProtectionAlgorithm=NONE")
                .shouldHaveExitValue(0);

        SecurityTools.keytool("-list -keystore ks1.p12 -debug")
                .shouldContain("Keystore type: PKCS12")
                .shouldContain("keystore contains 1 entry")
                .shouldNotContain("Enter keystore password:")
                .shouldHaveExitValue(0);
    }
}
