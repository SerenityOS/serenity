/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048357
 * @summary Read signed data in one or more PKCS7 objects from individual files,
 * verify SignerInfos and certificate chain.
 * @modules java.base/sun.security.pkcs
 * @run main/othervm PKCS7VerifyTest PKCS7TEST.DSA.base64
 * @run main/othervm PKCS7VerifyTest PKCS7TEST.DSA.base64 PKCS7TEST.SF
 */
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;

public class PKCS7VerifyTest {

    static final String TESTSRC = System.getProperty("test.src", ".");
    static final String FS = File.separator;
    static final String FILEPATH = TESTSRC + FS + "jarsigner" + FS + "META-INF"
            + FS;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new RuntimeException("usage: java JarVerify <file1> <file2>");
        }

        Security.setProperty("jdk.jar.disabledAlgorithms", "");

        // The command " java PKCS7VerifyTest file1 [file2] "
        // treats file1 as containing the DER encoding of a PKCS7 signed data
        // object. If file2 is absent, the program verifies that some signature
        // (SignerInfo) file1 correctly signs the data contained in the
        // ContentInfo component of the PKCS7 object encoded by file1. If file2
        // is present, the program verifies file1 contains a correct signature
        // for the contents of file2.

        PKCS7 pkcs7;
        byte[] data;

        // to avoid attaching binary DSA file, the DSA file was encoded
        // in Base64, decode encoded Base64 DSA file below
        byte[] base64Bytes = Files.readAllBytes(Paths.get(FILEPATH + args[0]));
        pkcs7 = new PKCS7(new ByteArrayInputStream(
                Base64.getMimeDecoder().decode(base64Bytes)));
        if (args.length < 2) {
            data = null;
        } else {
            data = Files.readAllBytes(Paths.get(FILEPATH + args[1]));

        }

        SignerInfo[] signerInfos = pkcs7.verify(data);

        if (signerInfos == null) {
            throw new RuntimeException("no signers verify");
        }
        System.out.println("Verifying SignerInfos:");
        for (SignerInfo signerInfo : signerInfos) {
            System.out.println(signerInfo.toString());
        }

        X509Certificate certs[] = pkcs7.getCertificates();

        HashMap<String, X509Certificate> certTable = new HashMap(certs.length);
        for (X509Certificate cert : certs) {
            certTable.put(cert.getSubjectDN().toString(), cert);
        }

        // try to verify all the certs
        for (Map.Entry<String, X509Certificate> entry : certTable.entrySet()) {

            X509Certificate cert = entry.getValue();
            X509Certificate issuerCert = certTable
                    .get(cert.getIssuerDN().toString());

            System.out.println("Subject: " + cert.getSubjectDN());
            if (issuerCert == null) {
                System.out.println("Issuer certificate not found");
            } else {
                System.out.println("Issuer:  " + cert.getIssuerDN());
                cert.verify(issuerCert.getPublicKey());
                System.out.println("Cert verifies.");
            }
            System.out.println();
        }
    }

}
