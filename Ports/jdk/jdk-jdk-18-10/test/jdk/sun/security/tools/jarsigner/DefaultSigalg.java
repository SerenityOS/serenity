/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8057810
 * @summary New defaults for DSA keys in jarsigner and keytool
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.tools.keytool
 *          java.base/sun.security.util
 *          java.base/sun.security.x509
 *          jdk.jartool/sun.security.tools.jarsigner
 *          jdk.jartool/sun.tools.jar
 */

import sun.security.pkcs.PKCS7;
import sun.security.util.KeyUtil;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.jar.JarFile;

public class DefaultSigalg {

    public static void main(String[] args) throws Exception {

        // Three test cases
        String[] keyalgs = {"DSA", "RSA", "EC"};
        // Expected default keytool sigalg
        String[] sigalgs = {"SHA256withDSA", "SHA256withRSA", "SHA256withECDSA"};
        // Expected keysizes
        int[] keysizes = {2048, 2048, 256};
        // Expected jarsigner digest alg used in signature
        String[] digestalgs = {"SHA-256", "SHA-256", "SHA-256"};

        // Create a jar file
        sun.tools.jar.Main m =
                new sun.tools.jar.Main(System.out, System.err, "jar");
        Files.write(Paths.get("x"), new byte[10]);
        if (!m.run("cvf a.jar x".split(" "))) {
            throw new Exception("jar creation failed");
        }

        // Generate keypairs and sign the jar
        Files.deleteIfExists(Paths.get("jks"));
        for (String keyalg: keyalgs) {
            sun.security.tools.keytool.Main.main(
                    ("-keystore jks -storepass changeit -keypass changeit " +
                            "-dname CN=A -alias " + keyalg + " -genkeypair " +
                            "-keyalg " + keyalg).split(" "));
            sun.security.tools.jarsigner.Main.main(
                    ("-keystore jks -storepass changeit a.jar " + keyalg).split(" "));
        }

        // Check result
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream jks = new FileInputStream("jks");
                JarFile jf = new JarFile("a.jar")) {
            ks.load(jks, "changeit".toCharArray());
            for (int i = 0; i<keyalgs.length; i++) {
                String keyalg = keyalgs[i];
                // keytool
                X509Certificate c = (X509Certificate) ks.getCertificate(keyalg);
                String sigalg = c.getSigAlgName();
                if (!sigalg.equals(sigalgs[i])) {
                    throw new Exception(
                            "keytool sigalg for " + keyalg + " is " + sigalg);
                }
                int keysize = KeyUtil.getKeySize(c.getPublicKey());
                if (keysize != keysizes[i]) {
                    throw new Exception(
                            "keytool keysize for " + keyalg + " is " + keysize);
                }
                // jarsigner
                String bk = "META-INF/" + keyalg + "." + keyalg;
                try (InputStream is = jf.getInputStream(jf.getEntry(bk))) {
                    String digestalg = new PKCS7(is).getSignerInfos()[0]
                            .getDigestAlgorithmId().toString();
                    if (!digestalg.equals(digestalgs[i])) {
                        throw new Exception(
                                "jarsigner digest of sig for " + keyalg
                                        + " is " + digestalg);
                    }
                }
            }
        }
    }
}
