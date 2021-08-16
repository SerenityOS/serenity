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
 * @bug 8206925
 * @library /javax/net/ssl/templates
 * @summary Support the certificate_authorities extension
 */
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class CacertsLimit {
    public static void main(String[] args) throws Exception {
        for (String algorithm : new String[] {"SunX509", "PKIX"}) {
            CacertsLimit.ensureLimit(algorithm);
        }
    }

    private static void ensureLimit(String algorithm) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
        tmf.init((KeyStore)null);
        TrustManager[] tms = tmf.getTrustManagers();

        if (tms == null || tms.length == 0) {
            throw new Exception("No default key store used for trust manager");
        }

        if (!(tms[0] instanceof X509TrustManager)) {
            throw new Exception(
                "The trust manger is not an instance of X509TrustManager");
        }

        checkLimit(((X509TrustManager)tms[0]).getAcceptedIssuers());
    }

    private static void checkLimit(
            X509Certificate[] trustedCerts) throws Exception {
        int sizeAccount = 0;
        for (X509Certificate cert : trustedCerts) {
            X500Principal x500Principal = cert.getSubjectX500Principal();
            byte[] encodedPrincipal = x500Principal.getEncoded();
            sizeAccount += encodedPrincipal.length;
            if (sizeAccount > 0xFFFF) {
                throw new Exception(
                        "There are too many trusted CAs in cacerts. The " +
                        "certificate_authorities extension cannot be used " +
                        "for TLS connections.  Please rethink about the size" +
                        "of the cacerts, or have a release note for the " +
                        "impacted behaviors");
            } else if (sizeAccount > 0x4000) {
                throw new Exception(
                        "There are too many trusted CAs in cacerts. The " +
                        "certificate_authorities extension cannot be " +
                        "packaged in one TLS record, which would result in " +
                        "interoperability issues.  Please rethink about the " +
                        "size of the cacerts, or have a release note for " +
                        "the impacted behaviors");
            }
        }
    }
}

