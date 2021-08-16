/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8192988 8266220
 * @summary keytool should support -storepasswd for pkcs12 keystores
 * @library /test/lib
 * @build jdk.test.lib.SecurityTools
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main PKCS12Passwd
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.File;
import java.security.KeyStore;
import java.util.Collections;

public class PKCS12Passwd {

    public static void main(String[] args) throws Exception {

        // A PrivateKeyEntry
        kt("-genkeypair -alias a -dname CN=A -keyalg DSA")
                .shouldHaveExitValue(0);

        // A TrustedCertificateEntry (genkeypair, export, delete, import)
        kt("-genkeypair -alias b -dname CN=B -keyalg DSA")
                .shouldHaveExitValue(0);
        kt("-exportcert -alias b -file b.cert")
                .shouldHaveExitValue(0);
        kt("-delete -alias b")
                .shouldHaveExitValue(0);
        kt("-list -alias b")
                .shouldHaveExitValue(1);
        kt("-importcert -alias b -file b.cert -noprompt")
                .shouldHaveExitValue(0);

        // A SecretKeyEntry
        kt("-genseckey -keyalg AES -keysize 256 -alias c")
                .shouldHaveExitValue(0);

        // Change password

        // 1. Using -importkeystore
        ktFull("-importkeystore -srckeystore ks -destkeystore ks2 "
                + "-srcstoretype pkcs12 -deststoretype pkcs12 "
                + "-srcstorepass changeit -deststorepass newpass")
                .shouldHaveExitValue(0);

        check("ks2", "newpass", "newpass");

        // 2. Using -storepasswd
        kt("-storepasswd -new newpass")
                .shouldHaveExitValue(0)
                .shouldNotContain("Ignoring user-specified");

        check("ks", "newpass", "newpass");

        // Other facts. Not necessarily the correct thing.

        // A PKCS12 keystore can be loaded as a JKS, and it follows JKS rules
        // which means the storepass and keypass can be changed separately!

        ktFull("-genkeypair -alias a -dname CN=A -storetype pkcs12 -keyalg DSA "
                    + "-storepass changeit -keypass changeit -keystore p12")
                .shouldHaveExitValue(0);

        // Only storepass is changed
        ktFull("-storepasswd -storepass changeit -new newpass "
                    + "-keystore p12 -storetype jks")
                .shouldHaveExitValue(0);

        check("p12", "newpass", "changeit");

        // Only keypass is changed
        ktFull("-keypasswd -storepass newpass -keypass changeit -new newpass "
                + "-keystore p12 -storetype jks -alias a")
                .shouldHaveExitValue(0);

        check("p12", "newpass", "newpass");

        // Conversely, a JKS keystore can be laoded as a PKCS12, and it follows
        // PKCS12 rules that both passwords are changed at the same time and
        // some commands are rejected.

        ktFull("-genkeypair -alias a -dname CN=A -storetype jks -keyalg DSA "
                    + "-storepass changeit -keypass changeit -keystore jks")
                .shouldHaveExitValue(0);

        // Both storepass and keypass changed.
        ktFull("-storepasswd -storepass changeit -new newpass "
                        + "-keystore jks -storetype pkcs12")
                .shouldHaveExitValue(0);

        check("jks", "newpass", "newpass");

        // -keypasswd is not available for pkcs12
        ktFull("-keypasswd -storepass newpass -keypass newpass -new newerpass "
                + "-keystore jks -storetype pkcs12 -alias a")
                .shouldHaveExitValue(1);

        // but available for JKS
        ktFull("-keypasswd -storepass newpass -keypass newpass -new newerpass "
                + "-keystore jks -alias a")
                .shouldHaveExitValue(0);

        check("jks", "newpass", "newerpass");

        // A password-less keystore
        ktFull("-keystore nopass -genkeypair -keyalg EC "
                + "-storepass changeit -alias no -dname CN=no "
                + "-J-Dkeystore.pkcs12.certProtectionAlgorithm=NONE "
                + "-J-Dkeystore.pkcs12.macAlgorithm=NONE")
                .shouldHaveExitValue(0);

        ktFull("-keystore nopass -list")
                .shouldHaveExitValue(0)
                .shouldNotContain("Enter keystore password:");

        ktFull("-keystore nopass -list -storetype pkcs12")
                .shouldHaveExitValue(0)
                .shouldNotContain("Enter keystore password:");
    }

    // Makes sure we can load entries in a keystore
    static void check(String file, String storePass, String keyPass)
            throws Exception {

        KeyStore ks = KeyStore.getInstance(
                new File(file), storePass.toCharArray());

        for (String a : Collections.list(ks.aliases())) {
            if (ks.isCertificateEntry(a)) {
                ks.getCertificate(a);
            } else {
                ks.getEntry(a,
                        new KeyStore.PasswordProtection(keyPass.toCharArray()));
            }
        }
    }

    static OutputAnalyzer kt(String arg) throws Exception {
        return ktFull("-keystore ks -storepass changeit " + arg);
    }

    static OutputAnalyzer ktFull(String arg) throws Exception {
        return SecurityTools.keytool("-debug " + arg);
    }
}
