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
 * @summary Test for aliasNotInStore warning
 * @library /test/lib ../
 * @build jdk.test.lib.util.JarUtils
 * @run main AliasNotInStoreTest
 */
public class AliasNotInStoreTest extends Test {

    /**
     * The test signs and verifies a jar that contains signed entries
     * that are not signed by any alias in keystore (aliasNotInStore).
     * Warning message is expected.
     */
    public static void main(String[] args) throws Throwable {
        AliasNotInStoreTest test = new AliasNotInStoreTest();
        test.start();
    }

    private void start() throws Throwable {
        Utils.createFiles(FIRST_FILE, SECOND_FILE);
        System.out.println(String.format("Create a %s that contains %s",
                new Object[]{UNSIGNED_JARFILE, FIRST_FILE}));
        JarUtils.createJar(UNSIGNED_JARFILE, FIRST_FILE);

        // create first key pair for signing
        createAlias(FIRST_KEY_ALIAS);
        createAlias(SECOND_KEY_ALIAS);

        // sign jar with first key
        OutputAnalyzer analyzer = jarsigner(
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-signedjar", SIGNED_JARFILE,
                UNSIGNED_JARFILE,
                FIRST_KEY_ALIAS);

        checkSigning(analyzer);

        System.out.println(String.format("Copy %s to %s, and add %s",
                new Object[] {SIGNED_JARFILE, UPDATED_SIGNED_JARFILE,
                    SECOND_FILE}));

        JarUtils.updateJar(SIGNED_JARFILE, UPDATED_SIGNED_JARFILE, SECOND_FILE);

        // sign jar with second key
        analyzer = jarsigner(
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                UPDATED_SIGNED_JARFILE,
                SECOND_KEY_ALIAS);

        checkSigning(analyzer);

        // create keystore that contains only first key
        keytool(
                "-importkeystore",
                "-srckeystore", KEYSTORE,
                "-srcalias", FIRST_KEY_ALIAS,
                "-srcstorepass", PASSWORD,
                "-srckeypass", PASSWORD,
                "-destkeystore", FIRST_KEY_KEYSTORE,
                "-destalias", FIRST_KEY_ALIAS,
                "-deststorepass", PASSWORD,
                "-destkeypass", PASSWORD).shouldHaveExitValue(0);

        // verify jar with keystore that contains only first key,
        // so there is signed entry (FirstClass.class) that is not signed
        // by any alias in the keystore
        analyzer = jarsigner(
                "-verify",
                "-verbose",
                "-keystore", FIRST_KEY_KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                UPDATED_SIGNED_JARFILE);

        checkVerifying(analyzer, 0, CHAIN_NOT_VALIDATED_VERIFYING_WARNING,
                ALIAS_NOT_IN_STORE_VERIFYING_WARNING);

        // verify jar with keystore that contains only first key in strict mode
        analyzer = jarsigner(
                "-verify",
                "-verbose",
                "-strict",
                "-keystore", FIRST_KEY_KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                UPDATED_SIGNED_JARFILE);

        int expectedExitCode = ALIAS_NOT_IN_STORE_EXIT_CODE
                + CHAIN_NOT_VALIDATED_EXIT_CODE;
        checkVerifying(analyzer, expectedExitCode,
                CHAIN_NOT_VALIDATED_VERIFYING_WARNING,
                ALIAS_NOT_IN_STORE_VERIFYING_WARNING);

        System.out.println("Test passed");
    }

}
