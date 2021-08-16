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
 * @bug 8255494
 * @summary Make sure the signature algorithm to verify a PKCS7 block is
 *      DIGwithENC instead of HASHwithENC.
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.tools.keytool
 *          java.base/sun.security.x509
 */

import sun.security.pkcs.PKCS7;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

public class TwoHash {
    public static void main(String[] args) throws Exception {

        byte[] content = "Hello You fool I love you".getBytes();

        CertAndKeyGen cak = new CertAndKeyGen("EC", "SHA512withECDSA");
        cak.generate("secp256r1");
        byte[] signature = PKCS7.generateNewSignedData(
                "SHA256withECDSA",
                null,
                cak.getPrivateKey(),
                new X509Certificate[] {cak.getSelfCertificate(new X500Name("CN=Me"), 1000)},
                content,
                false,
                true, // direct sign, so that RFC 6211 check is not possible
                null);

        // The original signature should verify.
        if (new PKCS7(signature).verify(content) == null) {
            throw new RuntimeException("Should be verified");
        }

        // Modify the SHA256withECDSA signature algorithm (OID encoded as
        // "06 08 2A 86 48 CE 3D 04 03 02") to SHA384withECDSA (OID encoded as
        // "06 08 2A 86 48 CE 3D 04 03 03"). ISO_8859_1 charset is chosen
        // because it's a strictly one byte per char encoding.
        String s = new String(signature, StandardCharsets.ISO_8859_1);
        String s1 = s.replace(
                "\u0006\u0008\u002A\u0086\u0048\u00CE\u003D\u0004\u0003\u0002",
                "\u0006\u0008\u002A\u0086\u0048\u00CE\u003D\u0004\u0003\u0003");
        byte[] modified = s1.getBytes(StandardCharsets.ISO_8859_1);

        // The modified signature should still verify because the HASH
        // part of signature algorithm is ignored.
        if (new PKCS7(modified).verify(content) == null) {
            throw new RuntimeException("Should be verified");
        }
    }
}
