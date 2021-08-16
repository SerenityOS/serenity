/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

/**
 * @test
 * @bug 8024302 8026037 8196213
 * @summary Checks warnings if -tsa and -tsacert options are not specified
 * @library /test/lib ../
 * @build jdk.test.lib.util.JarUtils
 * @run main NoTimestampTest
 */
public class NoTimestampTest extends Test {

    /**
     * The test signs and verifies a jar file without -tsa and -tsacert options,
     * and checks that proper warnings are shown.
     */
    public static void main(String[] args) throws Throwable {

        Locale reservedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);

        try {
            NoTimestampTest test = new NoTimestampTest();
            test.start();
        } finally {
            // Restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }

    private void start() throws Throwable {
        String timezone = System.getProperty("user.timezone");
        System.out.println(String.format("Timezone = %s", timezone));
        if (timezone != null) {
            TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        }

        // create a jar file that contains one class file
        Utils.createFiles(FIRST_FILE);
        JarUtils.createJar(UNSIGNED_JARFILE, FIRST_FILE);

        // create key pair
        createAlias(CA_KEY_ALIAS, "-ext", "bc:c");
        createAlias(KEY_ALIAS);
        issueCert(KEY_ALIAS,
                "-validity", Integer.toString(VALIDITY));

        Date expirationDate = getCertExpirationDate();
        System.out.println("Cert expiration: " + expirationDate);

        // sign jar file
        OutputAnalyzer analyzer = jarsigner(
                userTimezoneOpt(timezone),
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-signedjar", SIGNED_JARFILE,
                UNSIGNED_JARFILE,
                KEY_ALIAS);

        String warning = String.format(NO_TIMESTAMP_SIGNING_WARN_TEMPLATE,
                expirationDate);
        checkSigning(analyzer, warning);

        // verify signed jar
        analyzer = jarsigner(
                userTimezoneOpt(timezone),
                "-verify",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE,
                KEY_ALIAS);

        warning = String.format(NO_TIMESTAMP_VERIFYING_WARN_TEMPLATE, expirationDate);
        checkVerifying(analyzer, 0, warning);

        // verify signed jar in strict mode
        analyzer = jarsigner(
                userTimezoneOpt(timezone),
                "-verify",
                "-strict",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE,
                KEY_ALIAS);

        checkVerifying(analyzer, 0, warning);

        System.out.println("Test passed");
    }

    private static String userTimezoneOpt(String timezone) {
        return timezone == null ? null : "-J-Duser.timezone=" + timezone;
    }

    private static Date getCertExpirationDate() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream in = new FileInputStream(KEYSTORE)) {
            ks.load(in, PASSWORD.toCharArray());
        }
        X509Certificate cert = (X509Certificate) ks.getCertificate(KEY_ALIAS);
        return cert.getNotAfter();
    }
}
