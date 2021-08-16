/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8004502
 * @summary Sanity check to ensure that Kerberos cipher suites cannot be
 *   negotiated when running on a compact profile that does not include Kerberos
 */

import java.net.*;
import java.util.*;
import javax.net.ssl.*;

public class NoKerberos {

    static final List<String> KERBEROS_CIPHER_SUITES = Arrays.asList(
        "TLS_KRB5_WITH_RC4_128_SHA",
        "TLS_KRB5_WITH_RC4_128_MD5",
        "TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
        "TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
        "TLS_KRB5_WITH_DES_CBC_SHA",
        "TLS_KRB5_WITH_DES_CBC_MD5",
        "TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
        "TLS_KRB5_EXPORT_WITH_RC4_40_MD5",
        "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",
        "TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5"
    );

    /**
     * Checks that the given array of supported cipher suites does not include
     * any Kerberos cipher suites.
     */
    static void checkNotSupported(String[] supportedSuites) {
        for (String suites: supportedSuites) {
            if (KERBEROS_CIPHER_SUITES.contains(suites)) {
                throw new RuntimeException("Supported list of cipher suites " +
                    " should not include Kerberos cipher suites");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            Class.forName("javax.security.auth.kerberos.KerberosPrincipal");
            System.out.println("Kerberos is present, nothing to test");
            return;
        } catch (ClassNotFoundException okay) { }

        // test SSLSocket
        try (Socket s = SSLSocketFactory.getDefault().createSocket()) {
            SSLSocket sslSocket = (SSLSocket)s;

            checkNotSupported(sslSocket.getSupportedCipherSuites());

            // attempt to enable each of the Kerberos cipher suites
            for (String kcs: KERBEROS_CIPHER_SUITES) {
                String[] suites = { kcs };
                try {
                    sslSocket.setEnabledCipherSuites(suites);
                    throw new RuntimeException("SSLSocket.setEnabledCipherSuitessuites allowed " +
                        kcs + " but Kerberos not supported");
                } catch (IllegalArgumentException expected) { }
            }
        }

        // test SSLServerSocket
        try (ServerSocket ss = SSLServerSocketFactory.getDefault().createServerSocket()) {
            SSLServerSocket sslSocket = (SSLServerSocket)ss;

            checkNotSupported(sslSocket.getSupportedCipherSuites());

            // attempt to enable each of the Kerberos cipher suites
            for (String kcs: KERBEROS_CIPHER_SUITES) {
                String[] suites = { kcs };
                try {
                    sslSocket.setEnabledCipherSuites(suites);
                    throw new RuntimeException("SSLSocket.setEnabledCipherSuitessuites allowed " +
                        kcs + " but Kerberos not supported");
                } catch (IllegalArgumentException expected) { }
            }
        }
    }
}
