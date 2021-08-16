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
 * @summary Test for hasExpiredCert warning
 * @library /test/lib ../
 * @build jdk.test.lib.util.JarUtils
 * @run main HasExpiredCertTest
 */
public class HasExpiredCertTest extends Test {

    static final int SHORT_VALIDITY = 365;

    /**
     * The test signs and verifies a jar that contains entries
     * whose signer certificate has expired (hasExpiredCert).
     * Warning message is expected.
     */
    public static void main(String[] args) throws Throwable {
        HasExpiredCertTest test = new HasExpiredCertTest();
        test.start();
    }

    private void start() throws Throwable {
        // create a jar file that contains one class file
        Utils.createFiles(FIRST_FILE);
        JarUtils.createJar(UNSIGNED_JARFILE, FIRST_FILE);

        // create key pair for jar signing
        createAlias(CA_KEY_ALIAS);
        createAlias(KEY_ALIAS);

        issueCert(
                KEY_ALIAS,
                "-startdate", "-" + SHORT_VALIDITY * 2 + "d",
                "-validity", Integer.toString(SHORT_VALIDITY));

        // sign jar
        OutputAnalyzer analyzer = jarsigner(
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-signedjar", SIGNED_JARFILE,
                UNSIGNED_JARFILE,
                KEY_ALIAS);

        checkSigning(analyzer, HAS_EXPIRED_CERT_SIGNING_WARNING);

        // verify signed jar
        analyzer = jarsigner(
                "-verify",
                "-verbose",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE);

        checkVerifying(analyzer, 0, HAS_EXPIRED_CERT_VERIFYING_WARNING);

        analyzer = jarsigner(
                "-verify",
                "-strict",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE);

        checkVerifying(analyzer, HAS_EXPIRED_CERT_EXIT_CODE,
                HAS_EXPIRED_CERT_VERIFYING_WARNING);

        System.out.println("Test passed");
    }

}
