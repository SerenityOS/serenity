/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8270946
 * @library /test/lib
 * @modules java.base/sun.security.x509
 *          java.base/sun.security.util
 * @summary Check that X509CertImpl.getFingerprint does not return null when
 *          there are errors calculating the fingerprint
 */

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import sun.security.x509.X509CertImpl;
import sun.security.util.Debug;

import jdk.test.lib.Asserts;
import jdk.test.lib.security.CertUtils;

public class GetFingerprintError {

    private static final Debug dbg = Debug.getInstance("certpath");

    public static void main(String[] args) throws Exception {
        X509Certificate cert = CertUtils.getCertFromString(CertUtils.RSA_CERT);

        // test invalid MessageDigest algorithm
        Asserts.assertNull(X509CertImpl.getFingerprint("NoSuchAlg", cert, dbg));

        // test cert with bad encoding
        X509Certificate fcert = new X509CertificateWithBadEncoding(cert);
        Asserts.assertNull(X509CertImpl.getFingerprint("SHA-256", fcert, dbg));
    }

    private static class X509CertificateWithBadEncoding
            extends CertUtils.ForwardingX509Certificate {
        private X509CertificateWithBadEncoding(X509Certificate cert) {
            super(cert);
        }
        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            throw new CertificateEncodingException();
        }
    }
}
