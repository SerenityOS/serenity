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
 * @summary Test for notYetValidCert warning
 * @library /test/lib ../
 * @build jdk.test.lib.util.JarUtils
 * @run main NotYetValidCertTest
 */
public class NotYetValidCertTest extends Test {

    /**
     * The test signs and verifies a jar that contains entries
     * whose signer certificate is not yet valid (notYetValidCert).
     * Warning message is expected.
     */
    public static void main(String[] args) throws Throwable {
        NotYetValidCertTest test = new NotYetValidCertTest();
        test.start();
    }

    protected void start() throws Throwable {
        // create a jar file that contains one class file
        Utils.createFiles(FIRST_FILE);
        JarUtils.createJar(UNSIGNED_JARFILE, FIRST_FILE);

        // create certificate that will be valid only tomorrow
        createAlias(CA_KEY_ALIAS);
        createAlias(KEY_ALIAS);

        issueCert(
                KEY_ALIAS,
                "-startdate", "+1d",
                "-validity", Integer.toString(VALIDITY));

        // sign jar
        OutputAnalyzer analyzer = jarsigner(
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-signedjar", SIGNED_JARFILE,
                UNSIGNED_JARFILE,
                KEY_ALIAS);

        checkSigning(analyzer, NOT_YET_VALID_CERT_SIGNING_WARNING);

        // verify signed jar
        analyzer = jarsigner(
                "-verify",
                "-verbose",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE,
                KEY_ALIAS);

        checkVerifying(analyzer, 0, NOT_YET_VALID_CERT_VERIFYING_WARNING);

        // verify jar in strict mode
        analyzer = jarsigner(
                "-verify",
                "-verbose",
                "-strict",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE,
                KEY_ALIAS);

        checkVerifying(analyzer, HAS_EXPIRED_CERT_EXIT_CODE,
                NOT_YET_VALID_CERT_VERIFYING_WARNING);

        System.out.println("Test passed");
    }

}
