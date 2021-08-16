/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.JarEntry;
import jdk.test.lib.SecurityTools;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @test
 * @bug 8217375
 * @library /test/lib
 * @run testng DigestDontIgnoreCase
 * @summary Check that existing manifest digest entries are taken for valid
 * only if they match the actual digest value also taking upper and lower
 * case of the base64 encoded form of the digests into account.
 */
/*
 * <pre>mfDigest.equalsIgnoreCase(base64Digests[i])</pre>
 * previously in JarSigner.java on on line 985
 * @see jdk.security.jarsigner.JarSigner#updateDigests
 */
public class DigestDontIgnoreCase {

    static final String KEYSTORE_FILENAME = "test.jks";

    static final String DUMMY_FILE1 = "dummy1.txt";
    static final byte[] DUMMY_CONTENTS1 = DUMMY_FILE1.getBytes(UTF_8);
    static final String DUMMY_FILE2 = "dummy2.txt";
    static final byte[] DUMMY_CONTENTS2 = DUMMY_FILE2.getBytes(UTF_8);

    byte[] goodSignedManifest;

    @BeforeClass
    public void prepareCertificate() throws Exception {
        SecurityTools.keytool("-genkeypair -keyalg DSA -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit"
                + " -alias a -dname CN=X").shouldHaveExitValue(0);
    }

    void prepareJarFile(String filename, Map<String, byte[]> contents)
            throws IOException {
        try (OutputStream out = Files.newOutputStream(Path.of(filename));
                JarOutputStream jos = new JarOutputStream(out)) {
            for (Map.Entry<String, byte[]> entry : contents.entrySet()) {
                JarEntry je = new JarEntry(entry.getKey());
                jos.putNextEntry(je);
                jos.write(entry.getValue());
                jos.closeEntry();
            }
        }
    }

    @BeforeClass(dependsOnMethods = "prepareCertificate")
    public void prepareGoodSignedManifest() throws Exception {
        String filename = "prepare.jar";
        prepareJarFile(filename, Map.of(DUMMY_FILE1, DUMMY_CONTENTS1));
        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug " + filename + " a")
                .shouldHaveExitValue(0);
        goodSignedManifest = Utils.readJarManifestBytes(filename);
        Utils.echoManifest(goodSignedManifest,
                "reference manifest with one file signed");
    }

    void testWithManifest(String filename, byte[] manifestBytes)
            throws Exception {
        Utils.echoManifest(manifestBytes,
                "going to test " + filename + " with manifest");
        prepareJarFile(filename, Map.of(
                JarFile.MANIFEST_NAME, manifestBytes,
                DUMMY_FILE1, DUMMY_CONTENTS1, // with digest already in manifest
                DUMMY_FILE2, DUMMY_CONTENTS2)); // causes manifest update
        Utils.echoManifest(Utils.readJarManifestBytes(filename),
                filename + " created with manifest");
        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -debug -verbose " + filename + " a")
                .shouldHaveExitValue(0);
        Utils.echoManifest(Utils.readJarManifestBytes(filename),
                filename + " signed resulting in manifest");
        SecurityTools.jarsigner("-verify -strict -keystore " +
                KEYSTORE_FILENAME + " -storepass changeit -debug -verbose " +
                filename + " a").shouldHaveExitValue(0);
    }

    @Test
    public void verifyDigestGoodCase() throws Exception {
        testWithManifest("good.jar", goodSignedManifest);
    }

    @Test
    public void testDigestHeaderNameCase() throws Exception {
        byte[] mfBadHeader = new String(goodSignedManifest, UTF_8).
                replace("SHA-256-Digest", "sha-256-dIGEST").getBytes(UTF_8);
        testWithManifest("switch-header-name-case.jar", mfBadHeader);
    }

    @Test
    public void testDigestWrongCase() throws Exception {
        byte[] mfBadDigest = switchCase(goodSignedManifest, "Digest");
        testWithManifest("switch-digest-case.jar", mfBadDigest);
    }

    byte[] switchCase(byte[] manifest, String attrName) {
        byte[] wrongCase = Arrays.copyOf(manifest, manifest.length);
        byte[] name = (attrName + ":").getBytes(UTF_8);
        int matched = 0; // number of bytes before position i matching attrName
        for (int i = 0; i < wrongCase.length; i++) {
            if (wrongCase[i] == '\r' &&
                    (i == wrongCase.length - 1 || wrongCase[i + 1] == '\n')) {
                continue;
            } else if ((wrongCase[i] == '\r' || wrongCase[i] == '\n')
                    && (i == wrongCase.length - 1 || wrongCase[i + 1] != ' ')) {
                matched = 0;
            } else if (matched == name.length) {
                wrongCase[i] = switchCase(wrongCase[i]);
            } else if (name[matched] == wrongCase[i]) {
                matched++;
            } else {
                matched = 0;
            }
        }
        return wrongCase;
    }

    byte switchCase(byte c) {
        if (c >= 'A' && c <= 'Z') {
            return (byte) ('a' + (c - 'A'));
        } else if (c >= 'a' && c <= 'z') {
            return (byte) ('A' + (c - 'a'));
        } else {
            return c;
        }
    }

}
