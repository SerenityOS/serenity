/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.util.JarUtils;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @bug 8248745
 * @summary Test key generation and jar signing with disabled algorithms and
 *          key sizes, with and without entries in jdk.jar.disabledAlgorithms,
 *          jdk.certpath.disabledAlgorithms
 * @library /test/lib
 * @run main/othervm RestrictedAlgo RESTRICT
 * @run main/othervm RestrictedAlgo NO_RESTRICT
 */

public class RestrictedAlgo {

    private static final String KEYSTORE = "keystore.jks";
    private static final String PASSWORD = "password";
    private static final String SIGNED_JARFILE = "signed.jar";
    private static final String UNSIGNED_JARFILE = "unsigned.jar";
    private static final String SECURITY_FILE = "java.security";
    private static final String NO_RESTRICT = "-J-Djava.security.properties="
            + SECURITY_FILE;
    private static final String FIRST_FILE = "first.txt";
    private static final String WARNING = "Warning:";
    private static final String SECURITY_WARNING =
            ".* is considered a security risk and is disabled.";

    private static String algoStatus;

    public static void main(String[] args) throws Exception {

        algoStatus = args[0];
        // create a jar file that contains one file
        JarUtils.createJarFile(Path.of(UNSIGNED_JARFILE), Path.of("."),
                new File(FIRST_FILE).exists() ? Paths.get(FIRST_FILE)
                        : Files.createFile(Paths.get(FIRST_FILE)));
        if (!isAlgoRestricted()) {
            // An alternative security properties
            Files.writeString(Files.createFile(Paths.get(SECURITY_FILE)),
                        "jdk.certpath.disabledAlgorithms=\n"
                        + "jdk.jar.disabledAlgorithms=\n"
                        + "jdk.security.legacyAlgorithms=");
        }

        System.out.println("\nTesting sigalg MD2\n");
        test("RSA", "MD2withRSA", "SigAlgMD2", "SHA256", true);

        System.out.println("\nTesting sigalg MD5\n");
        test("RSA", "MD5withRSA", "SigAlgMD5", "SHA256", true);

        System.out.println("\nTesting digestalg MD2\n");
        test("RSA", "SHA256withRSA", "DigestAlgMD2", "MD2", false);

        System.out.println("\nTesting digestalg MD5\n");
        test("RSA", "SHA256withRSA", "DigestAlgMD5", "MD5", false);

        System.out.println("\nTesting RSA Keysize: RSA keySize < 1024\n");
        test("RSA", "SHA256withRSA", "KeySizeRSA", "SHA256", true,
                "-keysize", "512");

        System.out.println("\nTesting DSA Keysize: DSA keySize < 1024\n");
        test("DSA", "SHA256withDSA", "KeySizeDSA", "SHA256", true,
                "-keysize", "512");
    }

    private static void test(String keyAlg, String sigAlg, String aliasPrefix,
            String digestAlg, boolean isKeyToolVerify,
            String... addKeyToolArgs) throws Exception  {

        String alias = aliasPrefix + "_" + algoStatus;
        testKeytool(keyAlg, sigAlg, alias, isKeyToolVerify, addKeyToolArgs);
        testJarSignerSigning(sigAlg, alias, digestAlg);
        testJarSignerVerification();
    }

    private static void testKeytool(String keyAlg, String sigAlg, String alias,
            boolean isKeyToolVerify, String... additionalCmdArgs)
                    throws Exception {

        System.out.println("Testing Keytool\n");
        List<String> cmd = prepareCommand(
                "-genkeypair",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-dname", "CN=Test",
                "-ext", "bc:c",
                "-keyalg", keyAlg,
                "-sigalg", sigAlg,
                "-alias", alias);
        for (String additionalCMDArg : additionalCmdArgs) {
            cmd.add(additionalCMDArg);
        }

        OutputAnalyzer analyzer = SecurityTools.keytool(cmd)
                .shouldHaveExitValue(0);
        if (isKeyToolVerify) {
            verifyAnalyzer(analyzer);
        }
    }

    private static void testJarSignerSigning(String sigAlg, String alias,
            String digestAlg) throws Exception  {

        System.out.println("\nTesting JarSigner Signing\n");
        List<String> cmd = prepareCommand(
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-sigalg", sigAlg,
                "-digestalg", digestAlg,
                "-signedjar", SIGNED_JARFILE,
                UNSIGNED_JARFILE,
                alias);

        OutputAnalyzer analyzer = SecurityTools.jarsigner(cmd)
                .shouldHaveExitValue(0);

        verifyAnalyzer(analyzer);
    }

    private static void testJarSignerVerification()
            throws Exception  {

        System.out.println("\nTesting JarSigner Verification\n");
        List<String> cmd = prepareCommand(
                "-verify",
                SIGNED_JARFILE);

        OutputAnalyzer analyzer = SecurityTools.jarsigner(cmd)
                .shouldHaveExitValue(0);

        if (isAlgoRestricted()) {
            analyzer.shouldContain("The jar will be treated as unsigned,"
                    + " because it is signed with a weak algorithm that "
                    + "is now disabled.");
        } else {
            analyzer.shouldContain("jar verified.");
        }
    }

    private static List<String> prepareCommand(String... options) {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(options));
        if (!isAlgoRestricted()) {
            cmd.add(NO_RESTRICT);
        }
        return cmd;
    }

    private static void verifyAnalyzer(OutputAnalyzer analyzer) {
        if (isAlgoRestricted()) {
            analyzer.shouldContain(WARNING)
                    .shouldMatch(SECURITY_WARNING);
        } else {
            analyzer.shouldNotMatch(SECURITY_WARNING);
        }
    }

    private static boolean isAlgoRestricted() {
        return ("RESTRICT".equals(algoStatus)) ? true : false;
    }
}
