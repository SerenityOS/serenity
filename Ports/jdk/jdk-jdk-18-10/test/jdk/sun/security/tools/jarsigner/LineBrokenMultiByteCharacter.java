/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6695402
 * @summary verify signatures of jars containing classes with names
 *          with multi-byte unicode characters broken across lines
 * @library /test/lib
 */

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;

import static java.nio.charset.StandardCharsets.UTF_8;

import jdk.test.lib.SecurityTools;
import jdk.test.lib.util.JarUtils;

public class LineBrokenMultiByteCharacter {

    /**
     * this name will break across lines in MANIFEST.MF at the
     * middle of a two-byte utf-8 encoded character due to its e acute letter
     * at its exact position.
     *
     * because no file with such a name exists {@link JarUtils} will add the
     * name itself as contents to the jar entry which would have contained a
     * compiled class in the original bug. For this test, the contents of the
     * files contained in the jar file is not important as long as they get
     * signed.
     *
     * @see #verifyClassNameLineBroken(JarFile, String)
     */
    static final String testClassName =
            "LineBrokenMultiByteCharacterA1234567890B1234567890C123456789D1234\u00E9xyz.class";

    static final String anotherName =
            "LineBrokenMultiByteCharacterA1234567890B1234567890C123456789D1234567890.class";

    static final String alias = "a";
    static final String keystoreFileName = "test.jks";
    static final String manifestFileName = "MANIFEST.MF";

    public static void main(String[] args) throws Exception {
        prepare();

        testSignJar("test.jar");
        testSignJarNoManifest("test-no-manifest.jar");
        testSignJarUpdate("test-update.jar", "test-updated.jar");
    }

    static void prepare() throws Exception {
        SecurityTools.keytool("-keystore", keystoreFileName, "-genkeypair",
                "-keyalg", "dsa",
                "-storepass", "changeit", "-keypass", "changeit", "-storetype",
                "JKS", "-alias", alias, "-dname", "CN=X", "-validity", "366")
            .shouldHaveExitValue(0);

        Files.write(Paths.get(manifestFileName), (Name.
                MANIFEST_VERSION.toString() + ": 1.0\r\n").getBytes(UTF_8));
    }

    static void testSignJar(String jarFileName) throws Exception {
        JarUtils.createJar(jarFileName, manifestFileName, testClassName);
        verifyJarSignature(jarFileName);
    }

    static void testSignJarNoManifest(String jarFileName) throws Exception {
        JarUtils.createJar(jarFileName, testClassName);
        verifyJarSignature(jarFileName);
    }

    static void testSignJarUpdate(
            String initialFileName, String updatedFileName) throws Exception {
        JarUtils.createJar(initialFileName, manifestFileName, anotherName);
        SecurityTools.jarsigner("-keystore", keystoreFileName, "-storetype",
                "JKS", "-storepass", "changeit", "-debug", initialFileName,
                alias).shouldHaveExitValue(0);
        JarUtils.updateJar(initialFileName, updatedFileName, testClassName);
        verifyJarSignature(updatedFileName);
    }

    static void verifyJarSignature(String jarFileName) throws Exception {
        // actually sign the jar
        SecurityTools.jarsigner("-keystore", keystoreFileName, "-storetype",
                "JKS", "-storepass", "changeit", "-debug", jarFileName, alias)
            .shouldHaveExitValue(0);

        try (
            JarFile jar = new JarFile(jarFileName);
        ) {
            verifyClassNameLineBroken(jar, testClassName);
            verifyCodeSigners(jar, jar.getJarEntry(testClassName));
        }
    }

    /**
     * it would be too easy to miss the actual test case by just renaming an
     * identifier so that the multi-byte encoded character would not any longer
     * be broken across a line break.
     *
     * this check here verifies that the actual test case is tested based on
     * the manifest and not based on the signature file because at the moment,
     * the signature file does not even contain the desired entry at all.
     *
     * this relies on {@link java.util.jar.Manifest} breaking lines unaware
     * of bytes that belong to the same multi-byte utf characters.
     */
    static void verifyClassNameLineBroken(JarFile jar, String className)
            throws IOException {
        byte[] eAcute = "\u00E9".getBytes(UTF_8);
        byte[] eAcuteBroken =
                new byte[] {eAcute[0], '\r', '\n', ' ', eAcute[1]};

        if (jar.getManifest().getAttributes(className) == null) {
            throw new AssertionError(className + " not found in manifest");
        }

        JarEntry manifestEntry = jar.getJarEntry(JarFile.MANIFEST_NAME);
        try (
            InputStream manifestIs = jar.getInputStream(manifestEntry);
        ) {
            int bytesMatched = 0;
            for (int b = manifestIs.read(); b > -1; b = manifestIs.read()) {
                if ((byte) b == eAcuteBroken[bytesMatched]) {
                    bytesMatched++;
                    if (bytesMatched == eAcuteBroken.length) {
                        break;
                    }
                } else {
                    bytesMatched = 0;
                }
            }
            if (bytesMatched < eAcuteBroken.length) {
                throw new AssertionError("self-test failed: multi-byte "
                        + "utf-8 character not broken across lines");
            }
        }
    }

    static void verifyCodeSigners(JarFile jar, JarEntry jarEntry)
            throws IOException {
        // codeSigners is initialized only after the entry has been read
        try (
            InputStream inputStream = jar.getInputStream(jarEntry);
        ) {
            inputStream.readAllBytes();
        }

        // a check for the presence of code signers is sufficient to check
        // bug JDK-6695402. no need to also verify the actual code signers
        // attributes here.
        if (jarEntry.getCodeSigners() == null
                || jarEntry.getCodeSigners().length == 0) {
            throw new AssertionError(
                    "no signing certificate found for " + jarEntry.getName());
        }
    }

}
