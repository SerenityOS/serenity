/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8056174 8242260
 * @summary Make sure the jarsigner tool still works after it's modified to
 *          be based on JarSigner API
 * @library /test/lib
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.x509
 */

import com.sun.jarsigner.ContentSigner;
import com.sun.jarsigner.ContentSignerParameters;
import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.util.JarUtils;
import sun.security.pkcs.PKCS7;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Options {

    public static void main(String[] args) throws Exception {

        // Help
        boolean lastLineHasAltSigner = false;
        for (String line : SecurityTools.jarsigner("--help").asLines()) {
            if (line.contains("-altsigner")) {
                lastLineHasAltSigner = true;
            } else {
                if (lastLineHasAltSigner) {
                    Asserts.assertTrue(line.contains("deprecated and will be removed"));
                }
                lastLineHasAltSigner = false;
            }
        }

        // Prepares raw file
        Files.write(Path.of("a"), List.of("a"));

        // Pack
        JarUtils.createJarFile(Path.of("a.jar"), Path.of("."), Path.of("a"));

        // Prepare a keystore
        SecurityTools.keytool(
                "-keystore jks -storepass changeit -keypass changeit -dname" +
                        " CN=A -alias a -genkeypair -keyalg rsa")
                .shouldHaveExitValue(0);

        // -altsign
        SecurityTools.jarsigner(
                "-debug -signedjar altsign.jar -keystore jks -storepass changeit" +
                        " -altsigner Options$X" +
                        " -altsignerpath " + System.getProperty("test.classes") +
                        " a.jar a")
                .shouldContain("removed in a future release: -altsigner")
                .shouldContain("removed in a future release: -altsignerpath")
                .shouldContain("PKCS7.parse");  // signature not parseable
                                                // but signing succeeds

        try (JarFile jf = new JarFile("altsign.jar")) {
            JarEntry je = jf.getJarEntry("META-INF/A.RSA");
            try (InputStream is = jf.getInputStream(je)) {
                if (!Arrays.equals(is.readAllBytes(), "1234".getBytes())) {
                    throw new Exception("altsign go wrong");
                }
            }
        }

        // -altsign with no -altsignerpath
        Files.copy(Path.of(System.getProperty("test.classes"), "Options$X.class"),
                Path.of("Options$X.class"));
        SecurityTools.jarsigner(
                "-debug -signedjar altsign.jar -keystore jks -storepass changeit" +
                        " -altsigner Options$X" +
                        " a.jar a")
                .shouldContain("removed in a future release: -altsigner")
                .shouldNotContain("removed in a future release: -altsignerpath")
                .shouldContain("PKCS7.parse");  // signature not parseable
                                                // but signing succeeds

        // -sigfile, -digestalg, -sigalg, -internalsf, -sectionsonly
        SecurityTools.jarsigner(
                "-debug -signedjar new.jar -keystore jks -storepass changeit" +
                " -sigfile olala -digestalg SHA1 -sigalg SHA224withRSA" +
                " -internalsf -sectionsonly a.jar a")
                .shouldHaveExitValue(0)
                .shouldNotContain("Exception");     // a real success

        try (JarFile jf = new JarFile("new.jar")) {
            JarEntry je = jf.getJarEntry("META-INF/OLALA.SF");
            Objects.requireNonNull(je);     // check -sigfile
            byte[] sf = null;               // content of .SF
            try (InputStream is = jf.getInputStream(je)) {
                sf = is.readAllBytes();     // save for later comparison
                Attributes attrs = new Manifest(new ByteArrayInputStream(sf))
                        .getMainAttributes();
                // check -digestalg
                if (!attrs.containsKey(new Attributes.Name(
                        "SHA1-Digest-Manifest-Main-Attributes"))) {
                    throw new Exception("digestalg incorrect");
                }
                // check -sectionsonly
                if (attrs.containsKey(new Attributes.Name(
                        "SHA1-Digest-Manifest"))) {
                    throw new Exception("SF should not have file digest");
                }
            }

            je = jf.getJarEntry("META-INF/OLALA.RSA");
            try (InputStream is = jf.getInputStream(je)) {
                PKCS7 p7 = new PKCS7(is.readAllBytes());
                String alg = p7.getSignerInfos()[0]
                        .getDigestAlgorithmId().getName();
                if (!alg.equals("SHA-224")) {   // check -sigalg
                    throw new Exception("PKCS7 signing is using " + alg);
                }
                // check -internalsf
                if (!Arrays.equals(sf, p7.getContentInfo().getData())) {
                    throw new Exception("SF not in RSA");
                }
            }

        }

        // TSA-related ones are checked in ts.sh
    }

    public static class X extends ContentSigner {
        @Override
        public byte[] generateSignedData(ContentSignerParameters parameters,
                boolean omitContent, boolean applyTimestamp) {
            return "1234".getBytes();
        }
    }
}
