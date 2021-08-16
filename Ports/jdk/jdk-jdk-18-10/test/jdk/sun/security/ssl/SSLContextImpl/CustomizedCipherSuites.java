/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.

/*
 * @test
 * @bug 8162362
 * @summary Cannot enable previously default enabled cipher suites
 * @run main/othervm
 *      CustomizedCipherSuites Default true
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 *      TLS_ECDH_anon_WITH_AES_128_CBC_SHA
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites="unknown"
 *      CustomizedCipherSuites Default true
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 *      TLS_ECDH_anon_WITH_AES_128_CBC_SHA
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites=""
 *      CustomizedCipherSuites Default true
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 *      TLS_ECDH_anon_WITH_AES_128_CBC_SHA
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites="TLS_ECDH_anon_WITH_AES_128_CBC_SHA"
 *      CustomizedCipherSuites Default true
 *      TLS_ECDH_anon_WITH_AES_128_CBC_SHA
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 * @run main/othervm
 *      -Djdk.tls.server.cipherSuites="TLS_ECDH_anon_WITH_AES_128_CBC_SHA"
 *      CustomizedCipherSuites Default false
 *      TLS_ECDH_anon_WITH_AES_128_CBC_SHA
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites="TLS_RSA_WITH_AES_128_CBC_SHA,unknown,TLS_ECDH_anon_WITH_AES_128_CBC_SHA"
 *      CustomizedCipherSuites Default true
 *      TLS_ECDH_anon_WITH_AES_128_CBC_SHA
 *      ""
 * @run main/othervm
 *      -Djdk.tls.server.cipherSuites="TLS_RSA_WITH_AES_128_CBC_SHA,unknown,TLS_ECDH_anon_WITH_AES_128_CBC_SHA"
 *      CustomizedCipherSuites Default false
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 *      ""
 * @run main/othervm
 *      -Djdk.tls.server.cipherSuites="TLS_ECDH_anon_WITH_AES_128_CBC_SHA"
 *      CustomizedCipherSuites Default true
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 *      TLS_ECDH_anon_WITH_AES_128_CBC_SHA
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites="TLS_ECDH_anon_WITH_AES_128_CBC_SHA"
 *      CustomizedCipherSuites Default false
 *      TLS_RSA_WITH_AES_128_CBC_SHA
 *      TLS_ECDH_anon_WITH_AES_128_CBC_SHA
 */

import java.security.Security;
import javax.net.ssl.*;

/**
 * Test the customized default cipher suites.
 *
 * This test is based on the behavior that TLS_ECDH_anon_WITH_AES_128_CBC_SHA is
 * disabled by default, and TLS_RSA_WITH_AES_128_CBC_SHA is enabled by
 * default in JDK.  If the behavior is changed in the future, please
 * update the test cases above accordingly.
 */
public class CustomizedCipherSuites {

    private static String contextProtocol;
    private static boolean isClientMode;

    private static String enabledCipherSuite;
    private static String notEnabledCipherSuite;

    public static void main(String[] args) throws Exception {

        // reset the security property to make sure the cipher suites
        // used in this test are not disabled
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        contextProtocol = trimQuotes(args[0]);
        isClientMode = Boolean.parseBoolean(args[1]);
        enabledCipherSuite = trimQuotes(args[2]);
        notEnabledCipherSuite = trimQuotes(args[3]);

        //
        // Create instance of SSLContext with the specified protocol.
        //
        SSLContext context = SSLContext.getInstance(contextProtocol);

        // Default SSLContext is initialized automatically.
        if (!contextProtocol.equals("Default")) {
            // Use default TK, KM and random.
            context.init((KeyManager[])null, (TrustManager[])null, null);
        }

        // SSLContext default parameters is client mode in JDK.
        if (isClientMode) {
            //
            // Check default parameters of the specified SSLContext protocol
            //
            SSLParameters parameters = context.getDefaultSSLParameters();
            System.out.println("Checking SSLContext default parameters ...");
            checkEnabledCiphers(parameters.getCipherSuites());
        }

        //
        // Check supported parameters of the specified SSLContext protocol
        //
        SSLParameters parameters = context.getSupportedSSLParameters();
        System.out.println("Checking SSLContext suppport parameters ...");
        checkSupportedCiphers(parameters.getCipherSuites());


        //
        // Check the default cipher suites of SSLEngine.
        //
        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(isClientMode);

        System.out.println("Checking SSLEngine default cipher suites ...");
        checkEnabledCiphers(engine.getEnabledCipherSuites());

        //
        // Check the supported cipher suites of SSLEngine.
        //
        System.out.println("Checking SSLEngine supported cipher suites ...");
        checkSupportedCiphers(engine.getSupportedCipherSuites());

        if (isClientMode) {
            SSLSocketFactory factory = context.getSocketFactory();
            // Use an unconnected socket.
            try (SSLSocket socket = (SSLSocket)factory.createSocket()) {
                //
                // Check the default cipher suites of SSLSocket.
                //
                System.out.println(
                        "Checking SSLSocket default cipher suites ...");
                checkEnabledCiphers(socket.getEnabledCipherSuites());

                //
                // Check the supported cipher suites of SSLSocket.
                //
                System.out.println(
                        "Checking SSLSocket supported cipher suites ...");
                checkSupportedCiphers(socket.getSupportedCipherSuites());
            }
        } else {
            SSLServerSocketFactory factory = context.getServerSocketFactory();
            // Use an unbound server socket.
            try (SSLServerSocket socket =
                    (SSLServerSocket)factory.createServerSocket()) {
                //
                // Check the default cipher suites of SSLServerSocket.
                //
                System.out.println(
                        "Checking SSLServerSocket default cipher suites ...");
                checkEnabledCiphers(socket.getEnabledCipherSuites());

                //
                // Check the supported cipher suites of SSLServerSocket.
                //
                System.out.println(
                        "Checking SSLServerSocket supported cipher suites ...");
                checkSupportedCiphers(socket.getSupportedCipherSuites());
            }
        }

        System.out.println("\t... Success");
    }

    private static void checkEnabledCiphers(
            String[] ciphers) throws Exception {

        if (ciphers.length == 0) {
            throw new Exception("No default cipher suites");
        }

        boolean isMatch = false;
        if (enabledCipherSuite.isEmpty()) {
            // Don't check if not specify the expected cipher suite.
            isMatch = true;
        }

        boolean isBroken = false;
        for (String cipher : ciphers) {
            System.out.println("\tdefault cipher suite " + cipher);
            if (!enabledCipherSuite.isEmpty() &&
                        cipher.equals(enabledCipherSuite)) {
                isMatch = true;
            }

            if (!notEnabledCipherSuite.isEmpty() &&
                        cipher.equals(notEnabledCipherSuite)) {
                isBroken = true;
            }
        }

        if (!isMatch) {
            throw new Exception(
                "Cipher suite " + enabledCipherSuite + " should be enabled");
        }

        if (isBroken) {
            throw new Exception(
                "Cipher suite " + notEnabledCipherSuite + " should not be enabled");
        }
    }

    private static void checkSupportedCiphers(
            String[] ciphers) throws Exception {

        if (ciphers.length == 0) {
            throw new Exception("No supported cipher suites");
        }

        boolean hasEnabledCipherSuite = enabledCipherSuite.isEmpty();
        boolean hasNotEnabledCipherSuite = notEnabledCipherSuite.isEmpty();
        for (String cipher : ciphers) {
            System.out.println("\tsupported cipher suite " + cipher);
            if (!enabledCipherSuite.isEmpty() &&
                        cipher.equals(enabledCipherSuite)) {
                hasEnabledCipherSuite = true;
            }

            if (!notEnabledCipherSuite.isEmpty() &&
                        cipher.equals(notEnabledCipherSuite)) {
                hasNotEnabledCipherSuite = true;
            }
        }

        if (!hasEnabledCipherSuite) {
            throw new Exception(
                "Cipher suite " + enabledCipherSuite + " should be supported");
        }

        if (!hasNotEnabledCipherSuite) {
            throw new Exception(
                "Cipher suite " + notEnabledCipherSuite + " should not be enabled");
        }
    }

    private static String trimQuotes(String candidate) {
        if (candidate != null && candidate.length() != 0) {
            // Remove double quote marks from beginning/end of the string.
            if (candidate.length() > 1 && candidate.charAt(0) == '"' &&
                    candidate.charAt(candidate.length() - 1) == '"') {
                return candidate.substring(1, candidate.length() - 1);
            }
        }

        return candidate;
    }
}
