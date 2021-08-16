/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4371801
 * @summary check that generateCertificates() returns an empty
 *      Collection when the input stream contains an encoded
 *      PKCS #7 SignedData object with no certificates.
 */

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.util.Collection;

public class GenerateCertificatesEmptyCollection {

    public static void main(String[] args) throws Exception {
        /*
         * create an empty SignedData content type in ASN.1
         * as defined in PKCS#7
         */
        byte[] b = { 0x30, 0x23,
                     /* contentInfo ::= signedData */
                     0x06, 0x09, 0x2A, (byte)0x86, 0x48,
                     (byte)0x86, (byte)0xF7, 0x0D,
                     0x01, 0x07, 0x02,
                     0x00, 0x16,
                     0x30, 0x14,                /* SignedData */
                     0x02, 0x01, 0x01,          /* version */
                     0x31, 0x00,                /* digestAlgorithms */
                     0x30, 0x0B,                /* contentInfo ::= data */
                     0x06, 0x09, 0x2A, (byte)0x86, 0x48,
                     (byte)0x86, (byte)0xF7, 0x0D,
                     0x01, 0x07, 0x01,
                     /* certificates are absent */
                     0x31, 0x00                 /* signerInfos */
                   };

        CertificateFactory cf = CertificateFactory.getInstance( "X509", "SUN");
        Collection c = cf.generateCertificates( new ByteArrayInputStream(b));
        if (!c.isEmpty())
            throw new Exception("CertificateFactory.generateCertificates() "
                + "did not return an empty Collection");
    }
}
