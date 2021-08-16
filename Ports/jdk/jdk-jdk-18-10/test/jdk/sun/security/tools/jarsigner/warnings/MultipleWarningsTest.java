/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

/**
 * @test
 * @bug 8024302 8026037
 * @summary Checks if jarsigner prints appropriate warnings
 * @library /test/lib ../
 * @build jdk.test.lib.util.JarUtils
 * @run main MultipleWarningsTest
 */
public class MultipleWarningsTest extends Test {

    /**
     * The test signs and verifies a jar that:
     *   - contains entries whose signer certificate has expired
     *   - contains entries whose signer certificate's ExtendedKeyUsage
     *     extension doesn't allow code signing
     *   - contains unsigned entries which have not been integrity-checked
     *   - contains signed entries which are not signed by the specified alias
     * Warning messages are expected.
     */
    public static void main(String[] args) throws Throwable {
        MultipleWarningsTest test = new MultipleWarningsTest();
        test.start();
    }

    private void start() throws Throwable {
        Utils.createFiles(FIRST_FILE, SECOND_FILE);

        // create a jar file that contains one class file
        JarUtils.createJar(UNSIGNED_JARFILE, FIRST_FILE);

        createAlias(CA_KEY_ALIAS);

        // create first expired certificate
        // whose ExtendedKeyUsage extension does not allow code signing
        createAlias(FIRST_KEY_ALIAS);
        issueCert(
                FIRST_KEY_ALIAS,
                "-ext", "ExtendedkeyUsage=serverAuth",
                "-startdate", "-" + VALIDITY * 2 + "d",
                "-validity", Integer.toString(VALIDITY));

        // create second expired certificate
        // whose KeyUsage extension does not allow code signing
        createAlias(SECOND_KEY_ALIAS);
        issueCert(
                SECOND_KEY_ALIAS,
                "-ext", "ExtendedkeyUsage=serverAuth",
                "-startdate", "-" + VALIDITY * 2 + "d",
                "-validity", Integer.toString(VALIDITY));

        // sign jar with first key
        OutputAnalyzer analyzer = jarsigner(
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-signedjar", SIGNED_JARFILE,
                UNSIGNED_JARFILE,
                FIRST_KEY_ALIAS);

        checkSigning(analyzer, HAS_EXPIRED_CERT_SIGNING_WARNING,
                BAD_EXTENDED_KEY_USAGE_SIGNING_WARNING);

        // add a second class to created jar, so it contains unsigned entry
        JarUtils.updateJar(SIGNED_JARFILE, UPDATED_SIGNED_JARFILE, SECOND_FILE);

        // verify jar with second key
        analyzer = jarsigner(
                "-verify",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                UPDATED_SIGNED_JARFILE,
                SECOND_KEY_ALIAS);

        checkVerifying(analyzer, 0, BAD_EXTENDED_KEY_USAGE_VERIFYING_WARNING,
                HAS_EXPIRED_CERT_VERIFYING_WARNING,
                HAS_UNSIGNED_ENTRY_VERIFYING_WARNING,
                NOT_SIGNED_BY_ALIAS_VERIFYING_WARNING);

        // verify jar with second key in strict mode
        analyzer = jarsigner(
                "-verify",
                "-strict",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                UPDATED_SIGNED_JARFILE,
                SECOND_KEY_ALIAS);

        int expectedExitCode = HAS_EXPIRED_CERT_EXIT_CODE
                + BAD_EXTENDED_KEY_USAGE_EXIT_CODE
                + HAS_UNSIGNED_ENTRY_EXIT_CODE
                + NOT_SIGNED_BY_ALIAS_EXIT_CODE;
        checkVerifying(analyzer, expectedExitCode,
                BAD_EXTENDED_KEY_USAGE_VERIFYING_WARNING,
                HAS_EXPIRED_CERT_VERIFYING_WARNING,
                HAS_UNSIGNED_ENTRY_VERIFYING_WARNING,
                NOT_SIGNED_BY_ALIAS_VERIFYING_WARNING);

        // verify jar with non-exisiting alias
        analyzer = jarsigner(
                "-verify",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                UPDATED_SIGNED_JARFILE,
                "bogus");

        checkVerifying(analyzer, 0, BAD_EXTENDED_KEY_USAGE_VERIFYING_WARNING,
                HAS_EXPIRED_CERT_VERIFYING_WARNING,
                HAS_UNSIGNED_ENTRY_VERIFYING_WARNING,
                NOT_SIGNED_BY_ALIAS_VERIFYING_WARNING);

        // verify jar with non-exisiting alias in strict mode
        analyzer = jarsigner(
                "-verify",
                "-strict",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                UPDATED_SIGNED_JARFILE,
                "bogus");

        checkVerifying(analyzer, expectedExitCode,
                BAD_EXTENDED_KEY_USAGE_VERIFYING_WARNING,
                HAS_EXPIRED_CERT_VERIFYING_WARNING,
                HAS_UNSIGNED_ENTRY_VERIFYING_WARNING,
                NOT_SIGNED_BY_ALIAS_VERIFYING_WARNING);

        System.out.println("Test passed");
    }

}
