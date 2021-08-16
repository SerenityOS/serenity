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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import jdk.test.lib.util.JarUtils;
import jdk.test.lib.SecurityTools;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @test
 * @bug 8217375
 * @library /test/lib
 * @run testng SectionNameContinuedVsLineBreak
 * @summary Checks some specific line break character sequences in section name
 * continuation line breaks.
 */
public class SectionNameContinuedVsLineBreak {

    static final String KEYSTORE_FILENAME = "test.jks";

    @BeforeTest
    public void prepareCertificate() throws Exception {
        SecurityTools.keytool("-genkeypair -keyalg EC -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit"
                + " -alias a -dname CN=A").shouldHaveExitValue(0);
    }

    void manipulateManifestSignAgainA(
            String srcJarFilename, String dstJarFilename,
            Function<Manifest, byte[]> manifestManipulation) throws Exception {
        byte[] manipulatedManifest = manifestManipulation.apply(
                new Manifest(new ByteArrayInputStream(
                        Utils.readJarManifestBytes(srcJarFilename))));
        Utils.echoManifest(manipulatedManifest, "manipulated manifest");
        JarUtils.updateJar(srcJarFilename, dstJarFilename, Map.of(
                JarFile.MANIFEST_NAME, manipulatedManifest));
        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug " + dstJarFilename + " a")
                .shouldHaveExitValue(0);
        Utils.echoManifest(Utils.readJarManifestBytes(
                dstJarFilename), "manipulated jar signed again with a");
        // check assumption that jar is valid at this point
        SecurityTools.jarsigner("-verify -keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug " + dstJarFilename + " a")
                .shouldHaveExitValue(0);
    }

    void test(String name, Function<Manifest, byte[]> manifestManipulation,
            String jarContentFilename) throws Exception {
        String jarFilename1 = "test-" + name + "-step1.jar";
        Files.write(Path.of(jarContentFilename),
                jarContentFilename.getBytes(UTF_8));
        JarUtils.createJarFile(Path.of(jarFilename1), (Manifest)
                /* no manifest will let jarsigner create a default one */ null,
                Path.of("."), Path.of(jarContentFilename));
        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug " + jarFilename1 +
                " a").shouldHaveExitValue(0);
        Utils.echoManifest(Utils.readJarManifestBytes(
                jarFilename1), "signed jar");
        String jarFilename2 = "test-" + name + "-step2.jar";
        manipulateManifestSignAgainA(jarFilename1, jarFilename2,
                manifestManipulation);

        SecurityTools.jarsigner("-verify -strict -keystore " +
                KEYSTORE_FILENAME + " -storepass changeit -debug -verbose " +
                jarFilename2 + " a").shouldHaveExitValue(0);
    }

    /**
     * Test signing a jar with a manifest that has an entry the name of
     * which continued on a continuation line with '\r' as line break before
     * the continuation line space ' ' on the line the name starts.
     */
    @Test
    public void testContinueNameAfterCr() throws Exception {
        String filename = "abc";
        test("testContinueNameAfterCr", m -> {
            String digest = m.getAttributes("abc").getValue("SHA-256-Digest");
            m.getEntries().remove("abc");
            return (manifestToString(m)
                    + "Name: a\r"
                    + " bc\r\n"
                    + "SHA-256-Digest: " + digest + "\r\n"
                    + "\r\n").getBytes(UTF_8);
        }, filename);
    }

    /**
     * Test signing a jar with a manifest that has an entry the name of
     * which continued on a continuation line with '\r' as line break before
     * the continuation line space ' ' after a first continuation.
     */
    @Test
    public void testContinueNameAfterCrOnContinuationLine() throws Exception {
        String filename = "abc";
        test("testContinueNameAfterCr", m -> {
            String digest = m.getAttributes("abc").getValue("SHA-256-Digest");
            m.getEntries().remove("abc");
            return (manifestToString(m)
                    + "Name: a\r\n"
                    + " b\r"
                    + " c\r\n"
                    + "SHA-256-Digest: " + digest + "\r\n"
                    + "\r\n").getBytes(UTF_8);
        }, filename);
    }

    /**
     * Test signing a jar with a manifest that has an entry the name of
     * which continued on a continuation line and terminated with '\r' as line
     * break after the name.
     */
    @Test
    public void testEndNameWithCrOnContinuationLine() throws Exception {
        String filename = "abc";
        test("testContinueNameAfterCr", m -> {
            String digest = m.getAttributes("abc").getValue("SHA-256-Digest");
            m.getEntries().remove("abc");
            return (manifestToString(m)
                    + "Name: a\r\n"
                    + " bc\r"
                    + "SHA-256-Digest: " + digest + "\r\n"
                    + "\r\n").getBytes(UTF_8);
        }, filename);
    }

    String manifestToString(Manifest mf) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            mf.write(out);
            return out.toString(UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
