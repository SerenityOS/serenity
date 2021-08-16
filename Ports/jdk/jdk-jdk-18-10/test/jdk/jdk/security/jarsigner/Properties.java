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

/*
 * @test
 * @bug 8242068
 * @summary test the properties
 * @library /test/lib
 * @modules java.base/sun.security.tools.keytool
 *          java.base/sun.security.x509
 *          java.base/sun.security.util
 *          jdk.jartool
 */

import jdk.security.jarsigner.JarSigner;
import jdk.test.lib.Asserts;
import jdk.test.lib.security.DerUtils;
import jdk.test.lib.util.JarUtils;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipFile;

public class Properties {

    public static void main(String[] args) throws Exception {

        Files.writeString(Path.of("anything"), "anything");
        JarUtils.createJarFile(Path.of("src.jar"), Path.of("."),
                Files.write(Path.of("anything"), new byte[100]));

        var cakg = new CertAndKeyGen("EC", "SHA1withECDSA");
        cakg.generate("secp256r1");
        JarSigner.Builder jsb = new JarSigner.Builder(
                cakg.getPrivateKey(),
                CertificateFactory.getInstance("X.509").generateCertPath(List.of(
                        cakg.getSelfCertificate(new X500Name("CN=Me"), 100))));
        jsb.signerName("E");
        String sf;

        byte[] i0 = sign(jsb.setProperty("internalsf", "false"));
        // EncapsulatedContentInfo no content
        DerUtils.shouldNotExist(i0, "1021");

        byte[] i1 = sign(jsb.setProperty("internalsf", "true"));
        // EncapsulatedContentInfo has content being the SF
        sf = new String(DerUtils.innerDerValue(i1, "10210").getOctetString());
        Asserts.assertTrue(sf.startsWith("Signature-Version"));

        // There is a SignedAttributes
        byte[] d0 = sign(jsb);
        Asserts.assertTrue(DerUtils.innerDerValue(d0, "10403")
                .isContextSpecific((byte)0));

        // Has a hash for the whole manifest
        byte[] s0 = sign(jsb.setProperty("sectionsonly", "false"));
        sf = new String(DerUtils.innerDerValue(s0, "10210").getOctetString());
        Asserts.assertTrue(sf.contains("SHA-256-Digest-Manifest:"));

        // Has no hash for the whole manifest
        byte[] s1 = sign(jsb.setProperty("sectionsonly", "true"));
        sf = new String(DerUtils.innerDerValue(s1, "10210").getOctetString());
        Asserts.assertFalse(sf.contains("SHA-256-Digest-Manifest:"));
    }

    // Sign and returns the content of the PKCS7 signature block inside
    static byte[] sign(JarSigner.Builder b) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ZipFile zf = new ZipFile("src.jar")) {
            b.build().sign(zf, bout);
        }
        var jf = new JarInputStream(
                new ByteArrayInputStream(bout.toByteArray()));
        while (true) {
            JarEntry je = jf.getNextJarEntry();
            if (je == null) {
                throw new RuntimeException("Cannot find signature");
            }
            if (je.getName().equals("META-INF/E.EC")) {
                return jf.readAllBytes();
            }
        }
    }
}
