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

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @test
 * @bug 8024302 8026037
 * @summary Test for chainNotValidated warning
 * @library /test/lib ../
 * @build jdk.test.lib.util.JarUtils
 * @run main ChainNotValidatedTest ca2yes
 * @run main ChainNotValidatedTest ca2no
 */
public class ChainNotValidatedTest extends Test {

    public static void main(String[] args) throws Throwable {
        ChainNotValidatedTest test = new ChainNotValidatedTest();
        test.start(args[0].equals("ca2yes"));
    }

    private void start(boolean ca2yes) throws Throwable {
        // create a jar file that contains one class file
        Utils.createFiles(FIRST_FILE);
        JarUtils.createJar(UNSIGNED_JARFILE, FIRST_FILE);

        // We have 2 @run. Need cleanup.
        Files.deleteIfExists(Paths.get(KEYSTORE));

        // Root CA is not checked at all. If the intermediate CA has
        // BasicConstraints extension set to true, it will be valid.
        // Otherwise, chain validation will fail.
        createAlias(CA_KEY_ALIAS, "-ext", "bc:c");
        createAlias(CA2_KEY_ALIAS);
        issueCert(CA2_KEY_ALIAS,
                "-ext",
                "bc=ca:" + ca2yes);

        createAlias(KEY_ALIAS);
        issueCert(KEY_ALIAS, "-alias", CA2_KEY_ALIAS);

        // remove CA2 certificate so it's not trusted
        keytool(
                "-delete",
                "-alias", CA2_KEY_ALIAS,
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD).shouldHaveExitValue(0);

        // sign jar
        OutputAnalyzer analyzer = jarsigner(
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-signedjar", SIGNED_JARFILE,
                UNSIGNED_JARFILE,
                KEY_ALIAS);

        if (ca2yes) {
            checkSigning(analyzer, "!" + CHAIN_NOT_VALIDATED_SIGNING_WARNING);
        } else {
            checkSigning(analyzer, CHAIN_NOT_VALIDATED_SIGNING_WARNING);
        }

        // verify signed jar
        analyzer = jarsigner(
                "-verify",
                "-verbose",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE);

        if (ca2yes) {
            checkVerifying(analyzer, 0, "!" + CHAIN_NOT_VALIDATED_VERIFYING_WARNING);
        } else {
            checkVerifying(analyzer, 0, CHAIN_NOT_VALIDATED_VERIFYING_WARNING);
        }

        // verify signed jar in strict mode
        analyzer = jarsigner(
                "-verify",
                "-verbose",
                "-strict",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                SIGNED_JARFILE);

        if (ca2yes) {
            checkVerifying(analyzer, 0,
                    "!" + CHAIN_NOT_VALIDATED_VERIFYING_WARNING);
        } else {
            checkVerifying(analyzer, CHAIN_NOT_VALIDATED_EXIT_CODE,
                    CHAIN_NOT_VALIDATED_VERIFYING_WARNING);
        }

        System.out.println("Test passed");
    }

}
