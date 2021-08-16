/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7180907
 * @summary Jarsigner -verify fails if rsa file used sha-256 with authenticated attributes
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.tools.keytool
 *          java.base/sun.security.util
 *          java.base/sun.security.x509
 * @compile -XDignore.symbol.file NonStandardNames.java
 * @run main NonStandardNames
 */

import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.X509Certificate;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.pkcs.PKCS9Attributes;
import sun.security.pkcs.SignerInfo;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

public class NonStandardNames {

    public static void main(String[] args) throws Exception {

        byte[] data = "Hello".getBytes();
        X500Name n = new X500Name("cn=Me");

        CertAndKeyGen cakg = new CertAndKeyGen("RSA", "SHA256withRSA");
        cakg.generate(1024);
        X509Certificate cert = cakg.getSelfCertificate(n, 1000);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        PKCS9Attributes authed = new PKCS9Attributes(new PKCS9Attribute[]{
            new PKCS9Attribute(PKCS9Attribute.CONTENT_TYPE_OID, ContentInfo.DATA_OID),
            new PKCS9Attribute(PKCS9Attribute.MESSAGE_DIGEST_OID, md.digest(data)),
        });

        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(cakg.getPrivateKey());
        s.update(authed.getDerEncoding());
        byte[] sig = s.sign();

        SignerInfo signerInfo = new SignerInfo(
                n,
                cert.getSerialNumber(),
                AlgorithmId.get("SHA-256"),
                authed,
                AlgorithmId.get("SHA256withRSA"),
                sig,
                null
                );

        PKCS7 pkcs7 = new PKCS7(
                new AlgorithmId[] {signerInfo.getDigestAlgorithmId()},
                new ContentInfo(data),
                new X509Certificate[] {cert},
                new SignerInfo[] {signerInfo});

        if (pkcs7.verify(signerInfo, data) == null) {
            throw new Exception("Not verified");
        }
    }
}
