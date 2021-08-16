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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

/**
 * @test
 * @bug 8024302 8026037
 * @summary Test for notSignedByAlias warning
 * @library /test/lib ../
 * @build jdk.test.lib.util.JarUtils
 * @run main NotSignedByAliasTest
 */
public class NotSignedByAliasTest extends Test {

    /**
     * The test signs and verifies a jar that contains signed entries
     * which are not signed by the specified alias(es) (notSignedByAlias).
     * Warning message is expected.
     */
    public static void main(String[] args) throws Throwable {
        NotSignedByAliasTest test = new NotSignedByAliasTest();
        test.start();
    }

    protected void start() throws Throwable {
        // create a jar file that contains one class file
        Utils.createFiles(FIRST_FILE);
        JarUtils.createJar(UNSIGNED_JARFILE, FIRST_FILE);

        createAlias(CA_KEY_ALIAS, "-ext", "bc:c");

        // create first key pair for signing
        createAlias(FIRST_KEY_ALIAS);
        issueCert(
                FIRST_KEY_ALIAS,
                "-validity", Integer.toString(VALIDITY));

        // create first key pair for signing
        createAlias(SECOND_KEY_ALIAS);
        issueCert(
                SECOND_KEY_ALIAS,
                "-validity", Integer.toString(VALIDITY));

        // sign jar with first key
        OutputAnalyzer analyzer = jarsigner(
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-signedjar", SIGNED_JARFILE,
                UNSIGNED_JARFILE,
                FIRST_KEY_ALIAS);

        checkSigning(analyzer);

        // verify jar with second key
        analyzer = jarsigner(
                "-verify",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE,
                SECOND_KEY_ALIAS);

        checkVerifying(analyzer, 0, NOT_SIGNED_BY_ALIAS_VERIFYING_WARNING);

        // verify jar with second key in strict mode
        analyzer = jarsigner(
                "-verify",
                "-strict",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE,
                SECOND_KEY_ALIAS);

        checkVerifying(analyzer, NOT_SIGNED_BY_ALIAS_EXIT_CODE,
                NOT_SIGNED_BY_ALIAS_VERIFYING_WARNING);

        // verify jar with non-existing alias
        analyzer = jarsigner(
                "-verify",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE,
                "bogus");

        checkVerifying(analyzer, 0, NOT_SIGNED_BY_ALIAS_VERIFYING_WARNING);

        // verify jar with non-existing alias in strict mode
        analyzer = jarsigner(
                "-verify",
                "-strict",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE,
                "bogus");

        checkVerifying(analyzer, NOT_SIGNED_BY_ALIAS_EXIT_CODE,
                NOT_SIGNED_BY_ALIAS_VERIFYING_WARNING);

        System.out.println("Test passed");
    }

}
