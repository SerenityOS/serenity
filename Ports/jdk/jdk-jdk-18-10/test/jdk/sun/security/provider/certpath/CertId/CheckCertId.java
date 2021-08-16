/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6355295
 * @summary Certificate validation using OCSP fails for a particular class of certificates
 * @modules java.base/sun.security.provider.certpath
 *          java.base/sun.security.x509
 */

import java.io.*;
import java.security.MessageDigest;
import java.util.Arrays;
import sun.security.provider.certpath.CertId;
import sun.security.x509.X509CertImpl;

/*
 * Checks that the hash value for a certificate's issuer name is generated
 * correctly. Requires any certificate that is not self-signed.
 *
 * NOTE: this test uses Sun private classes which are subject to change.
 */
public class CheckCertId {

    private static final String CERT_FILENAME = "interCA.der";

    public static void main(String[] args) throws Exception {

        X509CertImpl cert = loadCert(CERT_FILENAME);

        /* Compute the hash in the same way as CertId constructor */
        MessageDigest hash = MessageDigest.getInstance("SHA1");
        hash.update(cert.getSubjectX500Principal().getEncoded());
        byte[] expectedHash = hash.digest();

        CertId certId = new CertId(cert, null);
        byte[] receivedHash = certId.getIssuerNameHash();

        if (! Arrays.equals(expectedHash, receivedHash)) {
            throw new
                Exception("Bad hash value for issuer name in CertId object");
        }
    }

    /*
     * Load an X.509 certificate from a file.
     * Return it as a Sun private class.
     */
    private static X509CertImpl loadCert(String filename) throws Exception {

        BufferedInputStream bis =
            new BufferedInputStream(
                new FileInputStream(
                    new File(System.getProperty("test.src", "."), filename)));

        return new X509CertImpl(bis);
    }
}
