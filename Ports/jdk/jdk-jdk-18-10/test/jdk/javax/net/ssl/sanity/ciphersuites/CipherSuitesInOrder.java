/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 7174244 8234728
 * @summary Test for ciphersuites order
 * @run main/othervm CipherSuitesInOrder
 */
import java.util.*;
import javax.net.ssl.*;

public class CipherSuitesInOrder {

    // Supported ciphersuites
    private final static List<String> supportedCipherSuites
            = Arrays.<String>asList(
                    // TLS 1.3 cipher suites.
                    "TLS_AES_256_GCM_SHA384",
                    "TLS_AES_128_GCM_SHA256",
                    "TLS_CHACHA20_POLY1305_SHA256",
                    // Suite B compliant cipher suites, see RFC 6460.
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                    // Not suite B, but we want it to position the suite early
                    //in the list of 1.2 suites.
                    "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                    //
                    // Forward secrecy cipher suites.
                    //
                    // AES_256(GCM) - ECDHE
                    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                    // AES_128(GCM) - ECDHE
                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                    // AES_256(GCM) - DHE
                    "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
                    "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
                    // AES_128(GCM) - DHE
                    "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
                    // AES_256(CBC) - ECDHE
                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                    // AES_128(CBC) - ECDHE
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                    // AES_256(CBC) - DHE
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
                    // AES_128(CBC) - DHE
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
                    //
                    // Not forward secret cipher suites.
                    //
                    // AES_256(GCM)
                    "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
                    // AES_128(GCM)
                    "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
                    // AES_256(CBC)
                    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
                    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
                    // AES_128(CBC)
                    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
                    //
                    // Legacy, used for compatibility
                    //
                    // AES_256(CBC) - ECDHE - Using SHA
                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                    // AES_128(CBC) - ECDHE - using SHA
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                    // AES_256(CBC) - DHE - Using SHA
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
                    // AES_128(CBC) - DHE - using SHA
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                    // AES_256(CBC) - using SHA, not forward secrecy
                    "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
                    "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
                    // AES_128(CBC) - using SHA, not forward secrecy
                    "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
                    "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
                    //
                    // Deprecated, used for compatibility
                    //
                    // RSA, AES_256(GCM)
                    "TLS_RSA_WITH_AES_256_GCM_SHA384",
                    // RSA, AES_128(GCM)
                    "TLS_RSA_WITH_AES_128_GCM_SHA256",
                    // RSA, AES_256(CBC)
                    "TLS_RSA_WITH_AES_256_CBC_SHA256",
                    // RSA, AES_128(CBC)
                    "TLS_RSA_WITH_AES_128_CBC_SHA256",
                    // RSA, AES_256(CBC) - using SHA, not forward secrecy
                    "TLS_RSA_WITH_AES_256_CBC_SHA",
                    // RSA, AES_128(CBC) - using SHA, not forward secrecy
                    "TLS_RSA_WITH_AES_128_CBC_SHA",
                    // 3DES_EDE, forward secrecy.
                    "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                    "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                    "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                    // 3DES_EDE, not forward secrecy.
                    "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
                    "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                    // Renegotiation protection request Signalling
                    // Cipher Suite Value (SCSV).
                    "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
                    // Definition of the Cipher Suites that are supported but not
                    // enabled by default.
                    "TLS_DH_anon_WITH_AES_256_GCM_SHA384",
                    "TLS_DH_anon_WITH_AES_128_GCM_SHA256",
                    "TLS_DH_anon_WITH_AES_256_CBC_SHA256",
                    "TLS_ECDH_anon_WITH_AES_256_CBC_SHA",
                    "TLS_DH_anon_WITH_AES_256_CBC_SHA",
                    "TLS_DH_anon_WITH_AES_128_CBC_SHA256",
                    "TLS_ECDH_anon_WITH_AES_128_CBC_SHA",
                    "TLS_DH_anon_WITH_AES_128_CBC_SHA",
                    "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
                    "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
                    // RC4
                    "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
                    "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                    "SSL_RSA_WITH_RC4_128_SHA",
                    "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
                    "TLS_ECDH_RSA_WITH_RC4_128_SHA",
                    "SSL_RSA_WITH_RC4_128_MD5",
                    "TLS_ECDH_anon_WITH_RC4_128_SHA",
                    "SSL_DH_anon_WITH_RC4_128_MD5",
                    // Weak cipher suites obsoleted in TLS 1.2 [RFC 5246]
                    "SSL_RSA_WITH_DES_CBC_SHA",
                    "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                    "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                    "SSL_DH_anon_WITH_DES_CBC_SHA",
                    // Weak cipher suites obsoleted in TLS 1.1  [RFC 4346]
                    "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                    "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                    "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
                    "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
                    "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                    "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
                    // No traffic encryption cipher suites
                    "TLS_RSA_WITH_NULL_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_NULL_SHA",
                    "TLS_ECDHE_RSA_WITH_NULL_SHA",
                    "SSL_RSA_WITH_NULL_SHA",
                    "TLS_ECDH_ECDSA_WITH_NULL_SHA",
                    "TLS_ECDH_RSA_WITH_NULL_SHA",
                    "TLS_ECDH_anon_WITH_NULL_SHA",
                    "SSL_RSA_WITH_NULL_MD5",
                    // Definition of the cipher suites that are not supported but the names
                    // are known.
                    "TLS_AES_128_CCM_SHA256",
                    "TLS_AES_128_CCM_8_SHA256"
            );

    private final static String[] protocols = {
        "", "SSL", "TLS", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"
    };

    public static void main(String[] args) throws Exception {
        // show all of the supported cipher suites
        showSuites(supportedCipherSuites.toArray(new String[0]),
                "All supported cipher suites");

        for (String protocol : protocols) {
            System.out.println("//");
            System.out.println("// "
                    + "Testing for SSLContext of " + protocol);
            System.out.println("//");
            checkForProtocols(protocol);
        }
    }

    public static void checkForProtocols(String protocol) throws Exception {
        SSLContext context;
        if (protocol.isEmpty()) {
            context = SSLContext.getDefault();
        } else {
            context = SSLContext.getInstance(protocol);
            context.init(null, null, null);
        }

        // check the order of default cipher suites of SSLContext
        SSLParameters parameters = context.getDefaultSSLParameters();
        checkSuites(parameters.getCipherSuites(),
                "Default cipher suites in SSLContext");

        // check the order of supported cipher suites of SSLContext
        parameters = context.getSupportedSSLParameters();
        checkSuites(parameters.getCipherSuites(),
                "Supported cipher suites in SSLContext");

        //
        // Check the cipher suites order of SSLEngine
        //
        SSLEngine engine = context.createSSLEngine();

        // check the order of endabled cipher suites
        String[] ciphers = engine.getEnabledCipherSuites();
        checkSuites(ciphers,
                "Enabled cipher suites in SSLEngine");

        // check the order of supported cipher suites
        ciphers = engine.getSupportedCipherSuites();
        checkSuites(ciphers,
                "Supported cipher suites in SSLEngine");

        //
        // Check the cipher suites order of SSLSocket
        //
        SSLSocketFactory factory = context.getSocketFactory();
        try (SSLSocket socket = (SSLSocket) factory.createSocket()) {

            // check the order of endabled cipher suites
            ciphers = socket.getEnabledCipherSuites();
            checkSuites(ciphers,
                    "Enabled cipher suites in SSLSocket");

            // check the order of supported cipher suites
            ciphers = socket.getSupportedCipherSuites();
            checkSuites(ciphers,
                    "Supported cipher suites in SSLSocket");
        }

        //
        // Check the cipher suites order of SSLServerSocket
        //
        SSLServerSocketFactory serverFactory = context.getServerSocketFactory();
        try (SSLServerSocket serverSocket
                = (SSLServerSocket) serverFactory.createServerSocket()) {
            // check the order of endabled cipher suites
            ciphers = serverSocket.getEnabledCipherSuites();
            checkSuites(ciphers,
                    "Enabled cipher suites in SSLServerSocket");

            // check the order of supported cipher suites
            ciphers = serverSocket.getSupportedCipherSuites();
            checkSuites(ciphers,
                    "Supported cipher suites in SSLServerSocket");
        }
    }

    private static void checkSuites(String[] suites, String title) {
        showSuites(suites, title);

        int loc = -1;
        int index = 0;
        for (String suite : suites) {
            index = supportedCipherSuites.indexOf(suite);
            if (index <= loc) {
                throw new RuntimeException(suite + " is not in order");
            }
            loc = index;
        }
    }

    private static void showSuites(String[] suites, String title) {
        System.out.println(title + "[" + suites.length + "]:");
        for (String suite : suites) {
            System.out.println("  " + suite);
        }
    }
}
